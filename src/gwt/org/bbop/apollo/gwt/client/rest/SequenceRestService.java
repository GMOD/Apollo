package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;

import java.util.List;

/**
 * Created by ndunn on 1/14/15.
 */
public class SequenceRestService {

    public static void loadSequences(RequestCallback requestCallback){
        Dictionary dictionary = Dictionary.getDictionary("Options");
        String rootUrl = dictionary.get("rootUrl");
        String url = rootUrl+"/jbrowse/data/seq/refSeqs.json";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");

        try {
            builder.setCallback(requestCallback);
            builder.send();
        } catch (RequestException e) {
            Window.alert(e.getMessage());
        }
    }

    public static void loadSequences(final List<SequenceInfo> sequenceInfoList) {
        Dictionary dictionary = Dictionary.getDictionary("Options");
        String rootUrl = dictionary.get("rootUrl");
        GWT.log("root URL: " + rootUrl);
//        String url = "/apollo/organism/findAllOrganisms";
//        http://localhost:8080/apollo/jbrowse/data/seq/refSeqs.json
        String url = rootUrl+"/jbrowse/data/seq/refSeqs.json";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();
//                Window.alert("array size: "+array.size());

                for(int i = 0 ; i < array.size() ; i++){
                    JSONObject object = array.get(i).isObject();
//                    GWT.log(object.toString());
                    SequenceInfo sequenceInfo = new SequenceInfo();
                    sequenceInfo.setName(object.get("name").isString().stringValue());
                    sequenceInfo.setLength((int) object.get("length").isNumber().isNumber().doubleValue());
                    sequenceInfoList.add(sequenceInfo);
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error loading organisms");
            }
        };
        try {
            builder.setCallback(requestCallback);
            builder.send();
        } catch (RequestException e) {
            // Couldn't connect to server
            Window.alert(e.getMessage());
        }

    }
}
