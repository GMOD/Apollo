package org.bbop.apollo.gwt.client.event;

import com.google.gwt.event.shared.GwtEvent;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;

/**
 * Created by ndunn on 1/19/15.
 */
public class SequenceLoadEvent extends GwtEvent<SequenceLoadEventHandler> {

    public static Type<SequenceLoadEventHandler> TYPE = new Type<SequenceLoadEventHandler>();

    public enum Action {
        FINISHED_LOADING
    }

    private Action thisAction;

    @Override
    public Type<SequenceLoadEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(SequenceLoadEventHandler handler) {
        handler.onSequenceLoaded(this);
    }

    //    public SequenceLoadEvent(OrganismInfo organismInfo,SequenceInfo sequenceInfo){
//        this.organismInfo = organismInfo ;
//        this.sequenceInfo = sequenceInfo ;
//    }

    public SequenceLoadEvent(Action action) {
        this.thisAction = action;
    }
}
