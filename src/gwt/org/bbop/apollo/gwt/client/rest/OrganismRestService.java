package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;
import com.google.gwt.view.client.SelectionChangeEvent;
import org.bbop.apollo.gwt.client.MainPanel;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;

import java.util.List;

/**
 * Created by ndunn on 1/14/15.
 */
public class OrganismRestService {

    public static void loadOrganisms(RequestCallback requestCallback) {
        RestService.sendRequest(requestCallback, "/organism/findAllOrganisms");
    }

    public static void loadOrganisms(final List<OrganismInfo> organismInfoList) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                organismInfoList.clear();
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();
                for (int i = 0; i < array.size(); i++) {
                    JSONObject object = array.get(i).isObject();
//                    GWT.log(object.toString());
                    OrganismInfo organismInfo = new OrganismInfo();
                    organismInfo.setId(object.get("id").isNumber().toString());
                    organismInfo.setName(object.get("commonName").isString().stringValue());
                    organismInfo.setNumSequences(object.get("sequences").isArray().size());
                    organismInfo.setDirectory(object.get("directory").isString().stringValue());
                    organismInfo.setNumFeatures(0);
                    organismInfo.setNumTracks(0);
//                    GWT.log(object.toString());
                    organismInfoList.add(organismInfo);
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error loading organisms");
            }
        };
        loadOrganisms(requestCallback);
    }

    public static void updateOrganismInfo(final OrganismInfo organismInfo) {
        RestService.sendRequest("/organism/updateOrganismInfo", organismInfo.toJSON());
    }

    public static void changeOrganism(final MainPanel mainPanel, String newOrganismId) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
//                JSONValue returnValue = JSONParser.parseStrict(response.getText());
//                GWT.log("returned: " + returnValue);
                mainPanel.updateGenomicViewer();
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error changing organisms");
            }
        };
        String payload = "data={organismId:'"+newOrganismId+"'}";

        RestService.sendRequest(requestCallback,"/organism/changeOrganism", payload);

    }
}
