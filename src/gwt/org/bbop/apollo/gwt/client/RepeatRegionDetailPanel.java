package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.bbop.apollo.gwt.client.event.AnnotationInfoChangeEvent;
import org.bbop.apollo.gwt.client.rest.AnnotationRestService;
import org.bbop.apollo.gwt.client.rest.RestService;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

/**
 * Created by Deepak on 4/28/15.
 */
public class RepeatRegionDetailPanel extends Composite {
    private AnnotationInfo internalAnnotationInfo;
   
    interface AnnotationDetailPanelUiBinder extends UiBinder<Widget, RepeatRegionDetailPanel> {
    }

    private static AnnotationDetailPanelUiBinder ourUiBinder = GWT.create(AnnotationDetailPanelUiBinder.class);

    @UiField
    TextBox nameField;
    @UiField
    TextBox descriptionField;
    @UiField
    TextBox locationField;
    @UiField
    TextBox sequenceField;
    @UiField
    TextBox userField;

    public RepeatRegionDetailPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }
    
    @UiHandler("nameField")
    void handleNameChange(ChangeEvent e) {
        String updatedName = nameField.getText();
        internalAnnotationInfo.setName(updatedName);
        updateEntity();
    }
    
    @UiHandler("descriptionField")
    void handleDescriptionChange(ChangeEvent e) {
        String updatedDescription = descriptionField.getText();
        internalAnnotationInfo.setDescription(updatedDescription);
        updateEntity();
    }
    
    private void enableFields(boolean enabled) {
        nameField.setEnabled(enabled);
        descriptionField.setEnabled(enabled);
    }
    
    private void updateEntity() {
        final AnnotationInfo updatedInfo = this.internalAnnotationInfo;
        enableFields(false);
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                GWT.log("successful update: " + returnValue);
                enableFields(true);
                Annotator.eventBus.fireEvent(new AnnotationInfoChangeEvent(updatedInfo, AnnotationInfoChangeEvent.Action.UPDATE));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error updating gene: " + exception);
                enableFields(true);
            }
        };
        RestService.sendRequest(requestCallback, "annotator/updateFeature/", AnnotationRestService.convertAnnotationInfoToJSONObject(this.internalAnnotationInfo));
    }
    
    public void updateData(AnnotationInfo annotationInfo) {
        GWT.log("Updating entity");
        this.internalAnnotationInfo = annotationInfo;
        nameField.setText(internalAnnotationInfo.getName());
        descriptionField.setText(internalAnnotationInfo.getDescription());
        sequenceField.setText(internalAnnotationInfo.getSequence());
        userField.setText(internalAnnotationInfo.getOwner());
        
        if (internalAnnotationInfo.getMin() != null) {
            String locationText = internalAnnotationInfo.getMin().toString();
            locationText += " - ";
            locationText += internalAnnotationInfo.getMax().toString();
            locationText += " strand(";
            locationText += internalAnnotationInfo.getStrand() > 0 ? "+" : "-";
            locationText += ")";
            locationField.setText(locationText);
            locationField.setVisible(true);
        }
        else {
            locationField.setVisible(false);
        }
        
        setVisible(true);
    }
    
    public void setEditable(boolean editable) {
        nameField.setEnabled(editable);
        descriptionField.setEnabled(editable);
    }
}