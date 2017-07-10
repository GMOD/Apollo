package org.bbop.apollo

import grails.transaction.Transactional
import htsjdk.samtools.util.Interval
import htsjdk.variant.vcf.VCFFileReader
import htsjdk.variant.variantcontext.VariantContext
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection
import org.bbop.apollo.gwt.shared.projection.ProjectionSequence

@Transactional
class VcfService {

    def projectionService
    def featureProjectionService

    def processProjection(JSONArray featuresArray, MultiSequenceProjection projection, VCFFileReader vcfFileReader, int start, int end) {
        Map<Integer, Integer> lengthMap = new TreeMap<>()
        // Note: incoming coordinates are zero-based
        println "[DEVEL][processProjection] incoming START: ${start}"
        println "[DEVEL][processProjection] incoming END: ${end}"

        if (start < 0 && end > 0) {
            println "[DEVEL][processProjection] start < 0 and end > 0; adjusting start to 0"
            start = 0
        }

        println "[DEVEL][processProjection] adjusted START: ${start}"
        println "[DEVEL][processProjection] adjusted END: ${end}"
        // unprojecting input coordinates
        start = projection.unProjectValue((long) start)
        end = projection.unProjectValue((long) end)
        println "[DEVEL][processProjection] unprojected START: ${start}"
        println "[DEVEL][processProjection] unprojected END: ${end}"

        if (start < 0 && end < 0) {
            // nothing to do since the requested region has negative coordinates
            println "[DEVEL][processProjection] both start and end are < 0; returning empty featuresArray"
            return featuresArray
        }

        // get all features from VCF that fall within the given coordinates
        for (ProjectionSequence projectionSequence : projection.sequenceDiscontinuousProjectionMap.keySet().sort() {a,b -> a.order <=> b.order}) {
            def vcfEntries = []
            println "[DEVEL][processProjection] projection sequence name ${projectionSequence.name}"
            lengthMap.put(projectionSequence.order, projection.sequenceDiscontinuousProjectionMap.get(projectionSequence).length)
            println "[DEVEL][processProjection] Querying VCF with projectionSequence.name: ${projectionSequence.name} ${start + 1}-${end} (note the adjusted start)"
            // changing zero-based start to one-based start while querying VCF
            def queryResults = vcfFileReader.query(projectionSequence.name, start + 1, end)
            while(queryResults.hasNext()) {
                vcfEntries.add(queryResults.next())
            }
            calculateFeaturesArray(featuresArray, vcfEntries, projection, projectionSequence)
        }

        return featuresArray
    }

    def processSequence(JSONArray featuresArray, String sequenceName, VCFFileReader vcfFileReader, int start, int end) {
        // TODO: In what scenario will this method be called
        if (start < 0 && end > 0) {
            println "[DEVEL][processSequence] start < 0 and end > 0; adjusting start to 0"
            start = 0
        }
        else if (start < 0 && end < 0) {
            // nothing to do since the requested region has negative coordinates
            println "[DEVEL][processSequence] both start and end are < 0; returning empty featuresArray"
            return featuresArray
        }

        def vcfEntries = []
        def queryResults = vcfFileReader.query(projectionSequence.name, start + 1, end)
        while(queryResults.hasNext()) {
            vcfEntries.add(queryResults.next())
        }
        calculateFeaturesArray(featuresArray, vcfEntries, sequenceName)
        return featuresArray
    }

    def calculateFeaturesArray(JSONArray featuresArray, def vcfEntries, MultiSequenceProjection projection, ProjectionSequence projectionSequence) {
        for (VariantContext vc : vcfEntries) {
            JSONObject jsonFeature = new JSONObject()
            jsonFeature = createJSONFeature(vc)
            println "[DEVEL] jsonFeature BEFORE: ${jsonFeature.toString()}"
            projectJSONFeature(jsonFeature, projection, false, 0)
            println "[DEVEL] jsonFeature AFTER: ${jsonFeature.toString()}"
            featuresArray.add(jsonFeature)
        }

        return featuresArray
    }

    def calculateFeaturesArray(JSONArray featuresArray, def vcfEntries, String sequenceName) {
        for (VariantContext vc : vcfEntries) {
            JSONObject jsonFeature = new JSONObject()
            jsonFeature = createJSONFeature(vc)
            featuresArray.add(jsonFeature)
        }
        return featuresArray
    }

    def createJSONFeature(VariantContext variantContext) {
        JSONObject jsonFeature = new JSONObject()
        JSONObject location = new JSONObject()
        // changing one-based start to zero-based start
        location.put("fmin", variantContext.getStart() - 1)
        location.put("fmax", variantContext.getEnd())
        location.put("sequence", variantContext.getContig())
        jsonFeature.put("start", variantContext.getStart())
        jsonFeature.put("end", variantContext.getEnd())
        jsonFeature.put("type", variantContext.getType().name())
        jsonFeature.put("uniqueID", variantContext.getID())
        jsonFeature.put("location", location)
        // TODO: add more information
        return jsonFeature
    }

    def projectJSONFeature(JSONObject jsonFeature, MultiSequenceProjection projection, Boolean unProject, Integer offset) {
        // Note: jsonFeature must have 'location' object for FPS:projectFeature to work
        featureProjectionService.projectFeature(jsonFeature, projection, unProject, offset)
        return jsonFeature
    }


}
