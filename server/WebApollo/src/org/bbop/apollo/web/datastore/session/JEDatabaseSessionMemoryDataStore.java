package org.bbop.apollo.web.datastore.session;

import java.util.Iterator;

import org.bbop.apollo.editor.session.MemoryDataStore;
import org.bbop.apollo.web.datastore.JEDatabase;
import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.SequenceAlteration;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureRelationship;

public class JEDatabaseSessionMemoryDataStore extends MemoryDataStore {
	
	public JEDatabaseSessionMemoryDataStore(JEDatabase dataStore, Feature sourceFeature, BioObjectConfiguration conf) {
		Iterator<Feature> iter = dataStore.getFeatureIterator();
		while (iter.hasNext()) {
			Feature feature = iter.next();
			addSourceToFeature(feature, sourceFeature);
			addFeature((AbstractSingleLocationBioFeature)BioObjectUtil.createBioObject(feature, conf));
		}
		iter = dataStore.getSequenceAlterationIterator();
		while (iter.hasNext()) {
			Feature sequenceAlteration = iter.next();
			addSourceToFeature(sequenceAlteration, sourceFeature);
			addSequenceAlteration((SequenceAlteration)BioObjectUtil.createBioObject(sequenceAlteration, conf));
		}

	}
	
	private void addSourceToFeature(Feature feature, Feature sourceFeature) {
		if (feature.getFeatureLocations().size() > 0) {
			feature.getFeatureLocations().iterator().next().setSourceFeature(sourceFeature);
		}
		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			addSourceToFeature(fr.getSubjectFeature(), sourceFeature);
		}
	}
}
