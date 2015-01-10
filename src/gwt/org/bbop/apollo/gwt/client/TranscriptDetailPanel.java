package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import org.gwtbootstrap3.client.ui.InputGroupAddon;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

/**
 * Created by ndunn on 1/9/15.
 */
public class TranscriptDetailPanel extends Composite {

    interface AnnotationDetailPanelUiBinder extends UiBinder<Widget, TranscriptDetailPanel> { }

    Dictionary dictionary = Dictionary.getDictionary("Options");
    String rootUrl = dictionary.get("rootUrl");
    private JSONObject internalData ;

    private static AnnotationDetailPanelUiBinder ourUiBinder = GWT.create(AnnotationDetailPanelUiBinder.class);

    @UiField
    org.gwtbootstrap3.client.ui.TextBox nameField;
    @UiField
    org.gwtbootstrap3.client.ui.TextBox symbolField;
    @UiField
    org.gwtbootstrap3.client.ui.TextBox descriptionField;
    @UiField
    InputGroupAddon locationField;

    @UiHandler("nameField")
    void handleNameChange(ChangeEvent e) {
//        Window.alert("changed: "+e);
        GWT.log("changing transcript name");
        String updatedName = nameField.getText();
        internalData.put("name", new JSONString(updatedName));
        updateTranscript(internalData);
    }

    @UiHandler("symbolField")
    void handleSymbolChange(ChangeEvent e) {
//        Window.alert("symbol field changed: "+e);
        String updatedName = symbolField.getText();
        internalData.put("symbol", new JSONString(updatedName));
        updateTranscript(internalData);
    }

    @UiHandler("descriptionField")
    void handleDescriptionChange(ChangeEvent e) {
//        Window.alert("symbol field changed: "+e);
        String updatedName = descriptionField.getText();
        internalData.put("description", new JSONString(updatedName));
        updateTranscript(internalData);
    }

    public TranscriptDetailPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    private void enableFields(boolean enabled){
        nameField.setEnabled(enabled);
        symbolField.setEnabled(enabled);
        descriptionField.setEnabled(enabled);
    }

    public void updateData(JSONObject internalData) {
        this.internalData = internalData ;
        GWT.log("updating transcript detail panel");
        GWT.log(internalData.toString());
        nameField.setText(internalData.get("name").isString().stringValue());
        symbolField.setText(internalData.containsKey("symbol") ? internalData.get("symbol").isString().stringValue(): "");
        descriptionField.setText(internalData.containsKey("description") ? internalData.get("description").isString().stringValue() : "");

        JSONObject locationObject = internalData.get("location").isObject();
        String locationText = locationObject.get("fmin").isNumber().toString();
        locationText += " - ";
        locationText += locationObject.get("fmax").isNumber().toString();
        locationText += " strand(";
        locationText += locationObject.get("strand").isNumber().doubleValue() > 0 ? "+" : "-";
        locationText += ")";

        locationField.setText(locationText);

        setVisible(true);
    }

    private void updateTranscript(JSONObject internalData) {
        String url = rootUrl + "/annotator/updateFeature";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
        sb.append("data="+internalData.toString());
        builder.setRequestData(sb.toString());
        enableFields(false);
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
//                Window.alert("successful update: "+returnValue);
                enableFields(true);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error updating gene: " + exception);
                enableFields(true);
            }
        };
        try {
            builder.setCallback(requestCallback);
            builder.send();
        } catch (RequestException e) {
            enableFields(true);
            // Couldn't connect to server
            Window.alert(e.getMessage());
        }

    }
}