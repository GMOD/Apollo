package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class VariantService {

    def featureService
    def permissionService

    def createVariant(JSONObject jsonFeature, Sequence sequence, Boolean suppressHistory) {
        Feature variant = featureService.convertJSONToFeature(jsonFeature, sequence)
        User owner = permissionService.getCurrentUser(jsonFeature)

        if (owner) {
            featureService.setOwner(variant, owner)
        }
        else {
            log.error "Unable to find valid user to set on variant: " + jsonFeature.toString()
        }

        // TODO: Name service
        featureService.updateNewGsolFeatureAttributes(variant, sequence)
        variant.save(flush: true)

        return variant
    }

    def addAlternateAlleles(JSONObject jsonFeature) {
        String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
        Feature feature = Feature.findByUniqueName(uniqueName)
        JSONArray alternateAllelesArray = jsonFeature.getJSONArray(FeatureStringEnum.ALTERNATE_ALLELES.value)

        for (int i = 0; i < alternateAllelesArray.size(); i++) {
            JSONObject alternateAlleleObject = alternateAllelesArray.getJSONObject(i)
            String bases = alternateAlleleObject.getString(FeatureStringEnum.BASES.value).toUpperCase()
            String alleleFrequencyString
            String provenance
            Allele allele = new Allele(bases: bases)

            if (alternateAlleleObject.has(FeatureStringEnum.ALLELE_INFO.value)) {
                JSONArray alleleInfoArray = alternateAlleleObject.getJSONArray(FeatureStringEnum.ALLELE_INFO.value)
                for (int j = 0; j < alleleInfoArray.size(); j++) {
                    JSONObject alleleInfoObject = alleleInfoArray.getJSONObject(j)
                    if (alleleInfoObject.getString(FeatureStringEnum.TAG.value) == FeatureStringEnum.ALLELE_FREQUENCY_TAG.value) {
                        if (alleleInfoObject.getString(FeatureStringEnum.VALUE.value)) {
                            alleleFrequencyString = alleleInfoObject.getString(FeatureStringEnum.VALUE.value)
                            Float alleleFrequency = Float.parseFloat(alleleFrequencyString)
                            if (alleleInfoObject.has(FeatureStringEnum.PROVENANCE.value)) {
                                provenance = alleleInfoObject.getString(FeatureStringEnum.PROVENANCE.value)
                            }

                            if (alleleFrequency >= 0 && alleleFrequency <= 1.0) {
                                if (provenance) {
                                    log.debug "Adding Alternate Allele ${bases} with AF: ${alleleFrequency} and provenance: ${provenance}"
                                    allele.alleleFrequency = alleleFrequency
                                    allele.provenance = provenance
                                }
                                else {
                                    log.error "Rejecting alleleFrequency for Allele: ${bases} as no provenance is provided"
                                }
                            }
                            else {
                                log.error "Unexpected Alternate Allele Frequency value of ${alleleFrequencyString}"
                            }
                        }
                    }
                }
            }

            allele.variant = (SequenceAlteration) feature
            allele.save()
            feature.addToAlternateAlleles(allele)
        }

        feature.save(flush: true, failOnError: true)
        return feature
    }

    def deleteAlternateAlleles(JSONObject jsonFeature) {
        String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
        Feature feature = Feature.findByUniqueName(uniqueName)
        JSONArray alternateAllelesArray = jsonFeature.getJSONArray(FeatureStringEnum.ALTERNATE_ALLELES.value)

        for (int j = 0; j < alternateAllelesArray.size(); j++) {
            JSONObject alternateAlleleObject = alternateAllelesArray.getJSONObject(j)
            String bases = alternateAlleleObject.getString(FeatureStringEnum.BASES.value).toUpperCase()
            def alternateAlleles = Allele.executeQuery("SELECT DISTINCT a FROM Allele AS a WHERE a.bases = :queryBases", [queryBases: bases])
            JSONObject alleleInfoObject = (JSONObject) alternateAlleleObject.getJSONArray(FeatureStringEnum.ALLELE_INFO.value).iterator().next()
            if (alleleInfoObject.getString(FeatureStringEnum.VALUE.value)) {
                String alleleFrequencyString = alleleInfoObject.getString(FeatureStringEnum.VALUE.value)
                Float alleleFrequency = Float.parseFloat(alleleFrequencyString)
                String provenance = alternateAlleleObject.getJSONArray(FeatureStringEnum.ALLELE_INFO.value).getJSONObject(0).getString(FeatureStringEnum.PROVENANCE.value)
                // get all alleles that match the given bases
                for (Allele allele : alternateAlleles) {
                    if (allele.alleleFrequency == alleleFrequency && allele.provenance == provenance) {
                        feature.removeFromAlternateAlleles(allele)
                        allele.delete(flush: true)
                        log.debug "Allele ${bases} with AF: ${alleleFrequency} and provenance: ${provenance} removed from feature: ${feature.uniqueName}"
                        break;
                    }
                }
            }
            else {
                if (alternateAlleles.size() == 1) {
                    // there is only one alternate allele with the given base
                    Allele allele = alternateAlleles.iterator().next()
                    feature.removeFromAlternateAlleles(allele)
                    allele.delete(flush: true)
                }
                else {
                    log.error "Cannot delete alternate allele ${bases} since more than one result matches the given allele"
                }
            }
        }
        feature.save(flush: true, failOnError: true)
    }

    def updateAlternateAlleles(JSONObject jsonFeature) {
        String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
        Feature feature = Feature.findByUniqueName(uniqueName)
        JSONArray oldAlternateAlleleArray = jsonFeature.getJSONArray(FeatureStringEnum.OLD_ALTERNATE_ALLELES.value)
        JSONArray newAlternateAlleleArray = jsonFeature.getJSONArray(FeatureStringEnum.NEW_ALTERNATE_ALLELES.value)

        for (int i = 0; i < oldAlternateAlleleArray.size(); i++) {
            JSONObject oldAlternateAlleleObject = oldAlternateAlleleArray.getJSONObject(i)
            JSONObject newAlternateAlleleObject = newAlternateAlleleArray.getJSONObject(i)
            String oldAltAlleleBases = oldAlternateAlleleObject.getString(FeatureStringEnum.BASES.value).toUpperCase()
            String newAltAlleleBases = newAlternateAlleleObject.getString(FeatureStringEnum.BASES.value).toUpperCase()
            Float oldAltAlleleFrequency
            Float newAltAlleleFrequency
            String oldProvenance
            String newProvenance

            if (oldAlternateAlleleObject.has(FeatureStringEnum.ALLELE_INFO.value)) {
                JSONObject alleleInfoObject = (JSONObject) oldAlternateAlleleObject.getJSONArray(FeatureStringEnum.ALLELE_INFO.value).iterator().next()
                if (alleleInfoObject.get(FeatureStringEnum.TAG.value) == FeatureStringEnum.ALLELE_FREQUENCY_TAG.value && alleleInfoObject.get(FeatureStringEnum.VALUE.value) != null) {
                    if (alleleInfoObject.getString(FeatureStringEnum.VALUE.value)) {
                        oldAltAlleleFrequency = Float.parseFloat(alleleInfoObject.getString(FeatureStringEnum.VALUE.value))
                        if (alleleInfoObject.has(FeatureStringEnum.PROVENANCE.value)) {
                            oldProvenance = alleleInfoObject.getString(FeatureStringEnum.PROVENANCE.value)
                        }
                    }
                }
            }

            if (newAlternateAlleleObject.has(FeatureStringEnum.ALLELE_INFO.value)) {
                JSONObject alleleInfoObject = (JSONObject) newAlternateAlleleObject.getJSONArray(FeatureStringEnum.ALLELE_INFO.value).iterator().next()
                if (alleleInfoObject.get(FeatureStringEnum.TAG.value) == FeatureStringEnum.ALLELE_FREQUENCY_TAG.value && alleleInfoObject.get(FeatureStringEnum.VALUE.value) != null) {
                    if (alleleInfoObject.getString(FeatureStringEnum.VALUE.value)) {
                        newAltAlleleFrequency = Float.parseFloat(alleleInfoObject.getString(FeatureStringEnum.VALUE.value))
                        if (alleleInfoObject.has(FeatureStringEnum.PROVENANCE.value)) {
                            newProvenance = alleleInfoObject.getString(FeatureStringEnum.PROVENANCE.value)
                        }
                    }
                }
            }

            def alternateAlleles
            if (oldAltAlleleBases) {
                alternateAlleles = Allele.executeQuery(
                        "SELECT DISTINCT a FROM Allele a WHERE a.bases = :queryBases",
                        [queryBases: oldAltAlleleBases])
            }
            else if (oldAltAlleleBases && oldAltAlleleFrequency) {
                alternateAlleles = Allele.executeQuery(
                        "SELECT DISTINCT a FROM Allele a WHERE a.bases = :queryBases AND a.alleleFrequency = :queryAlleleFrequency",
                        [queryBases: oldAltAlleleBases, queryAlleleFrequency: oldAltAlleleFrequency])
            }
            else if (oldAltAlleleBases && oldAltAlleleFrequency && oldProvenance) {
                alternateAlleles = Allele.executeQuery(
                        "SELECT DISTINCT a FROM Allele a WHERE a.bases = :queryBases AND a.alleleFrequency = :queryAlleleFrequency AND a.provenance = :queryProvenance",
                        [queryBases: oldAltAlleleBases, queryAlleleFrequency: oldAltAlleleFrequency, queryProvenance: oldProvenance])
            }
            if (alternateAlleles.size() == 0) {
                log.error "Cannot find alternate allele ${oldAltAlleleBases} with AF: ${oldAltAlleleFrequency} and provenance: ${oldProvenance}"
            }
            else if (alternateAlleles.size() == 1) {
                Allele allele = alternateAlleles.iterator().next()
                allele.bases = newAltAlleleBases
                if (newAltAlleleFrequency) {
                    if (newAltAlleleFrequency >= 0 && newAltAlleleFrequency <= 1) {
                        if (newAltAlleleFrequency && newProvenance) {
                            allele.alleleFrequency = newAltAlleleFrequency
                            allele.provenance = newProvenance
                        }
                        else {
                            log.error "Rejecting alleleFrequency for Allele: ${newAltAlleleBases} as no provenance is provided"
                        }
                    }
                    else {
                        log.error "Unexpected Alternate Allele Frequency value of ${newAltAlleleFrequency} with provenance: ${newProvenance}"
                    }
                }
                allele.save()
            }
            else {
                log.error "More than one alternate allele ${oldAltAlleleBases} found with  AF: ${oldAltAlleleFrequency} and provenance: ${oldProvenance}"
            }
        }

        feature.save(flush: true, failOnError: true)
        return feature
    }

    def addVariantInfo(JSONObject jsonFeature) {
        String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
        Feature feature = Feature.findByUniqueName(uniqueName)
        JSONArray variantInfoArray = jsonFeature.getJSONArray(FeatureStringEnum.VARIANT_INFO.value)

        for (int j = 0; j < variantInfoArray.size(); j++){
            JSONObject variantInfoObject = variantInfoArray.getJSONObject(j)
            String tag = variantInfoObject.get(FeatureStringEnum.TAG.value)
            String value = variantInfoObject.get(FeatureStringEnum.VALUE.value)
            featureService.addNonReservedProperties(feature, tag, value)
        }
        feature.save(flush: true, failOnError: true)
        return feature
    }

    def deleteVariantInfo(JSONObject jsonFeature) {
        Feature feature = Feature.findByUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))
        JSONArray properties = jsonFeature.getJSONArray(FeatureStringEnum.VARIANT_INFO.value)

        for (int j = 0; j < properties.length(); j++) {
            JSONObject property = properties.getJSONObject(j)
            String tag = property.get(FeatureStringEnum.TAG.value)
            String value = property.get(FeatureStringEnum.VALUE.value)
            FeatureProperty featureProperty = FeatureProperty.findByTagAndValueAndFeature(tag, value, feature)
            if (featureProperty) {
                feature.removeFromFeatureProperties(featureProperty)
                feature.save()
                featureProperty.delete(flush: true)
            }
            else {
                log.error "Could not find feature property ${property.toString()} to delete for variant: ${feature}"
            }
        }
        feature.save(flush: true, failOnError: true)
        return feature
    }

    def updateVariantInfo(JSONObject jsonFeature) {
        Feature feature = Feature.findByUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))
        JSONArray oldVariantInfoArray = jsonFeature.getJSONArray(FeatureStringEnum.OLD_VARIANT_INFO.value)
        JSONArray newVariantInfoArray = jsonFeature.getJSONArray(FeatureStringEnum.NEW_VARIANT_INFO.value)

        for (int i = 0; i < oldVariantInfoArray.size(); i++) {
            JSONObject oldVariantInfoObject = oldVariantInfoArray.getJSONObject(i)
            JSONObject newVariantInfoObject = newVariantInfoArray.getJSONObject(i)
            String oldTag = oldVariantInfoObject.getString(FeatureStringEnum.TAG.value)
            String oldValue = oldVariantInfoObject.getString(FeatureStringEnum.VALUE.value)
            String newTag = newVariantInfoObject.getString(FeatureStringEnum.TAG.value)
            String newValue = newVariantInfoObject.getString(FeatureStringEnum.VALUE.value)

            FeatureProperty featureProperty = FeatureProperty.findByTagAndValueAndFeature(oldTag, oldValue, feature)
            if (featureProperty) {
                featureProperty.tag = newTag
                featureProperty.value = newValue
                featureProperty.save()
            }
            else {
                log.error "Could not find feature property ${oldVariantInfoObject.toString()} to update for variant: ${feature}"
            }
        }
        feature.save(flush: true, failOnError: true)
        return feature
    }
}
