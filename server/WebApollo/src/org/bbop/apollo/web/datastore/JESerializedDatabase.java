package org.bbop.apollo.web.datastore;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;

public class JESerializedDatabase<T> {

	private Database database;
	private EntryBinding<T> dataBinding;
	
	public JESerializedDatabase(Environment environment, String databaseName, Class<T> clazz) {
		this(environment, databaseName, clazz, false);
	}
	
	public JESerializedDatabase(Environment environment, String databaseName, Class<T> clazz, boolean readOnly) {
		DatabaseConfig databaseConfig = new DatabaseConfig();
		databaseConfig.setAllowCreate(true);
		databaseConfig.setReadOnly(readOnly);
		database = environment.openDatabase(null, databaseName, databaseConfig);

		StoredClassCatalog classCatalog = new StoredClassCatalog(database);
		dataBinding = new SerialBinding<T>(classCatalog, clazz);
	}

	public DatabaseEntry objectToEntry(T data) {
		DatabaseEntry entry = new DatabaseEntry();
		dataBinding.objectToEntry(data, entry);
		return entry;
	}
	
	public T entryToObject(DatabaseEntry entry) {
		T retVal;
		retVal = dataBinding.entryToObject(entry);
		return retVal;
	}
	
	public void close() {
		database.close();
	}
}
