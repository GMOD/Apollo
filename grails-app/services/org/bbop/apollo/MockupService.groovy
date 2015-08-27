package org.bbop.apollo

import grails.util.Environment

import grails.transaction.Transactional
import org.apache.shiro.crypto.hash.Sha256Hash
import org.bbop.apollo.gwt.shared.PermissionEnum

//import grails.compiler.GrailsCompileStatic

//@GrailsCompileStatic
@Transactional
class MockupService {

//    CvTermService cvTermService
    
    def permissionService
    def sequenceService


    def bootstrapData(){
        addUsers()

        if (Environment.current == Environment.TEST) {
            // insert Test environment specific code here
            return
        }

        addDataAdapters()
        addOrganisms()
        try {


            def c = Organism.createCriteria()
            def results = c.list {
                isEmpty("sequences")
            }

            results.each { Organism organism ->
                log.debug "processing organism ${organism}"
                String fileName = organism.getRefseqFile()
                File testFile = new File(fileName)
                if (testFile.exists() && testFile.isFile()) {
                    log.debug "trying to load refseq file: ${testFile.absolutePath}"
                    sequenceService.loadRefSeqs(organism)
                } else {
                    log.error "file not found: " + testFile.absolutePath
                }
            }
        } catch (e) {
            log.error "Problem loading in external sequences: " + e
        }
    }

    private String generatePassword(){
        return "demo"
    }

    def addUsers() {
        if (User.count > 0) return

//        CVTerm userCvTerm = cvTermService.getTerm(FeatureStringEnum.OWNER.value)

        UserGroup publicGroup = new UserGroup(name: "Public").save()
        UserGroup bbopGroup = new UserGroup(name: "BBOP Group").save()
        UserGroup elsikLabGroup = new UserGroup(name: "Elsik Lab").save()
        UserGroup usdaGroup = new UserGroup(name: "USDA").save()
        UserGroup vectorBaseGroup = new UserGroup(name: "Vector Base").save()

        Role userRole = Role.findByName(UserService.USER)
        Role adminRole = Role.findByName(UserService.ADMIN)

        User demoUser = new User(
                username: "demo@demo.gov"
                , passwordHash: new Sha256Hash(generatePassword()).toHex()
                ,firstName: "Bob"
                ,lastName: "Smith"
        ).save(failOnError: true)
        demoUser.addToRoles(userRole)
        bbopGroup.addToUsers(demoUser)

        User nathan = new User(
                username: "nathandunn@lbl.gov"
                , passwordHash: new Sha256Hash(generatePassword()).toHex()
                ,firstName: "Nathan"
                ,lastName: "Dunn"
        ).save(failOnError: true)
        nathan.addToRoles(adminRole)
        bbopGroup.addToUsers(nathan)


        User moni = new User(
                username: "McMunozT@lbl.gov"
                , passwordHash: new Sha256Hash(generatePassword()).toHex()
                ,firstName: "Moni"
                ,lastName: "Munoz-Torrez"
        ).save(failOnError: true)
        moni.addToRoles(adminRole)
        bbopGroup.addToUsers(moni)

        User colin = new User(
                username: "colin.diesh@gmail.com"
                , passwordHash: new Sha256Hash(generatePassword()).toHex()
                ,firstName: "Colin"
                ,lastName: "Diesh"
        ).save(failOnError: true)
        colin.addToRoles(adminRole)
        elsikLabGroup.addToUsers(moni)

        User deepak = new User(
                username: "deepak.unni3@gmail.com"
                , passwordHash: new Sha256Hash(generatePassword()).toHex()
                ,firstName: "Deepak"
                ,lastName: "Unni"
        ).save(failOnError: true)
        deepak.addToRoles(adminRole)
        elsikLabGroup.addToUsers(moni)

        User adminUser = new User(
                username: "admin@admin.gov"
                , passwordHash: new Sha256Hash(generatePassword()).toHex()
                ,firstName: "Super"
                ,lastName: "Admin"
        ).save(failOnError: true)
        adminUser.addToRoles(userRole)
        bbopGroup.addToUsers(adminUser)


        User honeyBeeAdmin = new User(
                username: "admin@honeybee.org"
                , passwordHash: new Sha256Hash(generatePassword()).toHex()
                ,firstName: "Buzz"
                ,lastName: "Smith"
        ).save()
        User humanAdmin = new User(
                username: "admin@human.org"
                , passwordHash: new Sha256Hash(generatePassword()).toHex()
                ,firstName: "Human"
                ,lastName: "Smith"
        ).save()
        User yeastAdmin = new User(
                username: "admin@yeast.org"
                , passwordHash: new Sha256Hash(generatePassword()).toHex()
                ,firstName: "Yeast"
                ,lastName: "Smith"
        ).save()



    }

//    /**
//     * Replace stuff in mapping.xml
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
        Organism honeyBee = new Organism(abbreviation: "HB",commonName: "Honey Bee", genus: "Amel",species: "dsomething",directory: "/opt/apollo/honeybee/data").save(failOnError: true)
        Organism human = new Organism(abbreviation: "HM",commonName: "Human", genus: "Homo",species: "sapiens",directory: "/opt/apollo/human/data").save(failOnError: true)
        Organism yeast = new Organism(abbreviation: "YS",commonName: "Yeast", genus: "Saccharomyces",species: "cerevisiae",directory: "/opt/apollo/yeast/data").save(failOnError: true)

        User honeyBeeAdmin = User.findByUsername("admin@honeybee.org")
        User humanAdmin = User.findByUsername("admin@human.org")
        User yeastAdmin = User.findByUsername("admin@yeast.org")
       
        List<PermissionEnum> adminPermissions = new ArrayList<>()
        adminPermissions.add(PermissionEnum.ADMINISTRATE)
        permissionService.setOrganismPermissionsForUser(adminPermissions,honeyBee,honeyBeeAdmin)
        permissionService.setOrganismPermissionsForUser(adminPermissions,human,humanAdmin)
        permissionService.setOrganismPermissionsForUser(adminPermissions,yeast,yeastAdmin)


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
