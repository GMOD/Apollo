package org.bbop.apollo.web.datastore;

import java.util.Iterator;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

public class JEDatabaseIterator<T> implements Iterator<T> {
	
	private T next;
	private Cursor cursor;
	private Transaction transaction;
	private DatabaseEntry key;
	private DatabaseEntry value;
	private boolean fetched;
	private JESerializedDatabase<T> serializedDb;

	public JEDatabaseIterator(Database db, JESerializedDatabase<T> serializedDb) {
		transaction = db.getEnvironment().beginTransaction(null, null);
		cursor = db.openCursor(transaction, null);
		key = new DatabaseEntry();
		value = new DatabaseEntry();
		this.serializedDb = serializedDb;
	}

	@Override
	public boolean hasNext() {
		if (!fetched && next != null) {
			return true;
		}
		if (cursor.getNext(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
			next = serializedDb.entryToObject(value);
		}
		else {
			next = null;
			cursor.close();
			transaction.commit();
		}
		return next != null;
	}

	@Override
	public T next() {
		fetched = true;
		return next;
	}

	@Override
	public void remove() {
		cursor.delete();
	}

}
