package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.*;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.bookmark.BookmarkInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfoConverter;
import org.bbop.apollo.gwt.client.dto.bookmark.BookmarkSequence;
import org.bbop.apollo.gwt.client.dto.bookmark.BookmarkSequenceList;
import org.bbop.apollo.gwt.client.event.*;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.OrganismRestService;
import org.bbop.apollo.gwt.client.rest.SequenceRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.bbop.apollo.gwt.shared.PermissionEnum;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.AlertType;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.*;

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
    DataGrid<SequenceInfo> dataGrid = new DataGrid<SequenceInfo>(20, tablecss);
    @UiField(provided = true)
    WebApolloSimplePager pager = new WebApolloSimplePager(WebApolloSimplePager.TextLocation.CENTER);


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
    @UiField
    Button bookmarkButton;
    @UiField
    Alert panelMessage;

    private AsyncDataProvider<SequenceInfo> dataProvider;
    private MultiSelectionModel<SequenceInfo> multiSelectionModel = new MultiSelectionModel<SequenceInfo>();
    private SequenceInfo selectedSequenceInfo = null;
    private Integer selectedCount = 0;
    private Boolean exportAll = false;

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


        dataGrid.addColumn(nameColumn, "Name");
        dataGrid.addColumn(lengthColumn, "Length");
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
                if (selectedSequenceInfo.size() > 0) {
                    exportSelectedButton.setText("Selected (" + selectedSequenceInfo.size() + ")");
                    bookmarkButton.setEnabled(true);
                } else {
                    exportSelectedButton.setText("Selected");
                    bookmarkButton.setEnabled(false);
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
                        Integer sequenceCount = 0;
                        if (jsonArray!=null && jsonArray.size() > 0) {
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
                    String searchColumnString = columnIndex == 0 ? "name" : "length";
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
                                exportSingleButton.setEnabled(allowExport);
                                exportSelectedButton.setEnabled(allowExport);
                                break;
                        }
                    }
                }
        );

    }

    @UiHandler("bookmarkButton")
    void addNewBookmark(ClickEvent clickEvent){
        BookmarkInfo bookmarkInfo = new BookmarkInfo();
        BookmarkSequenceList sequenceArray = new BookmarkSequenceList();
        StringBuilder nameBuffer = new StringBuilder();
        for(SequenceInfo sequenceInfo : multiSelectionModel.getSelectedSet()){
            bookmarkInfo.setPadding(50);
            bookmarkInfo.setType("Exon");
            BookmarkSequence sequenceObject =new BookmarkSequence();
            sequenceObject.setName(sequenceInfo.getName());
//            sequenceObject.put(FeatureStringEnum.NAME.getValue(),new JSONString(sequenceInfo.getName()));
            sequenceArray.addSequence(sequenceObject);
//            sequenceArray.set(sequenceArray.size(),sequenceObject);
            nameBuffer.append(sequenceInfo.getName() + ",");
        }
//        name = name.substring(0,name.length()-1);
        bookmarkInfo.setSequenceList(sequenceArray);

        final String name = nameBuffer.toString() ;


        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
//                new InfoDialog("Added Bookmark","Added bookmark for sequences "+name.substring(0,name.length()-1),true);
                panelMessage.setText("Added bookmark: " + name.substring(0, name.length() - 1));
//                panelMessage.setText("Added bookmark!");
                panelMessage.setType(AlertType.SUCCESS);
                panelMessage.setVisible(true);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error adding bookmark: "+exception);
            }
        };

        MainPanel.getInstance().addBookmark(requestCallback,bookmarkInfo);


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
        }
//        GWT.log("Type selected is " + type);

        ExportPanel exportPanel = new ExportPanel(organismInfo,type,exportAll,sequenceInfoList);
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
        dataGrid.setVisibleRangeAndClearData(dataGrid.getVisibleRange(), true);
        dataGrid.redraw();
    }

}
