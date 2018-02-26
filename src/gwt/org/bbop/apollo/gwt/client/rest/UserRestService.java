package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.*;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.client.Annotator;
import org.bbop.apollo.gwt.client.ErrorDialog;
import org.bbop.apollo.gwt.client.LoginDialog;
import org.bbop.apollo.gwt.client.dto.UserInfo;
import org.bbop.apollo.gwt.client.dto.UserInfoConverter;
import org.bbop.apollo.gwt.client.dto.UserOrganismPermissionInfo;
import org.bbop.apollo.gwt.client.event.UserChangeEvent;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by ndunn on 1/14/15.
 */
public class UserRestService {


    public static void login(RequestCallback requestCallback, JSONObject data) {
        RestService.sendRequest(requestCallback, "Login", data.toString());
    }


    public static void registerAdmin(RequestCallback requestCallback, JSONObject data) {
        RestService.sendRequest(requestCallback, "login/registerAdmin", data);
    }

    public static void login(String username, String password, Boolean rememberMe, final LoginDialog loginDialog) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue j= null ;
                try {
                    j = JSONParser.parseStrict(response.getText());
                } catch (Exception e) {
                    GWT.log("Error parsing login response: "+e);
//                    Window.alert("Error parsing login response, reloading");
                    Window.Location.reload();
                    return ;
                }
                JSONObject o=j.isObject();
                if(o.get("error")!=null) {
                    loginDialog.setError(o.get("error").isString().stringValue() + "!");
                }
                else {
                    Window.Location.reload();
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error loading organisms");
            }
        };

        String passwordString = URL.encodeQueryString(password);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("operation", new JSONString("login"));
        jsonObject.put("username", new JSONString(username));
        jsonObject.put("password", new JSONString(passwordString));
        jsonObject.put("rememberMe", JSONBoolean.getInstance(rememberMe));
        login(requestCallback, jsonObject);
    }

    public static void loadUsers(RequestCallback requestCallback) {
        loadUsers(requestCallback,-1,-1,"","name",true);
    }

    public static void loadUsers(RequestCallback requestCallback, Integer start, Integer length, String searchNameString, String searchColumnString, Boolean sortAscending) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("start",new JSONNumber(start < 0 ? 0 : start));
        jsonObject.put("length",new JSONNumber(length < 0 ? 20 : length));
        jsonObject.put("name",new JSONString(searchNameString));
        jsonObject.put("sortColumn",new JSONString(searchColumnString));
        jsonObject.put("sortAscending",JSONBoolean.getInstance(sortAscending));
        RestService.sendRequest(requestCallback, "user/loadUsers/",jsonObject);
    }

    public static void loadUsers(final List<UserInfo> userInfoList) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();

                userInfoList.clear();

                for (int i = 0; array != null && i < array.size(); i++) {
                    JSONObject object = array.get(i).isObject();
                    UserInfo userInfo = UserInfoConverter.convertToUserInfoFromJSON(object);
                    userInfoList.add(userInfo);
                }
                Annotator.eventBus.fireEvent(new UserChangeEvent(UserChangeEvent.Action.USERS_RELOADED));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error loading organisms");
            }
        };

        loadUsers(requestCallback);
    }
    public static void logout() {
        logout(null);
    }

    public static void logout(final String redirectUrl) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                if(redirectUrl!=null){
                    Window.Location.replace(redirectUrl);
                }
                else{
                    Window.Location.reload();
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error logging out " + exception);
            }
        };
        RestService.sendRequest(requestCallback, "Login?operation=logout");
    }

    public static void updateUser(RequestCallback requestCallback, UserInfo selectedUserInfo) {
        JSONObject jsonObject = selectedUserInfo.toJSON();
        RestService.sendRequest(requestCallback, "user/updateUser", "data=" + jsonObject.toString());
    }

    public static void updateUserTrackPanelPreference(RequestCallback requestCallback, boolean tracklist) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tracklist", JSONBoolean.getInstance(tracklist));
        RestService.sendRequest(requestCallback, "user/updateTrackListPreference", "data=" + jsonObject.toString());
    }


    public static void deleteUser(final List<UserInfo> userInfoList, UserInfo selectedUserInfo) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue v=JSONParser.parseStrict(response.getText());
                JSONObject o=v.isObject();
                if(o.containsKey(FeatureStringEnum.ERROR.getValue())) {
                    new ErrorDialog("Error Deleting User",o.get(FeatureStringEnum.ERROR.getValue()).isString().stringValue(),true,true);
                }
                else{
                    loadUsers(userInfoList);
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error deleting user: " + exception);
            }
        };
        JSONObject jsonObject = selectedUserInfo.toJSON();
        RestService.sendRequest(requestCallback, "user/deleteUser", "data=" + jsonObject.toString());
    }

    public static void createUser(final List<UserInfo> userInfoList, UserInfo selectedUserInfo) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue v=JSONParser.parseStrict(response.getText());
                JSONObject o=v.isObject();
                if(o.containsKey(FeatureStringEnum.ERROR.getValue())) {
                    new ErrorDialog("Error Creating User",o.get(FeatureStringEnum.ERROR.getValue()).isString().stringValue(),true,true);
                }
                else {
                    loadUsers(userInfoList);
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error adding user: " + exception);
            }
        };
        JSONObject jsonObject = selectedUserInfo.toJSON();
        RestService.sendRequest(requestCallback, "user/createUser", "data=" + jsonObject.toString());

    }

    public static void removeUserFromGroup(final String groupName, final List<UserInfo> userInfoList, final UserInfo selectedUserInfo) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                List<UserInfo> userInfoList = new ArrayList<>();
                userInfoList.add(selectedUserInfo);
                Annotator.eventBus.fireEvent(new UserChangeEvent(userInfoList, UserChangeEvent.Action.REMOVE_USER_FROM_GROUP, groupName));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error removing group from user: " + exception);
            }
        };
        JSONObject jsonObject = selectedUserInfo.toJSON();
        jsonObject.put("group", new JSONString(groupName));
        RestService.sendRequest(requestCallback, "user/removeUserFromGroup", "data=" + jsonObject.toString());
    }

    public static void addUserToGroup(final String groupName, final UserInfo selectedUserInfo) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                List<UserInfo> userInfoList = new ArrayList<>();
                userInfoList.add(selectedUserInfo);
                Annotator.eventBus.fireEvent(new UserChangeEvent(userInfoList, UserChangeEvent.Action.ADD_USER_TO_GROUP, groupName));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error adding group to user: " + exception);
            }
        };
        JSONObject jsonObject = selectedUserInfo.toJSON();
        jsonObject.put("group", new JSONString(groupName));
        RestService.sendRequest(requestCallback, "user/addUserToGroup", "data=" + jsonObject.toString());
    }

    public static void updateOrganismPermission(UserOrganismPermissionInfo object) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                GWT.log("success");
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error updating permissions: " + exception);
            }
        };
        RestService.sendRequest(requestCallback, "user/updateOrganismPermission", "data=" + object.toJSON().toString());
    }


}
