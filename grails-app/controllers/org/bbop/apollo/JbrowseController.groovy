package org.bbop.apollo

import grails.converters.JSON
import liquibase.util.file.FilenameUtils
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.projection.DiscontinuousChunkProjector
import org.bbop.apollo.projection.Location
import org.bbop.apollo.projection.MultiSequenceProjection
import org.bbop.apollo.projection.ProjectionDescription
import org.bbop.apollo.projection.ProjectionInterface
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

    def grailsApplication
    def sequenceService
    def permissionService
    def preferenceService
    def servletContext
    def projectionService
    def trackService

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
            session.setAttribute(FeatureStringEnum.ORGANISM_ID.value, organism.id)
            session.setAttribute(FeatureStringEnum.ORGANISM_NAME.value, organism.commonName)

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
        Organism currentOrganism = preferenceService.currentOrganismForCurrentUser
        String dataDirectory = getJBrowseDirectoryForSession()
        String dataFileName = dataDirectory + "/" + params.path
        String fileName = FilenameUtils.getName(params.path)

        String referer = request.getHeader("Referer")
        int startIndex = referer.indexOf("?loc=")
        int endIndex = referer.indexOf("&")
        String refererLoc = referer.subSequence(startIndex + 5, endIndex)
        refererLoc = URLDecoder.decode(refererLoc, "UTF-8")

        String putativeSequencePathName = trackService.getSequencePathName(dataFileName)
        println "putative sequence path name ${dataFileName} -> ${putativeSequencePathName} "
//        String putativeTrackPathName = getTrackPathName(dataFileName)
//        /opt/apollo/honeybee/data//tracks/Official Gene Set v3.2/{"projection":"None", "padding":50, "referenceTrack":"Official Gene Set v3.2", "sequences":[{"name":"Group11.18"},{"name":"Group9.10"}]}/trackData.json
        if (putativeSequencePathName.contains("projection")) {
            if (fileName.endsWith("trackData.json")) {
                JSONObject projectionSequenceObject = JSON.parse(putativeSequencePathName)
                JSONArray sequenceArray = projectionSequenceObject.getJSONArray("sequences")
                List<String> sequenceStrings = new ArrayList<>()
//                "sequences":[{"n
                for (int i = 0; i < sequenceArray.size(); i++) {
                    JSONObject sequenceObject = sequenceArray.getJSONObject(i)
                    sequenceStrings.add(sequenceObject.name)
                }

                List<JSONObject> trackObjectList = new ArrayList<>()

                for (sequence in sequenceStrings) {
                    // next we want to load tracks from the REAL paths . .  .
//                    String sequencePathName = dataFileName.replaceAll(putativeSequencePathName, sequence)
                    String sequencePathName = trackService.generateTrackNameForSequence(dataFileName,sequence)
                    // this loads PROJECTED
                    JSONObject trackObject = trackService.loadTrackData(sequencePathName,refererLoc,currentOrganism)
                    trackObjectList.add(trackObject)
                }

                JSONObject trackObject = trackService.mergeTrackObject(trackObjectList)
                response.outputStream << trackObject.toString()
                return
            }
        }
        // 1 - if endsWith trackData and the "sequence" contains a projection then
//        if(fileName.endsWith("trackData.json"))
        //  a - extract the trackData (refactor into its own method)
        //  b - merge them together
        //  c - return that

        File file = new File(dataFileName);

//        http://localhost:8080/apollo/jbrowse/index.html?loc=[proj=None,padding=50,sequences=[Group11.18::Group9.10,Group1.1(GB42145-RA)]]%3A-1..-1&highlight=&tracklist=0

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
//            response.setContentLength((int) file.length());

            if (fileName.endsWith(".json") || params.format == "json") {
//            [{"length":1382403,"name":"Group1.1","seqChunkSize":20000,"end":1382403,"start":0},{"length":1405242,"name":"Group1.10","seqChunkSize":20000,"end":1405242,"start":0},{"length":2557,"name":"Group1.11","seqChunkSize":20000,"end":2557,"start":0},
                // this returns ALL of the sequences . . but if we project, we'll want to grab only certain ones
                if (fileName.endsWith("refSeqs.json")) {
                    JSONArray refSeqJsonObject = new JSONArray(file.text)
                    // TODO: it should look up the OGS track either default or variable
//                    if (projectionService.hasProjection(preferenceService.currentOrganismForCurrentUser,projectionService.getTrackName(file.absolutePath))) {
                    println "refseq size ${refSeqJsonObject.size()}"

                    JSONArray projectedArray = new JSONArray()
                    MultiSequenceProjection projection = projectionService.getProjection(refererLoc, currentOrganism)

                    for (int i = 0; i < refSeqJsonObject.size(); i++) {

                        JSONObject sequenceValue = refSeqJsonObject.getJSONObject(i)

                        String sequenceName = sequenceValue.getString("name")
                        if (projection && projection.containsSequence(sequenceName, sequenceValue.id, currentOrganism)) {
                            log.debug "input sequence ${sequenceValue as JSON}"
                            Integer projectedSequenceLength = projection.findProjectSequenceLength(sequenceName)
                            sequenceValue.put("length", projectedSequenceLength)
                            sequenceValue.put("end", projectedSequenceLength)
                            sequenceValue.put("name", refererLoc)
                            log.debug "output sequence ${sequenceValue as JSON}"
                            projectedArray = trackService.mergeRefseqProjections(projectedArray,sequenceValue)
                            log.debug "final array ${projectedArray as JSON}"
                        }
                    }

                    if (projection) {
                        response.outputStream << projectedArray.toString()
                    } else {
                        response.outputStream << refSeqJsonObject.toString()
                    }
                    return
                } else if (fileName.endsWith("trackData.json")) {
                    // TODO: project trackData.json
                    // if we are a projection, then we are going to want to merge multiple sequences
                    // into a single trackData.json
                    println "trying to get trackData for ${file.absolutePath}"
                    println "fileNAme is NOT lf-type ${fileName}"
                    // transform 2nd and 3rd array object in intervals/ncList
                    JSONObject trackDataJsonObject = trackService.loadTrackData(file.absolutePath,refererLoc,currentOrganism)
                    response.outputStream << trackDataJsonObject.toString()

                    return
                } else if (fileName.startsWith("lf-")) {
                    // TODO: project trackData.json
                    JSONArray trackDataJsonObject = new JSONArray(file.text)
                    String sequenceName = projectionService.getSequenceName(file.absolutePath)
                    // get the track from the json object

                    // TODO: it should look up the OGS track either default or variable
                    ProjectionInterface projection = projectionService.getProjection(preferenceService.currentOrganismForCurrentUser, projectionService.getTrackName(file.absolutePath), sequenceName)

                    if (projection) {
//                        println "re-writing the LF array ${trackDataJsonObject.size()}"
//                        println "coordinates are ${projection.toString()}"
                        for (int i = 0; i < trackDataJsonObject.size(); i++) {
                            JSONArray coordinate = trackDataJsonObject.getJSONArray(i)
//                            println "lf-from ${coordinate as JSON}"
                            trackService.projectJsonArray(projection, coordinate)
//                            println "lf-to ${coordinate as JSON}"
                        }
                    }
                    response.outputStream << trackDataJsonObject.toString()
                    return
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
            } else if (fileName.endsWith(".txt") && params.path.toString().startsWith("seq")) {
                // Set content size
                // fileName
//                Group1.22-18.txt
                String parentFile = file.parent
                println "HANDINGL SEQ DATA ${fileName}"
                String sequenceName = fileName.split("-")[0]

                // if getting
//                ProjectionInterface projection = projectionMap.values().iterator().next().get(sequenceName)
                ProjectionInterface projection = projectionService.getProjection(preferenceService.currentOrganismForCurrentUser, projectionService.getTrackName(file.absolutePath), sequenceName)
                if (projection) {
                    DiscontinuousChunkProjector discontinuousChunkProjector = DiscontinuousChunkProjector.instance
                    // TODO: get proper chunk size from the RefSeq
                    Integer defaultChunkSize = 20000
                    Integer chunkNumber = discontinuousChunkProjector.getChunkNumberFromFileName(fileName)
                    println "projected length ${projection.length}"
                    println "maping chunk ${chunkNumber} on proj"
                    List<Integer> chunks = discontinuousChunkProjector.getChunksForPath(parentFile)
                    for (Integer chunk in chunks) {
                        println "chunk ${chunk} / ${chunks.size()}"
                    }
                    Integer startProjectedChunk = discontinuousChunkProjector.getStartChunk(chunkNumber, defaultChunkSize)
                    Integer endProjectedChunk = discontinuousChunkProjector.getEndChunk(chunkNumber, defaultChunkSize)
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
                    Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, organism)
                    println "ffound sequence ${sequence} for org ${organism.commonName}"
                    // TODO: fix the offsets here
                    String concatenatedSequence = sequenceService.getRawResiduesFromSequence(sequence, startOriginal, endOriginal)
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

                } else {
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
        Organism currentOrganism = preferenceService.currentOrganismForCurrentUser
//        projectionService.createTranscriptProjection(currentOrganism, jsonObject.getJSONArray(FeatureStringEnum.TRACKS.value),50)

        // this comes from the
        if (grailsApplication.config.apollo.doProjection) {
            if (grailsApplication.config.apollo.useMultiSequence) {
                ProjectionDescription projectionDescription = new ProjectionDescription()
                projectionDescription.padding = 50
//                projectionDescription.featureNames = ["ALL"]
                projectionDescription.referenceTracks = ["Official Gene Set v3.2"]
                // TODO: get the proper name from the UI
                projectionDescription.type = "EXON"
                List<Location> locationList = projectionService.extractExonLocations(currentOrganism, jsonObject.getJSONArray(FeatureStringEnum.TRACKS.value), projectionDescription)
                projectionService.createMultiSequenceProjection(projectionDescription, locationList)
            } else {
                projectionService.createExonLevelProjection(currentOrganism, jsonObject.getJSONArray(FeatureStringEnum.TRACKS.value), 50)
            }
        }
        long id

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
            String url = "javascript:window.top.location.href = '../annotator/loadLink?"
            url += "organism=" + organism.getId();
            url += "&highlight=0";
            url += "&tracks='";
            organismObject.put("url", url)
            organismObjectContainer.put(organism.id, organismObject)
        }

        if (list.size() == 0) {
            JSONObject organismObject = new JSONObject()
            organismObject.put("name", Organism.findById(id).commonName)
            organismObject.put("url", "#")
            organismObjectContainer.put(id, organismObject)
        }

        jsonObject.put("datasets", organismObjectContainer)

        if (jsonObject.include == null) jsonObject.put("include", new JSONArray())
        jsonObject.include.add("../plugins/WebApollo/json/annot.json")

        response.outputStream << jsonObject.toString()
        response.outputStream.close()
    }

    private static boolean isCacheableFile(String fileName) {
        if (fileName.endsWith(".txt") || fileName.endsWith("txtz")) return true;
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
}
