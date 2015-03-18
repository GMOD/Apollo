package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.client.dto.UserInfo;

import java.util.List;

/**
 * Created by ndunn on 1/14/15.
 */
public class UserRestService {


    public static void login(RequestCallback requestCallback, JSONObject data) {
        RestService.sendRequest(requestCallback, "/Login", data.toString());
    }

    public static void login(String username, String password) {
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

                    UserInfo userInfo = new UserInfo();
                    userInfo.setUserId((long) object.get("userId").isNumber().doubleValue());
                    userInfo.setFirstName(object.get("firstName").isString().stringValue());
                    userInfo.setLastName(object.get("lastName").isString().stringValue());
                    userInfo.setEmail(object.get("username").isString().stringValue());

                    userInfoList.add(userInfo);
                }

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
                Window.alert("Error logging out "+exception);
            }
        };
        logout(requestCallback);
    }

    public static void updateUser(final List<UserInfo> userInfoList, UserInfo selectedUserInfo) {
        Window.alert("updated user info for " + selectedUserInfo.getEmail());
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                loadUsers(userInfoList);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error updating user: "+exception);
            }
        };
        JSONObject jsonObject = selectedUserInfo.toJSON();
        RestService.sendRequest(requestCallback, "/user/updateUser","data="+jsonObject.toString());
    }

    public static void deleteUser(final List<UserInfo> userInfoList, UserInfo selectedUserInfo) {
        Window.alert("deleteing user " + selectedUserInfo.getEmail());
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                loadUsers(userInfoList);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error deleting user: "+exception);
            }
        };
        JSONObject jsonObject = selectedUserInfo.toJSON();
        RestService.sendRequest(requestCallback, "/user/deleteUser","data="+jsonObject.toString());
    }

    public static void createUser(final List<UserInfo> userInfoList, UserInfo selectedUserInfo) {
        Window.alert("creating user with email "+selectedUserInfo.getEmail());
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                loadUsers(userInfoList); 
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error adding user: "+exception);
            }
        };
        JSONObject jsonObject = selectedUserInfo.toJSON();
        RestService.sendRequest(requestCallback, "/user/createUser","data="+jsonObject.toString());

    }
}
