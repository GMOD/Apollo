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
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

/**
 * Created by deepak.unni3 on 8/2/16.
 */
public class VariantDetailPanel extends Composite {
    private AnnotationInfo internalAnnotationInfo;

    interface AnnotationDetailPanelUiBinder extends UiBinder<Widget, VariantDetailPanel> {
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
    @UiField
    TextBox referenceNucleotideField;
    @UiField
    TextBox alternateNucleotideField;
    @UiField
    TextBox minorAlleleFrequencyField;

    public VariantDetailPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    public void updateData(AnnotationInfo variantAnnotationInfo) {
        this.internalAnnotationInfo = variantAnnotationInfo;
        nameField.setText(internalAnnotationInfo.getName());
        referenceNucleotideField.setText(internalAnnotationInfo.getReferenceNucleotide());
        alternateNucleotideField.setText(internalAnnotationInfo.getAlternateNucleotide());
        if (internalAnnotationInfo.getMinorAlleleFrequency() != null) {
            minorAlleleFrequencyField.setText(internalAnnotationInfo.getMinorAlleleFrequency().toString());
        }

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

    @UiHandler("nameField")
    void handleNameChange(ChangeEvent e) {
        String updatedName = nameField.getText();
        internalAnnotationInfo.setName(updatedName);
        updateVariant();
    }

    @UiHandler("descriptionField")
    void handleDescriptionChange(ChangeEvent e) {
        String updatedDescription = descriptionField.getText();
        internalAnnotationInfo.setDescription(updatedDescription);
        updateVariant();
    }

    @UiHandler("referenceNucleotideField")
    void handleReferenceNucleotideChange(ChangeEvent e) {
        String updatedReferenceNucleotide = referenceNucleotideField.getText().toUpperCase();
        if (isValidDNA(updatedReferenceNucleotide)) {
            internalAnnotationInfo.setReferenceNucleotide(updatedReferenceNucleotide);
            updateVariant();
        }
        else {
            Bootbox.alert("Nucleotides should be A, T, C, G or N");
            referenceNucleotideField.clear();
        }
    }

    @UiHandler("alternateNucleotideField")
    void handleAlternateNucleotideChange(ChangeEvent e) {
        String updatedAlternateNucleotide = alternateNucleotideField.getText().toUpperCase();
        if (isValidDNA(updatedAlternateNucleotide)) {
            internalAnnotationInfo.setAlternateNucleotide(updatedAlternateNucleotide);
            updateVariant();
        }
        else {
            Bootbox.alert("Nucleotides should be A, T, C, G or N");
            alternateNucleotideField.clear();
        }
    }

    @UiHandler("minorAlleleFrequencyField")
    void handleMinorAlleleFrequencyChange(ChangeEvent e) {
        Float updatedMinorAlleleFrequency;
        try {
            updatedMinorAlleleFrequency = Float.parseFloat(minorAlleleFrequencyField.getText());
            if (updatedMinorAlleleFrequency < 0.0F || updatedMinorAlleleFrequency > 1.0F) {
                Bootbox.alert("MAF should be within the range of 0.0 - 1.0");
                minorAlleleFrequencyField.clear();
            }
            else {
                internalAnnotationInfo.setMinorAlleleFrequency(updatedMinorAlleleFrequency.toString());
                updateVariant();
            }
        } catch (NumberFormatException n) {
            minorAlleleFrequencyField.clear();
        }

    }

    private void enableFields(boolean enabled) {
        nameField.setEnabled(enabled);
        referenceNucleotideField.setEnabled(enabled);
        alternateNucleotideField.setEnabled(enabled);
        minorAlleleFrequencyField.setEnabled(enabled);
        descriptionField.setEnabled(enabled);
    }

    private void updateVariant() {
        String url = Annotator.getRootUrl() + "annotator/updateFeature";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
        sb.append("data=" + AnnotationRestService.convertAnnotationInfoToJSONObject(this.internalAnnotationInfo).toString());
        final AnnotationInfo updatedInfo = this.internalAnnotationInfo;
        builder.setRequestData(sb.toString());
        GWT.log(sb.toString());
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
                Bootbox.alert("Error updating variant: " + exception);
                enableFields(true);
            }
        };
        try {
            builder.setCallback(requestCallback);
            builder.send();
        } catch(RequestException e) {
            enableFields(true);
            Bootbox.alert(e.getMessage());
        }
    }

    private boolean isValidDNA(String bases) {
        return bases.matches("^[ATCGN]+$");
    }
}