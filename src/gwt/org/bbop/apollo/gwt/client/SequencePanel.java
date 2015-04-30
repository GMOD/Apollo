package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONParser;
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
import org.bbop.apollo.gwt.client.dto.SequenceInfoConverter;
import org.bbop.apollo.gwt.client.event.*;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.OrganismRestService;
import org.bbop.apollo.gwt.client.rest.SequenceRestService;
import org.bbop.apollo.gwt.shared.PermissionEnum;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ButtonType;

import java.util.*;

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
//    @UiField
//    ListBox organismList;
    // TODO: a hack of a backing object fro the organism List
    // key is the ID as we can have a dupe org?
//    Map<String, OrganismInfo> organismInfoMap = new TreeMap<>();

    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<SequenceInfo> dataGrid = new DataGrid<SequenceInfo>(20, tablecss);
    @UiField(provided = true)
    SimplePager pager = new SimplePager(SimplePager.TextLocation.CENTER);
    ;

    @UiField
    HTML sequenceName;
    @UiField
    Button exportAllButton;
    @UiField
    Button exportSelectedButton;
    @UiField
    Button exportSingleButton;
    @UiField
    TextBox nameSearchBox;
    @UiField
    HTML sequenceLength;
    @UiField
    Button exportGff3Button;
    @UiField
    Button exportFastaButton;
    //    @UiField
//    Button exportChadoButton;
    @UiField
    Button selectSelectedButton;

    private AsyncDataProvider<SequenceInfo> dataProvider;
    private MultiSelectionModel<SequenceInfo> multiSelectionModel = new MultiSelectionModel<SequenceInfo>();
    private SequenceInfo selectedSequenceInfo = null;
    private Integer selectedCount = 0;
    private Boolean exportAll = false;

    public SequencePanel() {

        initWidget(ourUiBinder.createAndBindUi(this));
        ;
        dataGrid.setWidth("100%");
        dataGrid.setEmptyTableWidget(new Label("Loading"));
//
//        Column<SequenceInfo, Boolean> selectColumn = new Column<SequenceInfo, Boolean>(new CheckboxCell(true, false)) {
//            @Override
//            public Boolean getValue(SequenceInfo object) {
//                return object.getSelected();
//            }
//        };
//        selectColumn.setSortable(true);

//        selectColumn.setFieldUpdater(new FieldUpdater<SequenceInfo, Boolean>() {
//            @Override
//            public void update(int index, SequenceInfo object, Boolean value) {
//                selectedCount += value ? 1 : -1;
//                if (selectedCount > 0) {
//                } else {
//                    selectedCount = 0;
//                }
//                object.setSelected(value);
//                updatedExportSelectedButton();
//            }
//        });

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


//        dataGrid.addColumn(selectColumn, "Selected");
        dataGrid.addColumn(nameColumn, "Name");
        dataGrid.addColumn(lengthColumn, "Length");

//        dataGrid.setColumnWidth(0, "80px");
        dataGrid.setColumnWidth(1, "100px");

        dataGrid.setSelectionModel(multiSelectionModel);
        multiSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Set<SequenceInfo> selectedSequenceInfo = multiSelectionModel.getSelectedSet();
                if (selectedSequenceInfo.size() == 1) {
                    setSequenceInfo(selectedSequenceInfo.iterator().next());
                    selectSelectedButton.setEnabled(true);
                } else {
                    setSequenceInfo(null);
                }
                if(selectedSequenceInfo.size()>0){
                    exportSelectedButton.setText("Selected ("+selectedSequenceInfo.size()+")");
                }
                else{
                    exportSelectedButton.setText("Selected");
                }
                exportSelectedButton.setEnabled(selectedSequenceInfo.size() > 0);

                selectSelectedButton.setEnabled(selectedSequenceInfo.size() > 0);
            }
        });

        dataProvider = new AsyncDataProvider<SequenceInfo>() {
            @Override
            protected void onRangeChanged(HasData<SequenceInfo> display) {
                final Range range = display.getVisibleRange();
                final ColumnSortList sortList = dataGrid.getColumnSortList();
                final int start = range.getStart();
                final int length = range.getLength();

                RequestCallback requestCallback = new RequestCallback() {
                    @Override
                    public void onResponseReceived(Request request, Response response) {
                        JSONArray jsonArray = JSONParser.parseLenient(response.getText()).isArray();
                        dataGrid.setRowData(start, SequenceInfoConverter.convertFromJsonArray(jsonArray));
                    }

                    @Override
                    public void onError(Request request, Throwable exception) {
                        Window.alert("error getting sequence info: " + exception);
                    }
                };


                ColumnSortList.ColumnSortInfo nameSortInfo = sortList.get(0);
                if (nameSortInfo.getColumn().isSortable()) {
                    Column<SequenceInfo, ?> sortColumn = (Column<SequenceInfo, ?>) sortList.get(0).getColumn();
                    Integer columnIndex = dataGrid.getColumnIndex(sortColumn);
                    String searchColumnString = columnIndex == 0 ? "name" : "length";
                    Boolean sortNameAscending = nameSortInfo.isAscending();
                    SequenceRestService.getSequenceForOffsetAndMax(requestCallback, nameSearchBox.getText(), start, length, searchColumnString, sortNameAscending, minFeatureLength.getText(), maxFeatureLength.getText());
                }
//                nameSortInfo = sortList.get(1)
//                if (nameSortInfo.getColumn().isSortable()) {
//                    Boolean sortLengthAscending = nameSortInfo.isAscending();
//                    SequenceRestService.getSequenceForOffsetAndMax(requestCallback, nameSearchBox.getText(), start, length, "length", sortLengthAscending, minFeatureLength.getText(), maxFeatureLength.getText());
//                }

            }
        };

        ColumnSortEvent.AsyncHandler columnSortHandler = new ColumnSortEvent.AsyncHandler(dataGrid);
        dataGrid.addColumnSortHandler(columnSortHandler);
        dataGrid.getColumnSortList().push(nameColumn);
        dataGrid.getColumnSortList().push(lengthColumn);


        dataProvider.addDataDisplay(dataGrid);
        pager.setDisplay(dataGrid);


        // have to use a special handler instead of UiHandler for this type
        dataGrid.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                Set<SequenceInfo> sequenceInfoSet = multiSelectionModel.getSelectedSet();
                if (sequenceInfoSet.size() == 1) {
                    final SequenceInfo sequenceInfo = sequenceInfoSet.iterator().next();
//                    final OrganismInfo organismInfo = organismInfoMap.get(organismList.getSelectedValue());

                    // TODO: set the default here!
                    RequestCallback requestCallback = new RequestCallback() {
                        @Override
                        public void onResponseReceived(Request request, Response response) {
                            if (sequenceInfo != null) {
                                OrganismRestService.switchSequenceById(sequenceInfo.getId().toString());
                            }
//                            ContextSwitchEvent contextSwitchEvent = new ContextSwitchEvent(sequenceInfo.getName(), organismInfo);
//                            Annotator.eventBus.fireEvent(contextSwitchEvent);
                        }

                        @Override
                        public void onError(Request request, Throwable exception) {
                            Window.alert("Error setting current sequence: " + exception);
                        }
                    };
                    SequenceRestService.setCurrentSequence(requestCallback, sequenceInfo);

                }
            }
        }, DoubleClickEvent.getType());

        Annotator.eventBus.addHandler(OrganismChangeEvent.TYPE, new OrganismChangeEventHandler() {
            @Override
            public void onOrganismChanged(OrganismChangeEvent organismChangeEvent) {
                if (organismChangeEvent.getAction().equals(OrganismChangeEvent.Action.LOADED_ORGANISMS)) {
//                    OrganismInfo currentOrganism = MainPanel.getInstance().getCurrentOrganism();
//
//                    organismList.clear();
//                    organismInfoMap.clear();
//                    List<OrganismInfo> organismInfoList = MainPanel.getInstance().getOrganismInfoList();
//                    for (int i = 0; i < organismInfoList.size(); i++) {
//                        OrganismInfo organismInfo = organismInfoList.get(i);
//                        organismList.addItem(organismInfo.getName(), organismInfo.getId());
//                        organismInfoMap.put(organismInfo.getId(), organismInfo);
//                        if (organismInfo.getId().equals(currentOrganism.getId())) {
//                            organismList.setSelectedIndex(i);
//                        }
//                    }
                    reload();

                } else {
                    GWT.log("Unable to handle organism action " + organismChangeEvent.getAction());
                }
            }
        });

        Annotator.eventBus.addHandler(UserChangeEvent.TYPE,
                new UserChangeEventHandler() {
                    @Override
                    public void onUserChanged(UserChangeEvent authenticationEvent) {
                        switch (authenticationEvent.getAction()) {
                            case PERMISSION_CHANGED:
                                PermissionEnum hiPermissionEnum = authenticationEvent.getHighestPermission();
                                if (MainPanel.getInstance().isCurrentUserAdmin()) {
                                    hiPermissionEnum = PermissionEnum.ADMINISTRATE;
                                }
                                boolean allowExport = false;
                                switch (hiPermissionEnum) {
                                    case ADMINISTRATE:
                                    case WRITE:
                                    case EXPORT:
                                        allowExport = true;
                                        break;
                                    // default is false
                                }
                                exportAllButton.setEnabled(allowExport);
                                exportSingleButton.setEnabled(allowExport);
                                exportSelectedButton.setEnabled(allowExport);
                                break;
                        }
                    }
                }
        );

    }

    private void updatedExportSelectedButton() {
        if (selectedCount > 0) {
            exportSelectedButton.setEnabled(true);
            exportSelectedButton.setText("Selected (" + multiSelectionModel.getSelectedSet().size()+ ")");
        } else {
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
        dataGrid.setVisibleRangeAndClearData(dataGrid.getVisibleRange(), true);

//        filterSequences();
    }

    @UiHandler(value = {"exportGff3Button", "exportFastaButton"})
    // Disabling exportChadoButton for future release (Apollo 2.0 alpha2)
    // @UiHandler(value = {"exportGff3Button", "exportFastaButton", "exportChadoButton"})
    public void handleExportTypeChanged(ClickEvent clickEvent) {
        exportGff3Button.setType(ButtonType.DEFAULT);
        exportFastaButton.setType(ButtonType.DEFAULT);
//        exportChadoButton.setType(ButtonType.DEFAULT);
        Button selectedButton = (Button) clickEvent.getSource();
        switch (selectedButton.getText()) {
            case "GFF3":
                exportGff3Button.setType(ButtonType.PRIMARY);
                break;
            case "FASTA":
                exportFastaButton.setType(ButtonType.PRIMARY);
                break;
//            case "CHADO":
//                exportChadoButton.setType(ButtonType.PRIMARY);
//                break;
        }
    }

//    @UiHandler(value = {"organismList"})
//    public void handleOrganismChange(ChangeEvent changeEvent) {
//        selectedCount = 0;
//        multiSelectionModel.clear();
//        updatedExportSelectedButton();
//        OrganismRestService.switchOrganismById(organismList.getSelectedValue());
//    }


    @UiHandler("selectSelectedButton")
    public void handleSetSelections(ClickEvent clickEvent) {
        GWT.log("selecting selected?");

        boolean allSelectionsSelected = findAllSelectionsSelected();

        for (SequenceInfo sequenceInfo : multiSelectionModel.getSelectedSet()) {
            if (allSelectionsSelected) {
                if (sequenceInfo.getSelected()) {
                    --selectedCount;
                }
                sequenceInfo.setSelected(false);
            } else {
                if (!sequenceInfo.getSelected()) {
                    ++selectedCount;
                }
                sequenceInfo.setSelected(true);
            }
        }
        updatedExportSelectedButton();
        dataGrid.redraw();
    }

    private boolean findAllSelectionsSelected() {
        for (SequenceInfo sequenceInfo : multiSelectionModel.getSelectedSet()) {
            if (!sequenceInfo.getSelected()) return false;
        }
        return true;
    }

    private void exportValues(List<SequenceInfo> sequenceInfoList ) {
//        GWT.log(organismList.getSelectedValue());
//        Integer organismId = Integer.parseInt(organismList.getSelectedValue());
        OrganismInfo organismInfo = MainPanel.getInstance().getCurrentOrganism();
//        organismInfo.setId(organismId.toString());
//        organismInfo.setName(organismList.getSelectedItemText());

        // get the type based on the active button
        String type = null;
        if (exportGff3Button.getType().equals(ButtonType.DANGER.PRIMARY)) {
            type = exportGff3Button.getText();
        } else if (exportFastaButton.getType().equals(ButtonType.DANGER.PRIMARY)) {
            type = exportFastaButton.getText();
        }
        GWT.log("Type selected is " + type);
//        else if (exportChadoButton.getType().equals(ButtonType.DANGER.PRIMARY)) {
//            type = exportChadoButton.getText();
//        }

        ExportPanel exportPanel = new ExportPanel();
        exportPanel.setOrganismInfo(organismInfo);
        exportPanel.setSequenceList(sequenceInfoList);
        exportPanel.setType(type);
        exportPanel.setExportAll(exportAll);
        if (type.equals("FASTA")) {
            exportPanel.renderFastaSelection();
        }
        exportPanel.show();
    }

    @UiHandler("exportSelectedButton")
    public void exportSelectedHandler(ClickEvent clickEvent) {
        exportAll = false;
        List<SequenceInfo> sequenceInfoList1 = new ArrayList<>();
        for (SequenceInfo sequenceInfo : multiSelectionModel.getSelectedSet()) {
            sequenceInfoList1.add(sequenceInfo);
        }

        GWT.log("adding selected: " + sequenceInfoList1.size());
        exportValues(sequenceInfoList1);
    }

    @UiHandler("exportSingleButton")
    public void exportSingleHandler(ClickEvent clickEvent) {
        exportAll = false;
        SequenceInfo sequenceInfo = multiSelectionModel.getSelectedSet().iterator().next();
        List<SequenceInfo> sequenceInfoList1 = new ArrayList<>();
        sequenceInfoList1.add(sequenceInfo);
        GWT.log("single export of " + sequenceInfoList1.size());
        exportValues(sequenceInfoList1);

    }

    @UiHandler("exportAllButton")
    public void exportAllHandler(ClickEvent clickEvent) {
        exportAll = true;
        GWT.log("exporting gff3");

        exportValues(new ArrayList<SequenceInfo>());
    }


    public void reload() {
        dataGrid.redraw();
    }

}
