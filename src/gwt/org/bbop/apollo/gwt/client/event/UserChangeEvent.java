package org.bbop.apollo.gwt.client.event;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.UserInfo;
import org.bbop.apollo.gwt.shared.PermissionEnum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ndunn on 1/19/15.
 */
public class UserChangeEvent extends GwtEvent<UserChangeEventHandler>{

    public static Type<UserChangeEventHandler> TYPE = new Type<UserChangeEventHandler>();

    private List<UserInfo> userInfoList;
    private Action action ;
    private String group ;
    private PermissionEnum highestPermission ;

    public UserChangeEvent(Action action){
        this.action = action ;
    }
    public UserChangeEvent(List<UserInfo> userInfoList,Action action,String group){
        this.userInfoList = userInfoList ;
        this.action = action ;
        this.group = group ;
    }


    public UserChangeEvent(Action action,PermissionEnum highestPermission){
        this.action = action ;
        this.highestPermission = highestPermission ;
        GWT.log(highestPermission.getDisplay());
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

    public PermissionEnum getHighestPermission() {
        return highestPermission;
    }

    public void setHighestPermission(PermissionEnum highestPermission) {
        this.highestPermission = highestPermission;
    }

    public enum Action{
        ADD_USER_TO_GROUP,
        REMOVE_USER_FROM_GROUP,
        RELOAD_USERS,
        PERMISSION_CHANGED,
        USERS_RELOADED,
    }
}
