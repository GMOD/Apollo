package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.client.Annotator;
import org.bbop.apollo.gwt.client.ErrorDialog;
import org.bbop.apollo.gwt.client.LoginDialog;
import org.bbop.apollo.gwt.client.dto.UserInfo;
import org.bbop.apollo.gwt.client.dto.UserInfoConverter;
import org.bbop.apollo.gwt.client.dto.UserOrganismPermissionInfo;
import org.bbop.apollo.gwt.client.event.UserChangeEvent;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.bootbox.client.callback.ConfirmCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 1/14/15.
 */
public class GoRestService {


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
