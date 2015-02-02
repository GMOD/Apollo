package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.event.AnnotationInfoChangeEvent;
import org.bbop.apollo.gwt.client.rest.AnnotationRestService;
import org.bbop.apollo.gwt.shared.SharedStuff;
import org.gwtbootstrap3.client.ui.*;

/**
 * Created by ndunn on 1/9/15.
 */
public class GeneDetailPanel extends Composite {

    private AnnotationInfo internalAnnotationInfo;

    interface AnnotationDetailPanelUiBinder extends UiBinder<Widget, GeneDetailPanel> {
    }

    Dictionary dictionary = Dictionary.getDictionary("Options");
    String rootUrl = dictionary.get("rootUrl");

    private static AnnotationDetailPanelUiBinder ourUiBinder = GWT.create(AnnotationDetailPanelUiBinder.class);
    @UiField
    org.gwtbootstrap3.client.ui.TextBox nameField;
    @UiField
    org.gwtbootstrap3.client.ui.TextBox symbolField;
    @UiField
    org.gwtbootstrap3.client.ui.TextBox descriptionField;
    @UiField
    InputGroupAddon locationField;

//    private SharedStuff sharedStuff = new SharedStuff();
//    private JSONObject internalData ;

    public GeneDetailPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
//        Window.alert(sharedStuff.getSomthing());
    }

    @UiHandler("nameField")
    void handleNameChange(ChangeEvent e) {
        String updatedName = nameField.getText();
        internalAnnotationInfo.setName(updatedName);
        updateGene();
    }

    @UiHandler("symbolField")
    void handleSymbolChange(ChangeEvent e) {
//        Window.alert("symbol field changed: "+e);
        String updatedName = symbolField.getText();
        internalAnnotationInfo.setSymbol(updatedName);
//        internalData.put("symbol", new JSONString(updatedName));
        updateGene();
    }

    @UiHandler("descriptionField")
    void handleDescriptionChange(ChangeEvent e) {
//        Window.alert("symbol field changed: "+e);
        String updatedName = descriptionField.getText();
        internalAnnotationInfo.setDescription(updatedName);
//        internalData.put("description", new JSONString(updatedName));
        updateGene();
    }

    private void enableFields(boolean enabled) {
        nameField.setEnabled(enabled);
        symbolField.setEnabled(enabled);
        descriptionField.setEnabled(enabled);
    }


    private void updateGene() {
        String url = rootUrl + "/annotator/updateFeature";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
        sb.append("data=" + AnnotationRestService.convertAnnotationInfoToJSONObject(this.internalAnnotationInfo).toString());
        final AnnotationInfo updatedInfo = this.internalAnnotationInfo ;
        builder.setRequestData(sb.toString());
        enableFields(false);
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                GWT.log("f");
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                GWT.log("successful update: " + returnValue);
                enableFields(true);
                Annotator.eventBus.fireEvent(new AnnotationInfoChangeEvent(updatedInfo, AnnotationInfoChangeEvent.Action.UPDATE));
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

    /**
     * {"date_creation":1420750302883, "symbol":"sdf", "location":{"fmin":14836, "strand":-1, "fmax":15043}, "description":"adsf", "name":"GB50347-RAaa", "children":[{"date_creation":1420750302872, "symbol":"sdf", "location":{"fmin":14836, "strand":-1, "fmax":15043}, "description":"sdf", "parent_type":{"name":"gene", "cv":{"name":"sequence"}}, "name":"GB50347-RA-00001asdf", "children":[{"date_creation":1420750302852, "location":{"fmin":14836, "strand":-1, "fmax":15043}, "parent_type":{"name":"mRNA", "cv":{"name":"sequence"}}, "name":"ac106657-8872-4c16-85f6-db0da33b4248", "uniquename":"ac106657-8872-4c16-85f6-db0da33b4248", "type":{"name":"exon", "cv":{"name":"sequence"}}, "date_last_modified":1420750302957, "parent_id":"8a6c6037-9878-4b2e-9bb7-fe090e24c24b"}], "properties":[{"value":"sdf", "type":{"cv":{"name":"feature_property"}}},{"value":"sdf", "type":{"cv":{"name":"feature_property"}}}], "uniquename":"8a6c6037-9878-4b2e-9bb7-fe090e24c24b", "type":{"name":"mRNA", "cv":{"name":"sequence"}}, "date_last_modified":1420754201629, "parent_id":"c8288815-c476-41da-a4d0-f13f940acff5"}], "properties":[{"value":"sdf", "type":{"cv":{"name":"feature_property"}}},{"value":"adsf", "type":{"cv":{"name":"feature_property"}}}], "uniquename":"c8288815-c476-41da-a4d0-f13f940acff5", "type":{"name":"gene", "cv":{"name":"sequence"}}, "date_last_modified":1420750327299}
     */
    public void updateData(AnnotationInfo annotationInfo) {
        GWT.log("upodating gene pagen");
        this.internalAnnotationInfo = annotationInfo;
        GWT.log("A");
        nameField.setText(internalAnnotationInfo.getName());
        GWT.log("B");
        symbolField.setText(internalAnnotationInfo.getSymbol());
        descriptionField.setText(internalAnnotationInfo.getDescription());

//        if(this.internalData.get("name") instanceof JSONString){
//            nameField.setText(internalData.get("name").isString().stringValue());
//        }
//        if(this.internalData.get("symbol") !=null ) {
//            symbolField.setText(internalData.get("symbol").isString().stringValue());
//        }
//        if(this.internalData.get("description") != null ) {
//            descriptionField.setText(internalData.get("description").isString().stringValue());
//        }

//        JSONObject locationObject = internalData.get("location").isObject();
        if (internalAnnotationInfo.getMin() != null) {
            GWT.log("C");
            String locationText = internalAnnotationInfo.getMin().toString();
            locationText += " - ";
            locationText += internalAnnotationInfo.getMax().toString();
            locationText += " strand(";
            locationText += internalAnnotationInfo.getStrand() > 0 ? "+" : "-";
            locationText += ")";
            locationField.setText(locationText);
            locationField.setVisible(true);
            GWT.log("D");
        }
        else{
            GWT.log("E");
            locationField.setVisible(false);
        }


        setVisible(true);
    }
}