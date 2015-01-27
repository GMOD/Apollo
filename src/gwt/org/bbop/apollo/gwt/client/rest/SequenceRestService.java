package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.client.Annotator;
import org.bbop.apollo.gwt.client.MainPanel;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.event.SequenceLoadEvent;

import java.util.List;

/**
 * Created by ndunn on 1/14/15.
 */
public class SequenceRestService {

    public static void loadSequences(RequestCallback requestCallback,Long organismId){
        if(MainPanel.currentOrganismId==null){
            GWT.log("organism not set . . returrning ");
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
//                    GWT.log(object.toString());
                    SequenceInfo sequenceInfo = new SequenceInfo();
                    sequenceInfo.setName(object.get("name").isString().stringValue());
                    sequenceInfo.setLength((int) object.get("length").isNumber().isNumber().doubleValue());
                    if(object.get("default")!=null){
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
            GWT.log("organism not set . . returrning ");
            sequenceInfoList.clear();
            return ;
        }
        loadSequences(requestCallback,organismId);
    }

    public static void setDefaultSequence(final String sequenceName) {

        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                GWT.log("sequence: "+response.getText());
            }

            @Override
            public void onError(Request request, Throwable exception) {
                GWT.log("error setting sequence name: "+sequenceName);
            }
        };

        RestService.sendRequest(requestCallback,"/sequence/setDefaultSequence/"+ MainPanel.currentOrganismId+"?sequenceName="+ sequenceName);
    }
}
