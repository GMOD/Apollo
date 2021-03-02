package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.HasDirection;
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
import org.bbop.apollo.gwt.client.rest.AvailableStatusRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.bootbox.client.callback.ConfirmCallback;

import java.util.*;

/**
 * Created by deepak.unni3 on 8/2/16.
 */
public class VariantDetailPanel extends Composite {
    public static List<String> variantTypes = Arrays.asList("SNV", "SNP", "MNV", "MNP", "indel");
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
    TextBox referenceAlleleField;
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
    @UiField
    Button deleteAnnotation;
    @UiField
    Button gotoAnnotation;
    @UiField
    Button annotationIdButton;

    public VariantDetailPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    @UiHandler("statusListBox")
    void handleStatusLabelFieldChange(ChangeEvent e) {
        String updatedStatus = statusListBox.getSelectedValue();
        internalAnnotationInfo.setStatus(updatedStatus);
        updateVariant();
    }

    public void updateData(AnnotationInfo annotationInfo) {
        this.internalAnnotationInfo = annotationInfo;
        nameField.setText(internalAnnotationInfo.getName());
        referenceAlleleField.setText(internalAnnotationInfo.getReferenceAllele());
        descriptionField.setText(internalAnnotationInfo.getDescription());
        synonymsField.setText(internalAnnotationInfo.getSynonyms());
        sequenceField.setText(internalAnnotationInfo.getSequence());
        userField.setText(internalAnnotationInfo.getOwner());
        typeField.setText(internalAnnotationInfo.getType());
        dateCreatedField.setText(DateFormatService.formatTimeAndDate(internalAnnotationInfo.getDateCreated()));
        lastUpdatedField.setText(DateFormatService.formatTimeAndDate(internalAnnotationInfo.getDateLastModified()));

        if (internalAnnotationInfo.getMin() != null) {
            String locationText = Integer.toString(internalAnnotationInfo.getMin()+1);
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
        loadStatuses();
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

    @UiHandler("synonymsField")
    void handleSynonymsChange(ChangeEvent e) {
        final AnnotationInfo updateAnnotationInfo = this.internalAnnotationInfo;
        final String updatedName = synonymsField.getText().trim();
        String[] synonyms = updatedName.split("\\|");
        String infoString = "";
        for(String s : synonyms){
            infoString += "'"+ s.trim() + "' ";
        }
        infoString = infoString.trim();
        Bootbox.confirm(synonyms.length + " synonyms: " + infoString, new ConfirmCallback() {
            @Override
            public void callback(boolean result) {
                if(result){
                    updateAnnotationInfo.setSynonyms(updatedName);
                    synonymsField.setText(updatedName);
                    updateVariant();
                }
                else{
                    synonymsField.setText(updateAnnotationInfo.getSynonyms());
                }
            }
        });



    }


    @UiHandler("annotationIdButton")
    void getAnnotationInfo(ClickEvent clickEvent) {
      new LinkDialog("UniqueName: "+internalAnnotationInfo.getUniqueName(),"Link to: "+MainPanel.getInstance().generateApolloLink(internalAnnotationInfo.getUniqueName()),true);
    }

    @UiHandler("gotoAnnotation")
    void gotoAnnotation(ClickEvent clickEvent) {
        Integer min = internalAnnotationInfo.getMin() - 50;
        Integer max = internalAnnotationInfo.getMax() + 50;
        min = min < 0 ? 0 : min;
        MainPanel.updateGenomicViewerForLocation(internalAnnotationInfo.getSequence(), min, max, false, false);
    }

    private Set<AnnotationInfo> getDeletableChildren(AnnotationInfo selectedAnnotationInfo) {
        String type = selectedAnnotationInfo.getType();
        if (type.equalsIgnoreCase(FeatureStringEnum.GENE.getValue()) || type.equalsIgnoreCase(FeatureStringEnum.PSEUDOGENE.getValue())) {
            return selectedAnnotationInfo.getChildAnnotations();
        }
        return new HashSet<>();
    }

    @UiHandler("deleteAnnotation")
    void deleteAnnotation(ClickEvent clickEvent) {
        final Set<AnnotationInfo> deletableChildren = getDeletableChildren(internalAnnotationInfo);
        String confirmString = "";
        if (deletableChildren.size() > 0) {
            confirmString = "Delete the " + deletableChildren.size() + " annotation" + (deletableChildren.size() > 1 ? "s" : "") + " belonging to the " + internalAnnotationInfo.getType() + " " + internalAnnotationInfo.getName() + "?";
        } else {
            confirmString = "Delete the " + internalAnnotationInfo.getType() + " " + internalAnnotationInfo.getName() + "?";
        }


        final RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                if (response.getStatusCode() == 200) {
                    // parse to make sure we return the complete amount
                    try {
                        JSONValue returnValue = JSONParser.parseStrict(response.getText());
                        GWT.log("Return: "+returnValue.toString());
                        Bootbox.confirm("Success.  Reload page to reflect results?", new ConfirmCallback() {
                            @Override
                            public void callback(boolean result) {
                                if(result){
                                    Window.Location.reload();
                                }
                            }
                        });
                    } catch (Exception e) {
                        Bootbox.alert(e.getMessage());
                    }
                } else {
                    Bootbox.alert("Problem with deletion: " + response.getText());
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Problem with deletion: " + exception.getMessage());
            }
        };

        Bootbox.confirm(confirmString, new ConfirmCallback() {
            @Override
            public void callback(boolean result) {
                if (result) {
                    if (deletableChildren.size() == 0) {
                        Set<AnnotationInfo> annotationInfoSet = new HashSet<>();
                        annotationInfoSet.add(internalAnnotationInfo);
                        AnnotationRestService.deleteAnnotations(requestCallback, annotationInfoSet);
                    } else {
                        JSONObject jsonObject = AnnotationRestService.deleteAnnotations(requestCallback, deletableChildren);
                    }
                }
            }
        });
    }

    private void enableFields(boolean enabled) {
        nameField.setEnabled(enabled);
        descriptionField.setEnabled(enabled);
        synonymsField.setEnabled(enabled);
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

    private void updateVariant() {
        String url = Annotator.getRootUrl() + "annotator/updateFeature";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
        JSONObject data = AnnotationRestService.convertAnnotationInfoToJSONObject(this.internalAnnotationInfo);
        data.put(FeatureStringEnum.ORGANISM.getValue(),new JSONString(MainPanel.getInstance().getCurrentOrganism().getId()));
        sb.append("data=" + data.toString());
        final AnnotationInfo updatedInfo = this.internalAnnotationInfo;
        builder.setRequestData(sb.toString());
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

    public static boolean isValidDNA(String bases) {
        return bases.matches("^[ATCGN]+$");
    }
}
