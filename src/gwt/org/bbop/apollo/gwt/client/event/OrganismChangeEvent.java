package org.bbop.apollo.gwt.client.event;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by ndunn on 1/19/15.
 */
public class OrganismChangeEvent extends GwtEvent<OrganismChangeEventHandler>{

    public static Type<OrganismChangeEventHandler> TYPE = new Type<OrganismChangeEventHandler>();

    public String organismId ;

    public String getOrganismId() {
        return organismId;
    }

    public void setOrganismId(String organismId) {
        this.organismId = organismId;
    }

    @Override
    public Type<OrganismChangeEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(OrganismChangeEventHandler handler) {
        handler.onOrganismChanged(this);
    }
}
