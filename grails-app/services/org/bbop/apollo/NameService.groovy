package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray

@Transactional(readOnly = true)
class NameService {

    def transcriptService
    def letterPaddingStrategy = new LetterPaddingStrategy()
    def leftPaddingStrategy = new LeftPaddingStrategy()

    // TODO: replace with more reasonable naming schemas
    String generateUniqueName() {
        UUID.randomUUID().toString()
    }

    private String convertPrincipalToMultiSequence(String principalName) {
        if (principalName && principalName.startsWith("[")) {
            Integer lastIndex = principalName.lastIndexOf("]")
            String lastString = principalName.substring(0, lastIndex + 1)
            JSONArray array = JSON.parse(lastString) as JSONArray
            List strings = []
            for (int i = 0; i < array.size(); i++) {
                strings << array.getJSONObject(i).getString(FeatureStringEnum.NAME.value)
            }
            principalName = strings.join("-") + principalName.substring(lastIndex + 1)
        }
        return principalName
    }

    String generateUniqueName(Feature thisFeature, String principalName = null) {
        Organism organism = thisFeature.organism
        if (thisFeature.name) {
            if (thisFeature instanceof Transcript) {
                log.debug "instance of transcript"
                if (!principalName) {
                    Gene gene = transcriptService.getGene((Transcript) thisFeature)
                    log.debug "transcript has gene ${gene}"
                    if (!gene) {
                        gene = transcriptService.getPseudogene((Transcript) thisFeature)
                        log.debug "transcript has pseudogene ${gene}"
                    }
                    principalName = gene.name
                }
                principalName = convertPrincipalToMultiSequence(principalName)
                return makeUniqueTranscriptName(organism, principalName.trim() + "-")
            } else if (thisFeature instanceof Gene) {
                log.debug "instance of Gene"
                if (!principalName) {
                    principalName = ((Gene) thisFeature).name
                }
                if (Gene.countByName(principalName.trim()) == 0) {
                    return principalName
                }
                principalName = convertPrincipalToMultiSequence(principalName)
                return makeUniqueGeneName(organism, principalName.trim())
//                return makeUniqueFeatureName(organism,principalName.trim(),new LetterPaddingStrategy())
            }
            if (thisFeature instanceof Exon || thisFeature instanceof NonCanonicalFivePrimeSpliceSite || thisFeature instanceof NonCanonicalThreePrimeSpliceSite || thisFeature instanceof CDS) {
                log.debug "instance of Exon"
                if (!principalName) {
                    principalName = ((Exon) thisFeature).name
                }
//                return makeUniqueFeatureName(organism,principalName.trim(),new LeftPaddingStrategy())
                return generateUniqueName()
            } else {
                if (!principalName) {
                    principalName = thisFeature.name
                }
                return makeUniqueFeatureName(organism, principalName.trim(), new LetterPaddingStrategy())
            }
        } else {
            generateUniqueName()
        }
    }


    boolean isUniqueGene(Organism organism, String name) {
        if (Gene.countByName(name) == 0) {
            return true
        }
        List results = (Gene.executeQuery("select count(f) from Gene f join f.featureLocations fl join fl.sequence s join s.organism org where org = :org and f.name = :name ", [org: organism, name: name]))
        return 0 == (int) results.get(0)
    }

    boolean isUnique(Organism organism, String name) {
        if (Feature.countByName(name) == 0) {
            return true
        }
        List results = (Feature.executeQuery("select count(f) from Feature f join f.featureLocations fl join fl.sequence s join s.organism org where org = :org and f.name = :name ", [org: organism, name: name]))
        return 0 == (int) results.get(0)
    }

    String makeUniqueTranscriptName(Organism organism, String principalName) {
        String name

        name = principalName + leftPaddingStrategy.pad(0)
        if (Transcript.countByName(name) == 0) {
            return name
        }
        List results = (Feature.executeQuery("select f.name from Transcript f join f.featureLocations fl join fl.sequence s join s.organism org where org = :org and f.name like :name ", [org: organism, name: principalName + '%']))

        name = principalName + leftPaddingStrategy.pad(results.size())
        int count = results.size()
        while (results.contains(name)) {
            name = principalName + leftPaddingStrategy.pad(count)
            ++count
        }
        return name
    }

    String makeUniqueGeneName(Organism organism, String principalName, boolean useOriginal = false) {

        principalName = convertPrincipalToMultiSequence(principalName)
        if (useOriginal && isUniqueGene(organism, principalName)) {
            return principalName
        }

        if (isUniqueGene(organism, principalName)) {
            return principalName
        }

        String name = principalName + letterPaddingStrategy.pad(0)

        List results = (Gene.executeQuery("select f.name from Gene f join f.featureLocations fl join fl.sequence s join s.organism org where org = :org and f.name like :name ", [org: organism, name: principalName + '%']))
        int count = results.size()
        while (results.contains(name)) {
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

    String makeUniqueFeatureName(Organism organism, String principalName, PaddingStrategy paddingStrategy, boolean useOriginal = false) {
        String name
        int i = 0

        if (useOriginal && isUnique(organism, principalName)) {
            return principalName
        }

        if (isUnique(organism, principalName)) {
            return principalName
        }

        name = principalName + paddingStrategy.pad(i++)
        while (!isUnique(organism, name)) {
            name = principalName + paddingStrategy.pad(i++)
        }
        return name
    }

}
