package org.bbop.apollo

import grails.gorm.transactions.Transactional
import org.grails.web.json.JSONArray

@Transactional
class CannedAttributeService {

    JSONArray getCannedKeys(Organism organism, List<FeatureType> featureTypeList) {
        JSONArray cannedKeysJSONArray = new JSONArray();
        List<CannedKey> cannedKeyList = new ArrayList<>()
        if (featureTypeList) {
            cannedKeyList.addAll(CannedKey.executeQuery("select cc from CannedKey cc join cc.featureTypes ft where ft in (:featureTypeList)", [featureTypeList: featureTypeList]))
        }
        cannedKeyList.addAll(CannedKey.executeQuery("select cc from CannedKey cc where cc.featureTypes is empty"))

        // if there are organism filters for these canned comments for this organism, then apply them
        List<CannedKeyOrganismFilter> cannedKeyOrganismFilters = CannedKeyOrganismFilter.findAllByCannedKeyInList(cannedKeyList)
        if (cannedKeyOrganismFilters) {
            CannedKeyOrganismFilter.findAllByOrganismAndCannedKeyInList(organism, cannedKeyList).each {
                cannedKeysJSONArray.put(it.cannedKey.label)
            }
            // we have to add anything from cannedCommentList that isn't in another one
            List<CannedKey> cannedKeysToExclude = CannedKeyOrganismFilter.findAllByOrganismNotEqualAndCannedKeyInList(organism, cannedKeyList).cannedKey
            for(CannedKey cannedKey in cannedKeyList){
                if(!cannedKeysJSONArray.contains(cannedKey.label) && !cannedKeysToExclude.contains(cannedKey)){
                    cannedKeysJSONArray.put(cannedKey.label)
                }
            }
        }
        // otherwise ignore them
        else {
            cannedKeyList.each {
                cannedKeysJSONArray.put(it.label)
            }
        }
        return cannedKeysJSONArray
    }

    JSONArray getCannedValues(Organism organism, List<FeatureType> featureTypeList) {

        JSONArray cannedValuesJSONArray = new JSONArray();

        List<CannedValue> cannedValueList = new ArrayList<>()
        if (featureTypeList) {
            cannedValueList.addAll(CannedValue.executeQuery("select cc from CannedValue cc join cc.featureTypes ft where ft in (:featureTypeList)", [featureTypeList: featureTypeList]))
        }
        cannedValueList.addAll(CannedValue.executeQuery("select cc from CannedValue cc where cc.featureTypes is empty"))

        // if there are organism filters for these canned comments for this organism, then apply them
        List<CannedValueOrganismFilter> cannedValueOrganismFilters = CannedValueOrganismFilter.findAllByCannedValueInList(cannedValueList)
        if (cannedValueOrganismFilters) {
            CannedValueOrganismFilter.findAllByOrganismAndCannedValueInList(organism, cannedValueList).each {
                cannedValuesJSONArray.put(it.cannedValue.label)
            }
            // we have to add anything from cannedValueList that isn't in another one
            List<CannedValue> cannedValuesToExclude = CannedValueOrganismFilter.findAllByOrganismNotEqualAndCannedValueInList(organism, cannedValueList).cannedValue
            for(CannedValue cannedValue in cannedValueList){
                if(!cannedValuesJSONArray.contains(cannedValue.label) && !cannedValuesToExclude.contains(cannedValue)){
                    cannedValuesJSONArray.put(cannedValue.label)
                }
            }
        }
        // otherwise ignore them
        else {
            cannedValueList.each {
                cannedValuesJSONArray.put(it.label)
            }
        }
        return cannedValuesJSONArray
    }
}
