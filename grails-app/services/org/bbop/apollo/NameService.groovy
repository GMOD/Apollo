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
                return makeUniqueFeatureNameForTranscript(organism,principalName.trim()+"-")
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
            }
            if (thisFeature instanceof Exon) {
                log.debug "instance of Exon"
                if(!principalName){
                    principalName = ((Exon) thisFeature).name
                }
                return makeUniqueFeatureName(organism,principalName.trim(),new LeftPaddingStrategy())
            }
            else{
                if(!principalName){
                    principalName = thisFeature.name
                }
                return makeUniqueFeatureName(organism,principalName.trim(),new LetterPaddingStrategy())
            }
        }
        else{
            generateUniqueName()
        }
    }


    boolean isUnique(Organism organism,String name){
        List results = (Feature.executeQuery("select count(f) from Feature f join f.featureLocations fl join fl.sequence s join s.organism org where org = :org and f.name = :name ",[org:organism,name:name]))
        return 0 == (int) results.get(0)
    }

    String makeUniqueFeatureNameForTranscript(Organism organism,String principalName){
        String name
        int i = 0
        // 5
        LeftPaddingStrategy paddingStrategy = new LeftPaddingStrategy()
        name = principalName
        List results = (Feature.executeQuery("select f.name from Feature f join f.featureLocations fl join fl.sequence s join s.organism org where org = :org and f.name like :name order by f.name desc",[org:organism,name:name+'%']))

        name = principalName + paddingStrategy.pad(results.size())
        int count = results.size()
        while(results.contains(name)){
            name = principalName + paddingStrategy.pad(count)
            ++count
        }
        return name
    }

    String makeUniqueFeatureName(Organism organism,String principalName,PaddingStrategy paddingStrategy,boolean useOriginal=false){
        String name
        int i = 0

        if(useOriginal && isUnique(organism,principalName)){
            return principalName
        }

        name = principalName + paddingStrategy.pad(i++)
        while(!isUnique(organism,name)){
            name = principalName + paddingStrategy.pad(i++)
        }
        return name
    }

}
