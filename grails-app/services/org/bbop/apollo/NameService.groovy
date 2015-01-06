package org.bbop.apollo

import grails.transaction.Transactional

import java.text.NumberFormat

@Transactional
class NameService {

    def transcriptService

    // TODO: replace with more reasonable naming schemas
    String generateUniqueName() {
        UUID.randomUUID().toString()
    }

    String generateUniqueName(Feature thisFeature) {
        if(thisFeature.name) {
            if (thisFeature instanceof Transcript) {
                Gene gene = transcriptService.getGene((Transcript) thisFeature)
                String geneName = gene.name

                Integer transcriptNumber = 1
                String transcriptName = geneName + "-" + transcriptNumber.toString().padLeft(5)
                Transcript transcript = Transcript.findByName(transcriptName)

                while (transcript != null) {
                    ++transcriptNumber
                    transcriptName = geneName + "-" + transcriptNumber.toString().padLeft(5)
                    transcript = Transcript.findByName(transcriptName)
                }
                return transcriptName
            } else
            if (thisFeature instanceof Gene) {
                String geneName = ((Gene) thisFeature).name
                char transcriptLetter = 'a'
                String newGeneName = geneName + transcriptLetter
                Gene gene = Gene.findByName(newGeneName)
                while (gene != null) {
                    ++transcriptLetter
                    newGeneName = geneName + transcriptLetter
                    gene = Gene.findByName(newGeneName)
                }
                return newGeneName
            }
            else{
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
