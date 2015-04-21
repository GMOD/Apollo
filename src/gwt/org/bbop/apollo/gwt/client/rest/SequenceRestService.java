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
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.event.SequenceLoadEvent;

import java.io.*;
import java.util.List;

/**
 * Created by ndunn on 1/14/15.
 */
public class SequenceRestService {

    public static void setCurrentSequence(RequestCallback requestCallback,SequenceInfo sequenceInfo) {
        RestService.sendRequest(requestCallback, "sequence/setCurrentSequence/"+sequenceInfo.getId());
    }

    public static void generateLink(final ExportPanel exportPanel) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type",new JSONString(exportPanel.getType()));
        jsonObject.put("sequenceType",new JSONString(exportPanel.getSequenceType()));
        jsonObject.put("exportAllSequences", new JSONString(exportPanel.getExportAll().toString()));
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
                String sequenceType = responseObject.get("sequenceType").isString().stringValue();
                String exportUrl =  Annotator.getRootUrl() + "sequence/exportHandler/?filePath=" + filePath + "&exportType=" + exportType + "&sequenceType=" + sequenceType ;
                exportPanel.setExportUrl(exportUrl);
//                Window.open(rootUrl + "/sequence/exportHandler/?filePath=" + filePath + "&exportType=" + exportType + "&sequenceType=" + sequenceType, "_blank", "");
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("boo: "+exception);
            }
        };

        RestService.sendRequest(requestCallback, "sequence/exportSequences/","data="+jsonObject.toString());
    }


}
