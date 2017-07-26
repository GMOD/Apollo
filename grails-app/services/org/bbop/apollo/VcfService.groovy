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
    def processProjection(JSONArray featuresArray, Organism organism, MultiSequenceProjection projection, VCFFileReader vcfFileReader, int start, int end) {
        // Note: incoming coordinates are zero-based
        log.info "incoming start: ${start} end: ${end}"
        (start, end) = processCoordinates(organism, projection, start, end)
        VCFHeader vcfHeader = vcfFileReader.getFileHeader()

        if (start < 0 && end < 0) {
            return featuresArray
        }

        int count  = 0
        for (ProjectionSequence projectionSequence : projection.sequenceDiscontinuousProjectionMap.keySet().sort() {a,b -> a.order <=> b.order}) {
            def vcfEntries = []
            log.info "projection sequence name ${projectionSequence.name}"
            // changing zero-based start to one-based start while querying VCF
            log.info "querying VCF with projectionSequence.name: ${projectionSequence.name} ${start + 1}-${end} (note the adjusted start)"
            def queryResults = vcfFileReader.query(projectionSequence.name, (int) start + 1, (int) end)
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
    def processSequence(JSONArray featuresArray, Organism organism, String sequenceName, VCFFileReader vcfFileReader, int start, int end) {
        // Note: incoming coordinates are zero-based
        log.info "incoming start: ${start} end: ${end}"
        (start, end) = processCoordinates(organism, sequenceName, start, end)

        if (start < 0 && end < 0) {
            return featuresArray
        }

        def vcfEntries = []
        VCFHeader vcfHeader = vcfFileReader.getFileHeader()

        // changing zero-based start to one-based start while querying VCF
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
        int start = variantContext.getStart()
        int end = variantContext.getEnd()
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
            JSONObject filterObject = new JSONObject()
            JSONArray valuesArray = new JSONArray()
            variantContext.getFilters().each {
                valuesArray.add(it)
            }
            filterObject.put(FeatureStringEnum.VALUES.value, valuesArray)
            jsonFeature.put(FeatureStringEnum.FILTER.value, filterObject)
        }
        else {
            if (variantContext.getFiltersMaybeNull() != null) {
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

    def getFeatureDensitiesForRegion(JSONArray binsArray, Organism organism, MultiSequenceProjection projection, VCFFileReader vcfFileReader, int start, int end, int numBins, int basesPerBin) {
        log.info "incoming start: ${start} end: ${end}"
        (start,end) = processCoordinates(organism, projection, start, end)

        if (start < 0 && end < 0) {
            return binsArray
        }

        for (ProjectionSequence projectionSequence : projection.sequenceDiscontinuousProjectionMap.keySet().sort() {a,b -> a.order <=> b.order}) {
            log.debug "projection sequence name ${projectionSequence.name}"
            log.debug"region ${start}..${end}"
            log.debug"numBins: ${numBins}"
            log.debug "basesPerBin: ${basesPerBin}"
            int rangeStart = start
            int rangeEnd = 0

            for (int i = 1; i <= numBins; i++) {
                if (rangeEnd > 0) rangeStart = rangeEnd
                rangeEnd = rangeStart + basesPerBin
                // this will lead to N number of queries to the VCF where N is numBins
                def queryResults = vcfFileReader.query(projectionSequence.name, rangeStart + 1, rangeEnd)
                binsArray.add(queryResults.size())
                log.debug "${i} ::: rangeStart: ${rangeStart} rangeEnd: ${rangeEnd} size: ${binsArray.last()}"
            }
        }

        log.debug "Histogram Array: ${binsArray}"

        return binsArray
    }

    def getFeatureDensitiesForRegion(JSONArray binsArray, Organism organism, String sequenceName, VCFFileReader vcfFileReader, int start, int end, int numBins, int basesPerBin) {
        log.info "incoming start: ${start} end: ${end}"
        (start,end) = processCoordinates(organism, sequenceName, start, end)

        if (start < 0 && end < 0) {
            return binsArray
        }

        log.debug "sequence name ${sequenceName}"
        log.debug"region ${start}..${end}"
        log.debug"numBins: ${numBins}"
        log.debug "basesPerBin: ${basesPerBin}"
        int rangeStart = start
        int rangeEnd = 0

        for (int i = 1; i <= numBins; i++) {
            if (rangeEnd > 0) rangeStart = rangeEnd
            rangeEnd = rangeStart + basesPerBin
            // this will lead to N number of queries to the VCF where N is numBins
            def queryResults = vcfFileReader.query(sequenceName, rangeStart + 1, rangeEnd)
            binsArray.add(queryResults.size())
            log.debug "${i} ::: rangeStart: ${rangeStart} rangeEnd: ${rangeEnd} size: ${binsArray.last()}"
        }

        log.debug "Histogram Array: ${binsArray}"

        return binsArray
    }

    /**
     * Parse VCF header
     * @param header
     * @return
     */
    def parseVcfFileHeader(VCFHeader header) {
        JSONObject vcfHeaderJSONObject = new JSONObject()
        // only parsing 'filter' from header
        def filterMetaData = header.getFilterLines()
        JSONObject filterObject = new JSONObject()

        filterMetaData.each { filter ->
            JSONObject object = new JSONObject()
            JSONArray idArray = new JSONArray()
            JSONArray descriptionArray = new JSONArray()
            idArray.add(filter.getID())
            descriptionArray.add(filter.getDescription())
            object.put("id", idArray)
            object.put("description", descriptionArray)
            filterObject.put(filter.getID(), object)
        }

        vcfHeaderJSONObject.put("filter", filterObject)
        log.debug "header as JSON ${vcfHeaderJSONObject.toString()}"
        return vcfHeaderJSONObject
    }

    /**
     *
     * @param organism
     * @param projection
     * @param start
     * @param end
     * @return
     */
    def processCoordinates(Organism organism, MultiSequenceProjection projection, int start, int end) {
        if (start > 0 && end < 0) {
            // that means the request is to fetch something near the end of the sequence
            // and the requested end is outside the sequence boundary.
            // Thus, adjust end to max length of current sequence
            end = projection.length
            log.info "setting end to projection length: ${end}"
        }
        else if (start < 0 && end > 0) {
            // that means the request is to fetch something at the start of the sequence
            // and the requested start is outside the sequence boundary.
            // Thus, adjust start to 0
            log.info "setting start to 0"
            start = 0
        }

        // unprojecting input coordinates
        start = projection.unProjectValue(start)
        end = projection.unProjectValue(end)
        log.info "unprojected start: ${start} end: ${end}"

        if (start > end) {
            // in a reverse projection, unprojected start will always be greater than unprojected end
            int temp = start
            start = end
            end = temp
        }

        if (start < 0) start = 0
        if (end > projection.length) end = projection.length

        return [start, end]
    }

    /**
     *
     * @param organism
     * @param sequenceName
     * @param start
     * @param end
     * @return
     */
    def processCoordinates(Organism organism, String sequenceName, int start, int end) {
        Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, organism)
        if (start > 0 && end < 0) {
            // that means the request is to fetch something near the end of the sequence
            // and the requested end is outside the sequence boundary.
            // Thus, adjust end to max length of the current sequence
            end = sequence.length
            log.info "setting end to sequence length: ${end}"
        }
        else if (start < 0 && end > 0) {
            // that means the request is to fetch something at the start of the sequence
            // and the requested start is outside the sequence boundary.
            // Thus, adjust start to 0
            log.info "setting start to 0"
            start = 0
        }

        return [start, end]
    }
}
