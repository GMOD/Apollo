package org.bbop.apollo

import grails.transaction.Transactional

@Transactional
class ConfigurableFeatureService {

    /**
     * TODO: integrate with cvTermTranscriptList
     * @return
     */
    List<String> getTranscriptCvTerms() {
        List<String> returnList = new ArrayList<>()
        CustomTranscript.all.each {
            returnList.add(it.getCvTerm())
            if(it.getAlternateCvTerm()){
                returnList.add(it.getAlternateCvTerm())
            }
        }
        return returnList
    }

    /**
     * TODO: integrate into FS::generateFeatureForType
     * TODO: integrate into FS::convertJSONToOntologyId
     * @param cvTerm
     * @return
     */
    String getOntologyIdForCvTerm(String cvTerm){
        CustomDomainMapping customDomainMapping = CustomDomainMapping.findByCvTerm(cvTerm)
        if(customDomainMapping) return customDomainMapping.ontologyId
        return CustomDomainMapping.findByAlternateCvTerm(cvTerm)?.ontologyId
    }

    /**
     * TODO: FS::generateFeatureForType
     * TODO: FS::getCvTermFromFeature
     * @param cvTerm
     * @return
     */
    Feature generateFeatureForOntologyId(String ontologyId){
        CustomDomainMapping customDomainMapping = CustomDomainMapping.findByOntologyId(ontologyId)

        if(customDomainMapping.isTranscript){
            CustomTranscript genericTranscript = new CustomTranscript()
            genericTranscript.setCvTerm(customDomainMapping.cvTerm)
            genericTranscript.setAlternateCvTerm(customDomainMapping.alternateCvTerm)
            genericTranscript.setOntologyId(customDomainMapping.ontologyId)
            // set anything else?
            return genericTranscript
        }
        else{
            CustomFeature genericFeature = new CustomFeature()
            genericFeature.setCvTerm(customDomainMapping.cvTerm)
            genericFeature.setAlternateCvTerm(customDomainMapping.alternateCvTerm)
            genericFeature.setOntologyId(customDomainMapping.ontologyId)
            // set anything else?
            return genericFeature
        }
    }
}
