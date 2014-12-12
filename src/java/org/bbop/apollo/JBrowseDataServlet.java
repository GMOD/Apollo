package org.bbop.apollo;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;


//@WebServlet(urlPatterns = "/jbrowse/data/*", name = "JBrowseData")
public class JBrowseDataServlet extends HttpServlet {

    private static final int DEFAULT_BUFFER_SIZE = 10240; // ..bytes = 10KB.

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // TODO: move up so not recalculating each time
        String pathTranslated = request.getPathTranslated();
        String rootPath = pathTranslated.substring(0, pathTranslated.length() - request.getPathInfo().length());
        String configPath = rootPath + "/config/config.properties";


        File propertyFile = new File(configPath);
        String filename = null;

        if (propertyFile.exists()) {
            Properties properties = new Properties();
            FileInputStream fileInputStream = new FileInputStream(propertyFile);
            properties.load(fileInputStream);

            filename = properties.getProperty("jbrowse.data") + request.getPathInfo();
            File dataFile = new File(filename);
            if (!dataFile.exists() || !dataFile.canRead()) {
                logger.debug("NOT found: " + filename);
                filename = null;
            }
        }

        if (filename == null) {
            filename = rootPath + request.getServletPath() + request.getPathInfo();
            File testFile = new File(filename);
            if (FileUtils.isSymlink(testFile)) {
                filename = testFile.getAbsolutePath();
                logger.debug("symlink found so adjusting to absolute path: " + filename);
            }
        }


        // Get the absolute path of the file
        ServletContext sc = getServletContext();

        File file = new File(filename);
        if (!file.exists()) {
            logger.warn("Could not get MIME type of " + filename);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;

        }


        // Get the MIME type of the image
        String mimeType = sc.getMimeType(filename);
        if (mimeType == null) {
            if (filename.endsWith(".bam")
                    || filename.endsWith(".bw")
                    || filename.endsWith(".bai")
                    || filename.endsWith(".conf")
                    ) {
                mimeType = "text/plain";
            } else if (filename.endsWith(".tbi")) {
                mimeType = "application/x-gzip";
            } else {
                logger.error("Could not get MIME type of " + filename + " falling back to text/plain");
                mimeType = "text/plain";
//                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//                return;
            }
        }

        if(isCacheableFile(filename)){
            String eTag = createHashFromFile(file);
            String dateString = formatLastModifiedDate(file);

            response.setHeader("ETag",eTag);
            response.setHeader("Last-Modified",dateString );
        }

        String range = request.getHeader("range");
//        logger.debug("range: " + range);

        long length = file.length();
        Range full = new Range(0, length - 1, length);

        List<Range> ranges = new ArrayList<Range>();

        // from http://balusc.blogspot.com/2009/02/fileservlet-supporting-resume-and.html#sublong
        if (range != null) {

            // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
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

        if (ranges.isEmpty() || ranges.get(0) == full) {
            // Set content type

            // Set content size
            response.setContentLength((int) file.length());

            // Open the file and output streams
            FileInputStream in = new FileInputStream(file);
            OutputStream out = response.getOutputStream();

            // Copy the contents of the file to the output stream
            byte[] buf = new byte[1024];
            int count = 0;
            while ((count = in.read(buf)) >= 0) {
                out.write(buf, 0, count);
            }
            in.close();
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

    private boolean isCacheableFile(String filename) {
        if(filename.endsWith(".txt")) return true ;
        if(filename.endsWith(".json")){
            String[] names = filename.split("\\/");
            String requestName = names[names.length-1];
            return requestName.startsWith("lf-");
        }

        return false ;
    }

    private String formatLastModifiedDate(File file) {
        DateFormat simpleDateFormat = SimpleDateFormat.getDateInstance();
        return simpleDateFormat.format(new Date(file.lastModified()));
    }

    private String createHashFromFile(File file) {
        String fileName = file.getName();
        long length = file.length();
        long lastModified = file.lastModified();
        return fileName + "_" + length + "_" + lastModified;
    }

    /**
     * Copy the given byte range of the given input to the given output.
     *
     * @param input  The input to copy the given range to the given output for.
     * @param output The output to copy the given range from the given input for.
     * @param start  Start of the byte range.
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
     * @param value      The string value to return a substring as long for.
     * @param beginIndex The begin index of the substring to be returned as long.
     * @param endIndex   The end index of the substring to be returned as long.
     * @return A substring of the given string value as long or -1 if substring is empty.
     */
    private static long sublong(String value, int beginIndex, int endIndex) {
        String substring = value.substring(beginIndex, endIndex);
        return (substring.length() > 0) ? Long.parseLong(substring) : -1;
    }

    /**
     * This class represents a byte range.
     */
    protected class Range {
        long start;
        long end;
        long length;
        long total;

        /**
         * Construct a byte range.
         *
         * @param start Start of the byte range.
         * @param end   End of the byte range.
         * @param total Total length of the byte source.
         */
        public Range(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }

    }


}
