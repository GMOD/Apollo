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
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.OrganismInfoConverter;
import org.bbop.apollo.gwt.client.event.OrganismChangeEvent;
import org.bbop.apollo.gwt.client.event.OrganismChangeEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.OrganismRestService;
import org.bbop.apollo.gwt.client.rest.RestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.TextArea;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.bootbox.client.callback.ConfirmCallback;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by ndunn on 12/17/14.
 */
public class SearchPanel extends Composite {


    interface OrganismBrowserPanelUiBinder extends UiBinder<Widget, SearchPanel> {
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
    @UiField(provided = true)
    WebApolloSimplePager pager = new WebApolloSimplePager(WebApolloSimplePager.TextLocation.CENTER);
    @UiField
    TextBox nonDefaultTranslationTable;
    @UiField
    org.gwtbootstrap3.client.ui.Label organismIdLabel;
    @UiField
    CheckBox showOnlyPublicOrganisms;
    @UiField
    Button searchGenomes;
    @UiField
    Button uploadOrganismButton;
    @UiField
    Button downloadOrganismButton;
    @UiField
    static TextArea sequenceSearchBox;

    private boolean creatingNewOrganism = false; // a special flag for handling the clearSelection event when filling out new organism info
    private boolean savingNewOrganism = false; // a special flag for handling the clearSelection event when filling out new organism info

    final private LoadingDialog loadingDialog;
    final private ErrorDialog errorDialog;

    static private ListDataProvider<OrganismInfo> dataProvider = new ListDataProvider<>();
    private static List<OrganismInfo> organismInfoList = new ArrayList<>();
    private static List<OrganismInfo> filteredOrganismInfoList = dataProvider.getList();


    private final SingleSelectionModel<OrganismInfo> singleSelectionModel = new SingleSelectionModel<>();

    public SearchPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
        loadingDialog = new LoadingDialog("Processing ...", null, false);
        errorDialog = new ErrorDialog("Error", "Organism directory must be an absolute path pointing to 'trackList.json'", false, true);
        sequenceSearchBox.setVisibleLines(50);

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
//                organismInfoList.addAll(MainPanel.getInstance().getOrganismInfoList());
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


//        singleSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
//            @Override
//            public void onSelectionChange(SelectionChangeEvent event) {
//                if (!creatingNewOrganism) {
//                    loadOrganismInfo();
//                    changeButtonSelection();
//                } else {
//                    creatingNewOrganism = false;
//                }
//            }
//        });
//        dataGrid.setSelectionModel(singleSelectionModel);

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


    @UiHandler("sequenceSearchBox")
    public void doSearch(KeyUpEvent keyUpEvent) {
        filterList();
    }

    static void filterList() {
        String text = sequenceSearchBox.getText();
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
//        loadOrganismInfo(singleSelectionModel.getSelectedObject());
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

        if (organismInfo.getNumFeatures() == 0) {
          sequenceFile.setText(organismInfo.getDirectory() );
          sequenceFile.setEnabled(isEditable);
        }
        else{
          sequenceFile.setText(organismInfo.getDirectory() + " (remove " + organismInfo.getNumFeatures() + "annotations to change)" );
          sequenceFile.setEnabled(false);
        }

        publicMode.setValue(organismInfo.getPublicMode());
        publicMode.setEnabled(isEditable);

        obsoleteButton.setValue(organismInfo.getObsolete());
        obsoleteButton.setEnabled(isEditable);

        organismIdLabel.setHTML("Internal ID: " + organismInfo.getId());

        nonDefaultTranslationTable.setText(organismInfo.getNonDefaultTranslationTable());
        nonDefaultTranslationTable.setEnabled(isEditable);

        downloadOrganismButton.setVisible(false);
    }

    private class UpdateInfoListCallback implements RequestCallback {

        @Override
        public void onResponseReceived(Request request, Response response) {
            JSONValue j = JSONParser.parseStrict(response.getText());
            JSONObject obj = j.isObject();
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

    @UiHandler("obsoleteButton")
    public void handleObsoleteButton(ChangeEvent changeEvent) {
        GWT.log("Handling obsolete change " + obsoleteButton.getValue());
        if (singleSelectionModel.getSelectedObject() != null) {
            GWT.log("Handling obsolete not null " + obsoleteButton.getValue());
            singleSelectionModel.getSelectedObject().setObsolete(obsoleteButton.getValue());
            updateOrganismInfo();
        }
    }

    @UiHandler("showOnlyPublicOrganisms")
    public void handleShowOnlyPublicOrganisms(ClickEvent clickEvent) {
//        showOnlyPublicOrganisms.setValue(!showOnlyPublicOrganisms.getValue());
//        OrganismRestService.loadOrganisms(this.showOnlyPublicOrganisms.getValue(), this.showObsoleteOrganisms.getValue(), new UpdateInfoListCallback());
    }


    @UiHandler("searchGenomes")
    public void handleShowObsoleteOrganisms(ClickEvent clickEvent) {
//        showObsoleteOrganisms.setValue(!showObsoleteOrganisms.getValue());
//        OrganismRestService.loadOrganisms(this.showOnlyPublicOrganisms.getValue(), this.showObsoleteOrganisms.getValue(), new UpdateInfoListCallback());
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
      try {
        if (singleSelectionModel.getSelectedObject() != null) {
          Bootbox.confirm("Changing the source directory will remove all existing annotations.  Continue?", new ConfirmCallback() {
            @Override
            public void callback(boolean result) {
              if(result) {
                singleSelectionModel.getSelectedObject().setDirectory(sequenceFile.getText());
                updateOrganismInfo();
              }
            }
          });
        }
      } catch (Exception e) {
        Bootbox.alert("There was a problem updating the organism: "+e.getMessage());
        Bootbox.confirm("Reload", new ConfirmCallback() {
          @Override
          public void callback(boolean result) {
            if(result) Window.Location.reload();
          }
        });
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
            downloadOrganismButton.setVisible(false);
            publicMode.setVisible(isAdmin);
            obsoleteButton.setVisible(isAdmin);
        } else {
            downloadOrganismButton.setVisible(false);
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
