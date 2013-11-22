package org.bbop.apollo.web.datastore.history;

import java.io.Serializable;
import java.util.ArrayList;

public class TransactionList extends ArrayList<Transaction> implements Serializable {

	private static final long serialVersionUID = 1L;

	private int currentIndex;
	
	public TransactionList() {
		currentIndex = -1;
	}
	
	public boolean add(Transaction transaction) {
		if (currentIndex >= 0 && currentIndex + 1 < size()) {
			removeRange(currentIndex + 1, size());
		}
		++currentIndex;
		return super.add(transaction);
	}
	
	public Transaction getCurrentTransaction() {
		if (currentIndex < 0) {
			return null;
		}
		return get(currentIndex);
	}
	
	public void setToPreviousTransaction() {
		if (currentIndex < 0) {
			return;
		}
		--currentIndex;
	}
	
	public void setToNextTransaction() {
		if (currentIndex >= size() - 1) {
			return;
		}
		++currentIndex;
	}
	
	public void resetCurrentIndex() {
		currentIndex = 0;
	}
	
	public int getCurrentIndex() {
		return currentIndex;
	}
	
	public void pruneTransactions(int fromIndex) {
		removeRange(fromIndex, size());
		if (currentIndex > size() - 1) {
			currentIndex = size() - 1;
		}
	}
	
}
