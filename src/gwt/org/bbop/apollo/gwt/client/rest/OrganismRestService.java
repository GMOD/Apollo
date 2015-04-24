package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.*;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.client.Annotator;
import org.bbop.apollo.gwt.client.MainPanel;
import org.bbop.apollo.gwt.client.dto.AppInfoConverter;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.OrganismInfoConverter;
import org.bbop.apollo.gwt.client.event.OrganismChangeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 1/14/15.
 */
public class OrganismRestService {

    public static void loadOrganisms(RequestCallback requestCallback) {
        RestService.sendRequest(requestCallback, "organism/findAllOrganisms");
    }


    public static void loadOrganisms(final List<OrganismInfo> organismInfoList) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                organismInfoList.clear();
                organismInfoList.addAll(OrganismInfoConverter.convertJSONStringToOrganismInfoList(response.getText()));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error loading organisms");
            }
        };
        loadOrganisms(requestCallback);
    }

    public static void updateOrganismInfo(final OrganismInfo organismInfo,boolean forceReload) {
        JSONObject organismInfoObject = organismInfo.toJSON();
        organismInfoObject.put("forceReload",JSONBoolean.getInstance(forceReload));

        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                OrganismChangeEvent organismChangeEvent = new OrganismChangeEvent(OrganismChangeEvent.Action.CHANGED_ORGANISM);
                List<OrganismInfo> organismInfoList  = OrganismInfoConverter.convertJSONStringToOrganismInfoList(response.getText());
                organismChangeEvent.setOrganismInfoList(organismInfoList);
                Annotator.eventBus.fireEvent(organismChangeEvent);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("error updating organism info: "+exception);
            }
        };
        RestService.sendRequest(requestCallback, "organism/updateOrganismInfo", "data=" + organismInfoObject.toString());
    }

    public static void changeOrganism(String newOrganismId) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject returnValue = JSONParser.parseStrict(response.getText()).isObject();
                OrganismInfo organismInfo = OrganismInfoConverter.convertFromJson(returnValue);

                OrganismChangeEvent organismChangeEvent = new OrganismChangeEvent(OrganismChangeEvent.Action.CHANGED_ORGANISM);
                List<OrganismInfo> organismInfoList = new ArrayList<OrganismInfo>();
                organismInfoList.add(organismInfo);
                organismChangeEvent.setOrganismInfoList(organismInfoList);
                Annotator.eventBus.fireEvent(organismChangeEvent);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error changing organisms");
            }
        };
        String payload = "data={organismId:'"+newOrganismId+"'}";

        RestService.sendRequest(requestCallback, "organism/changeOrganism", payload);

    }

    public static void createOrganism(RequestCallback requestCallback, OrganismInfo organismInfo) {
        RestService.sendRequest(requestCallback,"organism/saveOrganism", OrganismInfoConverter.convertOrganismInfoToJSONObject(organismInfo));
    }

    public static void deleteOrganism(RequestCallback requestCallback, OrganismInfo organismInfo) {
        RestService.sendRequest(requestCallback,"organism/deleteOrganism", OrganismInfoConverter.convertOrganismInfoToJSONObject(organismInfo));
    }

    public static void switchOrganismById(String newOrganismId) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject returnValue = JSONParser.parseStrict(response.getText()).isObject();
                MainPanel.getInstance().setAppState(AppInfoConverter.convertFromJson(returnValue));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error changing organisms");
            }
        };

        RestService.sendRequest(requestCallback,"annotator/setCurrentOrganism/"+newOrganismId);
    }

    public static void switchSequenceById(String newSequenceId) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject returnValue = JSONParser.parseStrict(response.getText()).isObject();
//                Window.alert(returnValue.toString());
                MainPanel.getInstance().setAppState(AppInfoConverter.convertFromJson(returnValue));

//                OrganismInfo organismInfo = OrganismInfoConverter.convertFromJson(returnValue);
//                MainPanel.getInstance().setCurrentOrganism(organismInfo);

                OrganismChangeEvent organismChangeEvent = new OrganismChangeEvent(OrganismChangeEvent.Action.LOADED_ORGANISMS);
//                List<OrganismInfo> organismInfoList = new ArrayList<>();
//                organismInfoList.add(organismInfo);
//                organismChangeEvent.setOrganismInfoList(organismInfoList);
                Annotator.eventBus.fireEvent(organismChangeEvent);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error changing organisms");
            }
        };

        RestService.sendRequest(requestCallback,"annotator/setCurrentSequence/"+ newSequenceId);
    }
}
