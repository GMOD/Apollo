package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.commons.io.FileUtils
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class ChadoHandlerService {

    def writeFeatures(Organism organism,Collection<Feature> features){

        JSONObject jsonObject = JSON.parse(organism.metadata)


        if(!jsonObject.containsKey("chado")){
            log.error("No chado database specified")
            return
        }
        JSONObject chado = jsonObject.chado


        // run a script

//        ['sh', '/home/path/to/script.sh', str].execute()

        String fileName = "chadoDump.sh"
        File chadoFile = new File("script-repo/${fileName}")
        // copy the export
        File tempDirectory = File.createTempDir()
        FileUtils.copyFile(chadoFile,tempDirectory)
        def processBuilder=new ProcessBuilder(["sh",fileName,chado.url,chado.username,chado.password,organism.id])
        processBuilder.redirectErrorStream(true)
        processBuilder.directory(tempDirectory)
        def process = processBuilder.start()
        String errorString = process.err.text
        String outputString = process.in.text
        println "error ${errorString}"
        println "output ${outputString}"
    }
}
