package org.bbop.apollo.gwt.client.event;

import com.google.gwt.event.shared.GwtEvent;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;

import java.util.List;

/**
 * Created by ndunn on 1/19/15.
 */
public class OrganismChangeEvent extends GwtEvent<OrganismChangeEventHandler>{

    public static Type<OrganismChangeEventHandler> TYPE = new Type<OrganismChangeEventHandler>();

    public List<OrganismInfo> organismInfoList;
    private Action action;
    private String currentSequence;
    private String currentOrganism;

    public OrganismChangeEvent(){}
    public OrganismChangeEvent(List<OrganismInfo> organismInfoList){
        this.organismInfoList = organismInfoList ;
    }

    public OrganismChangeEvent(Action action) {
        this.action = action;
    }

    public OrganismChangeEvent(Action changedOrganism, String sequenceNameString,String organismNameString) {
        this.action = changedOrganism ;
        this.currentSequence = sequenceNameString ;
        this.currentOrganism = organismNameString ;
    }

    public String getCurrentOrganism() {
        return currentOrganism;
    }

    public List<OrganismInfo> getOrganismInfoList() {
        return organismInfoList;
    }

    public void setOrganismInfoList(List<OrganismInfo> organismInfoList) {
        this.organismInfoList = organismInfoList;
    }

    @Override
    public Type<OrganismChangeEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(OrganismChangeEventHandler handler) {
        handler.onOrganismChanged(this);
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    public String getCurrentSequence() {
        return currentSequence;
    }

    public enum Action {
        CHANGED_ORGANISM, LOADED_ORGANISMS
    }


}
