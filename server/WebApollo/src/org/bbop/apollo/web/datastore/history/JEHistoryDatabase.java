package org.bbop.apollo.web.datastore.history;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.bbop.apollo.web.datastore.JEDatabaseIterator;
import org.bbop.apollo.web.datastore.JESerializedDatabase;
import org.gmod.gbol.simpleObject.Feature;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

public class JEHistoryDatabase extends AbstractHistoryStore {

	private Environment dbEnvironment;
	private Database historyDb;
	private JESerializedDatabase<TransactionList> historySerializedDb;
	private int historySize;
	
	public JEHistoryDatabase(String storageDirectory) {
		this(storageDirectory, false, 0);
	}
	
	public JEHistoryDatabase(String storageDirectory, boolean readOnly, int historySize) {
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
		databaseConfig.setAllowCreate(true);
		databaseConfig.setTransactional(true);
		databaseConfig.setReadOnly(readOnly);
		historyDb = dbEnvironment.openDatabase(null, "history", databaseConfig);
		historySerializedDb = new JESerializedDatabase<TransactionList>(dbEnvironment,
				"history_class", TransactionList.class, readOnly);
		
		this.historySize = historySize;
	}
	
	@Override
	public void addTransaction(Transaction transaction) {
		TransactionList transactions = getTransactionListForFeature(transaction.getFeatureUniqueName());
		transactions.add(transaction);
		if (historySize > 0 && transactions.size() > historySize) {
			Transaction removedTransaction = transactions.remove(0);
			if (removedTransaction.getOperation() == Transaction.Operation.MERGE_TRANSCRIPTS) {
				deleteHistoryForFeature(removedTransaction.getOldFeatures().get(1).getUniqueName());
			}
			transactions.setToPreviousTransaction();
		}
		writeTransactionListForFeature(transaction.getFeatureUniqueName(), transactions);
	}
	
	@Override
	public Transaction getCurrentTransactionForFeature(String uniqueName) {
		TransactionList transactions = getTransactionListForFeature(uniqueName);
		return transactions.getCurrentTransaction();
	}

	@Override
	public void setToPreviousTransactionForFeature(String uniqueName) {
		TransactionList transactions = getTransactionListForFeature(uniqueName);
		transactions.setToPreviousTransaction();
		writeTransactionListForFeature(uniqueName, transactions);
	}

	@Override
	public void setToNextTransactionForFeature(String uniqueName) {
		TransactionList transactions = getTransactionListForFeature(uniqueName);
		transactions.setToNextTransaction();
		writeTransactionListForFeature(uniqueName, transactions);
	}
	
	@Override
	public void resetCurrentIndexForFeature(String uniqueName) {
		TransactionList transactions = getTransactionListForFeature(uniqueName);
		transactions.resetCurrentIndex();
		writeTransactionListForFeature(uniqueName, transactions);
	}
	
	@Override
	public int getCurrentIndexForFeature(String uniqueName) {
		return getTransactionListForFeature(uniqueName).getCurrentIndex();
	}

	@Override
	public int getHistorySizeForFeature(String uniqueName) {
		return getTransactionListForFeature(uniqueName).size();
	}

	@Override
	public void deleteHistoryForFeature(String uniqueName) {
		DatabaseEntry key = null;
		try {
			key = new DatabaseEntry(uniqueName.getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException e) {
		}
		historyDb.delete(null, key);
	}
	
	@Override
	public Transaction popTransactionForFeature(String uniqueName) {
		TransactionList transactions = getTransactionListForFeature(uniqueName);
		Transaction transaction = transactions.get(transactions.size() - 1);
		transactions.pruneTransactions(transactions.size() - 1);
		writeTransactionListForFeature(uniqueName, transactions);
		return transaction;
	}

	@Override
	public Transaction peekTransactionForFeature(String uniqueName) {
		TransactionList transactions = getTransactionListForFeature(uniqueName);
		Transaction transaction = transactions.get(transactions.size() - 1);
		return transaction;
	}

	public Collection<String> getKeys() {
		Collection<String> keys = new ArrayList<String>();
		Cursor cursor = historyDb.openCursor(null, null);
		DatabaseEntry key = new DatabaseEntry();
		DatabaseEntry value = new DatabaseEntry();
		try {
			while (cursor.getNext(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
				keys.add(new String(key.getData()));
			}
		}
		catch (DatabaseException e) {
		}
		finally {
			cursor.close();
		}
		return keys;
	}

	@Override
	public void close() {
		historyDb.close();
		historySerializedDb.close();
		dbEnvironment.close();
	}
	
	@Override
	public TransactionList getTransactionListForFeature(String uniqueName) {
		DatabaseEntry key = null;
		try {
			key = new DatabaseEntry(uniqueName.getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException e) {
		}
		DatabaseEntry value = new DatabaseEntry();
		if (historyDb.get(null, key, value, null) == OperationStatus.NOTFOUND) {
			return new TransactionList();
		}
		return historySerializedDb.entryToObject(value);
	}
	
	@Override
	public void writeTransactionListForFeature(String uniqueName, TransactionList transactions) {
		DatabaseEntry key = null;
		try {
			key = new DatabaseEntry(uniqueName.getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException e) {
		}
		DatabaseEntry value = historySerializedDb.objectToEntry(transactions);
		historyDb.put(null, key, value);
	}

	@Override
	public Iterator<TransactionList> getTransactionListIterator() {
		return new JEDatabaseIterator<TransactionList>(historyDb, historySerializedDb);
	}
}
