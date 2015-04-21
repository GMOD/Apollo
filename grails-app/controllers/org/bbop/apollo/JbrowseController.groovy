package org.bbop.apollo

import org.apache.shiro.session.Session
import org.apache.shiro.SecurityUtils
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.sequence.Range

import javax.servlet.http.HttpServletResponse
import java.text.DateFormat
import java.text.SimpleDateFormat
import static org.springframework.http.HttpStatus.*

//@CompileStatic
class JbrowseController {

    private static final int DEFAULT_BUFFER_SIZE = 10240; // ..bytes = 10KB.

    def sequenceService
    def permissionService


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
        // TODO: move to shiro
        Session session = SecurityUtils.subject.getSession(false)
        String organismJBrowseDirectory = session.getAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value)
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
                        println "creating a new one!"
                        userOrganismPreference = new UserOrganismPreference(
                                user: user
                                ,organism: organism
                                ,sequence: sequence
                                ,currentOrganism: true
                        ).save(insert:true,flush:true)
                    }
                    else{
                        println "updating an old one!!"
                        userOrganismPreference.sequence = sequence
                        userOrganismPreference.currentOrganism = true
                        userOrganismPreference.save()
                    }

                    println "222 - has a current organism ${UserOrganismPreference.countByCurrentOrganism(true)}"

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
 * For returning seq/refSeqs.json
 */
    def namesFiles(String directory, String jsonFile) {
//        String dataDirectory = grailsApplication.config.apollo.jbrowse.data.directory
        String dataDirectory = getJBrowseDirectoryForSession()
        String absoluteFilePath = dataDirectory + "/names/${directory}/${jsonFile}.json"
        log.debug "names Files ${directory} ${jsonFile}  ${absoluteFilePath}"
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
//        String dataDirectory = grailsApplication.config.apollo.jbrowse.data.directory
        String dataDirectory = getJBrowseDirectoryForSession()
        String absoluteFilePath = dataDirectory + "/names/${fileName}.json"
        println "names ${fileName}  ${absoluteFilePath}"
        File file = new File(absoluteFilePath);
        if (!file.exists()) {
            log.warn("Could not get ${absoluteFilePath}");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            render status: NOT_FOUND
            return;
        }
        render file.text
    }

//    /**
//     * For returning seq/refSeqs.json
//     */
//    def meta(){
//        log.debug  "meta"
//        String fileName = grailsApplication.config.apollo.jbrowse.data.directory
//        File file = new File(fileName+"/names/meta.json");
//        if(!file.exists()){
//            log.error("Could not get names/meta.json file " + fileName);
//            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//            return;
//        }
//        render file.text
//    }

    /**
     * For returning seq/refSeqs.json
     */
    def seq() {
        log.debug "seq"
//        String fileName = grailsApplication.config.apollo.jbrowse.data.directory
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
        println "bam! ${fileName}"
        return data("bam/" + fileName)
    }

    /**
     * Has to handle a number of routes based on selected genome or just use the default otherwise.
     *
     * trackList.json
     * tracks.conf . . .  should be a .json
     * names/meta.json
     * . .  pass-through for css . . . good to change
     * refSeq.json  (good to store in database)
     * 7.json  ??
     *
     * .. .  and original:
     //     * data/tracks/Hsal_OGSv3.3/Group1.1/trackData.json
     * data/tracks/<track>/<annotation>/trackData.json
     *
     * data/tracks/Amel_4.5_brain_ovary.gff/Group1.1/lf-1.json  ?? a GFF to json manipulation
     *
     * data/bigwig/<fileName>.bw
     *
     *
     */
    def data(String fileName) {
        log.debug "data"
        String dataDirectory = getJBrowseDirectoryForSession()
        log.debug "dataDir: ${dataDirectory}"

//        log.debug  "fileName ${fileName}"
        log.debug "URI: " + request.getRequestURI()
        log.debug "URL: " + request.getRequestURL()
        log.debug "pathInfo: " + request.getPathInfo()
        log.debug "pathTranslated: " + request.getPathTranslated()
        log.debug "params: " + params

        String dataFileName = dataDirectory + "/" + fileName

        log.debug "data directory: ${dataFileName}"

        File file = new File(dataFileName);

        println "file path abs ${file.absolutePath}"

        if (!file.exists()) {
            log.error("Could not get data directory: " + dataFileName);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }



        String mimeType = getServletContext().getMimeType(fileName);
        if (!mimeType) {
            log.info("No input MIME type of " + fileName);
//                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//                return;
            if (fileName.endsWith(".json") || params.format == "json") {
                mimeType = "application/json";
                response.setContentType(mimeType);

                // Open the file and output streams
                FileInputStream fis = new FileInputStream(file);
                OutputStream out = response.getOutputStream();

                // Copy the contents of the file to the output stream
                byte[] buf = new byte[1024];
                int count = 0;
                while ((count = fis.read(buf)) >= 0) {
                    out.write(buf, 0, count);
                }
                fis.close();
                out.close();
                return
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
//                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//                return;
            }
        }
        println "mimeType ${mimeType}"

        if (isCacheableFile(fileName)) {
            String eTag = createHashFromFile(file);
            String dateString = formatLastModifiedDate(file);

            response.setHeader("ETag", eTag);
            response.setHeader("Last-Modified", dateString);
        }

        String range = request.getHeader("range");
//        logger.debug("range: " + range);

        long length = file.length();
        Range full = new Range(0, length - 1, length);

        List<Range> ranges = new ArrayList<Range>();

        // from http://balusc.blogspot.com/2009/02/fileservlet-supporting-resume-and.html#sublong
        if (range != null) {

            // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*\$")) {
                response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
//                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
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
//                            response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                            return;
                        }

                        // Add range.
                        ranges.add(new Range(start, end, length));
                    }
                }
            }

        }

        response.setContentType(mimeType);
//        }

        // Set content size
//        response.setContentLength((int) file.length());

        if (ranges.isEmpty() || ranges.get(0) == full) {
            // Set content type

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
//            response.setContentType(contentType);
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
