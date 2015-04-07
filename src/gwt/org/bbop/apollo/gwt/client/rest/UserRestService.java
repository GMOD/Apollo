package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.*;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.client.Annotator;
import org.bbop.apollo.gwt.client.dto.UserInfo;
import org.bbop.apollo.gwt.client.dto.UserInfoConverter;
import org.bbop.apollo.gwt.client.dto.UserOrganismPermissionInfo;
import org.bbop.apollo.gwt.client.event.UserChangeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by ndunn on 1/14/15.
 */
public class UserRestService {


    public static void login(RequestCallback requestCallback, JSONObject data) {
        RestService.sendRequest(requestCallback, "/Login", data.toString());
    }

    public static void registerAdmin(String username, String password, Boolean rememberMe) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                Window.Location.reload();
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error loading organisms");
            }
        };
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("operation", new JSONString("register"));
        jsonObject.put("username", new JSONString(username));
        jsonObject.put("password", new JSONString(password));
        jsonObject.put("rememberMe", JSONBoolean.getInstance(rememberMe));
        registerAdmin(requestCallback, jsonObject);
    }

    public static void registerAdmin(RequestCallback requestCallback, JSONObject data) {
        RestService.sendRequest(requestCallback, "/login/registerAdmin", data);
    }

    public static void login(String username, String password, Boolean rememberMe) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                Window.Location.reload();
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error loading organisms");
            }
        };
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("operation", new JSONString("login"));
        jsonObject.put("username", new JSONString(username));
        jsonObject.put("password", new JSONString(password));
        jsonObject.put("rememberMe", JSONBoolean.getInstance(rememberMe));
        login(requestCallback, jsonObject);
    }

    public static void loadUsers(RequestCallback requestCallback) {
        RestService.sendRequest(requestCallback, "/user/loadUsers/");
    }

    public static void loadUsers(final List<UserInfo> userInfoList) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();

                userInfoList.clear();

                for (int i = 0; i < array.size(); i++) {
                    JSONObject object = array.get(i).isObject();
                    UserInfo userInfo = UserInfoConverter.convertToUserInfoFromJSON(object);
                    userInfoList.add(userInfo);
                }

                Annotator.eventBus.fireEvent(new UserChangeEvent(UserChangeEvent.Action.USERS_RELOADED));

            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error loading organisms");
            }
        };

        loadUsers(requestCallback);
    }

    public static void logout(RequestCallback requestCallback) {
        RestService.sendRequest(requestCallback, "/Login?operation=logout");
    }

    public static void logout() {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                Window.Location.reload();
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error logging out " + exception);
            }
        };
        logout(requestCallback);
    }

    public static void updateUser(final List<UserInfo> userInfoList, UserInfo selectedUserInfo) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                loadUsers(userInfoList);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error updating user: " + exception);
            }
        };
        JSONObject jsonObject = selectedUserInfo.toJSON();
        RestService.sendRequest(requestCallback, "/user/updateUser", "data=" + jsonObject.toString());
    }

    public static void deleteUser(final List<UserInfo> userInfoList, UserInfo selectedUserInfo) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                loadUsers(userInfoList);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error deleting user: " + exception);
            }
        };
        JSONObject jsonObject = selectedUserInfo.toJSON();
        RestService.sendRequest(requestCallback, "/user/deleteUser", "data=" + jsonObject.toString());
    }

    public static void createUser(final List<UserInfo> userInfoList, UserInfo selectedUserInfo) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                loadUsers(userInfoList);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error adding user: " + exception);
            }
        };
        JSONObject jsonObject = selectedUserInfo.toJSON();
        RestService.sendRequest(requestCallback, "/user/createUser", "data=" + jsonObject.toString());

    }

    public static void removeUserFromGroup(final String groupName, final List<UserInfo> userInfoList, final UserInfo selectedUserInfo) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                List<UserInfo> userInfoList = new ArrayList<>();
                userInfoList.add(selectedUserInfo);
                Annotator.eventBus.fireEvent(new UserChangeEvent(userInfoList, UserChangeEvent.Action.RELOAD_USERS));
                Annotator.eventBus.fireEvent(new UserChangeEvent(userInfoList, UserChangeEvent.Action.REMOVE_USER_FROM_GROUP, groupName));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error removing group from user: " + exception);
            }
        };
        JSONObject jsonObject = selectedUserInfo.toJSON();
        jsonObject.put("group", new JSONString(groupName));
        RestService.sendRequest(requestCallback, "/user/removeUserFromGroup", "data=" + jsonObject.toString());
    }

    public static void addUserToGroup(final String groupName, final UserInfo selectedUserInfo) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                List<UserInfo> userInfoList = new ArrayList<>();
                userInfoList.add(selectedUserInfo);
                Annotator.eventBus.fireEvent(new UserChangeEvent(userInfoList, UserChangeEvent.Action.RELOAD_USERS));
                Annotator.eventBus.fireEvent(new UserChangeEvent(userInfoList, UserChangeEvent.Action.ADD_USER_TO_GROUP, groupName));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error adding group to user: " + exception);
            }
        };
        JSONObject jsonObject = selectedUserInfo.toJSON();
        jsonObject.put("group", new JSONString(groupName));
        RestService.sendRequest(requestCallback, "/user/addUserToGroup", "data=" + jsonObject.toString());
    }

    public static void updateOrganismPermission(UserOrganismPermissionInfo object) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                GWT.log("success");
//                loadUsers(userInfoList);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error updating permissions: " + exception);
            }
        };
        RestService.sendRequest(requestCallback, "/user/updateOrganismPermission", "data=" + object.toJSON().toString());
    }
}
