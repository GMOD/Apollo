package org.bbop.apollo

import grails.converters.JSON
import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.web.json.JSONObject
import spock.lang.Ignore

class ChadoHandlerServiceIntegrationSpec extends IntegrationSpec {

    // NOTE: This is set to prevent rollback at the end of the integration test
    // for the sake of visual inspection of the database at the end of the test.
    //static transactional = false

    def chadoHandlerService
    def requestHandlingService
    def featureRelationshipService
    def featurePropertyService
    def transcriptService
    def exonService
    def sequenceService
    def configWrapperService

    def setup() {
        Organism organism = new Organism(
                directory: "test/integration/resources/sequences/honeybee-Group1.10/",
                genus: "Apis",
                species: "mellifera",
                commonName: "honey bee",
                abbreviation: "A. mellifera"
        ).save(flush: true)
        Sequence sequence = new Sequence(
                length: 1405242
                , seqChunkSize: 20000
                , start: 0
                , organism: organism
                , end: 1405242
                , name: "Group1.10"
        ).save(flush: true)
        organism.sequences = [sequence]
        organism.save(flush: true)


    }

    def cleanup() {
    }

    //@Ignore("This test will successfully run only on a prepared data source")
    void "test CHADO export for standard annotations"() {
        /*
        Standard annotations signifies no modifications/attributes added to the annotations
         */
        if (! configWrapperService.isPostgresChadoDataSource()) {
            println "Skipping test as the currently specified Chado data source is not PostgreSQL."
            return
        }

        given: "series of different types of annotations"
        String gene1transcript1String = '{"operation":"add_transcript","features":[{"location":{"fmin":128706,"strand":1,"fmax":136964},"name":"GB40797-RA","children":[{"location":{"fmin":136502,"strand":1,"fmax":136964},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":128706,"strand":1,"fmax":128768},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":131040,"strand":1,"fmax":131220},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":131322,"strand":1,"fmax":131487},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":131749,"strand":1,"fmax":131964},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":132025,"strand":1,"fmax":132264},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":132394,"strand":1,"fmax":132620},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":133815,"strand":1,"fmax":133832},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":134173,"strand":1,"fmax":134353},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":134476,"strand":1,"fmax":134684},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":135693,"strand":1,"fmax":136260},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":136335,"strand":1,"fmax":136964},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":128706,"strand":1,"fmax":136502},"type":{"name":"CDS","cv":{"name":"sequence"}}}],"type":{"name":"mRNA","cv":{"name":"sequence"}}}],"track":"Group1.10"}'
        String gene1transcript2String = '{"operation":"add_transcript","features":[{"location":{"fmin":128706,"strand":1,"fmax":136964},"name":"fgeneshpp_with_rnaseq_Group1.10_31_mRNA","children":[{"location":{"fmin":128706,"strand":1,"fmax":128768},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":131040,"strand":1,"fmax":131220},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":131322,"strand":1,"fmax":131487},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":131749,"strand":1,"fmax":131964},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":132025,"strand":1,"fmax":132264},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":132394,"strand":1,"fmax":132620},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":133815,"strand":1,"fmax":133832},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":134173,"strand":1,"fmax":134353},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":134476,"strand":1,"fmax":134684},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":135693,"strand":1,"fmax":136260},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":136335,"strand":1,"fmax":136964},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":136502,"strand":1,"fmax":136964},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":128706,"strand":1,"fmax":136502},"type":{"name":"CDS","cv":{"name":"sequence"}}}],"type":{"name":"mRNA","cv":{"name":"sequence"}}}],"track":"Group1.10"}'

        String gene2transcript1String = '{"operation":"add_transcript","features":[{"location":{"fmin":252004,"strand":-1,"fmax":255153},"name":"GB40770-RA","children":[{"location":{"fmin":255124,"strand":-1,"fmax":255153},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":252400,"strand":-1,"fmax":252408},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":252004,"strand":-1,"fmax":252303},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":252004,"strand":-1,"fmax":252303},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":252400,"strand":-1,"fmax":252462},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":252667,"strand":-1,"fmax":252814},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":253468,"strand":-1,"fmax":253601},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":253751,"strand":-1,"fmax":253878},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":254826,"strand":-1,"fmax":255153},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":252408,"strand":-1,"fmax":255124},"type":{"name":"CDS","cv":{"name":"sequence"}}}],"type":{"name":"mRNA","cv":{"name":"sequence"}}}],"track":"Group1.10"}'
        String gene2transcript2String = '{"operation":"add_transcript","features":[{"location":{"fmin":252004,"strand":-1,"fmax":255153},"name":"fgeneshpp_with_rnaseq_Group1.10_62_mRNA","children":[{"location":{"fmin":252004,"strand":-1,"fmax":252303},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":252400,"strand":-1,"fmax":252462},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":252667,"strand":-1,"fmax":252814},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":253468,"strand":-1,"fmax":253601},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":253751,"strand":-1,"fmax":253878},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":254826,"strand":-1,"fmax":255153},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":252400,"strand":-1,"fmax":252408},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":252004,"strand":-1,"fmax":252303},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":255124,"strand":-1,"fmax":255153},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":252408,"strand":-1,"fmax":255124},"type":{"name":"CDS","cv":{"name":"sequence"}}}],"type":{"name":"mRNA","cv":{"name":"sequence"}}}],"track":"Group1.10"}'

        String pseudogeneString = '{"operation":"add_feature","features":[{"location":{"fmin":747699,"strand":1,"fmax":747966},"children":[{"location":{"fmin":747699,"strand":1,"fmax":747966},"children":[{"location":{"fmin":747699,"strand":1,"fmax":747760},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":747822,"strand":1,"fmax":747894},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":747946,"strand":1,"fmax":747966},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":747699,"strand":1,"fmax":747966},"type":{"name":"CDS","cv":{"name":"sequence"}}}],"type":{"name":"transcript","cv":{"name":"sequence"}}}],"type":{"name":"pseudogene","cv":{"name":"sequence"}}}],"track":"Group1.10"}'
        String tRNAString = '{"operation":"add_feature","features":[{"location":{"fmin":775035,"strand":-1,"fmax":775413},"children":[{"location":{"fmin":775035,"strand":-1,"fmax":775413},"children":[{"location":{"fmin":775035,"strand":-1,"fmax":775185},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":775344,"strand":-1,"fmax":775413},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":775035,"strand":-1,"fmax":775413},"type":{"name":"CDS","cv":{"name":"sequence"}}}],"type":{"name":"tRNA","cv":{"name":"sequence"}}}],"type":{"name":"gene","cv":{"name":"sequence"}}}],"track":"Group1.10"}'
        String snRNAString = '{"operation":"add_feature","features":[{"location":{"fmin":278556,"strand":1,"fmax":281052},"children":[{"location":{"fmin":278556,"strand":1,"fmax":281052},"name":"fgeneshpp_with_rnaseq_Group1.10_67_mRNA","children":[{"location":{"fmin":278556,"strand":1,"fmax":278569},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":280446,"strand":1,"fmax":280615},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":280723,"strand":1,"fmax":281052},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":280550,"strand":1,"fmax":280615},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":280723,"strand":1,"fmax":281052},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":278556,"strand":1,"fmax":280550},"type":{"name":"CDS","cv":{"name":"sequence"}}}],"type":{"name":"snRNA","cv":{"name":"sequence"}}}],"type":{"name":"gene","cv":{"name":"sequence"}}}],"track":"Group1.10"}'
        String snoRNAString = '{"operation":"add_feature","features":[{"location":{"fmin":433518,"strand":1,"fmax":437436},"children":[{"location":{"fmin":433518,"strand":1,"fmax":437436},"children":[{"location":{"fmin":433518,"strand":1,"fmax":433570},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":436576,"strand":1,"fmax":436641},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":437424,"strand":1,"fmax":437436},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":433518,"strand":1,"fmax":437436},"type":{"name":"CDS","cv":{"name":"sequence"}}}],"type":{"name":"snoRNA","cv":{"name":"sequence"}}}],"type":{"name":"gene","cv":{"name":"sequence"}}}],"track":"Group1.10"}'
        String ncRNAString = '{"operation":"add_feature","features":[{"location":{"fmin":598161,"strand":-1,"fmax":598924},"children":[{"location":{"fmin":598161,"strand":-1,"fmax":598924},"children":[{"location":{"fmin":598161,"strand":-1,"fmax":598280},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":598782,"strand":-1,"fmax":598924},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":598161,"strand":-1,"fmax":598924},"type":{"name":"CDS","cv":{"name":"sequence"}}}],"type":{"name":"ncRNA","cv":{"name":"sequence"}}}],"type":{"name":"gene","cv":{"name":"sequence"}}}],"track":"Group1.10"}'
        String rRNAString = '{"operation":"add_feature","features":[{"location":{"fmin":664614,"strand":-1,"fmax":665729},"children":[{"location":{"fmin":664614,"strand":-1,"fmax":665729},"children":[{"location":{"fmin":664614,"strand":-1,"fmax":664637},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":665671,"strand":-1,"fmax":665729},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":664614,"strand":-1,"fmax":665729},"type":{"name":"CDS","cv":{"name":"sequence"}}}],"type":{"name":"rRNA","cv":{"name":"sequence"}}}],"type":{"name":"gene","cv":{"name":"sequence"}}}],"track":"Group1.10"}'
        String miRNAString = '{"operation":"add_feature","features":[{"location":{"fmin":675719,"strand":-1,"fmax":680586},"children":[{"location":{"fmin":675719,"strand":-1,"fmax":680586},"children":[{"location":{"fmin":675719,"strand":-1,"fmax":676397},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":675719,"strand":-1,"fmax":676397},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":678693,"strand":-1,"fmax":680586},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":678693,"strand":-1,"fmax":680586},"type":{"name":"CDS","cv":{"name":"sequence"}}}],"type":{"name":"miRNA","cv":{"name":"sequence"}}}],"type":{"name":"gene","cv":{"name":"sequence"}}}],"track":"Group1.10"}'
        String repeatRegionString = '{"operation":"add_feature","features":[{"location":{"fmin":654601,"strand":0,"fmax":657144},"type":{"name":"repeat_region","cv":{"name":"sequence"}}}],"track":"Group1.10"}'
        String transposableElementString = '{"operation":"add_feature","features":[{"location":{"fmin":621650,"strand":0,"fmax":628275},"type":{"name":"transposable_element","cv":{"name":"sequence"}}}],"track":"Group1.10"}'

        String insertionString = '{"operation":"add_sequence_alteration","features":[{"residues":"ATCG","location":{"fmin":689758,"strand":1,"fmax":689758},"type":{"name":"insertion","cv":{"name":"sequence"}}}],"track":"Group1.10"}'
        String deletionString = '{"operation":"add_sequence_alteration","features":[{"location":{"fmin":689725,"strand":1,"fmax":689735},"type":{"name":"deletion","cv":{"name":"sequence"}}}],"track":"Group1.10"}'
        String substitutionString = '{"operation":"add_sequence_alteration","features":[{"residues":"CCC","location":{"fmin":689699,"strand":1,"fmax":689702},"type":{"name":"substitution","cv":{"name":"sequence"}}}],"track":"Group1.10"}'

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

        requestHandlingService.addSequenceAlteration(JSON.parse(insertionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(deletionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(substitutionString) as JSONObject)

        then: "we should see 9 genes and 1 repeat region, 1 transposable element and 3 sequence alterations"
        assert Gene.count == 9
        assert RepeatRegion.count == 1
        assert TransposableElement.count == 1
        assert SequenceAlteration.count == 3

        when: "we try to export these annotations as Chado"
        def features = []
        Feature.all.each {
            if (!it.childFeatureRelationships) {
                // fetching only top-level features
                features.add(it)
            }
        }
        chadoHandlerService.writeFeatures(Organism.findByCommonName("honey bee"), Sequence.all, features)


        then: "we should see the exported annotations in Chado data source"
        assert org.gmod.chado.Organism.count == 1
        assert org.gmod.chado.Feature.count > 0
        assert org.gmod.chado.Featureloc.count > 0
        assert org.gmod.chado.FeatureRelationship.count > 0
    }

    void "test CHADO export for annotations with additional information"() {

        if (! configWrapperService.isPostgresChadoDataSource()) {
            println "Skipping test as the currently specified Chado data source is not PostgreSQL."
            return
        }

        given: "series of different types of annotations"
        String gene1transcript1String = '{"operation":"add_transcript","features":[{"location":{"fmin":102008,"strand":1,"fmax":111044},"name":"GB40794-RA","children":[{"location":{"fmin":102008,"strand":1,"fmax":102355},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":107137,"strand":1,"fmax":111044},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":102008,"strand":1,"fmax":102410},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":104075,"strand":1,"fmax":104390},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":104585,"strand":1,"fmax":104962},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":105289,"strand":1,"fmax":105448},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":105700,"strand":1,"fmax":106018},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":107017,"strand":1,"fmax":111044},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":102355,"strand":1,"fmax":107137},"type":{"name":"CDS","cv":{"name":"sequence"}}}],"type":{"name":"mRNA","cv":{"name":"sequence"}}}],"track":"Group1.10"}'
        String gene1transcript2String = '{"operation":"add_transcript","features":[{"location":{"fmin":102008,"strand":1,"fmax":111044},"name":"5:geneid_mRNA_CM000054.5_411","children":[{"location":{"fmin":102008,"strand":1,"fmax":102410},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":104075,"strand":1,"fmax":104390},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":104585,"strand":1,"fmax":104962},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":105289,"strand":1,"fmax":105448},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":105700,"strand":1,"fmax":106018},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":107017,"strand":1,"fmax":111044},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":102008,"strand":1,"fmax":102355},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":107137,"strand":1,"fmax":111044},"type":{"name":"exon","cv":{"name":"sequence"}}},{"location":{"fmin":102355,"strand":1,"fmax":107137},"type":{"name":"CDS","cv":{"name":"sequence"}}}],"type":{"name":"mRNA","cv":{"name":"sequence"}}}],"track":"Group1.10"}'

        String addNonReservedPropertyString = '{"operation":"add_non_reserved_properties","features":[{"non_reserved_properties":[{"tag":"@TAG@","value":"@VALUE@"}],"uniquename":"@UNIQUENAME@"}],"track":"Group1.10"}'
        String addNonPrimaryDbxrefString = '{"operation":"add_non_primary_dbxrefs","features":[{"dbxrefs":[{"db":"@DB@","accession":"@ACCESSION@"}],"uniquename":"@UNIQUENAME@"}],"track":"Group1.10"}'
        String addPublicationString = '{"operation":"add_non_primary_dbxrefs","features":[{"dbxrefs":[{"db":"PMID","accession":"@ACCESSION@"}],"uniquename":"@UNIQUENAME@"}],"track":"Group1.10"}'
        String addGeneOntologyString = '{"operation":"add_non_primary_dbxrefs","features":[{"dbxrefs":[{"db":"GO","accession":"@ACCESSION@"}],"uniquename":"@UNIQUENAME@"}],"track":"Group1.10"}'
        String addCommentString = '{"operation":"add_comments","features":[{"uniquename":"@UNIQUENAME@","comments":["@COMMENT@"]}],"track":"Group1.10"}'
        String addSymbolString = '{"operation":"set_symbol","features":[{"symbol":"@SYMBOL@","uniquename":"@UNIQUENAME@"}],"track":"Group1.10"}'
        String addDescriptionString = '{"operation":"set_description","features":[{"description":"@DESCRIPTION@","uniquename":"@UNIQUENAME@"}],"track":"Group1.10"}'

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
        chadoHandlerService.writeFeatures(Organism.findByCommonName("honey bee"), Sequence.all, features)

        then: "we should see features in Chado data source"
        assert org.gmod.chado.Feature.count > 0
        assert org.gmod.chado.Featureloc.count > 0
        assert org.gmod.chado.Featureprop.count > 0
        assert org.gmod.chado.FeatureRelationship.count > 0

    }
}
