package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;

/**
 * Created by ndunn on 1/14/15.
 */
public class RestService {

    public static void sendRequest(RequestCallback requestCallback,String url){
        Dictionary dictionary = Dictionary.getDictionary("Options");
        String rootUrl = dictionary.get("rootUrl");
        if(!url.startsWith(rootUrl)){
            url = rootUrl+url;
        }
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        try {
            builder.setCallback(requestCallback);
            builder.send();
        } catch (RequestException e) {
            Window.alert(e.getMessage());
        }

    }
}
