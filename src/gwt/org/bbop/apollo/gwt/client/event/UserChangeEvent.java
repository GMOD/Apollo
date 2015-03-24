package org.bbop.apollo.gwt.client.event;

import com.google.gwt.event.shared.GwtEvent;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.UserInfo;

import java.util.List;

/**
 * Created by ndunn on 1/19/15.
 */
public class UserChangeEvent extends GwtEvent<UserChangeEventHandler>{

    public static Type<UserChangeEventHandler> TYPE = new Type<UserChangeEventHandler>();

    private List<UserInfo> userInfoList;
    private Action action ;
    private String group ;

    public UserChangeEvent(){}
    public UserChangeEvent(List<UserInfo> userInfoList,Action action,String group){
        this.userInfoList = userInfoList ;
        this.action = action ;
        this.group = group ;
    }

    public UserChangeEvent(List<UserInfo> userInfoList,Action action){
        this.userInfoList = userInfoList ;
        this.action = action ;
    }

    public List<UserInfo> getUserInfoList() {
        return userInfoList;
    }

    public void setUserInfoList(List<UserInfo> userInfoList) {
        this.userInfoList = userInfoList;
    }

    @Override
    public Type<UserChangeEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(UserChangeEventHandler handler) {
        handler.onUserChanged(this);
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
        RELOAD_USERS,
    }
}
