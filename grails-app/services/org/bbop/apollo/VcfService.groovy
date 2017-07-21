package org.bbop.apollo

import grails.transaction.Transactional
import htsjdk.variant.variantcontext.Genotype
import htsjdk.variant.variantcontext.GenotypesContext
import htsjdk.variant.variantcontext.VariantContextUtils
import htsjdk.variant.vcf.VCFCompoundHeaderLine
import htsjdk.variant.vcf.VCFConstants
import htsjdk.variant.vcf.VCFFileReader
import htsjdk.variant.variantcontext.VariantContext
import htsjdk.variant.vcf.VCFHeader
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection
import org.bbop.apollo.gwt.shared.projection.ProjectionSequence
import java.text.DecimalFormat
import org.bbop.apollo.gwt.shared.FeatureStringEnum

@Transactional
class VcfService {

    def projectionService
    def featureProjectionService

    public static final String ALTERNATIVE_ALLELE_METADATA = "VCF ALT field, list of alternate non-reference alleles called on at least one of the samples"
    public static final String ALTERNATIVE_ALLELE_MISSING = "ALT_MISSING"
    public static final String SNV = "SNV"
    public static final String INVERSION = "inversion"
    public static final String SUBSTITUTION = "substitution"
    public static final String INSERTION = "insertion"
    public static final String DELETION = "deletion"

    /**
     *
     * @param featuresArray
     * @param projection
     * @param vcfFileReader
     * @param start
     * @param end
     * @return
     */
    def processProjection(JSONArray featuresArray, MultiSequenceProjection projection, VCFFileReader vcfFileReader, int start, int end) {
        Map<Integer, Integer> lengthMap = new TreeMap<>()
        // Note: incoming coordinates are zero-based
        log.info "incoming start: ${start} end: ${end}"

        if (start < 0 && end < 0) {
            // nothing to do since the requested region has negative coordinates
            log.info "start and end is < 0; returning empty features array"
            return featuresArray
        }
        else if (start > 0 && end < 0) {
            // that means the request is to fetch something near the end of the sequence
            // and the requested end is outside the sequence boundary.
            // Thus, adjust end to max length of the current sequence
            end = projection.length
            log.info "setting end to projection length: ${end}"
        }
        else if (start < 0 && end > 0) {
            // that means the request is to fetch something start the start of the sequence
            // and the requested start is outside the sequence boundary.
            // Thus, adjust start to 0
            log.info "setting start to 0"
            start = 0
        }

        // unprojecting input coordinates
        start = projection.unProjectValue((long) start)
        end = projection.unProjectValue((long) end)
        log.info "unprojected start: ${start} end: ${end}"

        if (start > end) {
            // in a reverse projection, unprojected start will always be greater than unprojected end
            int temp = start
            start = end
            end = temp
        }

        VCFHeader vcfHeader = vcfFileReader.getFileHeader()
        // get all features from VCF that fall within the given coordinate range
        for (ProjectionSequence projectionSequence : projection.sequenceDiscontinuousProjectionMap.keySet().sort() {a,b -> a.order <=> b.order}) {
            def vcfEntries = []
            log.info "projection sequence name ${projectionSequence.name}"
            lengthMap.put(projectionSequence.order, projection.sequenceDiscontinuousProjectionMap.get(projectionSequence).length)
            // changing zero-based start to one-based start while querying VCF
            log.info "querying VCF with projectionSequence.name: ${projectionSequence.name} ${start + 1}-${end} (note the adjusted start)"
            def queryResults = vcfFileReader.query(projectionSequence.name, start + 1, end)
            while(queryResults.hasNext()) {
                vcfEntries.add(queryResults.next())
            }
            calculateFeaturesArray(featuresArray, vcfHeader, vcfEntries, projection, projectionSequence)
        }

        return featuresArray
    }

    /**
     *
     * @param featuresArray
     * @param sequenceName
     * @param vcfFileReader
     * @param start
     * @param end
     * @return
     */
    def processSequence(JSONArray featuresArray, String sequenceName, VCFFileReader vcfFileReader, int start, int end) {
        // TODO: In what scenario will this method be called
        if (start < 0 && end > 0) {
            log.info "start < 0 and end > 0; adjusting start to 0"
            start = 0
        }
        else if (start < 0 && end < 0) {
            // nothing to do since the requested region has negative coordinates
            log.info "both start and end are < 0; returning empty featuresArray"
            return featuresArray
        }

        def vcfEntries = []
        VCFHeader vcfHeader = vcfFileReader.getFileHeader()
        def queryResults = vcfFileReader.query(sequenceName, start + 1, end)
        while(queryResults.hasNext()) {
            vcfEntries.add(queryResults.next())
        }
        calculateFeaturesArray(featuresArray, vcfHeader, vcfEntries, sequenceName)
        return featuresArray
    }

    /**
     * Populate features array with JSON feature representation of variants
     * @param featuresArray
     * @param vcfHeader
     * @param vcfEntries
     * @param projection
     * @param projectionSequence
     * @return
     */
    def calculateFeaturesArray(JSONArray featuresArray, VCFHeader vcfHeader, def vcfEntries, MultiSequenceProjection projection, ProjectionSequence projectionSequence) {
        for (VariantContext vc : vcfEntries) {
            JSONObject jsonFeature = new JSONObject()
            Long timeStart = System.currentTimeMillis()
            jsonFeature = createJSONFeature(vcfHeader, vc, projectionSequence.name)
            Long timeEnd = System.currentTimeMillis()
            log.debug "Time taken to create JSON feature: ${(timeEnd - timeStart)} ms"
            timeStart = System.currentTimeMillis()
            projectJSONFeature(jsonFeature, projection, false, 0)
            timeEnd = System.currentTimeMillis()
            log.debug "Time taken to project JSON feature: ${(timeEnd - timeStart)} ms"
            log.debug "jsonFeature: ${jsonFeature.toString()}"
            featuresArray.add(jsonFeature)
        }

        return featuresArray
    }

    /**
     * Populate features array with JSON feature representation of variants
     * @param featuresArray
     * @param vcfHeader
     * @param vcfEntries
     * @param sequenceName
     * @return
     */
    def calculateFeaturesArray(JSONArray featuresArray, VCFHeader vcfHeader, def vcfEntries, String sequenceName) {
        for (VariantContext vc : vcfEntries) {
            JSONObject jsonFeature = new JSONObject()
            jsonFeature = createJSONFeature(vcfHeader, vc, sequenceName)
            featuresArray.add(jsonFeature)
        }

        return featuresArray
    }

    /**
     * Create a JSON feature representation of a variant
     * @param vcfHeader
     * @param variantContext
     * @return
     */
    def createJSONFeature(VCFHeader vcfHeader, VariantContext variantContext, String sequenceName) {
        JSONObject jsonFeature = new JSONObject()
        String type = classifyType(variantContext)
        String referenceAlleleString = variantContext.getReference().baseString
        String alternativeAllelesString = variantContext.getAlternateAlleles().baseString.join(',')
        if (alternativeAllelesString.isEmpty()) alternativeAllelesString = ALTERNATIVE_ALLELE_MISSING

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
        String descriptionString = "${type} ${referenceAlleleString} -> ${alternativeAllelesString}"

        // changing one-based start to zero-based start
        Long start = variantContext.getStart()
        Long end = variantContext.getEnd()
        jsonFeature.put("seq_id", sequenceName)
        jsonFeature.put(FeatureStringEnum.START.value, variantContext.getStart() - 1)
        jsonFeature.put(FeatureStringEnum.END.value, variantContext.getEnd())
        jsonFeature.put(FeatureStringEnum.TYPE.value, type)

        if (variantContext.getID() != ".") {
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
        if (variantContext.getPhredScaledQual() > 0) {
            jsonFeature.put(FeatureStringEnum.SCORE.value, new DecimalFormat("##.###").format(variantContext.getPhredScaledQual()))
        }

        // attributes
        def variantAttributes = variantContext.getCommonInfo().getAttributes()
        for (String attributeKey : variantAttributes.keySet()) {
            JSONObject attributeObject = new JSONObject()
            JSONArray valuesArray = new JSONArray()
            valuesArray.add(variantAttributes.get(attributeKey))
            attributeObject.put(FeatureStringEnum.VALUES.value, valuesArray)

            // metadata for attributes
            VCFCompoundHeaderLine metaData = VariantContextUtils.getMetaDataForField(vcfHeader, attributeKey)
            if (metaData) {
                JSONObject attributeMetaObject = generateMetaObjectForProperty(metaData, variantContext)
                attributeObject.put(FeatureStringEnum.META.value, attributeMetaObject)
            }
            jsonFeature.put(attributeKey, attributeObject)
        }

        // genotypes
        def genotypes = variantContext.getGenotypes()
        def genotypesJsonObject = new JSONObject()

        for (int i = 0; i < genotypes.size(); i++) {
            Genotype genotype = genotypes.get(i)
            JSONObject formatJsonObject = new JSONObject()
            for (String key : availableFormatFields) {
                //log.debug "processing format field: ${key}"
                if (genotype.hasAnyAttribute(key)) {
                    JSONObject formatPropertiesJsonObject = new JSONObject()
                    if (key == VCFConstants.GENOTYPE_KEY) {
                        def alleles = genotype.getAlleles()
                        def alleleAsIndices = variantContext.getAlleleIndices(alleles)
                        String delimiter = genotype.isPhased() ? VCFConstants.PHASED : VCFConstants.UNPHASED
                        String genotypeAsIndices = alleleAsIndices.join(delimiter)

                        JSONArray valuesArray = new JSONArray()
                        valuesArray.add(genotypeAsIndices)
                        formatPropertiesJsonObject.put(FeatureStringEnum.VALUES.value, valuesArray)
                        VCFCompoundHeaderLine metaData = VariantContextUtils.getMetaDataForField(vcfHeader, key)
                        if (metaData) {
                            JSONObject attributeMetaObject = generateMetaObjectForProperty(metaData, variantContext)
                            formatPropertiesJsonObject.put(FeatureStringEnum.META.value, attributeMetaObject)
                        }
                    }
                    else {
                        def keyValues = genotype.getAnyAttribute(key)
                        JSONArray valuesArray = new JSONArray()
                        valuesArray.add(keyValues)
                        formatPropertiesJsonObject.put(FeatureStringEnum.VALUES.value, valuesArray)
                        VCFCompoundHeaderLine metaData = VariantContextUtils.getMetaDataForField(vcfHeader, key)
                        if (metaData) {
                            JSONObject attributeMetaObject = generateMetaObjectForProperty(metaData, variantContext)
                            formatPropertiesJsonObject.put(FeatureStringEnum.META.value, attributeMetaObject)
                        }
                    }
                    formatJsonObject.put(key, formatPropertiesJsonObject)
                }
                genotypesJsonObject.put(genotype.sampleName, formatJsonObject)
            }
            jsonFeature.put(FeatureStringEnum.GENOTYPES.value, genotypesJsonObject)
        }

        // filter
        if (variantContext.filtered) {
            // TODO: send values as an array
            jsonFeature.put(FeatureStringEnum.FILTER.value, variantContext.getFilters().join(","))
        }

        return jsonFeature
    }

    /**
     * project the JSON feature onto a given multi sequence projection
     * @param jsonFeature
     * @param projection
     * @param unProject
     * @param offset
     * @return
     */
    def projectJSONFeature(JSONObject jsonFeature, MultiSequenceProjection projection, Boolean unProject, Integer offset) {
        // Note: jsonFeature must have 'location' object for FPS:projectFeature to work
        JSONObject location = new JSONObject()
        location.put(FeatureStringEnum.FMIN.value, jsonFeature.start)
        location.put(FeatureStringEnum.FMAX.value, jsonFeature.end)
        location.put(FeatureStringEnum.SEQUENCE.value, jsonFeature.get("seq_id"))
        jsonFeature.put(FeatureStringEnum.LOCATION.value, location)
        featureProjectionService.projectFeature(jsonFeature, projection, unProject, offset)
        jsonFeature.start = jsonFeature.location.fmin
        jsonFeature.end = jsonFeature.location.fmax
        jsonFeature.remove(FeatureStringEnum.LOCATION.value)
        return jsonFeature
    }

    /**
     * Generate meta JSON object for a given property
     * @param metaData
     * @return
     */
    def generateMetaObjectForProperty(VCFCompoundHeaderLine metaData, VariantContext variantContext) {
        JSONObject metaObject = new JSONObject()
        metaObject.put(FeatureStringEnum.ID.value, new JSONArray())
        metaObject.put(FeatureStringEnum.TYPE.value, new JSONArray())
        metaObject.put(FeatureStringEnum.NUMBER.value, new JSONArray())
        metaObject.put(FeatureStringEnum.DESCRIPTION.value, new JSONArray())
        metaObject.getJSONArray(FeatureStringEnum.ID.value).add(metaData.getID())
        metaObject.getJSONArray(FeatureStringEnum.TYPE.value).add(metaData.getCountType())
        metaObject.getJSONArray(FeatureStringEnum.NUMBER.value).add(metaData.getCount(variantContext))
        metaObject.getJSONArray(FeatureStringEnum.DESCRIPTION.value).add(metaData.getDescription())
        return metaObject
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
            if (minAlternateAlleleLength == 0 && maxAlternateAlleleLength == 0) {
                minAlternateAlleleLength = it.length()
                maxAlternateAlleleLength = it.length()
            }
            else {
                if (minAlternateAlleleLength > it.length()) {
                    minAlternateAlleleLength = it.length()
                }
                if (maxAlternateAlleleLength < it.length()) {
                    maxAlternateAlleleLength = it.length()
                }
            }

        }

        if (referenceAlleleString.length() == minAlternateAlleleLength && referenceAlleleString.length() == maxAlternateAlleleLength) {
            type = SNV
        }
        else if (referenceAlleleString.length() == minAlternateAlleleLength && referenceAlleleString.length() == maxAlternateAlleleLength) {
            if (alternateAlleles.size() == 1 && referenceAlleleString.split('').reverse(true).join('') == alternateAlleles[0].baseString) {
                type = INVERSION
            }
            else {
                type = SUBSTITUTION
            }
        }
        if (referenceAlleleString.length() <= minAlternateAlleleLength && referenceAlleleString.length() < maxAlternateAlleleLength) {
            type = INSERTION
        }
        if (referenceAlleleString.length() > minAlternateAlleleLength && referenceAlleleString.length() >= maxAlternateAlleleLength) {
            type = DELETION
        }

        return type
    }

}
