package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import liquibase.util.file.FilenameUtils
import org.apache.shiro.SecurityUtils
import org.bbop.apollo.gwt.shared.ClientTokenGenerator
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection
import org.bbop.apollo.gwt.shared.projection.ProjectionChunk
import org.bbop.apollo.gwt.shared.projection.ProjectionSequence
import org.bbop.apollo.sequence.Range
import org.bbop.apollo.sequence.SequenceTranslationHandler

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

import javax.servlet.http.HttpServletResponse
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
    def projectionService
    def trackService
    def refSeqProjectorService
    def sequenceCacheService
    def assemblageService
    def configWrapperService
    def grailsLinkGenerator



    def chooseOrganismForJbrowse() {
        [organisms: Organism.findAllByPublicMode(true, [sort: 'commonName', order: 'asc']), flash: [message: params.error]]
    }


    def indexRouter() {
        log.debug "indexRouter ${params}"
        log.debug "path ${params.path}"
        log.debug "request path: ${request.requestURL}"

        def paramList = []
        String clientToken = params[FeatureStringEnum.CLIENT_TOKEN.value]
        params.each { entry ->
            if (entry.key != "action" && entry.key != "controller" && entry.key != "organism") {
                paramList.add(entry.key + "=" + entry.value)
            }
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
                String paramString = ""
                paramList.each {
                    if(it.toString().startsWith("addStore")){
                        paramString += URLEncoder.encode(it.toString(),"UTF-8")+"&"
                    }
                    else{
                        paramString += it + "&"
                    }
                }
                String uriString = createLink(url: "/${clientToken}/jbrowse/index.html?${paramString}")
                redirect(uri:  uriString)
                return
            }
            else{
                organism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)
            }
            def availableOrganisms = permissionService.getOrganisms(permissionService.currentUser)
            if (!availableOrganisms) {
                String urlString = "/jbrowse/index.html?${paramList.join("&")}"
                String username = permissionService.currentUser.username
                SecurityUtils.subject.logout()
                forward(controller: "jbrowse", action: "chooseOrganismForJbrowse", params: [urlString: urlString, error: "User '${username}' lacks permissions to view or edit the annotations of any organism."])
                return
            }
            if (!availableOrganisms.contains(organism)) {
                log.warn "Organism '${organism?.commonName}' is not viewable by this user so viewing ${availableOrganisms.first().commonName} instead."
                organism = availableOrganisms.first()
            }
            if (organism && clientToken) {
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

    def getSeqBoundaries() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        String clientToken = inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value)
        try {
            String dataDirectory = trackService.getJBrowseDirectoryForSession(request.session, clientToken)
            String dataFileName = dataDirectory + "/seq/refSeqs.json"
            String referer = request.getHeader("Referer")
            String refererLoc = trackService.extractLocation(referer)

            MultiSequenceProjection projection = null
            if (AssemblageService.isProjectionString(refererLoc)) {
                Organism currentOrganism = preferenceService.getCurrentOrganismForCurrentUser(clientToken) ?: preferenceService.inferOrganismFromReference(refererLoc)
                projection = projectionService.getProjection(refererLoc, currentOrganism)
            }

            int spaceIndex = refererLoc.indexOf("-1..-1");
            if (spaceIndex != -1) {
                refererLoc = refererLoc.substring(0, spaceIndex + 6);
            }
            File file = new File(dataFileName);

            if (!file.exists()) {
                log.warn("File not found: " + dataFileName);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            // for each sequence we have: name (typically sequence), location start, location end,
            // original start (0 if full scaffold), original end (length if full scaffold) left text (nullable), right text (nullable)
            // we also have folding information once that is available
            JSONArray displayArray = new JSONArray()
            def projectionLength = projection.getLength()
            List<ProjectionSequence> projectionSequences = projection.getUnProjectedSequences(0,projectionLength-1)


            for(ProjectionSequence projectionSequence in projectionSequences){
                Long projectedLength = projection.getLengthForSequence(projectionSequence)
//                JSONObject thisSeq = sequenceList.get(i)
                JSONObject regionObject = new JSONObject()
//                regionObject.refseq = generateRefSeqLabel(thisSeq)
                regionObject.refseq = projectionSequence.name
//                int currentPosition = thisSeq.start ?: 0
                regionObject.originalPosition = 0 ;
//                currentPosition = projection ? projection.projectValue(currentPosition,0,projectedOffset) : currentPosition
                regionObject.start =  projectionSequence.projectedOffset
//                regionObject.end = projectedLength + projectionSequence.offset
                regionObject.end = projectionSequence.projectedOffset + 1
                regionObject.ref = refererLoc
                regionObject.'background-color' = 'yellow'
                regionObject.color = 'yellow'
                regionObject.background = 'yellow'
                regionObject.type = 'left-edge'

                displayArray.add(regionObject)
            }
            JSONObject returnObject = new JSONObject()
            returnObject.features = displayArray
            render returnObject as JSON
        }
        catch (Exception e) {
            log.error e.message
            render([error: e.message] as JSON)
        }
    }

    private def generateRefSeqLabel(JSONObject refSeqObject) {
        String returnLabel = ""
        if (refSeqObject.feature) {
            returnLabel += refSeqObject.feature.name + " ("
        }
        returnLabel += refSeqObject.name
        if (refSeqObject.feature) {
            returnLabel += ")"
        }
        return returnLabel
    }
/**
 * Handles data directory serving for jbrowse
 */
    def data() {
        def p = params
        if(p.path == "trackData.json"){
            println "path ${p.path}"
        }
        String clientToken = params.get(FeatureStringEnum.CLIENT_TOKEN.value)
        String dataDirectory = trackService.getJBrowseDirectoryForSession(request.session, clientToken)
        log.debug "data directory: ${dataDirectory}"
        String dataFileName = dataDirectory + "/" + params.path
        dataFileName += params.fileType ? ".${params.fileType}" : ""
        String fileName = FilenameUtils.getName(params.path)
        String referer = request.getHeader("Referer")
        String refererLoc = trackService.extractLocation(referer)
        println "data directory ${dataDirectory} refererLoc = ${refererLoc}"
        Organism currentOrganism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)
        if (refererLoc.contains("sequenceList")) {
            if (fileName.endsWith("trackData.json") || fileName.startsWith("lf-")) {

                SequenceCache cache = SequenceCache.findByKey(dataFileName)
                if (cache && false ) {
                    if (cache.value == String.valueOf(HttpServletResponse.SC_NOT_FOUND)) {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND)
                    } else {
                        sequenceCacheService.generateCacheTags(response, cache, dataFileName, cache.value.bytes.length)
                        response.outputStream << cache.value
                    }
                    return
                }


                String putativeSequencePathName = trackService.getSequencePathName(dataFileName)
                println "putative sequence path name ${dataFileName} -> ${putativeSequencePathName} "

                JSONObject projectionSequenceObject = (JSONObject) JSON.parse(putativeSequencePathName)
                JSONArray sequenceArray = projectionSequenceObject.getJSONArray(FeatureStringEnum.SEQUENCE_LIST.value)
                println "putative sequence array ${sequenceArray as JSON}"

                if (fileName.endsWith("trackData.json")) {


                    JSONObject trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, currentOrganism)
                    if (trackObject == null || trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value).size() == 0) {
                        cache = new SequenceCache(key: dataFileName, value: HttpServletResponse.SC_NOT_FOUND).save(insert: true)
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND)
                    } else {
                        cache = new SequenceCache(key: dataFileName, value: trackObject.toString()).save(insert: true)
                        sequenceCacheService.generateCacheTags(response, cache, dataFileName, cache.value.bytes.length)
                        response.outputStream << trackObject.toString()
                    }
                    return
                } else if (fileName.startsWith("lf-")) {
                    String trackName = projectionService.getTrackName(dataFileName)
                    JSONArray trackArray = trackService.projectTrackChunk(fileName, dataFileName, refererLoc, currentOrganism, trackName)
                    if (trackArray.size() == 0) {
                        cache = new SequenceCache(key: dataFileName, value: HttpServletResponse.SC_NOT_FOUND).save(insert: true)
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND)
                    } else {
                        cache = new SequenceCache(key: dataFileName, value: trackArray.toString()).save(insert: true)
                        sequenceCacheService.generateCacheTags(response, cache, dataFileName, cache.value.bytes.length)
                        response.outputStream << trackArray.toString()
                    }
                    return
                }

            }
            else if (fileName.endsWith(".txt") && params.path.toString().startsWith("seq")) {
                String sequencePath = sequenceService.calculatePath(params.path)

                SequenceCache cache = SequenceCache.findByKey(sequencePath)
                String returnSequence
                if (cache) {
                    returnSequence = cache.value
                } else {
                    returnSequence = refSeqProjectorService.projectSequence(dataFileName, currentOrganism)
                    cache = new SequenceCache(key: sequencePath, value: returnSequence).save(insert: true)
                }
                Date lastModifiedDate = cache.lastUpdated
                String dateString = SimpleDateFormat.getDateInstance().format(lastModifiedDate)
                String eTag = sequenceCacheService.createHash(sequencePath, (long) returnSequence.bytes.length, (long) lastModifiedDate.time);
                response.setHeader("ETag", eTag);
                response.setHeader("Last-Modified", dateString);

                // output the string the response
                // TODO: optimize this to not store in memory?
                response.setContentLength((int) returnSequence.bytes.length);
                response.outputStream << returnSequence
                response.outputStream.close()
            }
            else if (fileName.endsWith(".bw") ) {
                String bigWigPath = params.path
                println "bigwig path ${bigWigPath}"
            }
            else if (fileName.endsWith(".vcf.gz")) {
                String vcfFilePath = params.path
            }

        }

        File file = new File(dataFileName)

        // see https://github.com/GMOD/Apollo/issues/1448
        if (!file.exists() && jbrowseService.hasOverlappingDirectory(dataDirectory,params.path)) {
            println "params.path: ${params.path} directory ${dataDirectory}"
            String newPath = jbrowseService.fixOverlappingPath(dataDirectory,params.path)
            dataFileName = newPath
            dataFileName += params.fileType ? ".${params.fileType}" : ""
            file = new File(dataFileName)
        }

        if (!file.exists()) {
            log.warn("File not found: " + dataFileName);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
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
            sequenceCacheService.cacheFile(response, file)
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

                        if (start == -1l) {
                            start = length - end;
                            end = length - 1;
                        } else if (end == -1l || end > length - 1) {
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

            if (fileName.endsWith(".json") || params.format == "json") {
                // this returns ALL of the sequences . . but if we project, we'll want to grab only certain ones
                if (fileName.endsWith("refSeqs.json")) {
                    // ONLY ever return the refSeq we are on
                    JSONArray sequenceArray = new JSONArray()


                    JSONObject refererObject
                    Boolean mangleNames = false

                    if (AssemblageService.isProjectionString(refererLoc)) {
                        MultiSequenceProjection projection = projectionService.getProjection(refererLoc, currentOrganism)
                        Integer lastIndex = refererLoc.lastIndexOf("}:")
                        String sequenceString = refererLoc.substring(0, lastIndex + 1)
                        refererObject = new JSONObject(sequenceString)
                        refererObject.seqChunkSize = 20000
                        sequenceArray.add(refererObject)
                        sequenceArray = refSeqProjectorService.projectRefSeq(sequenceArray, projection, currentOrganism, refererLoc)
                        mangleNames = true
                    } else
                    if (AssemblageService.isProjectionReferer(refererLoc)) {
                        MultiSequenceProjection projection = projectionService.getProjection(refererLoc, currentOrganism)

                        // NOTE: not sure if this is the correct object
                        refererObject = new JSONObject(refererLoc)
                        sequenceArray.add(refererObject)
                        sequenceArray = refSeqProjectorService.projectRefSeq(sequenceArray, projection, currentOrganism, refererLoc)
                        mangleNames = true
                    } else {
                        // get the sequence
                        String sequenceName = refererLoc.split(":")[0]
                        Sequence sequence = Sequence.findByName(sequenceName) ?: currentOrganism.sequences.first()
                        refererObject = new JSONObject()
                        refererObject.putAll(sequence.properties)
                        sequenceArray.add(refererObject)
                    }
                    // We add the data refSeq here
                    String fileText = new File(dataFileName).text
                    JSONArray inputArray = JSON.parse(fileText) as JSONArray

                    for(assemblage in Assemblage.findAllByOrganism(currentOrganism)){
                        JSONObject assemblageRefSeq = projectionService.generateRefSeqForAssemblage(assemblage)
                        assemblageRefSeq.name = assemblageRefSeq.toString()
                        sequenceArray.add(assemblageRefSeq)
                    }

                    if(mangleNames){
                        sequenceArray.addAll(projectionService.fixProjectionName(inputArray))
                    }
                    else{
                        sequenceArray.addAll(inputArray)
                    }

                    response.outputStream << sequenceArray.toString()
                } else {
                    // Set content size
                    response.setContentLength((int) file.length())

                    // Open the file and output streams
                    FileInputStream inputStream = new FileInputStream(file)
                    OutputStream out = response.outputStream

                    // Copy the contents of the file to the output stream
                    byte[] buf = new byte[DEFAULT_BUFFER_SIZE]
                    int count = 0;
                    while ((count = inputStream.read(buf)) >= 0) {
                        out.write(buf, 0, count)
                    }
                    inputStream.close()
                    out.close()
                }
            }
            // handle the sequence text data
            else
            if (fileName.endsWith(".txt") && params.path.toString().startsWith("seq")) {
                response.setContentLength((int) file.length());

                MultiSequenceProjection projection = projectionService.getProjection(refererLoc, currentOrganism)
                if(projection && projection.getLastSequence().reverse){
                    response.outputStream << SequenceTranslationHandler.reverseComplementSequence(file.text)
                    response.outputStream.close()
                }
                else{
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
                }
            }
//            else if (fileName.endsWith(".bai")) {
//                println "processing bai file"
//
//                // TODO: read in . . . write out another one to process . . . which will be alternate index?
//                // file, index file, etc. etc. etc.
//                // generate the BAM
//                if (projectionMap) {
//                    // TODO: implement
////                    String bamfileName = findBamFileName(fileName)
////                    File bamFile = new File(bamfileName)
////                    File newIndexFile = new File(generateBamIndexFileForProjection())
//////                    BAMIndexer.createIndex(new SAMFileReader(bamFile),newIndexFile)
////                    ProjectionBAMIndexer.createIndex(new SAMFileReader(bamFile),newIndexFile)
////                    BAMFileReader reader = new BAMFileReader(bamFile,file,false)
//                }
//
////                SAMFileReader samFileReader = new SAMFileReader()
//                // Set content size
//                response.setContentLength((int) file.length());
//
//                // Open the file and output streams
//                FileInputStream inputStream = new FileInputStream(file);
//                OutputStream out = response.getOutputStream();
//
//                // Copy the contents of the file to the output stream
//                byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
//                int count = 0;
//                while ((count = inputStream.read(buf)) >= 0) {
//                    out.write(buf, 0, count);
//                }
//                inputStream.close();
//                out.close();
//            }
            else {
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

            }
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


    private String calculateOriginalChunkName(List<ProjectionChunk> projectionChunks, String finalSequenceString, Integer chunkIndex) {
        for (int i = 0; i < projectionChunks.size(); i++) {
            if (projectionChunks.get(i).sequence == finalSequenceString) {
                return "lf-${chunkIndex - i + 1}.json"
            }
        }
        println "unable to find an offset "
        return "lf-${chunkIndex + 1}.json"
    }


    def trackList() {
        String clientToken = params.get(FeatureStringEnum.CLIENT_TOKEN.value)
        log.debug "track list client token: ${clientToken}"
        String dataDirectory = trackService.getJBrowseDirectoryForSession(request.session, clientToken)
        log.debug "got data directory of . . . ? ${dataDirectory}"

        String absoluteFilePath = dataDirectory + "/trackList.json"
        File file = new File(absoluteFilePath)

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
        JSONObject trackObject = JSON.parse(file.text) as JSONObject

        Organism currentOrganism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)

        trackObject = rewriteTracks(trackObject,currentOrganism)


        if (currentOrganism != null) {
            trackObject.put("dataset_id", currentOrganism.id)
        } else {
            id = request.session.getAttribute(FeatureStringEnum.ORGANISM_ID.value);
            trackObject.put("dataset_id", id);
        }
        List<Organism> list = permissionService.getOrganismsForCurrentUser()
        JSONObject organismObjectContainer = new JSONObject()
        for (organism in list) {
            JSONObject organismObject = new JSONObject()
            organismObject.put("name", organism.commonName)
            String url = "javascript:window.top.location.href = '../../annotator/loadLink?"
            url += "organism=" + organism.getId();
            url += "&highlight=0";
            url += "&clientToken=" + clientToken;
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

        trackService.putOrganismTrack(currentOrganism,trackObject)

        trackObject.put("datasets", organismObjectContainer)

        if (trackObject.include == null) {
            trackObject.put("include", new JSONArray())
        }
        trackObject.include.add("../plugins/WebApollo/json/annot.json")

        def plugins = grailsApplication.config.jbrowse?.plugins
        // not sure if I do it this way or via the include
        if (plugins) {
            def pluginKeys = []
            if (!trackObject.plugins) {
                trackObject.plugins = new JSONArray()
            } else {
                for (int i = 0; i < trackObject.plugins.size(); i++) {
                    if (trackObject.plugins[i] instanceof JSONObject) {
                        pluginKeys.add(trackObject.plugins[i].name)
                    } else if (trackObject.plugins[i] instanceof String) {
                        pluginKeys.add(trackObject.plugins[i])
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
                    trackObject.plugins.add(pluginObject)
                    log.info "Loading plugin: ${pluginObject.name} details: ${pluginObject as JSON}"
                }
            }
        }

        response.outputStream << trackObject.toString()
        response.outputStream.close()
    }


    @NotTransactional
    private JSONObject rewriteTrack(JSONObject obj) {
        log.debug "Rewriting track ${obj as JSON}"
        if(obj.type == "JBrowse/View/Track/Wiggle/XYPlot" || obj.type == "JBrowse/View/Track/Wiggle/Density"){
            String urlTemplate = obj.urlTemplate ?: obj.query.urlTemplate
            obj.storeClass = "JBrowse/Store/SeqFeature/REST"
            obj.baseUrl =  "${grailsLinkGenerator.contextPath}/bigwig/${obj.key}/${obj.organismId}"
            obj.query = obj.query ?: new JSONObject()
            obj.query.urlTemplate = urlTemplate
        }
        else if (obj.storeClass == "JBrowse/Store/SeqFeature/VCFTabix") {
            String urlTemplate = obj.urlTemplate ?: obj.query.urlTemplate
            // Switching to REST store
            obj.storeClass = "WebApollo/Store/SeqFeature/VCFTabixREST"
            obj.baseUrl = "${grailsLinkGenerator.contextPath}/vcf/${obj.key}/${obj.organismId}"
            obj.query = obj.query ?: new JSONObject()
            obj.query.urlTemplate = urlTemplate
            //obj.region_feature_densities = true
        }
        else
        if(obj.storeClass == "JBrowse/Store/SeqFeature/BAM"){
            log.debug "REWRITIGN BAM"
//            if(obj.type == "JBrowse/View/Track/DraggableAlignments"){
            String urlTemplate = obj.urlTemplate ?: obj.query.urlTemplate
            obj.storeClass = "JBrowse/Store/SeqFeature/REST"
            obj.baseUrl =  "${grailsLinkGenerator.contextPath}/bam/${obj.key}/${obj.organismId}"
            obj.query = obj.query ?: new JSONObject()
            obj.query.urlTemplate = urlTemplate
            obj.region_stats = true
        }
        log.debug "final obj ${obj}"
        log.debug "Rewrote track ${obj as JSON}"
        return obj
    }

    @NotTransactional
    private JSONObject rewriteTracks(JSONObject jsonObject,Organism organism) {
        JSONArray tracksArray = jsonObject.getJSONArray(FeatureStringEnum.TRACKS.value)
        for(JSONObject obj in tracksArray){
            obj.put(FeatureStringEnum.ORGANISM_ID.value,organism.id)
            obj = rewriteTrack(obj)
        }
        return jsonObject
    }

    def annotInclude() {
        String realPath = servletContext.getRealPath("/jbrowse/plugins/WebApollo/json/annot.json")
        File file = new File(realPath)
        String returnString = file.text

        response.outputStream << filterObject(returnString)
        response.outputStream.close()
    }

    // TODO: can optimize and get fancier if have to add more stuff to our filter replacment code
    def filterObject(String returnString) {

        String currentUrl = createLink(absolute: true, uri: "/")
        returnString = returnString.replaceAll("@SERVER@", currentUrl)

        return returnString
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
        if (!dataFileName.startsWith("/")) {
            dataFileName = "/" + dataFileName
        }
        File file = new File(servletContext.getRealPath(dataFileName))

        if (!file.exists()) {
            log.warn("File not found: " + dataFileName);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String mimeType = getServletContext().getMimeType(fileName);

        // TODO: refactor to use existing methods in cache service
        sequenceCacheService.cacheFile(response, file)

        response.setContentType(mimeType);
//        // Set content size
//        response << file.text
//        response.flushBuffer()
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
    }
}
