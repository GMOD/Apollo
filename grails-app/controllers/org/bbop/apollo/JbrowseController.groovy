package org.bbop.apollo

import grails.converters.JSON
import liquibase.util.file.FilenameUtils
import org.apache.shiro.SecurityUtils
import org.bbop.apollo.gwt.shared.ClientTokenGenerator
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.sequence.Range
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

import javax.servlet.http.HttpServletResponse
import java.text.DateFormat
import java.text.SimpleDateFormat

import static org.springframework.http.HttpStatus.NOT_FOUND

//@CompileStatic
class JbrowseController {

    private static final int DEFAULT_BUFFER_SIZE = 10240; // ..bytes = 10KB.

    def grailsApplication
    def sequenceService
    def permissionService
    def preferenceService
    def jbrowseService
    def servletContext
    def configWrapperService
    def trackService

    def chooseOrganismForJbrowse() {
        [organisms: Organism.findAllByPublicMode(true, [sort: 'commonName', order: 'asc']), flash: [message: params.error]]
    }


    def indexRouter() {
        log.debug "indexRouter ${params}"
        log.debug "path ${params.path}"
        log.debug "request path: ${request.requestURL}"

        def paramList = []
        String clientToken = params[FeatureStringEnum.CLIENT_TOKEN.value]

        boolean requireRedirect = false
        request.parameterMap.each { entry ->
            if (entry.key != "action" && entry.key != "controller" && entry.key != "organism") {
                entry.value.each {
                    if(entry.key.endsWith("urlTemplate")){
                        String oldValue = it
                        it = JBrowseUrlHandler.fixUrlTemplate(it,request.contextPath)
                        requireRedirect = requireRedirect ?: it!=oldValue
                    }
                    paramList.add("${entry.key}=${it}")
                }
            }
        }
        String paramString = paramList.join("&")
        if(requireRedirect){
            redirect(uri: createLink(url: "/${clientToken}/jbrowse/index.html?${paramString}"))
            return
        }

        // case 3 - validated login (just read from preferences, then
        if (permissionService.currentUser && clientToken) {
//            Organism organism = preferenceService.getOrganismForToken(clientToken)
            Organism organism = preferenceService.getOrganismForTokenInDB(clientToken)
            if(organism){
                // we need to generate a client_token and do a redirection
                paramList = paramList.findAll(){
                    !it.startsWith(FeatureStringEnum.CLIENT_TOKEN.value)
                }
                clientToken = ClientTokenGenerator.generateRandomString()
                preferenceService.setCurrentOrganism(permissionService.currentUser, organism, clientToken)
                String uriString
                if (JBrowseUrlHandler.hasProtocol(paramString)) {
                    uriString = createLink(url: "${request.contextPath}/${clientToken}/jbrowse/index.html?${paramString}")
                }
                else {
                    uriString = createLink(url: "/${clientToken}/jbrowse/index.html?${paramString}")
                }
                redirect(uri:  uriString)
                return
            }
            else{
                organism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)
            }
            def availableOrganisms = permissionService.getOrganisms(permissionService.currentUser)
            if(!availableOrganisms){
                String urlString = "/jbrowse/index.html?${paramList.join("&")}"
                String username = permissionService.currentUser.username
                SecurityUtils.subject.logout()
                forward(controller: "jbrowse", action: "chooseOrganismForJbrowse", params: [urlString: urlString, error: "User '${username}' lacks permissions to view or edit the annotations of any organism."])
                return
            }
            if(!availableOrganisms.contains(organism)){
                log.warn "Organism '${organism?.commonName}' is not viewable by this user so viewing ${availableOrganisms.first().commonName} instead."
                organism = availableOrganisms.first()
            }
            if(organism && clientToken){
                preferenceService.setCurrentOrganism(permissionService.currentUser, organism, clientToken)
            }
            File file = new File(servletContext.getRealPath("/jbrowse/index.html"))
            render file.text
            return
        }
//        // case 1 - anonymous login with organism ID, show organism
        else {
            log.debug "organism ID specified: ${clientToken}"

            if (clientToken) {
                Organism organism = preferenceService.getOrganismForToken(clientToken)
                if (!organism) {
                    String urlString = "/jbrowse/index.html?${paramList.join("&")}"
                    forward(controller: "jbrowse", action: "chooseOrganismForJbrowse", params: [urlString: urlString, error: "Unable to find organism for '${clientToken}'"])
                    return
                }
                // only show if public, otherwise will go to the end and force a login
                if(organism.publicMode) {
                    def session = request.getSession(true)
                    session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value, organism.directory)
                    session.setAttribute(FeatureStringEnum.ORGANISM_ID.value, organism.id)
                    session.setAttribute(FeatureStringEnum.ORGANISM_NAME.value, organism.commonName)
                    // create an anonymous login
                    File file = new File(servletContext.getRealPath("/jbrowse/index.html") as String)
                    render file.text
                    return
                }
            }


        }

        // case 2 - anonymous login with-OUT organism ID, show organism list
        paramList.add("organism=${clientToken}")
        String urlString = "/jbrowse/index.html?${paramList.join("&")}"
        forward(controller: "jbrowse", action: "chooseOrganismForJbrowse", params: [urlString: urlString])
    }

    private String getDirectoryFromSession(String clientToken) {
        String directory = request.session.getAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value)
        if (!directory) {
            Organism organism = preferenceService.getOrganismForToken(clientToken)
            if (organism) {
                def session = request.getSession(true)
                session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value, organism.directory)
                session.setAttribute(FeatureStringEnum.ORGANISM_ID.value, organism.id)
                session.setAttribute(FeatureStringEnum.ORGANISM_NAME.value, organism.commonName)
                session.setAttribute(FeatureStringEnum.CLIENT_TOKEN.value, clientToken)
                return organism.directory
            }
        }
        return directory
    }

    /**
     * @param clientToken
     * @return
     */
    private String getJBrowseDirectoryForSession(String clientToken) {
        log.debug "current user? ${permissionService.currentUser}"
        if (!permissionService.currentUser) {
            return getDirectoryFromSession(clientToken)
        }

        String thisToken = request.session.getAttribute(FeatureStringEnum.CLIENT_TOKEN.value)
        request.session.setAttribute(FeatureStringEnum.CLIENT_TOKEN.value, clientToken)

        log.debug "getting organism for client token ${clientToken}"
        Organism currentOrganism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)
        log.debug "got organism ${currentOrganism} for client token ${clientToken}"
        String organismJBrowseDirectory = currentOrganism.directory
        if (!organismJBrowseDirectory) {
            for (Organism organism in Organism.all) {
                // load if not
                if (!organism.sequences) {
                    sequenceService.loadRefSeqs(organism)
                }

                if (organism.sequences) {
                    User user = permissionService.currentUser
                    UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(user, organism, [max: 1, sort: "lastUpdated", order: "desc"])
                    Sequence sequence =  Sequence.findAllByOrganism(organism,[sort:"end",order:"desc",max: 1]).first()
                    if (userOrganismPreference == null) {
                        userOrganismPreference = new UserOrganismPreference(
                                user: user
                                , organism: organism
                                , sequence: sequence
                                , currentOrganism: true
                        ).save(insert: true, flush: true)
                    } else {
                        userOrganismPreference.sequence = sequence
                        userOrganismPreference.currentOrganism = true
                        userOrganismPreference.save()
                    }

                    organismJBrowseDirectory = organism.directory
                    session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value, organismJBrowseDirectory)
                    session.setAttribute(FeatureStringEnum.SEQUENCE_NAME.value, sequence.name)
                    session.setAttribute(FeatureStringEnum.ORGANISM_ID.value, sequence.organismId)
                    session.setAttribute(FeatureStringEnum.ORGANISM.value, sequence.organism.commonName)
                    return organismJBrowseDirectory
                }
            }
        }
        return organismJBrowseDirectory
    }

    /**
     * Handles data directory serving for jbrowse
     */
    def data() {
        String dataDirectory = getJBrowseDirectoryForSession(params.get(FeatureStringEnum.CLIENT_TOKEN.value).toString())
        log.debug "data directory: ${dataDirectory}"
        String dataFileName = dataDirectory + "/" + params.path
        dataFileName += params.fileType ? ".${params.fileType}" : ""
        String fileName = FilenameUtils.getName(params.path)
        File file = new File(dataFileName)

        // see https://github.com/GMOD/Apollo/issues/1448
        if (!file.exists() && jbrowseService.hasOverlappingDirectory(dataDirectory,params.path)) {
            log.debug "params.path: ${params.path} directory ${dataDirectory}"
            String newPath = jbrowseService.fixOverlappingPath(dataDirectory,params.path)
            dataFileName = newPath
            dataFileName += params.fileType ? ".${params.fileType}" : ""
            file = new File(dataFileName)
        }

        if (!file.exists()) {
            Organism currentOrganism = preferenceService.getCurrentOrganismForCurrentUser(params.get(FeatureStringEnum.CLIENT_TOKEN.value).toString())
            File extendedOrganismDataDirectory = new File(configWrapperService.commonDataDirectory + File.separator + currentOrganism.id + "-" + currentOrganism.commonName)

            if (extendedOrganismDataDirectory.exists()) {
                log.debug"track found in common data directory ${extendedOrganismDataDirectory.absolutePath}"
                String newPath = extendedOrganismDataDirectory.getCanonicalPath() + File.separator + params.path
                dataFileName = newPath
                dataFileName += params.fileType ? ".${params.fileType}" : ""
                file = new File(dataFileName)
                log.debug"data file name: ${dataFileName}"
            }

            if (!file.exists()) {
                log.error("File not found: " + dataFileName)
                response.setStatus(HttpServletResponse.SC_NOT_FOUND)
                return
            }
        }


        String mimeType = getServletContext().getMimeType(fileName);
        if (!mimeType) {
            log.debug("No input MIME type of " + fileName);
            if (fileName.endsWith(".json") || params.format == "json") {
                mimeType = "application/json";
                response.setContentType(mimeType);
            } else if (fileName.endsWith(".bam")
                    || fileName.endsWith(".bw")
                    || fileName.endsWith(".bai")
                    || fileName.endsWith(".conf")
                    || fileName.endsWith(".csv")
            ) {
                mimeType = "text/plain";
            } else if (fileName.endsWith(".tbi")) {
                mimeType = "application/x-gzip";
            } else {
                log.info("Could not get MIME type of " + fileName + " falling back to text/plain");
                mimeType = "text/plain";
            }
            if (fileName.endsWith("jsonz") || fileName.endsWith("txtz")) {
                response.setHeader 'Content-Encoding', 'x-gzip'
            }
        }



        if (isCacheableFile(fileName)) {
            String eTag = createHashFromFile(file);
            String dateString = formatLastModifiedDate(file);

            response.setHeader("ETag", eTag);
            response.setHeader("Last-Modified", dateString);
        }

        String range = request.getHeader("range");
        long length = file.length();
        Range full = new Range(0, length - 1, length);

        List<Range> ranges = new ArrayList<Range>();

        // from http://balusc.blogspot.com/2009/02/fileservlet-supporting-resume-and.html#sublong
        if (range != null) {

            // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*\$")) {
                response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            } else {
                // If any valid If-Range header, then process each part of byte range.

                if (ranges.isEmpty()) {
                    for (String part : range.substring(6).split(",")) {
                        // Assuming a file with length of 100, the following examples returns bytes at:
                        // 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
                        long start = sublong(part, 0, part.indexOf("-"));
                        long end = sublong(part, part.indexOf("-") + 1, part.length());

                        if (start == -1) {
                            start = length - end;
                            end = length - 1;
                        } else if (end == -1 || end > length - 1) {
                            end = length - 1;
                        }

                        // Check if Range is syntactically valid. If not, then return 416.
                        if (start > end) {
                            response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                            return;
                        }
                        ranges.add(new Range(start, end, length));
                    }
                }
            }

        }

        response.setContentType(mimeType);
        if (ranges.isEmpty() || ranges.get(0) == full) {
            // Set content size
            response.setContentLength((int) file.length());

            // Open the file and output streams
            FileInputStream inputStream = new FileInputStream(file);
            OutputStream out = response.getOutputStream();

            // Copy the contents of the file to the output stream
            byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
            int count = 0;
            while ((count = inputStream.read(buf)) >= 0) {
                out.write(buf, 0, count);
            }
            inputStream.close();
            out.close();
        } else if (ranges.size() == 1) {
            // Return single part of file.
            Range r = ranges.get(0);
            response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
            response.setHeader("Content-Length", String.valueOf(r.length));
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

            OutputStream output = response.getOutputStream();
            byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
            long count = r.start;
            try {

                // Copy single part range.
                long ret = bis.skip(r.start);
                if (ret != r.start) {
                    log.error("Failed to read range request!");
                    bis.close();
                    output.close();
                    return;
                }

                while (count < r.end) {
                    int bret = bis.read(buf, 0, DEFAULT_BUFFER_SIZE);
                    if (bret != -1) {
                        output.write(buf, 0, bret);
                        count += bret;
                    } else break;
                }

            } catch (Exception e) {
                log.error(e.message);
                e.printStackTrace();
            }

            output.close();
            bis.close();

        }

    }


    def trackList() {
        String clientToken = params.get(FeatureStringEnum.CLIENT_TOKEN.value)
        log.debug "track list client token: ${clientToken}"
        String dataDirectory = getJBrowseDirectoryForSession(clientToken)
        log.debug "got data directory of . . . ? ${dataDirectory}"
        String absoluteFilePath = dataDirectory + "/trackList.json"
        File file = new File(absoluteFilePath);
        def mimeType = "application/json";
        response.setContentType(mimeType);
        Long id

        if (!file.exists()) {
            log.warn("Could not get for name and path: ${absoluteFilePath}");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            render status: NOT_FOUND
            return;
        }

        // add datasets to the configuration
        JSONObject jsonObject = JSON.parse(file.text) as JSONObject

        Organism currentOrganism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)
        if (currentOrganism != null) {
            jsonObject.put("dataset_id", currentOrganism.id)
        } else {
            id = request.session.getAttribute(FeatureStringEnum.ORGANISM_ID.value);
            jsonObject.put("dataset_id", id);
        }
        List<Organism> list = permissionService.getOrganismsForCurrentUser()
        JSONObject organismObjectContainer = new JSONObject()
        for (organism in list) {
            JSONObject organismObject = new JSONObject()
            organismObject.put("name", organism.commonName)
            String url = "javascript:window.top.location.href = '../../annotator/loadLink?"
            url += "organism=" + organism.getId();
            url += "&highlight=0";
            url += "&clientToken="+clientToken;
            url += "&tracks='";
            organismObject.put("url", url)
            organismObjectContainer.put(organism.id, organismObject)
        }

        if (list.size() == 0) {
            JSONObject organismObject = new JSONObject()
            organismObject.put("name", currentOrganism.commonName)
            organismObject.put("url", "#")
            organismObjectContainer.put(id, organismObject)
        }

        jsonObject.put("datasets", organismObjectContainer)

        if (jsonObject.include == null) jsonObject.put("include", new JSONArray())
        jsonObject.include.add("../plugins/WebApollo/json/annot.json")

        def plugins = grailsApplication.config.jbrowse?.plugins
        // not sure if I do it this way or via the include
        if (plugins) {
            def pluginKeys = []
            if (!jsonObject.plugins) {
                jsonObject.plugins = new JSONArray()
            } else {
                for (int i = 0; i < jsonObject.plugins.size(); i++) {
                    if(jsonObject.plugins[i] instanceof JSONObject){
                        pluginKeys.add(jsonObject.plugins[i].name)
                    }
                    else
                    if(jsonObject.plugins[i] instanceof String){
                        pluginKeys.add(jsonObject.plugins[i])
                    }
                }
            }
            // add core plugin: https://github.com/GMOD/jbrowse/blob/master/src/JBrowse/Browser.js#L244
            pluginKeys.add("RegexSequenceSearch")
            for (plugin in plugins) {
                if (!pluginKeys.contains(plugin.key)) {
                    pluginKeys.add(plugin.key)
                    JSONObject pluginObject = new JSONObject()
                    pluginObject.name = plugin.key
                    pluginObject.location = "./plugins/${plugin.key}"
                    pluginObject.putAll(plugin.value)
                    jsonObject.plugins.add(pluginObject)
                    log.info "Loading plugin: ${pluginObject.name} details: ${pluginObject as JSON}"
                }
            }
        }

        trackService.removeIncludedPlugins(jsonObject.plugins)

        // add extendedTrackList.json, if available
        if (!currentOrganism.dataAddedViaWebServices) {
            log.info "${configWrapperService.commonDataDirectory + File.separator + currentOrganism.id + "-" + currentOrganism.commonName + File.separator + OrganismController.EXTENDED_TRACKLIST}"
            File extendedTrackListFile = new File(configWrapperService.commonDataDirectory + File.separator + currentOrganism.id + "-" + currentOrganism.commonName + File.separator + OrganismController.EXTENDED_TRACKLIST)
            if (extendedTrackListFile.exists()) {
                log.debug "augmenting track JSON Object with extendedTrackList.json contents"
                JSONObject extendedTrackListObject = JSON.parse(extendedTrackListFile.text) as JSONObject
                jsonObject.getJSONArray("tracks").addAll(extendedTrackListObject.getJSONArray("tracks"))
            }
        }

        response.outputStream << jsonObject.toString()
        response.outputStream.close()
    }

    private static boolean isCacheableFile(String fileName) {
        if (fileName.endsWith(".txt") || fileName.endsWith("txtz") || fileName.endsWith("csv")) {
            return true;
        }
        if (fileName.endsWith(".json") || fileName.endsWith("jsonz")) {
            String[] names = fileName.split("\\/");
            String requestName = names[names.length - 1];
            return requestName.startsWith("lf-");
        }

        return false;
    }

    private static String formatLastModifiedDate(File file) {
        DateFormat simpleDateFormat = SimpleDateFormat.getDateInstance();
        return simpleDateFormat.format(new Date(file.lastModified()));
    }

    private static String createHashFromFile(File file) {
        String fileName = file.getName();
        long length = file.length();
        long lastModified = file.lastModified();
        return fileName + "_" + length + "_" + lastModified;
    }

    /**
     * Returns a substring of the given string value from the given begin index to the given end
     * index as a long. If the substring is empty, then -1 will be returned
     *
     * @param value The string value to return a substring as long for.
     * @param beginIndex The begin index of the substring to be returned as long.
     * @param endIndex The end index of the substring to be returned as long.
     * @return A substring of the given string value as long or -1 if substring is empty.
     */
    private static long sublong(String value, int beginIndex, int endIndex) {
        String substring = value.substring(beginIndex, endIndex);
        return (substring.length() > 0) ? Long.parseLong(substring) : -1;
    }

    def passthrough() {
        String dataFileName = params.prefix + "/" + params.path
        String fileName = FilenameUtils.getName(params.path)
        // have to prefix with a "/"
        if(!dataFileName.startsWith("/")){
            dataFileName = "/" + dataFileName
        }
        File file = new File(servletContext.getRealPath(dataFileName))

        if (!file.exists()) {
            Organism currentOrganism = preferenceService.getCurrentOrganismForCurrentUser(params.get(FeatureStringEnum.CLIENT_TOKEN.value).toString())
            File extendedOrganismDataDirectory = new File(configWrapperService.commonDataDirectory + File.separator + currentOrganism.id + "-" + currentOrganism.commonName)
            if (extendedOrganismDataDirectory.exists()) {
                log.debug"track found in common data directory ${extendedOrganismDataDirectory.absolutePath}"
                String newPath = extendedOrganismDataDirectory.getCanonicalPath() + File.separator + params.path
                dataFileName = newPath
                dataFileName += params.fileType ? ".${params.fileType}" : ""
                file = new File(dataFileName)
                log.debug"data file name: ${dataFileName}"
            }

            if (!file.exists()) {
                log.error("File not found: " + dataFileName)
                response.setStatus(HttpServletResponse.SC_NOT_FOUND)
                return
            }
        }

        String mimeType = getServletContext().getMimeType(fileName);

        String eTag = createHashFromFile(file);
        String dateString = formatLastModifiedDate(file);

//        if (isCacheableFile(fileName)) {
            response.setHeader("ETag", eTag);
            response.setHeader("Last-Modified", dateString);
//        }
        
        response.setContentType(mimeType);
        // Set content size
        response.setContentLength((int) file.length());

        // Open the file and output streams
        FileInputStream inputStream = new FileInputStream(file);
        OutputStream out = response.getOutputStream();

        // Copy the contents of the file to the output stream
        byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
        int count = 0
        while ((count = inputStream.read(buf)) >= 0) {
            out.write(buf, 0, count)
        }
        inputStream.close()
        out.close()
    }

}
