package org.bbop.apollo.web.datastore;

import java.util.EventListener;

public interface DataStoreChangeListener extends EventListener {
	
	public void handleDataStoreChangeEvent(DataStoreChangeEvent ... events);

}
