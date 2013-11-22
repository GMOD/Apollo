package org.bbop.apollo.web.datastore.history;

import java.util.ArrayList;
import java.util.List;

public class CompoundTransaction {

	private List<Transaction> transactions;
	
	public CompoundTransaction() {
		transactions = new ArrayList<Transaction>();
	}
	
	public List<Transaction> getTransactions() {
		return transactions;
	}
	
	public void addTransaction(Transaction transaction) {
		transactions.add(transaction);
	}
	
}
