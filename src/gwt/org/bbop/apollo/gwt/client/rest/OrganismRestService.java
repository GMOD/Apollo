package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.*;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.client.Annotator;
import org.bbop.apollo.gwt.client.ErrorDialog;
import org.bbop.apollo.gwt.client.LoadingDialog;
import org.bbop.apollo.gwt.client.MainPanel;
import org.bbop.apollo.gwt.client.dto.AppInfoConverter;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.OrganismInfoConverter;
import org.bbop.apollo.gwt.client.event.OrganismChangeEvent;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 1/14/15.
 */
public class OrganismRestService {

    public static void loadOrganisms(RequestCallback requestCallback) {
        RestService.sendRequest(requestCallback, "organism/findAllOrganisms");
    }


    public static void loadOrganisms(final List<OrganismInfo> organismInfoList) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                organismInfoList.clear();
                organismInfoList.addAll(OrganismInfoConverter.convertJSONStringToOrganismInfoList(response.getText()));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error loading organisms");
            }
        };
        loadOrganisms(requestCallback);
    }

    public static void updateOrganismInfo(final OrganismInfo organismInfo,boolean forceReload) {
        final LoadingDialog loadingDialog = new LoadingDialog("Updating Organism Information");
        JSONObject organismInfoObject = organismInfo.toJSON();
        organismInfoObject.put("forceReload",JSONBoolean.getInstance(forceReload));



        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                loadingDialog.hide();
                JSONValue jsonValue = JSONParser.parseStrict(response.getText());
                if(jsonValue.isObject()!=null && jsonValue.isObject()!=null && jsonValue.isObject().containsKey(FeatureStringEnum.ERROR.getValue())){
                    String errorMessage = jsonValue.isObject().get(FeatureStringEnum.ERROR.getValue()).isString().stringValue();
                    ErrorDialog errorDialog = new ErrorDialog("Unable to update the organism",errorMessage,true,true);
                }
                else{
                    OrganismChangeEvent organismChangeEvent = new OrganismChangeEvent(OrganismChangeEvent.Action.LOADED_ORGANISMS);
                    List<OrganismInfo> organismInfoList  = OrganismInfoConverter.convertJSONStringToOrganismInfoList(response.getText());
                    organismChangeEvent.setOrganismInfoList(organismInfoList);
                    Annotator.eventBus.fireEvent(organismChangeEvent);
                }

            }

            @Override
            public void onError(Request request, Throwable exception) {
                loadingDialog.hide();
                Bootbox.alert("error updating organism info: "+exception);
            }
        };
        RestService.sendRequest(requestCallback, "organism/updateOrganismInfo", "data=" + organismInfoObject.toString());
    }


    public static void createOrganism(RequestCallback requestCallback, OrganismInfo organismInfo) {
        RestService.sendRequest(requestCallback,"organism/addOrganism", OrganismInfoConverter.convertOrganismInfoToJSONObject(organismInfo));
    }

    public static void deleteOrganism(RequestCallback requestCallback, OrganismInfo organismInfo) {
        RestService.sendRequest(requestCallback,"organism/deleteOrganism", OrganismInfoConverter.convertOrganismInfoToJSONObject(organismInfo));
    }

    public static void switchOrganismById(String newOrganismId) {
        final LoadingDialog loadingDialog = new LoadingDialog();

        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject returnValue = JSONParser.parseStrict(response.getText()).isObject();
                MainPanel.getInstance().setAppState(AppInfoConverter.convertFromJson(returnValue));
                loadingDialog.hide();
            }

            @Override
            public void onError(Request request, Throwable exception) {
                loadingDialog.hide();
                Bootbox.alert("Error changing organisms");
            }
        };

        RestService.sendRequest(requestCallback,"annotator/setCurrentOrganism/"+newOrganismId);
    }

    public static void switchSequenceById(String newSequenceId) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject returnValue = JSONParser.parseStrict(response.getText()).isObject();
                MainPanel.getInstance().setAppState(AppInfoConverter.convertFromJson(returnValue));

                OrganismChangeEvent organismChangeEvent = new OrganismChangeEvent(OrganismChangeEvent.Action.LOADED_ORGANISMS);
                Annotator.eventBus.fireEvent(organismChangeEvent);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error changing organisms: "+exception.getMessage());
            }
        };

        RestService.sendRequest(requestCallback,"annotator/setCurrentSequence/"+ newSequenceId);
    }
}
