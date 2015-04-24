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

    private ListDataProvider<OrganismInfo> dataProvider = new ListDataProvider<>();
    private List<OrganismInfo> organismInfoList = dataProvider.getList();
    private final SingleSelectionModel<OrganismInfo> singleSelectionModel = new SingleSelectionModel<>();

    public OrganismPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));

        TextColumn<OrganismInfo> organismNameColumn = new TextColumn<OrganismInfo>() {
            @Override
            public String getValue(OrganismInfo employee) {
                return employee.getName();
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
//                dataProvider.setList(organismChangeEvent.organismInfoList);
            }
        });

        dataGrid.setLoadingIndicator(new HTML("Calculating Annotations ... "));
        dataGrid.addColumn(organismNameColumn, "Name");
        dataGrid.addColumn(annotationsNameColumn, "Annotations");
        dataGrid.addColumn(sequenceColumn, "Ref Sequences");


        singleSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if (singleSelectionModel.getSelectedObject() != null) {
                    setSelectedInfo(singleSelectionModel.getSelectedObject());
                    setDefaultButtonState(singleSelectionModel.getSelectedObject() != null);
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
                        // TODO: set the organism here
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

    public void setSelectedInfo(OrganismInfo organismInfo){
        if(organismInfo==null) {
            setNoSelection();
            return;
        }
        organismName.setText(organismInfo.getName());
        organismName.setEnabled(true);
        blatdb.setText(organismInfo.getBlatDb());
        blatdb.setEnabled(true);
        genus.setText(organismInfo.getGenus());
        genus.setEnabled(true);
        species.setText(organismInfo.getSpecies());
        species.setEnabled(true);
        sequenceFile.setText(organismInfo.getDirectory());
        sequenceFile.setEnabled(true);
        annotationCount.setText(organismInfo.getNumFeatures().toString());
        deleteButton.setVisible(true);
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

        private boolean clearSelections = false ;

        public UpdateInfoListCallback(){
            this(false);
        }

        public UpdateInfoListCallback(boolean clearSelections){
            this.clearSelections = clearSelections ;
        }

        @Override
        public void onResponseReceived(Request request, Response response) {
            List<OrganismInfo> organismInfoList = OrganismInfoConverter.convertJSONStringToOrganismInfoList(response.getText());
            dataGrid.setSelectionModel(singleSelectionModel);
//            if(clearSelections){
//                clearSelections();
//            }
            MainPanel.getInstance().getOrganismInfoList().clear();
            MainPanel.getInstance().getOrganismInfoList().addAll(organismInfoList);
            setDefaultButtonState(singleSelectionModel.getSelectedObject() != null);
            OrganismChangeEvent organismChangeEvent = new OrganismChangeEvent(organismInfoList);
            organismChangeEvent.setAction(OrganismChangeEvent.Action.LOADED_ORGANISMS);
            Annotator.eventBus.fireEvent(organismChangeEvent);
        }

        @Override
        public void onError(Request request, Throwable exception) {
            Window.alert("problem handling organism: "+exception);
        }
    }

    public void clearSelections(){
        singleSelectionModel.clear();
        organismName.setText("");
        genus.setText("");
        species.setText("");
        sequenceFile.setText("");
        blatdb.setText("");
        validDirectory.setVisible(false);
        newButton.setEnabled(false);
    }

    @UiHandler("newButton")
    public void handleAddNewOrganism(ClickEvent clickEvent) {
        singleSelectionModel.clear();
        setNewOrganismButtonState();
    }

    @UiHandler("createButton")
    public void handleSaveNewOrganism(ClickEvent clickEvent) {
        setDefaultButtonState(singleSelectionModel.getSelectedObject() != null);
        OrganismInfo organismInfo = new OrganismInfo();
        organismInfo.setName(organismName.getText());
        organismInfo.setDirectory(sequenceFile.getText());
        organismInfo.setGenus(genus.getText());
        organismInfo.setSpecies(species.getText());
        organismInfo.setBlatDb(blatdb.getText());
        createButton.setEnabled(false);
        createButton.setText("Processing");
        OrganismRestService.createOrganism(new UpdateInfoListCallback(true), organismInfo);
    }

    @UiHandler("cancelButton")
    public void handleCancelNewOrganism(ClickEvent clickEvent) {
        organismName.setText("");
        sequenceFile.setText("");
        species.setText("");
        genus.setText("");
        blatdb.setText("");
        dataGrid.setSelectionModel(singleSelectionModel);
        newButton.setEnabled(true);
        setSelectedInfo(singleSelectionModel.getSelectedObject());
        setDefaultButtonState(singleSelectionModel.getSelectedObject() != null);
    }

    @UiHandler("deleteButton")
    public void handleDeleteOrganism(ClickEvent clickEvent) {
        if(Window.confirm("Delete organism: "+singleSelectionModel.getSelectedObject().getName())){
            deleteButton.setEnabled(false);
            OrganismRestService.deleteOrganism(new UpdateInfoListCallback(true), singleSelectionModel.getSelectedObject());
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
//        List<OrganismInfo> trackInfoList = dataProvider.getList();
//        OrganismRestService.loadOrganisms(trackInfoList);
//
//        return trackInfoList;
    }

//    public List<OrganismInfo> reload() {
//        List<OrganismInfo> trackInfoList = dataProvider.getList();
//        OrganismRestService.loadOrganisms(trackInfoList);
//
//        return trackInfoList;
//    }
    // Clear textboxes and make them unselectable
    private void setNoSelection() {
        organismName.setText("");
        sequenceFile.setText("");
        species.setText("");
        genus.setText("");
        blatdb.setText("");

        sequenceFile.setEnabled(false);
        organismName.setEnabled(false);
        genus.setEnabled(false);
        species.setEnabled(false);
        blatdb.setEnabled(false);

        annotationCount.setText("");
        validDirectory.setVisible(false);
        deleteButton.setVisible(false);
    }

    // Set the button states/visibility depending on whether there is a selection or not
    public void setDefaultButtonState(boolean selection){
        if(selection){
            newButton.setEnabled(true);
            newButton.setVisible(true);
            createButton.setVisible(false);
            cancelButton.setVisible(false);
            deleteButton.setVisible(true);
        }
        else{
            newButton.setEnabled(true);
            newButton.setVisible(true);
            createButton.setVisible(false);
            cancelButton.setVisible(false);
            deleteButton.setVisible(false);
        }
    }

    public void setNewOrganismButtonState(){
        createButton.setText("Create Organism");
        singleSelectionModel.clear();
        newButton.setEnabled(false);
        newButton.setVisible(true);
        createButton.setVisible(true);
        createButton.setEnabled(true);
        cancelButton.setVisible(true);
        cancelButton.setEnabled(true);
        deleteButton.setVisible(false);


        organismName.setText("");
        sequenceFile.setText("");
        genus.setText("");
        species.setText("");
        blatdb.setText("");
        validDirectory.setVisible(false);
        organismName.setEnabled(true);
        sequenceFile.setEnabled(true);
        genus.setEnabled(true);
        species.setEnabled(true);
        blatdb.setEnabled(true);
    }
    public void setThinkingInterface(){
    }

    public void unsetThinkingInterface(){
    }
}
