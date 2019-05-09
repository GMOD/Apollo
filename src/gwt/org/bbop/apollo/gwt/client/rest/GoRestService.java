package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.client.dto.GoAnnotationConverter;
import org.bbop.apollo.gwt.shared.go.GoAnnotation;

/**
 * Created by ndunn on 1/14/15.
 */
public class GoRestService {
    public static void saveGoAnnotation(RequestCallback requestCallback, GoAnnotation goAnnotation) {
        RestService.sendRequest(requestCallback, "goAnnotation/save", "data=" + GoAnnotationConverter.convertToJson(goAnnotation).toString());
    }

    public static void updateGoAnnotation(RequestCallback requestCallback, GoAnnotation goAnnotation) {
        RestService.sendRequest(requestCallback, "goAnnotation/update", "data=" + GoAnnotationConverter.convertToJson(goAnnotation).toString());
    }

    public static void deleteGoAnnotation(RequestCallback requestCallback, GoAnnotation goAnnotation) {
        RestService.sendRequest(requestCallback, "goAnnotation/delete", "data=" + GoAnnotationConverter.convertToJson(goAnnotation).toString());
    }

    public static void getGoAnnotation(RequestCallback requestCallback, String featureUniqueName) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("uniqueName",new JSONString(featureUniqueName));
        RestService.sendRequest(requestCallback, "goAnnotation/", "data=" + jsonObject.toString());
    }

//    public static void updateOrganismPermission(UserOrganismPermissionInfo object) {
//        RequestCallback requestCallback = new RequestCallback() {
//            @Override
//            public void onResponseReceived(Request request, Response response) {
//                GWT.log("success");
//            }
//
//            @Override
//            public void onError(Request request, Throwable exception) {
//                Bootbox.alert("Error updating permissions: " + exception);
//            }
//        };
//        RestService.sendRequest(requestCallback, "user/updateOrganismPermission", "data=" + object.toJSON().toString());
//    }


}
