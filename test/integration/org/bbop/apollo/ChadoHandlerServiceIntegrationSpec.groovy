package org.bbop.apollo

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject

class ChadoHandlerServiceIntegrationSpec extends AbstractIntegrationSpec{

    // NOTE: This is set to prevent rollback at the end of the integration test
    // for the sake of visual inspection of the database at the end of the test.
    //static transactional = false

    def chadoHandlerService
    def requestHandlingService
    def featureRelationshipService
    def configWrapperService

    void "test CHADO export for standard annotations"() {
        /*
        Standard annotations signifies no modifications/attributes added to the annotations
         */
        if (! configWrapperService.isPostgresChadoDataSource()) {
            log.debug "Skipping test as the currently specified Chado data source is not PostgreSQL."
            return
        }

        given: "series of different types of annotations"
        String gene1transcript1String = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":128706,\"strand\":1,\"fmax\":136964},\"name\":\"GB40797-RA\",\"children\":[{\"location\":{\"fmin\":136502,\"strand\":1,\"fmax\":136964},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":128706,\"strand\":1,\"fmax\":128768},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":131040,\"strand\":1,\"fmax\":131220},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":131322,\"strand\":1,\"fmax\":131487},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":131749,\"strand\":1,\"fmax\":131964},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":132025,\"strand\":1,\"fmax\":132264},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":132394,\"strand\":1,\"fmax\":132620},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":133815,\"strand\":1,\"fmax\":133832},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":134173,\"strand\":1,\"fmax\":134353},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":134476,\"strand\":1,\"fmax\":134684},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":135693,\"strand\":1,\"fmax\":136260},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":136335,\"strand\":1,\"fmax\":136964},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":128706,\"strand\":1,\"fmax\":136502},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String gene1transcript2String = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":128706,\"strand\":1,\"fmax\":136964},\"name\":\"fgeneshpp_with_rnaseq_Group1.10_31_mRNA\",\"children\":[{\"location\":{\"fmin\":128706,\"strand\":1,\"fmax\":128768},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":131040,\"strand\":1,\"fmax\":131220},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":131322,\"strand\":1,\"fmax\":131487},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":131749,\"strand\":1,\"fmax\":131964},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":132025,\"strand\":1,\"fmax\":132264},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":132394,\"strand\":1,\"fmax\":132620},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":133815,\"strand\":1,\"fmax\":133832},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":134173,\"strand\":1,\"fmax\":134353},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":134476,\"strand\":1,\"fmax\":134684},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":135693,\"strand\":1,\"fmax\":136260},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":136335,\"strand\":1,\"fmax\":136964},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":136502,\"strand\":1,\"fmax\":136964},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":128706,\"strand\":1,\"fmax\":136502},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String gene2transcript1String = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":252004,\"strand\":-1,\"fmax\":255153},\"name\":\"GB40770-RA\",\"children\":[{\"location\":{\"fmin\":255124,\"strand\":-1,\"fmax\":255153},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":252400,\"strand\":-1,\"fmax\":252408},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":252004,\"strand\":-1,\"fmax\":252303},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":252004,\"strand\":-1,\"fmax\":252303},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":252400,\"strand\":-1,\"fmax\":252462},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":252667,\"strand\":-1,\"fmax\":252814},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":253468,\"strand\":-1,\"fmax\":253601},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":253751,\"strand\":-1,\"fmax\":253878},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":254826,\"strand\":-1,\"fmax\":255153},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":252408,\"strand\":-1,\"fmax\":255124},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String gene2transcript2String = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":252004,\"strand\":-1,\"fmax\":255153},\"name\":\"fgeneshpp_with_rnaseq_Group1.10_62_mRNA\",\"children\":[{\"location\":{\"fmin\":252004,\"strand\":-1,\"fmax\":252303},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":252400,\"strand\":-1,\"fmax\":252462},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":252667,\"strand\":-1,\"fmax\":252814},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":253468,\"strand\":-1,\"fmax\":253601},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":253751,\"strand\":-1,\"fmax\":253878},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":254826,\"strand\":-1,\"fmax\":255153},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":252400,\"strand\":-1,\"fmax\":252408},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":252004,\"strand\":-1,\"fmax\":252303},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":255124,\"strand\":-1,\"fmax\":255153},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":252408,\"strand\":-1,\"fmax\":255124},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String pseudogeneString = "{ ${testCredentials} \"operation\":\"add_feature\",\"features\":[{\"location\":{\"fmin\":747699,\"strand\":1,\"fmax\":747966},\"children\":[{\"location\":{\"fmin\":747699,\"strand\":1,\"fmax\":747966},\"children\":[{\"location\":{\"fmin\":747699,\"strand\":1,\"fmax\":747760},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":747822,\"strand\":1,\"fmax\":747894},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":747946,\"strand\":1,\"fmax\":747966},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":747699,\"strand\":1,\"fmax\":747966},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"transcript\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"pseudogene\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String tRNAString = "{ ${testCredentials} \"operation\":\"add_feature\",\"features\":[{\"location\":{\"fmin\":775035,\"strand\":-1,\"fmax\":775413},\"children\":[{\"location\":{\"fmin\":775035,\"strand\":-1,\"fmax\":775413},\"children\":[{\"location\":{\"fmin\":775035,\"strand\":-1,\"fmax\":775185},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":775344,\"strand\":-1,\"fmax\":775413},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":775035,\"strand\":-1,\"fmax\":775413},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"tRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"gene\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String snRNAString = "{ ${testCredentials} \"operation\":\"add_feature\",\"features\":[{\"location\":{\"fmin\":278556,\"strand\":1,\"fmax\":281052},\"children\":[{\"location\":{\"fmin\":278556,\"strand\":1,\"fmax\":281052},\"name\":\"fgeneshpp_with_rnaseq_Group1.10_67_mRNA\",\"children\":[{\"location\":{\"fmin\":278556,\"strand\":1,\"fmax\":278569},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":280446,\"strand\":1,\"fmax\":280615},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":280723,\"strand\":1,\"fmax\":281052},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":280550,\"strand\":1,\"fmax\":280615},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":280723,\"strand\":1,\"fmax\":281052},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":278556,\"strand\":1,\"fmax\":280550},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"snRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"gene\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String snoRNAString = "{ ${testCredentials} \"operation\":\"add_feature\",\"features\":[{\"location\":{\"fmin\":433518,\"strand\":1,\"fmax\":437436},\"children\":[{\"location\":{\"fmin\":433518,\"strand\":1,\"fmax\":437436},\"children\":[{\"location\":{\"fmin\":433518,\"strand\":1,\"fmax\":433570},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":436576,\"strand\":1,\"fmax\":436641},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":437424,\"strand\":1,\"fmax\":437436},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":433518,\"strand\":1,\"fmax\":437436},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"snoRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"gene\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String ncRNAString = "{ ${testCredentials} \"operation\":\"add_feature\",\"features\":[{\"location\":{\"fmin\":598161,\"strand\":-1,\"fmax\":598924},\"children\":[{\"location\":{\"fmin\":598161,\"strand\":-1,\"fmax\":598924},\"children\":[{\"location\":{\"fmin\":598161,\"strand\":-1,\"fmax\":598280},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":598782,\"strand\":-1,\"fmax\":598924},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":598161,\"strand\":-1,\"fmax\":598924},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"ncRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"gene\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String rRNAString = "{ ${testCredentials} \"operation\":\"add_feature\",\"features\":[{\"location\":{\"fmin\":664614,\"strand\":-1,\"fmax\":665729},\"children\":[{\"location\":{\"fmin\":664614,\"strand\":-1,\"fmax\":665729},\"children\":[{\"location\":{\"fmin\":664614,\"strand\":-1,\"fmax\":664637},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":665671,\"strand\":-1,\"fmax\":665729},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":664614,\"strand\":-1,\"fmax\":665729},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"rRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"gene\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String miRNAString = "{ ${testCredentials} \"operation\":\"add_feature\",\"features\":[{\"location\":{\"fmin\":675719,\"strand\":-1,\"fmax\":680586},\"children\":[{\"location\":{\"fmin\":675719,\"strand\":-1,\"fmax\":680586},\"children\":[{\"location\":{\"fmin\":675719,\"strand\":-1,\"fmax\":676397},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":675719,\"strand\":-1,\"fmax\":676397},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":678693,\"strand\":-1,\"fmax\":680586},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":678693,\"strand\":-1,\"fmax\":680586},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"miRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"gene\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String repeatRegionString = "{ ${testCredentials} \"operation\":\"add_feature\",\"features\":[{\"location\":{\"fmin\":654601,\"strand\":0,\"fmax\":657144},\"type\":{\"name\":\"repeat_region\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String transposableElementString = "{ ${testCredentials} \"operation\":\"add_feature\",\"features\":[{\"location\":{\"fmin\":621650,\"strand\":0,\"fmax\":628275},\"type\":{\"name\":\"transposable_element\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String insertionString = "{ ${testCredentials} \"operation\":\"add_sequence_alteration\",\"features\":[{\"residues\":\"ATCG\",\"location\":{\"fmin\":689758,\"strand\":1,\"fmax\":689758},\"type\":{\"name\":\"insertion_artifact\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String deletionString = "{ ${testCredentials} \"operation\":\"add_sequence_alteration\",\"features\":[{\"location\":{\"fmin\":689725,\"strand\":1,\"fmax\":689735},\"type\":{\"name\":\"deletion_artifact\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String substitutionString = "{ ${testCredentials} \"operation\":\"add_sequence_alteration\",\"features\":[{\"residues\":\"CCC\",\"location\":{\"fmin\":689699,\"strand\":1,\"fmax\":689702},\"type\":{\"name\":\"substitution_artifact\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add all these annotations"
        requestHandlingService.addTranscript(JSON.parse(gene1transcript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(gene1transcript2String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(gene2transcript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(gene2transcript2String) as JSONObject)

        requestHandlingService.addFeature(JSON.parse(pseudogeneString) as JSONObject)
        requestHandlingService.addFeature(JSON.parse(tRNAString) as JSONObject)
        requestHandlingService.addFeature(JSON.parse(snRNAString) as JSONObject)
        requestHandlingService.addFeature(JSON.parse(snoRNAString) as JSONObject)
        requestHandlingService.addFeature(JSON.parse(ncRNAString) as JSONObject)
        requestHandlingService.addFeature(JSON.parse(rRNAString) as JSONObject)
        requestHandlingService.addFeature(JSON.parse(miRNAString) as JSONObject)
        requestHandlingService.addFeature(JSON.parse(repeatRegionString) as JSONObject)
        requestHandlingService.addFeature(JSON.parse(transposableElementString) as JSONObject)

        /*
        disabling since the loaded SO doesn't have the following ontology terms:
            - sequence_alteration_artifact
            - insertion_artifact
            - deletion_artifact
            - substitution_artifact
         */

        //requestHandlingService.addSequenceAlteration(JSON.parse(insertionString) as JSONObject)
        //requestHandlingService.addSequenceAlteration(JSON.parse(deletionString) as JSONObject)
        //requestHandlingService.addSequenceAlteration(JSON.parse(substitutionString) as JSONObject)

        then: "we should see 9 genes and 1 repeat region, 1 transposable element and 3 sequence alterations"
        assert Gene.count == 9
        assert RepeatRegion.count == 1
        assert TransposableElement.count == 1
        //assert SequenceAlterationArtifact.count == 3

        when: "we try to export these annotations as Chado"
        def features = []
        Feature.all.each {
            if (!it.childFeatureRelationships) {
                // fetching only top-level features
                features.add(it)
            }
        }
        log.debug "${features}"
        chadoHandlerService.writeFeatures(Organism.findByCommonName("sampleAnimal"), Sequence.all, features)


        then: "we should see the exported annotations in Chado data source"
        assert org.gmod.chado.Organism.count == 1
        assert org.gmod.chado.Feature.count > 0
        assert org.gmod.chado.Featureloc.count > 0
        assert org.gmod.chado.FeatureRelationship.count > 0
    }

    void "test CHADO export for annotations with additional information"() {

        if (! configWrapperService.isPostgresChadoDataSource()) {
            log.debug "Skipping test as the currently specified Chado data source is not PostgreSQL."
            return
        }

        given: "series of different types of annotations"
        String gene1transcript1String = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":102008,\"strand\":1,\"fmax\":111044},\"name\":\"GB40794-RA\",\"children\":[{\"location\":{\"fmin\":102008,\"strand\":1,\"fmax\":102355},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":107137,\"strand\":1,\"fmax\":111044},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":102008,\"strand\":1,\"fmax\":102410},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":104075,\"strand\":1,\"fmax\":104390},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":104585,\"strand\":1,\"fmax\":104962},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":105289,\"strand\":1,\"fmax\":105448},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":105700,\"strand\":1,\"fmax\":106018},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":107017,\"strand\":1,\"fmax\":111044},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":102355,\"strand\":1,\"fmax\":107137},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String gene1transcript2String = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":102008,\"strand\":1,\"fmax\":111044},\"name\":\"5:geneid_mRNA_CM000054.5_411\",\"children\":[{\"location\":{\"fmin\":102008,\"strand\":1,\"fmax\":102410},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":104075,\"strand\":1,\"fmax\":104390},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":104585,\"strand\":1,\"fmax\":104962},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":105289,\"strand\":1,\"fmax\":105448},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":105700,\"strand\":1,\"fmax\":106018},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":107017,\"strand\":1,\"fmax\":111044},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":102008,\"strand\":1,\"fmax\":102355},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":107137,\"strand\":1,\"fmax\":111044},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":102355,\"strand\":1,\"fmax\":107137},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String addNonReservedPropertyString = "{ ${testCredentials} \"operation\":\"add_non_reserved_properties\",\"features\":[{\"non_reserved_properties\":[{\"tag\":\"@TAG@\",\"value\":\"@VALUE@\"}],\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\"}"
        String addNonPrimaryDbxrefString = "{ ${testCredentials} \"operation\":\"add_non_primary_dbxrefs\",\"features\":[{\"dbxrefs\":[{\"db\":\"@DB@\",\"accession\":\"@ACCESSION@\"}],\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\"}"
        String addPublicationString = "{ ${testCredentials} \"operation\":\"add_non_primary_dbxrefs\",\"features\":[{\"dbxrefs\":[{\"db\":\"PMID\",\"accession\":\"@ACCESSION@\"}],\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\"}"
        String addGeneOntologyString = "{ ${testCredentials} \"operation\":\"add_non_primary_dbxrefs\",\"features\":[{\"dbxrefs\":[{\"db\":\"GO\",\"accession\":\"@ACCESSION@\"}],\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\"}"
        String addCommentString = "{ ${testCredentials} \"operation\":\"add_comments\",\"features\":[{\"uniquename\":\"@UNIQUENAME@\",\"comments\":[\"@COMMENT@\"]}],\"track\":\"Group1.10\"}"
        String addSymbolString = "{ ${testCredentials} \"operation\":\"set_symbol\",\"features\":[{\"symbol\":\"@SYMBOL@\",\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\"}"
        String addDescriptionString = "{ ${testCredentials} \"operation\":\"set_description\",\"features\":[{\"description\":\"@DESCRIPTION@\",\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\"}"

        when: "we add all the annotations"
        requestHandlingService.addTranscript(JSON.parse(gene1transcript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(gene1transcript2String) as JSONObject)

        then: "we should see genes and mRNAs"
        assert Gene.count == 1
        assert MRNA.count == 2


        Gene gene1 = Gene.findByName("GB40794-RA")
        MRNA mrna1 = MRNA.findByName("GB40794-RA-00001")

        when: "we add dbXrefs"
        String gene1DbxrefString = addNonPrimaryDbxrefString.replace("@UNIQUENAME@", gene1.uniqueName).replace("@DB@", "NCBI").replace("@ACCESSION@", "1312415")
        String transcript1DbxrefString1 = addNonPrimaryDbxrefString.replace("@UNIQUENAME@", mrna1.uniqueName).replace("@DB@", "NCBI").replace("@ACCESSION@", "XM2131231.1")
        String transcript1DbxrefString2 = addNonPrimaryDbxrefString.replace("@UNIQUENAME@", mrna1.uniqueName).replace("@DB@", "Ensembl").replace("@ACCESSION@", "ENSBTAT000000123")

        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(gene1DbxrefString) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(transcript1DbxrefString1) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(transcript1DbxrefString2) as JSONObject)

        then: "we should see the dbxrefs"
        DBXref.count == 3

        when: "we add properties"
        String gene1PropertyString1 = addNonReservedPropertyString.replace("@UNIQUENAME@", gene1.uniqueName).replace("@TAG@", "score").replace("@VALUE@", "256.7")
        String gene1PropertyString2 = addNonReservedPropertyString.replace("@UNIQUENAME@", gene1.uniqueName).replace("@TAG@", "score").replace("@VALUE@", "9302")
        String transcript1PropertyString1 = addNonReservedPropertyString.replace("@UNIQUENAME@", mrna1.uniqueName).replace("@TAG@", "score").replace("@VALUE@", "11.2")
        String transcript1PropertyString2 = addNonReservedPropertyString.replace("@UNIQUENAME@", mrna1.uniqueName).replace("@TAG@", "score").replace("@VALUE@", "42")

        requestHandlingService.addNonReservedProperties(JSON.parse(gene1PropertyString1) as JSONObject)
        requestHandlingService.addNonReservedProperties(JSON.parse(gene1PropertyString2) as JSONObject)
        requestHandlingService.addNonReservedProperties(JSON.parse(transcript1PropertyString1) as JSONObject)
        requestHandlingService.addNonReservedProperties(JSON.parse(transcript1PropertyString2) as JSONObject)

        then: "we should see 3 feature properties"
        FeatureProperty.count == 4

        when: "we add publications"
        String gene1PublicationString = addPublicationString.replace("@UNIQUENAME@", gene1.uniqueName).replace("@ACCESSION@", "8324934")
        String transcript1PublicationString = addPublicationString.replace("@UNIQUENAME@", mrna1.uniqueName).replace("@ACCESSION@", "798424")

        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(gene1PublicationString) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(transcript1PublicationString) as JSONObject)

        then: "we should see publications"
        // Unfortunately Apollo puts publications into DBxrefs
        DBXref.count == 5
        Publication.count == 0

        when: "we add GO"
        String gene1GeneOntologyString = addGeneOntologyString.replace("@UNIQUENAME@", gene1.uniqueName).replace("@ACCESSION@", "0015755")
        String transcript1GeneOntologyString1 = addGeneOntologyString.replace("@UNIQUENAME@", mrna1.uniqueName).replace("@ACCESSION@", "0015755")
        String transcript1GeneOntologyString2 = addGeneOntologyString.replace("@UNIQUENAME@", mrna1.uniqueName).replace("@ACCESSION@", "0030393")

        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(gene1GeneOntologyString) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(transcript1GeneOntologyString1) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(transcript1GeneOntologyString2) as JSONObject)

        then: "we should see GO in DBxref"
        assert DBXref.count == 7

        when: "we add comments"
        String gene1CommentString = addCommentString.replace("@UNIQUENAME@", gene1.uniqueName).replace("@COMMENT@", "Comment for gene")
        String transcript1CommentString = addCommentString.replace("@UNIQUENAME@", mrna1.uniqueName).replace("@COMMENT@", "Comment for transcript isoform")

        requestHandlingService.addComments(JSON.parse(gene1CommentString) as JSONObject)
        requestHandlingService.addComments(JSON.parse(transcript1CommentString) as JSONObject)

        then: "we should see comments"
        assert FeatureProperty.count == 6

        when: "we add symbols"
        requestHandlingService.setSymbol(JSON.parse(addSymbolString.replace("@UNIQUENAME@", gene1.uniqueName).replace("@SYMBOL@", "GN1S")) as JSONObject)
        requestHandlingService.setDescription(JSON.parse(addDescriptionString.replace("@UNIQUENAME@", gene1.uniqueName).replace("@DESCRIPTION@", "GN1S gene for test")) as JSONObject)
        requestHandlingService.setSymbol(JSON.parse(addSymbolString.replace("@UNIQUENAME@", mrna1.uniqueName).replace("@SYMBOL@", "GN1S-RA")) as JSONObject)
        requestHandlingService.setDescription(JSON.parse(addDescriptionString.replace("@UNIQUENAME@", mrna1.uniqueName).replace("@DESCRIPTION@", "GN1S isoform 1")) as JSONObject)

        then: "we should see the symbol and description attribute set"
        assert gene1.symbol == "GN1S"
        assert gene1.description == "GN1S gene for test"
        assert mrna1.symbol == "GN1S-RA"
        assert mrna1.description == "GN1S isoform 1"

        when: "we try Chado export"
        def features = []
        Feature.all.each {
            if (!it.childFeatureRelationships) {
                // fetching only top-level features
                features.add(it)
            }
        }
        chadoHandlerService.writeFeatures(Organism.findByCommonName("sampleAnimal"), Sequence.all, features)

        then: "we should see features in Chado data source"
        assert org.gmod.chado.Feature.count > 0
        assert org.gmod.chado.Featureloc.count > 0
        assert org.gmod.chado.Featureprop.count > 0
        assert org.gmod.chado.FeatureRelationship.count > 0

    }

    void "test CHADO export and re-export"() {

        if (! configWrapperService.isPostgresChadoDataSource()) {
            log.debug "Skipping test as the currently specified Chado data source is not PostgreSQL."
            return
        }

        given: "a set of annotations"
        String addTranscriptString1 = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":787022,\"strand\":-1,\"fmax\":836988},\"name\":\"GB40740-RA\",\"children\":[{\"location\":{\"fmin\":787022,\"strand\":-1,\"fmax\":787740},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":787022,\"strand\":-1,\"fmax\":788349},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":789768,\"strand\":-1,\"fmax\":790242},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":791007,\"strand\":-1,\"fmax\":791466},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":791853,\"strand\":-1,\"fmax\":792220},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":793652,\"strand\":-1,\"fmax\":793876},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":806935,\"strand\":-1,\"fmax\":807266},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":828378,\"strand\":-1,\"fmax\":829272},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":836953,\"strand\":-1,\"fmax\":836988},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":787740,\"strand\":-1,\"fmax\":836988},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addTranscriptString2 = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":845782,\"strand\":-1,\"fmax\":847278},\"name\":\"GB40739-RA\",\"children\":[{\"location\":{\"fmin\":845782,\"strand\":-1,\"fmax\":845798},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":847144,\"strand\":-1,\"fmax\":847278},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":845782,\"strand\":-1,\"fmax\":847278},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addFeatureString1 = "{ ${testCredentials} \"operation\":\"add_feature\",\"features\":[{\"location\":{\"fmin\":772054,\"strand\":1,\"fmax\":776061},\"children\":[{\"location\":{\"fmin\":772054,\"strand\":1,\"fmax\":776061},\"children\":[{\"location\":{\"fmin\":775816,\"strand\":1,\"fmax\":776061},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":772054,\"strand\":1,\"fmax\":772102},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":772808,\"strand\":1,\"fmax\":772942},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":773319,\"strand\":1,\"fmax\":773539},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":773682,\"strand\":1,\"fmax\":773848},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":774138,\"strand\":1,\"fmax\":774504},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":774865,\"strand\":1,\"fmax\":775227},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":775543,\"strand\":1,\"fmax\":776061},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":772054,\"strand\":1,\"fmax\":775816},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"transcript\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"pseudogene\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addFeatureString2 = "{ ${testCredentials} \"operation\":\"add_feature\",\"features\":[{\"location\":{\"fmin\":761542,\"strand\":-1,\"fmax\":768063},\"children\":[{\"location\":{\"fmin\":761542,\"strand\":-1,\"fmax\":768063},\"children\":[{\"location\":{\"fmin\":767945,\"strand\":-1,\"fmax\":768063},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":761542,\"strand\":-1,\"fmax\":763070},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":761542,\"strand\":-1,\"fmax\":763513},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":765327,\"strand\":-1,\"fmax\":765472},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":765551,\"strand\":-1,\"fmax\":766176},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":766255,\"strand\":-1,\"fmax\":767133},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":767207,\"strand\":-1,\"fmax\":767389},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":767485,\"strand\":-1,\"fmax\":768063},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":763070,\"strand\":-1,\"fmax\":767945},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"ncRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"gene\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addFeatureString3 = "{ ${testCredentials} \"operation\":\"add_feature\",\"features\":[{\"location\":{\"fmin\":747699,\"strand\":0,\"fmax\":747966},\"type\":{\"name\":\"repeat_region\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addFeatureString4 = "{ ${testCredentials} \"operation\":\"add_feature\",\"features\":[{\"location\":{\"fmin\":734606,\"strand\":0,\"fmax\":735570},\"type\":{\"name\":\"transposable_element\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addTranscriptString3 = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":891279,\"strand\":-1,\"fmax\":933037},\"name\":\"GB40737-RA\",\"children\":[{\"location\":{\"fmin\":891279,\"strand\":-1,\"fmax\":891532},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":893536,\"strand\":-1,\"fmax\":893577},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":931384,\"strand\":-1,\"fmax\":931630},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":932998,\"strand\":-1,\"fmax\":933037},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":891279,\"strand\":-1,\"fmax\":933037},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String addNonReservedPropertyString = "{ ${testCredentials} \"operation\":\"add_non_reserved_properties\",\"features\":[{\"non_reserved_properties\":[{\"tag\":\"@TAG@\",\"value\":\"@VALUE@\"}],\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\"}"
        String addNonPrimaryDbxrefString = "{ ${testCredentials} \"operation\":\"add_non_primary_dbxrefs\",\"features\":[{\"dbxrefs\":[{\"db\":\"@DB@\",\"accession\":\"@ACCESSION@\"}],\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\"}"
        String addPublicationString = "{ ${testCredentials} \"operation\":\"add_non_primary_dbxrefs\",\"features\":[{\"dbxrefs\":[{\"db\":\"PMID\",\"accession\":\"@ACCESSION@\"}],\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\"}"
        String addGeneOntologyString = "{ ${testCredentials} \"operation\":\"add_non_primary_dbxrefs\",\"features\":[{\"dbxrefs\":[{\"db\":\"GO\",\"accession\":\"@ACCESSION@\"}],\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\"}"
        String addCommentString = "{ ${testCredentials} \"operation\":\"add_comments\",\"features\":[{\"uniquename\":\"@UNIQUENAME@\",\"comments\":[\"@COMMENT@\"]}],\"track\":\"Group1.10\"}"
        String addSymbolString = "{ ${testCredentials} \"operation\":\"set_symbol\",\"features\":[{\"symbol\":\"@SYMBOL@\",\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\"}"
        String addDescriptionString = "{ ${testCredentials} \"operation\":\"set_description\",\"features\":[{\"description\":\"@DESCRIPTION@\",\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\"}"

        when: "we add all the annotations"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString1) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString2) as JSONObject)

        requestHandlingService.addFeature(JSON.parse(addFeatureString1) as JSONObject)
        requestHandlingService.addFeature(JSON.parse(addFeatureString2) as JSONObject)
        requestHandlingService.addFeature(JSON.parse(addFeatureString3) as JSONObject)
        requestHandlingService.addFeature(JSON.parse(addFeatureString4) as JSONObject)

        then: "we should see 4 genes, 1 repeat region and 1 transposable element"
        assert Gene.count == 4
        assert RepeatRegion.count == 1
        assert TransposableElement.count == 1
        Gene gene1 = Gene.findByName("GB40740-RA")
        MRNA mrna1 = MRNA.findByName("GB40740-RA-00001")
        String gene1UniqueName = gene1.uniqueName
        String mrna1UniqueName = mrna1.uniqueName
        Gene gene2 = Gene.findByName("GB40739-RA")
        MRNA mrna2 = MRNA.findByName("GB40739-RA-00001")

        when: "we add metadata"
        requestHandlingService.setSymbol(JSON.parse(addSymbolString.replace("@UNIQUENAME@", gene1.uniqueName).replace("@SYMBOL@", "GN1A")) as JSONObject)
        requestHandlingService.setSymbol(JSON.parse(addSymbolString.replace("@UNIQUENAME@", gene2.uniqueName).replace("@SYMBOL@", "GN1B")) as JSONObject)
        requestHandlingService.setSymbol(JSON.parse(addSymbolString.replace("@UNIQUENAME@", mrna1.uniqueName).replace("@SYMBOL@", "GN1A isoform 1")) as JSONObject)
        requestHandlingService.setSymbol(JSON.parse(addSymbolString.replace("@UNIQUENAME@", mrna2.uniqueName).replace("@SYMBOL@", "GN1B isoform 1")) as JSONObject)

        requestHandlingService.setDescription(JSON.parse(addDescriptionString.replace("@UNIQUENAME@", gene1.uniqueName).replace("@DESCRIPTION@", "Test gene GN1A")) as JSONObject)
        requestHandlingService.setDescription(JSON.parse(addDescriptionString.replace("@UNIQUENAME@", gene2.uniqueName).replace("@DESCRIPTION@", "Test gene GN1B")) as JSONObject)
        requestHandlingService.setDescription(JSON.parse(addDescriptionString.replace("@UNIQUENAME@", mrna1.uniqueName).replace("@DESCRIPTION@", "Test transcript isoform GN1A")) as JSONObject)
        requestHandlingService.setDescription(JSON.parse(addDescriptionString.replace("@UNIQUENAME@", mrna2.uniqueName).replace("@DESCRIPTION@", "Test transcript isoform GN1B")) as JSONObject)

        requestHandlingService.addComments(JSON.parse(addCommentString.replace("@UNIQUENAME@", gene1.uniqueName).replace("@COMMENT@", "Gene GN1A is created for test purposes")) as JSONObject)
        requestHandlingService.addComments(JSON.parse(addCommentString.replace("@UNIQUENAME@", gene2.uniqueName).replace("@COMMENT@", "Gene GN1B is created for test purposes")) as JSONObject)
        requestHandlingService.addComments(JSON.parse(addCommentString.replace("@UNIQUENAME@", mrna1.uniqueName).replace("@COMMENT@", "Transcript GN1A isoform 1 is created for test purposes")) as JSONObject)
        requestHandlingService.addComments(JSON.parse(addCommentString.replace("@UNIQUENAME@", mrna2.uniqueName).replace("@COMMENT@", "Transcript GN1B isoform 1 is created for test purposes")) as JSONObject)

        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addNonPrimaryDbxrefString.replace("@UNIQUENAME@", gene1.uniqueName).replace("@DB@", "NCBI").replace("@ACCESSION@", "1312415")) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addNonPrimaryDbxrefString.replace("@UNIQUENAME@", gene2.uniqueName).replace("@DB@", "NCBI").replace("@ACCESSION@", "3235223")) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addNonPrimaryDbxrefString.replace("@UNIQUENAME@", gene1.uniqueName).replace("@DB@", "Ensembl").replace("@ACCESSION@", "ENSG00000000012")) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addNonPrimaryDbxrefString.replace("@UNIQUENAME@", gene2.uniqueName).replace("@DB@", "Ensembl").replace("@ACCESSION@", "ENSG00000000014")) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addNonPrimaryDbxrefString.replace("@UNIQUENAME@", mrna1.uniqueName).replace("@DB@", "Ensembl").replace("@ACCESSION@", "ENST0000000032521")) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addNonPrimaryDbxrefString.replace("@UNIQUENAME@", mrna2.uniqueName).replace("@DB@", "Ensembl").replace("@ACCESSION@", "ENST00000000325")) as JSONObject)

        requestHandlingService.addNonReservedProperties(JSON.parse(addNonReservedPropertyString.replace("@UNIQUENAME@", gene1.uniqueName).replace("@TAG@", "validated").replace("@VALUE@", "false")) as JSONObject)
        requestHandlingService.addNonReservedProperties(JSON.parse(addNonReservedPropertyString.replace("@UNIQUENAME@", mrna1.uniqueName).replace("@TAG@", "validated").replace("@VALUE@", "false")) as JSONObject)

        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addPublicationString.replace("@UNIQUENAME@", gene1.uniqueName).replace("@ACCESSION@", "8324934")) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addPublicationString.replace("@UNIQUENAME@", mrna1.uniqueName).replace("@ACCESSION@", "798424")) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addGeneOntologyString.replace("@UNIQUENAME@", gene1.uniqueName).replace("@ACCESSION@", "0015755")) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addGeneOntologyString.replace("@UNIQUENAME@", mrna1.uniqueName).replace("@ACCESSION@", "0030393")) as JSONObject)

        then: "we should see these metadata"
        gene1.refresh()
        gene2.refresh()
        assert gene1.symbol != null
        assert gene1.description != null
        assert gene2.symbol != null
        assert gene2.description != null
        assert gene1.featureProperties.size() != 0
        assert gene2.featureProperties.size() != 0
        assert gene1.featureDBXrefs.size() != 0
        assert gene2.featureDBXrefs.size() != 0

        when: "we do a Chado export"
        def features = []
        Feature.all.each {
            if (!it.childFeatureRelationships) {
                features.add(it)
            }
        }
        chadoHandlerService.writeFeatures(Organism.findByCommonName("sampleAnimal"), Sequence.all, features)

        then: "we should see the annotations in Chado data source"
        assert org.gmod.chado.Feature.count > 0
        assert org.gmod.chado.Featureloc.count > 0
        assert org.gmod.chado.Featureprop.count > 0
        assert org.gmod.chado.FeatureDbxref.count > 0
        assert org.gmod.chado.FeatureRelationship.count > 0
        assert org.gmod.chado.Feature.findByUniquename(gene1.uniqueName) != null
        assert org.gmod.chado.Feature.findByUniquename(gene2.uniqueName) != null


        when: "we add GB40737-RA and re-export to Chado"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString3) as JSONObject)
        Gene gene3 = Gene.findByName("GB40737-RA")

        features = []
        Feature.all.each {
            if (!it.childFeatureRelationships) {
                features.add(it)
            }
        }

        chadoHandlerService.writeFeatures(Organism.findByCommonName("sampleAnimal"), Sequence.all, features)

        then: "we should find GB40737-RA in the Chado data source"
        assert org.gmod.chado.Feature.findByUniquename(gene3.uniqueName) != null

        when: "we delete an annotation and do another Chado export"
        gene1.refresh()
        featureRelationshipService.deleteFeatureAndChildren(gene1)
        features = []
        Feature.all.each {
            if (!it.childFeatureRelationships) {
                features.add(it)
            }
        }

        chadoHandlerService.writeFeatures(Organism.findByCommonName("sampleAnimal"), Sequence.all, features)

        then: "GB40740-RA, its dbxrefs and feature properties should not exist in Chado"
        assert org.gmod.chado.Feature.findByUniquename(gene1UniqueName) == null
        assert org.gmod.chado.Feature.findByUniquename(mrna1UniqueName) == null
        assert org.gmod.chado.Dbxref.findByAccession("1312415") == null
        assert org.gmod.chado.Dbxref.findByAccession("ENSG00000000012") == null
        assert org.gmod.chado.Featureprop.findByValue("Gene GN1A is created for test purposes") == null

        // and that GO terms are not deleted as a result
        assert org.gmod.chado.Dbxref.findByAccession("0015755") != null
        assert org.gmod.chado.Dbxref.findByAccession("0030393") != null
    }
}
