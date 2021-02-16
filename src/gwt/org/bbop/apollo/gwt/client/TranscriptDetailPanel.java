package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.json.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.rest.AnnotationRestService;
import org.bbop.apollo.gwt.client.rest.AvailableStatusRestService;
import org.bbop.apollo.gwt.client.rest.RestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.bootbox.client.callback.ConfirmCallback;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by ndunn on 1/9/15.
 */
public class TranscriptDetailPanel extends Composite {

    private AnnotationInfo internalAnnotationInfo;

    interface AnnotationDetailPanelUiBinder extends UiBinder<Widget, TranscriptDetailPanel> {
    }

    private static AnnotationDetailPanelUiBinder ourUiBinder = GWT.create(AnnotationDetailPanelUiBinder.class);


    @UiField
    TextBox nameField;
    @UiField
    Button syncNameButton;
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
    @UiField
    Button deleteAnnotation;
    @UiField
    Button gotoAnnotation;
    @UiField
    Button annotationIdButton;
    @UiField
    InlineCheckBox partialMin;
    @UiField
    InlineCheckBox partialMax;
    @UiField
    InlineCheckBox obsoleteButton;

    private Boolean editable = false;

    public TranscriptDetailPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    @UiHandler("obsoleteButton")
    void handleObsoleteChange(ChangeEvent e) {
        internalAnnotationInfo.setObsolete(obsoleteButton.getValue());
        updateTranscript();
    }

    @UiHandler("nameField")
    void handleNameChange(ChangeEvent e) {
        internalAnnotationInfo.setName(nameField.getText());
        updateTranscript();
    }

    @UiHandler("syncNameButton")
    void handleSyncName(ClickEvent e) {
        String inputName = internalAnnotationInfo.getName();
        AnnotationInfo geneAnnotation = MainPanel.annotatorPanel.getCurrentGene();
        Set<AnnotationInfo> childAnnotations = geneAnnotation.getChildAnnotations();
        assert childAnnotations.size()==1 ;
        geneAnnotation.setName(inputName);
//
        setEditable(false);
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                setEditable(true);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error updating gene: " + exception);
                setEditable(true);
            }
        };
        JSONObject data = AnnotationRestService.convertAnnotationInfoToJSONObject(geneAnnotation);
        data.put(FeatureStringEnum.ORGANISM.getValue(),new JSONString(MainPanel.getInstance().getCurrentOrganism().getId()));

        RestService.sendRequest(requestCallback, "annotator/updateFeature/", data);

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
                    updateTranscript();
                }
                else{
                    synonymsField.setText(updateAnnotationInfo.getSynonyms());
                }
            }
        });



    }

    @UiHandler({"partialMin", "partialMax"})
    void handlePartial(ChangeEvent e){
        internalAnnotationInfo.setPartialMin(partialMin.getValue());
        internalAnnotationInfo.setPartialMax(partialMax.getValue());
        updatePartials();
    }

    private void updatePartials() {
        setEditable(false);
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                setEditable(true);
                MainPanel.annotatorPanel.setSelectedChildUniqueName(null);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error updating gene: " + exception);
                setEditable(true);
            }
        };
//        RestService.sendRequest(requestCallback, "annotator/updateFeature/", AnnotationRestService.convertAnnotationInfoToJSONObject(this.internalAnnotationInfo));
        JSONObject data = AnnotationRestService.convertAnnotationInfoToJSONObject(this.internalAnnotationInfo);
        data.put(FeatureStringEnum.ORGANISM.getValue(),new JSONString(MainPanel.getInstance().getCurrentOrganism().getId()));

        RestService.sendRequest(requestCallback, "annotator/updatePartials/", data);

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
                        GWT.log("Return: " + returnValue.toString());
                        Bootbox.confirm("Success.  Reload page to reflect results?", new ConfirmCallback() {
                            @Override
                            public void callback(boolean result) {
                                if (result) {
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

    public void updateData(AnnotationInfo annotationInfo) {
        this.internalAnnotationInfo = annotationInfo;
        nameField.setText(internalAnnotationInfo.getName());
        descriptionField.setText(internalAnnotationInfo.getDescription());
        synonymsField.setText(internalAnnotationInfo.getSynonyms());
        userField.setText(internalAnnotationInfo.getOwner());
        typeField.setText(internalAnnotationInfo.getType());
        partialMin.setValue(internalAnnotationInfo.getPartialMin());
        partialMax.setValue(internalAnnotationInfo.getPartialMax());
        obsoleteButton.setValue(internalAnnotationInfo.getObsolete());
        sequenceField.setText(internalAnnotationInfo.getSequence());
        dateCreatedField.setText(DateFormatService.formatTimeAndDate(internalAnnotationInfo.getDateCreated()));
        lastUpdatedField.setText(DateFormatService.formatTimeAndDate(internalAnnotationInfo.getDateLastModified()));
        checkSyncButton();

        if (internalAnnotationInfo.getMin() != null) {
            String locationText = Integer.toString(internalAnnotationInfo.getMin()+1);
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
        MainPanel.getInstance().setSelectedAnnotationInfo(updatedInfo);
        setEditable(false);
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject jsonObject = JSONParser.parseStrict(response.getText()).isObject();
//                GWT.log("response array: "+jsonObject.toString());
                setEditable(true);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error updating transcript: " + exception);
                setEditable(true);
            }
        };
        JSONObject data = AnnotationRestService.convertAnnotationInfoToJSONObject(this.internalAnnotationInfo);
        data.put(FeatureStringEnum.ORGANISM.getValue(),new JSONString(MainPanel.getInstance().getCurrentOrganism().getId()));
        RestService.sendRequest(requestCallback, "annotator/updateFeature/", data);
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

    private void checkSyncButton(){
        AnnotationInfo geneAnnotation = MainPanel.annotatorPanel.getCurrentGene();
        if(geneAnnotation==null){
            GWT.log("Please select gene to synchronize name");
            return ;
        }
        Set<AnnotationInfo> childAnnotations = geneAnnotation.getChildAnnotations();
        if(childAnnotations.size()==1){
            syncNameButton.setEnabled(!this.internalAnnotationInfo.getName().equals(geneAnnotation.getName()));
        }
        else{
            syncNameButton.setEnabled(false);
        }
    }


    public void setEditable(boolean editable) {
        this.editable = editable;
        nameField.setEnabled(this.editable);
        descriptionField.setEnabled(this.editable);
        synonymsField.setEnabled(this.editable);
        deleteAnnotation.setEnabled(this.editable);
        partialMin.setEnabled(editable);
        partialMax.setEnabled(editable);
        if(!editable || this.internalAnnotationInfo==null || MainPanel.annotatorPanel.getCurrentGene() == null){
            syncNameButton.setEnabled(false);
        }
        else{
            checkSyncButton();
        }
    }
}
