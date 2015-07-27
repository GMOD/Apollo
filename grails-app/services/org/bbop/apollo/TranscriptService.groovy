package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.json.JSONArray
import org.grails.plugins.metrics.groovy.Timed


//@GrailsCompileStatic
@Transactional(readOnly = true)
class TranscriptService {

    List<String> ontologyIds = [Transcript.ontologyId, SnRNA.ontologyId, MRNA.ontologyId, SnoRNA.ontologyId, MiRNA.ontologyId, TRNA.ontologyId, NcRNA.ontologyId, RRNA.ontologyId]

    // services
    def featureService
    def featureRelationshipService
    def exonService
    def nameService
    def nonCanonicalSplitSiteService
    def sequenceService
    def featureEventService

    /** Retrieve the CDS associated with this transcript.  Uses the configuration to determine
     *  which child is a CDS.  The CDS object is generated on the fly.  Returns <code>null</code>
     *  if no CDS is associated.
     *
     * @return CDS associated with this transcript
     */
    public CDS getCDS(Transcript transcript) {
        return (CDS) featureRelationshipService.getChildForFeature(transcript, CDS.ontologyId)

    }

    /** Retrieve all the exons associated with this transcript.  Uses the configuration to determine
     *  which children are exons.  Exon objects are generated on the fly.  The collection
     *  will be empty if there are no exons associated with the transcript.
     *
     * @return Collection of exons associated with this transcript
     */
    public Collection<Exon> getExons(Transcript transcript) {
        return (Collection<Exon>) featureRelationshipService.getChildrenForFeatureAndTypes(transcript, Exon.ontologyId)
    }

    public Collection<Exon> getSortedExons(Transcript transcript) {
        Collection<Exon> exons = getExons(transcript)
        List<Exon> sortedExons = new LinkedList<Exon>(exons);
        Collections.sort(sortedExons, new FeaturePositionComparator<Exon>(false))
        return sortedExons
    }

    /** Retrieve the gene that this transcript is associated with.  Uses the configuration to
     * determine which parent is a gene.  The gene object is generated on the fly.  Returns
     * <code>null</code> if this transcript is not associated with any gene.
     *
     * @return Gene that this Transcript is associated with
     */
    public Gene getGene(Transcript transcript) {
        return (Gene) featureRelationshipService.getParentForFeature(transcript, Gene.ontologyId, Pseudogene.ontologyId)
    }

    public Pseudogene getPseudogene(Transcript transcript) {
        return (Pseudogene) featureRelationshipService.getParentForFeature(transcript, Pseudogene.ontologyId)
    }

    public boolean isProteinCoding(Transcript transcript) {
        return transcript instanceof MRNA
//        if (getGene(transcript) != null && getGene(transcript) instanceof Pseudogene) {
//            return false;
//        }
//        return true;
    }

    @Transactional
    CDS createCDS(Transcript transcript) {
        String uniqueName = transcript.getUniqueName() + FeatureStringEnum.CDS_SUFFIX.value;

        CDS cds = new CDS(
                uniqueName: uniqueName
                , isAnalysis: transcript.isAnalysis
                , isObsolete: transcript.isObsolete
                , name: uniqueName
        ).save(failOnError: true)

        FeatureLocation transcriptFeatureLocation = FeatureLocation.findByFeature(transcript)

        FeatureLocation featureLocation = new FeatureLocation(
                strand: transcriptFeatureLocation.strand
                , sequence: transcriptFeatureLocation.sequence
                , fmin: transcriptFeatureLocation.fmin
                , fmax: transcriptFeatureLocation.fmax
                , feature: cds
        ).save(insert: true, failOnError: true)
        cds.addToFeatureLocations(featureLocation);
        cds.save(flush: true, insert: true)
        return cds;
    }

    /** Delete a transcript.  Deletes both the gene -> transcript and transcript -> gene
     *  relationships.
     *
     * @param transcript - Transcript to be deleted
     */
    @Transactional
    public void deleteTranscript(Gene gene, Transcript transcript) {
        featureRelationshipService.removeFeatureRelationship(gene, transcript)

        // update bounds
        Integer fmin = null;
        Integer fmax = null;
        for (Transcript t : getTranscripts(gene)) {
            if (fmin == null || t.getFeatureLocation().getFmin() < fmin) {
                fmin = t.getFeatureLocation().getFmin();
            }
            if (fmax == null || t.getFeatureLocation().getFmax() > fmax) {
                fmax = t.getFeatureLocation().getFmax();
            }
        }
        if (fmin != null) {
            setFmin(transcript, fmin)
        }
        if (fmax != null) {
            setFmax(transcript, fmax)
        }
//        transcript.save(flush: true )
    }

    /** Retrieve all the transcripts associated with this gene.  Uses the configuration to determine
     *  which children are transcripts.  Transcript objects are generated on the fly.  The collection
     *  will be empty if there are no transcripts associated with the gene.
     *
     * @return Collection of transcripts associated with this gene
     */
    public Collection<Transcript> getTranscripts(Gene gene) {
        return (Collection<Transcript>) featureRelationshipService.getChildrenForFeatureAndTypes(gene, ontologyIds as String[])
    }

    List<Transcript> getTranscriptsSortedByFeatureLocation(Gene gene, boolean sortByStrand) {
        return getTranscripts(gene).sort(true, new FeaturePositionComparator<Transcript>(sortByStrand))
    }

    @Transactional
    public void setFmin(Transcript transcript, Integer fmin) {
        transcript.getFeatureLocation().setFmin(fmin);
        Gene gene = getGene(transcript)
        if (gene != null && fmin < gene.getFmin()) {
            featureService.setFmin(gene, fmin)
        }
    }

    @Transactional
    public void setFmax(Transcript transcript, Integer fmax) {
        transcript.getFeatureLocation().setFmax(fmax);
        Gene gene = getGene(transcript)
        if (gene != null && fmax > gene.getFmax()) {
            featureService.setFmax(gene, fmax);
        }
    }

    @Transactional
    def updateGeneBoundaries(Transcript transcript) {
        Gene gene = getGene(transcript)
        if (gene == null) {
            return;
        }
        int geneFmax = Integer.MIN_VALUE;
        int geneFmin = Integer.MAX_VALUE;
        for (Transcript t : getTranscripts(gene)) {
            if (t.getFmin() < geneFmin) {
                geneFmin = t.getFmin();
            }
            if (t.getFmax() > geneFmax) {
                geneFmax = t.getFmax();
            }
        }
        featureService.setFmin(gene, geneFmin)
        featureService.setFmax(gene, geneFmax)

        // not sure if we want this if not actually saved
//        gene.setLastUpdated(new Date());
    }

    List<String> getFrameShiftOntologyIds() {
        List<String> intFrameshiftOntologyIds = new ArrayList<>()

        intFrameshiftOntologyIds.add(Plus1Frameshift.ontologyId)
        intFrameshiftOntologyIds.add(Plus2Frameshift.ontologyId)
        intFrameshiftOntologyIds.add(Minus1Frameshift.ontologyId)
        intFrameshiftOntologyIds.add(Minus2Frameshift.ontologyId)


        return intFrameshiftOntologyIds
    }

    List<Frameshift> getFrameshifts(Transcript transcript) {
        return featureRelationshipService.getFeaturePropertyForTypes(transcript, frameShiftOntologyIds)
    }

    /** Set the CDS associated with this transcript.  Uses the configuration to determine
     *  the default term to use for CDS features.
     *
     * @param cds - CDS to be set to this transcript
     */
    @Transactional
    public void setCDS(Feature feature, CDS cds, boolean replace = true) {
        if (replace) {
            log.debug "replacing CDS on feature"
            if (featureRelationshipService.setChildForType(feature, cds)) {
                log.debug "returning "
                return
            }
        }

        FeatureRelationship fr = new FeatureRelationship(
//                type:partOfCvTerm
                parentFeature: feature
                , childFeature: cds
                , rank: 0
        ).save(insert: true, failOnError: true)


        log.debug "fr: ${fr}"
        log.debug "feature: ${feature}"
        log.debug "cds: ${cds}"
        feature.addToParentFeatureRelationships(fr)
        cds.addToChildFeatureRelationships(fr)

        cds.save()
        feature.save(flush: true)
    }

    @Transactional
    def addExon(Transcript transcript, Exon exon) {

        log.debug "exon feature locations ${exon.featureLocation}"
        log.debug "transcript feature locations ${transcript.featureLocation}"
        if (exon.getFeatureLocation().getFmin() < transcript.getFeatureLocation().getFmin()) {
            transcript.getFeatureLocation().setFmin(exon.getFeatureLocation().getFmin());
        }
        if (exon.getFeatureLocation().getFmax() > transcript.getFeatureLocation().getFmax()) {
            transcript.getFeatureLocation().setFmax(exon.getFeatureLocation().getFmax());
        }
        transcript.save()

        // if the transcript's bounds are beyond the gene's bounds, need to adjust the gene's bounds
        Gene gene = getGene(transcript)
        if (gene) {
            if (transcript.featureLocation.getFmin() < gene.featureLocation.getFmin()) {
                gene.featureLocation.setFmin(transcript.featureLocation.getFmin());
            }
            if (transcript.featureLocation.getFmax() > gene.featureLocation.getFmax()) {
                gene.featureLocation.setFmax(transcript.getFmax());
            }
        }
        gene.save()

        int initialSize = transcript.parentFeatureRelationships?.size() ?: 0
        log.debug "initial size: ${initialSize}" // 3
        featureRelationshipService.addChildFeature(transcript, exon, false)
        int finalSize = transcript.parentFeatureRelationships?.size()
        log.debug "final size: ${finalSize}" // 4 (+1 exon)


        featureService.removeExonOverlapsAndAdjacencies(transcript)
        log.debug "post remove exons: ${transcript.parentFeatureRelationships?.size()}" // 6 (+2 splice sites)
//
//        // if the exon is removed during a merge, then we will get a null-pointer
        updateGeneBoundaries(transcript);  // 6, moved transcript fmin, fmax
        log.debug "post update gene boundaries: ${transcript.parentFeatureRelationships?.size()}"
    }

    Transcript getParentTranscriptForFeature(Feature feature) {
        return (Transcript) featureRelationshipService.getParentForFeature(feature, ontologyIds as String[])
    }

    @Transactional
    Transcript splitTranscript(Transcript transcript, Exon leftExon, Exon rightExon) {
        List<Exon> exons = exonService.getSortedExons(transcript)
        Transcript splitTranscript = (Transcript) transcript.getClass().newInstance()
        splitTranscript.uniqueName = nameService.generateUniqueName()
        splitTranscript.name = nameService.generateUniqueName(transcript)
        splitTranscript.save()

        // copy feature locations if right of right exon
        transcript.featureLocations.each { featureLocation ->
            FeatureLocation newFeatureLocation = new FeatureLocation(
                    fmin: featureLocation.fmin
                    , fmax: featureLocation.fmax
                    , rank: featureLocation.rank
                    , sequence: featureLocation.sequence
                    , strand: featureLocation.strand

                    , feature: splitTranscript
            ).save()
            splitTranscript.addToFeatureLocations(newFeatureLocation)
        }
        splitTranscript.save(flush: true)

        Gene gene = getGene(transcript)
        if (gene) {
            featureService.addTranscriptToGene(gene, splitTranscript)
        } else {
            featureService.addFeature(splitTranscript)
        }

        FeatureLocation transcriptFeatureLocation = transcript.featureLocation
        transcriptFeatureLocation.fmax = leftExon.fmax
        FeatureLocation splitFeatureLocation = splitTranscript.featureLocation
        log.debug "right1: ${rightExon.featureLocation}"
        splitFeatureLocation.fmin = rightExon.featureLocation.fmin
        log.debug "right2: ${rightExon.featureLocation}"
        for (Exon exon : exons) {
            FeatureLocation exonFeatureLocation = exon.featureLocation
            FeatureLocation leftFeatureLocation = leftExon.featureLocation
            if (exonFeatureLocation.fmin > leftFeatureLocation.getFmin()) {
                log.debug "right3: ${rightExon.featureLocation}"
//                featureRelationshipService.removeFeatureRelationship()
//                exonService.deleteExon(transcript, exon);
                if (exon.equals(rightExon)) {
                    featureRelationshipService.removeFeatureRelationship(transcript, rightExon)
                    log.debug "right4: ${rightExon.featureLocation}"
                    addExon(splitTranscript, rightExon);
                } else {
                    featureRelationshipService.removeFeatureRelationship(transcript, exon)
                    log.debug "right5: ${rightExon.featureLocation}"
                    addExon(splitTranscript, exon);
                }
            }
        }

        return splitTranscript
    }

    /**
     * Duplicate a transcript.  Adds it to the parent gene if it is set.
     *
     * @param transcript - Transcript to be duplicated
     */
    @Transactional
    public Transcript duplicateTranscript(Transcript transcript) {
        Transcript duplicate = (Transcript) transcript.generateClone();
        duplicate.name = transcript.name + "-copy"
        duplicate.uniqueName = nameService.generateUniqueName(transcript)

        Gene gene = getGene(transcript)
        if (gene) {
            featureService.addTranscriptToGene(gene, duplicate)
            gene.save()
        }
        // copy exons
        for (Exon exon : getExons(transcript)) {
            Exon duplicateExon = (Exon) exon.generateClone()
            duplicateExon.name = exon.name + "-copy"
            duplicateExon.uniqueName = nameService.generateUniqueName(duplicateExon)
            addExon(duplicate, duplicateExon)
        }
        // copy CDS
        CDS cds = getCDS(transcript)
        if (cds) {
            CDS duplicateCDS = (CDS) cds.generateClone()
            duplicateCDS.name = cds.name + "-copy"
            duplicateCDS.uniqueName = nameService.generateUniqueName(duplicateCDS)
            setCDS(duplicate, cds)
        }


        duplicate.save()

        return duplicate
    }

    @Transactional
    def mergeTranscripts(Transcript transcript1, Transcript transcript2) {
        // Merging transcripts basically boils down to moving all exons from one transcript to the other

        for (Exon exon : getExons(transcript2)) {
            featureRelationshipService.removeFeatureRelationship(transcript2, exon)
            addExon(transcript1, exon)
        }
//        transcript1.save()
        Gene gene1 = getGene(transcript1)
        Gene gene2 = getGene(transcript2)
        if (gene1) {
            gene1.save(flush: true)
        }
        // if the parent genes aren't the same, this leads to a merge of the genes
        if (gene1 && gene2) {
            if (gene1 != gene2) {
                List<Transcript> gene2Transcripts = getTranscripts(gene2)
                for (Transcript transcript : gene2Transcripts) {
                    if (transcript != transcript2) {
                        deleteTranscript(gene2, transcript)
                        featureService.addTranscriptToGene(gene1, transcript)
                    }
                }
                featureRelationshipService.deleteFeatureAndChildren(gene2)
            }
        }
        // Delete the empty transcript from the gene, if gene not already deleted
        if (getGene(transcript2)) {
            def childFeatures = featureRelationshipService.getChildren(transcript2)
            featureRelationshipService.deleteChildrenForTypes(transcript2)
            Feature.deleteAll(childFeatures)
            deleteTranscript(gene2, transcript2);
            featureEventService.deleteHistory(transcript2.uniqueName)
        } else {
            featureService.deleteFeature(transcript2);
        }
        featureService.removeExonOverlapsAndAdjacencies(transcript1);
    }

    @Transactional
    Transcript flipTranscriptStrand(Transcript oldTranscript) {
        Gene oldGene = getGene(oldTranscript)
        boolean isPseudogene = oldGene instanceof Pseudogene
//        featureRelationshipService.removeFeatureRelationship(oldGene, oldTranscript)

//        if (getTranscripts(oldGene)?.size() == 0) {
//            featureService.deleteFeature(oldGene)
//        }
        oldTranscript = featureService.flipStrand(oldTranscript)
        oldTranscript.save()
        nonCanonicalSplitSiteService.findNonCanonicalAcceptorDonorSpliceSites(oldTranscript)
        oldTranscript.save()

//        String oldGeneName = oldGene.name
//        String oldTranscriptName = oldTranscript.name
//        JSONObject jsonTranscript = featureService.convertFeatureToJSON(oldTranscript, false)
//        JSONObject requestJSONObject = requestHandlingService.createJSONFeatureContainer(jsonTranscript)
//        requestJSONObject.put(FeatureStringEnum.TRACK.value,oldTranscript.featureLocation.sequence.name)

//        String sequenceName = oldTranscript.featureLocation.sequence.name
//        Transcript newTranscript = featureService.generateTranscript(jsonTranscript,sequenceName)
//        newTranscript.name = oldTranscriptName
//        newTranscript.save()

//        JSONObject newJsonTranscript = requestHandlingService.addTranscript(requestJSONObject)
//        if (getTranscripts(oldGene).size() == 0) {
//            oldGene.delete(flush: true)
//        } else {
//            oldGene.save(flush: true )
//        }
//        deleteTranscript(oldGene,oldTranscript)
//        oldTranscript.delete(flush: true )
//        writeFeatureToStore(editor, dataStore, getTopLevelFeatureForTranscript(newTranscript), track);
//        if (historyStore != null) {
//            Transaction transaction = new Transaction(Transaction.Operation.FLIP_STRAND, newTranscript.getUniqueName(), username);
//            transaction.addNewFeature(newTranscript);
//            writeHistoryToStore(historyStore, transaction);
//        }
//        JSONArray features = newJsonTranscript.getJSONArray(FeatureStringEnum.FEATURES.value)
//        String uniqueName = features.getJSONObject(0).getString(FeatureStringEnum.UNIQUENAME.value)
//        Transcript newTranscript = Transcript.findByUniqueName(uniqueName)

        return oldTranscript;
    }

    String getResiduesFromTranscript(Transcript transcript) {
        def exons = exonService.getSortedExons(transcript)
        if (!exons) {
            return null
        }

        StringBuilder residues = new StringBuilder()
        for (Exon exon in exons) {
            residues.append(sequenceService.getResiduesFromFeature(exon))
        }
        return residues.size() > 0 ? residues.toString() : null
    }

    Transcript getTranscript(CDS cds) {
        return (Transcript) featureRelationshipService.getParentForFeature(cds, ontologyIds as String[])
    }

    /**
     * @param gsolFeature
     * @param includeSequence
     * @return
     */
    @Timed
    JSONObject convertTranscriptToJSON(Transcript gsolFeature) {
        JSONObject jsonFeature = new JSONObject();
        try {
            if (gsolFeature.id) {
                jsonFeature.put(FeatureStringEnum.ID.value, gsolFeature.id);
            }
            jsonFeature.put(FeatureStringEnum.TYPE.value, featureService.generateJSONFeatureStringForType(gsolFeature.ontologyId));
            jsonFeature.put(FeatureStringEnum.UNIQUENAME.value, gsolFeature.getUniqueName());
            if (gsolFeature.getName() != null) {
                jsonFeature.put(FeatureStringEnum.NAME.value, gsolFeature.getName());
            }
            if (gsolFeature.symbol) {
                jsonFeature.put(FeatureStringEnum.SYMBOL.value, gsolFeature.symbol);
            }
            if (gsolFeature.description) {
                jsonFeature.put(FeatureStringEnum.DESCRIPTION.value, gsolFeature.description);
            }
            long start = System.currentTimeMillis();
            String finalOwnerString = ""
            if (gsolFeature.owners) {
                String ownerString = ""
                for (owner in gsolFeature.owners) {
                    ownerString += gsolFeature.owner.username + " "
                }
                finalOwnerString = ownerString?.trim()
            } else if (gsolFeature.owner) {
                finalOwnerString = gsolFeature?.owner?.username
            } else {
                finalOwnerString = "None"
            }
            jsonFeature.put(FeatureStringEnum.OWNER.value.toLowerCase(), finalOwnerString);


            start = System.currentTimeMillis();
            if (gsolFeature.featureLocation) {
                Sequence sequence = gsolFeature.featureLocation.sequence
                jsonFeature.put(FeatureStringEnum.SEQUENCE.value, sequence.name);
            }

            List<Feature> childFeatures = featureRelationshipService.getChildrenForFeatureAndTypes(gsolFeature)


            if (childFeatures) {
                JSONArray children = new JSONArray();
                jsonFeature.put(FeatureStringEnum.CHILDREN.value, children);
                for (Feature f : childFeatures) {
                    Feature childFeature = f
                    children.put(featureService.convertFeatureToJSON(childFeature));
                }
            }


            start = System.currentTimeMillis()
            // get parents
            List<Feature> parentFeatures = featureRelationshipService.getParentsForFeature(gsolFeature)

            //log.debug "parents ${durationInMilliseconds}"
            if (parentFeatures?.size() == 1) {
                Feature parent = parentFeatures.iterator().next();
                jsonFeature.put(FeatureStringEnum.PARENT_ID.value, parent.getUniqueName());
                jsonFeature.put(FeatureStringEnum.PARENT_TYPE.value, featureService.generateJSONFeatureStringForType(parent.ontologyId));
            }


            start = System.currentTimeMillis()

            Collection<FeatureLocation> featureLocations = gsolFeature.getFeatureLocations();
            if (featureLocations) {
                FeatureLocation gsolFeatureLocation = featureLocations.iterator().next();
                if (gsolFeatureLocation != null) {
                    jsonFeature.put(FeatureStringEnum.LOCATION.value, featureService.convertFeatureLocationToJSON(gsolFeatureLocation));
                }
            }


            if (gsolFeature instanceof SequenceAlteration) {
                SequenceAlteration sequenceAlteration = (SequenceAlteration) gsolFeature
                if (sequenceAlteration.alterationResidue) {
                    jsonFeature.put(FeatureStringEnum.RESIDUES.value, sequenceAlteration.alterationResidue);
                }
            }


            //e.g. properties: [{value: "demo", type: {name: "owner", cv: {name: "feature_property"}}}]
            Collection<FeatureProperty> gsolFeatureProperties = gsolFeature.getFeatureProperties();

            JSONArray properties = new JSONArray();
            jsonFeature.put(FeatureStringEnum.PROPERTIES.value, properties);
            if (gsolFeatureProperties) {
                for (FeatureProperty property : gsolFeatureProperties) {
                    JSONObject jsonProperty = new JSONObject();
                    JSONObject jsonPropertyType = new JSONObject()
                    if (property instanceof Comment) {
                        //  TODO: This is a hack
                        jsonPropertyType.put(FeatureStringEnum.NAME.value, "comment")
                        JSONObject jsonPropertyTypeCv = new JSONObject()
                        jsonPropertyTypeCv.put(FeatureStringEnum.NAME.value, FeatureStringEnum.FEATURE_PROPERTY.value)
                        jsonPropertyType.put(FeatureStringEnum.CV.value, jsonPropertyTypeCv)
                        jsonProperty.put(FeatureStringEnum.TYPE.value, jsonPropertyType);
                        jsonProperty.put(FeatureStringEnum.VALUE.value, property.getValue());
                        properties.put(jsonProperty);
                        continue
                    }
                    jsonPropertyType.put(FeatureStringEnum.NAME.value, property.type)
                    JSONObject jsonPropertyTypeCv = new JSONObject()
                    jsonPropertyTypeCv.put(FeatureStringEnum.NAME.value, FeatureStringEnum.FEATURE_PROPERTY.value)
                    jsonPropertyType.put(FeatureStringEnum.CV.value, jsonPropertyTypeCv)

                    jsonProperty.put(FeatureStringEnum.TYPE.value, jsonPropertyType);
                    jsonProperty.put(FeatureStringEnum.VALUE.value, property.getValue());
                    properties.put(jsonProperty);
                }
            }
            JSONObject ownerProperty = JSON.parse("{value: ${finalOwnerString}, type: {name: 'owner', cv: {name: 'feature_property'}}}") as JSONObject
            properties.put(ownerProperty)


            Collection<DBXref> gsolFeatureDbxrefs = gsolFeature.getFeatureDBXrefs();
            if (gsolFeatureDbxrefs) {
                JSONArray dbxrefs = new JSONArray();
                jsonFeature.put(FeatureStringEnum.DBXREFS.value, dbxrefs);
                for (DBXref gsolDbxref : gsolFeatureDbxrefs) {
                    JSONObject dbxref = new JSONObject();
                    dbxref.put(FeatureStringEnum.ACCESSION.value, gsolDbxref.getAccession());
                    dbxref.put(FeatureStringEnum.DB.value, new JSONObject().put(FeatureStringEnum.NAME.value, gsolDbxref.getDb().getName()));
                    dbxrefs.put(dbxref);
                }
            }
            jsonFeature.put(FeatureStringEnum.DATE_LAST_MODIFIED.value, gsolFeature.lastUpdated.time);
            jsonFeature.put(FeatureStringEnum.DATE_CREATION.value, gsolFeature.dateCreated.time);
        }
        catch (JSONException e) {
            log.error(e)
            return null;
        }
        return jsonFeature;
    }
}
