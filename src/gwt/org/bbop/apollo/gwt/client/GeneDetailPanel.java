package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import org.bbop.apollo.AvailableStatusService;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.event.AnnotationInfoChangeEvent;
import org.bbop.apollo.gwt.client.rest.AnnotationRestService;
import org.bbop.apollo.gwt.client.rest.AvailableStatusRestService;
import org.bbop.apollo.gwt.client.rest.RestService;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.Date;

/**
 * Created by ndunn on 1/9/15.
 */
public class GeneDetailPanel extends Composite {

    private AnnotationInfo internalAnnotationInfo;


    interface AnnotationDetailPanelUiBinder extends UiBinder<Widget, GeneDetailPanel> {
    }

    private static AnnotationDetailPanelUiBinder ourUiBinder = GWT.create(AnnotationDetailPanelUiBinder.class);
    @UiField(provided = true)
    SuggestBox nameField;
    @UiField
    TextBox symbolField;
    @UiField
    TextBox descriptionField;
    @UiField
    TextBox locationField;
    @UiField
    TextBox sequenceField;
    @UiField
    TextBox userField;
    @UiField
    TextBox dateCreatedField;
    @UiField
    TextBox lastUpdatedField;
    @UiField
    TextBox synonymsField;
    @UiField
    TextBox typeField;
    @UiField
    ListBox statusField;
    @UiField
    InputGroupAddon statusLabelField;

    private SuggestedNameOracle suggestedNameOracle = new SuggestedNameOracle();

    public GeneDetailPanel() {
        nameField = new SuggestBox(suggestedNameOracle);

        initWidget(ourUiBinder.createAndBindUi(this));

        nameField.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {
            @Override
            public void onSelection(SelectionEvent<SuggestOracle.Suggestion> event) {
                handleNameChange();
            }
        });
    }

    private void handleNameChange() {
        String updatedName = nameField.getText();
        internalAnnotationInfo.setName(updatedName);
        updateGene();
    }

    @UiHandler("symbolField")
    void handleSymbolChange(ChangeEvent e) {
        String updatedName = symbolField.getText();
        internalAnnotationInfo.setSymbol(updatedName);
        updateGene();
    }

    @UiHandler("descriptionField")
    void handleDescriptionChange(ChangeEvent e) {
        String updatedName = descriptionField.getText();
        internalAnnotationInfo.setDescription(updatedName);
        updateGene();
    }

    private void enableFields(boolean enabled) {
        nameField.setEnabled(enabled);
        symbolField.setEnabled(enabled);
        descriptionField.setEnabled(enabled);
    }


    private void updateGene() {
        final AnnotationInfo updatedInfo = this.internalAnnotationInfo;
        enableFields(false);

        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
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

    /**
     * updateData
     */
    public void updateData(AnnotationInfo annotationInfo) {
        this.internalAnnotationInfo = annotationInfo;
        suggestedNameOracle.setOrganismName(MainPanel.getInstance().getCurrentOrganism().getName());
        suggestedNameOracle.setFeatureType("sequence:"+annotationInfo.getType());
        nameField.setText(internalAnnotationInfo.getName());
        symbolField.setText(internalAnnotationInfo.getSymbol());
        typeField.setText(internalAnnotationInfo.getType());
        descriptionField.setText(internalAnnotationInfo.getDescription());
        sequenceField.setText(internalAnnotationInfo.getSequence());
        userField.setText(internalAnnotationInfo.getOwner());
        dateCreatedField.setText(DateFormatService.formatTimeAndDate(internalAnnotationInfo.getDateCreated()));
        lastUpdatedField.setText(DateFormatService.formatTimeAndDate(internalAnnotationInfo.getDateLastModified()));

        if (internalAnnotationInfo.getMin() != null) {
            String locationText = internalAnnotationInfo.getMin().toString();
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

    private void loadStatuses() {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                resetStatusBox();
                JSONArray availableStatusArray = JSONParser.parseStrict(response.getText()).isArray();
                if(availableStatusArray.size()>0){
                    statusField.addItem("No status selected", HasDirection.Direction.DEFAULT,null);
                    for(int i = 0 ; i < availableStatusArray.size() ; i++){
                        String availableStatus = availableStatusArray.get(i).isString().stringValue();
                        statusField.addItem(availableStatus);
                    }
                    statusLabelField.setVisible(true);
                    statusField.setVisible(true);
                }
                else{
                    statusLabelField.setVisible(false);
                    statusField.setVisible(false);
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert(exception.getMessage());
            }
        };
        AvailableStatusRestService.getAvailableStatuses(requestCallback,getInternalAnnotationInfo());
    }

    private void resetStatusBox() {
        statusField.clear();
    }

    public AnnotationInfo getInternalAnnotationInfo() {
        return internalAnnotationInfo;
    }

    public void setEditable(boolean editable) {
        nameField.setEnabled(editable);
        symbolField.setEnabled(editable);
        descriptionField.setEnabled(editable);

    }
}
