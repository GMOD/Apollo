package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.*;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.OrganismInfoConverter;
import org.bbop.apollo.gwt.client.event.OrganismChangeEvent;
import org.bbop.apollo.gwt.client.event.OrganismChangeEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.OrganismRestService;
import org.bbop.apollo.gwt.client.rest.RestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.bbop.apollo.gwt.shared.track.SequenceTypeEnum;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.bootbox.client.callback.ConfirmCallback;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by ndunn on 12/17/14.
 */
public class OrganismPanel extends Composite {


    interface OrganismBrowserPanelUiBinder extends UiBinder<Widget, OrganismPanel> {
    }


    private static OrganismBrowserPanelUiBinder ourUiBinder = GWT.create(OrganismBrowserPanelUiBinder.class);
    @UiField
    TextBox organismName;
    @UiField
    TextBox blatdb;
    @UiField
    CheckBox publicMode;
    @UiField
    CheckBox obsoleteButton;
    @UiField
    TextBox genus;
    @UiField
    TextBox species;
    @UiField
    TextBox sequenceFile;
    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<OrganismInfo> dataGrid = new DataGrid<OrganismInfo>(20, tablecss);
    @UiField
    Button newButton;
    @UiField
    Button createButton;
    @UiField
    Button duplicateButton;
    @UiField
    Button cancelButton;
    @UiField
    Button deleteButton;
    @UiField(provided = true)
    WebApolloSimplePager pager = new WebApolloSimplePager(WebApolloSimplePager.TextLocation.CENTER);
    @UiField
    TextBox nonDefaultTranslationTable;
    @UiField
    org.gwtbootstrap3.client.ui.Label organismIdLabel;
    @UiField
    CheckBoxButton showOnlyPublicOrganisms;
    @UiField
    CheckBoxButton showObsoleteOrganisms;
    @UiField
    Modal addOrganismFromSequencePanel;
    @UiField
    com.google.gwt.user.client.ui.Button saveNewOrganism;
    @UiField
    Button cancelNewOrganism;
    @UiField
    Button uploadOrganismButton;
    @UiField
    Button downloadOrganismButton;
    @UiField
    FormPanel newOrganismForm;
    @UiField
    TextBox organismUploadName;
    @UiField
    FileUpload organismUploadSequence;
    @UiField
    HTML uploadDescription;
    @UiField
    TextBox organismUploadGenus;
    @UiField
    TextBox organismUploadSpecies;
    @UiField
    TextBox organismUploadNonDefaultTranslationTable;
    @UiField
    static TextBox nameSearchBox;

    boolean creatingNewOrganism = false; // a special flag for handling the clearSelection event when filling out new organism info
    boolean savingNewOrganism = false; // a special flag for handling the clearSelection event when filling out new organism info

    final LoadingDialog loadingDialog;
    final ErrorDialog errorDialog;

    static private ListDataProvider<OrganismInfo> dataProvider = new ListDataProvider<>();
    private static List<OrganismInfo> organismInfoList = new ArrayList<>();
    private static List<OrganismInfo> filteredOrganismInfoList = dataProvider.getList();


    private final SingleSelectionModel<OrganismInfo> singleSelectionModel = new SingleSelectionModel<>();

    public OrganismPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
        loadingDialog = new LoadingDialog("Processing ...", null, false);
        errorDialog = new ErrorDialog("Error", "Organism directory must be an absolute path pointing to 'trackList.json'", false, true);

        organismUploadName.getElement().setPropertyString("placeholder", "Enter organism name");
        newOrganismForm.setEncoding(FormPanel.ENCODING_MULTIPART);
        newOrganismForm.setMethod(FormPanel.METHOD_POST);
        newOrganismForm.setAction(RestService.fixUrl("organism/addOrganismWithSequence"));

        uploadDescription.setHTML("<small>" + SequenceTypeEnum.generateSuffixDescription() + "</small>");

        newOrganismForm.addSubmitHandler(new FormPanel.SubmitHandler() {
            @Override
            public void onSubmit(FormPanel.SubmitEvent event) {
                addOrganismFromSequencePanel.hide();
            }
        });

        newOrganismForm.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
            @Override
            public void onSubmitComplete(FormPanel.SubmitCompleteEvent event) {
                String results = event.getResults();
                int errorResults1 = results.indexOf("\">{\"error\":\"");
                int errorResults2 = results.indexOf("\"}</pre>");
                if (results.startsWith("<pre") && errorResults1 > 0 && errorResults2 > errorResults1) {
                    String jsonSubString = results.substring(errorResults1+2, errorResults2+2);
                    GWT.log("Error response: " + jsonSubString);
                    JSONObject errorObject = JSONParser.parseStrict(jsonSubString).isObject();
                    GWT.log(errorObject.toString());
                    Bootbox.alert("There was a problem adding the organism: "+errorObject.get("error").isString().stringValue());
                    return;
                }

                Bootbox.confirm("Organism '" + organismUploadName.getText() + "' submitted successfully.  Reload to see?", new ConfirmCallback() {
                    @Override
                    public void callback(boolean result) {
                        if (result) {
                            Window.Location.reload();
                        }
                    }
                });

            }
        });

        TextColumn<OrganismInfo> organismNameColumn = new TextColumn<OrganismInfo>() {
            @Override
            public String getValue(OrganismInfo organism) {
                if (organism.getObsolete()) {
                    return "(obs) " + organism.getName();
                }
                return organism.getName();
            }
        };
        Column<OrganismInfo, Number> annotationsNameColumn = new Column<OrganismInfo, Number>(new NumberCell()) {
            @Override
            public Integer getValue(OrganismInfo object) {
                return object.getNumFeatures();
            }
        };
        Column<OrganismInfo, Number> sequenceColumn = new Column<OrganismInfo, Number>(new NumberCell()) {
            @Override
            public Integer getValue(OrganismInfo object) {
                return object.getNumSequences();
            }
        };

        sequenceColumn.setSortable(true);
        organismNameColumn.setSortable(true);
        annotationsNameColumn.setSortable(true);

        Annotator.eventBus.addHandler(OrganismChangeEvent.TYPE, new OrganismChangeEventHandler() {
            @Override
            public void onOrganismChanged(OrganismChangeEvent organismChangeEvent) {
                organismInfoList.clear();
                organismInfoList.addAll(MainPanel.getInstance().getOrganismInfoList());
                filterList();
            }
        });

        dataGrid.setLoadingIndicator(new HTML("Calculating Annotations ... "));
        dataGrid.addColumn(organismNameColumn, "Name");
        dataGrid.addColumn(annotationsNameColumn, "Annotations");
        SafeHtmlHeader safeHtmlHeader = new SafeHtmlHeader(new SafeHtml() {
            @Override
            public String asString() {
                return "<div style=\"text-align: right;\">Ref Sequences</p>";
            }
        });
        dataGrid.addColumn(sequenceColumn, safeHtmlHeader);
        dataGrid.setEmptyTableWidget(new Label("No organisms available. Add new organisms using the form field."));


        singleSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if (!creatingNewOrganism) {
                    loadOrganismInfo();
                    changeButtonSelection();
                } else {
                    creatingNewOrganism = false;
                }
            }
        });
        dataGrid.setSelectionModel(singleSelectionModel);

        dataProvider.addDataDisplay(dataGrid);
        pager.setDisplay(dataGrid);


        dataGrid.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                if (singleSelectionModel.getSelectedObject() != null) {
                    OrganismInfo organismInfo = singleSelectionModel.getSelectedObject();
                    if (organismInfo.getObsolete()) {
                        Bootbox.alert("You will have to make this organism 'active' by unselecting the 'Obsolete' checkbox in the Organism Details panel at the bottom.");
                        return;
                    }
                    String orgId = organismInfo.getId();
                    if (!MainPanel.getInstance().getCurrentOrganism().getId().equals(orgId)) {
                        OrganismRestService.switchOrganismById(orgId);
                    }
                }
            }
        }, DoubleClickEvent.getType());

        ColumnSortEvent.ListHandler<OrganismInfo> sortHandler = new ColumnSortEvent.ListHandler<OrganismInfo>(organismInfoList);
        dataGrid.addColumnSortHandler(sortHandler);
        sortHandler.setComparator(organismNameColumn, new Comparator<OrganismInfo>() {
            @Override
            public int compare(OrganismInfo o1, OrganismInfo o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        sortHandler.setComparator(annotationsNameColumn, new Comparator<OrganismInfo>() {
            @Override
            public int compare(OrganismInfo o1, OrganismInfo o2) {
                return o1.getNumFeatures() - o2.getNumFeatures();
            }
        });
        sortHandler.setComparator(sequenceColumn, new Comparator<OrganismInfo>() {
            @Override
            public int compare(OrganismInfo o1, OrganismInfo o2) {
                return o1.getNumSequences() - o2.getNumSequences();
            }
        });


    }


    @UiHandler("nameSearchBox")
    public void doSearch(KeyUpEvent keyUpEvent) {
        filterList();
    }

    static void filterList() {
        String text = nameSearchBox.getText();
        filteredOrganismInfoList.clear();
        if(text.trim().length()==0){
            filteredOrganismInfoList.addAll(organismInfoList);
            return ;
        }
        for (OrganismInfo organismInfo : organismInfoList) {
            if (organismInfo.getName().toLowerCase().contains(text.toLowerCase())) {
                    filteredOrganismInfoList.add(organismInfo);
            }
        }
    }

    public void loadOrganismInfo() {
        loadOrganismInfo(singleSelectionModel.getSelectedObject());
    }

    public void loadOrganismInfo(OrganismInfo organismInfo) {
        if (organismInfo == null) {
            setNoSelection();
            return;
        }

        setTextEnabled(organismInfo.isEditable());

        GWT.log("loadOrganismInfo setValue " + organismInfo.getPublicMode());
        Boolean isEditable = organismInfo.isEditable() || MainPanel.getInstance().isCurrentUserAdmin();

        organismName.setText(organismInfo.getName());
        organismName.setEnabled(isEditable);

        blatdb.setText(organismInfo.getBlatDb());
        blatdb.setEnabled(isEditable);

        genus.setText(organismInfo.getGenus());
        genus.setEnabled(isEditable);

        species.setText(organismInfo.getSpecies());
        species.setEnabled(isEditable);

        sequenceFile.setText(organismInfo.getDirectory());
        sequenceFile.setEnabled(isEditable);

        publicMode.setValue(organismInfo.getPublicMode());
        publicMode.setEnabled(isEditable);

        obsoleteButton.setValue(organismInfo.getObsolete());
        obsoleteButton.setEnabled(isEditable);

        organismIdLabel.setHTML("Internal ID: " + organismInfo.getId());

        nonDefaultTranslationTable.setText(organismInfo.getNonDefaultTranslationTable());
        nonDefaultTranslationTable.setEnabled(isEditable);

        downloadOrganismButton.setVisible(false);
        deleteButton.setVisible(isEditable);
        deleteButton.setEnabled(isEditable);
    }

    private class UpdateInfoListCallback implements RequestCallback {

        @Override
        public void onResponseReceived(Request request, Response response) {
            JSONValue j = JSONParser.parseStrict(response.getText());
            JSONObject obj = j.isObject();
            deleteButton.setText("Delete Organism");
            if (obj != null && obj.containsKey("error")) {
                Bootbox.alert(obj.get("error").isString().stringValue());
                changeButtonSelection();
                setTextEnabled(false);
                clearTextBoxes();
                singleSelectionModel.clear();
            } else {
                List<OrganismInfo> organismInfoList = OrganismInfoConverter.convertJSONStringToOrganismInfoList(response.getText());
                dataGrid.setSelectionModel(singleSelectionModel);
                MainPanel.getInstance().getOrganismInfoList().clear();
                MainPanel.getInstance().getOrganismInfoList().addAll(organismInfoList);
                changeButtonSelection();
                OrganismChangeEvent organismChangeEvent = new OrganismChangeEvent(organismInfoList);
                organismChangeEvent.setAction(OrganismChangeEvent.Action.LOADED_ORGANISMS);
                Annotator.eventBus.fireEvent(organismChangeEvent);

                // in the case where we just add one . . .we should refresh the app state
                if (organismInfoList.size() == 1) {
                    MainPanel.getInstance().getAppState();
                }
            }
            if (savingNewOrganism) {
                savingNewOrganism = false;
                setNoSelection();
                changeButtonSelection(false);
                loadingDialog.hide();
                Window.Location.reload();
            }
        }

        @Override
        public void onError(Request request, Throwable exception) {
            loadingDialog.hide();
            Bootbox.alert("Error: " + exception);
        }
    }


    @UiHandler("uploadOrganismButton")
    public void uploadOrganismButton(ClickEvent event) {
        addOrganismFromSequencePanel.show();
    }

    @UiHandler("downloadOrganismButton")
    public void downloadOrganismButton(ClickEvent event) {

        OrganismInfo organismInfo = singleSelectionModel.getSelectedObject();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("organism",new JSONString(organismInfo.getId()));
        jsonObject.put("directory",new JSONString(organismInfo.getDirectory()));
        jsonObject.put("type", new JSONString(FeatureStringEnum.TYPE_JBROWSE.getValue()));
        jsonObject.put("output", new JSONString("file"));
        jsonObject.put("format", new JSONString("gzip"));
//        jsonObject.put("format", new JSONString("tar.gz"));
        jsonObject.put("exportFullJBrowse", JSONBoolean.getInstance(true));
        jsonObject.put("exportJBrowseSequence", JSONBoolean.getInstance(false));
//        String type = exportPanel.getType();
//        jsonObject.put("type", new JSONString(exportPanel.getType()));
//        jsonObject.put("exportAllSequences", new JSONString(exportPanel.getExportAll().toString()));

        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject responseObject = JSONParser.parseStrict(response.getText()).isObject();
                GWT.log("Responded: "+responseObject.toString());
                String uuid = responseObject.get("uuid").isString().stringValue();
                String exportType = responseObject.get("exportType").isString().stringValue();
//                String sequenceType = responseObject.get("seqType").isString().stringValue();
//                String exportUrl = Annotator.getRootUrl() + "IOService/download?uuid=" + uuid + "&exportType=" + exportType + "&seqType=" + sequenceType+"&format=gzip";
//                String exportUrl = Annotator.getRootUrl() + "IOService/download?uuid=" + uuid + "&exportType=" + exportType + "&format=tar.gz";
                String exportUrl = Annotator.getRootUrl() + "IOService/download?uuid=" + uuid + "&exportType=" + exportType + "&format=gzip";

                Window.Location.assign(exportUrl);
//                exportPanel.setExportUrl(exportUrl);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error: " + exception);
            }
        };


        RestService.sendRequest(requestCallback, "IOService/write", "data=" + jsonObject.toString());
    }

    @UiHandler("saveNewOrganism")
    public void saveNewOrganism(ClickEvent event) {
        String resultMessage = checkForm();
        if (resultMessage == null) {
            newOrganismForm.submit();
            addOrganismFromSequencePanel.hide();
        } else {
            Bootbox.alert(resultMessage);
        }
    }

    /**
     * TODO: check the form ehre
     *
     * @return
     */
    private String checkForm() {
        if(organismUploadName.getText().trim().length()==0){
            return "Organism needs a name";
        }
        if(organismUploadName.getText().contains(" ")){
            return "Organism name must not have spaces";
        }
        SequenceTypeEnum sequenceTypeEnum = SequenceTypeEnum.getSequenceTypeForFile(organismUploadSequence.getFilename());
        if (sequenceTypeEnum == null) {
            String filename = organismUploadSequence.getFilename();
            String suffix = null ;
            if (filename != null && filename.contains(".")) {
               suffix = filename.substring(filename.lastIndexOf("."));
            }
            return "Filename extension not supported"+ (suffix!=null ? "'"+suffix+"'": "" ) ;
        }
        return null;
    }

    @UiHandler("cancelNewOrganism")
    public void setCancelNewOrganism(ClickEvent event) {
        addOrganismFromSequencePanel.hide();
    }

    @UiHandler("obsoleteButton")
    public void handleObsoleteButton(ChangeEvent changeEvent) {
        GWT.log("Handling obsolete change " + obsoleteButton.getValue());
        if (singleSelectionModel.getSelectedObject() != null) {
            GWT.log("Handling obsolete not null " + obsoleteButton.getValue());
            singleSelectionModel.getSelectedObject().setObsolete(obsoleteButton.getValue());
            updateOrganismInfo();
        }
    }

    @UiHandler("newButton")
    public void handleAddNewOrganism(ClickEvent clickEvent) {
        creatingNewOrganism = true;
        clearTextBoxes();
        singleSelectionModel.clear();

        createButton.setText("Create Organism");
        deleteButton.setText("Delete Organism");
        newButton.setEnabled(false);
        uploadOrganismButton.setVisible(false);
//        downloadOrganismButton.setVisible(singleSelectionModel.getSelectedObject()!=null);
        downloadOrganismButton.setVisible(false);
        cancelButton.setEnabled(MainPanel.getInstance().isCurrentUserInstructorOrBetter());
        createButton.setEnabled(MainPanel.getInstance().isCurrentUserInstructorOrBetter());

        createButton.setVisible(MainPanel.getInstance().isCurrentUserInstructorOrBetter());
        cancelButton.setVisible(MainPanel.getInstance().isCurrentUserInstructorOrBetter());
        newButton.setVisible(MainPanel.getInstance().isCurrentUserInstructorOrBetter());
        deleteButton.setVisible(MainPanel.getInstance().isCurrentUserInstructorOrBetter());


        setTextEnabled(MainPanel.getInstance().isCurrentUserInstructorOrBetter());
    }

    @UiHandler("createButton")
    public void handleSaveNewOrganism(ClickEvent clickEvent) {

        if (!sequenceFile.getText().startsWith("/")) {
            errorDialog.show();
            return;
        }

        GWT.log("handleSaveNewOrganism " + publicMode.getValue());
        OrganismInfo organismInfo = new OrganismInfo();
        organismInfo.setName(organismName.getText());
        organismInfo.setDirectory(sequenceFile.getText());
        organismInfo.setGenus(genus.getText());
        organismInfo.setSpecies(species.getText());
        organismInfo.setBlatDb(blatdb.getText());
        organismInfo.setNonDefaultTranslationTable(nonDefaultTranslationTable.getText());
        organismInfo.setPublicMode(publicMode.getValue());
        organismInfo.setObsolete(obsoleteButton.getValue());

        createButton.setEnabled(false);
        createButton.setText("Processing");
        savingNewOrganism = true;

        OrganismRestService.createOrganism(new UpdateInfoListCallback(), organismInfo);
        loadingDialog.show();
    }

    @UiHandler("showOnlyPublicOrganisms")
    public void handleShowOnlyPublicOrganisms(ClickEvent clickEvent) {
        showOnlyPublicOrganisms.setValue(!showOnlyPublicOrganisms.getValue());
        OrganismRestService.loadOrganisms(this.showOnlyPublicOrganisms.getValue(), this.showObsoleteOrganisms.getValue(), new UpdateInfoListCallback());
    }


    @UiHandler("showObsoleteOrganisms")
    public void handleShowObsoleteOrganisms(ClickEvent clickEvent) {
        showObsoleteOrganisms.setValue(!showObsoleteOrganisms.getValue());
        OrganismRestService.loadOrganisms(this.showOnlyPublicOrganisms.getValue(), this.showObsoleteOrganisms.getValue(), new UpdateInfoListCallback());
    }

    @UiHandler("duplicateButton")
    public void handleDuplicateOrganism(ClickEvent clickEvent) {
        duplicateButton.setEnabled(MainPanel.getInstance().isCurrentUserAdmin());
        OrganismInfo organismInfo = singleSelectionModel.getSelectedObject();
        organismInfo.setName("Copy of " + organismInfo.getName());
        OrganismRestService.createOrganism(new UpdateInfoListCallback(), organismInfo);
        setNoSelection();
    }

    @UiHandler("cancelButton")
    public void handleCancelNewOrganism(ClickEvent clickEvent) {
        newButton.setEnabled(MainPanel.getInstance().isCurrentUserAdmin());
        uploadOrganismButton.setVisible(MainPanel.getInstance().isCurrentUserAdmin());
//        downloadOrganismButton.setVisible(singleSelectionModel.getSelectedObject()!=null);
        downloadOrganismButton.setVisible(false);
        deleteButton.setVisible(false);
        createButton.setVisible(false);
        cancelButton.setVisible(false);
        setNoSelection();
    }

    @UiHandler("deleteButton")
    public void handleDeleteOrganism(ClickEvent clickEvent) {
        OrganismInfo organismInfo = singleSelectionModel.getSelectedObject();
        if (organismInfo == null) return;
        if (organismInfo.getNumFeatures() > 0) {
            new ErrorDialog("Cannot delete organism '" + organismInfo.getName() + "'", "You must first remove " + singleSelectionModel.getSelectedObject().getNumFeatures() + " annotations before deleting organism '" + organismInfo.getName() + "'.  Please see our <a href='../WebServices/'>Web Services API</a> from the 'Help' menu for more details on how to perform this operation in bulk.", true, true);
            return;
        }
        Bootbox.confirm("Are you sure you want to delete organism " + singleSelectionModel.getSelectedObject().getName() + "?", new ConfirmCallback() {
            @Override
            public void callback(boolean result) {
                if (result) {
                    deleteButton.setEnabled(false);
                    deleteButton.setText("Processing");
                    savingNewOrganism = true;
                    OrganismRestService.deleteOrganism(new UpdateInfoListCallback(), singleSelectionModel.getSelectedObject());
                    loadingDialog.show();
                }
            }
        });
    }


    @UiHandler("organismName")
    public void handleOrganismNameChange(ChangeEvent changeEvent) {
        if (singleSelectionModel.getSelectedObject() != null) {
            singleSelectionModel.getSelectedObject().setName(organismName.getText());
            updateOrganismInfo();
        }
    }

    @UiHandler("blatdb")
    public void handleBlatDbChange(ChangeEvent changeEvent) {
        if (singleSelectionModel.getSelectedObject() != null) {
            singleSelectionModel.getSelectedObject().setBlatDb(blatdb.getText());
            updateOrganismInfo();
        }
    }

    @UiHandler("nonDefaultTranslationTable")
    public void handleNonDefaultTranslationTable(ChangeEvent changeEvent) {
        if (singleSelectionModel.getSelectedObject() != null) {
            singleSelectionModel.getSelectedObject().setNonDefaultTranslationTable(nonDefaultTranslationTable.getText());
            updateOrganismInfo();
        }
    }

    @UiHandler("publicMode")
    public void handlePublicModeChange(ChangeEvent changeEvent) {
        GWT.log("Handling mode change " + publicMode.getValue());
        if (singleSelectionModel.getSelectedObject() != null) {
            GWT.log("Handling mode not null " + publicMode.getValue());
            singleSelectionModel.getSelectedObject().setPublicMode(publicMode.getValue());
            updateOrganismInfo();
        }
    }

    @UiHandler("species")
    public void handleSpeciesChange(ChangeEvent changeEvent) {
        if (singleSelectionModel.getSelectedObject() != null) {
            singleSelectionModel.getSelectedObject().setSpecies(species.getText());
            updateOrganismInfo();
        }
    }

    @UiHandler("genus")
    public void handleGenusChange(ChangeEvent changeEvent) {
        if (singleSelectionModel.getSelectedObject() != null) {
            singleSelectionModel.getSelectedObject().setGenus(genus.getText());
            updateOrganismInfo();
        }
    }


    @UiHandler("sequenceFile")
    public void handleOrganismDirectory(ChangeEvent changeEvent) {
        if (singleSelectionModel.getSelectedObject() != null) {
            singleSelectionModel.getSelectedObject().setDirectory(sequenceFile.getText());
            updateOrganismInfo();
        }
    }

    private void updateOrganismInfo() {
        updateOrganismInfo(false);
    }

    private void updateOrganismInfo(boolean forceReload) {
        OrganismRestService.updateOrganismInfo(singleSelectionModel.getSelectedObject(), forceReload);
    }


    public void reload() {
        dataGrid.redraw();
    }

    // Clear textboxes and make them unselectable
    private void setNoSelection() {

        clearTextBoxes();
        setTextEnabled(false);

        deleteButton.setVisible(false);
        downloadOrganismButton.setVisible(false);
    }

    private void changeButtonSelection() {
        changeButtonSelection(singleSelectionModel.getSelectedObject() != null);
    }

    // Set the button states/visibility depending on whether there is a selection or not
    private void changeButtonSelection(boolean selection) {
        //Boolean isAdmin = MainPanel.getInstance().isCurrentUserAdmin();
        boolean isAdmin = MainPanel.getInstance().isCurrentUserInstructorOrBetter();
        if (selection) {
            newButton.setEnabled(isAdmin);
            newButton.setVisible(isAdmin);
            deleteButton.setVisible(isAdmin);
            createButton.setVisible(false);
//            downloadOrganismButton.setVisible(true);
            downloadOrganismButton.setVisible(false);
            cancelButton.setVisible(false);
            duplicateButton.setVisible(isAdmin);
            publicMode.setVisible(isAdmin);
            obsoleteButton.setVisible(isAdmin);
        } else {
            newButton.setEnabled(isAdmin);
            newButton.setVisible(isAdmin);
            createButton.setVisible(false);
            downloadOrganismButton.setVisible(false);
            cancelButton.setVisible(false);
            deleteButton.setVisible(false);
            duplicateButton.setVisible(false);
            publicMode.setVisible(false);
            obsoleteButton.setVisible(false);
        }
    }

    //Utility function for toggling the textboxes (gray out)
    private void setTextEnabled(boolean enabled) {
        sequenceFile.setEnabled(enabled);
        organismName.setEnabled(enabled);
        genus.setEnabled(enabled);
        species.setEnabled(enabled);
        blatdb.setEnabled(enabled);
        nonDefaultTranslationTable.setEnabled(enabled);
        publicMode.setEnabled(enabled);
        obsoleteButton.setEnabled(enabled);
    }

    //Utility function for clearing the textboxes ("")
    private void clearTextBoxes() {
        organismName.setText("");
        sequenceFile.setText("");
        genus.setText("");
        species.setText("");
        blatdb.setText("");
        nonDefaultTranslationTable.setText("");
        publicMode.setValue(false);
        obsoleteButton.setValue(false);
    }

}
