package org.bbop.apollo.web.datastore.history;

import java.util.Iterator;

public abstract class AbstractHistoryStore {

	public abstract void addTransaction(Transaction transaction);
	
	public abstract Transaction getCurrentTransactionForFeature(String uniqueName);

	public abstract void setToPreviousTransactionForFeature(String uniqueName);
	
	public abstract void setToNextTransactionForFeature(String uniqueName);
	
	public abstract void resetCurrentIndexForFeature(String uniqueName);
	
	public abstract int getCurrentIndexForFeature(String uniqueName);
	
	public abstract int getHistorySizeForFeature(String uniqueName);
	
	public abstract void deleteHistoryForFeature(String uniqueName);
	
	public abstract Transaction popTransactionForFeature(String uniqueName);
	
	public abstract Transaction peekTransactionForFeature(String uniqueName);

	public abstract TransactionList getTransactionListForFeature(String uniqueName);
	
	public abstract void close();
	
	public abstract Iterator<TransactionList> getTransactionListIterator();
	
	public abstract void writeTransactionListForFeature(String uniqueName, TransactionList transactions);

}
