package org.bbop.apollo.web.datastore.history;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AbstractHistoryStoreManager {

	private static AbstractHistoryStoreManager instance;
	
	private Map<String, AbstractHistoryStore> trackToStore;
	
	public static AbstractHistoryStoreManager getInstance() {
		if (instance == null) {
			instance = new AbstractHistoryStoreManager();
		}
		return instance;
	}
	
	private AbstractHistoryStoreManager() {
		trackToStore = new HashMap<String, AbstractHistoryStore>();
	}
	
	public void addHistoryStore(String track, AbstractHistoryStore historyStore) {
		trackToStore.put(track, historyStore);
	}
	
	public AbstractHistoryStore getHistoryStore(String track) {
		return trackToStore.get(track);
	}
	
	public Collection<AbstractHistoryStore> getHistoryStores() {
		return trackToStore.values();
	}
	
	public void closeHistoryStore(String track) {
		AbstractHistoryStore store = getHistoryStore(track);
		if (store != null) {
			store.close();
			trackToStore.remove(track);
		}
	}
	
}
