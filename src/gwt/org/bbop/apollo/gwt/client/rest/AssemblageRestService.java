package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import org.bbop.apollo.gwt.client.MainPanel;
import org.bbop.apollo.gwt.client.dto.assemblage.AssemblageInfo;
import org.bbop.apollo.gwt.client.dto.assemblage.AssemblageInfoConverter;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;


/**
 * This class stores Boorkmars
 * Created by nathandunn on 10/1/15.
 */
public class AssemblageRestService {

    public static void loadAssemblage(RequestCallback requestCallback) {
        RestService.sendRequest(requestCallback, "assemblage/list");
    }

    public static void addAssemblage(RequestCallback requestCallback, AssemblageInfo... assemblageInfoCollection) {
        RestService.sendRequest(requestCallback, "assemblage/addAssemblage", AssemblageInfoConverter.convertAssemblageInfoToJSONArray(assemblageInfoCollection));
    }

    public static void addAssemblageAndReturn(RequestCallback requestCallback, AssemblageInfo... assemblageInfoCollection) {
        RestService.sendRequest(requestCallback, "assemblage/addAssemblageAndReturn", AssemblageInfoConverter.convertAssemblageInfoToJSONArray(assemblageInfoCollection));
    }

    public static void removeAssemblage(RequestCallback requestCallback, AssemblageInfo... selectedSet) {
        JSONArray removeArray = new JSONArray();
        JSONArray idList = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(FeatureStringEnum.ID.getValue(),idList);
        for(AssemblageInfo assemblageInfo : selectedSet){
            idList.set(idList.size(),new JSONNumber(assemblageInfo.getId()));
        }
        removeArray.set(0,jsonObject);
        RestService.sendRequest(requestCallback, "assemblage/deleteAssemblage",removeArray);
    }

    public static void getAssemblage(RequestCallback requestCallback, AssemblageInfo assemblageInfo) {
        RestService.sendRequest(requestCallback, "assemblage/getAssemblage", AssemblageInfoConverter.convertAssemblageInfoToJSONObject(assemblageInfo));
    }

    public static void searchAssemblage(RequestCallback requestCallback, String searchString) {
        String requestString = "assemblage/searchAssemblage/?searchQuery=" + searchString;
        RestService.sendRequest(requestCallback, requestString);
    }


    public static void addAssemblageAndView(final AssemblageInfo assemblageInfo){
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                MainPanel.getInstance().setCurrentAssemblageAndView(AssemblageInfoConverter.convertJSONObjectToAssemblageInfo(JSONParser.parseStrict(response.getText()).isObject()));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert(exception.getMessage());
            }
        };
        AssemblageRestService.addAssemblageAndReturn(requestCallback,assemblageInfo);
    }
}
