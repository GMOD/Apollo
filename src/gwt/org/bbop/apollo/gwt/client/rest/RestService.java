package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;

/**
 * Created by ndunn on 1/14/15.
 */
public class RestService {

    public static void sendRequest(String url,String data){
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                GWT.log("response received: "+response.getText());
            }

            @Override
            public void onError(Request request, Throwable exception) {
                GWT.log("error on request: "+exception);
            }
        };
        sendRequest(requestCallback,url,data);
    }

    public static void sendRequest(RequestCallback requestCallback,String url){
        sendRequest(requestCallback,url,null);
    }

    public static void sendRequest(RequestCallback requestCallback,String url,String data){
        Dictionary dictionary = Dictionary.getDictionary("Options");
        String rootUrl = dictionary.get("rootUrl");
        if(!url.startsWith(rootUrl)){
            url = rootUrl+url;
        }
        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
        if(data!=null){
            builder.setRequestData(data);
        }
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        try {
            if(requestCallback!=null){
                builder.setCallback(requestCallback);
            }
            builder.send();
        } catch (RequestException e) {
            Window.alert(e.getMessage());
        }
    }

}
