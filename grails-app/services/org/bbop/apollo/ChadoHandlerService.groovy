package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.sequence.SequenceTranslationHandler
import org.codehaus.groovy.grails.web.json.JSONObject
import org.gmod.chado.Cvterm

import java.security.MessageDigest
import java.sql.Timestamp

/**
 *
 * Chado Compliance Layers
 * Level 0: Relational schema - this basically means that the schema is adhered to
 * Level 1: Ontologies - this means that all features in the feature table are of a type represented in SO and
 * all feature relationships in feature_relationship table must be SO relationship types
 * Level 2: Graph - all features relationships between a feature of type X and Y must correspond to relationship of
 * that type in SO.
 *
 * Relevant Chado modules:
 * Chado General Module
 * Chado CV Module
 * Chado Organism Module
 * Chado Sequence Module
 * Chado Publication Module
 *
 */

@Transactional
class ChadoHandlerService {

    def grailsApplication
    def sequenceService
    def featureRelationshipService
    def transcriptService
    def cdsService

    HashMap<String, org.gmod.chado.Organism> chadoOrganismsMap = new HashMap<String, org.gmod.chado.Organism>()
    HashMap<String, org.gmod.chado.Feature> chadoFeaturesMap = new HashMap<String, org.gmod.chado.Feature>()
    HashMap<Sequence, org.gmod.chado.Feature> referenceSequenceMap = new HashMap<Sequence, org.gmod.chado.Feature>()
    HashMap<String, org.gmod.chado.Db> chadoDbMap = new HashMap<String, org.gmod.chado.Db>()
    HashMap<String, org.gmod.chado.Dbxref> chadoDbxrefsMap = new HashMap<String, org.gmod.chado.Dbxref>()
    HashMap<String, org.gmod.chado.Featureprop> chadoPropertiesMap = new HashMap<String, org.gmod.chado.Featureprop>()
    HashMap<String, org.gmod.chado.Pub> chadoPublicationsMap = new HashMap<String, org.gmod.chado.Pub>()
    HashMap<String, org.gmod.chado.Cvterm> cvtermsMap = new HashMap<String, org.gmod.chado.Cvterm>()
    HashMap<String, org.gmod.chado.Synonym> chadoSynonymMap = new HashMap<String, org.gmod.chado.Synonym>()
    ArrayList<org.bbop.apollo.Feature> processedFeatures = new ArrayList<org.bbop.apollo.Feature>()
    ArrayList<org.bbop.apollo.Feature> failedFeatures = new ArrayList<org.bbop.apollo.Feature>()

    def writeFeatures(Organism organism, ArrayList<Sequence> sequenceList, ArrayList<Feature> features) {
        JSONObject returnObject = new JSONObject()
        if (!grailsApplication.config.dataSource_chado) {
            log.error("Cannot export annotations to Chado as Chado data source has not been configured")
            returnObject.error = "Cannot export annotations to Chado as Chado data source has not been configured."
        }
        else {
            def chadoFeatures = org.gmod.chado.Feature.all
            if (chadoFeatures.size() > 0) {
                // The Chado datasource has existing features in the Feature table
                // How to identify what is already existing? - [uniquename, organism_id, type_id] triplet
                log.error "The provided Chado data source has existing features in the feature table. " +
                        "Initial efforts for Chado export is aimed at exporting all Apollo features to a clean Chado database"
                returnObject.error = "The provided Chado data source already has existing features in the feature table."
            } else {
                if (organism.genus == null || organism.species == null) {
                    log.error "Apollo Organism must have genus and species defined."
                    returnObject.error = "Apollo Organism must have genus and species defined."
                }
                else {
                    returnObject = writeFeaturesToChado(organism, sequenceList, features)
                }
            }
        }

        return returnObject
    }

    /**
     * Writes all features in features array into Chado for the given organism
     * @param organism
     * @param features
     * @return
     */
    def writeFeaturesToChado(Organism organism, ArrayList<Sequence> sequenceList, ArrayList<Feature> features) {
        /*
        The exporter assumes that the following ontologies are pre-loaded into the Chado data source:
        1. Sequence Ontology
        2. Gene Ontology
        3. Relations Ontology
         */

        long startTime, endTime, totalTime
        totalTime = System.currentTimeMillis()
        startTime = System.currentTimeMillis()
        List<org.gmod.chado.Cvterm> cvTerms = org.gmod.chado.Cvterm.all
        endTime = System.currentTimeMillis()
        log.debug "Time taken to query for existing Chado Cvterm: ${endTime - startTime} ms"
        if (cvTerms.size() > 0) {
            cvTerms.each {
                cvtermsMap.put(it.name, it)
            }
        }
        else {
            log.warn "No ontologies loaded into the chado database"
            return null
        }

        startTime = System.currentTimeMillis()
        ArrayList<org.gmod.chado.Db> existingChadoDbs = org.gmod.chado.Db.all
        endTime = System.currentTimeMillis()
        log.debug "Time taken to query for existing Chado Db: ${endTime - startTime} ms"
        if (existingChadoDbs.size() > 0) {
            existingChadoDbs.each {
                chadoDbMap.put(it.name, it)
            }
        }

        // Create the organism
        startTime = System.currentTimeMillis()
        createChadoOrganism(organism)
        endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado Organism for ${organism.commonName}: ${endTime - startTime} ms"

        // Create chado feature for sequence in sequenceList
        if (sequenceList.size() > 0) {
            createChadoFeatureForSequences(organism, sequenceList, false)
        }

        // creating Chado feature for each Apollo feature
        features.each { apolloFeature ->
            startTime = System.currentTimeMillis()
            createChadoFeaturesForAnnotation(organism, apolloFeature)
            endTime = System.currentTimeMillis()
            log.debug "Time taken to process annotation ${apolloFeature.name} of type ${apolloFeature.class.canonicalName}: ${endTime - startTime} ms"
        }

        JSONObject exportStatistics = new JSONObject()
        exportStatistics = [ "Organism count" : chadoOrganismsMap.size(), "Sequence count" : referenceSequenceMap.size(),
                             "Feature count" : chadoFeaturesMap.size(), "dbxref count" : chadoDbxrefsMap.size(),
                             "Time Taken" : (System.currentTimeMillis() - totalTime) / 1000 + " seconds" ]

        return exportStatistics
    }

    /**
     * For each Apollo annotation, traverses through the model hierarchy and creates a Chado representation of the annotation.
     * @param organism
     * @param topLevelFeature
     * @return
     */
    def createChadoFeaturesForAnnotation(org.bbop.apollo.Organism organism, org.bbop.apollo.Feature topLevelFeature) {
        /*
        Top-level features are gene, pseudogene, transposable_element, repeat_region, insertion, deletion, substitution.
        A top-level feature that is not an instance of type Gene is likely to be a singleton feature.
         */
        createChadoFeature(organism, topLevelFeature)
        if (topLevelFeature instanceof Gene) {
            // annotation is a Gene / Pseudogene
            def transcripts = transcriptService.getTranscripts(topLevelFeature)
            transcripts.each { transcript ->
                org.gmod.chado.Feature chadoFeature = createChadoFeature(organism, transcript)
                transcript.childFeatureRelationships.each { featureRelationship ->
                    createChadoFeatureRelationship(organism, chadoFeature, featureRelationship)
                }

                def exons = transcriptService.getSortedExons(transcript)
                /*
                In GMOD Chado Best Practices, it is noted that exons can be part_of more than one mRNA and that
                no two distinct exon rows should have exact same featureloc coordinates (this would indicate they are the same exon).
                TODO: Do we factor this logic into export if two exons from two separate transcripts/isoforms have same fmin and fmax?
                */
                exons.each { exon ->
                        org.gmod.chado.Feature chadoExonFeature = createChadoFeature(organism, exon)
                        exon.childFeatureRelationships.each { featureRelationship ->
                            createChadoFeatureRelationship(organism, chadoExonFeature, featureRelationship)
                        }
                }
                if (transcript instanceof MRNA) {
                    // TODO: Do we create a chado feature for stop_codon_read_through
                    def cds = transcriptService.getCDS(transcript)
                    org.gmod.chado.Feature chadoCdsFeature = createChadoCdsFeature(organism, transcript, cds)
                    cds.childFeatureRelationships.each { featureRelationship ->
                        createChadoFeatureRelationship(organism, chadoCdsFeature, featureRelationship, "part_of")
                    }
                }
            }
        }
    }

    /**
     * Create an instance of Chado feature of type 'CDS' for a given Apollo CDS.
     * @param organism
     * @param transcript
     * @param cds
     * @param storeSequence
     * @return chadoCdsFeature
     */
    def createChadoCdsFeature(org.bbop.apollo.Organism organism, org.bbop.apollo.Transcript transcript, org.bbop.apollo.CDS cds, boolean storeSequence = false){
        long startTime, endTime
        startTime = System.currentTimeMillis()
        org.gmod.chado.Feature chadoCdsFeature = new org.gmod.chado.Feature(
                uniquename: cds.uniqueName,
                name: cds.name,
                isAnalysis: cds.isAnalysis,
                isObsolete: cds.isObsolete,
                timeaccessioned: cds.dateCreated,
                timelastmodified: cds.lastUpdated,
                organism: chadoOrganismsMap.get(organism.abbreviation),
                type: cvtermsMap.get(cds.cvTerm)
        )
        if (storeSequence) {
            String sequence = cdsService.getResiduesFromCDS(cds)
            chadoCdsFeature.residues = sequence
            chadoCdsFeature.seqlen = sequence.length()
            chadoCdsFeature.md5checksum = generateMD5checksum(sequence)
        }

        chadoCdsFeature.save()
        endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado feature of CDS ${cds.uniqueName}: ${endTime - startTime} ms"
        createChadoFeatureloc(chadoCdsFeature, cds)
        chadoFeaturesMap.put(chadoCdsFeature.uniquename, chadoCdsFeature)
        return chadoCdsFeature
    }

    /**
     * Create an instance of Chado feature of type 'polypeptide' from a given Apollo CDS and also
     * store its amino acid sequence.
     * @param organism
     * @param transcript
     * @param cds
     * @param storeSequence
     * @return chadoPolyPeptideFeature
     */
    def createChadoPolypeptide(org.bbop.apollo.Organism organism, org.bbop.apollo.Transcript transcript, org.bbop.apollo.CDS cds, boolean storeSequence = true) {
        long startTime, endTime
        Timestamp timestamp = generateTimeStamp()
        startTime = System.currentTimeMillis()
        org.gmod.chado.Feature chadoPolypeptideFeature = new org.gmod.chado.Feature(
                uniquename: generateUniqueName(),
                name: transcript.name + "-pep",
                isAnalysis: true,
                isObsolete: false,
                timeaccessioned: timestamp,
                timelastmodified: timestamp,
                organism: chadoOrganismsMap.get(organism.abbreviation),
                type: cvtermsMap.get("polypeptide")
        )

        if (storeSequence) {

        }

        chadoPolypeptideFeature.save()
        endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado feature of Polypeptide for Transcript ${transcript.name}: ${endTime - startTime} ms"
        createChadoFeatureloc(chadoPolypeptideFeature, cds)
        chadoFeaturesMap.put(chadoPolypeptideFeature.uniquename, chadoPolypeptideFeature);
        return chadoPolypeptideFeature
    }

    /**
     * Create an instance of Chado featureloc, for a given Chado feature, from Apollo feature location.
     * @param chadoFeature
     * @param feature
     * @return chadoFeatureLoc
     */
    def createChadoFeatureloc(org.gmod.chado.Feature chadoFeature, org.bbop.apollo.Feature feature) {
        /*
         In Chado, locgroup and rank are used to uniquely identify featureloc for features that
         have more than one featureloc.
         In Apollo, we currently do not use locgroup and rank for any purposes and their values
         are set to 0, the default as suggested by standard Chado specification.
         */
        long startTime, endTime
        startTime = System.currentTimeMillis()
        org.gmod.chado.Featureloc chadoFeatureLoc = new org.gmod.chado.Featureloc(
                fmin: feature.featureLocation.fmin,
                fmax: feature.featureLocation.fmax,
                isFminPartial: feature.featureLocation.isFminPartial,
                isFmaxPartial: feature.featureLocation.isFmaxPartial,
                strand: feature.featureLocation.strand,
                locgroup: feature.featureLocation.locgroup,
                rank: feature.featureLocation.rank,
                feature: chadoFeature,
                //srcfeature: referenceSequenceMap.get(feature.featureLocation.sequence.name)
                srcfeature: getSrcFeatureForFeature(feature.featureLocation.sequence)
        ).save()
        endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado featureloc for feature fmin: ${feature.fmin} fmax: ${feature.fmax}: ${endTime - startTime} ms"
        return chadoFeatureLoc
    }

    /**
     * Checks if there is a Chado feature representation of Apollo Sequence.
     * If not, creates and returns the a Chado feature.
     * @param sequence
     * @return
     */
    def getSrcFeatureForFeature(org.bbop.apollo.Sequence sequence) {
        org.gmod.chado.Feature srcFeature
        if(referenceSequenceMap.containsKey(sequence.name)) {
            srcFeature = referenceSequenceMap.get(sequence.name)
        }
        else {
            srcFeature = createChadoFeatureForSequence(sequence.organism, sequence, false)
        }
        return srcFeature
    }

    /**
     * Creates an instance of Chado feature_relationship for a given Apollo FeatureRelationship.
     * @param organism
     * @param feature
     * @param featureRelationship
     * @param relationshipType
     * @return chadoFeatureRelationship
     */
    def createChadoFeatureRelationship(org.bbop.apollo.Organism organism, org.gmod.chado.Feature feature, org.bbop.apollo.FeatureRelationship featureRelationship, String relationshipType = "part_of") {
        long startTime, endTime
        startTime = System.currentTimeMillis()
        org.gmod.chado.FeatureRelationship chadoFeatureRelationship = new org.gmod.chado.FeatureRelationship(
                subject: feature,
                object: chadoFeaturesMap.get(featureRelationship.parentFeature.uniqueName),
                value: featureRelationship.value,
                rank: featureRelationship.rank,
                type: getChadoCvterm(relationshipType)
        ).save()
        endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado feature_relationship ${feature.class.simpleName} ${relationshipType} ${featureRelationship.parentFeature.class.simpleName}: ${endTime - startTime} ms"
        featureRelationship.featureRelationshipPublications.each { featureRelationshipPublication ->
            createChadoFeatureRelationshipPublication(chadoFeatureRelationship, featureRelationshipPublication)
        }

        //featureRelationship.featureRelationshipProperties.each { featureRelationshipProperty ->
        //    createChadoFeatureRelationshipProperty(featureRelationshipProperty)
        //}
        return chadoFeatureRelationship
    }

    def createChadoFeatureRelationshipPublication(org.gmod.chado.FeatureRelationship chadoFeatureRelationship, org.bbop.apollo.Publication publication) {
        org.gmod.chado.Pub chadoPublication = createChadoPublication(publication)
        org.gmod.chado.FeatureRelationshipPub chadoFeatureRelationshipPub = new org.gmod.chado.FeatureRelationshipPub(
                featureRelationship: chadoFeatureRelationship,
                pub: chadoPublication
        ).save()
        return chadoFeatureRelationshipPub
    }

    /**
     * Wrapper for createChadoSynonym() for handling multiple featureSynonyms.
     * @param chadoFeature
     * @param featureSynonyms
     * @return
     */
    def createChadoSynonym(org.gmod.chado.Feature chadoFeature, Set<org.bbop.apollo.FeatureSynonym> featureSynonyms) {
        featureSynonyms.each { featureSynonym ->
            createChadoSynonym(chadoFeature, featureSynonym)
        }
    }

    /**
     * Creates an instance of Chado synonym for a given Apollo FeatureSynonym and creates a linking relationship between
     * Chado feature and Chado synonym via feature_synonym.
     * @param chadoFeature
     * @param featureSynonym
     * @return chadoSynonym
     */
    def createChadoSynonym(org.gmod.chado.Feature chadoFeature, org.bbop.apollo.FeatureSynonym featureSynonym) {
        String synonymKey = featureSynonym.synonym.type.name + ":" + featureSynonym.synonym.name
        org.gmod.chado.Synonym chadoSynonym
        if (chadoSynonymMap.containsKey(synonymKey)) {
            chadoSynonym = getChadoCvterm(synonymKey)
        }
        else {
            chadoSynonym = new org.gmod.chado.Synonym(
                    name: featureSynonym.synonym.name,
                    synonymSgml: featureSynonym.synonym.synonymSGML,
                    type: getChadoCvterm(featureSynonym.synonym.type.name)
            ).save()
            chadoSynonymMap.put(synonymKey, chadoSynonym)
        }

        org.gmod.chado.Pub chadoPublication = createChadoPublication(featureSynonym.publication)

        org.gmod.chado.FeatureSynonym chadoFeatureSynonym = new org.gmod.chado.FeatureSynonym(
                feature: chadoFeature,
                synonym: chadoSynonym,
                pub: chadoPublication,
                isCurrent: true,  // default
                isInternal: false // default
        ).save()

        chadoSynonymMap.put(synonymKey, chadoSynonym)
        return chadoSynonym
    }

    /**
     * Wrapper for createChadoPublication() for handling multiple publications.
     * @param chadoFeature
     * @param publications
     * @return
     */
    def createChadoPublication(org.gmod.chado.Feature chadoFeature, Set<org.bbop.apollo.Publication> publications) {
        publications.each { publication ->
            createChadoPublication(chadoFeature, publication)
        }
    }

    /**
     * Creates an instance of Chado publication for a given Apollo Publication and creates a linking relationship between
     * Chado feature and Chado pub via Chado feature_pub.
     * @param chadoFeature
     * @param publication
     */
    def createChadoPublication(org.gmod.chado.Feature chadoFeature, org.bbop.apollo.Publication publication) {
        /*
        pub
        pub_dbxref
        pub_relationship
        pubauthor
        pubprop
         */

        org.gmod.chado.Pub chadoPublication = new org.gmod.chado.Pub(
                uniquename: publication.uniqueName,
                title: publication.title,
                volumetitle: publication.volumeTitle,
                volume: publication.volume,
                seriesName: publication.seriesName,
                issue: publication.issue,
                pyear: publication.publicationYear,
                pages: publication.pages,
                miniref: publication.miniReference,
                isObsolete: publication.isObsolete,
                publisher: publication.publisher,
                pubplace: publication.publicationPlace,
                type: cvtermsMap.get(publication.type.name)
        ).save()

        // Linking feature to publication via feature_pub
        org.gmod.chado.FeaturePub chadoFeaturePub = new org.gmod.chado.FeaturePub(
                feature: chadoFeature,
                pub: chadoPublication
        ).save()

        // Chado pubauthor
        publication.publicationAuthors.each { publicationAuthor ->
            org.gmod.chado.Pubauthor chadoPubAuthor = new org.gmod.chado.Pubauthor(
                    pub: chadoPublication,
                    rank: publicationAuthor.rank,
                    editor: publicationAuthor.editor,
                    givennames: publicationAuthor.givenNames,
                    surname: publicationAuthor.surname,
                    suffix: publicationAuthor.suffix
            ).save()
        }

        publication.publicationDBXrefs.each { publicationDbxref ->
            // Chado db
            org.gmod.chado.Db chadoDb
            if (chadoDbMap.containsKey(publicationDbxref.dbxref.db.name)) {
                chadoDb = chadoDbMap.get(publicationDbxref.dbxref.db.name)
            }
            else {
                chadoDb = new org.gmod.chado.Db(
                        name: publicationDbxref.dbxref.db.name,
                        description: publicationDbxref.dbxref.db.description,
                        urlprefix: publicationDbxref.dbxref.db.urlPrefix,
                        url: publicationDbxref.dbxref.db.url
                ).save()
            }

            // Chado dbxref
            org.gmod.chado.Dbxref chadoDbxref
            String dbxrefKey = publicationDbxref.dbxref.db + ":" + publicationDbxref.dbxref.accession + ":" + publicationDbxref.dbxref.version
            if (chadoDbxrefsMap.containsKey(dbxrefKey)) {
                chadoDbxref = chadoDbxrefsMap.get(dbxrefKey)
            }
            else {
                chadoDbxref = new org.gmod.chado.Dbxref(
                        accession: publicationDbxref.dbxref.accession,
                        version: Integer.getInteger(publicationDbxref.dbxref.version),
                        description: publicationDbxref.dbxref.description,
                        db: chadoDb
                ).save()
                chadoDbxrefsMap.put(dbxrefKey, chadoDbxref)
            }

            // Chado pub_dbxref
            org.gmod.chado.PubDbxref chadoPubDbxref = new org.gmod.chado.PubDbxref(
                    isCurrent: publicationDbxref.isCurrent,
                    pub: chadoPublication,
                    dbxref: chadoDbxref
            ).save()
        }

        // Chado pub_relationship
        publication.childPublicationRelationships.each { publicationRelationship ->
            org.gmod.chado.PubRelationship chadoPubRelationship = new org.gmod.chado.PubRelationship(
                    subject: chadoPublication,
                    cvterm: cvtermsMap.get(publicationRelationship.type.name)
            )

            org.gmod.chado.Pub objectPublication
            if (chadoPublicationsMap.containsKey(publicationRelationship.objectPublication.uniqueName)) {
                objectPublication = chadoPublicationsMap.get(publicationRelationship.objectPublication.uniqueName)
            } else {
                objectPublication = new org.gmod.chado.Pub(
                        uniquename: publicationRelationship.objectPublication.uniqueName,
                        volumetitle: publicationRelationship.objectPublication.volumeTitle,
                        volume: publicationRelationship.objectPublication.volume,
                        seriesName: publicationRelationship.objectPublication.seriesName,
                        issue: publicationRelationship.objectPublication.issue,
                        pyear: publicationRelationship.objectPublication.publicationYear,
                        pages: publicationRelationship.objectPublication.pages,
                        miniref: publicationRelationship.objectPublication.miniReference,
                        isObsolete: publicationRelationship.objectPublication.isObsolete,
                        publisher: publicationRelationship.objectPublication.publisher,
                        pubplace: publicationRelationship.objectPublication.publicationPlace,
                        type: cvtermsMap.get(publicationRelationship.objectPublication.type.name)
                ).save()
            }
            chadoPubRelationship.object = objectPublication
        }
        return chadoPublication
    }

    /**
     * Wrapper for createChadoProperty() for handling multiple FeatureProperties
     * @param chadoFeature
     * @param featureProperties
     * @return
     */
    def createChadoProperty(org.gmod.chado.Feature chadoFeature, Set<org.bbop.apollo.FeatureProperty> featureProperties) {
        int commentRank = 0
        featureProperties.each { featureProperty ->
            if (featureProperty instanceof org.bbop.apollo.Comment) {
                createChadoFeaturePropertyForComment(chadoFeature, featureProperty, commentRank)
                commentRank++;
            }
            else {
                createChadoProperty(chadoFeature, featureProperty)
            }

        }
    }

    /**
     * Creates an instance of Chado featureprop, for an Apollo FeatureProperty, and its respective Chado featureprop_pub.
     * @param chadoFeature
     * @param featureProperty
     */
    def createChadoProperty(org.gmod.chado.Feature chadoFeature, org.bbop.apollo.FeatureProperty featureProperty) {
        long startTime, endTime
        String type = "feature_property"
        startTime = System.currentTimeMillis()
        org.gmod.chado.Featureprop chadoFeatureProp = new org.gmod.chado.Featureprop(
                value: featureProperty.value,
                rank: featureProperty.rank,
                feature: chadoFeature,
                type: getChadoCvterm(type)
        ).save()
        endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado featureprop of type '${type}' and value ${featureProperty.value}: ${endTime - startTime} ms"
        featureProperty.featurePropertyPublications.each { featurePropertyPublication ->
            createChadoPropertyPub(chadoFeatureProp, featurePropertyPublication)
        }
        return chadoFeatureProp
    }

    def createChadoFeaturePropertyForComment(org.gmod.chado.Feature chadoFeature, org.bbop.apollo.FeatureProperty comment, int rank) {
        long startTime, endTime
        startTime = System.currentTimeMillis()
        org.gmod.chado.Featureprop chadoFeatureProp = new org.gmod.chado.Featureprop(
                value: comment.value,
                rank: rank,
                feature: chadoFeature,
                type: getChadoCvterm("comment")
        ).save()
        endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado featureprop of type 'comment' and value ${comment.value}: ${endTime - startTime} ms"
        return chadoFeatureProp
    }
    /**
     * Creates an instance of Chado publication for a given Apollo publication and creates a linking relationship
     * beteen featureprop and publication via Chado featureprop_pub.
     * @param chadoFeatureProp
     * @param publication
     */
    def createChadoPropertyPub(org.gmod.chado.Featureprop chadoFeatureProp, org.bbop.apollo.Publication publication) {
        org.gmod.chado.Pub chadoPublication = new org.gmod.chado.Pub(
                uniquename: publication.uniqueName,
                title: publication.title,
                volumetitle: publication.volumeTitle,
                volume: publication.volume,
                seriesName: publication.seriesName,
                issue: publication.issue,
                pyear: publication.publicationYear,
                pages: publication.pages,
                miniref: publication.miniReference,
                isObsolete: publication.isObsolete,
                publisher: publication.publisher,
                pubplace: publication.publicationPlace,
                type: getChadoCvterm(publication.type.name)
        ).save()

        org.gmod.chado.FeaturepropPub featurePropertyPublication = new org.gmod.chado.FeaturepropPub(
                featureprop: chadoFeatureProp,
                pub: chadoPublication
        ).save()
    }

    /**
     * Experimental
     * @param cvTerm
     */
    def createChadoCvterm(org.bbop.apollo.CVTerm cvTerm) {
        // TODO: Must be careful in creating and setting all properties
        org.gmod.chado.Cv chadoCv = new org.gmod.chado.Cv(
                name: cvTerm.cv.name,
                definition: cvTerm.cv.definition
        ).save()

        org.gmod.chado.Db chadoDb = new org.gmod.chado.Db(
                name: cvTerm.dbxref.db.name,
                description: cvTerm.dbxref.db.description,
                urlprefix: cvTerm.dbxref.db.urlPrefix,
                url: cvTerm.dbxref.db.url
        ).save()

        org.gmod.chado.Dbxref chadoDbxref = new org.gmod.chado.Dbxref(
                accession: cvTerm.dbxref.accession,
                description: cvTerm.dbxref.description,
                version: cvTerm.dbxref.version,
                db: chadoDb
        ).save()

        org.gmod.chado.Cvterm chadoCvterm = new org.gmod.chado.Cvterm(
                name: cvTerm.name,
                definition: cvTerm.definition,
                isObsolete: cvTerm.isObsolete,
                isRelationshiptype: cvTerm.isRelationshipType,
                cv: chadoCv,
                dbxref: chadoDbxref,
        ).save()

        org.gmod.chado.CvtermDbxref chadoCvTermDbxref = new org.gmod.chado.CvtermDbxref(
                cvTerm: chadoCvterm,
                dbxref: chadoDbxref,
                isForDefinition: 0 // TODO: How to set this attribute for new Cvterm from Apollo?
        ).save()

        org.gmod.chado.CvtermRelationship chadoCvTermRelationship = new org.gmod.chado.CvtermRelationship(

        )

        //cvTerm.childCVTermRelationships
        //cvTerm.parentCVTermRelationships
    }

    /**
     * A wrapper for createChadoDbxref() for handling multiple dbxrefs
     * @param chadoFeature
     * @param dbxrefs
     * @return
     */
    def createChadoDbxref(org.gmod.chado.Feature chadoFeature, Set<org.bbop.apollo.DBXref> dbxrefs) {
        dbxrefs.each { dbxref ->
            createChadoDbxref(chadoFeature, dbxref)
        }
    }

    /**
     * Creates an instance of Chado Dbxref, creates a linking relationship between Chado Feature and
     * Chado Dbxref via Chado feature_dbxref and creates Chado dbxrefprop
     * @param chadoFeature
     * @param dbxref
     * @return
     */
    def createChadoDbxref(org.gmod.chado.Feature chadoFeature, org.bbop.apollo.DBXref dbxref) {
        long startTime, endTime
        startTime = System.currentTimeMillis()
        org.gmod.chado.Dbxref chadoDbxref = createChadoDbxref(dbxref)
        // create feature_dbxref relationship
        org.gmod.chado.FeatureDbxref chadoFeatureDbxref = new org.gmod.chado.FeatureDbxref(
                feature: chadoFeature,
                dbxref: chadoDbxref,
                isCurrent: true
        ).save()
        endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado dbxref for ${dbxref.db.name}:${dbxref.accession} : ${endTime - startTime} ms"
        // dbxref properties
        dbxref.dbxrefProperties.each { dbxrefProperty ->
            createChadoDbxrefProp(chadoDbxref, dbxrefProperty)
        }
    }

    def createChadoDb(org.bbop.apollo.DB db) {
        long startTime, endTime
        org.gmod.chado.Db chadoDb
        if (chadoDbMap.containsKey(db.name)) {
            chadoDb = chadoDbMap.get(db.name)
        }
        else {
            startTime = System.currentTimeMillis()
            chadoDb = new org.gmod.chado.Db(
                    name: db.name,
                    description: db.description,
                    urlprefix: db.urlPrefix,
                    url: db.url
            ).save()
            endTime = System.currentTimeMillis()
            log.debug "Time taken to create Chado Db for ${db.name} : ${endTime - startTime} ms"
            chadoDbMap.put(db.name, chadoDb)
        }
        return chadoDb
    }

    def createChadoDbxref(org.bbop.apollo.DBXref dbxref) {
        String dbxrefKey = dbxref.db.name + ":" + dbxref.accession + ":" + dbxref.version
        org.gmod.chado.Dbxref chadoDbxref
        if (chadoDbxrefsMap.containsKey(dbxrefKey)) {
            // this xref was seen before and hence we do not create it again.
            chadoDbxref = chadoDbxrefsMap.get(dbxrefKey)
        } else {
            // create Chado db for dbxref
            org.gmod.chado.Db db = createChadoDb(dbxref.db)

            // create Chado dbxref
            chadoDbxref = new org.gmod.chado.Dbxref(
                    accession: dbxref.accession,
                    description: dbxref.description,
                    db: db
            )
            if (dbxref.version != null) {
                chadoDbxref.version = Integer.getInteger(dbxref.version)
            }
            chadoDbxref.save()
            chadoDbxrefsMap.put(dbxrefKey, chadoDbxref)
        }
        return chadoDbxref
    }


    /**
     * Create an instance of Chado dbxref_prop for a given instance of Apollo DBXrefProperty.
     * @param chadoDbxref
     * @param dbXrefProperty
     * @return
     */
    def createChadoDbxrefProp(org.gmod.chado.Dbxref chadoDbxref, org.bbop.apollo.DBXrefProperty dbXrefProperty) {
        org.gmod.chado.Dbxrefprop chadoDbxrefProp = new org.gmod.chado.Dbxrefprop(
                dbxref: chadoDbxref,
                type: cvtermsMap.get(dbXrefProperty.type.name),
                value: dbXrefProperty.value,
                rank: dbXrefProperty.rank
        ).save()
        return chadoDbxrefProp
    }

    def createChadoFeatureCvterm(org.gmod.chado.Feature chadoFeature, Set<org.bbop.apollo.FeatureCVTerm> featureCVTerms) {
        featureCVTerms.each { featureCvterm ->
            createChadoFeatureCvterm(chadoFeature, featureCvterm)
        }
    }

    def createChadoFeatureCvterm(org.gmod.chado.Feature chadoFeature, org.bbop.apollo.FeatureCVTerm featureCvterm) {
        org.gmod.chado.FeatureCvterm chadoFeatureCvterm = new org.gmod.chado.FeatureCvterm(
                feature: chadoFeature,
                cvterm: getChadoCvterm(featureCvterm.cvterm.name),
                pub: createChadoPublication(featureCvterm.publication),
                rank: featureCvterm.rank,
                isNot: featureCvterm.isNot
        ).save()
        return chadoFeatureCvterm
    }

    def createChadoPublication(org.bbop.apollo.Publication publication) {
        org.gmod.chado.Pub chadoPublication
        if (chadoPublicationsMap.containsKey(publication.uniqueName)) {
            chadoPublication = chadoPublicationsMap.get(publication.uniqueName)
        }
        else {
            chadoPublication = new org.gmod.chado.Pub(
                    uniquename: publication.uniqueName,
                    title: publication.title,
                    volumetitle: publication.volumeTitle,
                    volume: publication.volume,
                    seriesName: publication.seriesName,
                    issue: publication.issue,
                    pyear: publication.publicationYear,
                    pages: publication.pages,
                    miniref: publication.miniReference,
                    isObsolete: publication.isObsolete,
                    publisher: publication.publisher,
                    pubplace: publication.publicationPlace,
                    type: getChadoCvterm(publication.type.name)
            ).save()
            chadoPublicationsMap.put(publication.uniqueName, chadoPublication)
        }
        return chadoPublication
    }

    /**
     * A wrapper for createChadoFeatureForSequence() for handling multiple sequences
     * @param sequences
     * @return
     */
    def createChadoFeatureForSequences(Organism organism, Collection<Sequence> sequences, boolean storeSequence = true) {
        long startTime, endTime
        sequences.each { sequence ->
            startTime = System.currentTimeMillis()
            createChadoFeatureForSequence(organism, sequence, storeSequence)
            endTime = System.currentTimeMillis()

        }
    }

    /**
     * Creates a Feature instance for an Apollo Sequence instance.
     * Optionally, also stores the residues (default is true).
     * @param organism
     * @param sequence
     * @param storeSequence
     * @return chadoFeature
     */
    def createChadoFeatureForSequence(Organism organism, org.bbop.apollo.Sequence sequence, boolean storeSequence = true) {
        long startTime, endTime
        Timestamp timeStamp = generateTimeStamp()
        startTime = System.currentTimeMillis()
        org.gmod.chado.Feature chadoFeature = new org.gmod.chado.Feature(
                uniquename: generateUniqueName(),
                name: sequence.name,
                seqlen: sequence.end - sequence.start,
                isAnalysis: false,
                isObsolete: false,
                timelastmodified: timeStamp,
                timeaccessioned: timeStamp,
                organism: org.gmod.chado.Organism.findByCommonName(sequence.getOrganism().commonName),
                type: getChadoCvterm("chromosome")
        )

        if (storeSequence) {
            String residues = sequenceService.getRawResiduesFromSequence(sequence, sequence.start, sequence.end)
            chadoFeature.residues = residues
            chadoFeature.md5checksum = generateMD5checksum(residues)
        }

        chadoFeature.save()
        endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado Feature for sequence ${sequence.name}: ${endTime - startTime} ms"
        referenceSequenceMap.put(sequence.name, chadoFeature)
        return chadoFeature
    }

    def getChadoCvterm(String term) {
        org.gmod.chado.Cvterm chadoCvterm
        def cvTermlist = []
        if (cvtermsMap.containsKey(term)) {
            cvTermlist.addAll(cvtermsMap.get(term))
            if ( cvTermlist.size() > 1){
                println "Ambiguous CV term: ${term}"
                return null;
            }
            chadoCvterm = cvtermsMap.get(term)
        }
        else {
            log.error("Cannot find cvTerm " + term + " in Chado data source")
            /*
             Currently the assumption is that Apollo uses standardized CV terms and thus there will not be a need for
             creating a new de-novo CV term.
             In future if this assumption doesn't hold true then creating a new CV term will involve the following tables:
             cv, cvterm, cvterm_relationship, cvterm_dbxref, cvtermpath, cvtermsynonym, cvtermprop, dbxrefprop
             */
        }
        return chadoCvterm
    }

    /**
     * Create a Chado feature for a given Apollo feature.
     * @param Organism  Apollo organism
     * @param feature   Apollo feature
     * @return
     */
    def createChadoFeature(org.bbop.apollo.Organism organism, org.bbop.apollo.Feature feature) {
        long startTime, endTime
        String type = feature.hasProperty('alternateCvTerm') ? feature.alternateCvTerm : feature.cvTerm
        // feature
        startTime = System.currentTimeMillis()
        org.gmod.chado.Feature chadoFeature = new org.gmod.chado.Feature(
                uniquename: feature.uniqueName,
                name: feature.name,
                seqlen: feature.sequenceLength,
                md5checksum: feature.md5checksum,
                isAnalysis: feature.isAnalysis,
                isObsolete: feature.isObsolete,
                timeaccessioned: feature.dateCreated,
                timelastmodified: feature.lastUpdated,
                organism: chadoOrganismsMap.get(organism.abbreviation),
                type: getChadoCvterm(type)
        ).save()
        endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado feature of type ${feature.class.simpleName}: ${endTime - startTime} ms"
        // featureloc
        createChadoFeatureloc(chadoFeature, feature)

        /*
        // feature.symbol treated as Chado synonym
        // TODO: Cannot treat feature.symbol as a Chado synonym because feature -> synonym link requires a Publication,
        // TODO: according to Chado specification
        if (feature.symbol) {
            String synonymKey = "symbol:" + feature.symbol
            org.gmod.chado.Synonym chadoSynonym
            if (chadoSynonymMap.containsKey(synonymKey)) {
                chadoSynonym = chadoSynonymMap.get(synonymKey)
            }
            else {
                chadoSynonym = new org.gmod.chado.Synonym(
                        name: feature.symbol,
                        synonymSgml: feature.symbol,
                        type: getChadoCvterm("symbol")
                ).save()
                chadoSynonymMap.put(synonymKey, chadoSynonym)
            }

            org.gmod.chado.FeatureSynonym chadoFeatureSynonym = new org.gmod.chado.FeatureSynonym(
                    feature: chadoFeature,
                    synonym: chadoSynonym,
                    pub: null,
                    isCurrent: true,
                    isInternal: false
            ).save()
        }
        */

        // As an alternative, feature.symbol is treated as Chado featureprop
        if (feature.symbol) {
            org.gmod.chado.Featureprop chadoFeatureprop = new org.gmod.chado.Featureprop(
                    value: feature.symbol,
                    rank: 0,
                    feature: chadoFeature,
                    type: getChadoCvterm("symbol")
            ).save()
        }

        // Feature description treated as Chado featureprop
        if (feature.description) {
            org.gmod.chado.Featureprop chadoFeatureProp = new org.gmod.chado.Featureprop(
                    value: feature.description,
                    rank: 0,
                    feature: chadoFeature,
                    type: getChadoCvterm("description")
            ).save()
        }

        // dbxref
        // TODO: How to determine the primary dbxref?
        /* Currently the feature.dbxref_id will remain empty as we do not know which of
         the dbxref will serve as the primary identifier.
         Chado specification suggests using feature.dbxref_id to link to primary
         identifier and to use feature_dbxref table for all additional identifiers.
        */
        if (feature.featureDBXrefs) {
            createChadoDbxref(chadoFeature, feature.featureDBXrefs)
        }

        // properties
        if (feature.featureProperties) {
            createChadoProperty(chadoFeature, feature.featureProperties)
        }

        // publications
        if (feature.featurePublications) {
            createChadoPublication(chadoFeature, feature.featurePublications)
        }

        // Feature owner treated as featureprop
        if (feature.owners) {
            int rank = 0
            feature.owners.each { owner ->
                org.gmod.chado.Featureprop chadoFeatureProp = new org.gmod.chado.Featureprop(
                        value: owner.username,
                        rank: rank,
                        feature: chadoFeature,
                        type: getChadoCvterm("owner")
                ).save()
                rank++
            }
        }

        // synonyms
        if (feature.featureProperties) {
            createChadoSynonym(chadoFeature, feature.featureSynonyms)
        }

        // genotypes - TODO: When Apollo has Genotype associated with annotations
        //if (feature.featureGenotypes) {
        //    createChadoGenotype(chadoFeature, feature.featureGenotypes)
        //}

        // phenotypes - TODO: When Apollo has Phenotype associated with annotations
        //if (feature.featurePhenotypes) {
        //    createChadoPhenotype(chadoFeature, feature.featurePhenotypes)
        //}

        //feature.featureLocations - TODO: If Apollo has features with multiple Feature Locations
        //feature.featureCVTerms - TODO: If Apollo has features with multiple CvTerms

        chadoFeaturesMap.put(feature.uniqueName, chadoFeature)
        return chadoFeature
    }

    /**
     * Generates and returns an instance of java.SQL.Timestamp for the current time, without timezone,
     * which is required by Chado for Feature attributes such as timelastmodified and timelastaccessioned.
     * @return Timestamp
     */
    def generateTimeStamp() {
        return new Timestamp(System.currentTimeMillis())
    }

    /**
     * Creates an instance of Chado Organism for a given Apollo Organism.
     * @param organism
     * @return
     */
    def createChadoOrganism(org.bbop.apollo.Organism organism) {
        org.gmod.chado.Organism chadoOrganism = new org.gmod.chado.Organism(
                abbreviation: organism.abbreviation,
                genus: organism.genus,
                species: organism.species,
                commonName: organism.commonName,
                comment: organism.comment
        )

        chadoOrganism.save()
        chadoOrganismsMap.put(organism.abbreviation, chadoOrganism)
    }

    /**
     * Generates and returns a unique name using UUID.
     * @return String
     */
    def generateUniqueName() {
        return UUID.randomUUID().toString()
    }

    /**
     * Generates an MD5 checksum for a given string
     * @param s
     * @return
     */
    def generateMD5checksum(String s){
        return MessageDigest.getInstance("MD5").digest(s.bytes).encodeHex().toString()
    }

}
