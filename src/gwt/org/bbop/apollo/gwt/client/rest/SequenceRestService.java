package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.client.Annotator;
import org.bbop.apollo.gwt.client.ExportPanel;
import org.bbop.apollo.gwt.client.SequencePanel;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

/**
 * Created by ndunn on 1/14/15.
 */
public class SequenceRestService {

    public static void setCurrentSequence(RequestCallback requestCallback, SequenceInfo sequenceInfo) {
        RestService.sendRequest(requestCallback, "sequence/setCurrentSequence/" + sequenceInfo.getId());
    }

    public static void setCurrentSequenceForString(RequestCallback requestCallback, String sequenceName, OrganismInfo organismInfo) {
        RestService.sendRequest(requestCallback, "sequence/setCurrentSequenceForNameAndOrganism/" +organismInfo.getId() +"?sequenceName="+sequenceName);
    }

    public static void generateLink(final ExportPanel exportPanel) {
        JSONObject jsonObject = new JSONObject();
        String type = exportPanel.getType();
        jsonObject.put("type", new JSONString(exportPanel.getType()));
        jsonObject.put("exportAllSequences", new JSONString(exportPanel.getExportAll().toString()));

        if (type.equals(FeatureStringEnum.TYPE_CHADO.getValue())) {
            jsonObject.put("chadoExportType", new JSONString(exportPanel.getChadoExportType()));
            jsonObject.put("seqType", new JSONString(""));
            jsonObject.put("exportGff3Fasta", new JSONString(""));
            jsonObject.put("output", new JSONString(""));
            jsonObject.put("format", new JSONString(""));
        }
        else if (type.equals(FeatureStringEnum.TYPE_VCF.getValue())) {
            GWT.log("type is TYPE_VCF");
            jsonObject.put("output", new JSONString("file"));
            jsonObject.put("format", new JSONString("gzip"));
            jsonObject.put("seqType", new JSONString(""));
            jsonObject.put("exportGff3Fasta", new JSONString(""));
            jsonObject.put("chadoExportType", new JSONString(""));
        }
        else {
            jsonObject.put("chadoExportType", new JSONString(""));
            jsonObject.put("seqType", new JSONString(exportPanel.getSequenceType()));
            jsonObject.put("exportGff3Fasta", new JSONString(exportPanel.getExportGff3Fasta().toString()));
            jsonObject.put("output", new JSONString("file"));
            jsonObject.put("format", new JSONString("gzip"));
        }

        JSONArray jsonArray = new JSONArray();
        for (SequenceInfo sequenceInfo : exportPanel.getSequenceList()) {
            jsonArray.set(jsonArray.size(), new JSONString(sequenceInfo.getName()));
        }
        jsonObject.put("sequences", jsonArray);
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject responseObject = JSONParser.parseStrict(response.getText()).isObject();
                GWT.log("Received response: "+responseObject.toString());
                String uuid = responseObject.get("uuid").isString().stringValue();
                String exportType = responseObject.get("exportType").isString().stringValue();
                String sequenceType = responseObject.get("seqType").isString().stringValue();
                String exportUrl = Annotator.getRootUrl() + "IOService/download?uuid=" + uuid + "&exportType=" + exportType + "&seqType=" + sequenceType+"&format=gzip";
                exportPanel.setExportUrl(exportUrl);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error: " + exception);
            }
        };

        RequestCallback requestCallbackForChadoExport = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject responseObject = JSONParser.parseStrict(response.getText()).isObject();
                exportPanel.showExportStatus(responseObject.toString());
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error: " + exception);
            }
        };

        if (type.equals(FeatureStringEnum.TYPE_CHADO.getValue())) {
            RestService.sendRequest(requestCallbackForChadoExport, "IOService/write", "data=" + jsonObject.toString());
        }
        else {
            RestService.sendRequest(requestCallback, "IOService/write", "data=" + jsonObject.toString());
        }
    }

    public static void setCurrentSequenceAndLocation(RequestCallback requestCallback, String sequenceNameString, Integer start, Integer end) {
        setCurrentSequenceAndLocation(requestCallback,sequenceNameString,start,end,false) ;
    }

    public static void setCurrentSequenceAndLocation(RequestCallback requestCallback, String sequenceNameString, Integer start, Integer end,boolean suppressOutput) {
        String url = "sequence/setCurrentSequenceLocation/?name=" + sequenceNameString + "&start=" + start + "&end=" + end;
        if(suppressOutput){
            url += "&suppressOutput=true";
        }

        RestService.sendRequest(requestCallback, url);
    }

    public static void getSequenceForOffsetAndMax(RequestCallback requestCallback, String text, int start, int length, String sortBy,Boolean sortNameAscending, String minFeatureLengthText, String maxFeatureLengthText) {
        String searchString = "sequence/getSequences/?name=" + text + "&start=" + start + "&length=" + length ;
        if(sortBy!=null && sortBy.length()>1){
            searchString += "&sort="+sortBy+"&asc=" + sortNameAscending;
        }
        try {
            searchString += "&minFeatureLength=" + Integer.parseInt(minFeatureLengthText);
        } catch (NumberFormatException nfe) {
            //
        }
        try {
            searchString += "&maxFeatureLength=" + Integer.parseInt(maxFeatureLengthText);
        } catch (NumberFormatException nfe) {
            //
        }
        RestService.sendRequest(requestCallback, searchString);
    }

    public static void getChadoExportStatus(final SequencePanel sequencePanel) {
        String requestUrl = Annotator.getRootUrl() + "IOService/chadoExportStatus";
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject responseObject = JSONParser.parseStrict(response.getText()).isObject();
                sequencePanel.setChadoExportStatus(responseObject.get("export_status").isString().stringValue());
            }

            @Override
            public void onError(Request request, Throwable exception) {
                sequencePanel.setChadoExportStatus("false");
            }
        };
        RestService.sendRequest(requestCallback, requestUrl);
    }

}
