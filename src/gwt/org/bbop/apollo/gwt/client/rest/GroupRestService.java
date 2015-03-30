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
import org.bbop.apollo.gwt.client.AnnotatorPanel;
import org.bbop.apollo.gwt.client.dto.GroupInfo;
import org.bbop.apollo.gwt.client.event.GroupChangeEvent;

import java.util.List;

/**
 * Created by ndunn on 3/30/15.
 */
public class GroupRestService {


    public static void loadGroups(final List<GroupInfo> groupInfoList) {

        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                groupInfoList.clear();
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();

                for (int i = 0; i < array.size(); i++) {
                    JSONObject object = array.get(i).isObject();

                    GroupInfo groupInfo = new GroupInfo();
                    groupInfo.setId((long) object.get("id").isNumber().doubleValue());
                    groupInfo.setName(object.get("name").isString().stringValue());
                    groupInfo.setNumberOfUsers((int) object.get("numberOfUsers").isNumber().doubleValue());


                    groupInfoList.add(groupInfo);
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("errror retrieving groups");
            }
        };

        RestService.sendRequest(requestCallback, "/group/loadGroups/");
    }

    public static void updateGroup(final GroupInfo selectedGroupInfo) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                Annotator.eventBus.fireEvent(new GroupChangeEvent(GroupChangeEvent.Action.RELOAD_GROUPS));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("error updating group "+selectedGroupInfo.getName()+" "+exception);
            }
        };
        RestService.sendRequest(requestCallback, "/group/updateGroup/", "data="+selectedGroupInfo.toJSON().toString());
    }
}
