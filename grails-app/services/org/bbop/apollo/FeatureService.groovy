package org.bbop.apollo

import grails.transaction.Transactional
import groovy.transform.CompileStatic
import org.apache.shiro.SecurityUtils
import org.bbop.apollo.web.util.JSONUtil
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject
import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature
import org.gmod.gbol.bioObject.util.BioObjectUtil

//import org.json.JSONObject

/**
 * taken from AbstractBioFeature
 */
@CompileStatic
@Transactional
class FeatureService {

    def nameService
    def configWrapperService
    def transcriptService
    def cvTermService

    def addProperty(Feature feature, FeatureProperty property) {
        int rank = 0;
        for (FeatureProperty fp : feature.getFeatureProperties()) {
            if (fp.getType().equals(property.getType())) {
                if (fp.getRank() > rank) {
                    rank = fp.getRank();
                }
            }
        }
        property.setRank(rank + 1);
        boolean ok = feature.getFeatureProperties().add(property);

    }

    public boolean deleteProperty(Feature feature, FeatureProperty property) {
        for (FeatureProperty fp : feature.getFeatureProperties()) {
            if (fp.getType().equals(property.getType()) && fp.getValue().equals(property.getValue())) {
                feature.getFeatureProperties().remove(fp);
                return true;
            }
        }
    }

    /**
     * Is there a feature property called "owner"
     * @param feature
     * @return
     */
    public User getOwner(Feature feature) {
//        Collection<CVTerm> ownerCvterms = conf.getCVTermsForClass("Owner");
        List<CVTerm> ownerCvTerms = CVTerm.findAllByName("Owner")

        for (FeatureProperty fp : feature.getFeatureProperties()) {
            if (fp.type in ownerCvTerms) {
                return User.findByUsername(fp.type.name)
            }
//            if (ownerCvterms.contains(fp.getType())) {
//                return new User(fp, conf);
//            }
        }

        // if no owner found, try to get the first owner found in an ancestor
        for (FeatureRelationship fr : feature.getParentFeatureRelationships()) {
//            Feature parent = (AbstractBioFeature)BioObjectUtil.createBioObject(fr.getObjectFeature(), getConfiguration());
            Feature parent = fr.objectFeature // may be subject Feature . . not sure
            User parentOwner = getOwner(parent)
            if (parentOwner != null) {
                return parentOwner;
            }
        }

        return null;
    }

    public void setUser(Feature feature, User owner) {
//        Collection<CVTerm> ownerCvterms = conf.getCVTermsForClass("User");
        List<CVTerm> ownerCvTerms = CVTerm.findAllByName("Owner")

        for (FeatureProperty fp : feature.getFeatureProperties()) {
//            if (ownerCvterms.contains(fp.getType())) {
            if (fp.type in ownerCvTerms) {
                feature.getFeatureProperties().remove(fp);
                break;
            }
        }
        addProperty(feature,owner);
    }

    /** Set the owner of this feature.
     *
     * @param owner - User of this feature
     */
    public void setOwner(Feature feature,String owner) {
        User user = User.findByUsername(owner)
        if(user){
            setOwner(feature,user)
        }
        else{
            throw new AnnotationException("User ${owner} not found")
        }
//        setOwner(new User(owner));
    }

    public void setOwner(Feature feature,User user){
        addProperty(feature,user)
    }


    def generateTranscript(JSONObject jsonTranscript, String trackName) {
        Gene gene = jsonTranscript.has("parent_id") ? (Gene) Feature.findByUniqueName(jsonTranscript.getString("parent_id")) : null;
        Transcript transcript = null
        FeatureLazyResidues featureLazyResidues = FeatureLazyResidues.findByName(trackName)
        if (gene != null) {
//            Feature gsolTranscript = convertJSONToFeature(jsonTranscript, featureLazyResidues);
            transcript = convertJSONToTranscript(jsonTranscript, featureLazyResidues);
//            transcript = (Transcript) BioObjectUtil.createBioObject(gsolTranscript, bioObjectConfiguration);
            if (transcript.getFmin() < 0 || transcript.getFmax() < 0) {
                throw new AnnotationException("Feature cannot have negative coordinates")
//                throw new AnnotationEditorServiceException("Feature cannot have negative coordinates");
            }

//            setOwner(transcript, (String) session.getAttribute("username"));
            String username = SecurityUtils?.subject?.principal
            setOwner(transcript, username);


            boolean useCDS = configWrapperService.useCDS()

            if (!useCDS || transcriptService.getCDS(transcript) == null) {
                calculateCDS(transcript);
            }

            addTranscriptToGene(gene, transcript);
            findNonCanonicalAcceptorDonorSpliceSites(editor, transcript);
            updateTranscriptAttributes(transcript);
        } else {
//            Collection<AbstractSingleLocationBioFeature> overlappingFeatures = editor.getSession().getOverlappingFeatures(JSONUtil.convertJSONToFeatureLocation(jsonTranscript.getJSONObject("location"), trackToSourceFeature.get(track)));
            Collection<Feature> overlappingFeatures = getOverlappingFeatures(JSONUtil.convertJSONToFeatureLocation(jsonTranscript.getJSONObject("location"), trackToSourceFeature.get(track)));
            for (AbstractSingleLocationBioFeature feature : overlappingFeatures) {
                if (feature instanceof Gene && !((Gene) feature).isPseudogene() && overlapper != null) {
                    Gene tmpGene = (Gene) feature;
                    Feature gsolTranscript = JSONUtil.convertJSONToFeature(jsonTranscript, bioObjectConfiguration, trackToSourceFeature.get(track), nameAdapter);
                    updateNewGsolFeatureAttributes(gsolTranscript, trackToSourceFeature.get(track));
                    Transcript tmpTranscript = (Transcript) BioObjectUtil.createBioObject(gsolTranscript, bioObjectConfiguration);
                    if (tmpTranscript.getFmin() < 0 || tmpTranscript.getFmax() < 0) {
                        throw new AnnotationException("Feature cannot have negative coordinates");
                    }
                    setOwner(tmpTranscript, (String) session.getAttribute("username"));
                    if (!useCDS || tmpTranscript.getCDS() == null) {
                        calculateCDS(editor, tmpTranscript);
                    }
                    updateTranscriptAttributes(tmpTranscript);
                    if (overlapper.overlaps(tmpTranscript, tmpGene)) {
                        transcript = tmpTranscript;
                        gene = tmpGene;
                        editor.addTranscript(gene, transcript);
                        findNonCanonicalAcceptorDonorSpliceSites(editor, transcript);
                        break;
                    } else {
//                        editor.getSession().endTransactionForFeature(feature);
                    }
                } else {
//                    editor.getSession().endTransactionForFeature(feature);
                }
            }
        }
        if (gene == null) {
            JSONObject jsonGene = new JSONObject();
            jsonGene.put("children", new JSONArray().put(jsonTranscript));
            jsonGene.put("location", jsonTranscript.getJSONObject("location"));
            jsonGene.put("type", JSONUtil.convertCVTermToJSON(bioObjectConfiguration.getDefaultCVTermForClass(isPseudogene ? "Pseudogene" : "Gene")));
            Feature gsolGene = JSONUtil.convertJSONToFeature(jsonGene, bioObjectConfiguration, trackToSourceFeature.get(track), nameAdapter);
            updateNewGsolFeatureAttributes(gsolGene, trackToSourceFeature.get(track));
            gene = (Gene) BioObjectUtil.createBioObject(gsolGene, bioObjectConfiguration);
            if (gene.getFmin() < 0 || gene.getFmax() < 0) {
//                throw new AnnotationEditorServiceException("Feature cannot have negative coordinates");
            }
            setOwner(gene, (String) session.getAttribute("username"));
            transcript = gene.getTranscripts().iterator().next();
            if (!useCDS || transcript.getCDS() == null) {
                calculateCDS(editor, transcript);
            }
            editor.addFeature(gene);
            updateTranscriptAttributes(transcript);
            findNonCanonicalAcceptorDonorSpliceSites(editor, transcript);
        }
        return transcript;

    }

    def addTranscriptToGene(Gene gene, Transcript transcript) {
        removeExonOverlapsAndAdjacencies(transcript);
//        gene.addTranscript(transcript);
        CVTerm partOfCvterm = cvTermService.partOf

        // no feature location, set location to transcript's
        if (gene.getSingleFeatureLocation() == null) {
            FeatureLocation transcriptFeatureLocation = transcript.getSingleFeatureLocation()
            FeatureLocation featureLocation = new FeatureLocation()
            featureLocation.properties = transcriptFeatureLocation.properties
            featureLocation.id = null
            featureLocation.save()
            gene.setFeatureLocation( featureLocation );
        }
        else {
            // if the transcript's bounds are beyond the gene's bounds, need to adjust the gene's bounds
            if (transcript.getSingleFeatureLocation().getFmin() < gene.getSingleFeatureLocation().getFmin()) {
                gene.getSingleFeatureLocation().setFmin(transcript.getSingleFeatureLocation().getFmin());
            }
            if (transcript.getSingleFeatureLocation().getFmax() > gene.getSingleFeatureLocation().getFmax()) {
                gene.getSingleFeatureLocation().setFmax(transcript.getSingleFeatureLocation().getFmax());
            }
        }

        // add transcript
        int rank = 0;
        //TODO: do we need to figure out the rank?
        FeatureRelationship featureRelationship = new FeatureRelationship(
                type: partOfCvterm.iterator().next()
                ,objectFeature: gene
                ,subjectFeature: transcript
                ,rank: rank
        ).save()
        gene.childFeatureRelationships.add(featureRelationship)


        updateGeneBoundaries(gene);

//        getSession().indexFeature(transcript);

        // event fire
//        TODO: determine event model?
//        fireAnnotationChangeEvent(transcript, gene, AnnotationChangeEvent.Operation.ADD);
    }

    Object updateGeneBoundaries(Gene gene) {
        null
    }

    def removeExonOverlapsAndAdjacencies(Transcript transcript) {
        if (transcriptService.getExons(transcript).size() <= 1) {
            return;
        }
        List<Exon> exons = BioObjectUtil.createSortedFeatureListByLocation(transcriptService.getExons(transcript), false);
        int inc = 1;
        for (int i = 0; i < exons.size() - 1; i += inc) {
            inc = 1;
            Exon leftExon = exons.get(i);
            for (int j = i + 1; j < exons.size(); ++j) {
                Exon rightExon = exons.get(j);
                overlaps(leftExon,rightExon)
                if (overlaps(leftExon,rightExon) || isAdjacentTo(leftExon.getFeatureLocation(),rightExon.getFeatureLocation())) {
                    try {
                        mergeExons(leftExon, rightExon);
                    } catch (AnnotationException e) {
                        log.error(e)
                    }
                    ++inc;
                }
            }
        }
    }

    /** Checks whether this AbstractSimpleLocationBioFeature is adjacent to the FeatureLocation.
     *
     * @param location - FeatureLocation to check adjacency against
     * @return true if there is adjacency
     */
    public boolean isAdjacentTo(FeatureLocation leftLocation,FeatureLocation location) {
        return isAdjacentTo(leftLocation,location, true);
    }

    public boolean isAdjacentTo(FeatureLocation leftFeatureLocation,FeatureLocation rightFeatureLocation, boolean compareStrands) {
        if (leftFeatureLocation.getSourceFeature() != rightFeatureLocation.getSourceFeature() &&
                !leftFeatureLocation.getSourceFeature().equals(rightFeatureLocation.getSourceFeature())) {
            return false;
        }
        int thisFmin = leftFeatureLocation.getFmin();
        int thisFmax = leftFeatureLocation.getFmax();
        int thisStrand = leftFeatureLocation.getStrand();
        int otherFmin = rightFeatureLocation.getFmin();
        int otherFmax = rightFeatureLocation.getFmax();
        int otherStrand = rightFeatureLocation.getStrand();
        boolean strandsOverlap = compareStrands ? thisStrand == otherStrand : true;
        if (strandsOverlap &&
                (thisFmax == otherFmin ||
                        thisFmin == otherFmax)) {
            return true;
        }
        return false;
    }

    def overlaps(Exon leftExon, Exon rightExon) {
        return overlaps(leftExon.featureLocation,rightExon.featureLocation)
    }

    def overlaps(FeatureLocation leftFeatureLocation, FeatureLocation rightFeatureLocation,boolean compareStrands = true) {
        if (leftFeatureLocation.getSourceFeature() != rightFeatureLocation.getSourceFeature() &&
                !leftFeatureLocation.getSourceFeature().equals(rightFeatureLocation.getSourceFeature())) {
            return false;
        }
        int thisFmin = leftFeatureLocation.getFmin();
        int thisFmax = leftFeatureLocation.getFmax();
        int thisStrand = leftFeatureLocation.getStrand();
        int otherFmin = rightFeatureLocation.getFmin();
        int otherFmax = rightFeatureLocation.getFmax();
        int otherStrand = rightFeatureLocation.getStrand();
        boolean strandsOverlap = compareStrands ? thisStrand == otherStrand : true;
        if (strandsOverlap &&
                (thisFmin <= otherFmin && thisFmax > otherFmin ||
                        thisFmin >= otherFmin && thisFmin < otherFmax)) {
            return true;
        }
        return false;
    }


    def calculateCDS(Transcript transcript) {
        // NOTE: isPseudogene call seemed redundant with isProtenCoding
        if (transcriptService.isProteinCoding(transcript) && (transcriptService.getGene(transcript) == null)) {
//            calculateCDS(editor, transcript, transcript.getCDS() != null ? transcript.getCDS().getStopCodonReadThrough() != null : false);
//            calculateCDS(transcript, transcript.getCDS() != null ? transcript.getCDS().getStopCodonReadThrough() != null : false);
            calculateCDS(transcript, transcriptService.getCDS(transcript) != null ? transcriptService.getStopCodonReadThrough(transcript) != null : false);
        }
    }

    def calculateCDS(Transcript transcript,boolean readThroughStopCodon) {
        CDS cds = transcriptService.getCDS(transcript);
        if (cds == null) {
            setLongestORF(transcript, readThroughStopCodon);
            return;
        }
        boolean manuallySetStart = isManuallySetTranslationStart(cds);
        boolean manuallySetEnd = isManuallySetTranslationEnd(cds);
        if (manuallySetStart && manuallySetEnd) {
            return;
        }
        if (!manuallySetStart && !manuallySetEnd) {
            setLongestORF(transcript, readThroughStopCodon);
        } else if (manuallySetStart) {
            setTranslationStart(transcript, cds.getFeatureLocation().getStrand().equals(-1) ? cds.getFmax() - 1 : cds.getFmin(), true, readThroughStopCodon);
        } else {
            setTranslationEnd(transcript, cds.getFeatureLocation().getStrand().equals(-1) ? cds.getFmin() : cds.getFmax() - 1, true);
        }
    }


    Transcript convertJSONToTranscript(JSONObject jsonObject, FeatureLazyResidues featureLazyResidues) {

    }

    public Feature convertJSONToFeature(JSONObject jsonFeature, Organism organism, Feature sourceFeature) {
        Feature gsolFeature = new Feature();
        try {
            gsolFeature.setOrganism(organism);
            JSONObject type = jsonFeature.getJSONObject("type");
            gsolFeature.setType(convertJSONToCVTerm(type));
            if (jsonFeature.has("uniquename")) {
                gsolFeature.setUniqueName(jsonFeature.getString("uniquename"));
            }
            if (jsonFeature.has("name")) {
                gsolFeature.setName(jsonFeature.getString("name"));
                gsolFeature.setUniqueName(nameService.generateUniqueName(gsolFeature));
            }
            else{
                gsolFeature.setUniqueName(nameService.generateUniqueName());
            }
            if (jsonFeature.has("residues")) {
                gsolFeature.setResidues(jsonFeature.getString("residues"));
            }
            if (jsonFeature.has("location")) {
                JSONObject jsonLocation = jsonFeature.getJSONObject("location");
                gsolFeature.addToFeatureLocations(convertJSONToFeatureLocation(jsonLocation, sourceFeature));
            }
            if (jsonFeature.has("children")) {
                JSONArray children = jsonFeature.getJSONArray("children");
                CVTerm partOfCvTerm = CVTerm.findByName("PartOf")
                for (int i = 0; i < children.length(); ++i) {
                    Feature child = convertJSONToFeature(children.getJSONObject(i),  sourceFeature != null ? sourceFeature.getOrganism() : null, sourceFeature);
                    FeatureRelationship fr = new FeatureRelationship();
                    fr.setObjectFeature(gsolFeature);
                    fr.setSubjectFeature(child);
//                    fr.setType(configuration.getDefaultCVTermForClass("PartOf"));
                    fr.setType(partOfCvTerm);
                    child.getParentFeatureRelationships().add(fr);
                    gsolFeature.getChildFeatureRelationships().add(fr);
                }
            }
            if (jsonFeature.has("timeaccessioned")) {
                gsolFeature.setTimeAccessioned(new Date(jsonFeature.getInt("timeaccessioned")));
            } else {
                gsolFeature.setTimeAccessioned(new Date());
            }
            if (jsonFeature.has("timelastmodified")) {
                gsolFeature.setTimeLastModified(new Date(jsonFeature.getInt("timelastmodified")));
            } else {
                gsolFeature.setTimeLastModified(new Date());
            }
            if (jsonFeature.has("properties")) {
                JSONArray properties = jsonFeature.getJSONArray("properties");
                for (int i = 0; i < properties.length(); ++i) {
                    JSONObject property = properties.getJSONObject(i);
                    JSONObject propertyType = property.getJSONObject("type");
                    FeatureProperty gsolProperty = new FeatureProperty();
                    gsolProperty.setType(new CVTerm(propertyType.getString("name"), new CV(propertyType.getJSONObject("cv").getString("name"))));
                    gsolProperty.setValue(property.getString("value"));
                    gsolProperty.setFeature(gsolFeature);
                    int rank = 0;
                    for (FeatureProperty fp : gsolFeature.getFeatureProperties()) {
                        if (fp.getType().equals(gsolProperty.getType())) {
                            if (fp.getRank() > rank) {
                                rank = fp.getRank();
                            }
                        }
                    }
                    gsolProperty.setRank(rank + 1);
                    gsolFeature.getFeatureProperties().add(gsolProperty);
                }
            }
            if (jsonFeature.has("dbxrefs")) {
                JSONArray dbxrefs = jsonFeature.getJSONArray("dbxrefs");
                for (int i = 0; i < dbxrefs.length(); ++i) {
                    JSONObject dbxref = dbxrefs.getJSONObject(i);
                    JSONObject db = dbxref.getJSONObject("db");
                    gsolFeature.addFeatureDBXref(new DB(db.getString("name")), dbxref.getString("accession"));
                }
            }
        }
        catch (JSONException e) {
            return null;
        }
        return gsolFeature;
    }

//    public FeatureProperty convertJSONToFeatureProperty(JSONObject jsonFeatureProperty) {
//        FeatureProperty gsolFeatureProperty = new FeatureProperty();
//        try {
//            gsolFeatureProperty.setType(convertJSONToCVTerm(jsonFeatureProperty.getJSONObject("type")));
//            gsolFeatureProperty.setValue(jsonFeatureProperty.getString("value"));
//        }
//        catch (JSONException e) {
//            return null;
//        }
//        return gsolFeatureProperty;
//    }

    public FeatureLocation convertJSONToFeatureLocation(JSONObject jsonLocation, Feature sourceFeature) throws JSONException {
        FeatureLocation gsolLocation = new FeatureLocation();
        gsolLocation.setFmin(jsonLocation.getInt("fmin"));
        gsolLocation.setFmax(jsonLocation.getInt("fmax"));
        gsolLocation.setStrand(jsonLocation.getInt("strand"));
        gsolLocation.setSourceFeature(sourceFeature);
        return gsolLocation;
    }

//    public CVTerm convertJSONToCVTerm(JSONObject jsonCVTerm) throws JSONException {
//        return new CVTerm(jsonCVTerm.getString("name"), new CV(jsonCVTerm.getJSONObject("cv").getString("name")));
//    }

    private void updateGeneBoundaries(Gene gene) {
        if (gene == null) {
            return;
        }
        int geneFmax = Integer.MIN_VALUE;
        int geneFmin = Integer.MAX_VALUE;
        for (Transcript t : transcriptService.getTranscripts(gene)) {
            if (t.getFmin() < geneFmin) {
                geneFmin = t.getFmin();
            }
            if (t.getFmax() > geneFmax) {
                geneFmax = t.getFmax();
            }
        }
        gene.featureLocation.setFmin(geneFmin);
        gene.featureLocation.setFmax(geneFmax);
        gene.setTimeLastModified(new Date());
    }
}
