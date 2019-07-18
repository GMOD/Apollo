package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONObject;
import org.bbop.apollo.gwt.client.Annotator;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

/**
 * Created by ndunn on 1/14/15.
 */
public class RestService {

    public static void sendRequest(RequestCallback requestCallback, String url) {
        sendRequest(requestCallback, url, (String) null);
    }


    public static void sendRequest(RequestCallback requestCallback, String url, JSONObject jsonObject) {
        sendRequest(requestCallback, url, "data=" + jsonObject.toString());
    }

    public static void sendRequest(RequestCallback requestCallback, String url, String data) {
        sendRequest(requestCallback, url, data, RequestBuilder.POST);
    }

    public static String fixUrl(String url) {
        String rootUrl = Annotator.getRootUrl();
        if (!url.startsWith(rootUrl)) {
            url = rootUrl + url;
        }
        // add the clientToken parameter if not exists
        if (!url.contains(FeatureStringEnum.CLIENT_TOKEN.getValue())) {
            url += url.contains("?") ? "&" : "?";
            url += FeatureStringEnum.CLIENT_TOKEN.getValue();
            url += "=";
            url += Annotator.getClientToken();
        }
        return url;
    }

    public static void sendRequest(RequestCallback requestCallback, String url, String data, RequestBuilder.Method method) {
        url = fixUrl(url);
        RequestBuilder builder = generateBuilder(requestCallback,method, url, data);
    }

    public static void sendGetRequest(RequestCallback requestCallback, String url) {
        sendRequest(requestCallback, url, null, RequestBuilder.GET);
    }

    public static RequestBuilder generateBuilder(RequestCallback requestCallback, RequestBuilder.Method method, String url) {
        return generateBuilder(requestCallback,method,url,null);
    }

    public static RequestBuilder generateBuilder(RequestCallback requestCallback, RequestBuilder.Method method, String url, String data) {
        RequestBuilder builder = new RequestBuilder(method, URL.encode(url));
        if (data != null) {
            builder.setRequestData(data);
        }
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        builder.setHeader("Accept", "application/json");
        try {
            if (requestCallback != null) {
                builder.setCallback(requestCallback);
            }
            builder.send();
        } catch (RequestException e) {
            Bootbox.alert(e.getMessage());
        }
        return builder;
    }
}
