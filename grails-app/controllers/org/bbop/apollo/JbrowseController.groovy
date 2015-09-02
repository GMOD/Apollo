package org.bbop.apollo

import grails.converters.JSON
import liquibase.util.file.FilenameUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FileFilterUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.projection.Coordinate
import org.bbop.apollo.projection.DiscontinuousChunkProjector
import org.bbop.apollo.projection.DiscontinuousProjection
import org.bbop.apollo.projection.Projection
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


    // TODO: move to database as JSON
    // track, sequence, projection
    // TODO: should also include organism at some point as well
    private Map<String, Map<String, Projection>> projectionMap = new HashMap<>()

    def indexRouter() {
        log.debug "indexRouter ${params}"

        List<String> paramList = new ArrayList<>()
        params.eachWithIndex { entry, int i ->
            if (entry.key != "action" && entry.key != "controller") {
                paramList.add(entry.key + "=" + entry.value)
            }
        }
        String urlString = "/jbrowse/index.html?${paramList.join("&")}"
        // case 3 - validated login (just read from preferences, then
        if (permissionService.currentUser && params.organism) {
            Organism organism = Organism.findById(params.organism)
            preferenceService.setCurrentOrganism(permissionService.currentUser, organism)
        }

        if (permissionService.currentUser) {
            File file = new File(servletContext.getRealPath("/jbrowse/index.html"))
            render file.text
            return
        }

        // case 1 - anonymous login with organism ID, show organism
        if (params.organism) {
            log.debug "organism ID specified: ${params.organism}"

            // set the organism
            Organism organism = Organism.findById(params.organism)
            def session = request.getSession(true)
            session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value, organism.directory)

            // create an anonymous login
            File file = new File(servletContext.getRealPath("/jbrowse/index.html"))
            render file.text
            return
        }

        // case 2 - anonymous login with-OUT organism ID, show organism list
        forward(controller: "organism", action: "chooseOrganismForJbrowse", params: [urlString: urlString])
    }


    private String getJBrowseDirectoryForSession() {
        if (!permissionService.currentUser) {
            return request.session.getAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value)
        }

        String organismJBrowseDirectory = preferenceService.currentOrganismForCurrentUser.directory
        if (!organismJBrowseDirectory) {
            for (Organism organism in Organism.all) {
                // load if not
                if (!organism.sequences) {
                    sequenceService.loadRefSeqs(organism)
                }

                if (organism.sequences) {
                    User user = permissionService.currentUser
                    UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(user, organism)
                    Sequence sequence = organism?.sequences?.first()
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

        String dataDirectory = getJBrowseDirectoryForSession()
        String dataFileName = dataDirectory + "/" + params.path
        String fileName = FilenameUtils.getName(params.path)
        File file = new File(dataFileName);

        log.debug "processing path ${params.path} -> ${dataFileName}"

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

            if (fileName.endsWith(".json") || params.format == "json") {
//            [{"length":1382403,"name":"Group1.1","seqChunkSize":20000,"end":1382403,"start":0},{"length":1405242,"name":"Group1.10","seqChunkSize":20000,"end":1405242,"start":0},{"length":2557,"name":"Group1.11","seqChunkSize":20000,"end":2557,"start":0},
                if (fileName.endsWith("refSeqs.json")) {
                    JSONArray refSeqJsonObject = new JSONArray(file.text)
                    // TODO: it should look up the OGS track either default or variable
                    if (projectionMap) {
                        for (int i = 0; i < refSeqJsonObject.size(); i++) {

                            JSONObject sequenceValue = refSeqJsonObject.getJSONObject(i)

                            String sequenceName = sequenceValue.getString("name")
                            DiscontinuousProjection projection = projectionMap.values()?.iterator()?.next()?.get(sequenceName)
                            // not projections for every sequence  . . .
                            if (projection) {
                                Integer projectedSequenceLength = projection.length
                                sequenceValue.put("length", projectedSequenceLength)
                                sequenceValue.put("end", projectedSequenceLength)
                            }
                        }
                    }
                    response.outputStream << refSeqJsonObject.toString()
                } else if (fileName.endsWith("trackData.json")) {
                    // TODO: project trackData.json
                    // transform 2nd and 3rd array object in intervals/ncList
                    JSONObject trackDataJsonObject = new JSONObject(file.text)
                    String sequenceName = getSequenceName(file.absolutePath)
                    // get the track from the json object

                    // TODO: it should look up the OGS track either default or variable
//                    Projection projection = projectionMap.get(trackName)?.get(sequenceName)
                    Projection projection = projectionMap.values()?.iterator()?.next()?.get(sequenceName)

                    if (projection) {
                        JSONObject intervalsJsonArray = trackDataJsonObject.getJSONObject(FeatureStringEnum.INTERVALS.value)
                        JSONArray coordinateJsonArray = intervalsJsonArray.getJSONArray(FeatureStringEnum.NCLIST.value)
                        for (int i = 0; i < coordinateJsonArray.size(); i++) {
                            JSONArray coordinate = coordinateJsonArray.getJSONArray(i)
                            projectJsonArray(projection, coordinate)
                        }
                    }


                    response.outputStream << trackDataJsonObject.toString()
//                    return
                } else {
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
            else if (fileName.endsWith(".txt") && params.path.toString().startsWith("seq")) {
                // Set content size
                // fileName
//                Group1.22-18.txt
                String parentFile = file.parent
                println "HANDINGL SEQ DATA ${fileName}"
                String sequenceName = fileName.split("-")[0]

                // if getting
                Projection projection = projectionMap.values().iterator().next().get(sequenceName)
                if(projection ){
                    DiscontinuousChunkProjector discontinuousChunkProjector = DiscontinuousChunkProjector.instance
                    // TODO: get proper chunk size from the RefSeq
                    Integer defaultChunkSize = 20000
                    Integer chunkNumber = discontinuousChunkProjector.getChunkNumberFromFileName(fileName)
                    println "projected length ${projection.length}"
                    println "maping chunk ${chunkNumber} on proj"
                    List<Integer> chunks = discontinuousChunkProjector.getChunksForPath(parentFile)
                    for(Integer chunk in chunks){
                        println "chunk ${chunk} / ${chunks.size()}"
                    }
                    Integer startProjectedChunk = discontinuousChunkProjector.getStartChunk(chunkNumber,defaultChunkSize)
                    Integer endProjectedChunk = discontinuousChunkProjector.getEndChunk(chunkNumber,defaultChunkSize)
                    println "projected chunk ${startProjectedChunk}::${endProjectedChunk}"
                    Integer startOriginal = projection.projectReverseValue(startProjectedChunk)
                    Integer endOriginal = projection.projectReverseValue(endProjectedChunk)
                    println "original coord values ${startOriginal}::${endOriginal}"

//                    Integer startOriginalChunkNumber = discontinuousChunkProjector.getChunkForCoordinate(startOriginal,defaultChunkSize)
//                    Integer endOriginalChunkNumber = discontinuousChunkProjector.getChunkForCoordinate(endOriginal,defaultChunkSize)
//                    println "original chunk number ${startOriginalChunkNumber}::${endOriginalChunkNumber}"
//                    Integer startOriginalChunkCoordinate = startOriginalChunkNumber * defaultChunkSize
//                    Integer endOriginalChunkCoordinate = (endOriginalChunkNumber+1) * defaultChunkSize
//                    println "original chunk coordinate ${startOriginalChunkCoordinate}::${endOriginalChunkCoordinate}"


                    Organism organism = preferenceService.currentOrganismForCurrentUser
                    Sequence sequence = Sequence.findByNameAndOrganism(sequenceName,organism)
                    println "ffound sequence ${sequence} for org ${organism.commonName}"
                    String concatenatedSequence = sequenceService.getRawResiduesFromSequence(sequence,startOriginal,endOriginal)
                    println "concatenated length ${concatenatedSequence.length()}"

                    // re-project
//                    String inputText = concatenatedSequence
//                    String inputText = projection.projectSequence(concatenatedSequence,startOriginal,endOriginal,startOriginal)
                    String inputText = concatenatedSequence
                    println "return string length ${inputText.length()}"


                    // TODO: get chunks needed for cuts . . ..
                    // TODO: get substrings from start / end
//                    projection.cutToProjection()

                    response.setContentLength((int) inputText.bytes.length);

                    // Open the file and output streams
//                    FileInputStream inputStream = new FileInputStream(file);
//                    OutputStream out = response.getOutputStream();

                    // Copy the contents of the file to the output stream
//                    byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
//                    int count = 0;
//                    while ((count = inputStream.read(buf)) >= 0) {
//                        out.write(buf, 0, count);
//                    }

                    response.outputStream << inputText
                    response.outputStream.close()

                }
                else{
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
            println "files with range 1 ${file.absolutePath}"
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


    private JSONArray projectJsonArray(Projection projection, JSONArray coordinate) {

        // see if there are any subarrays of size >4 where the first one is a number 0-5 and do the same  . . .
        for (int subIndex = 0; subIndex < coordinate.size(); ++subIndex) {
            def subArray = coordinate.get(subIndex)
//            if(subArray?.size()>4 && (0..5).contains(subArray.getInt(0)) ){
            if (subArray instanceof JSONArray) {
//                println "rewriting subArray ${subArray}"
                projectJsonArray(projection, subArray)
            }
//            else{
//                println "not rewriting ${coordinate.get(subIndex)}"
//            }
//            }
        }

        if (coordinate.size() > 4
                && coordinate.get(0) instanceof Integer
                && coordinate.get(1) instanceof Integer
                && coordinate.get(2) instanceof Integer
        ) {
            Integer oldMin = coordinate.getInt(1)
            Integer oldMax = coordinate.getInt(2)
            Coordinate newCoordinate = projection.projectCoordinate(oldMin, oldMax)
            if (newCoordinate && newCoordinate.isValid()) {
                coordinate.set(1, newCoordinate.min)
                coordinate.set(2, newCoordinate.max)
            } else {
                log.error("Invalid mapping of coordinate ${coordinate} -> ${newCoordinate}")
                coordinate.set(1, -1)
                coordinate.set(2, -1)
            }
        }

        return coordinate
    }

    def trackList() {
        String dataDirectory = getJBrowseDirectoryForSession()
        String absoluteFilePath = dataDirectory + "/trackList.json"
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

        createProjection(jsonObject.getJSONArray(FeatureStringEnum.TRACKS.value))


        Organism currentOrganism = preferenceService.currentOrganismForCurrentUser
        if (currentOrganism != null) {
            jsonObject.put("dataset_id", currentOrganism.id)
        }
        List<Organism> list = permissionService.getOrganismsForCurrentUser()
        JSONObject organismObjectContainer = new JSONObject()
        for (organism in list) {
            JSONObject organismObject = new JSONObject()
            organismObject.put("name", organism.commonName)
            String url = "javascript:window.top.location.href = '../annotator/loadLink?"
            url += "organism=" + organism.getId();
            url += "&highlight=0";
            url += "&tracks='";
            organismObject.put("url", url)
            organismObjectContainer.put(organism.id, organismObject)
        }
        jsonObject.put("datasets", organismObjectContainer)

        if (jsonObject.include == null) jsonObject.put("include", new JSONArray())
        jsonObject.include.add("../plugins/WebApollo/json/annot.json")

        response.outputStream << jsonObject.toString()
        response.outputStream.close()
    }

    private createProjection(JSONArray tracksArray) {
        // TODO: refactor to a single method
        // get the OGS data if it exists
//        JSONArray tracksArray = jsonObject.getJSONArray(FeatureStringEnum.TRACKS.value)

        // TODO: this is only hear for debuggin
        projectionMap.clear()
        long startTime = System.currentTimeMillis()
        for (int i = 0; i < tracksArray.size(); i++) {
            JSONObject trackObject = tracksArray.getJSONObject(i)
            if (trackObject.containsKey("OGS") && trackObject.getBoolean("OGS") && !projectionMap.containsKey(trackObject.keys())) {
                println "tring to generate projection for ${trackObject.key}"
                File trackDirectory = new File(getJBrowseDirectoryForSession() + "/tracks/" + trackObject.key)
                println "track directory ${trackDirectory.absolutePath}"

                File[] files = FileUtils.listFiles(trackDirectory, FileFilterUtils.nameFileFilter("trackData.json"), TrueFileFilter.INSTANCE)

                println "# of files ${files.length}"

                Map<String, Projection> sequenceProjectionMap = new HashMap<>()

                for (File trackDataFile in files) {
//                    println "file ${trackDataFile.absolutePath}"

//                    String sequenceFileName = trackDataFile.absolutePath.substring(trackDirectory.absolutePath.length(),trackDataFile.absolutePath.length()-"trackData.json".length()).replaceAll("/","")
                    String sequenceFileName = getSequenceName(trackDataFile.absolutePath)

//                    println "sequencefileName [${sequenceFileName}]"

                    JSONObject referenceJsonObject = new JSONObject(trackDataFile.text)

                    // TODO: interpret the format properly
                    DiscontinuousProjection discontinuousProjection = new DiscontinuousProjection()
                    JSONArray coordinateReferenceJsonArray = referenceJsonObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray("nclist")
                    for (int coordIndex = 0; coordIndex < coordinateReferenceJsonArray.size(); ++coordIndex) {
                        JSONArray coordinate = coordinateReferenceJsonArray.getJSONArray(coordIndex)
                        discontinuousProjection.addInterval(coordinate.getInt(1), coordinate.getInt(2))
                    }

                    sequenceProjectionMap.put(sequenceFileName, discontinuousProjection)
                }

                println "final size: ${sequenceProjectionMap.size()}"

                projectionMap.put(trackObject.key, sequenceProjectionMap)
            }
        }

        println "total time ${System.currentTimeMillis() - startTime}"


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

    private String getTrackName(String fileName) {
        String[] tokens = fileName.split("/")
        return tokens[tokens.length - 3]
    }

    private String getSequenceName(String fileName) {
        String[] tokens = fileName.split("/")
        return tokens[tokens.length - 2]
    }
}
