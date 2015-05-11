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
        Organism organism = thisFeature.featureLocation.sequence.organism
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
                return makeUniqueFeatureName(organism,principalName.trim()+"-",new LeftPaddingStrategy())

//                Integer transcriptNumber = 1
//                String transcriptName = principalName.trim() + "-" + transcriptNumber.toString().padLeft(5,"0")
//                Transcript transcript = Transcript.findByName(transcriptName)
//
//                while (transcript != null) {
//                    transcriptName = principalName.trim() + "-" + transcriptNumber.toString().padLeft(5,"0")
//                    transcript = Transcript.findByName(transcriptName)
//                    ++transcriptNumber
//                }
//                return transcriptName
            } else
            if (thisFeature instanceof Gene) {
                log.debug "instance of Gene"
                if(!principalName){
                    principalName = ((Gene) thisFeature).name
                }
                if(Gene.countByName(principalName.trim())==0){
                    return principalName
                }
                return makeUniqueFeatureName(organism,principalName.trim(),new LetterPaddingStrategy())
//                char transcriptLetter = 'a'
//                String newGeneName = principalName.trim() + transcriptLetter
//                Gene gene = Gene.findByName(newGeneName)
//                while (gene != null) {
//                    newGeneName = principalName.trim() + transcriptLetter
//                    gene = Gene.findByName(newGeneName)
//                    ++transcriptLetter
//                }
//                return newGeneName
            }
            if (thisFeature instanceof Exon) {
                log.debug "instance of Exon"
                if(!principalName){
                    principalName = ((Exon) thisFeature).name
                }
                return makeUniqueFeatureName(organism,principalName.trim(),new LeftPaddingStrategy())
//                Integer exonNumber = 1
//                String exonName = principalName.trim() + "-" + exonNumber.toString().padLeft(5,"0")
//                Exon exon = Exon.findByName(exonName)
//                while (exon != null) {
//                    exonName = principalName.trim() + "-" + exonNumber.toString().padLeft(5,"0")
//                    exon = Exon.findByName(exonName)
//                    ++exonNumber
//                }
//                return exonName
            }
            else{
                if(!principalName){
                    principalName = thisFeature.name
                }
                return makeUniqueFeatureName(organism,principalName.trim(),new LeftPaddingStrategy())
//                Integer exonNumber = 1
//                String exonName = principalName.trim() + "-" + exonNumber.toString().padLeft(5,"0")
//                Feature exon = Feature.findByName(exonName)
//                while (exon != null) {
//                    exonName = principalName.trim() + "-" + exonNumber.toString().padLeft(5,"0")
//                    exon = Feature.findByName(exonName)
//                    ++exonNumber
//                }
//                return exonName
//                log.debug "using source string"
//                String sourceString = thisFeature.name.replaceAll("[_\\.0-9]","")
//                log.debug "source string ${sourceString}"
//                UUID.fromString(sourceString).toString()
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
    /**
//     * @deprecated . . . TODO: should use generateUniqueName and "instanceof Gene
     * @param newGeneName
     * @return
     */
    String generateUniqueGeneName(Organism organism,String newGeneName) {
        return makeUniqueFeatureName(organism,newGeneName,new LetterPaddingStrategy())
//        if(!isUnique(organism,newGeneName)){
//
//        }
//        return newGeneName
//        String originalName  = newGeneName
//        Gene gene = Gene.findByName(originalName)
//        char transcriptLetter = 'a'
//        while (gene != null) {
//            newGeneName = originalName.trim() + transcriptLetter
//            gene = Gene.findByName(newGeneName)
//            ++transcriptLetter
//        }
//        return newGeneName
    }

    boolean isUnique(Organism organism,String name){
        List results = (Feature.executeQuery("select count(f) from Feature f join f.featureLocations fl join fl.sequence s join s.organism org where org = :org and f.name = :name ",[org:organism,name:name]))
        return 0 == (int) results.get(0)
    }

    String makeUniqueFeatureName(Organism organism,String principalName,PaddingStrategy paddingStrategy){
        String name
        int i = 1
        name = principalName + paddingStrategy.pad(i++)
        while(!isUnique(organism,name)){
            name = principalName + paddingStrategy.pad(i++)
        }
        return name
    }

}
