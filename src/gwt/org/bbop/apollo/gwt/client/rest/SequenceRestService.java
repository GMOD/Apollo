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
        RestService.sendRequest(requestCallback,"/jbrowse/data/seq/refSeqs.json");
    }

    public static void loadSequences(final List<SequenceInfo> sequenceInfoList) {
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
        loadSequences(requestCallback);
    }
}
