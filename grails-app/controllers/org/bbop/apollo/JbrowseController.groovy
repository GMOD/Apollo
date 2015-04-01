package org.bbop.apollo

import grails.converters.JSON
import org.apache.catalina.Session
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.messaging.handler.annotation.MessageMapping

import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

//@CompileStatic
class JbrowseController {

    def configWrapperService
    def brokerMessagingTemplate
    def sequenceService

//    def index() {
//
//        // this should serve the index.html wherever jbrowse is
//        File file = new File(".")
//        log.debug  file.absolutePath
//
//        File file = new File("")
//
//    }


//    @MessageMapping("/topic/TrackListReturn")
//    def allTracks(String input) {
//        println "GETTING ALL TRACKS 2 ${input}"
//        JSONObject inputObject = new JSONObject()
//        inputObject.command = "list"
//        println "returnString = ${returnString}"
//        render returnString as JSON
////            return "i[${inputString}]"
//    }

    // is typically checking for trackData.json
    def tracks(String jsonFile, String trackName, String groupName) {
        String filename = getJBrowseDirectoryForSession()
        filename += "/tracks/${trackName}/${groupName}/${jsonFile}.json"
        File file = new File(filename);
        if (!file.exists()) {
            log.error("Could not get tracks file " + filename);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        render file.text
    }

    private String getJBrowseDirectoryForSession() {
        // TODO: move to shiro
        HttpSession session = request.session
        String organismJBrowseDirectory = session.getAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value)
        if(!organismJBrowseDirectory ){
            for(Organism organism in Organism.all){
                // load if not
                if(!organism.sequences){
                    sequenceService.loadRefSeqs(organism)
                }

                if(organism.sequences){
                    Sequence sequence = organism?.sequences?.first()
                    organismJBrowseDirectory = organism.directory
                    session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value,organismJBrowseDirectory)
                    session.setAttribute(FeatureStringEnum.SEQUENCE_NAME.value,sequence.name)
                    session.setAttribute(FeatureStringEnum.ORGANISM_ID.value,sequence.organismId)
                    session.setAttribute(FeatureStringEnum.ORGANISM.value,sequence.organism.commonName)
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
            return;
        }
        render file.text
    }

//    /**
//     * For returning seq/refSeqs.json
//     */
//    def meta(){
//        log.debug  "meta"
//        String filename = grailsApplication.config.apollo.jbrowse.data.directory
//        File file = new File(filename+"/names/meta.json");
//        if(!file.exists()){
//            log.error("Could not get names/meta.json file " + filename);
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
//        String filename = grailsApplication.config.apollo.jbrowse.data.directory
        String filename = getJBrowseDirectoryForSession()
        File file = new File(filename + "/seq/refSeqs.json");
        if (!file.exists()) {
            log.error("Could not get seq file " + filename);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        render file.text
    }

    def seqMapper() {
        String filename = getJBrowseDirectoryForSession()
        File file = new File(filename + "/seq/${params.a}/${params.b}/${params.c}/${params.group}");
        if (!file.exists()) {
            log.error("Could not get seq file " + file.absolutePath);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        render file.text
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
     * data/bigwig/<filename>.bw
     *
     *
     */
    def data(String fileName) {
        log.debug "data"
//        String dataDirectory = grailsApplication.config.apollo.jbrowse.data.directory
        String dataDirectory = getJBrowseDirectoryForSession()
        log.debug "dataDir: ${dataDirectory}"

//        log.debug  "filename ${filename}"
        log.debug "URI: " + request.getRequestURI()
        log.debug "URL: " + request.getRequestURL()
        log.debug "pathInfo: " + request.getPathInfo()
        log.debug "pathTranslated: " + request.getPathTranslated()
        log.debug "params: " + params
//
//        int paramCount = 0
//        for (p in params) {
//            log.debug  "param: ${p}"
//            filename += paramCount == 0 ? "?" : "&"
//            ++paramCount
//
//            filename += "${p.key}=${p.value}"
//        }
//
//        log.debug  "filename ${filename}"

//        if(params.id=="trackList" &&  params.format=="json"){
//            filename += "/trackList.json"
//        }
//        else
//        if(params.id=="tracks" &&  params.format=="conf"){
//            filename += "/tracks.conf"
//        }
//        else
//        if(params.id=="root" &&  params.format=="conf"){
//            filename += "/root.json"
//        }

        String dataFileName = dataDirectory + "/" + fileName

        log.debug "data directory: ${dataFileName}"

        File file = new File(dataFileName);


        if (!file.exists()) {
            log.error("Could not get data directory: " + dataFileName);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Get the MIME type of the image
//        String mimeType = getServletContext().getMimeType(filename);
//        if (mimeType == null) {
//            log.error("Could not get MIME type of " + filename);
//            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//            return;
//        }


        if (params.format == "json") {
            // Set content type
//            response.setContentType("contentType: \"text/xml\"");
            response.setContentType("contentType: \"application/json\"");
        }

        // Set content size
        response.setContentLength((int) file.length());

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
    }


}
