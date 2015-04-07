package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.view.client.SelectionChangeEvent;
import org.bbop.apollo.gwt.client.Annotator;
import org.bbop.apollo.gwt.client.MainPanel;
import org.bbop.apollo.gwt.client.OrganismPanel;
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
        RestService.sendRequest(requestCallback, "/organism/findAllOrganisms");
    }

    public static JSONObject convertOrganismInfoToJSONObject(OrganismInfo organismInfo){
        JSONObject object = new JSONObject();
        if(organismInfo.getId()!=null){
            object.put("id",new JSONString(organismInfo.getId()));
        }
        object.put("commonName",new JSONString(organismInfo.getName()));
        object.put("directory",new JSONString(organismInfo.getDirectory()));
        object.put("genus",new JSONString(organismInfo.getGenus()));
        object.put("species",new JSONString(organismInfo.getSpecies()));
        if(organismInfo.getNumSequences()!=null){
            object.put("sequences",new JSONNumber(organismInfo.getNumFeatures()));
        }
        if(organismInfo.getNumFeatures()!=null) {
            object.put("annotationCount", new JSONNumber(organismInfo.getNumFeatures()));
        }
        return object;
    }

    public static List<OrganismInfo> convertJSONStringToOrganismInfoList(String jsonString){
        JSONValue returnValue = JSONParser.parseStrict(jsonString);
        List<OrganismInfo> organismInfoList = new ArrayList<>();
        JSONArray array = returnValue.isArray();
        for (int i = 0; i < array.size(); i++) {
            JSONObject object = array.get(i).isObject();
            OrganismInfo organismInfo = new OrganismInfo();
            organismInfo.setId(object.get("id").isNumber().toString());
            organismInfo.setName(object.get("commonName").isString().stringValue());
            if(object.get("sequences")!=null){
                organismInfo.setNumSequences((int) Math.round(object.get("sequences").isNumber().doubleValue()));
            }
            else{
                organismInfo.setNumSequences(0);
            }
            if(object.get("annotationCount")!=null){
                organismInfo.setNumFeatures((int) Math.round(object.get("annotationCount").isNumber().doubleValue()));
            }
            else{
                organismInfo.setNumFeatures(0);
            }
            organismInfo.setDirectory(object.get("directory").isString().stringValue());
            if(object.get("valid")!=null){
                organismInfo.setValid(object.get("valid").isBoolean().booleanValue());
            }
            if(object.get("genus")!=null && object.get("genus").isString()!=null){
                organismInfo.setGenus(object.get("genus").isString().stringValue());
            }
            if(object.get("species")!=null && object.get("species").isString()!=null){
                organismInfo.setSpecies(object.get("species").isString().stringValue());
            }
            organismInfoList.add(organismInfo);
        }
        return organismInfoList ;
    }

    public static void loadOrganisms(final List<OrganismInfo> organismInfoList) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                organismInfoList.clear();
                organismInfoList.addAll(convertJSONStringToOrganismInfoList(response.getText()));
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
                List<OrganismInfo> organismInfoList  = convertJSONStringToOrganismInfoList(response.getText());
                Annotator.eventBus.fireEvent(new OrganismChangeEvent(organismInfoList));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("error updating organism info: "+exception);
            }
        };
        RestService.sendRequest(requestCallback, "/organism/updateOrganismInfo", "data=" + organismInfoObject.toString());
    }

    public static void changeOrganism(String newOrganismId) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject returnValue = JSONParser.parseStrict(response.getText()).isObject();
                OrganismInfo organismInfo = OrganismInfoConverter.convertFromJson(returnValue);

                OrganismChangeEvent organismChangeEvent = new OrganismChangeEvent(OrganismChangeEvent.Action.CHANGED_ORGANISM);
                List<OrganismInfo> organismInfoList = new ArrayList<>();
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

        RestService.sendRequest(requestCallback,"/organism/changeOrganism", payload);

    }

    public static void createOrganism(RequestCallback requestCallback, OrganismInfo organismInfo) {
        String payload = "data="+convertOrganismInfoToJSONObject(organismInfo);
        RestService.sendRequest(requestCallback,"/organism/saveOrganism", payload);
    }

    public static void deleteOrganism(RequestCallback requestCallback, OrganismInfo organismInfo) {
        RestService.sendRequest(requestCallback,"/organism/deleteOrganism", convertOrganismInfoToJSONObject(organismInfo));
    }
}
