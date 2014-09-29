package org.bbop.apollo.editor;

import java.util.Collection;

import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.SequenceAlteration;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.Organism;

public interface AbstractAnnotationSession {

    /** Get the source feature associated with this session.
     * 
     * @return Source feature associated with this session
     */
    public Feature getSourceFeature();

    /** Set the source feature associated with this session.
     * 
     * @param sourceFeature - Source feature to be associated with this session
     */
    public void setSourceFeature(Feature sourceFeature);

    /** Get the organism associated with this session.
     * 
     * @return Organism associated with this session
     */
    public Organism getOrganism();

    /** Set the organism to be associated with this session.
     * 
     * @param organism - Organism to be associated with this session
     */
    public void setOrganism(Organism organism);

    /** Add a feature to the session.  Features are always stored in sorted order by position.
     * 
     * @param feature - AbstractSingleLocationBioFeature to be added to the session
     */
    public void addFeature(AbstractSingleLocationBioFeature feature);

    /** Delete a feature from the session.
     * 
     * @param feature - AbstractSingleLocationBioFeature to be deleted from the session
     */
    public void deleteFeature(AbstractSingleLocationBioFeature feature);

    /** Get features.
     * 
     * @return Collection of AbstractSingleLocationBioFeature objects
     */
    public Collection<AbstractSingleLocationBioFeature> getFeatures();

    /** Get a feature by unique name.  Returns null if there are no features with the unique name.
     * 
     * @param uniqueName - Unique name to look up the feature by
     * @return AbstractSingleLocationBioFeature with the unique name
     */
    public AbstractSingleLocationBioFeature getFeatureByUniqueName(
            String uniqueName);

    /** Get features that overlap a given location.  Compares strand as well as coordinates.
     * 
     * @param location - FeatureLocation that the features overlap
     * @return Collection of AbstractSingleLocationBioFeature objects that overlap the FeatureLocation
     */
    public Collection<AbstractSingleLocationBioFeature> getOverlappingFeatures(
            FeatureLocation location);

    /** Get features that overlap a given location.
     * 
     * @param location - FeatureLocation that the features overlap
     * @param compareStrands - Whether to compare strands in overlap
     * @return Collection of AbstractSingleLocationBioFeature objects that overlap the FeatureLocation
     */
    public Collection<AbstractSingleLocationBioFeature> getOverlappingFeatures(
            FeatureLocation location, boolean compareStrands);

    public int convertModifiedLocalCoordinateToSourceCoordinate(
            AbstractSingleLocationBioFeature feature, int localCoordinate);

    /** Add a sequence alteration.  It uses the source feature of the alteration, which should be the source
     *  feature of the annotations in this session, for retrieving the modified residues for an annotation.
     *  
     * @param sequenceAlteration - SequenceAlteration to add to the session
     */
    public void addSequenceAlteration(SequenceAlteration sequenceAlteration);

    /** Delete a sequence alteration.
     *  
     * @param sequenceAlteration - SequenceAlteration to delete from the session
     */
    public void deleteSequenceAlteration(SequenceAlteration sequenceAlteration);

    /** Get the sequence alterations stored in the session.
     * 
     * @return Collection of sequence alterations in this session
     */
    public Collection<SequenceAlteration> getSequenceAlterations();

    /** Get the residues for a feature with any frameshifts.  If the feature is not an instance of
     *  CDS, it will just return the raw feature.
     *  
     * @param feature - AbstractSingleLocationBioFeature to retrieve the annotation with any frameshifts
     * @return Residues of the feature with frameshifts
     */
    public String getResiduesWithFrameshifts(
            AbstractSingleLocationBioFeature feature);

    /** Get the residues for a feature with any alterations and frameshifts.
     * 
     * @param feature - AbstractSingleLocationBioFeature to retrieve the residues for
     * @return Residues for the feature with any alterations and frameshifts
     */
    public String getResiduesWithAlterationsAndFrameshifts(
            AbstractSingleLocationBioFeature feature);

    /** Get the residues for a feature with any alterations.
     * 
     * @param feature - AbstractSingleLocationBioFeature to retrieve the residues for
     * @return Residues for the feature with any alterations
     */
    public String getResiduesWithAlterations(
            AbstractSingleLocationBioFeature feature);

}
