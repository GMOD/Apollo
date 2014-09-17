package org.bbop.apollo

import javax.servlet.http.HttpServletResponse
//@CompileStatic
class JbrowseController {


//    def index() {
//
//        // this should serve the index.html wherever jbrowse is
//        File file = new File(".")
//        println file.absolutePath
//
//        File file = new File("")
//
//    }

    // is typically checking for trackData.json
    def tracks(String jsonFile,String trackName,String groupName){
        String filename = grailsApplication.config.apollo.jbrowse.data.directory
        filename += "/tracks/${trackName}/${groupName}/${jsonFile}.json"
        File file = new File(filename);
        if(!file.exists()){
            log.error("Could not get find file " + filename);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        render file.text
    }

    /**
     * For returning seq/refSeqs.json
     */
    def seq(){
        println "seq"
        String filename = grailsApplication.config.apollo.jbrowse.data.directory
        File file = new File(filename+"/seq/refSeqs.json");
        if(!file.exists()){
            log.error("Could not get find file " + filename);
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
    def data() {
        println "data"
        String dataDirectory = grailsApplication.config.apollo.jbrowse.data.directory
//        println "dataDir: ${dataDirectory}"

        String filename = dataDirectory
//        println "filename ${filename}"
        println "URI: " + request.getRequestURI()
        println "URL: " + request.getRequestURL()
        println "pathInfo: " + request.getPathInfo()
        println "pathTranslated: " + request.getPathTranslated()
        println "params: " + params
//
//        int paramCount = 0
//        for (p in params) {
//            println "param: ${p}"
//            filename += paramCount == 0 ? "?" : "&"
//            ++paramCount
//
//            filename += "${p.key}=${p.value}"
//        }
//
//        println "filename ${filename}"

        if(params.id=="trackList" &&  params.format=="json"){
            filename += "/trackList.json"
        }
        else
        if(params.id=="tracks" &&  params.format=="conf"){
            filename += "/tracks.conf"
        }
        else
        if(params.id=="root" &&  params.format=="conf"){
            filename += "/root.json"
        }

        println "filename: ${filename}"

        File file = new File(filename);


        if(!file.exists() || !file.isFile()){
            log.error("Could not get find file " + filename);
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


        if(params.format=="json"){
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
