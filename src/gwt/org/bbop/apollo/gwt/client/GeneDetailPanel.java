package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.event.AnnotationInfoChangeEvent;
import org.bbop.apollo.gwt.client.rest.AnnotationRestService;
import org.bbop.apollo.gwt.client.rest.RestService;
import org.gwtbootstrap3.client.ui.SuggestBox;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

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

    private SuggestedNameOracle suggestedNameOracle = new SuggestedNameOracle();

    public GeneDetailPanel() {
        nameField = new SuggestBox(suggestedNameOracle);

        initWidget(ourUiBinder.createAndBindUi(this));

//        nameField.addValueChangeHandler(new ValueChangeHandler<String>() {
//            @Override
//            public void onValueChange(ValueChangeEvent<String> event) {
//                handleNameChange();
//            }
//        });

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
        } else {
            locationField.setVisible(false);
        }


        setVisible(true);
    }

    public void setEditable(boolean editable) {
        nameField.setEnabled(editable);
        symbolField.setEnabled(editable);
        descriptionField.setEnabled(editable);

    }
}
