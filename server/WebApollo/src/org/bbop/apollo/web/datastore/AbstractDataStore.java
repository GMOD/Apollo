package org.bbop.apollo.web.datastore;

import java.util.Collection;
import java.util.Iterator;

import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.simpleObject.Feature;

public abstract class AbstractDataStore {

	public abstract void writeFeature(Feature feature);
	
	public abstract void writeFeature(AbstractSingleLocationBioFeature feature);
	
	public abstract void writeSequenceAlteration(Feature sequenceAlteration);
	
	public abstract void readFeatures(Collection<Feature> features);

	public abstract void readSequenceAlterations(Collection<Feature> sequenceAlterations);

	public abstract void deleteFeature(Feature feature);

	public abstract void deleteFeature(AbstractSingleLocationBioFeature feature);

	public abstract void deleteSequenceAlteration(Feature sequenceAlteration);

	public abstract void close();
	
	public abstract Iterator<Feature> getFeatureIterator();

	public abstract Iterator<Feature> getSequenceAlterationIterator();
	
	public abstract Feature getFeatureByUniqueName(String uniqueName);
	
	public abstract Feature getSequenceAlterationByUniqueName(String uniqueName);

}