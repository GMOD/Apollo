package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.*;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfoConverter;
import org.bbop.apollo.gwt.client.dto.assemblage.AssemblageInfo;
import org.bbop.apollo.gwt.client.dto.assemblage.AssemblageSequence;
import org.bbop.apollo.gwt.client.dto.assemblage.AssemblageSequenceList;
import org.bbop.apollo.gwt.client.event.OrganismChangeEvent;
import org.bbop.apollo.gwt.client.event.OrganismChangeEventHandler;
import org.bbop.apollo.gwt.client.event.UserChangeEvent;
import org.bbop.apollo.gwt.client.event.UserChangeEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.AssemblageRestService;
import org.bbop.apollo.gwt.client.rest.OrganismRestService;
import org.bbop.apollo.gwt.client.rest.SequenceRestService;
import org.bbop.apollo.gwt.shared.PermissionEnum;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.select.client.ui.Option;
import org.gwtbootstrap3.extras.select.client.ui.Select;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Nathan Dunn on 12/17/14.
 */
public class SequencePanel extends Composite {

    interface SequencePanelUiBinder extends UiBinder<Widget, SequencePanel> {
    }

    private static SequencePanelUiBinder ourUiBinder = GWT.create(SequencePanelUiBinder.class);
    @UiField
    TextBox minFeatureLength;
    @UiField
    TextBox maxFeatureLength;

    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<SequenceInfo> dataGrid = new DataGrid<SequenceInfo>(50, tablecss);
    @UiField(provided = true)
    WebApolloSimplePager pager = new WebApolloSimplePager(WebApolloSimplePager.TextLocation.CENTER);


    @UiField
    HTML sequenceName;
    @UiField
    Button exportAllButton;
    @UiField
    Button exportSelectedButton;
    @UiField
    Select selectedSequenceDisplay;
    @UiField
    Button clearSelectionButton;
    @UiField
    TextBox nameSearchBox;
    @UiField
    HTML sequenceLength;
    @UiField
    Button exportGff3Button;
    @UiField
    Button exportFastaButton;
    @UiField
    Button selectSelectedButton;
    @UiField
    Button exportChadoButton;
    @UiField
    Button assemblageButton;
    @UiField
    Alert panelMessage;
    @UiField
    Button addToView;
    @UiField
    Button viewSequence;

    private AsyncDataProvider<SequenceInfo> dataProvider;
    private MultiSelectionModel<SequenceInfo> multiSelectionModel = new MultiSelectionModel<SequenceInfo>();
    private SequenceInfo selectedSequenceInfo = null;
    private Integer selectedCount = 0;
    private Boolean exportAll = false;
    private Boolean chadoExportStatus = false;

    public SequencePanel() {

        initWidget(ourUiBinder.createAndBindUi(this));
        dataGrid.setWidth("100%");
        getChadoExportStatus();
        TextColumn<SequenceInfo> nameColumn = new TextColumn<SequenceInfo>() {
            @Override
            public String getValue(SequenceInfo employee) {
                return employee.getName();
            }
        };
        nameColumn.setSortable(true);

        Column<SequenceInfo, Number> lengthColumn = new Column<SequenceInfo, Number>(new NumberCell()) {
            @Override
            public Long getValue(SequenceInfo object) {
                return object.getLength();
            }
        };
        lengthColumn.setDefaultSortAscending(false);
        lengthColumn.setSortable(true);

        Column<SequenceInfo, Number> annotationCount = new Column<SequenceInfo, Number>(new NumberCell()) {
            @Override
            public Integer getValue(SequenceInfo object) {
                return object.getCount();
            }
        };

        annotationCount.setSortable(true);
        dataGrid.addColumn(nameColumn, "Name");
        dataGrid.addColumn(lengthColumn, "Length");
        dataGrid.setColumnWidth(1, "100px");
        dataGrid.addColumn(annotationCount, "Annotations");

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
                if (selectedSequenceInfo.size() > 0) {
                    exportSelectedButton.setText("Selected (" + selectedSequenceInfo.size() + ")");
                    enableAssemblages(true);
                } else {
                    exportSelectedButton.setText("Selected");
                    enableAssemblages(false);
                }
                exportSelectedButton.setEnabled(selectedSequenceInfo.size() > 0);
                selectSelectedButton.setEnabled(selectedSequenceInfo.size() > 0);

                updateSelectedSequenceDisplay(multiSelectionModel.getSelectedSet());
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
                        Integer sequenceCount = 0;
                        if (jsonArray != null && jsonArray.size() > 0) {
                            JSONObject jsonObject = jsonArray.get(0).isObject();
                            sequenceCount = (int) jsonObject.get("sequenceCount").isNumber().doubleValue();
                        }
                        dataGrid.setRowCount(sequenceCount, true);
                        dataGrid.setRowData(start, SequenceInfoConverter.convertFromJsonArray(jsonArray));
                    }

                    @Override
                    public void onError(Request request, Throwable exception) {
                        Bootbox.alert("Error getting sequence info: " + exception);
                    }
                };


                ColumnSortList.ColumnSortInfo nameSortInfo = sortList.get(0);
                if (nameSortInfo.getColumn().isSortable()) {
                    Column<SequenceInfo, ?> sortColumn = (Column<SequenceInfo, ?>) sortList.get(0).getColumn();
                    Integer columnIndex = dataGrid.getColumnIndex(sortColumn);
                    String searchColumnString = columnIndex == 0 ? "name" : columnIndex == 1 ? "length" : "count";
                    Boolean sortNameAscending = nameSortInfo.isAscending();
                    SequenceRestService.getSequenceForOffsetAndMax(requestCallback, nameSearchBox.getText(), start, length, searchColumnString, sortNameAscending, minFeatureLength.getText(), maxFeatureLength.getText());
                }
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
                // can only double-click on a single one really
                viewSingleSequence();
            }
        }, DoubleClickEvent.getType());

        Annotator.eventBus.addHandler(OrganismChangeEvent.TYPE, new OrganismChangeEventHandler() {
            @Override
            public void onOrganismChanged(OrganismChangeEvent organismChangeEvent) {
                if (organismChangeEvent.getAction().equals(OrganismChangeEvent.Action.LOADED_ORGANISMS)) {
                    Scheduler.get().scheduleDeferred(new Command() {
                        @Override
                        public void execute() {
                            selectedCount = 0;
                            multiSelectionModel.clear();
                            updatedExportSelectedButton();
                            updateSelectedSequenceDisplay(multiSelectionModel.getSelectedSet());
                            reload();
                        }
                    });
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
                                exportSelectedButton.setEnabled(allowExport);
                                selectedSequenceDisplay.setEnabled(allowExport);
                                break;
                        }
                    }
                }
        );

    }

    private void viewSingleSequence(){
        final SequenceInfo sequenceInfo = multiSelectionModel.getSelectedSet().iterator().next();

        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                if (sequenceInfo != null) {
                    OrganismRestService.switchSequenceById(sequenceInfo.getId().toString());
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error setting current sequence: " + exception);
            }
        };
        SequenceRestService.setCurrentSequence(requestCallback, sequenceInfo);
    }


    private void enableAssemblages(boolean b) {
        assemblageButton.setEnabled(b);
        addToView.setEnabled(b);
        viewSequence.setEnabled(b);
    }

    @UiHandler("addToView")
    void addSequenceToView(ClickEvent clickEvent) {
        Set<SequenceInfo> sequenceInfoSet = multiSelectionModel.getSelectedSet();
        // send sequences and current assemblage and return a new current assemblage to view
        AssemblageInfo assemblageInfo = MainPanel.getInstance().getCurrentAssemblage().addSequenceInfoSet(sequenceInfoSet);
        AssemblageRestService.addAssemblageAndView(assemblageInfo);
    }

    @UiHandler("viewSequence")
    void viewSequence(ClickEvent clickEvent) {
        if(multiSelectionModel.getSelectedSet().size()==1){
            viewSingleSequence();
        }
        else{
            AssemblageInfo assemblageInfo = new AssemblageInfo();
            assemblageInfo.addSequenceInfoSet(multiSelectionModel.getSelectedSet());
            AssemblageRestService.addAssemblageAndView(assemblageInfo);
        }
    }


    @UiHandler("assemblageButton")
    void addNewAssemblage(ClickEvent clickEvent) {
        AssemblageInfo assemblageInfo = new AssemblageInfo();
        AssemblageSequenceList sequenceArray = new AssemblageSequenceList();
        StringBuilder nameBuffer = new StringBuilder();
        long start = 0;
        long end = 0;
        for (SequenceInfo sequenceInfo : multiSelectionModel.getSelectedSet()) {
            assemblageInfo.setPadding(50);
            assemblageInfo.setType("Exon");
            AssemblageSequence sequenceObject = new AssemblageSequence();
            sequenceObject.setName(sequenceInfo.getName());
            sequenceObject.setStart(sequenceInfo.getStart());
            sequenceObject.setEnd(sequenceInfo.getEnd());
//            sequenceObject.put(FeatureStringEnum.NAME.getValue(),new JSONString(sequenceInfo.getName()));
            sequenceArray.addSequence(sequenceObject);
//            sequenceArray.set(sequenceArray.size(),sequenceObject);
            nameBuffer.append(sequenceInfo.getName() + ",");
            if (start == 0) {
                start = sequenceInfo.getStart();
            }
            end += sequenceInfo.getEnd();
        }
//        name = name.substring(0,name.length()-1);
        assemblageInfo.setStart(start);
        assemblageInfo.setEnd(end);
        assemblageInfo.setSequenceList(sequenceArray);

        final String name = nameBuffer.toString();


        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                new InfoDialog("Added Assemblage", "Added assemblage: " + name.substring(0, name.length() - 1), true);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error adding assemblage: " + exception);
            }
        };

        MainPanel.getInstance().addAssemblage(requestCallback, assemblageInfo);


    }

    private void updatedExportSelectedButton() {
        if (selectedCount > 0) {
            exportSelectedButton.setEnabled(true);
            exportSelectedButton.setText("Selected (" + multiSelectionModel.getSelectedSet().size() + ")");
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
        } else {
            sequenceName.setHTML(selectedSequenceInfo.getName());
            sequenceLength.setText(selectedSequenceInfo.getLength().toString());
        }
    }

    @UiHandler(value = {"nameSearchBox", "minFeatureLength", "maxFeatureLength"})
    public void handleNameSearch(KeyUpEvent keyUpEvent) {
        pager.setPageStart(0);
        dataGrid.setVisibleRangeAndClearData(dataGrid.getVisibleRange(), true);
    }

    @UiHandler(value = {"exportGff3Button", "exportFastaButton", "exportChadoButton"})
    public void handleExportTypeChanged(ClickEvent clickEvent) {
        exportGff3Button.setType(ButtonType.DEFAULT);
        exportFastaButton.setType(ButtonType.DEFAULT);
        exportChadoButton.setType(ButtonType.DEFAULT);
        Button selectedButton = (Button) clickEvent.getSource();
        switch (selectedButton.getText()) {
            case "GFF3":
                exportGff3Button.setType(ButtonType.PRIMARY);
                break;
            case "FASTA":
                exportFastaButton.setType(ButtonType.PRIMARY);
                break;
            case "CHADO":
                exportChadoButton.setType(ButtonType.PRIMARY);
                break;
        }
    }


    @UiHandler("selectSelectedButton")
    public void handleSetSelections(ClickEvent clickEvent) {
        GWT.log("handleSetSelection");

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

    private void exportValues(List<SequenceInfo> sequenceInfoList) {
        OrganismInfo organismInfo = MainPanel.getInstance().getCurrentOrganism();
        // get the type based on the active button
        String type = null;
        if (exportGff3Button.getType().equals(ButtonType.DANGER.PRIMARY)) {
            type = exportGff3Button.getText();
        } else if (exportFastaButton.getType().equals(ButtonType.DANGER.PRIMARY)) {
            type = exportFastaButton.getText();
        } else if (exportChadoButton.getType().equals(ButtonType.DANGER.PRIMARY)) {
            type = exportChadoButton.getText();
        }
//        GWT.log("Type selected is " + type);

        ExportPanel exportPanel = new ExportPanel(organismInfo, type, exportAll, sequenceInfoList);
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

    @UiHandler("exportAllButton")
    public void exportAllHandler(ClickEvent clickEvent) {
        exportAll = true;
        GWT.log("exporting gff3");

        exportValues(new ArrayList<SequenceInfo>());
    }

    public void updateSelectedSequenceDisplay(Set<SequenceInfo> selectedSequenceInfoList) {
        selectedSequenceDisplay.clear();
        if (selectedSequenceInfoList.size() == 0) {
            selectedSequenceDisplay.setEnabled(false);
        }
        else {
            selectedSequenceDisplay.setEnabled(true);
            for (SequenceInfo s : selectedSequenceInfoList) {
                Option option = new Option();
                option.setValue(s.getName());
                option.setText(s.getName());
                selectedSequenceDisplay.add(option);
            }
        }
        selectedSequenceDisplay.refresh();
    }

    @UiHandler("clearSelectionButton")
    public void clearSelection(ClickEvent clickEvent) {
        multiSelectionModel.clear();
    }

    public void reload() {
        pager.setPageStart(0);
        dataGrid.setVisibleRangeAndClearData(dataGrid.getVisibleRange(), true);
        dataGrid.redraw();
    }

    public void getChadoExportStatus() {
        SequenceRestService.getChadoExportStatus(this);
    }

    public void setChadoExportStatus(String exportStatus) {
        this.chadoExportStatus = exportStatus.equals("true");
        this.exportChadoButton.setEnabled(this.chadoExportStatus);
    }
}
