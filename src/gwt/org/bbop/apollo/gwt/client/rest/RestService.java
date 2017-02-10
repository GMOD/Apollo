package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.JSONObject;
import org.bbop.apollo.gwt.client.Annotator;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

/**
 * Created by ndunn on 1/14/15.
 */
public class RestService {

    public static void sendRequest(RequestCallback requestCallback,String url){
        sendRequest(requestCallback,url,(String) null);
    }


    public static void sendRequest(RequestCallback requestCallback,String url,JSONObject jsonObject){
        sendRequest(requestCallback,url,"data="+jsonObject.toString());
    }

    public static void sendRequest(RequestCallback requestCallback,String url,String data){
        String rootUrl = Annotator.getRootUrl();
        if(!url.startsWith(rootUrl)){
            url = rootUrl+url;
        }
        // add the clientToken parameter if not exists
        if(!url.contains(FeatureStringEnum.CLIENT_TOKEN.getValue())){
            url += url.contains("?") ? "&" : "?";
            url += FeatureStringEnum.CLIENT_TOKEN.getValue();
            url += "=";
            url += Annotator.getClientToken();
        }
        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
        if(data!=null){
            builder.setRequestData(data);
        }
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        String remoteUserString = Dictionary.getDictionary("Options").get(FeatureStringEnum.REMOTE_USER.getValue());
        if(remoteUserString!=null && remoteUserString.trim().length()>0){
            builder.setHeader(FeatureStringEnum.REMOTE_USER.getValue(), remoteUserString);
        }
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
