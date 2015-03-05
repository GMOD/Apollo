package org.bbop.apollo

import grails.transaction.Transactional
import org.apache.shiro.crypto.hash.Sha256Hash
//import grails.compiler.GrailsCompileStatic

//@GrailsCompileStatic
@Transactional
class MockupService {

//    CvTermService cvTermService


    def addUsers() {
        if (User.count > 0) return
        def userRole = new Role(name: UserService.USER).save()
        userRole.addToPermissions("*:*")
        def adminRole = new Role(name: UserService.ADMIN).save()
        adminRole.addToPermissions("*:*")

//        CVTerm userCvTerm = cvTermService.getTerm(FeatureStringEnum.OWNER.value)


        User demoUser = new User(
                username: "demo@demo.gov"
                , passwordHash: new Sha256Hash("demo").toHex()
//                ,value: "demo@demo.gov"
//                ,type: userCvTerm
        ).save(failOnError: true)
        demoUser.addToRoles(userRole)

        User adminUser = new User(
                username: "admin@admin.gov"
                , passwordHash: new Sha256Hash("admin").toHex()
//                ,value: "admin@admin.gov"
//                ,type: userCvTerm
        ).save(failOnError: true)
        adminUser.addToRoles(userRole)
    }

//    /**
//     * Repalce stuff in mapping.xml
//     */
//    def addTerms() {
//        if (Term.count > 0) return
//        new Term(term: "region", vocabulary: "sequence", readClass: "Region", type: TermTypeEnum.FEATURE_MAPPING).save()
//        new Term(term: "gene", vocabulary: "sequence", readClass: "Gene", type: TermTypeEnum.FEATURE_MAPPING).save()
//        new Term(term: "pseudogene", vocabulary: "sequence", readClass: "Pseudogene", type: TermTypeEnum.FEATURE_MAPPING).save()
//        new Term(term: "transcript", vocabulary: "sequence", readClass: "Transcript", type: TermTypeEnum.FEATURE_MAPPING).save()
//        new Term(term: "mRNA", vocabulary: "sequence", readClass: "MRNA", type: TermTypeEnum.FEATURE_MAPPING).save()
//        new Term(term: "tRNA", vocabulary: "sequence", readClass: "TRNA", type: TermTypeEnum.FEATURE_MAPPING).save()
//        new Term(term: "snRNA", vocabulary: "sequence", readClass: "SnRNA", type: TermTypeEnum.FEATURE_MAPPING).save()
//        new Term(term: "snoRNA", vocabulary: "sequence", readClass: "SnoRNA", type: TermTypeEnum.FEATURE_MAPPING).save()
//        new Term(term: "ncRNA", vocabulary: "sequence", readClass: "NcRNA", type: TermTypeEnum.FEATURE_MAPPING).save()
//        new Term(term: "rRNA", vocabulary: "sequence", readClass: "RRNA", type: TermTypeEnum.FEATURE_MAPPING).save()
//
//        // and several many more
//    }

    def addOrganisms() {
        if (Organism.count > 0) return
        new Organism(abbreviation: "HB",commonName: "Honey Bee", genus: "Amel",species: "dsomething",directory: "/opt/apollo/honeybee/data").save(failOnError: true)
//        new Organism(abbreviation: "ZF",commonName: "Zebrafish", genus: "Danio",species: "rerio",directory: "/opt/zebrafish/jbrowse/data").save(failOnError: true)
        new Organism(abbreviation: "HM",commonName: "Human", genus: "Homo",species: "sapiens",directory: "/opt/apollo/human/data").save(failOnError: true)
        new Organism(abbreviation: "YS",commonName: "Yeast", genus: "Saccharomyces",species: "cerevisiae",directory: "/opt/apollo/yeast/data").save(failOnError: true)
//        new Organism(abbreviation: "PIPE",commonName: "Alligator Pipefish", genus: "Syngnathoides",species: "biaculeatus").save(failOnError: true)
//        new Organism(abbreviation: "BloodSD",commonName: "Bloody Stickleback", genus: "Apeltes",species: "quadracus").save(failOnError: true)
//        new Organism(abbreviation: "BrookSD",commonName: "Brook Stickleback", genus: "Culaea",species: "inconstans").save(failOnError: true)
//        new Organism(abbreviation: "3SD",commonName: "Three-spined Stickleback", genus: "Gasterosteus",species: "aculeatus").save(failOnError: true)
//        new Organism(abbreviation: "ASD",commonName: "Amur Stickleback", genus: "Pungitius",species: "sinensis").save(failOnError: true)
//        new Organism(abbreviation: "SPINSB",commonName: "Spinach Stickleback", genus: "Spinachia",species: "spinachia").save(failOnError: true)
//        Sequence track1 = new Sequence(name: "Annotations-Group1.1", organism: organism
////                , sequenceCV: "sequence", sequenceType: "mRNA"
//                , refSeqFile: "/opt/apollo/jbrowse/data/seq/refSeqs.json"
//                , dataDirectory: "/opt/apollo/jbrowse/data"
////                ,translationTableLocation:
//        ).save(failOnError: true)


        // in the config
//        sourceFeature = new SourceFeatureConfiguration(sequenceDirectory, sequenceChunkSize, sequenceChunkPrefix, sequenceLength, uniqueName, type, start, end);


//        FeatureSequenceChunkManager chunkManager = FeatureSequenceChunkManager.getInstance(track.getName());
//        chunkManager.setSequenceDirectory(track.getSourceFeature().getSequenceDirectory());
//        chunkManager.setChunkSize(track.getSourceFeature().getSequenceChunkSize());
//        chunkManager.setChunkPrefix(track.getSourceFeature().getSequenceChunkPrefix());
//        chunkManager.setSequenceLength(track.getSourceFeature().getEnd());

//        FeatureLazyResidues sourceFeature = new FeatureLazyResidues(track.getName());
//        sourceFeature.setUniqueName(track.getSourceFeature().getUniqueName());
//        sourceFeature.setFmin(track.getSourceFeature().getStart());
//        sourceFeature.setFmax(track.getSourceFeature().getEnd());
//        String[] type = track.getSourceFeature().getType().split(":");
//        sourceFeature.setType(new CVTerm(type[1], new CV(type[0])));

//        Track track2 = new Track(name: "Zebrafish Track 2").save()
//        organism.addToSequences(track1)
//        genome1.addToTracks(track2)

        User demoUser = User.findByUsername("demo@demo.gov")
        User adminUser = User.findByUsername("admin@admin.gov")

//        track1.addToUsers(demoUser)
//        track1.addToUsers(adminUser)

//        Organism genome2 = new Organism(name: "Caenorhabditis elegans").save()
//        Track track3 = new Track(name: "Celegans Track 1").save()
//        genome2.addToTracks(track3)
//        track3.addToUsers(demoUser)
//
//        track3.save(flush:true)
    }

    // {"data_adapters":[{"permission":1,"key":"GFF3","options":"output=file&format=gzip"},
    // {"permission":1,"key":"FASTA","data_adapters":
    // [{"permission":1,"key":"peptide","options":"output=file&format=gzip&seqType=peptide"}
    // ,{"permission":1,"key":"cDNA","options":"output=file&format=gzip&seqType=cdna"}
    // ,{"permission":1,"key":"CDS","options":"output=file&format=gzip&seqType=cds"}]}]}
    def addDataAdapters() {
        if (DataAdapter.count > 0) return
        new DataAdapter(permission: 1, key: "GFF3", options: "output=file&format=gzip").save()
        DataAdapter fastaDataAdapter = new DataAdapter(permission: 1, key: "FASTA").save(failOnError: true,flush: true)
        DataAdapter peptideFastaDataAdapater = new DataAdapter(permission: 1, key: "peptide", options: "output=file&format=gzip&seqType=peptide").save()
        DataAdapter cDNAFastaDataAdapater = new DataAdapter(permission: 1, key: "cDNA", options: "output=file&format=gzip&seqType=cdna").save()
        DataAdapter cdsFastaDataAdapater = new DataAdapter(permission: 1, key: "cDNA", options: "output=file&format=gzip&seqType=cds").save()
        fastaDataAdapter.addToDataAdapters(peptideFastaDataAdapater)
        fastaDataAdapter.addToDataAdapters(cDNAFastaDataAdapater)
        fastaDataAdapter.addToDataAdapters(cdsFastaDataAdapater)
    }

    def addSequences() {

        if(Sequence.count>0 )return


    }

    def addFeatureWithLocations() {
        if(FeatureLocation.count>0 )return

//        Gene gene1 = new Gene( name: "sox9a" ).save(failOnError: true)
        Gene gene1 = new Gene( name: "geneid_mRNA_CM000054.5_3" ).save(failOnError: true)



        FeatureLocation featureLocation1 = new FeatureLocation(
                feature: gene1
                ,fmin: 88365
                ,fmax: 88550
                ,sequence: Sequence.first()
        ).save(failOnError: true)

        gene1.addToFeatureLocations(featureLocation1)

        FeatureLocation featureLocation2 = new FeatureLocation(
                feature: gene1
                ,fmin: 89001
                ,fmax: 90027
                ,sequence: Sequence.first()
        ).save(failOnError: true)

        gene1.addToFeatureLocations(featureLocation2)

        gene1.save(failOnError: true)




    }
}
