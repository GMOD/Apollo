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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
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
import org.gwtbootstrap3.client.ui.InputGroupAddon;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.IconType;

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
    TextBox genus;
    @UiField
    TextBox species;
    @UiField
    InputGroupAddon annotationCount;
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
    @UiField
    InputGroupAddon validDirectory;
    @UiField
    Button reloadButton;

    boolean creatingNewOrganism=false;

    private ListDataProvider<OrganismInfo> dataProvider = new ListDataProvider<>();
    private List<OrganismInfo> organismInfoList = dataProvider.getList();
    private final SingleSelectionModel<OrganismInfo> singleSelectionModel = new SingleSelectionModel<>();

    public OrganismPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));

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
        dataGrid.addColumn(sequenceColumn, "Ref Sequences");


        singleSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if(!creatingNewOrganism) {
                    loadOrganismInfo();
                    changeButtonSelection();
                }
                else {
                    creatingNewOrganism=false;
                }
            }
        });
        dataGrid.setSelectionModel(singleSelectionModel);

        dataProvider.addDataDisplay(dataGrid);


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
    public void loadOrganismInfo(OrganismInfo organismInfo){
        if(organismInfo==null) {
            setNoSelection();
            return;
        }
        organismName.setEnabled(true);
        blatdb.setEnabled(true);
        genus.setEnabled(true);
        species.setEnabled(true);

        organismName.setText(organismInfo.getName());
        blatdb.setText(organismInfo.getBlatDb());
        genus.setText(organismInfo.getGenus());
        species.setText(organismInfo.getSpecies());
        sequenceFile.setText(organismInfo.getDirectory());
        annotationCount.setText(organismInfo.getNumFeatures().toString());

        deleteButton.setVisible(true);
        sequenceFile.setEnabled(true);
        deleteButton.setEnabled(true);


        if(organismInfo.getValid()==null){
            validDirectory.setIcon(IconType.QUESTION);
            validDirectory.setColor("Red");
        }
        else if(organismInfo.getValid()){
            validDirectory.setIcon(IconType.CHECK);
            validDirectory.setColor("Green");
        }
        else{
            validDirectory.setIcon(IconType.XING);
            validDirectory.setColor("Red");
        }
        validDirectory.setVisible(true);
    }

    private class UpdateInfoListCallback implements  RequestCallback{

        @Override
        public void onResponseReceived(Request request, Response response) {
            JSONValue j=JSONParser.parseStrict(response.getText());
            JSONObject o=j.isObject();
            if(o.get("error")!=null) {
                Window.alert(o.get("error").isString().stringValue());
                changeButtonSelection();
                setTextEnabled(false);
                clearTextBoxes();
            }
            else {
                List<OrganismInfo> organismInfoList = OrganismInfoConverter.convertJSONStringToOrganismInfoList(response.getText());
                dataGrid.setSelectionModel(singleSelectionModel);
                MainPanel.getInstance().getOrganismInfoList().clear();
                MainPanel.getInstance().getOrganismInfoList().addAll(organismInfoList);
                changeButtonSelection();
                OrganismChangeEvent organismChangeEvent = new OrganismChangeEvent(organismInfoList);
                organismChangeEvent.setAction(OrganismChangeEvent.Action.LOADED_ORGANISMS);
                Annotator.eventBus.fireEvent(organismChangeEvent);
            }

        }

        @Override
        public void onError(Request request, Throwable exception) {
            Window.alert("problem handling organism: "+exception);
        }
    }

    public void clearSelections(){
        singleSelectionModel.clear();
        clearTextBoxes();
        validDirectory.setVisible(false);
        newButton.setEnabled(false);
    }

    @UiHandler("newButton")
    public void handleAddNewOrganism(ClickEvent clickEvent) {
        creatingNewOrganism=true;
        clearTextBoxes();
        annotationCount.setText("");
        singleSelectionModel.clear();

        createButton.setText("Create Organism");
        newButton.setEnabled(false);
        cancelButton.setEnabled(true);
        createButton.setEnabled(true);

        createButton.setVisible(true);
        cancelButton.setVisible(true);
        newButton.setVisible(true);
        deleteButton.setVisible(false);
        validDirectory.setVisible(false);


        setTextEnabled(true);
    }

    @UiHandler("createButton")
    public void handleSaveNewOrganism(ClickEvent clickEvent) {
        changeButtonSelection();
        OrganismInfo organismInfo = new OrganismInfo();
        organismInfo.setName(organismName.getText());
        organismInfo.setDirectory(sequenceFile.getText());
        organismInfo.setGenus(genus.getText());
        organismInfo.setSpecies(species.getText());
        organismInfo.setBlatDb(blatdb.getText());

        createButton.setEnabled(false);
        createButton.setText("Processing");
        OrganismRestService.createOrganism(new UpdateInfoListCallback(), organismInfo);
    }

    @UiHandler("cancelButton")
    public void handleCancelNewOrganism(ClickEvent clickEvent) {
        newButton.setEnabled(true);
        deleteButton.setVisible(false);
        createButton.setVisible(false);
        cancelButton.setVisible(false);
        setNoSelection();
    }

    @UiHandler("deleteButton")
    public void handleDeleteOrganism(ClickEvent clickEvent) {
        if(Window.confirm("Delete organism: "+singleSelectionModel.getSelectedObject().getName())){
            deleteButton.setEnabled(false);
            OrganismRestService.deleteOrganism(new UpdateInfoListCallback(), singleSelectionModel.getSelectedObject());
            setNoSelection();
        }
    }


    @UiHandler("organismName")
    public void handleOrganismNameChange(ChangeEvent changeEvent) {
        if(singleSelectionModel.getSelectedObject()!=null) {
            singleSelectionModel.getSelectedObject().setName(organismName.getText());
            updateOrganismInfo();
        }
    }

    @UiHandler("blatdb")
    public void handleBlatDbChange(ChangeEvent changeEvent) {
        if(singleSelectionModel.getSelectedObject()!=null) {
            singleSelectionModel.getSelectedObject().setBlatDb(blatdb.getText());
            updateOrganismInfo();
        }
    }


    @UiHandler("species")
    public void handleSpeciesChange(ChangeEvent changeEvent) {
        if(singleSelectionModel.getSelectedObject()!=null) {
            singleSelectionModel.getSelectedObject().setSpecies(species.getText());
            updateOrganismInfo();
        }
    }

    @UiHandler("genus")
    public void handleGenusChange(ChangeEvent changeEvent) {
        if(singleSelectionModel.getSelectedObject()!=null) {
            singleSelectionModel.getSelectedObject().setGenus(genus.getText());
            updateOrganismInfo();
        }
    }


    @UiHandler("sequenceFile")
    public void handleOrganismDirectory(ChangeEvent changeEvent) {
        if(singleSelectionModel.getSelectedObject()!=null) {
            singleSelectionModel.getSelectedObject().setDirectory(sequenceFile.getText());
            updateOrganismInfo();
        }
    }

    @UiHandler("reloadButton")
    public void handleReloadButton(ClickEvent clickEvent) {
        updateOrganismInfo(true);
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

        annotationCount.setText("");
        validDirectory.setVisible(false);
        deleteButton.setVisible(false);
    }

    public void changeButtonSelection() {
        changeButtonSelection(singleSelectionModel.getSelectedObject()!=null);
    }
    // Set the button states/visibility depending on whether there is a selection or not
    public void changeButtonSelection(boolean selection){
        if(selection){
            newButton.setEnabled(true);
            newButton.setVisible(true);
            deleteButton.setVisible(true);
            createButton.setVisible(false);
            cancelButton.setVisible(false);
        }
        else{
            newButton.setEnabled(true);
            newButton.setVisible(true);
            createButton.setVisible(false);
            cancelButton.setVisible(false);
            deleteButton.setVisible(false);
        }
    }

    //Utility function for toggling the textboxes (gray out)
    public void setTextEnabled(boolean enabled) {
        sequenceFile.setEnabled(enabled);
        organismName.setEnabled(enabled);
        genus.setEnabled(enabled);
        species.setEnabled(enabled);
        blatdb.setEnabled(enabled);
    }
    //Utility function for clearing the textboxes ("")
    public void clearTextBoxes() {
        organismName.setText("");
        sequenceFile.setText("");
        genus.setText("");
        species.setText("");
        blatdb.setText("");
    }


    public void setThinkingInterface(){
    }

    public void unsetThinkingInterface(){
    }
}
