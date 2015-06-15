package org.bbop.apollo

import grails.converters.JSON
import org.apache.shiro.session.Session
import org.apache.shiro.SecurityUtils
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.sequence.Range
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.json.JSONArray

import javax.servlet.http.HttpServletResponse
import java.text.DateFormat
import java.text.SimpleDateFormat
import static org.springframework.http.HttpStatus.*

//@CompileStatic
class JbrowseController {

    private static final int DEFAULT_BUFFER_SIZE = 10240; // ..bytes = 10KB.

    def sequenceService
    def permissionService
    def preferenceService
    def servletContext

    def indexRouter(){
        log.debug "routing the index: ${params}"

        List<String> paramList = new ArrayList<>()
        params.eachWithIndex{ entry, int i ->
            if(entry.key!="action" && entry.key!="controller"){
                paramList.add(entry.key+"="+entry.value)
            }
        }
        String urlString = "/jbrowse/index.html?${paramList.join("&")}"
        // case 3 - validated login (just read from preferences, then
        if(permissionService.currentUser&&params.organism){
            Organism organism = Organism.findById(params.organism)
            preferenceService.setCurrentOrganism(permissionService.currentUser,organism)
        }

        if(permissionService.currentUser) {
            File file = new File(servletContext.getRealPath("jbrowse/index.html"))
            render file.text
            return
        }

        log.debug "anonymous user"

        // case 1 - anonymous login with organism ID, show organism
        if(params.organism){
            log.debug "organism ID specified: ${params.organism}"

            // set the organism
            Organism organism = Organism.findById(params.organism)
            def session = request.getSession(true)
            session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value,organism.directory)

            // create an anonymous login
            File file = new File(servletContext.getRealPath("jbrowse/index.html"))
            render file.text
            return
        }

        // case 2 - anonymous login with-OUT organism ID, show organism list
        forward(controller: "organism", action: "chooseOrganismForJbrowse",params:[urlString:urlString])
    }


    // is typically checking for trackData.json
    def tracks(String jsonFile, String trackName, String groupName) {
        String fileName = getJBrowseDirectoryForSession()
        fileName += "/tracks/${trackName}/${groupName}/${jsonFile}.json"
        File file = new File(fileName);
        if (!file.exists()) {
            log.error("Could not get tracks file " + fileName);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            render status: NOT_FOUND
            return;
        }
        render file.text
    }

    private String getJBrowseDirectoryForSession() {
        if(!permissionService.getCurrentUser()){
            return request.session.getAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value)
        }

        long startTime = System.currentTimeMillis()
        String organismJBrowseDirectory = preferenceService.currentOrganismForCurrentUser.directory
        long stopTime = System.currentTimeMillis()
        // this is about 0.004 ms per request .. .
        log.debug "total time to grab preference ${(stopTime-startTime)/1000f}"

        if (!organismJBrowseDirectory) {
            for (Organism organism in Organism.all) {
                // load if not
                if (!organism.sequences) {
                    sequenceService.loadRefSeqs(organism)
                }

                if (organism.sequences) {
                    User user = permissionService.currentUser
                    UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(user,organism)
                    Sequence sequence = organism?.sequences?.first()
                    if(userOrganismPreference ==null){
                        userOrganismPreference = new UserOrganismPreference(
                                user: user
                                ,organism: organism
                                ,sequence: sequence
                                ,currentOrganism: true
                        ).save(insert:true,flush:true)
                    }
                    else{
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


    def namesFiles(String directory, String jsonFile) {
        String dataDirectory = getJBrowseDirectoryForSession()
        String absoluteFilePath = dataDirectory + "/names/${directory}/${jsonFile}.json"
        log.debug "names Files ${absoluteFilePath}"
        File file = new File(absoluteFilePath);
        if (!file.exists()) {
            log.warn("Could not get for name and path: ${absoluteFilePath}");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            render status: NOT_FOUND
            return;
        }
        render file.text
    }

    /**
     * For returning seq/refSeqs.json
     */
    def names(String fileName) {
        log.debug "names"
        String dataDirectory = getJBrowseDirectoryForSession()
        String absoluteFilePath = dataDirectory + "/names/${fileName}.json"
        File file = new File(absoluteFilePath);
        if (!file.exists()) {
            log.warn("Could not get ${absoluteFilePath}");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            render status: NOT_FOUND
            return;
        }
        render file.text
    }

    /**
     * For returning seq/refSeqs.json
     */
    def seq() {
        log.debug "seq"
        String fileName = getJBrowseDirectoryForSession()
        File file = new File(fileName + "/seq/refSeqs.json");
        if (!file.exists()) {
            log.error("Could not get seq file " + fileName);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        render file.text
    }

    def seqMapper() {
        String fileName = getJBrowseDirectoryForSession()
        File file = new File(fileName + "/seq/${params.a}/${params.b}/${params.c}/${params.group}");
        if (!file.exists()) {
            log.error("Could not get seq file " + file.absolutePath);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            render status: NOT_FOUND
            return;
        }

        String eTag = createHashFromFile(file);
        String dateString = formatLastModifiedDate(file);
        response.setHeader("ETag", eTag);
        response.setHeader("Last-Modified", dateString);
        response.setContentType("application/json");


        render file.text
    }

    def bigwig(String fileName) {
        return data("bigwig/" + fileName)
    }

    def bam(String fileName) {
        return data("bam/" + fileName)
    }

    /**
     * Has to handle a number of routes for the data directory.
     *
     * e.g. --
     * tracks.conf
     * data/tracks/<track>/<annotation>/trackData.json
     * data/tracks/Amel_4.5_brain_ovary.gff/Group1.1/lf-1.json
     * data/bigwig/<fileName>.bw
     */
    def data(String fileName) {
        String dataDirectory = getJBrowseDirectoryForSession()
        log.debug "dataDir: ${dataDirectory}"
        log.debug "URI: " + request.getRequestURI()
        log.debug "URL: " + request.getRequestURL()
        log.debug "pathInfo: " + request.getPathInfo()
        log.debug "pathTranslated: " + request.getPathTranslated()
        log.debug "params: " + params

        String dataFileName = dataDirectory + "/" + fileName
        File file = new File(dataFileName);

        if (!file.exists()) {
            log.error("Could not get data directory: " + dataFileName);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }



        String mimeType = getServletContext().getMimeType(fileName);
        if (!mimeType) {
            log.info("No input MIME type of " + fileName);
            if (fileName.endsWith(".json") || params.format == "json") {
                mimeType = "application/json";
                response.setContentType(mimeType);
            } else if (fileName.endsWith(".bam")
                    || fileName.endsWith(".bw")
                    || fileName.endsWith(".bai")
                    || fileName.endsWith(".conf")
            ) {
                mimeType = "text/plain";
            } else if (fileName.endsWith(".tbi")) {
                mimeType = "application/x-gzip";
            } else {
                log.error("Could not get MIME type of " + fileName + " falling back to text/plain");
                mimeType = "text/plain";
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
            byte[] buf = new byte[1024];
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

            RandomAccessFile input = new RandomAccessFile(file, "r");
            OutputStream output = response.getOutputStream();

            // Copy single part range.
            copy(input, output, r.start, r.length);

            input.close();
            output.close();

        }

    }

    def trackList() {
        String dataDirectory = getJBrowseDirectoryForSession()
        String absoluteFilePath = dataDirectory + "/trackList.json"
        log.debug "trackList ${absoluteFilePath}"
        File file = new File(absoluteFilePath);
        def mimeType = "application/json";
        response.setContentType(mimeType);

        if (!file.exists()) {
            log.warn("Could not get for name and path: ${absoluteFilePath}");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            render status: NOT_FOUND
            return;
        }

        // add datasets to the configuration
        JSONObject jsonObject = JSON.parse(file.text) as JSONObject
        Organism currentOrganism = preferenceService.currentOrganismForCurrentUser
        if(currentOrganism!=null) {
            jsonObject.put("dataset_id",currentOrganism.id)
        }
        List<Organism> list=Organism.getAll()
        JSONObject organismObjectContainer = new JSONObject()
        for(organism in list) {
            JSONObject organismObject = new JSONObject()
            organismObject.put("name",organism.commonName)
            String url = "javascript:window.top.location.href = '../annotator/loadLink?"
            url += "organism=" + organism.getId();
            url += "&highlight=0";
            url += "&tracks='";
            organismObject.put("url",url)
            organismObjectContainer.put(organism.id, organismObject)
        }
        jsonObject.put("datasets",organismObjectContainer)

        if(jsonObject.include==null) jsonObject.put("include",new JSONArray())
        jsonObject.include.add("../plugins/WebApollo/json/annot.json")

        response.outputStream << jsonObject.toString()
        response.outputStream.close()
    }

    private static boolean isCacheableFile(String fileName) {
        if (fileName.endsWith(".txt")) return true;
        if (fileName.endsWith(".json")) {
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
     * Copy the given byte range of the given input to the given output.
     *
     * @param input The input to copy the given range to the given output for.
     * @param output The output to copy the given range from the given input for.
     * @param start Start of the byte range.
     * @param length Length of the byte range.
     * @throws IOException If something fails at I/O level.
     */
    private static void copy(RandomAccessFile input, OutputStream output, long start, long length)
            throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;

        if (input.length() == length) {
            // Write full range.
            while ((read = input.read(buffer)) > 0) {
                output.write(buffer, 0, read);
            }
        } else {
            // Write partial range.
            input.seek(start);
            long toRead = length;

            while ((read = input.read(buffer)) > 0) {
                if ((toRead -= read) > 0) {
                    output.write(buffer, 0, read);
                } else {
                    output.write(buffer, 0, (int) toRead + read);
                    break;
                }
            }
        }
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
}
