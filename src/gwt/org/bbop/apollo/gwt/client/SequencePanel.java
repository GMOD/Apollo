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
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.*;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfoConverter;
import org.bbop.apollo.gwt.client.event.OrganismChangeEvent;
import org.bbop.apollo.gwt.client.event.OrganismChangeEventHandler;
import org.bbop.apollo.gwt.client.event.UserChangeEvent;
import org.bbop.apollo.gwt.client.event.UserChangeEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.SequenceRestService;
import org.bbop.apollo.gwt.shared.PermissionEnum;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.select.client.ui.MultipleSelect;
import org.gwtbootstrap3.extras.select.client.ui.Option;

import java.util.ArrayList;
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
    MultipleSelect selectedSequenceDisplay;
    @UiField
    Button clearSelectionButton;
    @UiField
    TextBox nameSearchBox;
    @UiField
    HTML sequenceLength;
    @UiField
    Button exportGff3Button;
    @UiField
    Button exportVcfButton;
    @UiField
    Button exportFastaButton;
    @UiField
    Button selectSelectedButton;
    @UiField
    Button exportChadoButton;

    private AsyncDataProvider<SequenceInfo> dataProvider;
    private MultiSelectionModel<SequenceInfo> multiSelectionModel = new MultiSelectionModel<SequenceInfo>();
    private SequenceInfo selectedSequenceInfo = null;
    private Integer selectedCount = 0;
    private Boolean exportAll = false;
    private Boolean chadoExportStatus = false;

    public SequencePanel() {

        initWidget(ourUiBinder.createAndBindUi(this));
        dataGrid.setWidth("100%");

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
                } else {
                    exportSelectedButton.setText("Selected");
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
                        Bootbox.alert("error getting sequence info: " + exception);
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
                Set<SequenceInfo> sequenceInfoSet = multiSelectionModel.getSelectedSet();
                if (sequenceInfoSet.size() == 1) {
                    final SequenceInfo sequenceInfo = sequenceInfoSet.iterator().next();

                    RequestCallback requestCallback = new RequestCallback() {
                        @Override
                        public void onResponseReceived(Request request, Response response) {
                            JSONObject sequenceInfoJson = JSONParser.parseStrict(response.getText()).isObject();
                            MainPanel mainPanel = MainPanel.getInstance();
                            SequenceInfo currentSequence = mainPanel.setCurrentSequenceAndEnds(SequenceInfoConverter.convertFromJson(sequenceInfoJson));
                            mainPanel.sequenceSuggestBox.setText(currentSequence.getName());
                            Annotator.eventBus.fireEvent(new OrganismChangeEvent(OrganismChangeEvent.Action.LOADED_ORGANISMS, currentSequence.getName(),mainPanel.getCurrentOrganism().getName()));
                            MainPanel.updateGenomicViewerForLocation(currentSequence.getName(),currentSequence.getStartBp(),currentSequence.getEndBp(),true,false);
                        }

                        @Override
                        public void onError(Request request, Throwable exception) {
                            Bootbox.alert("Error setting current sequence: " + exception);
                        }
                    };
                    SequenceRestService.setCurrentSequence(requestCallback, sequenceInfo);

                }
            }
        }, DoubleClickEvent.getType());

        Annotator.eventBus.addHandler(OrganismChangeEvent.TYPE, new OrganismChangeEventHandler() {
            @Override
            public void onOrganismChanged(OrganismChangeEvent organismChangeEvent) {
                if (organismChangeEvent.getAction().equals(OrganismChangeEvent.Action.LOADED_ORGANISMS) && (organismChangeEvent.getCurrentOrganism() == null || !organismChangeEvent.getCurrentOrganism().equals(MainPanel.getInstance().getCurrentOrganism().getName()))) {
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

        Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if(MainPanel.getInstance().getCurrentUser()!=null) {
                    if (MainPanel.getInstance().isCurrentUserAdmin()) {
                        exportChadoButton.setVisible(true);
                        getChadoExportStatus();
                    } else {
                        exportChadoButton.setVisible(false);
                    }
                    return false ;
                }
                return true ;
            }
        },100);

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

    @UiHandler(value = {"exportGff3Button", "exportVcfButton", "exportFastaButton", "exportChadoButton"})
    public void handleExportTypeChanged(ClickEvent clickEvent) {
        exportGff3Button.setType(ButtonType.DEFAULT);
        exportVcfButton.setType(ButtonType.DEFAULT);
        exportFastaButton.setType(ButtonType.DEFAULT);
        exportChadoButton.setType(ButtonType.DEFAULT);
        Button selectedButton = (Button) clickEvent.getSource();
        switch (selectedButton.getText()) {
            case "GFF3":
                exportGff3Button.setType(ButtonType.PRIMARY);
                break;
            case "VCF":
                exportVcfButton.setType(ButtonType.PRIMARY);
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
        } else if (exportVcfButton.getType().equals(ButtonType.DANGER.PRIMARY)) {
            type = exportVcfButton.getText();
        } else if (exportFastaButton.getType().equals(ButtonType.DANGER.PRIMARY)) {
            type = exportFastaButton.getText();
        } else if (exportChadoButton.getType().equals(ButtonType.DANGER.PRIMARY)) {
            type = exportChadoButton.getText();
        }

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
        } else {
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
        reload(false);
    }

    public void reload(Boolean forceReload) {
        if (MainPanel.getInstance().getSequencePanel().isVisible() || forceReload) {
            pager.setPageStart(0);
            dataGrid.setVisibleRangeAndClearData(dataGrid.getVisibleRange(), true);
            dataGrid.redraw();
        }
    }

    public void getChadoExportStatus() {
        SequenceRestService.getChadoExportStatus(this);
    }

    public void setChadoExportStatus(String exportStatus) {
        this.chadoExportStatus = exportStatus.equals("true");
        this.exportChadoButton.setEnabled(this.chadoExportStatus);
    }
}
