package org.bbop.apollo.gwt.client.event;

import com.google.gwt.event.shared.GwtEvent;
import org.bbop.apollo.gwt.client.dto.GroupInfo;

import java.util.List;

/**
 * Created by ndunn on 1/19/15.
 */
public class GroupChangeEvent extends GwtEvent<GroupChangeEventHandler>{

    public static Type<GroupChangeEventHandler> TYPE = new Type<GroupChangeEventHandler>();

    private List<GroupInfo> groupInfoList;
    private Action action ;
    private String group ;

    public GroupChangeEvent(){}
    public GroupChangeEvent(List<GroupInfo> groupInfoList, Action action, String group){
        this.groupInfoList = groupInfoList ;
        this.action = action ;
        this.group = group ;
    }

    public GroupChangeEvent(List<GroupInfo> groupInfoList, Action action){
        this.groupInfoList = groupInfoList ;
        this.action = action ;
    }

    public GroupChangeEvent(Action action) {
        this.action = action;
    }

    public List<GroupInfo> getGroupInfoList() {
        return groupInfoList;
    }

    public void setGroupInfoList(List<GroupInfo> groupInfoList) {
        this.groupInfoList = groupInfoList;
    }

    @Override
    public Type<GroupChangeEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(GroupChangeEventHandler handler) {
        handler.onGroupChanged(this);
    }

    public Action getAction() {
        return action;
    }

    public String getGroup() {
        return group;
    }

    public enum Action{
        ADD_USER_TO_GROUP,
        REMOVE_USER_FROM_GROUP,
        RELOAD_GROUPS,
        ADD_GROUP,
        REMOVE_GROUP,
        GROUPS_RELOADED,
    }
}
