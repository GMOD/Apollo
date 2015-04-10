package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.client.Annotator;
import org.bbop.apollo.gwt.client.ExportPanel;
import org.bbop.apollo.gwt.client.MainPanel;
import org.bbop.apollo.gwt.client.dto.ExportInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.event.SequenceLoadEvent;

import java.io.*;
import java.util.List;

/**
 * Created by ndunn on 1/14/15.
 */
public class SequenceRestService {

    public static void loadSequences(RequestCallback requestCallback,Long organismId){
        if(MainPanel.currentOrganismId==null){
            GWT.log("organism not set...returning");
            return ;
        }
        RestService.sendRequest(requestCallback,"/sequence/loadSequences/"+ organismId);
    }

    public static void loadSequences(final List<SequenceInfo> sequenceInfoList,Long organismId) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();
                sequenceInfoList.clear();

                for(int i = 0 ; i < array.size() ; i++){
                    JSONObject object = array.get(i).isObject();
                    SequenceInfo sequenceInfo = new SequenceInfo();
                    sequenceInfo.setId((long) object.get("id").isNumber().doubleValue());
                    sequenceInfo.setName(object.get("name").isString().stringValue());
                    sequenceInfo.setLength((int) object.get("length").isNumber().doubleValue());
                    sequenceInfo.setStart((int) object.get("start").isNumber().doubleValue());
                    sequenceInfo.setEnd((int) object.get("end").isNumber().doubleValue());
//                    GWT.log("get default: "+object.get("default"));
                    if(object.get("default")!=null){
//                        GWT.log("setting default to "+ sequenceInfo.getName());
                        sequenceInfo.setDefault(object.get("default").isBoolean().booleanValue());
                    }
                    sequenceInfoList.add(sequenceInfo);
                }
                SequenceLoadEvent contextSwitchEvent = new SequenceLoadEvent(SequenceLoadEvent.Action.FINISHED_LOADING);
                Annotator.eventBus.fireEvent(contextSwitchEvent);
                GWT.log("added # sequences: "+sequenceInfoList.size());

            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error loading organisms");
            }
        };
        if(MainPanel.currentOrganismId==null){
            GWT.log("organism not set...returning");
            sequenceInfoList.clear();
            return ;
        }
        loadSequences(requestCallback, organismId);
    }

    public static void setDefaultSequence(RequestCallback requestCallback,final String sequenceName) {
        RestService.sendRequest(requestCallback, "/sequence/setDefaultSequence/" + MainPanel.currentOrganismId + "?sequenceName=" + sequenceName);
    }

    public static void setDefaultSequence(final String sequenceName) {

        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                GWT.log("sequence: " + response.getText());
            }

            @Override
            public void onError(Request request, Throwable exception) {
                GWT.log("error setting sequence name: " + sequenceName);
            }
        };

        setDefaultSequence(requestCallback, sequenceName);
    }

    public static void generateLink(final ExportPanel exportPanel) {
        Dictionary dictionary = Dictionary.getDictionary("Options");
        final String rootUrl = dictionary.get("rootUrl");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type",new JSONString(exportPanel.getType()));
        JSONArray jsonArray = new JSONArray();
        for(SequenceInfo sequenceInfo : exportPanel.getSequenceList()){
            jsonArray.set(jsonArray.size(), sequenceInfo.toJSON());
        }
        jsonObject.put("sequences", jsonArray);
        GWT.log("GWTLAND: " + jsonObject.toString());
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                GWT.log("RESPONSE: " + response.getText());
                JSONObject responseObject = JSONParser.parseStrict(response.getText()).isObject();
                String filePath = responseObject.get("filePath").isString().stringValue();
                String exportType = responseObject.get("exportType").isString().stringValue();
                Window.open(rootUrl + "/sequence/exportHandler/?filePath=" + filePath + "&exportType=" + exportType, "_blank", "");
                exportPanel.enableCloseButton();
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("boo: "+exception);
            }
        };

        RestService.sendRequest(requestCallback, "/sequence/exportSequences/","data="+jsonObject.toString());
    }


}
