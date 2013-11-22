package org.bbop.apollo.web.dataadapter.gff3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.bbop.apollo.web.datastore.JEDatabase;
import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.io.GFF3Handler;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.simpleObject.io.SimpleObjectIOException;

public class Gff3JEDatabaseIO extends Gff3IO {

	private JEDatabase jeDatabase;
	private BioObjectConfiguration conf;
	private String source;
	
	public Gff3JEDatabaseIO(String databaseDir, String path, String source, BioObjectConfiguration conf, Set<String> metaDataToExport) throws Exception {
		this(databaseDir, path, source, conf, true, GFF3Handler.Format.GZIP, metaDataToExport);
	}

	public Gff3JEDatabaseIO(String databaseDir, String path, String source, BioObjectConfiguration conf, boolean readOnly, Set<String> metaDataToExport) throws Exception {
		this(databaseDir, path, source, conf, readOnly, GFF3Handler.Format.GZIP, metaDataToExport);
	}
	
	public Gff3JEDatabaseIO(String databaseDir, String path, String source, BioObjectConfiguration conf, boolean readOnly, GFF3Handler.Format format, Set<String> metaDataToExport) throws Exception {
		super(path, source, format, metaDataToExport);
		this.conf = conf;
		setJeDatabase(databaseDir, readOnly);
	}
	
	public void setJeDatabase(String databaseDir, boolean readOnly) {
		if (jeDatabase != null) {
			jeDatabase.close();
		}
		jeDatabase = new JEDatabase(databaseDir, readOnly);
	}

	public Collection<? extends Feature> writeFeatures(Feature sourceFeature, String source) throws SimpleObjectIOException, IOException {
		FeatureIterator featureIterator = new FeatureIterator(jeDatabase.getFeatureIterator(), sourceFeature);
		writeFeatures(featureIterator, source, true);
		FeatureIterator sequenceAlterationIterator = new FeatureIterator(jeDatabase.getSequenceAlterationIterator(), sourceFeature);
		writeFeatures(sequenceAlterationIterator, source, false);

		Collection<Feature> sequenceAlterations = new ArrayList<Feature>();
		Iterator<Feature> iter = jeDatabase.getSequenceAlterationIterator();
		while (iter.hasNext()) {
			sequenceAlterations.add(iter.next());
		}
		return sequenceAlterations;
	}
	
	@Override
	public void close() {
		super.close();
		if (jeDatabase != null) {
			jeDatabase.close();
		}
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

	private class FeatureIterator implements Iterator<AbstractSingleLocationBioFeature> {
		
		private Iterator<Feature> iterator;
		private Feature sourceFeature;
		
		public FeatureIterator(Iterator<Feature> iterator, Feature sourceFeature) {
			this.iterator = iterator;
			this.sourceFeature = sourceFeature;
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
		
	}
	
}
