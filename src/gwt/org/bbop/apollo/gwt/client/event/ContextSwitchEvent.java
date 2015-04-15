package org.bbop.apollo.gwt.client.event;

import com.google.gwt.event.shared.GwtEvent;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;

import java.util.List;

/**
 * Created by ndunn on 1/19/15.
 */
public class ContextSwitchEvent extends GwtEvent<ContextSwitchEventHandler>{

    public static Type<ContextSwitchEventHandler> TYPE = new Type<ContextSwitchEventHandler>();

    public OrganismInfo organismInfo;
    public SequenceInfo sequenceInfo;

//    public ContextSwitchEvent(OrganismInfo organismInfo,SequenceInfo sequenceInfo){
//        this.organismInfo = organismInfo ;
//        this.sequenceInfo = sequenceInfo ;
//    }

    public ContextSwitchEvent(String organismInfoId) {
        OrganismInfo organismInfo = new OrganismInfo();
        organismInfo.setId(organismInfoId);
        this.organismInfo = organismInfo ;
    }

    public ContextSwitchEvent(String sequenceName, String organismInfoId) {
        SequenceInfo sequenceInfo = new SequenceInfo();
        sequenceInfo.setName(sequenceName);
        this.sequenceInfo = sequenceInfo ;

        OrganismInfo organismInfo = new OrganismInfo();
        organismInfo.setId(organismInfoId);
        this.organismInfo = organismInfo ;
    }

    public OrganismInfo getOrganismInfo() {
        return organismInfo;
    }

    public void setOrganismInfo(OrganismInfo organismInfo) {
        this.organismInfo = organismInfo;
    }

    public SequenceInfo getSequenceInfo() {
        return sequenceInfo;
    }

    public void setSequenceInfo(SequenceInfo sequenceInfo) {
        this.sequenceInfo = sequenceInfo;
    }

    @Override
    public Type<ContextSwitchEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ContextSwitchEventHandler handler) {
        handler.onContextSwitched(this);
    }
}
