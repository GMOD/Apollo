package org.bbop.apollo.gwt.client.assemblage;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by nathandunn on 9/21/16.
 */
public class AssemblageViewEvent extends GwtEvent<AssemblageViewEventHandler> {
    public static Type<AssemblageViewEventHandler> TYPE = new Type<AssemblageViewEventHandler>();

    public Type<AssemblageViewEventHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(AssemblageViewEventHandler handler) {
        handler.onAssemblageView(this);
    }
}
