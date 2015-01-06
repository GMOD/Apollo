package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.event.AnnotationEvent
import org.bbop.apollo.event.AnnotationListener

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Mimicks: org.bbop.apollo.web.datastore.AbstractDataStoreManager
 * but as a service instead of a Singleton
 */
//@Transactional
class DataListenerHandler {

    private static DataListenerHandler instance  = null

//    private Map<String, AbstractDataStore> trackToStore;
    Map<String,AnnotationListener> listeners = new HashMap<>()

    static DataListenerHandler getInstance() {
        if(!instance){
            instance = new DataListenerHandler()
        }
        return instance
    }
//    public void addDataStore(String track, AbstractDataStore dataStore) {
//        trackToStore.put(track, dataStore);
//    }
//
//    public AbstractDataStore getDataStore(String track) {
//        return trackToStore.get(track);
//    }
//
//    public Collection<AbstractDataStore> getDataStores() {
//        return trackToStore.values();
//    }

    public void addDataStoreChangeListener(AnnotationListener listener) {
        listeners.put(listener.class,listener);
    }

    public void removeDataStoreChangeListener(AnnotationListener listener) {
        listeners.remove(listener.class);
    }

    public void fireDataStoreChange(AnnotationEvent ... events) {
        println "# of listeners ${listeners.size()}"
        for (AnnotationListener listener : listeners.values()) {
            listener.handleChangeEvent(events);
        }
    }

//    public void closeDataStore(String track) {
//        AbstractDataStore store = getDataStore(track);
//        if (store != null) {
//            store.close();
//            trackToStore.remove(track);
//        }
//    }


}
