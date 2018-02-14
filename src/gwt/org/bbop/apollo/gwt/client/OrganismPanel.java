package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.OrganismInfoConverter;
import org.bbop.apollo.gwt.client.event.OrganismChangeEvent;
import org.bbop.apollo.gwt.client.event.OrganismChangeEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.OrganismRestService;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.bootbox.client.callback.ConfirmCallback;

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
    TextBox genus;
    @UiField
    TextBox species;
    @UiField
    TextBox sequenceFile;
    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<OrganismInfo> dataGrid = new DataGrid<OrganismInfo>(10, tablecss);
    @UiField
    Button newButton;
    @UiField
    Button createButton;
    @UiField
    Button cancelButton;
    @UiField
    Button deleteButton;
    @UiField(provided = true)
    WebApolloSimplePager pager = new WebApolloSimplePager(WebApolloSimplePager.TextLocation.CENTER);
    @UiField
    TextBox nonDefaultTranslationTable;

    boolean creatingNewOrganism = false; // a special flag for handling the clearSelection event when filling out new organism info
    boolean savingNewOrganism = false; // a special flag for handling the clearSelection event when filling out new organism info

    final LoadingDialog loadingDialog;
    final ErrorDialog errorDialog;
    private ListDataProvider<OrganismInfo> dataProvider = new ListDataProvider<>();
    private List<OrganismInfo> organismInfoList = dataProvider.getList();
    private final SingleSelectionModel<OrganismInfo> singleSelectionModel = new SingleSelectionModel<>();

    public OrganismPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
        loadingDialog = new LoadingDialog("Processing ...", null, false);
        errorDialog = new ErrorDialog("Error", "Organism directory must be an absolute path pointing to 'trackList.json'", false, true);

        TextColumn<OrganismInfo> organismNameColumn = new TextColumn<OrganismInfo>() {
            @Override
            public String getValue(OrganismInfo organism) {
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
//        dataGrid.addColumn(sequenceColumn, "Ref Sequences");
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
                    String orgId = singleSelectionModel.getSelectedObject().getId();
                    if (!MainPanel.getInstance().getCurrentOrganism().getId().equals(orgId)) {
                        OrganismRestService.switchOrganismById(orgId);
                    }
                }
            }
        }, DoubleClickEvent.getType());

        List<OrganismInfo> trackInfoList = dataProvider.getList();

        ColumnSortEvent.ListHandler<OrganismInfo> sortHandler = new ColumnSortEvent.ListHandler<OrganismInfo>(trackInfoList);
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
        Boolean isEditable = organismInfo.isEditable()||MainPanel.getInstance().isCurrentUserAdmin();

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

        nonDefaultTranslationTable.setText(organismInfo.getNonDefaultTranslationTable());
        nonDefaultTranslationTable.setEnabled(isEditable);

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


    @UiHandler("newButton")
    public void handleAddNewOrganism(ClickEvent clickEvent) {
        creatingNewOrganism = true;
        clearTextBoxes();
        singleSelectionModel.clear();

        createButton.setText("Create Organism");
        deleteButton.setText("Delete Organism");
        newButton.setEnabled(false);
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

        createButton.setEnabled(false);
        createButton.setText("Processing");
        savingNewOrganism = true;

        OrganismRestService.createOrganism(new UpdateInfoListCallback(), organismInfo);
        loadingDialog.show();
    }


    @UiHandler("cancelButton")
    public void handleCancelNewOrganism(ClickEvent clickEvent) {
        newButton.setEnabled(MainPanel.getInstance().isCurrentUserAdmin());
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
            new ErrorDialog("Cannot delete organism '" + organismInfo.getName() + "'", "You must first remove " + singleSelectionModel.getSelectedObject().getNumFeatures() + " annotations before deleting organism '"+organismInfo.getName()+"'.  Please see our <a href='../WebServices/'>Web Services API</a> from the 'Help' menu for more details on how to perform this operation in bulk.", true, true);
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
    }

    private void changeButtonSelection() {
        changeButtonSelection(singleSelectionModel.getSelectedObject() != null);
    }

    // Set the button states/visibility depending on whether there is a selection or not
    private void changeButtonSelection(boolean selection) {
        //Boolean isAdmin = MainPanel.getInstance().isCurrentUserAdmin();
        Boolean isAdmin = MainPanel.getInstance().isCurrentUserInstructorOrBetter();
        if (selection) {
            newButton.setEnabled(isAdmin);
            newButton.setVisible(isAdmin);
            deleteButton.setVisible(isAdmin);
            createButton.setVisible(false);
            cancelButton.setVisible(false);
        } else {
            newButton.setEnabled(isAdmin);
            newButton.setVisible(isAdmin);
            createButton.setVisible(false);
            cancelButton.setVisible(false);
            deleteButton.setVisible(false);
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
    }

}
