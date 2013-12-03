package org.bbop.apollo.web.datastore;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Iterator;

import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.SimpleObjectIteratorInterface;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

public class JEDatabase extends AbstractDataStore {

	private Environment dbEnvironment;
	private Database featuresDb;
	private Database sequenceAlterationsDb;
	private JESerializedDatabase<Feature> featuresSerializedDb;
	
	public JEDatabase(String storageDirectory) {
		this(storageDirectory, false);
	}
	
	public JEDatabase(String storageDirectory, boolean readOnly) {
		EnvironmentConfig environmentConfig = new EnvironmentConfig();
		environmentConfig.setAllowCreate(true);
		environmentConfig.setReadOnly(readOnly);
		environmentConfig.setTransactional(true);
		File dir = new File(storageDirectory);
		if (!readOnly && !dir.exists()) {
			dir.mkdirs();
		}
		dbEnvironment = new Environment(dir, environmentConfig);
		
		DatabaseConfig databaseConfig = new DatabaseConfig();
		databaseConfig.setAllowCreate(!readOnly);
		databaseConfig.setTransactional(true);
		databaseConfig.setReadOnly(readOnly);
		featuresDb = dbEnvironment.openDatabase(null, "features", databaseConfig);
		sequenceAlterationsDb = dbEnvironment.openDatabase(null, "sequence_alterations", databaseConfig);
	
		featuresSerializedDb = new JESerializedDatabase<Feature>(dbEnvironment,
				"features_class", Feature.class, readOnly);
	}

	public void writeFeature(Feature feature) {
		writeEntry(featuresDb, feature, generateUniqueName(feature, false));
	}
	
	public void writeFeature(AbstractSingleLocationBioFeature feature) {
		SimpleObjectIteratorInterface iterator = feature.getWriteableSimpleObjects(feature.getConfiguration());
		writeFeature((Feature)iterator.next());
	}
	
	public void writeSequenceAlteration(Feature sequenceAlteration) {
		writeEntry(sequenceAlterationsDb, sequenceAlteration, generateUniqueName(sequenceAlteration, true));
	}
	
	public void readFeatures(Collection<Feature> features) {
		readValues(featuresDb, features);
	}

	public void readSequenceAlterations(Collection<Feature> sequenceAlterations) {
		readValues(sequenceAlterationsDb, sequenceAlterations);
	}
	
	public void deleteFeature(Feature feature) {
		deleteEntry(featuresDb, feature, generateUniqueName(feature, false));
	}

	public void deleteFeature(AbstractSingleLocationBioFeature feature) {
		SimpleObjectIteratorInterface iterator = feature.getWriteableSimpleObjects(feature.getConfiguration());
		deleteFeature((Feature)iterator.next());
	}
	
	public void deleteSequenceAlteration(Feature sequenceAlteration) {
		deleteEntry(sequenceAlterationsDb, sequenceAlteration, generateUniqueName(sequenceAlteration, true));
	}

	public void close() {
		featuresDb.close();
		featuresSerializedDb.close();
		sequenceAlterationsDb.close();
		dbEnvironment.close();
	}
	
	public Iterator<Feature> getFeatureIterator() {
		return getIterator(featuresDb);
	}
	
	public Iterator<Feature> getSequenceAlterationIterator() {
		return getIterator(sequenceAlterationsDb);
	}
	
	public Feature getFeatureByUniqueName(String uniqueName) {
		return getEntry(featuresDb, uniqueName);
	}
	
	public Feature getSequenceAlterationByUniqueName(String uniqueName) {
		return getEntry(sequenceAlterationsDb, uniqueName);
	}
	
	private Iterator<Feature> getIterator(Database db) {
		return new JEDatabaseIterator<Feature>(db, featuresSerializedDb);
	}
	
	private void readValues(Database db, Collection<Feature> data) {
		Cursor cursor = db.openCursor(null, null);
		DatabaseEntry key = new DatabaseEntry();
		DatabaseEntry value = new DatabaseEntry();
		try {
			while (cursor.getNext(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
				Feature feature = featuresSerializedDb.entryToObject(value);
				data.add(feature);
			}
		}
		catch (DatabaseException e) {
		}
		finally {
			cursor.close();
		}

	}
	
	private String generateUniqueName(Feature feature, boolean isSequenceAlteration) {
		/*
		if (isSequenceAlteration) {
			FeatureLocation loc = feature.getFeatureLocations().iterator().next();
			return String.format("%d-%d-%d", loc.getFmin(), loc.getFmax(), loc.getStrand());
		}
		else {
		*/
			return feature.getUniqueName();
//		}
	}
	
	private void writeEntry(Database db, Feature feature, String uniqueName) {
		DatabaseEntry key = null;
		try {
			key = new DatabaseEntry(uniqueName.getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException e) {
		}
		DatabaseEntry value = featuresSerializedDb.objectToEntry(feature);
		db.put(null, key, value);
	}
	
	private void deleteEntry(Database db, Feature feature, String uniqueName) {
		DatabaseEntry key = null;
		try {
			key = new DatabaseEntry(uniqueName.getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException e) {
		}
		db.delete(null, key);
	}
	
	private Feature getEntry(Database db, String uniqueName) {
		DatabaseEntry key = null;
		DatabaseEntry value = new DatabaseEntry();
		try {
			key = new DatabaseEntry(uniqueName.getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException e) {
		}
		if (db.get(null, key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
			return featuresSerializedDb.entryToObject(value);
		}
		return null;
	}
	
}
