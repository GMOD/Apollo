package org.bbop.apollo.gwt.client.event;

import com.google.gwt.event.shared.EventHandler;

/**
 * Created by ndunn on 1/19/15.
 */
public interface OrganismChangeEventHandler extends EventHandler{

    void onOrganismChanged(OrganismChangeEvent authenticationEvent);


}
