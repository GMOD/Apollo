package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DialogBox;
import org.bbop.apollo.gwt.client.Annotator;
import org.bbop.apollo.gwt.client.MainPanel;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.dto.UserInfo;
import org.bbop.apollo.gwt.client.event.SequenceLoadEvent;

import java.util.List;

/**
 * Created by ndunn on 1/14/15.
 */
public class UserRestService {


    public static void login(RequestCallback requestCallback, JSONObject data){
        RestService.sendRequest(requestCallback,"/Login",data.toString());
    }

    public static void login(String username,String password){
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                Window.alert(response.getText());
//                JSONValue returnValue = JSONParser.parseStrict(response.getText());
//                JSONArray array = returnValue.isArray();
                
                Window.Location.reload();

//                dialogBox.hide();

            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error loading organisms");
            }
        };
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("operation", new JSONString("login"));
        jsonObject.put("username", new JSONString(username));
        jsonObject.put("password",new JSONString(password));
        login(requestCallback, jsonObject);
    }

    public static void loadUsers(RequestCallback requestCallback){
        RestService.sendRequest(requestCallback,"/user/loadUsers/");
    }

    public static void loadUsers(final List<UserInfo> userInfoList) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();

                userInfoList.clear();

                for(int i = 0 ; i < array.size() ; i++){
                    JSONObject object = array.get(i).isObject();
                    
                    UserInfo userInfo = new UserInfo();
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

}
