package org.bbop.apollo

import grails.transaction.Transactional
import htsjdk.variant.variantcontext.Genotype
import htsjdk.variant.variantcontext.VariantContext
import htsjdk.variant.variantcontext.VariantContextUtils
import htsjdk.variant.vcf.VCFCompoundHeaderLine
import htsjdk.variant.vcf.VCFConstants
import htsjdk.variant.vcf.VCFFileReader
import htsjdk.variant.vcf.VCFHeader
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

import java.text.DecimalFormat

@Transactional
class VcfService {

    public static final String ALTERNATIVE_ALLELE_METADATA = "VCF ALT field, list of alternate non-reference alleles called on at least one of the samples"
    public static final String ALTERNATIVE_ALLELE_MISSING = "ALT_MISSING"
    public static final String SNV = "SNV"
    public static final String INVERSION = "inversion"
    public static final String SUBSTITUTION = "substitution"
    public static final String INSERTION = "insertion"
    public static final String DELETION = "deletion"

    /**
     * Given a VCF and a location, extract variants and process them into a JSON array
     * @param organism
     * @param vcfFileReader
     * @param sequenceName
     * @param start
     * @param end
     * @param includeGenotypes
     * @return
     */
    def processVcfRecords(Organism organism, VCFFileReader vcfFileReader, String sequenceName, Long start, Long end, boolean includeGenotypes = false) {
        // Note: incoming coordinates are zero-based
        JSONArray processedFeaturesArray = new JSONArray()
        log.info "incoming start: ${start} end: ${end}"
        (start, end) = processCoordinates(organism, sequenceName, start, end)

        if(start < 0 && end < 0) {
            return processedFeaturesArray
        }

        def vcfEntries = []
        VCFHeader vcfHeader = vcfFileReader.getFileHeader()

        // changing zero-based start to one-based start while querying VCF
        log.info "querying with ${sequenceName} ${start + 1} ${end}"
        // casting start and end to int since query() method doesn't support long data type for coordinates
        def queryResults = vcfFileReader.query(sequenceName, (int) start + 1, (int) end)

        // we want to be generous with the sequence name lookup
        if(!queryResults && sequenceName.toLowerCase().startsWith("chr")){
            String filteredName = sequenceName.substring("chr".length())
            queryResults = vcfFileReader.query(filteredName, (int) start + 1, (int) end)
        }
        vcfEntries.addAll(queryResults.toList())
        log.info "result size: ${vcfEntries.size()}"
        processedFeaturesArray = processVcfRecords(vcfHeader, vcfEntries, sequenceName, includeGenotypes)

        return processedFeaturesArray
    }

    /**
     * Process a set of variants and its genotypes into a JSON array
     * @param vcfHeader
     * @param vcfEntries
     * @param sequenceName
     * @param includeGenotypes
     * @return
     */
    def processVcfRecords(VCFHeader vcfHeader, def vcfEntries, String sequenceName, boolean includeGenotypes = false) {
        JSONArray featuresArray = new JSONArray()
        for(VariantContext vc : vcfEntries) {
            JSONObject jsonFeature = processVcfRecord(vcfHeader, vc, sequenceName, includeGenotypes)
            featuresArray.add(jsonFeature)
        }

        return featuresArray
    }

    /**
     * Process a given variant and its genotypes into JSON
     * @param vcfHeader
     * @param variantContext
     * @param sequenceName
     * @param includeGenotypes
     * @return
     */
    def processVcfRecord(VCFHeader vcfHeader, VariantContext variantContext, String sequenceName, boolean includeGenotypes = false) {
        JSONObject jsonFeature = new JSONObject()
        String type = classifyType(variantContext)
        String referenceAlleleString = variantContext.getReference().baseString
        String alternativeAllelesString = variantContext.getAlternateAlleles().baseString.join(',')
        if(alternativeAllelesString.isEmpty()) alternativeAllelesString = ALTERNATIVE_ALLELE_MISSING

        def availableFormatFields = []
        vcfHeader.getFormatHeaderLines().each {
            availableFormatFields.add(it.properties.get("ID"))
        }

        // alternative alleles
        JSONObject alternativeAllelesMetaObject = new JSONObject()
        alternativeAllelesMetaObject.put(FeatureStringEnum.DESCRIPTION.value, ALTERNATIVE_ALLELE_METADATA)
        JSONObject alternativeAllelesObject = new JSONObject()
        alternativeAllelesObject.put(FeatureStringEnum.VALUES.value, alternativeAllelesString)
        alternativeAllelesObject.put(FeatureStringEnum.META.value, alternativeAllelesMetaObject)
        jsonFeature.put(FeatureStringEnum.ALTERNATIVE_ALLELES.value, alternativeAllelesObject)
        String descriptionString = "${type} ${referenceAlleleString} > ${alternativeAllelesString}"

        // changing one-based start to zero-based start
        int start = variantContext.getStart()
        int end = variantContext.getEnd()
        // Note: the use of 'seqId' doesn't match Apollo's default JSON format; might need revisions in the future
        jsonFeature.put("seqId", sequenceName)
        jsonFeature.put(FeatureStringEnum.FMIN.value, start - 1)
        jsonFeature.put(FeatureStringEnum.FMAX.value, end)
        jsonFeature.put(FeatureStringEnum.TYPE.value, type)

        if(variantContext.getID() != ".") {
            jsonFeature.put("uniqueID", variantContext.getID())
            jsonFeature.put(FeatureStringEnum.NAME.value, variantContext.getID())
        }
        else {
            jsonFeature.put("uniqueID", "${descriptionString} at position ${start}")
        }

        // reference allele
        jsonFeature.put(FeatureStringEnum.REFERENCE_ALLELE.value, referenceAlleleString)

        // description
        jsonFeature.put(FeatureStringEnum.DESCRIPTION.value, descriptionString)

        // score
        if(variantContext.getPhredScaledQual() > 0) {
            jsonFeature.put(FeatureStringEnum.SCORE.value, new DecimalFormat("##.###").format(variantContext.getPhredScaledQual()))
        }

        // attributes
        def variantAttributes = variantContext.getCommonInfo().getAttributes()
        for(String attributeKey : variantAttributes.keySet()) {
            JSONObject attributeObject = new JSONObject()
            JSONArray valuesArray = new JSONArray()
            valuesArray.add(variantAttributes.get(attributeKey))
            attributeObject.put(FeatureStringEnum.VALUES.value, valuesArray)

            // metadata for attributes
            VCFCompoundHeaderLine metaData = VariantContextUtils.getMetaDataForField(vcfHeader, attributeKey)
            if(metaData) {
                JSONObject attributeMetaDataObject = generateMetaDataObjectForProperty(metaData, variantContext)
                attributeObject.put(FeatureStringEnum.META.value, attributeMetaDataObject)
            }
            jsonFeature.put(attributeKey, attributeObject)
        }

        // genotypes
        def genotypes = variantContext.getGenotypes()
        if(genotypes.size() > 0) {
            if(includeGenotypes) {
                JSONObject genotypeJSONObject = parseGenotypes(variantContext, genotypes, vcfHeader, availableFormatFields)
                jsonFeature.put(FeatureStringEnum.GENOTYPES.value, genotypeJSONObject)
            }
            else {
                jsonFeature.put(FeatureStringEnum.GENOTYPES.value, true)
            }
        }

        // filter
        if(variantContext.filtered) {
            JSONObject filterObject = new JSONObject()
            JSONArray valuesArray = new JSONArray()
            variantContext.getFilters().each {
                valuesArray.add(it)
            }
            filterObject.put(FeatureStringEnum.VALUES.value, valuesArray)
            jsonFeature.put(FeatureStringEnum.FILTER.value, filterObject)
        }
        else {
            if(variantContext.getFiltersMaybeNull() != null) {
                // PASS
                JSONObject filterObject = new JSONObject()
                JSONArray valuesArray = new JSONArray()
                valuesArray.add("PASS")
                filterObject.put(FeatureStringEnum.VALUES.value, valuesArray)
                jsonFeature.put(FeatureStringEnum.FILTER.value, filterObject)
            }
        }

        return jsonFeature
    }

    /**
     * Parse genotypes for a given variant
     * @param variantContext
     * @param genotypes
     * @param vcfHeader
     * @param availableFormatFields
     */
    def parseGenotypes(VariantContext variantContext, def genotypes, VCFHeader vcfHeader, def availableFormatFields) {
        JSONObject genotypeObject = new JSONObject()
        for(int i = 0; i < genotypes.size(); i++) {
            Genotype genotype = genotypes.get(i)
            JSONObject formatJsonObject = new JSONObject()
            for(String key : availableFormatFields) {
                log.debug "processing format field: ${key}"
                if(genotype.hasAnyAttribute(key)) {
                    JSONObject formatPropertiesJsonObject = new JSONObject()
                    if(key == VCFConstants.GENOTYPE_KEY) {
                        def alleles = genotype.getAlleles()
                        def alleleAsIndices = variantContext.getAlleleIndices(alleles)
                        String delimiter = genotype.isPhased() ? VCFConstants.PHASED : VCFConstants.UNPHASED
                        String genotypeAsIndices = alleleAsIndices.join(delimiter)

                        JSONArray valuesArray = new JSONArray()
                        valuesArray.add(genotypeAsIndices)
                        formatPropertiesJsonObject.put(FeatureStringEnum.VALUES.value, valuesArray)
                        VCFCompoundHeaderLine metaData = VariantContextUtils.getMetaDataForField(vcfHeader, key)
                        if(metaData) {
                            JSONObject attributeMetaDataObject = generateMetaDataObjectForProperty(metaData, variantContext)
                            formatPropertiesJsonObject.put(FeatureStringEnum.META.value, attributeMetaDataObject)
                        }
                    }
                    else {
                        JSONArray valuesArray = new JSONArray()
                        def keyValues = genotype.getAnyAttribute(key)
                        keyValues instanceof String ? valuesArray.add(keyValues.split(",")) : valuesArray.add(keyValues)
                        formatPropertiesJsonObject.put(FeatureStringEnum.VALUES.value, valuesArray)
                        VCFCompoundHeaderLine metaData = VariantContextUtils.getMetaDataForField(vcfHeader, key)
                        if(metaData) {
                            JSONObject attributeMetaDataObject = generateMetaDataObjectForProperty(metaData, variantContext)
                            formatPropertiesJsonObject.put(FeatureStringEnum.META.value, attributeMetaDataObject)
                        }
                    }
                    formatJsonObject.put(key, formatPropertiesJsonObject)
                }
                genotypeObject.put(genotype.sampleName, formatJsonObject)
            }
        }

        return genotypeObject
    }

    /**
     *
     * @param metaData
     * @param variantContext
     * @return
     */
    def generateMetaDataObjectForProperty(VCFCompoundHeaderLine metaData, VariantContext variantContext) {
        JSONObject metaDataObject = new JSONObject()
        metaDataObject.put(FeatureStringEnum.ID.value, new JSONArray())
        metaDataObject.put(FeatureStringEnum.TYPE.value, new JSONArray())
        metaDataObject.put(FeatureStringEnum.NUMBER.value, new JSONArray())
        metaDataObject.put(FeatureStringEnum.DESCRIPTION.value, new JSONArray())
        metaDataObject.getJSONArray(FeatureStringEnum.ID.value).add(metaData.getID())
        metaDataObject.getJSONArray(FeatureStringEnum.TYPE.value).add(metaData.getCountType())
        metaDataObject.getJSONArray(FeatureStringEnum.NUMBER.value).add(metaData.getCount(variantContext))
        metaDataObject.getJSONArray(FeatureStringEnum.DESCRIPTION.value).add(metaData.getDescription())
        return metaDataObject
    }

    /**
     * Classify variant type for compatibility with JBrowse
     * @param variantContext
     * @return
     */
    def classifyType(VariantContext variantContext) {
        String type = variantContext.getType().name()
        String referenceAlleleString = variantContext.getReference().baseString
        def alternateAlleles = variantContext.getAlternateAlleles()
        int minAlternateAlleleLength = 0
        int maxAlternateAlleleLength = 0

        alternateAlleles.baseString.each {
            if(minAlternateAlleleLength == 0 && maxAlternateAlleleLength == 0) {
                minAlternateAlleleLength = it.length()
                maxAlternateAlleleLength = it.length()
            }
            else {
                if(minAlternateAlleleLength > it.length()) {
                    minAlternateAlleleLength = it.length()
                }
                if(maxAlternateAlleleLength < it.length()) {
                    maxAlternateAlleleLength = it.length()
                }
            }

        }

        if(referenceAlleleString.length() == minAlternateAlleleLength && referenceAlleleString.length() == maxAlternateAlleleLength) {
            type = SNV
        }
        else if(referenceAlleleString.length() == minAlternateAlleleLength && referenceAlleleString.length() == maxAlternateAlleleLength) {
            if(alternateAlleles.size() == 1 && referenceAlleleString.split('').reverse(true).join('') == alternateAlleles[0].baseString) {
                type = INVERSION
            }
            else {
                type = SUBSTITUTION
            }
        }
        if(referenceAlleleString.length() <= minAlternateAlleleLength && referenceAlleleString.length() < maxAlternateAlleleLength) {
            type = INSERTION
        }
        if(referenceAlleleString.length() > minAlternateAlleleLength && referenceAlleleString.length() >= maxAlternateAlleleLength) {
            type = DELETION
        }

        return type
    }

    /**
     *
     * @param organism
     * @param sequenceName
     * @param start
     * @param end
     * @return
     */
    def processCoordinates(Organism organism, String sequenceName, Long start, Long end) {
        Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, organism)

        if(start > 0) {
            if(end < 0) {
                // that means the request is to fetch something near the end of the sequence
                // and the requested end is outside the sequence boundary.
                // Thus, adjust end to max length of current sequence
                end = sequence.length
            }
            else {
                // that means the request is to fetch something near the end of the sequence
                // and the requested end is outside the sequence boundary.
                // Thus, adjust end to max length of current sequence
                if(start < sequence.length && end > sequence.length) end = sequence.length
            }
        }
        else if(start < 0) {
            if(end < 0) {
                // do nothing
            }
            else {
                // that means the request is to fetch something at the start of the sequence
                // and the requested start is outside the sequence boundary.
                // Thus, adjust start to 0
                start = 0
            }
        }

        return [start, end]
    }
}
