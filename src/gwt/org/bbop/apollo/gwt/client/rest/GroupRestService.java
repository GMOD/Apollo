package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.client.dto.GroupInfo;

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
}
