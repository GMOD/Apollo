package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.AbstractSafeHtmlRenderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.event.OrganismChangeEvent;
import org.bbop.apollo.gwt.client.event.OrganismChangeEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.OrganismRestService;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.Button;

import java.util.Comparator;
import java.util.List;

/**
 * Created by ndunn on 12/17/14.
 */
public class OrganismPanel extends Composite {


    interface OrganismBrowserPanelUiBinder extends UiBinder<Widget, OrganismPanel> {
    }

    private OrganismInfo selectedOrganismInfo;

    private static OrganismBrowserPanelUiBinder ourUiBinder = GWT.create(OrganismBrowserPanelUiBinder.class);
    @UiField
    org.gwtbootstrap3.client.ui.TextBox organismName;
//    @UiField
//    InputGroupAddon trackCount;
    @UiField
    InputGroupAddon annotationCount;
    @UiField
    org.gwtbootstrap3.client.ui.TextBox sequenceFile;

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

    private PopupPanel popupPanel ;



    private ListDataProvider<OrganismInfo> dataProvider = new ListDataProvider<>();
    private final SingleSelectionModel<OrganismInfo> singleSelectionModel = new SingleSelectionModel<>();

    public OrganismPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));



        TextColumn<OrganismInfo> organismNameColumn = new TextColumn<OrganismInfo>() {
            @Override
            public String getValue(OrganismInfo employee) {
                return employee.getName();
            }
        };
        organismNameColumn.setSortable(true);

        Column<OrganismInfo, Number> annotationsNameColumn = new Column<OrganismInfo, Number>(new NumberCell()) {
            @Override
            public Integer getValue(OrganismInfo object) {
                return object.getNumFeatures();
            }
        };
        annotationsNameColumn.setSortable(true);
        Column<OrganismInfo, Number> sequenceColumn = new Column<OrganismInfo, Number>(new NumberCell()) {
            @Override
            public Integer getValue(OrganismInfo object) {
                return object.getNumSequences();
            }
        };
        sequenceColumn.setSortable(true);

        Annotator.eventBus.addHandler(OrganismChangeEvent.TYPE, new OrganismChangeEventHandler() {
            @Override
            public void onOrganismChanged(OrganismChangeEvent organismChangeEvent) {
                dataProvider.setList(organismChangeEvent.organismInfoList);
            }
        });

//        Column<OrganismInfo, Number> tracksColumn = new Column<OrganismInfo, Number>(new NumberCell()) {
//            @Override
//            public Integer getValue(OrganismInfo object) {
//                return object.getNumTracks();
//            }
//        };
//        tracksColumn.setSortable(true);

//        SafeHtmlRenderer<String> anchorRenderer = new AbstractSafeHtmlRenderer<String>() {
//            @Override
//            public SafeHtml render(String object) {
//                SafeHtmlBuilder sb = new SafeHtmlBuilder();
//                sb.appendHtmlConstant("<a href=\"javascript:;\">Select</a>");
//                return sb.toSafeHtml();
//            }
//        };

//        Column<OrganismInfo, String> actionColumn = new Column<OrganismInfo, String>(new ClickableTextCell(anchorRenderer)) {
//            @Override
//            public String getValue(OrganismInfo employee) {
//                return "Select";
//            }
//        };

        dataGrid.setLoadingIndicator(new HTML("Calculating Annotations ... "));

        dataGrid.addColumn(organismNameColumn, "Name");
        dataGrid.addColumn(annotationsNameColumn, "Annotations");
//        dataGrid.addColumn(tracksColumn, "Tracks");
        dataGrid.addColumn(sequenceColumn, "Sequences");
//        dataGrid.addColumn(actionColumn, "Action");


        singleSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                selectedOrganismInfo = singleSelectionModel.getSelectedObject();
                setSelectedInfo(selectedOrganismInfo);
                setDefaultButtonState();
            }
        });
        dataGrid.setSelectionModel(singleSelectionModel);

        dataProvider.addDataDisplay(dataGrid);

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
//        sortHandler.setComparator(tracksColumn, new Comparator<OrganismInfo>() {
//            @Override
//            public int compare(OrganismInfo o1, OrganismInfo o2) {
//                return o1.getNumTracks() - o2.getNumTracks();
//            }
//        });

    }

    public void setSelectedInfo(OrganismInfo organismInfo){
        if(organismInfo==null) return ;
        organismName.setText(organismInfo.getName());
        sequenceFile.setText(organismInfo.getDirectory());
        annotationCount.setText(organismInfo.getNumFeatures().toString());
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
            GWT.log("recived response: "+response.getText());
            List<OrganismInfo> organismInfoList = OrganismRestService.convertJSONStringToOrganismInfoList(response.getText());
            GWT.log("converted responsde : " + organismInfoList.size());
            dataGrid.setSelectionModel(singleSelectionModel);
            if(clearSelections){
                singleSelectionModel.clear();
                organismName.setText("");
                sequenceFile.setText("");
            }
            setDefaultButtonState();
            OrganismChangeEvent organismChangeEvent = new OrganismChangeEvent(organismInfoList);
            Annotator.eventBus.fireEvent(organismChangeEvent);
        }

        @Override
        public void onError(Request request, Throwable exception) {
            Window.alert("problem handling organism: "+exception);
        }
    }

    @UiHandler("newButton")
    public void handleAddNewOrganism(ClickEvent clickEvent) {
        selectedOrganismInfo = null ;
        organismName.setText("");
        sequenceFile.setText("");
        dataGrid.setSelectionModel(new NoSelectionModel<OrganismInfo>());
        setNewOrganismButtonState();
//        selectedOrganismInfo.setName(organismName.getText());
//        updateOrganismInfo();
    }

    @UiHandler("createButton")
    public void handleSaveNewOrganism(ClickEvent clickEvent) {
        setDefaultButtonState();
        OrganismInfo organismInfo = new OrganismInfo();
        organismInfo.setName(organismName.getText());
        organismInfo.setDirectory(sequenceFile.getText());
        OrganismRestService.createOrganism(new UpdateInfoListCallback(true), organismInfo);
    }

    @UiHandler("cancelButton")
    public void handleCancelNewOrganism(ClickEvent clickEvent) {
        organismName.setText("");
        sequenceFile.setText("");
        dataGrid.setSelectionModel(singleSelectionModel);
        setSelectedInfo(selectedOrganismInfo);

        setDefaultButtonState();
    }

    @UiHandler("deleteButton")
    public void handleDeleteOrganism(ClickEvent clickEvent) {
        if(selectedOrganismInfo==null) return ;

        if(Window.confirm("Delete organism: "+selectedOrganismInfo.getName())){
            OrganismRestService.deleteOrganism(new UpdateInfoListCallback(true),selectedOrganismInfo);
        }
    }


    @UiHandler("organismName")
    public void handleOrganismNameChange(ChangeEvent changeEvent) {
        selectedOrganismInfo.setName(organismName.getText());
        updateOrganismInfo();
    }

    @UiHandler("sequenceFile")
    public void handleOrganismDirectory(ChangeEvent changeEvent) {
        selectedOrganismInfo.setDirectory(sequenceFile.getText());
        updateOrganismInfo();
    }


    private void updateOrganismInfo() {
        OrganismRestService.updateOrganismInfo(selectedOrganismInfo);
    }


    public List<OrganismInfo> reload() {
        List<OrganismInfo> trackInfoList = dataProvider.getList();
        OrganismRestService.loadOrganisms(trackInfoList);

        return trackInfoList;
    }

    public void setDefaultButtonState(){
        if(singleSelectionModel.getSelectedSet().size()>0){
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
        newButton.setEnabled(false);
        newButton.setVisible(true);
        createButton.setVisible(true);
        createButton.setEnabled(true);
        cancelButton.setVisible(false);
        cancelButton.setEnabled(true);
        deleteButton.setVisible(false);
    }

    public void setThinkingInterface(){
    }

    public void unsetThinkingInterface(){
    }
}