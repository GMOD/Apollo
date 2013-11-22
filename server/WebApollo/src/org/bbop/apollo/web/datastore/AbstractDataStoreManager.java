package org.bbop.apollo.web.datastore;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AbstractDataStoreManager {

	private static AbstractDataStoreManager instance;
	
	private Map<String, AbstractDataStore> trackToStore;
	private Collection<DataStoreChangeListener> listeners;
	
	public static AbstractDataStoreManager getInstance() {
		if (instance == null) {
			instance = new AbstractDataStoreManager();
		}
		return instance;
	}
	
	private AbstractDataStoreManager() {
		trackToStore = new ConcurrentHashMap<String, AbstractDataStore>();
		listeners = new ConcurrentLinkedQueue<DataStoreChangeListener>();
	}
	
	public void addDataStore(String track, AbstractDataStore dataStore) {
		trackToStore.put(track, dataStore);
	}
	
	public AbstractDataStore getDataStore(String track) {
		return trackToStore.get(track);
	}
	
	public Collection<AbstractDataStore> getDataStores() {
		return trackToStore.values();
	}
	
	public void addDataStoreChangeListener(DataStoreChangeListener listener) {
		listeners.add(listener);
	}
	
	public void removeDataStoreChangeListener(DataStoreChangeListener listener) {
		listeners.remove(listener);
	}
	
	public void fireDataStoreChange(DataStoreChangeEvent ... events) {
		for (DataStoreChangeListener listener : listeners) {
			listener.handleDataStoreChangeEvent(events);
		}
	}
	
	public void closeDataStore(String track) {
		AbstractDataStore store = getDataStore(track);
		if (store != null) {
			store.close();
			trackToStore.remove(track);
		}
	}
	
}
