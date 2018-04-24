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
    def sequenceService
    def nameService

    def createVariant(JSONObject jsonFeature, Sequence sequence, Boolean suppressHistory) {

        // this scenario would happen when a variant is created from an evidence track such as a VCF track
        log.info "create variant from an evidence track"
        if (! (jsonFeature.has(FeatureStringEnum.ALTERNATE_ALLELES.value) &&
                jsonFeature.getJSONArray(FeatureStringEnum.ALTERNATE_ALLELES.value).size() > 0)) {
            throw new AnnotationException("Variant has no alternate allele(s)");
        }
        if (! validateRefBases(jsonFeature, sequence)) {
            throw new AnnotationException("REF allele from Variant at position: ${jsonFeature.get(FeatureStringEnum.LOCATION.value).fmin} does not match the genomic residues at the same position.")
        }

        SequenceAlteration variant = (SequenceAlteration) featureService.convertJSONToFeature(jsonFeature, sequence)
        variant.name = nameService.makeUniqueVariantName(variant)

        User owner = permissionService.getCurrentUser(jsonFeature)
        if (owner) {
            featureService.setOwner(variant, owner)
        }
        else {
            log.error "Unable to find valid user to set on variant: " + jsonFeature.toString()
        }

        featureService.updateNewGsolFeatureAttributes(variant, sequence)
        variant.save(flush: true)

        // TODO: parse metadata
        return variant
    }

    def deleteAlternateAlleles(JSONObject jsonFeature) {
        String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
        Feature feature = Feature.findByUniqueName(uniqueName)
        JSONArray alternateAllelesArray = jsonFeature.getJSONArray(FeatureStringEnum.ALTERNATE_ALLELES.value)

        for (int j = 0; j < alternateAllelesArray.size(); j++) {
            JSONObject alternateAlleleObject = alternateAllelesArray.getJSONObject(j)
            String bases = alternateAlleleObject.getString(FeatureStringEnum.BASES.value).toUpperCase()
            def alternateAlleles = Allele.executeQuery("SELECT DISTINCT a FROM Allele AS a WHERE a.bases = :queryBases", [queryBases: bases])
            if (alternateAlleleObject.has(FeatureStringEnum.ALLELE_FREQUENCY.value) && alternateAlleleObject.getString(FeatureStringEnum.ALLELE_FREQUENCY.value)) {
                String alleleFrequencyString = alternateAlleleObject.getString(FeatureStringEnum.ALLELE_FREQUENCY.value)
                Float alleleFrequency = getAlleleFrequencyFromJsonObject(alleleFrequencyString)
                String provenance
                if (alternateAlleleObject.has(FeatureStringEnum.PROVENANCE.value) && alternateAlleleObject.getString(FeatureStringEnum.PROVENANCE.value)) {
                    provenance = alternateAlleleObject.getString(FeatureStringEnum.PROVENANCE.value)
                }
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
            if (oldAlternateAlleleObject.has(FeatureStringEnum.ALLELE_FREQUENCY.value) && oldAlternateAlleleObject.getString(FeatureStringEnum.ALLELE_FREQUENCY.value)){
                oldAltAlleleFrequency = Float.parseFloat(oldAlternateAlleleObject.getString(FeatureStringEnum.ALLELE_FREQUENCY.value))
            }
            if (newAlternateAlleleObject.has(FeatureStringEnum.ALLELE_FREQUENCY.value) && newAlternateAlleleObject.getString(FeatureStringEnum.ALLELE_FREQUENCY.value)) {
                newAltAlleleFrequency = Float.parseFloat(newAlternateAlleleObject.getString(FeatureStringEnum.ALLELE_FREQUENCY.value))
            }

            String oldProvenance
            String newProvenance
            if (oldAlternateAlleleObject.has(FeatureStringEnum.PROVENANCE.value) && oldAlternateAlleleObject.getString(FeatureStringEnum.PROVENANCE.value)) {
                oldProvenance = oldAlternateAlleleObject.get(FeatureStringEnum.PROVENANCE.value)
            }
            if (newAlternateAlleleObject.has(FeatureStringEnum.PROVENANCE.value) && newAlternateAlleleObject.getString(FeatureStringEnum.PROVENANCE.value)) {
                newProvenance = newAlternateAlleleObject.get(FeatureStringEnum.PROVENANCE.value)
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

    def addAlleleInfo(JSONObject jsonFeature) {
        String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
        Feature feature = Feature.findByUniqueName(uniqueName)
        JSONArray alleleInfoArray = jsonFeature.getJSONArray(FeatureStringEnum.ALLELE_INFO.value)

        for (int j = 0; j < alleleInfoArray.size(); j++){
            JSONObject alleleInfoObject = alleleInfoArray.getJSONObject(j)
            String alleleBase = alleleInfoObject.get(FeatureStringEnum.ALLELE.value)
            Allele allele = getAlleleForVariant(uniqueName, alleleBase)
            String tag = alleleInfoObject.get(FeatureStringEnum.TAG.value)
            String value = alleleInfoObject.get(FeatureStringEnum.VALUE.value)
            createAlleleInfo(allele, tag, value)
        }
        feature.save(flush: true, failOnError: true)
        return feature
    }

    def deleteAlleleInfo(JSONObject jsonFeature) {
        String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
        Feature feature = Feature.findByUniqueName(uniqueName)
        JSONArray alleleInfoArray = jsonFeature.getJSONArray(FeatureStringEnum.ALLELE_INFO.value)

        for (int j = 0; j < alleleInfoArray.length(); j++) {
            JSONObject alleleInfoObject = alleleInfoArray.getJSONObject(j)
            String alleleBase = alleleInfoObject.get(FeatureStringEnum.ALLELE.value)
            Allele allele = getAlleleForVariant(uniqueName, alleleBase)
            String tag = alleleInfoObject.get(FeatureStringEnum.TAG.value)
            String value = alleleInfoObject.get(FeatureStringEnum.VALUE.value)
            AlleleInfo alleleInfo = AlleleInfo.findByAlleleAndTagAndValue(allele, tag, value)
            if (alleleInfo) {
                allele.removeFromAlleleInfo(alleleInfo)
                alleleInfo.delete(flush: true)
            }
            else {
                log.error "Could not find AlleleInfo ${tag}:${value} for Allele: ${alleleBase}"
            }
        }
        return feature
    }

    def updateAlleleInfo(JSONObject jsonFeature) {
        Feature feature = Feature.findByUniqueName(jsonFeature.getString(FeatureStringEnum.UNIQUENAME.value))
        JSONArray oldAlleleInfoArray = jsonFeature.getJSONArray(FeatureStringEnum.OLD_ALLELE_INFO.value)
        JSONArray newAlleleInfoArray = jsonFeature.getJSONArray(FeatureStringEnum.NEW_ALLELE_INFO.value)

        for (int i = 0; i < oldAlleleInfoArray.size(); i++) {
            JSONObject oldAlleleInfoObject = oldAlleleInfoArray.getJSONObject(i)
            JSONObject newAlleleInfoObject = newAlleleInfoArray.getJSONObject(i)
            String oldAlleleBase = oldAlleleInfoObject.getString(FeatureStringEnum.ALLELE.value)
            Allele oldAllele = getAlleleForVariant(feature.uniqueName, oldAlleleBase)
            String newAlleleBase = newAlleleInfoObject.getString(FeatureStringEnum.ALLELE.value)
            Allele newAllele = getAlleleForVariant(feature.uniqueName, newAlleleBase)
            String oldTag = oldAlleleInfoObject.getString(FeatureStringEnum.TAG.value)
            String oldValue = oldAlleleInfoObject.getString(FeatureStringEnum.VALUE.value)
            String newTag = newAlleleInfoObject.getString(FeatureStringEnum.TAG.value)
            String newValue = newAlleleInfoObject.getString(FeatureStringEnum.VALUE.value)

            AlleleInfo oldAlleleInfo = AlleleInfo.findByAlleleAndTagAndValue(oldAllele, oldTag, oldValue)
            if (oldAlleleInfo) {
                if (oldAlleleBase != newAlleleBase) {
                    oldAllele.removeFromAlleleInfo(oldAlleleInfo)
                    oldAllele.save()
                    newAllele.addToAlleleInfo(new AlleleInfo(tag: newTag, value: newValue, allele: newAllele).save())
                    newAllele.save()
                }
                else {
                    oldAlleleInfo.tag = newTag
                    oldAlleleInfo.value = newValue
                    oldAlleleInfo.save()
                }
            }
            else {
                log.error "Cannot find AlleleInfo ${tag}:${value} for Allele: ${oldAlleleBase}"
            }

        }
        feature.save(flush: true, failOnError: true)
        return feature
    }

    def createAlleleInfo(Allele allele, String tag, String value) {
        // TODO: Check if tag-value pair for this allele already exists
        AlleleInfo alleleInfo = new AlleleInfo(
                tag: tag,
                value: value,
                allele: allele
        ).save()
    }

    def getAlleleFrequencyFromJsonObject(String alleleFrequencyString) {
        try {
            Float alleleFrequency = Float.parseFloat(alleleFrequencyString)
            if (alleleFrequency >= 0.0 && alleleFrequency <= 1.0) {
                return alleleFrequency
            }
            else {
                log.error "Allele Frequency ${alleleFrequency} value must be within the range 0.0 - 1.0"
            }
        } catch (NumberFormatException e){
            log.error "Unexpected Allele Frequency value of ${alleleFrequencyString} with exception: ${e.stackTrace}"
        }
    }

    def getAlleleForVariant(String variantUniqueName, String alleleBase) {
        Allele allele
        def results = Allele.executeQuery(
                "SELECT DISTINCT a FROM Allele a JOIN a.variant v WHERE v.uniqueName = :variantUniqueNameString AND a.bases = :alleleBaseString",
                [variantUniqueNameString: variantUniqueName, alleleBaseString: alleleBase]
        )

        if (results.size() == 0) {
            log.error "Cannot find allele: ${alleleBase} for variant: ${variantUniqueName}"
        }
        else if (results.size() == 1) {
            allele = results.first()
        }
        else {
            log.error "More than one allele: ${alleleBase} for variant: ${variantUniqueName}"
        }
        return allele
    }

    def validateRefBases(JSONObject feature, Sequence sequence) {
        String refString = feature.get(FeatureStringEnum.REFERENCE_ALLELE.value)
        int fmin = feature.get(FeatureStringEnum.LOCATION.value).fmin
        int fmax = feature.get(FeatureStringEnum.LOCATION.value).fmax
        String genomicResidues = sequenceService.getRawResiduesFromSequence(sequence, fmin, fmax)
        return (refString == genomicResidues)
    }
}
