package org.bbop.apollo

import grails.transaction.Transactional

@Transactional
class NameService {

    def transcriptService

    // TODO: replace with more reasonable naming schemas
    String generateUniqueName() {
        UUID.randomUUID().toString()
    }

    String generateUniqueName(Feature thisFeature,String geneName = null ) {
        if(thisFeature.name) {
            if (thisFeature instanceof Transcript) {
                println "instance of transcript"
                if(!geneName){
                    Gene gene = transcriptService.getGene((Transcript) thisFeature)
                    println "transcript has gene ${gene}"
                    geneName = gene.name
                }

                Integer transcriptNumber = 1
                String transcriptName = geneName.trim() + "-" + transcriptNumber.toString().padLeft(5,"0")
                Transcript transcript = Transcript.findByName(transcriptName)

                while (transcript != null) {
                    ++transcriptNumber
                    transcriptName = geneName.trim() + "-" + transcriptNumber.toString().padLeft(5,"0")
                    transcript = Transcript.findByName(transcriptName)
                }
                return transcriptName
            } else
            if (thisFeature instanceof Gene) {
                println "instance of Gene"
                if(!geneName){
                    geneName = ((Gene) thisFeature).name
                }
                char transcriptLetter = 'a'
                String newGeneName = geneName.trim() + transcriptLetter
                Gene gene = Gene.findByName(newGeneName)
                while (gene != null) {
                    ++transcriptLetter
                    newGeneName = geneName.trim() + transcriptLetter
                    gene = Gene.findByName(newGeneName)
                }
                return newGeneName
            }
            else{
                println "using source string"
                String sourceString = thisFeature.name.replaceAll("[_\\.0-9]","")
                println "source string ${sourceString}"
                UUID.fromString(sourceString).toString()
            }
        }
        else{
            generateUniqueName()
        }
    }

//
//    String generateUniqueNameFromSource(Feature sourceFeature,Feature thisFeature) {
//        UUID.fromString(thisFeature.name.replaceFirst("\\W","")+"::"+sourceFeature.name.replaceFirst("\\W","")).toString()
//    }
}
