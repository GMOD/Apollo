package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;
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
////                    GWT.log(object.toString());
//                    SequenceInfo sequenceInfo = new SequenceInfo();
//                    sequenceInfo.setId((long) object.get("id").isNumber().doubleValue());
//                    sequenceInfo.setName(object.get("name").isString().stringValue());
//                    sequenceInfo.setLength((int) object.get("length").isNumber().isNumber().doubleValue());
//                    sequenceInfo.setStart((int) object.get("start").isNumber().isNumber().doubleValue());
//                    sequenceInfo.setEnd((int) object.get("end").isNumber().isNumber().doubleValue());
//                    GWT.log("get default: "+object.get("default"));
//                    if(object.get("default")!=null){
//                        GWT.log("setting default to "+ sequenceInfo.getName());
//                        sequenceInfo.setDefault(object.get("default").isBoolean().booleanValue());
//                    }
//                    sequenceInfoList.add(sequenceInfo);
                }
//                SequenceLoadEvent contextSwitchEvent = new SequenceLoadEvent(SequenceLoadEvent.Action.FINISHED_LOADING);
//                Annotator.eventBus.fireEvent(contextSwitchEvent);
//                GWT.log("added # sequences: "+sequenceInfoList.size());

            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error loading organisms");
            }
        };
        
        loadUsers(requestCallback);
    }

}
