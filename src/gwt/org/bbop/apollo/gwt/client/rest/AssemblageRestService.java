package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.Window;
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

    public static void saveAssemblage(AssemblageInfo assemblageInfo) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                GWT.log("Successfully saved assemblage");
//                resetPanel();
//                reload();
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Failed to save: " + exception.getMessage());
            }
        };
        saveAssemblage(requestCallback,assemblageInfo);
    }

    public static void saveAssemblage(RequestCallback requestCallback,AssemblageInfo assemblageInfo) {
        RestService.sendRequest(requestCallback, "assemblage/saveAssemblage", AssemblageInfoConverter.convertAssemblageInfoToJSONObject(assemblageInfo));
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

    public static void searchAssemblage(RequestCallback requestCallback, String searchString,String filterString) {
        String requestString = "assemblage/searchAssemblage/?searchQuery=" + searchString + "&filter="+filterString;
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
        addAssemblageAndReturn(requestCallback,assemblageInfo);
    }

    private static void updateForAssemblage(Response response){
        AssemblageInfo assemblageInfo = AssemblageInfoConverter.convertJSONObjectToAssemblageInfo(JSONParser.parseStrict(response.getText()).isObject());
        MainPanel.getInstance().setCurrentAssemblage(assemblageInfo);
        MainPanel.updateGenomicViewer(true,true);
    }

    public static void projectFeatures(JSONObject projectionCommand) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                AssemblageInfo assemblageInfo = AssemblageInfoConverter.convertJSONObjectToAssemblageInfo(JSONParser.parseStrict(response.getText()).isObject());

                MainPanel.getInstance().setCurrentAssemblageAndView(assemblageInfo);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert(exception.getMessage());
            }
        };
        RestService.sendRequest(requestCallback, "assemblage/projectFeatures", projectionCommand);
    }

    public static void foldTranscripts(JSONObject projectionCommand) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                updateForAssemblage(response);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert(exception.getMessage());
            }
        };
        RestService.sendRequest(requestCallback, "assemblage/foldTranscripts", projectionCommand);
    }

    public static void removeFolds(JSONObject projectionCommand) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                updateForAssemblage(response);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert(exception.getMessage());
            }
        };
        RestService.sendRequest(requestCallback, "assemblage/removeFolds", projectionCommand);
    }

    public static void foldBetweenExons(JSONObject projectionCommand) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                AssemblageInfo assemblageInfo = AssemblageInfoConverter.convertJSONObjectToAssemblageInfo(JSONParser.parseStrict(response.getText()).isObject());
                MainPanel.getInstance().setCurrentAssemblage(assemblageInfo);
                MainPanel.updateGenomicViewer(true,true);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert(exception.getMessage());
            }
        };
        RestService.sendRequest(requestCallback, "assemblage/foldBetweenExons", projectionCommand);
    }

}
