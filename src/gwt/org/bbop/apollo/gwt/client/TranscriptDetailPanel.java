package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.http.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.AnnotationInfoConverter;
import org.bbop.apollo.gwt.client.event.AnnotationInfoChangeEvent;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

/**
 * Created by Nathan Dunn on 1/9/15.
 */
public class TranscriptDetailPanel extends Composite {

    private AnnotationInfo internalAnnotationInfo;


    interface AnnotationDetailPanelUiBinder extends UiBinder<Widget, TranscriptDetailPanel> { }

    private static AnnotationDetailPanelUiBinder ourUiBinder = GWT.create(AnnotationDetailPanelUiBinder.class);

    @UiField
    TextBox nameField;
    @UiField
    TextBox descriptionField;
    @UiField
    TextBox locationField;
    @UiField
    TextBox userField;
    @UiField
    TextBox sequenceField;

    private Boolean editable = false ;

    public TranscriptDetailPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }


    @UiHandler("nameField")
    void handleNameChange(ChangeEvent e) {
        internalAnnotationInfo.setName(nameField.getText());
        updateTranscript();
    }


    @UiHandler("descriptionField")
    void handleDescriptionChange(ChangeEvent e) {
        internalAnnotationInfo.setDescription(descriptionField.getText());
        updateTranscript();
    }



    public void updateData(AnnotationInfo annotationInfo) {
        this.internalAnnotationInfo = annotationInfo ;
        nameField.setText(internalAnnotationInfo.getName());
        descriptionField.setText(internalAnnotationInfo.getDescription());
        userField.setText(internalAnnotationInfo.getOwner());
        sequenceField.setText(internalAnnotationInfo.getSequence());

        if (internalAnnotationInfo.getMin() != null) {
            String locationText = Integer.toString(internalAnnotationInfo.getMin() + 1);
            locationText += " - ";
            locationText += internalAnnotationInfo.getMax().toString();
            locationText += " strand(";
            locationText += internalAnnotationInfo.getStrand() > 0 ? "+" : "-";
            locationText += ")";
            locationField.setText(locationText);
            locationField.setVisible(true);
        }
        else{
            locationField.setVisible(false);
        }

        setVisible(true);
    }

    private void updateTranscript() {
        String url = Annotator.getRootUrl() + "annotator/updateFeature";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
        sb.append("data=" + AnnotationInfoConverter.convertAnnotationInfoToJSONObject(this.internalAnnotationInfo).toString());
        final AnnotationInfo updatedInfo = this.internalAnnotationInfo ;
        builder.setRequestData(sb.toString());
        enableFields(false);
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                Annotator.eventBus.fireEvent(new AnnotationInfoChangeEvent(updatedInfo, AnnotationInfoChangeEvent.Action.UPDATE));
                enableFields(true);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error updating transcript: " + exception);
                enableFields(true);
            }
        };
        try {
            builder.setCallback(requestCallback);
            builder.send();
        } catch (RequestException e) {
            enableFields(true);
            // Couldn't connect to server
            Bootbox.alert(e.getMessage());
        }

    }

    private void enableFields(boolean enabled){
        nameField.setEnabled(enabled && editable);
        descriptionField.setEnabled(enabled && editable);
    }

    public void setEditable(boolean editable) {
        this.editable = editable ;
        nameField.setEnabled(this.editable);
        descriptionField.setEnabled(this.editable);
    }
}