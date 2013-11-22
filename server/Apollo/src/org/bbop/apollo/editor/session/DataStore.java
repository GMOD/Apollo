package org.bbop.apollo.editor.session;

import java.util.Collection;
import java.util.List;

import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.SequenceAlteration;
import org.gmod.gbol.simpleObject.FeatureLocation;

public interface DataStore {

	public List<AbstractSingleLocationBioFeature> getFeatures();
	
	public List<SequenceAlteration> getSequenceAlterations();
	
	public AbstractSingleLocationBioFeature getFeatureByUniqueName(String uniqueName);
	
	public void addSequenceAlteration(SequenceAlteration sequenceAlteration);
	
	public void deleteSequenceAlteration(SequenceAlteration sequenceAlteration);

	public void addFeature(AbstractSingleLocationBioFeature feature);
	
	public void deleteFeature(AbstractSingleLocationBioFeature feature);
	
	public Collection<AbstractSingleLocationBioFeature> getOverlappingFeatures(FeatureLocation location, boolean compareStrands);
	
	public void indexFeature(AbstractSingleLocationBioFeature feature);
	
	public void unindexFeature(AbstractSingleLocationBioFeature feature);
	
	public void beginTransactionForFeature(AbstractSingleLocationBioFeature feature);
	
	public void endTransactionForFeature(AbstractSingleLocationBioFeature feature);
	
	public void endTransactionForAllFeatures();
	
	public void addToStore(AbstractSingleLocationBioFeature feature);
	
	public void removeFromStore(AbstractSingleLocationBioFeature feature);

}
