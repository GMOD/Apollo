package org.bbop.apollo

import grails.converters.JSON
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.sequence.Strand
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import spock.lang.Ignore
import spock.lang.IgnoreRest

class FeatureServiceIntegrationSpec extends AbstractIntegrationSpec{

    def featureService
    def transcriptService
    def requestHandlingService
    def featureRelationshipService


    void "convert JSON to Features"() {

        given: "a set string and existing sequence, when we have a complicated mRNA as JSON"
        String jsonString = "{${testCredentials}  \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":1216824,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40856-RA\",\"children\":[{\"location\":{\"fmin\":1235534,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1216850,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1224676,\"fmax\":1224823,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1228682,\"fmax\":1228825,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235237,\"fmax\":1235396,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235487,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1235534,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"

        when: "we parse it"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject

        then: "is is a valid object"
        assert jsonObject != null
        JSONArray jsonArray = jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert jsonArray.size() == 1
        JSONObject mRNAJsonObject = jsonArray.getJSONObject(0)
        JSONArray childArray = jsonArray.getJSONObject(0).getJSONArray(FeatureStringEnum.CHILDREN.value)
        assert childArray.size() == 7

        when: "we convert it to a feature"
        Feature feature = featureService.convertJSONToFeature(mRNAJsonObject, Sequence.first())

        then: "it should convert it to the same feature"
        assert feature != null
        feature.ontologyId == MRNA.ontologyId

    }

    void "convert Feature to JSON and convert the JSON back to a feature"() {

        given: "a transcript GB40744-RA"
        String transcriptString = "{${testCredentials}  \"operation\":\"add_feature\",\"features\":[{\"location\":{\"fmin\":761542,\"strand\":-1,\"fmax\":768063},\"children\":[{\"location\":{\"fmin\":761542,\"strand\":-1,\"fmax\":768063},\"children\":[{\"location\":{\"fmin\":767945,\"strand\":-1,\"fmax\":768063},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":761542,\"strand\":-1,\"fmax\":763070},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":761542,\"strand\":-1,\"fmax\":763513},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":765327,\"strand\":-1,\"fmax\":765472},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":765551,\"strand\":-1,\"fmax\":766176},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":766255,\"strand\":-1,\"fmax\":767133},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":767207,\"strand\":-1,\"fmax\":767389},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":767485,\"strand\":-1,\"fmax\":768063},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":763070,\"strand\":-1,\"fmax\":767945},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"transcript\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"pseudogene\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String setSymbolString = "{${testCredentials}  \"operation\":\"set_symbol\",\"features\":[{\"symbol\":\"@SYMBOL@\",\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\"}"
        String setDescriptionString = "{${testCredentials}  \"operation\":\"set_description\",\"features\":[{\"description\":\"@DESCRIPTION@\",\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\"}"
        String addNonPrimaryDbxrefString = "{${testCredentials}  \"operation\":\"add_non_primary_dbxrefs\",\"features\":[{\"dbxrefs\":[{\"db\":\"@DB@\",\"accession\":\"@ACCESSION@\"}],\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\"}"
        String addNonReservedPropertyString = "{${testCredentials}  \"operation\":\"add_non_reserved_properties\",\"features\":[{\"non_reserved_properties\":[{\"tag\":\"@TAG@\",\"value\":\"@VALUE@\"}],\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\"}"
        String addCommentString = "{${testCredentials}  \"operation\":\"add_comments\",\"features\":[{\"uniquename\":\"@UNIQUENAME@\",\"comments\":[\"@COMMENT@\"]}],\"track\":\"Group1.10\"}"

        when: "we add the transcript"
        requestHandlingService.addFeature(JSON.parse(transcriptString) as JSONObject)

        then: "we should see the transcript"
        assert Gene.count == 1
        assert Transcript.count == 1
        Transcript transcript = Transcript.all.get(0)
        Gene gene = Gene.all.get(0)

        when: "we add feature properties and attributes to the gene"
        requestHandlingService.setSymbol(JSON.parse(setSymbolString.replace("@UNIQUENAME@", gene.uniqueName).replace("@SYMBOL@", "TGN1")) as JSONObject)
        requestHandlingService.setDescription(JSON.parse(setDescriptionString.replace("@UNIQUENAME@", gene.uniqueName).replace("@DESCRIPTION@", "This is a test gene TGN1")) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addNonPrimaryDbxrefString.replace("@UNIQUENAME@", gene.uniqueName).replace("@DB@", "NCBI").replace("@ACCESSION@", "48734522")) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addNonPrimaryDbxrefString.replace("@UNIQUENAME@", gene.uniqueName).replace("@DB@", "Ensembl").replace("@ACCESSION@", "ENSG0000000131")) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addNonPrimaryDbxrefString.replace("@UNIQUENAME@", gene.uniqueName).replace("@DB@", "PMID").replace("@ACCESSION@", "437598")) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addNonPrimaryDbxrefString.replace("@UNIQUENAME@", gene.uniqueName).replace("@DB@", "GO").replace("@ACCESSION@", "0048564")) as JSONObject)
        requestHandlingService.addNonReservedProperties(JSON.parse(addNonReservedPropertyString.replace("@UNIQUENAME@", gene.uniqueName).replace("@TAG@", "isValidated").replace("@VALUE@", "false")) as JSONObject)
        requestHandlingService.addNonReservedProperties(JSON.parse(addNonReservedPropertyString.replace("@UNIQUENAME@", gene.uniqueName).replace("@TAG@", "annotationType").replace("@VALUE@", "Pseudogene")) as JSONObject)
        requestHandlingService.addComments(JSON.parse(addCommentString.replace("@UNIQUENAME@", gene.uniqueName).replace("@COMMENT@", "This is a test gene")) as JSONObject)
        requestHandlingService.addComments(JSON.parse(addCommentString.replace("@UNIQUENAME@", gene.uniqueName).replace("@COMMENT@", "This gene is not validated")) as JSONObject)

        then: "we should see the added properties"
        assert gene.featureDBXrefs.size() > 0
        assert gene.featureProperties.size() > 0

        when: "we add feature properties and attributes to the transcript"
        requestHandlingService.setSymbol(JSON.parse(setSymbolString.replace("@UNIQUENAME@", transcript.uniqueName).replace("@SYMBOL@", "TGN1-1A")) as JSONObject)
        requestHandlingService.setDescription(JSON.parse(setDescriptionString.replace("@UNIQUENAME@", transcript.uniqueName).replace("@DESCRIPTION@", "This is an isoform for gene TGN1")) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addNonPrimaryDbxrefString.replace("@UNIQUENAME@", transcript.uniqueName).replace("@DB@", "NCBI").replace("@ACCESSION@", "XM_3973451.2")) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addNonPrimaryDbxrefString.replace("@UNIQUENAME@", transcript.uniqueName).replace("@DB@", "Ensembl").replace("@ACCESSION@", "ENST00000031241")) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addNonPrimaryDbxrefString.replace("@UNIQUENAME@", transcript.uniqueName).replace("@DB@", "PMID").replace("@ACCESSION@", "3749242")) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addNonPrimaryDbxrefString.replace("@UNIQUENAME@", transcript.uniqueName).replace("@DB@", "GO").replace("@ACCESSION@", "0051497")) as JSONObject)
        requestHandlingService.addNonReservedProperties(JSON.parse(addNonReservedPropertyString.replace("@UNIQUENAME@", transcript.uniqueName).replace("@TAG@", "isValidated").replace("@VALUE@", "false")) as JSONObject)
        requestHandlingService.addNonReservedProperties(JSON.parse(addNonReservedPropertyString.replace("@UNIQUENAME@", transcript.uniqueName).replace("@TAG@", "annotationType").replace("@VALUE@", "Pseudogene transcript")) as JSONObject)
        requestHandlingService.addComments(JSON.parse(addCommentString.replace("@UNIQUENAME@", transcript.uniqueName).replace("@COMMENT@", "This is a test transcript")) as JSONObject)
        requestHandlingService.addComments(JSON.parse(addCommentString.replace("@UNIQUENAME@", transcript.uniqueName).replace("@COMMENT@", "This transcript is not validated")) as JSONObject)

        then: "we should see the added properties"
        assert transcript.featureDBXrefs.size() > 0
        assert transcript.featureProperties.size() > 0

        when: "we convert the gene to a JSONObject via convertFeatureToJSON()"
        JSONObject geneFeatureJsonObject = featureService.convertFeatureToJSON(gene)

        then: "we should see the properties of gene and transcript in the JSONObject"
        JSONArray featureProperties = geneFeatureJsonObject.getJSONArray(FeatureStringEnum.PROPERTIES.value)
        assert featureProperties.size() > 0
        def expectedDbxrefForGene = ["NCBI:48734522", "Ensembl:ENSG0000000131", "PMID:437598", "GO:0048564" ]
        def expectedDbxrefForTranscript = ["NCBI:XM_3973451.2", "Ensembl:ENST00000031241", "PMID:3749242", "GO:0051497"]
        def expectedFeaturePropertiesForGene = ["isValidated:false", "annotationType:Pseudogene"]
        def expectedFeaturePropertiesForTranscript = ["isValidated:false", "annotationType:Pseudogene transcript"]
        def expectedCommentsForGene = ["This is a test gene", "This gene is not validated"]
        def expectedCommentsForTranscript = ["This is a test transcript", "This transcript is not validated"]

        when: "we delete the gene and transcript and try to add the feature via the JSONObject"
        featureRelationshipService.deleteFeatureAndChildren(gene)
        Sequence sequence = Sequence.all.get(0)
        featureService.convertJSONToFeature(geneFeatureJsonObject, sequence)

        then: "convertJSONToFeature() should interpret the JSONObject properly and we should see all the properties that we added above"
        assert Gene.count == 1
        Gene newGene = Gene.all.get(0)
        Transcript newTranscript = Transcript.all.get(0)

        assert newGene.symbol == "TGN1"
        assert newGene.description == "This is a test gene TGN1"
        assert newGene.featureDBXrefs.size() == 4
        assert newGene.featureProperties.size() == 4

        newGene.featureProperties.each { fp ->
            if (fp instanceof Comment) {
                assert expectedCommentsForGene.indexOf(fp.value) != -1
                expectedCommentsForGene.remove(expectedCommentsForGene.indexOf(fp.value))
            }
            else {
                String key = fp.tag + ":" + fp.value
                assert expectedFeaturePropertiesForGene.indexOf(key) != -1
                expectedFeaturePropertiesForGene.remove(expectedFeaturePropertiesForGene.indexOf(key))
            }
        }

        assert expectedCommentsForGene.size() == 0
        assert expectedFeaturePropertiesForGene.size() == 0

        newGene.featureDBXrefs.each { dbxref ->
            String key = dbxref.db.name + ":" + dbxref.accession
            assert expectedDbxrefForGene.indexOf(key) != -1
            expectedDbxrefForGene.remove(expectedDbxrefForGene.indexOf(key))
        }

        assert expectedDbxrefForGene.size() == 0


        assert newTranscript.symbol == "TGN1-1A"
        assert newTranscript.description == "This is an isoform for gene TGN1"
        assert newTranscript.featureDBXrefs.size() == 4
        assert newTranscript.featureProperties.size() == 4

        newTranscript.featureProperties.each { fp ->
            if (fp instanceof Comment) {
                assert expectedCommentsForTranscript.indexOf(fp.value) != -1
                expectedCommentsForTranscript.remove(expectedCommentsForTranscript.indexOf(fp.value))
            }
            else {
                String key = fp.tag + ":" + fp.value
                assert expectedFeaturePropertiesForTranscript.indexOf(key) != -1
                expectedFeaturePropertiesForTranscript.remove(expectedFeaturePropertiesForTranscript.indexOf(key))
            }
        }

        assert expectedCommentsForTranscript.size() == 0
        assert expectedFeaturePropertiesForTranscript.size() == 0

        newTranscript.featureDBXrefs.each { dbxref ->
            String key = dbxref.db.name + ":" + dbxref.accession
            assert expectedDbxrefForTranscript.indexOf(key) != -1
            expectedDbxrefForTranscript.remove(expectedDbxrefForTranscript.indexOf(key))
        }

        assert expectedDbxrefForTranscript.size() == 0
    }

    void "If an annotation doesn't have strand information then it should, by default, be set to the sense strand"() {

        given: "a transcript with no strand information"
        String featureString = "{${testCredentials} \"operation\":\"add_feature\",\"features\":[{\"location\":{\"fmin\":761542,\"strand\":0,\"fmax\":768063},\"children\":[{\"location\":{\"fmin\":761542,\"strand\":0,\"fmax\":768063},\"children\":[{\"location\":{\"fmin\":767945,\"strand\":0,\"fmax\":768063},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":761542,\"strand\":0,\"fmax\":763070},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":761542,\"strand\":0,\"fmax\":763513},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":765327,\"strand\":0,\"fmax\":765472},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":765551,\"strand\":0,\"fmax\":766176},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":766255,\"strand\":0,\"fmax\":767133},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":767207,\"strand\":0,\"fmax\":767389},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":767485,\"strand\":0,\"fmax\":768063},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":763070,\"strand\":0,\"fmax\":767945},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"transcript\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"pseudogene\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add a feature that has its strand as 0"
        requestHandlingService.addFeature(JSON.parse(featureString) as JSONObject)

        then: "we should see the feature, and all of its sub-features, placed on the sense strand"
        Gene gene = Gene.all.get(0)
        Transcript transcript = transcriptService.getTranscripts(gene).iterator().next()
        def exonList = transcriptService.getExons(transcript)

        assert gene.featureLocation.strand == Strand.POSITIVE.value
        assert transcript.featureLocation.strand == Strand.POSITIVE.value

        exonList.each {
            it.featureLocation.strand == Strand.POSITIVE.value
        }
    }

    void "When adding a transcript (on the reverse strand) and transcript fragment, all the fragments should be isoforms of the main transcript and have their CDS set as expected"() {

        given: "1 transcript and 3 transcript fragments"
        String addTranscript1String = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":689640,\"strand\":-1,\"fmax\":693859},\"name\":\"GB40750-RA\",\"children\":[{\"location\":{\"fmin\":693543,\"strand\":-1,\"fmax\":693859},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":692451,\"strand\":-1,\"fmax\":692480},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":689640,\"strand\":-1,\"fmax\":690442},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":689640,\"strand\":-1,\"fmax\":690739},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":690844,\"strand\":-1,\"fmax\":691015},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":691158,\"strand\":-1,\"fmax\":691354},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":691436,\"strand\":-1,\"fmax\":691587},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":691674,\"strand\":-1,\"fmax\":691846},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":691974,\"strand\":-1,\"fmax\":692181},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":692310,\"strand\":-1,\"fmax\":692480},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":693543,\"strand\":-1,\"fmax\":693859},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":690442,\"strand\":-1,\"fmax\":692451},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addTranscriptFragment1String = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":689640,\"strand\":-1,\"fmax\":690739},\"name\":\"GB40750-RA\",\"children\":[{\"location\":{\"fmin\":689640,\"strand\":-1,\"fmax\":690739},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addTranscriptFragment2String = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":691158,\"strand\":-1,\"fmax\":691354},\"name\":\"GB40750-RA\",\"children\":[{\"location\":{\"fmin\":691158,\"strand\":-1,\"fmax\":691354},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addTranscriptFragment3String = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":691674,\"strand\":-1,\"fmax\":691846},\"name\":\"GB40750-RA\",\"children\":[{\"location\":{\"fmin\":691674,\"strand\":-1,\"fmax\":691846},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add the complete transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1
        Gene parentGene = Gene.all.get(0)

        when: "we add transcript fragment 1"
        JSONObject addTranscriptFragment1ReturnObject = requestHandlingService.addTranscript(JSON.parse(addTranscriptFragment1String) as JSONObject)
        String transcriptFragment1UniqueName = addTranscriptFragment1ReturnObject.getJSONArray(FeatureStringEnum.FEATURES.value).getJSONObject(0).get(FeatureStringEnum.UNIQUENAME.value)

        then: "we should see the transcript fragment"
        assert MRNA.all.size() == 2
        MRNA transcriptFragment1 = MRNA.findByUniqueName(transcriptFragment1UniqueName)
        CDS transcriptFragment1Cds = transcriptService.getCDS(transcriptFragment1)

        assert transcriptService.getGene(transcriptFragment1) == parentGene
        assert transcriptFragment1Cds.featureLocation.fmin == 690442
        assert transcriptFragment1Cds.featureLocation.fmax == 690739
        assert !transcriptFragment1Cds.featureLocation.isFminPartial
        assert transcriptFragment1Cds.featureLocation.isFmaxPartial

        when: "we add transcript fragment 2"
        JSONObject addTranscriptFragment2ReturnObject = requestHandlingService.addTranscript(JSON.parse(addTranscriptFragment2String) as JSONObject)
        String transcriptFragment2UniqueName = addTranscriptFragment2ReturnObject.getJSONArray(FeatureStringEnum.FEATURES.value).getJSONObject(0).get(FeatureStringEnum.UNIQUENAME.value)

        then: "we should see the transcript fragment"
        assert MRNA.all.size() == 3
        MRNA transcriptFragment2 = MRNA.findByUniqueName(transcriptFragment2UniqueName)
        CDS transcriptFragment2Cds = transcriptService.getCDS(transcriptFragment2)

        assert transcriptService.getGene(transcriptFragment2) == parentGene
        assert transcriptFragment2Cds.featureLocation.fmin == 691158
        assert transcriptFragment2Cds.featureLocation.fmax == 691353
        assert transcriptFragment2Cds.featureLocation.isFminPartial
        assert transcriptFragment2Cds.featureLocation.isFmaxPartial

        when: "we add transcript fragment 3"
        JSONObject addTranscriptFragment3ReturnObject = requestHandlingService.addTranscript(JSON.parse(addTranscriptFragment3String) as JSONObject)
        String transcriptFragment3UniqueName = addTranscriptFragment3ReturnObject.getJSONArray(FeatureStringEnum.FEATURES.value).getJSONObject(0).get(FeatureStringEnum.UNIQUENAME.value)

        then: "we should see the transcript fragment"
        assert MRNA.all.size() == 4
        MRNA transcriptFragment3 = MRNA.findByUniqueName(transcriptFragment3UniqueName)
        CDS transcriptFragment3Cds = transcriptService.getCDS(transcriptFragment3)

        assert transcriptService.getGene(transcriptFragment3) == parentGene
        assert transcriptFragment3Cds.featureLocation.fmin == 691674
        assert transcriptFragment3Cds.featureLocation.fmax == 691846
        assert transcriptFragment3Cds.featureLocation.isFminPartial
        assert transcriptFragment3Cds.featureLocation.isFmaxPartial
    }

    void "When adding a transcript (on the forward strand) and transcript fragment, all the fragments should be isoforms of the main transcript and have their CDS set as expected"() {

        given: "a transcript and 3 transcript fragments"
        String addTranscriptString = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":577493,\"strand\":1,\"fmax\":583605},\"name\":\"GB40819-RA\",\"children\":[{\"location\":{\"fmin\":583280,\"strand\":1,\"fmax\":583605},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":577493,\"strand\":1,\"fmax\":577643},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":582506,\"strand\":1,\"fmax\":582677},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":583187,\"strand\":1,\"fmax\":583605},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":577493,\"strand\":1,\"fmax\":583280},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addTranscriptFragment1String = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":577493,\"strand\":1,\"fmax\":577643},\"name\":\"GB40819-RA\",\"children\":[{\"location\":{\"fmin\":577493,\"strand\":1,\"fmax\":577643},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addTranscriptFragment2String = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":582506,\"strand\":1,\"fmax\":582677},\"name\":\"GB40819-RA\",\"children\":[{\"location\":{\"fmin\":582506,\"strand\":1,\"fmax\":582677},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addTranscriptFragment3String = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":583187,\"strand\":1,\"fmax\":583605},\"name\":\"GB40819-RA\",\"children\":[{\"location\":{\"fmin\":583187,\"strand\":1,\"fmax\":583605},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add transcript GB40819-RA"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1
        Gene parentGene = Gene.all.get(0)

        when: "we add transcript fragment 1"
        JSONObject addTranscriptFragment1ReturnObject = requestHandlingService.addTranscript(JSON.parse(addTranscriptFragment1String) as JSONObject)
        String transcriptFragment1UniqueName = addTranscriptFragment1ReturnObject.getJSONArray(FeatureStringEnum.FEATURES.value).getJSONObject(0).get(FeatureStringEnum.UNIQUENAME.value)

        then: "we should see the transcript fragment"
        assert MRNA.all.size() == 2
        MRNA transcriptFragment1 = MRNA.findByUniqueName(transcriptFragment1UniqueName)
        CDS transcriptFragment1Cds = transcriptService.getCDS(transcriptFragment1)

        assert transcriptService.getGene(transcriptFragment1) == parentGene
        assert transcriptFragment1Cds.featureLocation.fmin == 577493
        assert transcriptFragment1Cds.featureLocation.fmax == 577643
        assert !transcriptFragment1Cds.featureLocation.isFminPartial
        assert transcriptFragment1Cds.featureLocation.isFmaxPartial

        when: "we add transcript fragment 2"
        JSONObject addTranscriptFragment2ReturnObject = requestHandlingService.addTranscript(JSON.parse(addTranscriptFragment2String) as JSONObject)
        String transcriptFragment2UniqueName = addTranscriptFragment2ReturnObject.getJSONArray(FeatureStringEnum.FEATURES.value).getJSONObject(0).get(FeatureStringEnum.UNIQUENAME.value)

        then: "we should see the transcript fragment"
        assert MRNA.all.size() == 3
        MRNA transcriptFragment2 = MRNA.findByUniqueName(transcriptFragment2UniqueName)
        CDS transcriptFragment2Cds = transcriptService.getCDS(transcriptFragment2)

        assert transcriptService.getGene(transcriptFragment2) == parentGene
        assert transcriptFragment2Cds.featureLocation.fmin == 582506
        assert transcriptFragment2Cds.featureLocation.fmax == 582677
        assert transcriptFragment2Cds.featureLocation.isFminPartial
        assert transcriptFragment2Cds.featureLocation.isFmaxPartial

        when: "we add transcript fragment 3"
        JSONObject addTranscriptFragment3ReturnObject = requestHandlingService.addTranscript(JSON.parse(addTranscriptFragment3String) as JSONObject)
        String transcriptFragment3UniqueName = addTranscriptFragment3ReturnObject.getJSONArray(FeatureStringEnum.FEATURES.value).getJSONObject(0).get(FeatureStringEnum.UNIQUENAME.value)

        then: "we should see the transcript fragment but it shouldn't be an isoform of the main transcript"
        assert MRNA.all.size() == 4
        MRNA transcriptFragment3 = MRNA.findByUniqueName(transcriptFragment3UniqueName)
        CDS transcriptFragment3Cds = transcriptService.getCDS(transcriptFragment3)

        assert transcriptService.getGene(transcriptFragment3) != parentGene
        assert transcriptFragment3Cds.featureLocation.fmin == 583188
        assert transcriptFragment3Cds.featureLocation.fmax == 583554
        assert transcriptFragment3Cds.featureLocation.isFminPartial
        assert !transcriptFragment3Cds.featureLocation.isFmaxPartial
    }
}
