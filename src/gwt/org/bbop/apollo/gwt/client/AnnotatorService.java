package org.bbop.apollo.gwt.client;

import com.google.gwt.http.client.*;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;

/**
 * Created by Nathan Dunn on 1/9/15.
 */
public class AnnotatorService {

//    private String updateFeature(JSONObject internalData) {
//        String url = rootUrl + "/annotator/updateFeature";
//        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
//        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
//        StringBuilder sb = new StringBuilder();
//        sb.append("data="+internalData.toString());
////        sb.append("&key2=val2");
////        sb.append("&key3=val3");
//        builder.setRequestData(sb.toString());
//        enableFields(false);
//        RequestCallback requestCallback = new RequestCallback() {
//            @Override
//            public void onResponseReceived(Request request, Response response) {
//                JSONValue returnValue = JSONParser.parseStrict(response.getText());
////                Window.alert("successful update: "+returnValue);
//                enableFields(true);
//            }
//
//            @Override
//            public void onError(Request request, Throwable exception) {
//                Window.alert("Error updating gene: " + exception);
//                enableFields(true);
//            }
//        };
//        try {
//            builder.setCallback(requestCallback);
//            builder.send();
//        } catch (RequestException e) {
//            enableFields(true);
//            // Couldn't connect to server
//            Window.alert(e.getMessage());
//        }
//
//    }
}
