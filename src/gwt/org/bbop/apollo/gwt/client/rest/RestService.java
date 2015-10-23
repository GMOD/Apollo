package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.client.Annotator;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

/**
 * Created by Nathan Dunn on 1/14/15.
 */
public class RestService {

    public static void sendRequest(RequestCallback requestCallback,String url){
        sendRequest(requestCallback,url,(String) null);
    }


    public static void sendRequest(RequestCallback requestCallback,String url,JSONObject jsonObject){
        sendRequest(requestCallback,url,"data="+jsonObject.toString());
    }

    public static void sendRequest(RequestCallback requestCallback,String url,JSONArray jsonArray){
        sendRequest(requestCallback,url,"data="+jsonArray.toString());
    }

    public static void sendRequest(RequestCallback requestCallback,String url,String data){
        String rootUrl = Annotator.getRootUrl();
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
            Bootbox.alert(e.getMessage());
        }
    }

}
