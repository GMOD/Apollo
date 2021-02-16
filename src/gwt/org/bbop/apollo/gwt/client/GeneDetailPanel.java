package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
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
import com.google.gwt.user.client.ui.SuggestOracle;
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
public class GeneDetailPanel extends Composite {

    private AnnotationInfo internalAnnotationInfo;


    interface AnnotationDetailPanelUiBinder extends UiBinder<Widget, GeneDetailPanel> {
    }

    private static final AnnotationDetailPanelUiBinder ourUiBinder = GWT.create(AnnotationDetailPanelUiBinder.class);
    @UiField(provided = true)
    SuggestBox nameField;
    @UiField
    Button syncNameButton;
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

    @UiHandler("obsoleteButton")
    void handleObsoleteChange(ChangeEvent e) {
        internalAnnotationInfo.setObsolete(obsoleteButton.getValue());
        updateGene();
    }

    @UiHandler("syncNameButton")
    void handleSyncName(ClickEvent e) {
        String inputName = internalAnnotationInfo.getName();
        Set<AnnotationInfo> childAnnotations = internalAnnotationInfo.getChildAnnotations();
        assert childAnnotations.size()==1 ;
        AnnotationInfo firstChild = childAnnotations.iterator().next();
        firstChild.setName(inputName);
        updateData(internalAnnotationInfo);

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
        JSONObject data = AnnotationRestService.convertAnnotationInfoToJSONObject(firstChild);
        data.put(FeatureStringEnum.ORGANISM.getValue(),new JSONString(MainPanel.getInstance().getCurrentOrganism().getId()));

        RestService.sendRequest(requestCallback, "annotator/updateFeature/", data);

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
                    updateGene();
                }
                else{
                    synonymsField.setText(updateAnnotationInfo.getSynonyms());
                }
            }
        });



    }

    @UiHandler("descriptionField")
    void handleDescriptionChange(ChangeEvent e) {
        String updatedName = descriptionField.getText();
        internalAnnotationInfo.setDescription(updatedName);
        updateGene();
    }

    @UiHandler("statusListBox")
    void handleStatusLabelFieldChange(ChangeEvent e) {
        String updatedStatus = statusListBox.getSelectedValue();
        internalAnnotationInfo.setStatus(updatedStatus);
        updateGene();
    }


    @UiHandler({"partialMin", "partialMax"})
    void handlePartial(ChangeEvent e){
        internalAnnotationInfo.setPartialMin(partialMin.getValue());
        internalAnnotationInfo.setPartialMax(partialMax.getValue());
        updatePartials();
    }


    private void checkSyncButton(){
        Set<AnnotationInfo> childAnnotations = internalAnnotationInfo.getChildAnnotations();
        if(childAnnotations.size()==1){
            AnnotationInfo firstChild = childAnnotations.iterator().next();
            syncNameButton.setEnabled(!this.internalAnnotationInfo.getName().equals(firstChild.getName()));
        }
        else{
            syncNameButton.setEnabled(false);
        }

    }

    public void setEditable(boolean editable) {
        nameField.setEnabled(editable);
        symbolField.setEnabled(editable);
        descriptionField.setEnabled(editable);
        synonymsField.setEnabled(editable);
        deleteAnnotation.setEnabled(editable);
        partialMin.setEnabled(editable);
        partialMax.setEnabled(editable);

        if(!editable || this.internalAnnotationInfo==null){
            syncNameButton.setEnabled(false);
        }
        else{
            checkSyncButton();
        }
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

    private void updateGene() {
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

        RestService.sendRequest(requestCallback, "annotator/updateFeature/", data);

    }

    /**
     * updateData
     */
    public void updateData(AnnotationInfo annotationInfo) {
        this.internalAnnotationInfo = annotationInfo;
        MainPanel.annotatorPanel.setSelectedChildUniqueName(null);
        suggestedNameOracle.setOrganismName(MainPanel.getInstance().getCurrentOrganism().getName());
        suggestedNameOracle.setFeatureType("sequence:" + annotationInfo.getType());
        nameField.setText(internalAnnotationInfo.getName());
        partialMin.setValue(internalAnnotationInfo.getPartialMin());
        partialMax.setValue(internalAnnotationInfo.getPartialMax());
        obsoleteButton.setValue(internalAnnotationInfo.getObsolete());
        symbolField.setText(internalAnnotationInfo.getSymbol());
        typeField.setText(internalAnnotationInfo.getType());
        synonymsField.setText(internalAnnotationInfo.getSynonyms());
        descriptionField.setText(internalAnnotationInfo.getDescription());
        sequenceField.setText(internalAnnotationInfo.getSequence());
        userField.setText(internalAnnotationInfo.getOwner());
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
                  statusLabelField.setText("Status");
                  statusListBox.setEnabled(true);
                } else {
                  statusLabelField.setText("No status created");
                  statusListBox.setEnabled(false);
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

}
