package org.bbop.apollo

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONObject

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

    def configWrapperService
    def sequenceService
    def featureRelationshipService
    def transcriptService
    def cdsService

    private static final String SEQUENCE_ONTOLOGY = "sequence"
    private static final String RELATIONSHIP_ONTOLOGY = "relationship"
    private static final String FEATURE_PROPERTY = "feature_property"
    private static final def topLevelFeatureTypes = [Gene.cvTerm, Pseudogene.cvTerm, TransposableElement.cvTerm, RepeatRegion.cvTerm,
                                                     InsertionArtifact.cvTerm, DeletionArtifact.cvTerm, SubstitutionArtifact.cvTerm]
    private static final ontologyDb = ["SO", "GO", "RO"]
    Map<String, org.gmod.chado.Organism> chadoOrganismsMap = new HashMap<String, org.gmod.chado.Organism>()
    Map<String, Integer> exportStatisticsMap = new HashMap<String, Integer>();
    ArrayList<org.bbop.apollo.Feature> processedFeatures = new ArrayList<org.bbop.apollo.Feature>()
    ArrayList<org.bbop.apollo.Feature> failedFeatures = new ArrayList<org.bbop.apollo.Feature>()

    def writeFeatures(Organism organism, ArrayList<Sequence> sequenceList, ArrayList<Feature> features, boolean exportAllSequences = false) {
        JSONObject returnObject = new JSONObject()
        if (!configWrapperService.hasChadoDataSource()) {
            log.error("Cannot export annotations to Chado as Chado data source has not been configured")
            returnObject.error = "Cannot export annotations to Chado as Chado data source has not been configured."
        }
        else {
            if (!checkForOntologies()) {
                log.error "No ontologies loaded into the Chado database"
                returnObject.error = "No ontologies loaded into the Chado database. Refer to "
            }
            else if (organism.genus == null || organism.species == null) {
                log.error "Apollo Organism must have genus and species defined."
                returnObject.error = "Apollo Organism must have genus and species defined."
            }
            else {
                returnObject = writeFeaturesToChado(organism, sequenceList, features, exportAllSequences)
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
    def writeFeaturesToChado(Organism organism, ArrayList<Sequence> sequenceList, ArrayList<Feature> features, boolean exportAllSequences = false) {
        /*
        The exporter assumes that the following ontologies are pre-loaded into the Chado data source:
        1. Sequence Ontology
        2. Gene Ontology
        3. Relations Ontology
         */

        initializeExportStatistics()
        long totalTime = System.currentTimeMillis()
        // Create the organism
        long startTime = System.currentTimeMillis()
        createChadoOrganism(organism)
        long endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado Organism for ${organism.commonName}: ${endTime - startTime} ms"

        // Create chado feature for sequence in sequenceList
        if (sequenceList.size() > 0) {
            createChadoFeatureForSequences(organism, sequenceList, configWrapperService.getChadoExportFastaForSequence())
        }
        else
        if (exportAllSequences) {
            sequenceList = Sequence.findAllByOrganism(organism)
            createChadoFeatureForSequences(organism, sequenceList, configWrapperService.getChadoExportFastaForSequence())
        }

        def existingChadoAnnotations = org.gmod.chado.Feature.executeQuery(
                "SELECT DISTINCT f.uniquename FROM org.gmod.chado.Feature f WHERE f.type.name IN :topLevelFeatureTypes",
                [topLevelFeatureTypes: topLevelFeatureTypes])

        // creating Chado feature for each Apollo feature
        features.each { apolloFeature ->
            startTime = System.currentTimeMillis()
            createChadoFeaturesForAnnotation(organism, apolloFeature)
            existingChadoAnnotations.remove(apolloFeature.uniqueName)
            endTime = System.currentTimeMillis()
            log.debug "Time taken to process annotation ${apolloFeature.name} of type ${apolloFeature.class.canonicalName}: ${endTime - startTime} ms"
        }

        // delete features that are in Chado but not in Apollo
        existingChadoAnnotations.each { uniquename ->
            org.gmod.chado.Feature chadoFeature = getChadoFeature(uniquename)
            log.debug "Deleting ${uniquename}"
            deleteChadoFeature(chadoFeature)
        }

        JSONObject exportStatistics = new JSONObject()
        exportStatistics = [ "Organism count" : chadoOrganismsMap.size(),
                             "Sequence count" : exportStatisticsMap.get("sequence_feature_count"),
                             "Feature count" : exportStatisticsMap.get("feature_count"),
                             "Featureloc count" : exportStatisticsMap.get("featureloc_count"),
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
        org.gmod.chado.Feature topLevelChadoFeature = getChadoFeature(topLevelFeature.uniqueName)
        if (topLevelChadoFeature) {
            // if a top level annotation already exists we delete it
            long startTime = System.currentTimeMillis()
            deleteChadoFeature(topLevelChadoFeature)
            long endTime = System.currentTimeMillis()
            log.debug "Time taken to delete existing annotation ${endTime - startTime} ms"
        }

        createChadoFeature(organism, topLevelFeature)
        if (topLevelFeature instanceof Gene) {
            // annotation is a Gene / Pseudogene
            def transcripts = transcriptService.getTranscripts(topLevelFeature)
            transcripts.each { transcript ->
                org.gmod.chado.Feature chadoFeature = createChadoFeature(organism, transcript)
                transcript.childFeatureRelationships.each { featureRelationship ->
                    createChadoFeatureRelationship(organism, chadoFeature, featureRelationship)
                }

                def exons = transcriptService.getSortedExons(transcript,false)
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
                    org.gmod.chado.Feature chadoCdsFeature = createChadoCdsFeature(organism, transcript, cds, configWrapperService.getChadoExportFastaForCds())
                    cds.childFeatureRelationships.each { featureRelationship ->
                        createChadoFeatureRelationship(organism, chadoCdsFeature, featureRelationship, "part_of")
                    }
                }
            }
        }
    }

    /**
     * Queries the Chado database to find feature that has the given uniquename.
     * @param uniqueName
     * @return
     */
    def getChadoFeature(String uniqueName) {
        org.gmod.chado.Feature chadoFeature
        def featureResult = org.gmod.chado.Feature.findAllByUniquename(uniqueName)
        if (featureResult.size() == 0) {
            chadoFeature = null
        }
        else if (featureResult.size() == 1) {
            chadoFeature = featureResult.get(0)
        }
        else {
            log.error "${featureResult} - More than one result found for feature uniquename '${uniqueName}'. Returning null."
            chadoFeature = null
        }

        return chadoFeature
    }

    /**
     * Deletes a given feature from the Chado database.
     * @param chadoFeature
     * @return
     */
    def deleteChadoFeature(org.gmod.chado.Feature chadoFeature) {
        def chadoFeatureLocs = getChadoFeatureloc(chadoFeature)
        chadoFeatureLocs.each { fl ->
            fl.delete()
        }

        def chadoFeatureDbxrefs = getChadoFeatureDbxrefs(chadoFeature)
        chadoFeatureDbxrefs.each { fd ->
            if (! ontologyDb.contains(fd.dbxref.db.name)) {
                fd.dbxref.delete()
            }
        }

        def chadoFeatureProperties = getChadoFeatureProps(chadoFeature)
        chadoFeatureProperties.each { fp ->
            fp.delete()
        }

        def chadoChildFeatureRelationships = getChildFeatureRelationships(chadoFeature)
        chadoChildFeatureRelationships.each { child ->
            deleteChadoFeature(child.subject)
        }

        // TODO: featureSynonyms, featurePubs, featureGenotypes, featurePhenotypes
        chadoFeature.delete(flush: true)
    }

    /**
     * Queries the Chado database and returns all featurelocs for a given Chado Feature
     * @param chadoFeature
     * @return
     */
    def getChadoFeatureloc(org.gmod.chado.Feature chadoFeature) {
        long startTime = System.currentTimeMillis()
        def results = org.gmod.chado.Featureloc.executeQuery(
                        "SELECT DISTINCT fl FROM org.gmod.chado.Featureloc fl WHERE fl.feature.uniquename = :queryUniqueName",
                        [queryUniqueName: chadoFeature.uniquename]
        )
        long endTime = System.currentTimeMillis()
        log.debug "Time taken to query featurelocs for ChadoFeature: ${chadoFeature.uniquename} - ${endTime - startTime} ms"
        return results
    }

    /**
     * Queries the Chado database and returns all FeatureDbxrefs for a given Chado Feature
     * @param chadoFeature
     * @return
     */
    def getChadoFeatureDbxrefs(org.gmod.chado.Feature chadoFeature) {
        long startTime = System.currentTimeMillis()
        def results = org.gmod.chado.FeatureDbxref.executeQuery(
                        "SELECT DISTINCT fd FROM org.gmod.chado.FeatureDbxref fd JOIN fd.dbxref dbxref JOIN dbxref.db db WHERE fd.feature.uniquename = :queryUniqueName",
                        [queryUniqueName: chadoFeature.uniquename]
        )
        long endTime = System.currentTimeMillis()
        log.debug "Time taken to query featureDbxrefs for ChadoFeature: ${chadoFeature.uniquename} - ${endTime - startTime} ms"
        return results
    }

    /**
     * Queries the Chado database and returns all FeatureProps for a given Chado Feature
     * @param chadoFeature
     * @return
     */
    def getChadoFeatureProps(org.gmod.chado.Feature chadoFeature) {
        long startTime = System.currentTimeMillis()
        def results = org.gmod.chado.Featureprop.executeQuery(
                        "SELECT DISTINCT fp FROM org.gmod.chado.Featureprop fp JOIN fp.feature f WHERE f.uniquename = :queryUniqueName",
                        [queryUniqueName: chadoFeature.uniquename]
        )
        long endTime = System.currentTimeMillis()
        log.debug "Time taken to query featureprops for ChadoFeature: ${chadoFeature.uniquename} - ${endTime - startTime} ms"
        return results
    }

    /**
     * Queries the Chado database and retuns all child feature relationships for a given Chado Feature
     * @param chadoFeature
     * @return
     */
    def getChildFeatureRelationships(org.gmod.chado.Feature chadoFeature) {
        long startTime = System.currentTimeMillis()
        def results = org.gmod.chado.FeatureRelationship.executeQuery(
                        "SELECT DISTINCT fr FROM org.gmod.chado.FeatureRelationship fr JOIN fr.subject s JOIN fr.object o WHERE o.uniquename = :queryUniqueName",
                        [queryUniqueName: chadoFeature.uniquename]
        )
        long endTime = System.currentTimeMillis()
        log.debug "Time taken to query feature_relationships for ChadoFeature: ${chadoFeature.uniquename} - ${endTime - startTime} ms"
        return results
    }

    /**
     * Create a Chado feature for a given Apollo feature.
     * @param Organism
     * @param feature
     * @return
     */
    def createChadoFeature(org.bbop.apollo.Organism organism, org.bbop.apollo.Feature feature) {
        long startTime, endTime
        String type = feature.cvTerm

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
                organism: chadoOrganismsMap.get(organism.commonName),
                type: getChadoCvterm(type, SEQUENCE_ONTOLOGY)
        ).save()
        exportStatisticsMap['feature_count'] += 1
        endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado feature of type ${feature.class.simpleName}: ${endTime - startTime} ms"

        // featureloc
        createChadoFeatureloc(organism, chadoFeature, feature)

        /*
         TODO: Cannot treat feature.symbol as a Chado synonym
         because feature -> synonym link requires a Publication, according to Chado specification
        */
        //if (feature.symbol) {
        //    org.gmod.chado.Synonym chadoSynonym = getChadoSynonym(feature.symbol, "symbol")
        //
        //    // Creating a linking relationship between Chado feature and Chado synonym
        //    org.gmod.chado.FeatureSynonym chadoFeatureSynonym = new org.gmod.chado.FeatureSynonym(
        //            feature: chadoFeature,
        //            synonym: chadoSynonym,
        //            pub: null,
        //            isCurrent: true,
        //            isInternal: false
        //    ).save()
        //}

        // As an alternative, feature.symbol is treated as Chado featureprop
        if (feature.symbol) {
            org.gmod.chado.Featureprop chadoFeatureprop = new org.gmod.chado.Featureprop(
                    value: feature.symbol,
                    rank: 0,
                    feature: chadoFeature,
                    type: getChadoCvterm("symbol", FEATURE_PROPERTY)
            ).save()
        }

        // Feature description treated as Chado featureprop
        if (feature.description) {
            org.gmod.chado.Featureprop chadoFeatureProp = new org.gmod.chado.Featureprop(
                    value: feature.description,
                    rank: 0,
                    feature: chadoFeature,
                    type: getChadoCvterm("description", FEATURE_PROPERTY)
            ).save()
        }

        /* TODO: How to determine the primary dbxref?
         Currently the feature.dbxref_id will remain empty as we do not know which of
         the dbxref will serve as the primary identifier.
         Chado specification suggests using feature.dbxref_id to link to primary
         identifier and to use feature_dbxref table for all additional identifiers.
        */

        // dbxref
        if (feature.featureDBXrefs) {
            createChadoDbxref(chadoFeature, feature.featureDBXrefs)
        }

        // properties
        if (feature.featureProperties) {
            createChadoProperty(chadoFeature, feature.featureProperties)
        }

        // Feature owner treated as featureprop
        if (feature.owners) {
            int rank = 0
            feature.owners.each { owner ->
                org.gmod.chado.Featureprop chadoFeatureProp = new org.gmod.chado.Featureprop(
                        value: owner.username,
                        rank: rank,
                        feature: chadoFeature,
                        type: getChadoCvterm("owner", FEATURE_PROPERTY)
                ).save()
                rank++
            }
        }

        // publications
        //if (feature.featurePublications) {
        //    createChadoFeaturePub(chadoFeature, feature.featurePublications)
        //}

        // synonyms
        //if (feature.featureSynonyms) {
        //    createChadoSynonym(chadoFeature, feature.featureSynonyms)
        //}

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

        chadoFeature.save(flush: true)

        return chadoFeature
    }

    /**
     * Create an instance of Chado featureloc, for a given Chado feature, with location information
     * from an Apollo feature location.
     * @param chadoFeature
     * @param feature
     * @return chadoFeatureLoc
     */
    def createChadoFeatureloc(org.bbop.apollo.Organism organism, org.gmod.chado.Feature chadoFeature, org.bbop.apollo.Feature feature) {
        /*
         In Chado, locgroup and rank are used to uniquely identify featureloc for features that
         have more than one featureloc.
         In Apollo, we currently do not use locgroup and rank for any purposes and their values
         are set to 0, the default, as suggested by standard Chado specification.
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
                srcfeature: getSrcFeatureForFeature(organism, feature.featureLocation.sequence)
        ).save(flush: true)
        endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado featureloc for feature fmin: ${feature.fmin} fmax: ${feature.fmax}: ${endTime - startTime} ms"
        exportStatisticsMap['featureloc_count'] += 1
        feature.featureLocation.featureLocationPublications.each { featureLocationPublication ->
            createChadoFeaturelocPub(chadoFeatureLoc, featureLocationPublication)
        }
        return chadoFeatureLoc
    }

    /**
     * Create an instance of Chado feature of type 'CDS' for a given Apollo CDS.
     * @param organism
     * @param transcript
     * @param cds
     * @param storeSequence
     * @return chadoCdsFeature
     */
    def createChadoCdsFeature(org.bbop.apollo.Organism organism, org.bbop.apollo.Transcript transcript, org.bbop.apollo.CDS cds, boolean storeSequence = false) {
        long startTime, endTime
        startTime = System.currentTimeMillis()
        org.gmod.chado.Feature chadoCdsFeature = new org.gmod.chado.Feature(
                uniquename: cds.uniqueName,
                name: cds.name,
                isAnalysis: cds.isAnalysis,
                isObsolete: cds.isObsolete,
                timeaccessioned: cds.dateCreated,
                timelastmodified: cds.lastUpdated,
                organism: chadoOrganismsMap.get(organism.commonName),
                type: getChadoCvterm(cds.cvTerm, SEQUENCE_ONTOLOGY)
        )
        if (storeSequence) {
            String sequence = cdsService.getResiduesFromCDS(cds)
            chadoCdsFeature.residues = sequence
            chadoCdsFeature.seqlen = sequence.length()
            chadoCdsFeature.md5checksum = generateMD5checksum(sequence)
        }

        chadoCdsFeature.save()
        exportStatisticsMap['feature_count'] += 1
        endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado feature of CDS ${cds.uniqueName}: ${endTime - startTime} ms"
        createChadoFeatureloc(organism, chadoCdsFeature, cds)
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
    def createChadoPolypeptide(org.bbop.apollo.Organism organism, org.bbop.apollo.Transcript transcript, org.bbop.apollo.CDS cds, boolean storeSequence = false) {
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
                organism: chadoOrganismsMap.get(organism.commonName),
                type: getChadoCvterm("polypeptide", SEQUENCE_ONTOLOGY)
        )

        if (storeSequence) {}

        chadoPolypeptideFeature.save()
        endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado feature of Polypeptide for Transcript ${transcript.name}: ${endTime - startTime} ms"
        createChadoFeatureloc(organism, chadoPolypeptideFeature, cds)
        return chadoPolypeptideFeature
    }

    /**
     * Checks if there is a Chado feature representation for a given Apollo Sequence.
     * @param sequence
     * @return
     */
    def getSrcFeatureForFeature(org.bbop.apollo.Organism organism, org.bbop.apollo.Sequence sequence) {
        org.gmod.chado.Feature srcFeature
        long startTime = System.currentTimeMillis()
        def sequenceFeatureResult = org.gmod.chado.Feature.executeQuery(
                "SELECT DISTINCT sf FROM org.gmod.chado.Feature sf WHERE sf.name = :querySequenceName AND sf.organism.genus = :queryGenus AND sf.organism.species = :querySpecies AND sf.type.name = :querySequenceType",
                [querySequenceName: sequence.name, queryGenus: organism.genus, querySpecies: organism.species, querySequenceType: "chromosome"])
        long endTime = System.currentTimeMillis()
        log.debug "Time taken for querying for sequence ${sequence.name} of organism ${organism.genus} ${organism.species}: ${endTime - startTime} ms"

        if (sequenceFeatureResult.size() == 0) {
            srcFeature = createChadoFeatureForSequence(organism, sequence, configWrapperService.getChadoExportFastaForSequence())
        }
        else if (sequenceFeatureResult.size() == 1) {
            srcFeature = sequenceFeatureResult.get(0)
        }
        else {
            log.error "${sequenceFeatureResult} - More than one result found for sequence name '${sequence.name}'. Returning null."
            srcFeature = null
        }
        return srcFeature
    }

    /**
     * Creates an instance of Chado FeaturelocPub and creates a linking relationship between Chado feature and Chado pub.
     * @param chadoFeatureloc
     * @param featureLocationPublication
     * @return
     */
    def createChadoFeaturelocPub(org.gmod.chado.Featureloc chadoFeatureloc, org.bbop.apollo.Publication featureLocationPublication) {
        org.gmod.chado.Pub chadoPublication = createChadoPublication(featureLocationPublication)
        org.gmod.chado.FeaturelocPub chadoFeaturelocPub = new org.gmod.chado.FeaturelocPub(
                featureloc: chadoFeatureloc,
                pub: chadoPublication
        ).save()
        return chadoFeaturelocPub
    }

    /**
     * Creates an instance of Chado feature_relationship for a given Apollo FeatureRelationship.
     * Default relationship type is 'part_of'.
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
                object: getChadoFeature(featureRelationship.parentFeature.uniqueName),
                value: featureRelationship.value,
                rank: featureRelationship.rank,
                type: getChadoCvterm(relationshipType, RELATIONSHIP_ONTOLOGY)
        ).save()
        endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado feature_relationship ${feature.class.simpleName} ${relationshipType} ${featureRelationship.parentFeature.class.simpleName}: ${endTime - startTime} ms"

        featureRelationship.featureRelationshipPublications.each { featureRelationshipPublication ->
            createChadoFeatureRelationshipPublication(chadoFeatureRelationship, featureRelationshipPublication)
        }

        featureRelationship.featureRelationshipProperties.each { featureRelationshipProperty ->
            createChadoFeatureRelationshipProperty(chadoFeatureRelationship, featureRelationshipProperty)
        }
        return chadoFeatureRelationship
    }

    /**
     * Creates a Chado feature_relationship_pub for a Chado feature_relationship based on a given Apollo Publication.
     * @param chadoFeatureRelationship
     * @param publication
     * @return
     */
    def createChadoFeatureRelationshipPublication(org.gmod.chado.FeatureRelationship chadoFeatureRelationship, org.bbop.apollo.Publication publication) {
        org.gmod.chado.Pub chadoPublication = createChadoPublication(publication)
        org.gmod.chado.FeatureRelationshipPub chadoFeatureRelationshipPub = new org.gmod.chado.FeatureRelationshipPub(
                featureRelationship: chadoFeatureRelationship,
                pub: chadoPublication
        ).save()
        return chadoFeatureRelationshipPub
    }

    /**
     * Creates a Chado feature_relationshipprop for a Chado feature_relationship based on a given Apollo FeatureProperty
     * @param chadoFeatureRelationship
     * @param featureProperty
     * @return
     */
    def createChadoFeatureRelationshipProperty(org.gmod.chado.FeatureRelationship chadoFeatureRelationship, org.bbop.apollo.FeatureProperty featureProperty) {
        org.gmod.chado.FeatureRelationshipprop chadoFeatureRelationshipprop = new org.gmod.chado.FeatureRelationshipprop(
                featureRelationship: chadoFeatureRelationship,
                rank: featureProperty.rank,
                value: featureProperty.value,
                type: getChadoCvterm(featureProperty.type.name, FEATURE_PROPERTY)
        ).save()

        featureProperty.featurePropertyPublications.each { featurePropertyPublication ->
            createChadoFeatureRelationshipPropertyPublication(chadoFeatureRelationshipprop, featurePropertyPublication)
        }
        return chadoFeatureRelationshipprop
    }

    /**
     * Creates a Chado feature_relationshipprop_pub for a Chado feature_relationshipprop based on a given Apollo Publication
     * @param chadoFeatureRelationshipprop
     * @param featurePropertyPublication
     * @return
     */
    def createChadoFeatureRelationshipPropertyPublication(org.gmod.chado.FeatureRelationshipprop chadoFeatureRelationshipprop, org.bbop.apollo.Publication featurePropertyPublication) {
        org.gmod.chado.Pub chadoPublication = createChadoPublication(featurePropertyPublication)
        org.gmod.chado.FeatureRelationshippropPub chadoFeatureRelationshippropPub = new org.gmod.chado.FeatureRelationshippropPub(
                featureRelationshipprop: chadoFeatureRelationshipprop,
                pub: chadoPublication
        ).save()
        return chadoFeatureRelationshippropPub
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
     *
     * @param synonym
     * @return
     */
    def getChadoSynonym(org.bbop.apollo.Synonym synonym) {
        org.gmod.chado.Synonym chadoSynonym
        def synonymResult = org.gmod.chado.Synonym.executeQuery(
                "SELECT DISTINCT s FROM org.gmod.chado.Synonym s JOIN s.type c WHERE s.name = :querySynonym AND c.name = :queryCvterm",
                [querySynonym: synonym.name, queryCvterm: synonym.type.name])
        if (synonymResult.size() == 0) {
            chadoSynonym = createChadoSynonym(synonym.name)
        }
        else if (synonymResult.size() == 1) {
            chadoSynonym = synonymResult.get(0)
        }
        else {
            log.error "${synonymResult} - More than one result found for synonym '${synonym.name}'of type '${synonym.type.name}'. Returning null."
            chadoSynonym = null
        }
        return chadoSynonym
    }

    /**
     *
     * @param synonymName
     * @param synonymType
     * @return
     */
    def getChadoSynonym(String synonymName, String synonymType) {
        org.gmod.chado.Synonym chadoSynonym
        def synonymResult = org.gmod.chado.Synonym.executeQuery(
                "SELECT DISTINCT s FROM org.gmod.chado.Synonym s JOIN s.type c WHERE s.name = :querySynonym AND c.name = :queryCvterm",
                [querySynonym: synonymName, queryCvterm: synonymType])
        if (synonymResult.size() == 0) {
            chadoSynonym = createChadoSynonym(synonymName)
        }
        else if (synonymResult.size() == 1) {
            chadoSynonym = synonymResult.get(0)
        }
        else {
            log.error "${synonymResult} - More than one result found for synonym '${synonymName}'of type '${synonymType}'. Returning null."
            chadoSynonym = null
        }
        return chadoSynonym
    }

    /**
     * Creates an instance of Chado synonym for a given Apollo FeatureSynonym and creates a linking relationship between
     * Chado feature and Chado synonym via feature_synonym.
     * @param chadoFeature
     * @param featureSynonym
     * @return chadoSynonym
     */
    def createChadoSynonym(org.gmod.chado.Feature chadoFeature, org.bbop.apollo.FeatureSynonym featureSynonym) {
        org.gmod.chado.Synonym chadoSynonym = createChadoSynonym(featureSynonym.synonym)

        //Publications from FeatureSynonym
        org.gmod.chado.Pub chadoPublication = createChadoPublication(featureSynonym.publication)

        org.gmod.chado.FeatureSynonym chadoFeatureSynonym = new org.gmod.chado.FeatureSynonym(
                feature: chadoFeature,
                synonym: chadoSynonym,
                pub: chadoPublication,
                isCurrent: featureSynonym.isCurrent,  // default
                isInternal: featureSynonym.isInternal // default
        ).save()

        return chadoSynonym
    }

    /**
     * Creates an instance of Chado synonym for a given Apollo Synonym
     * @param synonymName
     * @param synonymType
     * @return
     */
    def createChadoSynonym(String synonymName) {
        org.gmod.chado.Synonym chadoSynonym = new org.gmod.chado.Synonym(
                name: synonymName,
                synonymSgml: synonymName,
                type: getChadoCvterm("synonym", FEATURE_PROPERTY)
        ).save()

        return chadoSynonym
    }

    /**
     * Wrapper for createChadoFeaturePub() for handling multiple publications.
     * @param chadoFeature
     * @param publications
     * @return
     */
    def createChadoFeaturePub(org.gmod.chado.Feature chadoFeature, Set<org.bbop.apollo.Publication> publications) {
        publications.each { publication ->
            createChadoFeaturePub(chadoFeature, publication)
        }
    }

    /**
     * Creates an instance of Chado pub for a given Apollo Publication and creates a linking relationship between
     * Chado feature and Chado pub via Chado feature_pub.
     * @param chadoFeature
     * @param publication
     */
    def createChadoFeaturePub(org.gmod.chado.Feature chadoFeature, org.bbop.apollo.Publication publication) {
        org.gmod.chado.Pub chadoPublication = createChadoPublication(publication)

        // Linking feature to publication via feature_pub
        org.gmod.chado.FeaturePub chadoFeaturePub = new org.gmod.chado.FeaturePub(
                feature: chadoFeature,
                pub: chadoPublication
        ).save()

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
     * Creates an instance of Chado featureprop for an Apollo FeatureProperty.
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
                type: getChadoCvterm(type, FEATURE_PROPERTY)
        ).save()
        endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado featureprop of type '${type}' and value ${featureProperty.value}: ${endTime - startTime} ms"

        featureProperty.featurePropertyPublications.each { featurePropertyPublication ->
            createChadoPropertyPub(chadoFeatureProp, featurePropertyPublication)
        }
        return chadoFeatureProp
    }

    /**
     * Creates an instance of Chado featureprop for a given Apollo FeatureProperty of type 'comment'.
     * @param chadoFeature
     * @param comment
     * @param rank
     * @return
     */
    def createChadoFeaturePropertyForComment(org.gmod.chado.Feature chadoFeature, org.bbop.apollo.FeatureProperty comment, int rank) {
        long startTime, endTime
        startTime = System.currentTimeMillis()
        org.gmod.chado.Featureprop chadoFeatureProp = new org.gmod.chado.Featureprop(
                value: comment.value,
                rank: rank,
                feature: chadoFeature,
                type: getChadoCvterm("comment", FEATURE_PROPERTY)
        ).save()
        endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado featureprop of type 'comment' and value ${comment.value}: ${endTime - startTime} ms"
        return chadoFeatureProp
    }

    /**
     * Creates an instance of Chado pub for a given Apollo publication and creates a linking relationship
     * beteen Chado featureprop and Chado pub via Chado featureprop_pub.
     * @param chadoFeatureProp
     * @param publication
     */
    def createChadoPropertyPub(org.gmod.chado.Featureprop chadoFeatureProp, org.bbop.apollo.Publication publication) {
        org.gmod.chado.Pub chadoPublication = createChadoPublication(publication)
        org.gmod.chado.FeaturepropPub featurePropertyPublication = new org.gmod.chado.FeaturepropPub(
                featureprop: chadoFeatureProp,
                pub: chadoPublication
        ).save()
        return featurePropertyPublication
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
     *
     * @param dbxref
     * @return
     */
    def getChadoDbxref(org.bbop.apollo.DBXref dbxref) {
        org.gmod.chado.Dbxref chadoDbxref
        def dbxrefResult
        if (dbxref.version == null) {
            dbxrefResult = org.gmod.chado.Dbxref.executeQuery(
                    "SELECT DISTINCT d FROM org.gmod.chado.Dbxref d JOIN d.db db WHERE d.accession = :queryDbxref AND db.name = :queryDb",
                    [queryDbxref: dbxref.accession, queryDb: dbxref.db.name])
        }
        else {
            dbxrefResult = org.gmod.chado.Dbxref.executeQuery(
                    "SELECT DISTINCT d FROM org.gmod.chado.Dbxref d JOIN d.db db WHERE d.accession = :queryDbxref AND d.version = :queryDbxrefVersion AND db.name = :queryDb",
                    [queryDbxref: dbxref.accession, queryDbxrefVersion: dbxref.version, queryDb: dbxref.db.name])
        }

        if (dbxrefResult.size() == 0) {
            chadoDbxref = createChadoDbxref(dbxref)
        }
        else if (dbxrefResult.size() == 1) {
            chadoDbxref = dbxrefResult.get(0)
        }
        else {
            log.error "${dbxrefResult} - More than one result found for dbxref '${dbxref.db.name}:${dbxref.accession}'. Returning null."
            chadoDbxref = null
        }
        return chadoDbxref
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
     * Chado Dbxref via Chado feature_dbxref and creates Chado dbxrefprop.
     * @param chadoFeature
     * @param dbxref
     * @return
     */
    def createChadoDbxref(org.gmod.chado.Feature chadoFeature, org.bbop.apollo.DBXref dbxref) {
        long startTime, endTime
        startTime = System.currentTimeMillis()
        org.gmod.chado.Dbxref chadoDbxref = getChadoDbxref(dbxref)
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
        return chadoDbxref
    }

    /**
     * Creates an instance of Chado db for a given Apollo DB.
     * @param db
     * @return
     */
    def createChadoDb(org.bbop.apollo.DB db) {
        long startTime, endTime
        startTime = System.currentTimeMillis()
        org.gmod.chado.Db chadoDb = new org.gmod.chado.Db(
                name: db.name,
                description: db.description,
                urlprefix: db.urlPrefix,
                url: db.url
        ).save()
        endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado Db for ${db.name} : ${endTime - startTime} ms"
        return chadoDb
    }

    /**
     *
     * @param dbxref
     * @return
     */
    def createChadoDbxref(org.bbop.apollo.DBXref dbxref) {
        // create Chado db for dbxref
        org.gmod.chado.Db db = getChadoDb(dbxref.db)

        // create Chado dbxref
        org.gmod.chado.Dbxref chadoDbxref = new org.gmod.chado.Dbxref(
                accession: dbxref.accession,
                description: dbxref.description,
                db: db
        )
        if (dbxref.version != null) {
            chadoDbxref.version = Integer.getInteger(dbxref.version)
        }
        chadoDbxref.save()

        dbxref.dbxrefProperties.each { dbxrefProperty ->
            createChadoDbxrefProp(chadoDbxref, dbxrefProperty)
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
                type: getChadoCvterm(dbXrefProperty.type.name),
                value: dbXrefProperty.value,
                rank: dbXrefProperty.rank
        ).save()
        return chadoDbxrefProp
    }

    /**
     * wrapper for createChadoFeatureCvTerm()
     * @param chadoFeature
     * @param featureCVTerms
     * @return
     */
    def createChadoFeatureCvterm(org.gmod.chado.Feature chadoFeature, Set<org.bbop.apollo.FeatureCVTerm> featureCVTerms) {
        featureCVTerms.each { featureCvterm ->
            createChadoFeatureCvterm(chadoFeature, featureCvterm)
        }
    }

    /**
     * Creates an instance of Chado feature_cvterm for a given Apollo FeatureCVTerm.
     * @param chadoFeature
     * @param featureCvterm
     * @return
     */
    def createChadoFeatureCvterm(org.gmod.chado.Feature chadoFeature, org.bbop.apollo.FeatureCVTerm featureCvterm) {
        org.gmod.chado.Pub chadoPublication = createChadoPublication(featureCvterm.publication)
        org.gmod.chado.FeatureCvterm chadoFeatureCvterm = new org.gmod.chado.FeatureCvterm(
                feature: chadoFeature,
                cvterm: getChadoCvterm(featureCvterm.cvterm.name),
                pub: chadoPublication,
                rank: featureCvterm.rank,
                isNot: featureCvterm.isNot
        ).save()
        return chadoFeatureCvterm
    }

    /**
     * Queries the database and returns an instance of Chado pub.
     * If the query returns no result then creates a Chado pub.
     * @param publication
     * @return
     */
    def getChadoPublication(org.bbop.apollo.Publication publication) {
        org.gmod.chado.Pub chadoPublication
        def pubResult = org.gmod.chado.Pub.executeQuery(
                "SELECT DISTINCT p FROM org.gmod.chado.Pub p JOIN p.type c WHERE p.id = :queryPublicationId AND c.name = :queryPublicationType",
                [queryPublicationId: publication.uniqueName, queryPublicationType: publication.type.name])

        if (pubResult.size() == 0) {
            chadoPublication = createChadoPublication(publication)
        }
        else if (pubResult.size() == 1) {
            chadoPublication = pubResult.get(0)
        }
        else {
            log.error "${pubResult} - More than one result found for publication '${publication.uniqueName}' of type '${publication.type.name}'. Returning null."
            chadoPublication = null
        }
        return chadoPublication
    }

    /**
     * Creates an instance of Chado pub for a given Apollo Publication.
     * @param publication
     * @return
     */
    def createChadoPublication(org.bbop.apollo.Publication publication) {
        org.gmod.chado.Pub chadoPublication
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

        // TODO - Chado pubauthor
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

        /* Cannot set Chado pubprop as Apollo Publication does not have Publication Properties. */

        // Chado pub_dbxref
        publication.publicationDBXrefs.each { publicationDbxref ->
            // Chado dbxref
            org.gmod.chado.Dbxref chadoDbxref = getChadoDbxref(publicationDbxref.dbxref)

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
                    cvterm: getChadoCvterm(publicationRelationship.type.name)
            )
            org.gmod.chado.Pub objectPublication = getChadoPublication(publicationRelationship.objectPublication)
            chadoPubRelationship.object = objectPublication
            chadoPubRelationship.save()
        }
        return chadoPublication
    }

    /**
     * A wrapper for createChadoFeatureForSequence() for handling multiple sequences
     * @param sequences
     * @return
     */
    def createChadoFeatureForSequences(Organism organism, Collection<Sequence> sequences, boolean storeSequence = false) {
        org.gmod.chado.Feature chadoFeature
        sequences.each { sequence ->
            chadoFeature = createChadoFeatureForSequence(organism, sequence, storeSequence)
        }
        chadoFeature.save(flush: true)
    }

    /**
     * Creates a Feature instance for an Apollo Sequence instance.
     * Optionally, also stores the residues (default is true).
     * @param organism
     * @param sequence
     * @param storeSequence
     * @return chadoFeature
     */
    def createChadoFeatureForSequence(Organism organism, org.bbop.apollo.Sequence sequence, boolean storeSequence = false) {
        long startTime, endTime
        Timestamp timeStamp = generateTimeStamp()
        startTime = System.currentTimeMillis()
        org.gmod.chado.Feature chadoFeature = getChadoFeatureForSequence(organism, sequence)

        if (chadoFeature == null) {
            chadoFeature = new org.gmod.chado.Feature(
                    uniquename: generateUniqueName(),
                    name: sequence.name,
                    seqlen: sequence.end - sequence.start,
                    isAnalysis: false,
                    isObsolete: false,
                    timelastmodified: timeStamp,
                    timeaccessioned: timeStamp,
                    organism: chadoOrganismsMap.get(organism.commonName),
                    type: getChadoCvterm("chromosome", SEQUENCE_ONTOLOGY)
            )
        }

        if (storeSequence && chadoFeature.residues == null) {
            String residues = sequenceService.getRawResiduesFromSequence(sequence, sequence.start, sequence.end)
            chadoFeature.residues = residues
            chadoFeature.md5checksum = generateMD5checksum(residues)
        }

        chadoFeature.save()
        exportStatisticsMap['sequence_feature_count'] += 1
        endTime = System.currentTimeMillis()
        log.debug "Time taken to create Chado Feature for sequence ${sequence.name}: ${endTime - startTime} ms"
        return chadoFeature
    }

    def getChadoFeatureForSequence(Organism organism, org.bbop.apollo.Sequence sequence) {
        org.gmod.chado.Feature chadoFeature
        def sequenceResults = org.gmod.chado.Feature.executeQuery(
                "SELECT DISTINCT s FROM org.gmod.chado.Feature s WHERE s.name = :querySequenceName AND s.organism.genus = :queryGenus AND s.organism.species = :querySpecies AND s.type.name = :querySequenceType",
                [querySequenceName: sequence.name, queryGenus: organism.genus, querySpecies: organism.species, querySequenceType: "chromosome"])
        if (sequenceResults.size() == 0) {
            chadoFeature = null
        }
        else if (sequenceResults.size() == 1) {
            chadoFeature = sequenceResults.get(0)
        }
        else {
            log.error "${sequenceResults} - More than one result found for sequence name ${sequence.name}. Returning null."
            chadoFeature = null
        }
        return chadoFeature
    }

    /**
     * Queries the Chado database and returns a Chado cvterm corresponding to a given CvTerm name.
     * @param term
     * @return
     */
    def getChadoCvterm(String cvTermString) {
        org.gmod.chado.Cvterm chadoCvterm
        long startTime = System.currentTimeMillis()
        def cvTermList = org.gmod.chado.Cvterm.executeQuery(
                "SELECT DISTINCT t FROM org.gmod.chado.Cvterm t WHERE t.name = :queryCvTerm",
                [queryCvTerm: cvTermString])
        long endTime = System.currentTimeMillis()
        log.debug "Time taken for querying ChadoCvTerm ${cvTermString}: ${endTime - startTime} ms"

        if (cvTermList.size() == 0) {
            chadoCvterm = null
        }
        else if (cvTermList.size() == 1) {
            chadoCvterm = cvTermList.get(0)
        }
        else {
            log.error "${cvTermList} - More than one result found for cvterm: ${cvTermString}. Returning null."
            chadoCvterm = null
        }

        return chadoCvterm
    }

    /**
     * Queries the Chado database and returns a Chado cvterm corresponding to a given CvTerm name and CV name.
     * @param term
     * @param cvString
     * @return
     */
    def getChadoCvterm(String cvTermString, String cvString) {
        org.gmod.chado.Cvterm chadoCvterm
        long startTime = System.currentTimeMillis()
        def cvTermList = org.gmod.chado.Cvterm.executeQuery(
                "SELECT DISTINCT t FROM org.gmod.chado.Cvterm t JOIN t.cv cv WHERE cv.name = :queryCvString AND t.name = :queryCvTerm",
                [queryCvString: cvString, queryCvTerm: cvTermString])
        long endTime = System.currentTimeMillis()
        log.debug "Time taken for querying for ChadoCvTerm ${cvTermString} of CV ${cvString}: ${endTime - startTime} ms"

        if (cvTermList.size() == 0) {
            chadoCvterm = null
        }
        else if (cvTermList.size() == 1) {
            chadoCvterm = cvTermList.get(0)

        }
        else {
            log.error "${cvTermList} - More than one result found for cvterm: ${cvTermString} of cv: ${cvString}. Returning null."
            chadoCvterm = null
        }

        return chadoCvterm
    }

    /**
     * Queries the Chado database and returns a Chado db corresponding to a given db
     * @param dbName
     * @return
     */
    def getChadoDb(org.bbop.apollo.DB db) {
        org.gmod.chado.Db chadoDb
        long startTime = System.currentTimeMillis()
        def dbResult = org.gmod.chado.Db.findAllByName(db.name)
        long endTime = System.currentTimeMillis()
        log.debug "Time taken for querying for db ${db.name}: ${endTime - startTime} ms"

        if (dbResult.size() == 0) {
            chadoDb = createChadoDb(db)
        }
        else if (dbResult.size() == 1) {
            chadoDb = dbResult.get(0)
        }
        else {
            log.error "${dbResult} - More than one result found for db name '${db.name}'. Returning null."
            chadoDb = null
        }

        return chadoDb
    }

    /**
     * Creates an instance of Chado Organism for a given Apollo Organism.
     * @param organism
     * @return
     */
    def createChadoOrganism(org.bbop.apollo.Organism organism) {
        org.gmod.chado.Organism chadoOrganism = getChadoOrganism(organism)
        if (chadoOrganism == null) {
            chadoOrganism = new org.gmod.chado.Organism(
                    abbreviation: organism.abbreviation,
                    genus: organism.genus,
                    species: organism.species,
                    commonName: organism.commonName,
                    comment: organism.comment
            ).save()

            organism.organismDBXrefs.each { organismDbxref ->
                createChadoOrganismDbxref(chadoOrganism, organismDbxref)
            }

            organism.organismProperties.each { organismProperty ->
                createChadoOrganismProperty(chadoOrganism, organismProperty)
            }

            chadoOrganism.save(flush: true)
        }
        chadoOrganismsMap.put(organism.commonName, chadoOrganism)
        return chadoOrganism
    }

    /**
     * Queries the Chado database for an Organism that has the same genus and species name as that of given Apollo Organism.
     * @param organism
     * @return
     */
    def getChadoOrganism(org.bbop.apollo.Organism organism) {
        org.gmod.chado.Organism chadoOrganism
        def organismResults = org.gmod.chado.Organism.executeQuery(
                "SELECT DISTINCT o FROM org.gmod.chado.Organism o WHERE o.genus = :queryGenus AND o.species = :querySpecies",
                [queryGenus: organism.genus, querySpecies: organism.species])

        if (organismResults.size() == 0) {
            chadoOrganism = null
        }
        else if (organismResults.size() == 1) {
            chadoOrganism = organismResults.get(0)
        }
        else {
            log.error "${organismResults} - more than one result found for organism '${organism.genus} ${organism.species}'. Returning null."
            chadoOrganism = null
        }

        return chadoOrganism
    }

    /**
     * Creates an instance of Chado organism_dbxref from a given Apollo OrganismDBXref
     */
    def createChadoOrganismDbxref(org.gmod.chado.Organism chadoOrganism, org.bbop.apollo.OrganismDBXref organismDbxref) {
        org.gmod.chado.Dbxref chadoDbxref = getChadoDbxref(organismDbxref.dbxref)
        org.gmod.chado.OrganismDbxref chadoOrganismDbxref = new org.gmod.chado.OrganismDbxref(
                organism: chadoOrganism,
                dbxref: chadoDbxref
        ).save()
        return chadoOrganismDbxref
    }

    /**
     *
     * @param chadoOrganism
     * @param organismProperty
     */
    def createChadoOrganismProperty(org.gmod.chado.Organism chadoOrganism, org.bbop.apollo.OrganismProperty organismProperty) {}

    /**
     * Generates and returns an instance of java.SQL.Timestamp for the current time, without timezone,
     * which is required by Chado for Feature attributes such as timelastmodified and timelastaccessioned.
     * @return Timestamp
     */
    def generateTimeStamp() {
        return new Timestamp(System.currentTimeMillis())
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

    /**
     * Checks the database for existing Chado features in the feature table.
     * @return
     */
    def checkForExistingFeatures() {
        int chadoFeatureCount = org.gmod.chado.Feature.count
        org.gmod.chado.Feature.all.size()
        if (chadoFeatureCount > 0) {
            return true
        }
        return false
    }

    /**
     * Checks the database to make sure that the cvterm table is not empty.
     * @return
     */
    def checkForOntologies() {
        int cvTermCount = org.gmod.chado.Cvterm.count
        if (cvTermCount > 0) {
            return true
        }
        return false
    }

    /**
     * Initialize map for gathering export statistics.
     * @return
     */
    def initializeExportStatistics() {
        exportStatisticsMap['feature_count'] = 0
        exportStatisticsMap['featureloc_count'] = 0
        exportStatisticsMap['sequence_feature_count'] = 0
    }
}
