package org.bbop.apollo

import grails.transaction.Transactional

@Transactional(readOnly = true)
class NameService {

    def transcriptService
    def letterPaddingStrategy = new LetterPaddingStrategy()
    def leftPaddingStrategy = new LeftPaddingStrategy()


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
                return makeUniqueTranscriptName(organism,principalName.trim()+"-")
            } else
            if (thisFeature instanceof Gene) {
                log.debug "instance of Gene"
                if(!principalName){
                    principalName = ((Gene) thisFeature).name
                }
                if(Gene.countByName(principalName.trim())==0){
                    return principalName
                }
                  return makeUniqueGeneName(organism,principalName.trim())
            }
            if (thisFeature instanceof Exon || thisFeature instanceof NonCanonicalFivePrimeSpliceSite || thisFeature instanceof NonCanonicalThreePrimeSpliceSite || thisFeature instanceof CDS) {
                log.debug "instance of Exon"
                return generateUniqueName()
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


    boolean isUniqueGene(Organism organism,String name){
//        if(Gene.countByName(name)==0) {
//            return true
//        }
//        List results = (Gene.executeQuery("select count(f) from Gene f join f.featureLocations fl join fl.sequence s where s.organism = :org and f.name = :name ",[org:organism,name:name]))
        Integer numberResults = Gene.findAllByName(name).findAll(){
            it.featureLocation.sequence.organism == organism
        }.size()
        return 0 == numberResults
    }

    boolean isUnique(Organism organism,String name){
//        if(Feature.countByName(name)==0) {
//            return true
//        }
//        List results = (Feature.executeQuery("select count(f) from Feature f join f.featureLocations fl join fl.sequence s where s.organism = :org and f.name = :name ",[org:organism,name:name]))
        Integer numberResults = Feature.findAllByName(name).findAll(){
            it.featureLocation.sequence.organism == organism
        }.size()
        return 0 == numberResults
    }

    String makeUniqueTranscriptName(Organism organism,String principalName){
        String name

        name = principalName + leftPaddingStrategy.pad(0)
        if(Transcript.countByName(name)==0){
            return name
        }

//        List results = (Feature.executeQuery("select f.name from Transcript f join f.featureLocations fl join fl.sequence s where s.organism = :org and f.name like :name ",[org:organism,name:principalName+'%']))
        // See https://github.com/GMOD/Apollo/issues/1276
        // only does sort over found results
        List<String> results= Feature.findAllByNameLike(principalName+"%").findAll(){
            it.featureLocation.sequence.organism == organism
        }.name

        name = principalName + leftPaddingStrategy.pad(results.size())
        int count = results.size()
        while(results.contains(name)){
            name = principalName + leftPaddingStrategy.pad(count)
            ++count
        }
        return name
    }

    String makeUniqueGeneName(Organism organism,String principalName,boolean useOriginal=false){

        if(useOriginal && isUniqueGene(organism,principalName)){
            return principalName
        }

        if(isUniqueGene(organism,principalName)){
            return principalName
        }

        String name = principalName + letterPaddingStrategy.pad(0)

//        List results = (Gene.executeQuery("select f.name from Gene f join f.featureLocations fl join fl.sequence s where s.organism = :org and f.name like :name ",[org:organism,name:principalName+'%']))
        List<String> results= Gene.findAllByNameLike(principalName+"%").findAll(){
            it.featureLocation.sequence.organism == organism
        }.name
        int count = results.size()
        while(results.contains(name)){
            name = principalName + letterPaddingStrategy.pad(count)
            ++count
        }
        return name

//        name = principalName + letterPaddingStrategy.pad(i++)
//        while(!isUnique(organism,name)){
//            name = principalName + letterPaddingStrategy.pad(i++)
//        }
//        return name
    }

    String makeUniqueFeatureName(Organism organism,String principalName,PaddingStrategy paddingStrategy,boolean useOriginal=false){
        String name
        int i = 0

        if(useOriginal && isUnique(organism,principalName)){
            return principalName
        }

        if(isUnique(organism,principalName)){
            return principalName
        }

        name = principalName + paddingStrategy.pad(i++)
        while(!isUnique(organism,name)){
            name = principalName + paddingStrategy.pad(i++)
        }
        return name
    }

    /**
     * Generates name for a given variant based on its properties
     * @param variant
     * @return
     */
    String makeUniqueVariantName(SequenceAlteration variant) {
        String name
        String position = variant.featureLocation.fmin + 1
        def alternateAlleles = []
        String referenceAllele
        for (Allele allele : variant.alleles.sort { a,b -> a.id <=> b.id }) {
            if (allele.reference) {
                referenceAllele = allele.bases
            }
            else {
                alternateAlleles.add(allele.bases)
            }
        }
        name = position + " " + referenceAllele + " > " + alternateAlleles.join(",")
        log.info "Name for variant: ${name}"
        return name
    }

}
