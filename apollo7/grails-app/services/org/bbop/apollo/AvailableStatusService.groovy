package org.bbop.apollo

import grails.gorm.transactions.Transactional
import org.grails.web.json.JSONArray

@Transactional
class AvailableStatusService {

    JSONArray getAvailableStatuses(Organism organism, List<FeatureType> featureTypeList) {
        JSONArray availableStatusesJSONArray = new JSONArray();
        // add all CC with no type and organism
        List<AvailableStatus> availableStatusList = new ArrayList<>()
        if (featureTypeList) {
            availableStatusList.addAll(AvailableStatus.executeQuery("select cc from AvailableStatus cc join cc.featureTypes ft where ft in (:featureTypeList)", [featureTypeList: featureTypeList]))
        }
        availableStatusList.addAll(AvailableStatus.executeQuery("select cc from AvailableStatus cc where cc.featureTypes is empty"))

        // if there are organism filters for these canned comments for this organism, then apply them
        List<AvailableStatusOrganismFilter> availableStatusOrganismFilters = AvailableStatusOrganismFilter.findAllByAvailableStatusInList(availableStatusList)
        if (availableStatusOrganismFilters) {
            // if the organism is in the list, that is good
            AvailableStatusOrganismFilter.findAllByOrganismAndAvailableStatusInList(organism, availableStatusList).each {
                availableStatusesJSONArray.put(it.availableStatus.value)
            }
            // we have to add anything from availableStatusList that isn't in another one
            List<AvailableStatus> availableStatusesToExclude = AvailableStatusOrganismFilter.findAllByOrganismNotEqualAndAvailableStatusInList(organism, availableStatusList).availableStatus
            for(AvailableStatus availableStatus in availableStatusList){
                if(!availableStatusesJSONArray.contains(availableStatus.value) && !availableStatusesToExclude.contains(availableStatus)){
                    availableStatusesJSONArray.put(availableStatus.value)
                }
            }
        }
        // otherwise ignore them
        else {
            availableStatusList.each {
                availableStatusesJSONArray.put(it.value)
            }
        }
        return availableStatusesJSONArray
    }
}
