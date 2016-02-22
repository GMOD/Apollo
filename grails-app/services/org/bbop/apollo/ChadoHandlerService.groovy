package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.sequence.SequenceTranslationHandler
import org.gmod.chado.Cvterm

import java.security.MessageDigest
import java.sql.Timestamp

/**
 * Notes on Chado Export:
 * Storing the sequence of chromosomes is optional with the default being true.
 * Feature for chromosome do not have a corresponding featureloc as they serve as a srcfeature for other featurelocs.
 * Symbol is treated as a synonym of a feature.
 * A feature can have zero or more featurelocs where a typical feature will have one featureloc.
 * featureloc coordinates are in interbase coordinate system.
 * Orientation of a featureloc is represented by a value of 1 or -1 where default is 1.
 * A featureloc is uniquely identified by the [feature_id, locgroup, rank].
 * If a feature has more than one featureloc then it should be given a different locgroup and rank.
 *
 * Chado Compliance Layers
 * Level 0: Relational schema - this basically means that the schema is adhered to
 * Level 1: Ontologies - this means that all features in the feature table are of a type represented in SO and
 * all feature relationships in feature_relationship table must be SO relationship types
 * Level 2: Graph - all features relationships between a feature of type X and Y must correspond to relationship of
 * that type in SO.
 *
 * 02.19.2016 - Initial version of export is aimed at exporting all information pertaining to Chado Sequence module
 */

@Transactional
class ChadoHandlerService {

    def sequenceService
    def featureRelationshipService
    def transcriptService
    def exonSerivce
    def cdsService


    HashMap<String, org.gmod.chado.Feature> existingChadoFeaturesMap = new HashMap<String, org.gmod.chado.Feature>()
    HashMap<String, org.gmod.chado.Organism> chadoOrganismsMap = new HashMap<String, org.gmod.chado.Organism>()
    HashMap<String, org.gmod.chado.Feature> chadoFeaturesMap = new HashMap<String, org.gmod.chado.Feature>()
    HashMap<Sequence, org.gmod.chado.Feature> referenceSequenceMap = new HashMap<Sequence, org.gmod.chado.Feature>()
    HashMap<String, org.gmod.chado.Dbxref> chadoDbxrefsMap = new HashMap<String, org.gmod.chado.Dbxref>()
    HashMap<String, org.gmod.chado.Featureprop> chadoFeaturePropertiesMap = new HashMap<String, org.gmod.chado.Featureprop>()
    HashMap<String, org.gmod.chado.FeaturePub> chadoFeaturePublications = new HashMap<String, org.gmod.chado.FeaturePub>()

    HashMap<String, org.gmod.chado.Cvterm> cvtermsMap = new HashMap<String, org.gmod.chado.Cvterm>()
    ArrayList<org.bbop.apollo.Feature> processedFeatures = new ArrayList<org.bbop.apollo.Feature>()
    ArrayList<org.bbop.apollo.Feature> failedFeatures = new ArrayList<org.bbop.apollo.Feature>()

    def writeFeatures(Organism organism, ArrayList<Feature> features) {
        // Note: Eager fetch of features to avoid subsequent querying to database
        def chadoFeatures = org.gmod.chado.Feature.all
        if (chadoFeatures.size() > 0) {
            // The Chado datasource has existing features in the Feature table
            // How to identify what is already existing? - [uniquename, organism_id, type_id] triplet
            log.info("The provided Chado data source has existing features in the feature table. " +
                    "Initial efforts for Chado export is aimed at exporting all Apollo features to a clean Chado database")
            return null
        } else {
            // The Chado datasource has no existing features in the Feature table
            writeFeaturesToChado(organism, features)
        }
    }

    /**
     * Writes all features in features array into Chado for the given organism
     * @param organism
     * @param features
     * @return
     */
    def writeFeaturesToChado(Organism organism, ArrayList<Feature> features) {
        /*
         If the assumption is a fresh Chado database then that means there are no ontologies.
         We should support the minimal set: Gene Ontology and Relations Ontology
         */

        ArrayList<org.gmod.chado.Cvterm> cvTerms = org.gmod.chado.Cvterm.all
        if (cvTerms.size() > 0) {
            cvTerms.each {
                cvtermsMap.put(it.name, it)
            }
        }
        else {
            log.warn "No ontologies loaded into the chado database"
            return null
        }

        // Create the organism
        createChadoOrganism(organism)

        // get all sequences for current organism and create Chado feature for each sequence
        def sequenceList = organism.sequences
        createChadoFeatureForSequences(organism, sequenceList, false)

        String startTime, endTime;
        // creating Chado feature for each Apollo feature
        features.each { apolloFeature ->
            if (! processedFeatures.contains(apolloFeature)) {
                startTime = System.currentTimeMillis()
                createChadoFeaturesForAnnotation(organism, apolloFeature)
                endTime = System.currentTimeMillis()
                log.info "Time taken to process annotation ${apolloFeature.uniqueName} of type ${apolloFeature.class.canonicalName}: ${endTime - startTime} ms"
            }
        }
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
        println "Top level feature: ${topLevelFeature.cvTerm}"
        if (topLevelFeature instanceof Gene) {
            // annotation is a Gene / Pseudogene
            def transcripts = transcriptService.getTranscripts(topLevelFeature)
            println "Transcripts for top level gene: ${transcripts.name}"
            transcripts.each { transcript ->
                createChadoFeature(organism, transcript)
                transcript.childFeatureRelationships.each { featureRelationship ->
                    createChadoFeatureRelationship(organism, chadoFeaturesMap.get(transcript.uniqueName), featureRelationship)
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
                    createChadoCdsFeature(organism, transcript, cds)
                    //createChadoPolypeptide(organism, transcript, cds, true)
                    cds.childFeatureRelationships.each { featureRelationship ->
                        createChadoFeatureRelationship(organism, chadoFeaturesMap.get(cds.uniqueName), featureRelationship, "derives_from")
                    }
                }
            }
        }
    }

    def createChadoCdsFeature(org.bbop.apollo.Organism organism, org.bbop.apollo.Transcript transcript, org.bbop.apollo.CDS cds, boolean storeSequence = false){
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
        createChadoFeatureloc(chadoCdsFeature, cds)
        chadoFeaturesMap.put(chadoCdsFeature.uniquename, chadoCdsFeature)
        return chadoCdsFeature
    }


    def createChadoFeatureRelationship(org.bbop.apollo.Organism organism, org.gmod.chado.Feature chadoExonFeature, org.bbop.apollo.Transcript transcript, String relationshipType = "part_of") {
        // create relationship between exon and transcript
        org.gmod.chado.FeatureRelationship chadoFeatureRelationship = new org.gmod.chado.FeatureRelationship(
                subject: chadoExonFeature,
                object: chadoFeaturesMap.get(transcript.uniqueName),
                rank: 0,
                type: cvtermsMap.get(relationshipType)
        ).save()
        println "@createChadoFeatureRelationship WITH COMMON EXON: ${chadoFeatureRelationship.toString()}"
    }

    /**
     * Create an instance of Chado feature of type 'polypeptide' from a given Apollo CDS and also
     * store its amino acid sequence.
     * @param organism
     * @param transcript
     * @param cds
     * @param storeSequence
     * @return
     */
    def createChadoPolypeptide(org.bbop.apollo.Organism organism, org.bbop.apollo.Transcript transcript, org.bbop.apollo.CDS cds, boolean storeSequence = true) {
        org.gmod.chado.Feature chadoPolypeptideFeature = new org.gmod.chado.Feature(
                uniquename: generateUniqueName(),
                name: transcript.name + "-pep", // TODO: Is there a standard way to do this?
                isAnalysis: true,
                isObsolete: false,
                timeaccessioned: generateTimeStamp(),
                timelastmodified: generateTimeStamp(),
                organism: chadoOrganismsMap.get(organism.abbreviation),
                type: cvtermsMap.get("polypeptide")
        )

        if (storeSequence) {
        }

        chadoPolypeptideFeature.save()
        createChadoFeatureloc(chadoPolypeptideFeature, cds)

        chadoFeaturesMap.put(chadoPolypeptideFeature.uniquename, chadoPolypeptideFeature);
        return chadoPolypeptideFeature
    }

    /**
     * Create an instance of Chado featureloc, for a given Chado feature, from Apollo feature location
     * @param chadoFeature
     * @param feature
     * @return chadoFeatureLoc
     */
    def createChadoFeatureloc(org.gmod.chado.Feature chadoFeature, org.bbop.apollo.Feature feature) {
        /*
         In Chado, locgroup and rank are used to uniquely identify featureloc for features that
         have more than one featureloc.
         In Apollo, we currently do not use locgroup and rank for any purposes and their values
         are set to default as suggested by standard Chado specification.
         */
        org.gmod.chado.Featureloc chadoFeatureLoc = new org.gmod.chado.Featureloc(
                fmin: feature.featureLocation.fmin,
                fmax: feature.featureLocation.fmax,
                isFminPartial: feature.featureLocation.isFminPartial,
                isFmaxPartial: feature.featureLocation.isFmaxPartial,
                strand: feature.featureLocation.strand,
                locgroup: feature.featureLocation.locgroup,
                rank: feature.featureLocation.rank,
                feature: chadoFeature,
                srcfeature: referenceSequenceMap.get(feature.featureLocation.sequence)
        ).save()
        return chadoFeatureLoc
    }

    /**
     * Create a Chado feature for a given Apollo feature
     * @param Organism  Apollo organism
     * @param feature   Apollo feature
     * @return
     */
    def createChadoFeature(org.bbop.apollo.Organism organism, org.bbop.apollo.Feature feature) {
        org.gmod.chado.Feature chadoFeature = new org.gmod.chado.Feature(
                uniquename: feature.uniqueName,
                name: feature.name,
                seqlen: feature.sequenceLength,
                md5checksum: feature.md5checksum,
                isAnalysis: feature.isAnalysis,
                isObsolete: feature.isObsolete,
                timeaccessioned: feature.dateCreated,
                timelastmodified: feature.lastUpdated,
                organism: chadoOrganismsMap.get(organism.abbreviation)
        )

        String type = feature.hasProperty('alternateCvTerm') ? feature.alternateCvTerm : feature.cvTerm
        println "TYPE: ${type}"
        if (cvtermsMap.containsKey(type)) {
            chadoFeature.type = cvtermsMap.get(type)
        }
        else {
            println "Cannot find cvterm entry for ${type} in Chado database. Skipping feature ${feature.uniqueName}"
            failedFeatures.add(feature)
        }

        chadoFeature.save()

        createChadoFeatureloc(chadoFeature, feature)

        // feature.symbol - treated as synonym
        if (feature.symbol) {
            org.gmod.chado.Synonym chadoSynonym = new org.gmod.chado.Synonym(
                    name: feature.symbol,
                    synonymSgml: feature.symbol
            )
            org.gmod.chado.Cvterm cvTerm = cvtermsMap.get("symbol")
            if (cvTerm != null) {
                chadoSynonym.type = cvTerm
            } else {
                log.error("Cannot find cvTerm 'symbol' in Chado data source")
            }
        }

        // Feature description - treated as featureprop
        if (feature.description) {
            org.gmod.chado.Featureprop chadoFeatureProp = new org.gmod.chado.Featureprop(
                    value: feature.description,
                    rank: 0,
                    feature: chadoFeature
            )

            org.gmod.chado.Cvterm cvTerm = cvtermsMap.get("description")
            if (cvTerm != null) {
                chadoFeatureProp.type = cvTerm
            } else {
                log.error("Cannot find cvTerm 'description' in Chado data source")
            }

            chadoFeatureProp.save()
        }

        // dbxref
        // Currently the feature.dbxref_id will remain empty as we do not know which of the dbxref will serve as the primary identifier.
        // Chado specification suggests using feature.dbxref_id to link to primary identifier and to use feature_dbxref
        // table for all additional identifiers.
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

        // Feature owner - treated as featureprop
        if (feature.owners) {
            int rank = 0
            feature.owners.each { owner ->
                org.gmod.chado.Featureprop chadoFeatureProp = new org.gmod.chado.Featureprop(
                        value: owner.username,
                        rank: rank,
                        feature: chadoFeature
                )

                org.gmod.chado.Cvterm cvTerm = cvtermsMap.get("owner")
                if (cvTerm != null) {
                    chadoFeatureProp.type = cvTerm
                } else {
                    log.error("Cannot find cvTerm 'owner' in Chado data source")
                }

                chadoFeatureProp.save()
                rank++
            }
        }

        //feature.featureLocations - TODO: If Apollo has annotations with multiple Feature Locations
        //feature.featureCVTerms - TODO: If Apollo has CvTerms that are not part of the SO and REL OBO

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

        chadoFeaturesMap.put(feature.uniqueName, chadoFeature)
        return chadoFeature
    }

    /**
     * Creates an instance of Chado feature_relationship for a given Apollo FeatureRelationship ...
     * @param organism
     * @param feature
     * @param featureRelationship
     * @param relationshipType
     * @return
     */
    def createChadoFeatureRelationship(org.bbop.apollo.Organism organism, org.gmod.chado.Feature feature, org.bbop.apollo.FeatureRelationship featureRelationship, String relationshipType = "part_of") {
        // Relationship logic: subject_id part_of object_id
        // Ex: mRNA part_of Gene
        println "FeatureRelationship ParentFeature: ${featureRelationship.parentFeature}"
        println "FeatureRelationship ChildFeature: ${featureRelationship.childFeature}"
        org.gmod.chado.FeatureRelationship chadoFeatureRelationship = new org.gmod.chado.FeatureRelationship(
                subject: feature,
                object: chadoFeaturesMap.get(featureRelationship.parentFeature.uniqueName),
                value: featureRelationship.value,
                rank: featureRelationship.rank,
                type: cvtermsMap.get(relationshipType)
        ).save()
        println "@createChadoFeatureRelationship: ${chadoFeatureRelationship.toString()}"
    }

    /**
     * Wrapper for createChadoSynonym() for handling multiple featureSynonyms
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
     */
    def createChadoSynonym(org.gmod.chado.Feature chadoFeature, org.bbop.apollo.FeatureSynonym featureSynonym) {
        org.gmod.chado.Synonym chadoSynonym = new org.gmod.chado.Synonym(
                name: featureSynonym.synonym.name,
                synonymSgml: featureSynonym.synonym.synonymSGML,
                type: cvtermsMap.get(featureSynonym.synonym.type.name)
        ).save(flush: true, failOnError: true)

        org.gmod.chado.Pub chadoPublication = new org.gmod.chado.Pub(
                title: featureSynonym.publication.title,
                volumetitle: featureSynonym.publication.volumeTitle,
                volume: featureSynonym.publication.volume,
                seriesName: featureSynonym.publication.seriesName,
                issue: featureSynonym.publication.issue,
                pyear: featureSynonym.publication.publicationYear,
                pages: featureSynonym.publication.pages,
                miniref: featureSynonym.publication.miniReference,
                uniquename: featureSynonym.publication.uniqueName,
                isObsolete: featureSynonym.publication.isObsolete,
                publisher: featureSynonym.publication.publisher,
                pubplace: featureSynonym.publication.publicationPlace,
                type: cvtermsMap.get("paper")
        ).save(flush: true, failOnError: true)

        org.gmod.chado.FeatureSynonym chadoFeatureSynonym = new org.gmod.chado.FeatureSynonym(
                feature: chadoFeature,
                synonym: chadoSynonym,
                pub: chadoPublication,
                isCurrent: true,  // default
                isInternal: false // default
        ).save(flush: true, failOnError: true)
    }


    /**
     * Wrapper for createChadoPublication() for handling multiple publications
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
//     * Chado feature and Chado pub via Chado feature_pub.
     * @param chadoFeature
     * @param publication
     */
    def createChadoPublication(org.gmod.chado.Feature chadoFeature, org.bbop.apollo.Publication publication) {
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
                type: cvtermsMap.get(publication.type.name) // default should be 'paper'
        ).save()

        // Linking feature to publication via FeaturePub (feature_pub table)
        org.gmod.chado.FeaturePub chadoFeaturePub = new org.gmod.chado.FeaturePub(
                feature: chadoFeature,
                pub: chadoPublication
        ).save()

        /*
        TODO: Part of Publication module:
        TODO: Linking publication to its Dbxref via pub_dbxref
        TODO: Linking relatiionships between publications via pub_relationship

        publication.publicationDBXrefs.each { publicationDbxref ->

            org.gmod.chado.Db chadoDb = new org.gmod.chado.Db(
                    name: publicationDbxref.dbxref.db.name,
                    description: publicationDbxref.dbxref.db.description,
                    urlprefix: publicationDbxref.dbxref.db.urlPrefix,
                    url: publicationDbxref.dbxref.db.url
            ).save()

            org.gmod.chado.Dbxref chadoDbxref = new org.gmod.chado.Dbxref(
                    accession: publicationDbxref.dbxref.accession,
                    version: publicationDbxref.dbxref.version,
                    description: publicationDbxref.dbxref.description,
                    db: chadoDb
            ).save()

            org.gmod.chado.PubDbxref chadoPublicationDbxref = new org.gmod.chado.PubDbxref(
                    pub: chadoPublication,
                    dbxref:chadoDbxref,
                    isCurrent: publicationDbxref.isCurrent
            ).save()

        }

        publication.childPublicationRelationships // childPub published_in parentPub
         */

    }

    /**
     * Wrapper for createChadoProperty() for handling multiple FeatureProperties
     * @param chadoFeature
     * @param featureProperties
     * @return
     */
    def createChadoProperty(org.gmod.chado.Feature chadoFeature, Set<org.bbop.apollo.FeatureProperty> featureProperties) {
        featureProperties.each { featureProperty ->
            createChadoProperty(chadoFeature, featureProperty)
        }
    }

    /**
     * Creates an instance of Chado featureprop, for an Apollo FeatureProperty, and its respective Chado featureprop_pub.
     * @param chadoFeature
     * @param featureProperty
     */
    def createChadoProperty(org.gmod.chado.Feature chadoFeature, org.bbop.apollo.FeatureProperty featureProperty) {
        org.gmod.chado.Featureprop chadoFeatureProp = new org.gmod.chado.Featureprop(
                value: featureProperty.value,
                rank: featureProperty.rank,
                feature: chadoFeature
        )

        org.gmod.chado.Cvterm cvTerm = cvtermsMap.get(featureProperty.type.name)
        if (cvTerm != null) {
            chadoFeatureProp.type = cvTerm
        } else {
          log.error("Cannot find cvTerm " + featureProperty.type.name + " in Chado data source")
        }

        chadoFeatureProp.save()

        featureProperty.featurePropertyPublications.each { featurePropertyPublication ->
            createChadoPropertyPub(chadoFeatureProp, featurePropertyPublication)
        }
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
                type: cvtermsMap.get(publication.type.name)
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
     * Creates an instance of Chado Dbxref and creates a linking relationship between Chado Feature and Chado Dbxref.
     * @param chadoFeature
     * @param dbxref
     * @return
     */
    def createChadoDbxref(org.gmod.chado.Feature chadoFeature, org.bbop.apollo.DBXref dbxref) {
        println "@createChadoDbxref for chadoFeature: ${chadoFeature.uniquename} with dbxref: ${dbxref}"
        if (chadoDbxrefsMap.containsKey(dbxref.db.name + ":" + dbxref.accession)) {
            // this xref was seen before and hence we do not create it again

            // create feature_dbxref relationship
            org.gmod.chado.FeatureDbxref chadoFeatureDbxref = new org.gmod.chado.FeatureDbxref(
                    feature: chadoFeature,
                    dbxref: chadoDbxrefsMap.get(dbxref.accession),
                    isCurrent: true
            ).save()
        }
        else {
            // create Chado dbxref
            org.gmod.chado.Dbxref chadoDbxref = new org.gmod.chado.Dbxref(
                    accession: dbxref.accession,
                    version: dbxref.version,
                    description: dbxref.description
            )

            // create Chado db for dbxref
            org.gmod.chado.Db db = new org.gmod.chado.Db(
                    name: dbxref.db.name,
                    description: dbxref.db.description,
                    urlprefix: dbxref.db.urlPrefix,
                    url: dbxref.db.url
            ).save(flush: true, failOnError: true)

            chadoDbxref.db = db
            chadoDbxref.save()

            // create feature_dbxref relationship
            org.gmod.chado.FeatureDbxref chadoFeatureDbxref = new org.gmod.chado.FeatureDbxref(
                    feature: chadoFeature,
                    dbxref: chadoDbxref,
                    isCurrent: true
            ).save()

            chadoDbxrefsMap.put(dbxref.db.name + ":" + dbxref.accession, chadoDbxref)

            dbxref.dbxrefProperties.each { dbxrefProperty ->
                createChadoDbxrefProp(chadoDbxref, dbxrefProperty)
            }
        }
    }

    def createChadoDbxrefProp(org.gmod.chado.Dbxref chadoDbxref, org.bbop.apollo.DBXrefProperty dbXrefProperty) {
        org.gmod.chado.Dbxrefprop chadoDbxrefProp = new org.gmod.chado.Dbxrefprop(
                dbxref: chadoDbxref,
                type: cvtermsMap.get(dbXrefProperty.type.name),
                value: dbXrefProperty.value,
                rank: dbXrefProperty.rank
        ).save()
        return chadoDbxrefProp
    }

    def createChadoFeatureCvterm(org.gmod.chado.Feature chadoFeature, org.bbop.apollo.DBXref dbxref) {
        // TODO: GO annotations should go to feature_cvterm table where CV is the term with DB as 'GO'
        // TODO: Technically we shouldn't have to populate the CV term table since all GO annotations should be in there.
        // TODO: Relevant tables are feature_cvterm, feature_cvtermprop, feature_cvterm_dbxref, feature_cvterm_pub
        // The actual GO annotation is stored in the feature_cvterm
        // Evidence code and qualifier information are stored in feature_cvtermprop
        // feature_cvterm_dbxref should be used to store external IDs associated with the evidence code
        // feature_cvterm_pub should link publications to annotations
        // Apollo stores GO annotations as Dbxref which leaves featurecvterms empty in Apollo feature instance.

    }

    /**
     * A wrapper for createChadoFeatureForSequence() for handling multiple sequences
     * @param sequences
     * @return
     */
    def createChadoFeatureForSequences(Organism organism, Collection<Sequence> sequences, boolean storeSequence = true) {
        sequences.each { sequence ->
            createChadoFeatureForSequence(organism, sequence, storeSequence)
        }
    }

    /**
     * Creates a Feature instance for an Apollo Sequence instance.
     * Optionally, also stores the residues (default is true).
     * @param organism
     * @param sequence
     * @param storeSequence
     * @return
     */
    def createChadoFeatureForSequence(Organism organism, org.bbop.apollo.Sequence sequence, boolean storeSequence = true) {
        Timestamp timeStamp = generateTimeStamp()

        org.gmod.chado.Feature chadoFeature = new org.gmod.chado.Feature(
                uniquename: generateUniqueName(),
                name: sequence.name,
                seqlen: sequence.end - sequence.start,
                isAnalysis: false,
                isObsolete: false,
                timelastmodified: timeStamp,
                timeaccessioned: timeStamp,
                organism: org.gmod.chado.Organism.findByCommonName(sequence.getOrganism().commonName)
        )

        if (storeSequence) {
            String residues = sequenceService.getRawResiduesFromSequence(sequence, sequence.start, sequence.end)
            chadoFeature.residues = residues
            chadoFeature.md5checksum = generateMD5checksum(residues)
        }

        try {
            chadoFeature.type = cvtermsMap.get("chromosome")
        } catch (Exception e) {
            log.error("Cannot find cvTerm 'chromosome' in Chado data source:\n", e)
        }

        org.gmod.chado.Cvterm cvTerm = cvtermsMap.get("chromosome")
        if (cvTerm != null) {
            chadoFeature.type = cvTerm
        } else {
            log.error("Cannot find cvTerm 'chromosome' in Chado data source")
        }
        chadoFeature.save()
        referenceSequenceMap.put(sequence, chadoFeature)
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
