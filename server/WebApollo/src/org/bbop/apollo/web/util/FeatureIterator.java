package org.bbop.apollo.web.util;

import java.util.Iterator;

import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureRelationship;

public class FeatureIterator implements Iterator<AbstractSingleLocationBioFeature> {
	
	private Iterator<Feature> iterator;
	private Feature sourceFeature;
	private BioObjectConfiguration conf;
	
	public FeatureIterator(Iterator<Feature> iterator, Feature sourceFeature, BioObjectConfiguration conf) {
		this.iterator = iterator;
		this.sourceFeature = sourceFeature;
		this.conf = conf;
	}
	
	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public AbstractSingleLocationBioFeature next() {
		Feature gsolFeature = iterator.next();
		setName(gsolFeature);
		AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)BioObjectUtil.createBioObject(gsolFeature, conf);
		addSourceFeature(feature, sourceFeature);
		return feature;
	}

	@Override
	public void remove() {
	}
	
	private void addSourceFeature(AbstractSingleLocationBioFeature feature, Feature sourceFeature) {
		feature.getFeatureLocation().setSourceFeature(sourceFeature);
		for (AbstractSingleLocationBioFeature child : feature.getChildren()) {
			addSourceFeature(child, sourceFeature);
		}
	}
	
	private void setName(Feature feature) {
		if (feature.getName() == null) {
			feature.setName(feature.getUniqueName());
		}
		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			setName(fr.getSubjectFeature());
		}
	}

}
