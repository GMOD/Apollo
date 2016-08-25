package org.bbop.apollo

import grails.transaction.NotTransactional
import grails.transaction.Transactional

import javax.servlet.http.HttpServletResponse
import java.text.SimpleDateFormat

@Transactional(readOnly = true)
class SequenceCacheService {

    def generateCacheTags(HttpServletResponse response, SequenceCache cache, String path, long bytesLength) {
        Date lastModifiedDate = cache.lastUpdated
        String dateString = SimpleDateFormat.getDateInstance().format(lastModifiedDate)
        String eTag = createHash(path, bytesLength, (long) lastModifiedDate.time);
        response.setHeader("ETag", eTag);
        response.setHeader("Last-Modified", dateString);
    }

    def cacheFile(HttpServletResponse response,File file) {
        String eTag = createHashFromFile(file);
        String dateString = formatLastModifiedDate(file);
        response.setHeader("ETag", eTag);
        response.setHeader("Last-Modified", dateString);
    }

    @NotTransactional
    String createHash(String name, long length, long lastModified) {
        return name + "_" + length + "_" + lastModified;
    }

    @NotTransactional
    String createHashFromFile(File file) {
        String fileName = file.getName();
        long length = file.length();
        long lastModified = file.lastModified();
        return createHash(fileName, length, lastModified)
    }

    /**
     * We choose a date to use for last modified
     * @param files
     * @return
     */
    @NotTransactional
    String formatLastModifiedDate(File... files) {
        Date earliestDate = getLastModifiedDate(files)
        return SimpleDateFormat.getDateInstance().format(earliestDate)
    }

    /**
     * We choose a date to use for last modified
     * @param files
     * @return
     */
    @NotTransactional
    Date getLastModifiedDate(File... files) {
        Date earliestDate = new Date()
        for (File file : files) {
            Date lastModifiedDate = new Date(file.lastModified())
            if (lastModifiedDate.before(earliestDate)) {
                earliestDate = lastModifiedDate
            }
        }
        return earliestDate
    }
}
