package org.bbop.apollo

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray

@Transactional
class CannedAttributeService {

    JSONArray getCannedKeys(Organism organism, List<FeatureType> featureTypeList) {

        JSONArray cannedKeys = new JSONArray();

        List<CannedKey> cannedKeyList = new ArrayList<>()
        if (featureTypeList) {
            cannedKeyList.addAll(CannedKey.executeQuery("select cc from CannedKey cc join cc.featureTypes ft where ft in (:featureTypeList)", [featureTypeList: featureTypeList]))
        }
        cannedKeyList.addAll(CannedKey.executeQuery("select cc from CannedKey cc where cc.featureTypes is empty"))

        // if there are organism filters for these canned comments for this organism, then apply them
        List<CannedKeyOrganismFilter> cannedKeyOrganismFilters = CannedKeyOrganismFilter.findAllByCannedKeyInList(cannedKeyList)
        if (cannedKeyOrganismFilters) {
            CannedKeyOrganismFilter.findAllByOrganismAndCannedKeyInList(organism, cannedKeyList).each {
                cannedKeys.put(it.cannedKey.label)
            }
        }
        // otherwise ignore them
        else {
            cannedKeyList.each {
                cannedKeys.put(it.label)
            }
        }
        return cannedKeys
    }

    JSONArray getCannedValues(Organism organism, List<FeatureType> featureTypeList) {

        JSONArray cannedValues = new JSONArray();

        List<CannedValue> cannedValueList = new ArrayList<>()
        if (featureTypeList) {
            cannedValueList.addAll(CannedValue.executeQuery("select cc from CannedValue cc join cc.featureTypes ft where ft in (:featureTypeList)", [featureTypeList: featureTypeList]))
        }
        cannedValueList.addAll(CannedValue.executeQuery("select cc from CannedValue cc where cc.featureTypes is empty"))

        // if there are organism filters for these canned comments for this organism, then apply them
        List<CannedValueOrganismFilter> cannedValueOrganismFilters = CannedValueOrganismFilter.findAllByCannedValueInList(cannedValueList)
        if (cannedValueOrganismFilters) {
            CannedValueOrganismFilter.findAllByOrganismAndCannedValueInList(organism, cannedValueList).each {
                cannedValues.put(it.cannedValue.label)
            }
        }
        // otherwise ignore them
        else {
            cannedValueList.each {
                cannedValues.put(it.label)
            }
        }
        return cannedValues
    }
}
