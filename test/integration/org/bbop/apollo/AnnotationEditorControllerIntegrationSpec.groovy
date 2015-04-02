package org.bbop.apollo

import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.json.parser.JSONParser
import spock.lang.Ignore

class AnnotationEditorControllerIntegrationSpec extends IntegrationSpec {

    //def anotationEditorController
    def requestHandlingService
    
    def setup() {
        Sequence refSequence = new Sequence(
                length: 1000
                ,refSeqFile: "adsf"
                ,seqChunkPrefix: "adsf"
                ,seqChunkSize: 1000
                ,start: 1
                ,end: 1000
                ,sequenceDirectory: "temp"
                ,name: "chr1"
        ).save(failOnError: true)
        
        Feature gene = new Gene(
                name: "abc123"
                ,uniqueName: "abc123-gene"
        ).save(failOnError: true)

        FeatureLocation geneFeatureLocation = new FeatureLocation(
                feature: gene
                ,fmin: 200
                ,fmax: 800
                ,strand: 1
                ,sequence: refSequence
        ).save()
        
        MRNA mrna = new MRNA(
                name: "abc123-RA"
                ,uniqueName: "abc123-mRNA"
                ,id: 100
        ).save(flush: true, failOnError: true)

        FeatureRelationship mrnaFeatureRelationship = new FeatureRelationship(
                childFeature: mrna
                ,parentFeature: gene
        ).save()

        FeatureLocation mrnaFeatureLocation = new FeatureLocation(
                fmin: 200
                ,fmax: 800
                ,feature: mrna
                ,sequence: refSequence
                ,strand: 1
        ).save()
        mrna.addToFeatureLocations(mrnaFeatureLocation)
        
        Exon exonOne = new Exon(
                name: "exon1"
                ,uniqueName: "Bob-mRNA-exon1"
        ).save()
        FeatureLocation exonOneFeatureLocation = new FeatureLocation(
                fmin: 300
                ,fmax: 750
                ,feature: exonOne
                ,sequence: refSequence
                ,strand: 1
        ).save()
        exonOne.addToFeatureLocations(exonOneFeatureLocation)
       
        CDS cdsOne = new CDS(
                name: "cds1"
                ,uniqueName: "Bob-mRNA-cds1"
        ).save()
        FeatureLocation cdsOneFeatureLocation = new FeatureLocation(
                fmin: 300
                ,fmax: 750
                ,feature: cdsOne
                ,sequence: refSequence
                ,strand: 1
        ).save()
        cdsOne.addToFeatureLocations(cdsOneFeatureLocation)
        
        println "Features: ${Feature.count}"
    }

    def cleanup() {
    }
    
    @Ignore
    void "test something"() {
    }
    
    @Ignore
    void "given a proper JSON"() {
        
        when: "we have a proper JSON"
        String jsonString = "{ \"track\": \"chr1\", \"features\": [ { \"uniquename\": \"abc123-gene\" } ], \"operation\": \"get_gff3\" }"
        JSONParser parser = new JSONParser(new StringReader(jsonString))
        JSONObject jsonObject = (JSONObject) parser.parseJSON()
//        println "===> JSONOBJECT: ${jsonObject}"
//        params.data = jsonObject.toString()

        
        then: "we should have GFF3 of the requested feature"
        String responseString = controller.response.contentAsString
        
    }
}
