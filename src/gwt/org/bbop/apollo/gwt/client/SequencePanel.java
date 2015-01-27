package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.*;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.event.SequenceLoadEvent;
import org.bbop.apollo.gwt.client.event.SequenceLoadEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.OrganismRestService;
import org.bbop.apollo.gwt.client.rest.SequenceRestService;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ButtonType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Created by ndunn on 12/17/14.
 */
public class SequencePanel extends Composite {

    interface SequencePanelUiBinder extends UiBinder<Widget, SequencePanel> {
    }

    private static SequencePanelUiBinder ourUiBinder = GWT.create(SequencePanelUiBinder.class);
    @UiField
    TextBox minFeatureLength;
    @UiField
    TextBox maxFeatureLength;
    @UiField
    ListBox organismList;

    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<SequenceInfo> dataGrid = new DataGrid<SequenceInfo>(20, tablecss);
    @UiField(provided = true)
    SimplePager pager = null;

    @UiField
    HTML sequenceName;
    //    @UiField
//    HTML sequenceStart;
//    @UiField
//    HTML sequenceStop;
    @UiField
    Button exportAllButton;
    @UiField
    Button exportSelectedButton;
    @UiField
    Button exportSingleButton;
    @UiField
    TextBox nameSearchBox;
    @UiField
    org.gwtbootstrap3.client.ui.Label viewableLabel;
    @UiField
    HTML sequenceLength;
    @UiField
    Button exportGff3Button;
    @UiField
    Button exportFastaButton;
    @UiField
    Button exportChadoButton;
    @UiField
    Button selectSelectedButton;

    private ListDataProvider<SequenceInfo> dataProvider = new ListDataProvider<>();
    private List<SequenceInfo> sequenceInfoList = new ArrayList<>();
    private List<SequenceInfo> filteredSequenceList = dataProvider.getList();
    private MultiSelectionModel<SequenceInfo> multiSelectionModel = new MultiSelectionModel<SequenceInfo>();
    private SequenceInfo selectedSequenceInfo = null;
    private Integer selectedCount = 0 ;

    public SequencePanel() {
        pager = new SimplePager(SimplePager.TextLocation.CENTER);
        initWidget(ourUiBinder.createAndBindUi(this));

        dataGrid.setWidth("100%");
        dataGrid.setEmptyTableWidget(new Label("Loading"));

        Column<SequenceInfo, Boolean> selectColumn = new Column<SequenceInfo, Boolean>(new CheckboxCell(true, false)) {
            @Override
            public Boolean getValue(SequenceInfo object) {
                return object.getSelected();
            }
        };
        selectColumn.setSortable(true);

        selectColumn.setFieldUpdater(new FieldUpdater<SequenceInfo, Boolean>() {
            @Override
            public void update(int index, SequenceInfo object, Boolean value) {
                selectedCount += value ? 1 : -1 ;
                if(selectedCount>0){
                }
                else{
                    selectedCount=0;
                }
                object.setSelected(value);
                updatedExportSelectedButton();
            }
        });

        TextColumn<SequenceInfo> nameColumn = new TextColumn<SequenceInfo>() {
            @Override
            public String getValue(SequenceInfo employee) {
                return employee.getName();
            }
        };
        nameColumn.setSortable(true);

        Column<SequenceInfo, Number> lengthColumn = new Column<SequenceInfo, Number>(new NumberCell()) {
            @Override
            public Integer getValue(SequenceInfo object) {
                return object.getLength();
            }
        };
        lengthColumn.setSortable(true);


        dataGrid.addColumn(selectColumn, "Selected");
        dataGrid.addColumn(nameColumn, "Name");
        dataGrid.addColumn(lengthColumn, "Length");

        dataGrid.setColumnWidth(0, "80px");

        dataGrid.setSelectionModel(multiSelectionModel);
        multiSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Set<SequenceInfo> selectedSequenceInfo = multiSelectionModel.getSelectedSet();
                if(selectedSequenceInfo.size()==1){
                    setSequenceInfo(selectedSequenceInfo.iterator().next());
                    selectSelectedButton.setEnabled(true);
                }
                else {
                    setSequenceInfo(null);
                }

                selectSelectedButton.setEnabled(selectedSequenceInfo.size()>0);
            }
        });

        dataProvider.addDataDisplay(dataGrid);
        pager.setDisplay(dataGrid);


        SequenceRestService.loadSequences(sequenceInfoList, MainPanel.currentOrganismId);

        ColumnSortEvent.ListHandler<SequenceInfo> sortHandler = new ColumnSortEvent.ListHandler<SequenceInfo>(filteredSequenceList);
        dataGrid.addColumnSortHandler(sortHandler);

        sortHandler.setComparator(selectColumn, new Comparator<SequenceInfo>() {
                    @Override
                    public int compare(SequenceInfo o1, SequenceInfo o2) {
                        return o1.getSelected().compareTo(o2.getSelected());
                    }
                }
        );
        sortHandler.setComparator(nameColumn, new Comparator<SequenceInfo>() {
                    @Override
                    public int compare(SequenceInfo o1, SequenceInfo o2) {
                        return o1.compareTo(o2);
                    }
                }
        );
        sortHandler.setComparator(lengthColumn, new Comparator<SequenceInfo>() {
                    @Override
                    public int compare(SequenceInfo o1, SequenceInfo o2) {
                        return o1.getLength() - o2.getLength();
                    }
                }
        );

        loadOrganisms(organismList);

        Annotator.eventBus.addHandler(SequenceLoadEvent.TYPE,
                new SequenceLoadEventHandler() {
                    @Override
                    public void onSequenceLoaded(SequenceLoadEvent sequenceLoadEvent) {
                        filterSequences();
                        if(sequenceInfoList.size()>0){
                            exportAllButton.setEnabled(true);
                            exportAllButton.setText("All ("+sequenceInfoList.size()+")");
                        }
                        else{
                            exportAllButton.setEnabled(false);
                            exportAllButton.setText("None Available");
                        }
                    }
                }
        );

    }

    private void updatedExportSelectedButton() {
        if(selectedCount>0){
            exportSelectedButton.setEnabled(true);
            exportSelectedButton.setText("Selected ("+selectedCount+")");
        }
        else{
            exportSelectedButton.setEnabled(false);
            exportSelectedButton.setText("None Selected");
        }
    }

    private void setSequenceInfo(SequenceInfo selectedObject) {
        selectedSequenceInfo = selectedObject;
        if (selectedSequenceInfo == null) {
            sequenceName.setText("");
            sequenceLength.setText("");
            exportSingleButton.setEnabled(false);
            exportSingleButton.setText("None");
        } else {
            sequenceName.setHTML(selectedSequenceInfo.getName());
            sequenceLength.setText(selectedSequenceInfo.getLength().toString());
            exportSingleButton.setEnabled(true);
            exportSingleButton.setText(selectedSequenceInfo.getName());
        }
    }

    @UiHandler(value = {"nameSearchBox", "minFeatureLength", "maxFeatureLength"})
    public void handleNameSearch(KeyUpEvent keyUpEvent) {
        filterSequences();
    }

    @UiHandler(value = {"exportGff3Button", "exportFastaButton", "exportChadoButton"})
    public void handleExportTypeChanged(ClickEvent clickEvent) {
        exportGff3Button.setType(ButtonType.DEFAULT);
        exportFastaButton.setType(ButtonType.DEFAULT);
        exportChadoButton.setType(ButtonType.DEFAULT);
        Button selectedButton = (Button) clickEvent.getSource();
        switch (selectedButton.getText()){
            case "GFF3":  exportGff3Button.setType(ButtonType.PRIMARY); break ;
            case "FASTA":  exportFastaButton.setType(ButtonType.PRIMARY); break ;
            case "CHADO":  exportChadoButton.setType(ButtonType.PRIMARY); break ;
        }
    }

    @UiHandler(value = {"organismList"})
    public void handleOrganismChange(ChangeEvent changeEvent) {
        reload();
    }


    @UiHandler("selectSelectedButton")
    public void handleSetSelections(ClickEvent clickEvent){
        GWT.log("selecting selected?");

        boolean allSelectionsSelected = findAllSelectionsSelected();

        for(SequenceInfo sequenceInfo : multiSelectionModel.getSelectedSet()){
            if(allSelectionsSelected){
                if(sequenceInfo.getSelected()){
                    --selectedCount ;
                }
                sequenceInfo.setSelected(false);
            }
            else{
                if(!sequenceInfo.getSelected()){
                    ++selectedCount ;
                }
                sequenceInfo.setSelected(true);
            }
        }
        updatedExportSelectedButton();
        dataGrid.redraw();
    }

    private boolean findAllSelectionsSelected() {
        for(SequenceInfo sequenceInfo : multiSelectionModel.getSelectedSet()){
            if(!sequenceInfo.getSelected()) return false ;
        }
        return true ;
    }

    private void exportValues(List<SequenceInfo> sequenceInfoList){
        Integer organismId = Integer.parseInt(organismList.getSelectedValue());
        OrganismInfo organismInfo = new OrganismInfo();
        organismInfo.setId(organismId.toString());
        organismInfo.setName(organismList.getSelectedItemText());

        // get the type based on the active button
        String type = null  ;
        if(exportGff3Button.getType().equals(ButtonType.DANGER.PRIMARY)){
            type = exportGff3Button.getText();
        }
        else
        if(exportFastaButton.getType().equals(ButtonType.DANGER.PRIMARY)){
            type = exportFastaButton.getText();
        }
        else
        if(exportChadoButton.getType().equals(ButtonType.DANGER.PRIMARY)){
            type = exportChadoButton.getText();
        }

        ExportPanel exportPanel = new ExportPanel();
        exportPanel.setOrganismInfo(organismInfo);
        exportPanel.setSequenceList(sequenceInfoList);
        exportPanel.setType(type);
        exportPanel.show();
    }

    @UiHandler("exportSelectedButton")
    public void exportSelectedHandler(ClickEvent clickEvent) {
        List<SequenceInfo> sequenceInfoList1 = new ArrayList<>();
        for(SequenceInfo sequenceInfo : sequenceInfoList){
            if(sequenceInfo.getSelected()){
                sequenceInfoList1.add(sequenceInfo);
            }
        }

        GWT.log("adding selected: "+sequenceInfoList1.size());
        exportValues(sequenceInfoList1);
    }

    @UiHandler("exportSingleButton")
    public void exportSingleHandler(ClickEvent clickEvent) {
        SequenceInfo sequenceInfo= multiSelectionModel.getSelectedSet().iterator().next();
        List<SequenceInfo> sequenceInfoList1 = new ArrayList<>();
        sequenceInfoList1.add(sequenceInfo);
        exportValues(sequenceInfoList1);

    }

    @UiHandler("exportAllButton")
    public void exportAllHandler(ClickEvent clickEvent) {
        GWT.log("exporting gff3");

        exportValues(sequenceInfoList);
//        Annotator.eventBus.fireEvent(new ExportEvent(ExportEvent.Action.EXPORT_READY, ExportEvent.Flavor.GFF3, organismInfo, selectedSequenceInfoArrayList));
    }


    private void filterSequences() {
        GWT.log("original size: " + sequenceInfoList.size());
        filteredSequenceList.clear();

        String nameText = nameSearchBox.getText().toLowerCase();
        String minLengthText = minFeatureLength.getText();
        String maxLengthText = maxFeatureLength.getText();
        Long minLength = Long.MIN_VALUE;
        Long maxLength = Long.MAX_VALUE;


        if (minLengthText.length() > 0) {
            minLength = Long.parseLong(minLengthText);
        }

        if (maxLengthText.length() > 0) {
            maxLength = Long.parseLong(maxLengthText);
        }

        for (SequenceInfo sequenceInfo : sequenceInfoList) {
            if (sequenceInfo.getName().toLowerCase().contains(nameText)
                    && sequenceInfo.getLength() >= minLength
                    && sequenceInfo.getLength() <= maxLength
                    ) {
                filteredSequenceList.add(sequenceInfo);
            }
        }
//        else {
//            filteredSequenceList.addAll(sequenceInfoList);
//        }

        GWT.log("filtered size: " + filteredSequenceList.size());
        viewableLabel.setText(filteredSequenceList.size() + "");

    }

    /**
     * could use an organism callback . . . however, this element needs to use the callback directly.
     *
     * @param trackInfoList
     */
    public void loadOrganisms(final ListBox trackInfoList) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();

                trackInfoList.clear();
                for (int i = 0; i < array.size(); i++) {
                    JSONObject object = array.get(i).isObject();
//                    GWT.log(object.toString());
                    OrganismInfo organismInfo = new OrganismInfo();
                    organismInfo.setId(object.get("id").isNumber().toString());
                    organismInfo.setName(object.get("commonName").isString().stringValue());
                    organismInfo.setNumSequences((int) Math.round(object.get("sequences").isNumber().doubleValue()));
                    organismInfo.setDirectory(object.get("directory").isString().stringValue());
                    organismInfo.setNumFeatures(0);
                    organismInfo.setNumTracks(0);
//                    GWT.log(object.toString());
                    trackInfoList.addItem(organismInfo.getName(), organismInfo.getId());
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error loading organisms");
            }
        };

        OrganismRestService.loadOrganisms(requestCallback);

    }

    public void reload() {
        GWT.log("item count: " + organismList.getItemCount());
        if (organismList.getItemCount() > 0) {
            Long organismListId = Long.parseLong(organismList.getSelectedValue());
            GWT.log("list id: " + organismListId);
            SequenceRestService.loadSequences(sequenceInfoList, organismListId);
        } else {
            SequenceRestService.loadSequences(sequenceInfoList, MainPanel.currentOrganismId);
        }
    }

}