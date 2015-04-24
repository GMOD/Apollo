package org.bbop.apollo

import org.bbop.apollo.gwt.shared.FeatureStringEnum

import grails.transaction.Transactional

@Transactional
class NameService {

    def transcriptService

    // TODO: replace with more reasonable naming schemas
    String generateUniqueName() {
        UUID.randomUUID().toString()
    }

    String generateUniqueName(Feature thisFeature,String principalName = null ) {
        if(thisFeature.name) {
            if (thisFeature instanceof Transcript) {
                log.debug "instance of transcript"
                if(!principalName){
                    Gene gene = transcriptService.getGene((Transcript) thisFeature)
                    log.debug "transcript has gene ${gene}"
                    if(!gene){
                        gene = transcriptService.getPseudogene((Transcript) thisFeature)
                        log.debug "transcript has pseudogene ${gene}"
                    }
                    principalName = gene.name
                }

                Integer transcriptNumber = 1
                String transcriptName = principalName.trim() + "-" + transcriptNumber.toString().padLeft(5,"0")
                Transcript transcript = Transcript.findByName(transcriptName)

                while (transcript != null) {
                    transcriptName = principalName.trim() + "-" + transcriptNumber.toString().padLeft(5,"0")
                    transcript = Transcript.findByName(transcriptName)
                    ++transcriptNumber
                }
                return transcriptName
            } else
            if (thisFeature instanceof Gene) {
                log.debug "instance of Gene"
                if(!principalName){
                    principalName = ((Gene) thisFeature).name
                }
                if(Gene.countByName(principalName.trim())==0){
                    return principalName
                }
                char transcriptLetter = 'a'
                String newGeneName = principalName.trim() + transcriptLetter
                Gene gene = Gene.findByName(newGeneName)
                while (gene != null) {
                    newGeneName = principalName.trim() + transcriptLetter
                    gene = Gene.findByName(newGeneName)
                    ++transcriptLetter
                }
                return newGeneName
            }
            if (thisFeature instanceof Exon) {
                log.debug "instance of Exon"
                if(!principalName){
                    principalName = ((Exon) thisFeature).name
                }
                Integer exonNumber = 1
                String exonName = principalName.trim() + "-" + exonNumber.toString().padLeft(5,"0")
                Exon exon = Exon.findByName(exonName)
                while (exon != null) {
                    exonName = principalName.trim() + "-" + exonNumber.toString().padLeft(5,"0")
                    exon = Exon.findByName(exonName)
                    ++exonNumber
                }
                return exonName
            }
            else{
                log.debug "using source string"
                String sourceString = thisFeature.name.replaceAll("[_\\.0-9]","")
                log.debug "source string ${sourceString}"
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
    String generateUniqueGeneName(String newGeneName) {
        String originalName  = newGeneName
        Gene gene = Gene.findByName(originalName)
        char transcriptLetter = 'a'
        while (gene != null) {
            newGeneName = originalName.trim() + transcriptLetter
            gene = Gene.findByName(newGeneName)
            ++transcriptLetter
        }
        return newGeneName
    }
}
