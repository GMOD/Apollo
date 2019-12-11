package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.event.AnnotationInfoChangeEvent;
import org.bbop.apollo.gwt.client.rest.AnnotationRestService;
import org.bbop.apollo.gwt.client.rest.AvailableStatusRestService;
import org.bbop.apollo.gwt.client.rest.RestService;
import org.gwtbootstrap3.client.ui.InputGroupAddon;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

/**
 * Created by ndunn on 1/9/15.
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
    @UiField
    TextBox dateCreatedField;
    @UiField
    TextBox lastUpdatedField;
    @UiField
    TextBox synonymsField;
    @UiField
    TextBox typeField;
    @UiField
    ListBox statusListBox;
    @UiField
    InputGroupAddon statusLabelField;

    private Boolean editable = false;

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

    @UiHandler("statusListBox")
    void handleStatusLabelFieldChange(ChangeEvent e) {
        String updatedStatus = statusListBox.getSelectedValue();
        internalAnnotationInfo.setStatus(updatedStatus);
        updateTranscript();
    }

    public void updateData(AnnotationInfo annotationInfo) {
        this.internalAnnotationInfo = annotationInfo;
        nameField.setText(internalAnnotationInfo.getName());
        descriptionField.setText(internalAnnotationInfo.getDescription());
        userField.setText(internalAnnotationInfo.getOwner());
        typeField.setText(internalAnnotationInfo.getType());
        sequenceField.setText(internalAnnotationInfo.getSequence());
        dateCreatedField.setText(DateFormatService.formatTimeAndDate(internalAnnotationInfo.getDateCreated()));
        lastUpdatedField.setText(DateFormatService.formatTimeAndDate(internalAnnotationInfo.getDateLastModified()));

        if (internalAnnotationInfo.getMin() != null) {
            String locationText = Integer.toString(internalAnnotationInfo.getMin());
            locationText += " - ";
            locationText += internalAnnotationInfo.getMax().toString();
            locationText += " strand(";
            locationText += internalAnnotationInfo.getStrand() > 0 ? "+" : "-";
            locationText += ")";
            locationField.setText(locationText);
            locationField.setVisible(true);
        } else {
            locationField.setVisible(false);
        }

        loadStatuses();
        setVisible(true);
    }

    private void updateTranscript() {
        final AnnotationInfo updatedInfo = this.internalAnnotationInfo;
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
        RestService.sendRequest(requestCallback, "annotator/updateFeature/", AnnotationRestService.convertAnnotationInfoToJSONObject(this.internalAnnotationInfo));
    }

    private void loadStatuses() {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                resetStatusBox();
                JSONArray availableStatusArray = JSONParser.parseStrict(response.getText()).isArray();
                if (availableStatusArray.size() > 0) {
                    statusListBox.addItem("No status selected", HasDirection.Direction.DEFAULT, null);
                    String status = getInternalAnnotationInfo().getStatus();
                    for (int i = 0; i < availableStatusArray.size(); i++) {
                        String availableStatus = availableStatusArray.get(i).isString().stringValue();
                        statusListBox.addItem(availableStatus);
                        if (availableStatus.equals(status)) {
                            statusListBox.setSelectedIndex(i + 1);
                        }
                    }
                    statusLabelField.setVisible(true);
                    statusListBox.setVisible(true);
                } else {
                    statusLabelField.setVisible(false);
                    statusListBox.setVisible(false);
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert(exception.getMessage());
            }
        };
        AvailableStatusRestService.getAvailableStatuses(requestCallback, getInternalAnnotationInfo());
    }

    private void resetStatusBox() {
        statusListBox.clear();
    }

    public AnnotationInfo getInternalAnnotationInfo() {
        return internalAnnotationInfo;
    }

    private void enableFields(boolean enabled) {
        nameField.setEnabled(enabled && editable);
        descriptionField.setEnabled(enabled && editable);
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        nameField.setEnabled(this.editable);
        descriptionField.setEnabled(this.editable);
    }
}
