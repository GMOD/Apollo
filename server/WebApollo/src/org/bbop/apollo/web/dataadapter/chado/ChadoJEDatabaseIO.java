package org.bbop.apollo.web.dataadapter.chado;

import java.util.ArrayList;
import java.util.Collection;

import org.bbop.apollo.web.datastore.JEDatabase;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.simpleObject.io.SimpleObjectIOException;

public class ChadoJEDatabaseIO extends ChadoIO {

	private JEDatabase jeDatabase;
	
	public ChadoJEDatabaseIO(String databaseDir, String hibernateConfig) throws Exception {
		this(databaseDir, hibernateConfig, true);
	}
	
	public ChadoJEDatabaseIO(String databaseDir, String hibernateConfig, boolean readOnly) throws Exception {
		super(hibernateConfig);
		jeDatabase = new JEDatabase(databaseDir, readOnly);
	}

	public void writeFeatures(Feature sourceFeature) throws SimpleObjectIOException {
		Collection<Feature> features = new ArrayList<Feature>();
		jeDatabase.readFeatures(features);
		jeDatabase.readSequenceAlterations(features);
		for (Feature feature : features) {
			setName(feature);
		}
		writeFeatures(features, sourceFeature);
	}
	
	@Override
	public void close() {
		super.close();
		jeDatabase.close();
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
