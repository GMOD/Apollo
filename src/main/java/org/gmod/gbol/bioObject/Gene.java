package org.gmod.gbol.bioObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.simpleObject.Organism;

/** Wrapper class representing a gene.
 * 
 * @author elee
 *
 */

public class Gene extends AbstractSingleLocationBioFeature {

    private static final long serialVersionUID = 1L;

    /** Constructor.
     * 
     * @param feature - Feature object that this class wraps
     * @param conf - Configuration containing mapping information
     */
    public Gene(Feature feature, BioObjectConfiguration conf) {
        super(feature, conf);
    }

    /** Alternate constructor to create a Gene object without having to pre-create the underlying
     *  Feature object.  The constructor will take care of creating the underlying Feature object.
     * 
     * @param organism - Organism that this Gene belongs to
     * @param uniqueName - String representing the unique name for this Gene
     * @param analysis - boolean flag for whether this feature is a result of an analysis
     * @param obsolete - boolean flag for whether this feature is obsolete
     * @param dateAccessioned - Timestamp for when this feature was first accessioned
     * @param conf - Configuration containing mapping information
     */
    public Gene(Organism organism, String uniqueName, boolean analysis,
            boolean obsolete, Timestamp dateAccessioned, BioObjectConfiguration conf) {
        super(new Feature(
                conf.getDefaultCVTermForClass("Gene"),
                null,
                organism,
                null,
                uniqueName,
                null,
                null,
                null,
                analysis,
                obsolete,
                dateAccessioned,
                null),
                conf);
    }

    /** Copy constructor.
     * 
     * @param gene - Gene to create the copy from
     * @param uniqueName - String representing the unique name for this Gene
     */
    public Gene(Gene gene, String uniqueName) {
        this(gene.getFeature().getOrganism(), uniqueName, gene.getFeature().isIsAnalysis(),
                gene.getFeature().isIsObsolete(),
                new Timestamp(gene.getFeature().getTimeAccessioned().getTime()), gene.getConfiguration());
        feature.addFeatureLocation(new FeatureLocation(gene.getFeatureLocation()));
    }
    
    /** Retrieve all the transcripts associated with this gene.  Uses the configuration to determine
     *  which children are transcripts.  Transcript objects are generated on the fly.  The collection
     *  will be empty if there are no transcripts associated with the gene.
     *  
     * @return Collection of transcripts associated with this gene
     */
    public Collection<Transcript> getTranscripts() {
        Collection<Transcript> transcripts = new ArrayList<Transcript>();
        Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
        Collection<CVTerm> transcriptCvterms = conf.getDescendantCVTermsForClass("Transcript");
        
        for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
            if (!partOfCvterms.contains(fr.getType())) {
                continue;
            }
            if (!transcriptCvterms.contains(fr.getSubjectFeature().getType())) {
                continue;
            }
            transcripts.add((Transcript)BioObjectUtil.createBioObject(fr.getSubjectFeature(), conf));
        }
        return transcripts;
    }
    
    /** Add a transcript.  If the transcript's bounds are beyond the gene's bounds, the gene's bounds
     *  are adjusted accordingly.  Sets the transcript's gene to this gene object.
     * 
     * @param transcript - Transcript to be added
     */
    public void addTranscript(Transcript transcript) {
        CVTerm partOfCvterm = conf.getDefaultCVTermForClass("PartOf");

        // no feature location, set location to transcript's
        if (getFeatureLocation() == null) {
            setFeatureLocation(new FeatureLocation(transcript.getFeatureLocation()));
        }
        else {
            // if the transcript's bounds are beyond the gene's bounds, need to adjust the gene's bounds
            if (transcript.getFeatureLocation().getFmin() < getFeatureLocation().getFmin()) {
                getFeatureLocation().setFmin(transcript.getFeatureLocation().getFmin());
            }
            if (transcript.getFeatureLocation().getFmax() > getFeatureLocation().getFmax()) {
                getFeatureLocation().setFmax(transcript.getFeatureLocation().getFmax());
            }
        }

        // add transcript
        int rank = 0;
        //TODO: do we need to figure out the rank?
        feature.getChildFeatureRelationships().add(
                new FeatureRelationship(partOfCvterm, feature, transcript.getFeature(), rank));
        transcript.setGene(this);
    }

    /** Delete a transcript.  Deletes both the gene -> transcript and transcript -> gene
     *  relationships.
     *  
     * @param transcript - Transcript to be deleted
     */
    public void deleteTranscript(Transcript transcript) {
        Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
        Collection<CVTerm> transcriptCvterms = conf.getDescendantCVTermsForClass("Transcript");
        Collection<CVTerm> geneCvterms = conf.getCVTermsForClass(getClassName());

        // delete gene -> transcript child relationship
        for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
            if (!partOfCvterms.contains(fr.getType())) {
                continue;
            }
            if (!transcriptCvterms.contains(fr.getSubjectFeature().getType())) {
                continue;
            }
            if (fr.getSubjectFeature().equals(transcript.getFeature())) {
                feature.getChildFeatureRelationships().remove(fr);
                break;
            }
        }
        
        // delete gene -> transcript parent relationship
        for (FeatureRelationship fr : transcript.getFeature().getParentFeatureRelationships()) {
            if (!partOfCvterms.contains(fr.getType())) {
                continue;
            }
            if (!geneCvterms.contains(fr.getObjectFeature().getType())) {
                continue;
            }
            if (fr.getSubjectFeature().equals(transcript.getFeature())) {
                transcript.getFeature().getParentFeatureRelationships().remove(fr);
                break;
            }
        }
        
        // update bounds
        Integer fmin = null;
        Integer fmax = null;
        for (Transcript t : getTranscripts()) {
            if (fmin == null || t.getFmin() < fmin) {
                fmin = t.getFmin();
            }
            if (fmax == null || t.getFmax() > fmax) {
                fmax = t.getFmax();
            }
        }
        if (fmin != null) {
            setFmin(fmin);
        }
        if (fmax != null) {
            setFmax(fmax);
        }
        
    }
    
    /** Get the number of transcripts.
     * 
     * @return Number of transcripts
     */
    public int getNumberOfTranscripts() {
        Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
        Collection<CVTerm> transcriptCvterms = conf.getCVTermsForClass("Transcript");
        int numTranscripts = 0;

        for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
            if (!partOfCvterms.contains(fr.getType())) {
                continue;
            }
            if (!transcriptCvterms.contains(fr.getSubjectFeature().getType())) {
                continue;
            }
            ++numTranscripts;
        }
        return numTranscripts;
    }
    
    public boolean isPseudogene() {
        return false;
    }
    
    public String getClassName() {
        return BioObjectUtil.stripPackageNameFromClassName(getClass().getName());
    }
}
