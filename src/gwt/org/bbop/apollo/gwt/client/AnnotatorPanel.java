package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.builder.shared.DivBuilder;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.AbstractSafeHtmlRenderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.*;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.AnnotationInfoConverter;
import org.bbop.apollo.gwt.client.dto.UserInfo;
import org.bbop.apollo.gwt.client.dto.UserInfoConverter;
import org.bbop.apollo.gwt.client.event.AnnotationInfoChangeEvent;
import org.bbop.apollo.gwt.client.event.AnnotationInfoChangeEventHandler;
import org.bbop.apollo.gwt.client.event.UserChangeEvent;
import org.bbop.apollo.gwt.client.event.UserChangeEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.UserRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.bbop.apollo.gwt.shared.PermissionEnum;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.Label;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ndunn on 12/17/14.
 */
public class AnnotatorPanel extends Composite {


    interface AnnotatorPanelUiBinder extends UiBinder<com.google.gwt.user.client.ui.Widget, AnnotatorPanel> {
    }

    private static AnnotatorPanelUiBinder ourUiBinder = GWT.create(AnnotatorPanelUiBinder.class);
    private DateTimeFormat outputFormat = DateTimeFormat.getFormat("MMM dd, yyyy");
    private Column<AnnotationInfo, String> nameColumn;
    private TextColumn<AnnotationInfo> typeColumn;
    private TextColumn<AnnotationInfo> sequenceColumn;
    private Column<AnnotationInfo, Number> lengthColumn;
    private Column<AnnotationInfo, String> dateColumn;
    private Column<AnnotationInfo, String> showHideColumn;
    private long requestIndex = 0;
    private static String selectedChildUniqueName = null;

    private static int selectedSubTabIndex = 0 ;


    private final String COLLAPSE_ICON_UNICODE = "\u25BC";
    private final String EXPAND_ICON_UNICODE = "\u25C0";

    @UiField
    TextBox nameSearchBox;
    @UiField(provided = true)
    org.gwtbootstrap3.client.ui.SuggestBox sequenceList;

    private static DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);

    @UiField(provided = true)
    static DataGrid<AnnotationInfo> dataGrid = new DataGrid<>(20, tablecss);
    @UiField(provided = true)
    WebApolloSimplePager pager = null;

    @UiField
    ListBox typeList;
    @UiField
    static GeneDetailPanel geneDetailPanel;
    @UiField
    static TranscriptDetailPanel transcriptDetailPanel;
    @UiField
    static ExonDetailPanel exonDetailPanel;
    @UiField
    static RepeatRegionDetailPanel repeatRegionDetailPanel;
    @UiField
    static VariantDetailPanel variantDetailPanel;
    @UiField
    static VariantAllelesPanel variantAllelesPanel;
    @UiField
    static VariantInfoPanel variantInfoPanel;
    @UiField
    static AlleleInfoPanel alleleInfoPanel;
    @UiField
    static TabLayoutPanel tabPanel;
    @UiField
    ListBox userField;
    @UiField
    DockLayoutPanel splitPanel;
    @UiField
    Container northPanelContainer;
    @UiField
    static Button gotoAnnotation;

    private static AnnotationInfo selectedAnnotationInfo;
    private MultiWordSuggestOracle sequenceOracle = new ReferenceSequenceOracle();

    private static AsyncDataProvider<AnnotationInfo> dataProvider;
    private SingleSelectionModel<AnnotationInfo> singleSelectionModel = new SingleSelectionModel<>();
    private final Set<String> showingTranscripts = new HashSet<String>();

    public AnnotatorPanel() {
        sequenceList = new org.gwtbootstrap3.client.ui.SuggestBox(sequenceOracle);
        sequenceList.getElement().setAttribute("placeHolder", "Reference Sequence");
        dataGrid.setWidth("100%");
        dataGrid.setTableBuilder(new CustomTableBuilder());
        dataGrid.setLoadingIndicator(new Label("Loading"));
        dataGrid.setEmptyTableWidget(new Label("No results"));
        initializeTable();


        pager = new WebApolloSimplePager(WebApolloSimplePager.TextLocation.CENTER);

        dataGrid.addCellPreviewHandler(new CellPreviewEvent.Handler<AnnotationInfo>() {
            @Override
            public void onCellPreview(CellPreviewEvent<AnnotationInfo> event) {
                AnnotationInfo annotationInfo = event.getValue();
                if (event.getNativeEvent().getType().equals(BrowserEvents.CLICK)) {
                    if (event.getContext().getSubIndex() == 0) {
                        // subIndex from dataGrid will be 0 only when top-level cell values are clicked
                        // ie. gene, pseudogene
                        GWT.log("Safe to call updateAnnotationInfo");
                        updateAnnotationInfo(annotationInfo);
                    }
                }
            }
        });


        exportStaticMethod(this);


        initWidget(ourUiBinder.createAndBindUi(this));

        dataProvider = new AsyncDataProvider<AnnotationInfo>() {
            @Override
            protected void onRangeChanged(HasData<AnnotationInfo> display) {
                final Range range = display.getVisibleRange();
                final ColumnSortList sortList = dataGrid.getColumnSortList();
                final int start = range.getStart();
                final int length = range.getLength();
                String sequenceName = sequenceList.getText().trim();


                String url = Annotator.getRootUrl() + "annotator/findAnnotationsForSequence/?sequenceName=" + sequenceName;
                url += "&request=" + requestIndex;
                url += "&offset=" + start + "&max=" + length;
                url += "&annotationName=" + nameSearchBox.getText() + "&type=" + typeList.getSelectedValue();
                url += "&user=" + userField.getSelectedValue();
                url += "&clientToken=" + Annotator.getClientToken();


                ColumnSortList.ColumnSortInfo nameSortInfo = sortList.get(0);
                Column<AnnotationInfo, ?> sortColumn = (Column<AnnotationInfo, ?>) sortList.get(0).getColumn();
                Integer columnIndex = dataGrid.getColumnIndex(sortColumn);
                String searchColumnString = null;
                switch (columnIndex) {
                    case 0:
                        searchColumnString = "name";
                        break;
                    case 1:
                        searchColumnString = "sequence";
                        break;
                    case 3:
                        searchColumnString = "length";
                        break;
                    case 4:
                        searchColumnString = "date";
                    default:
                        break;
                }
                Boolean sortNameAscending = nameSortInfo.isAscending();
                url += "&sortorder=" + (sortNameAscending ? "asc" : "desc");
                url += "&sort=" + searchColumnString;

                RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
                builder.setHeader("Content-type", "application/x-www-form-urlencoded");
                RequestCallback requestCallback = new RequestCallback() {
                    @Override
                    public void onResponseReceived(Request request, Response response) {
                        JSONValue returnValue = null;
                        try {
                            returnValue = JSONParser.parseStrict(response.getText());
                        } catch (Exception e) {
                            Bootbox.alert(e.getMessage());
                        }
                        JSONValue localRequestObject = returnValue.isObject().get(FeatureStringEnum.REQUEST_INDEX.getValue());
                        if (localRequestObject != null) {
                            long localRequestValue = (long) localRequestObject.isNumber().doubleValue();
                            if (localRequestValue <= requestIndex) {
                                return;
                            } else {
                                requestIndex = localRequestValue;
                            }
                            Integer annotationCount = (int) returnValue.isObject().get(FeatureStringEnum.ANNOTATION_COUNT.getValue()).isNumber().doubleValue();

                            JSONArray jsonArray = returnValue.isObject().get(FeatureStringEnum.FEATURES.getValue()).isArray();

                            dataGrid.setRowCount(annotationCount, true);
                            final List<AnnotationInfo> annotationInfoList = AnnotationInfoConverter.convertFromJsonArray(jsonArray);
                            dataGrid.setRowData(start, annotationInfoList);
                            if (annotationInfoList.size() == 1) {
                                selectedAnnotationInfo = annotationInfoList.get(0);
                                String type = selectedAnnotationInfo.getType();
                                if (!type.equals("repeat_region") && !type.equals("transposable_element")) {
                                    toggleOpen(1, selectedAnnotationInfo);
                                }

                            }

                            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                                @Override
                                public void execute() {
                                    if (selectedAnnotationInfo != null) {
//                                    Window.alert("setting data: "+selectedAnnotationInfo.getName());
                                        // refind and update internally
                                        for (AnnotationInfo annotationInfo : annotationInfoList) {
                                            GWT.log("top-level: " + annotationInfo.getType());
                                            // will be found if a top-level selection
                                            if (annotationInfo.getUniqueName().equals(selectedAnnotationInfo.getUniqueName())) {
                                                selectedAnnotationInfo = annotationInfo;
                                                singleSelectionModel.clear();
                                                singleSelectionModel.setSelected(selectedAnnotationInfo, true);
                                                updateAnnotationInfo(selectedAnnotationInfo);
                                                return;
                                            }
                                            // if a child, we need to get the index I think?
                                            final String thisUniqueName = selectedChildUniqueName;
                                            for (AnnotationInfo annotationInfoChild : annotationInfo.getAnnotationInfoSet()) {
                                                GWT.log("next-level: " + annotationInfoChild.getType());
                                                if (annotationInfoChild.getUniqueName().equals(selectedAnnotationInfo.getUniqueName())) {
//                                                    selectedAnnotationInfo = annotationInfo;
                                                    selectedAnnotationInfo = getChildAnnotation(annotationInfo, thisUniqueName);
                                                    singleSelectionModel.clear();
                                                    singleSelectionModel.setSelected(selectedAnnotationInfo, true);
                                                    updateAnnotationInfo(selectedAnnotationInfo);
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                }
                            });
                        }

                    }

                    @Override
                    public void onError(Request request, Throwable exception) {
                        Bootbox.alert("Error loading organisms");
                    }
                };
                try {
                    if (MainPanel.getInstance().getCurrentUser() != null) {
                        builder.setCallback(requestCallback);
                        builder.send();
                    }
                } catch (RequestException e) {
                    // Couldn't connect to server
                    Bootbox.alert(e.getMessage());
                }
            }
        };

        ColumnSortEvent.AsyncHandler columnSortHandler = new ColumnSortEvent.AsyncHandler(dataGrid);
        dataGrid.addColumnSortHandler(columnSortHandler);
        dataGrid.getColumnSortList().push(nameColumn);
        dataGrid.getColumnSortList().push(sequenceColumn);
        dataGrid.getColumnSortList().push(lengthColumn);
        dataGrid.getColumnSortList().push(dateColumn);

        dataProvider.addDataDisplay(dataGrid);
        pager.setDisplay(dataGrid);


        initializeTypes();
        initializeUsers();

        sequenceList.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {
            @Override
            public void onSelection(SelectionEvent<SuggestOracle.Suggestion> event) {
                reload();
            }
        });

        sequenceList.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                if (sequenceList.getText() == null || sequenceList.getText().trim().length() == 0) {
                    reload();
                }
            }
        });


        tabPanel.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> event) {
                selectedSubTabIndex = event.getSelectedItem();
                switch(selectedSubTabIndex) {
                    case 0:
                        break;
                    case 1:
                        exonDetailPanel.redrawExonTable();
                        break;
                    case 2:
                        variantAllelesPanel.redrawTable();
                        break;
                    case 3:
                        variantInfoPanel.redrawTable();
                    case 4:
                        alleleInfoPanel.redrawTable();
                }
            }
        });

        Annotator.eventBus.addHandler(AnnotationInfoChangeEvent.TYPE, new AnnotationInfoChangeEventHandler() {
            @Override
            public void onAnnotationChanged(AnnotationInfoChangeEvent annotationInfoChangeEvent) {
                reload();
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
                                boolean editable = false;
                                switch (hiPermissionEnum) {
                                    case ADMINISTRATE:
                                    case WRITE:
                                        editable = true;
                                        break;
                                    // default is false
                                }
                                transcriptDetailPanel.setEditable(editable);
                                geneDetailPanel.setEditable(editable);
                                exonDetailPanel.setEditable(editable);
                                repeatRegionDetailPanel.setEditable(editable);
                                reload();
                                break;
                        }
                    }
                }
        );

        // TODO: not sure if this was necessary, leaving it here until it fails
//        Annotator.eventBus.addHandler(OrganismChangeEvent.TYPE, new OrganismChangeEventHandler() {
//            @Override
//            public void onOrganismChanged(OrganismChangeEvent organismChangeEvent) {
//                if (organismChangeEvent.getAction() == OrganismChangeEvent.Action.LOADED_ORGANISMS) {
//                    sequenceList.setText(organismChangeEvent.getCurrentSequence());
//                    reload();
//                }
//            }
//        });

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                userField.setVisible(true);
            }
        });

    }


    private void initializeUsers() {
        userField.clear();
        userField.addItem("All Users", "");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();

                for (int i = 0; array != null && i < array.size(); i++) {
                    JSONObject object = array.get(i).isObject();
                    UserInfo userInfo = UserInfoConverter.convertToUserInfoFromJSON(object);
                    userField.addItem(userInfo.getName(), userInfo.getEmail());
                }

            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error retrieving users: " + exception.fillInStackTrace());
            }
        };
        if (MainPanel.getInstance().getCurrentUser() != null) {
            UserRestService.loadUsers(requestCallback);
        }
    }

    private void initializeTypes() {
        typeList.addItem("All Types", "");
        typeList.addItem("Gene");
        typeList.addItem("Pseudogene");
        typeList.addItem("Transposable Element", "transposable_element");
        typeList.addItem("Repeat Region", "repeat_region");
        typeList.addItem("Variant", "sequence_alteration");
    }

    private static void hideDetailPanels() {
        geneDetailPanel.setVisible(false);
        transcriptDetailPanel.setVisible(false);
        repeatRegionDetailPanel.setVisible(false);
//        exonDetailPanel.setVisible(false);
    }

    private static void updateAnnotationInfo(AnnotationInfo annotationInfo) {
        if (annotationInfo == null) {
            return;
        }
        String type = annotationInfo.getType();
        GWT.log("annotation type: " + type);
        hideDetailPanels();
        switch (type) {
            case "gene":
            case "pseudogene":
                geneDetailPanel.updateData(annotationInfo);
                tabPanel.getTabWidget(0).getParent().setVisible(true);
                tabPanel.getTabWidget(1).getParent().setVisible(false);
                tabPanel.getTabWidget(2).getParent().setVisible(false);
                tabPanel.getTabWidget(3).getParent().setVisible(false);
                tabPanel.getTabWidget(4).getParent().setVisible(false);
                break;
            case "transcript":
                transcriptDetailPanel.updateData(annotationInfo);
                tabPanel.getTabWidget(1).getParent().setVisible(true);
                exonDetailPanel.updateData(annotationInfo, selectedAnnotationInfo);
                tabPanel.getTabWidget(0).getParent().setVisible(true);
                tabPanel.getTabWidget(1).getParent().setVisible(true);
                tabPanel.getTabWidget(2).getParent().setVisible(false);
                tabPanel.getTabWidget(3).getParent().setVisible(false);
                tabPanel.getTabWidget(4).getParent().setVisible(false);
                break;
            case "mRNA":
            case "miRNA":
            case "tRNA":
            case "rRNA":
            case "snRNA":
            case "snoRNA":
            case "ncRNA":
                transcriptDetailPanel.updateData(annotationInfo);
                exonDetailPanel.updateData(annotationInfo, selectedAnnotationInfo);
                tabPanel.getTabWidget(0).getParent().setVisible(true);
                tabPanel.getTabWidget(1).getParent().setVisible(true);
                tabPanel.getTabWidget(2).getParent().setVisible(false);
                tabPanel.getTabWidget(3).getParent().setVisible(false);
                tabPanel.getTabWidget(4).getParent().setVisible(false);
                break;
            case "transposable_element":
            case "repeat_region":
                repeatRegionDetailPanel.updateData(annotationInfo);
                tabPanel.getTabWidget(0).getParent().setVisible(true);
                tabPanel.getTabWidget(1).getParent().setVisible(false);
                tabPanel.getTabWidget(2).getParent().setVisible(false);
                tabPanel.getTabWidget(3).getParent().setVisible(false);
                tabPanel.getTabWidget(4).getParent().setVisible(false);
                break;
            case "SNV":
            case "SNP":
            case "MNV":
            case "MNP":
            case "indel":
                variantDetailPanel.updateData(annotationInfo);
                variantAllelesPanel.updateData(annotationInfo);
                variantInfoPanel.updateData(annotationInfo);
                alleleInfoPanel.updateData(annotationInfo);
                tabPanel.getTabWidget(0).getParent().setVisible(true);
                tabPanel.getTabWidget(1).getParent().setVisible(false);
                tabPanel.getTabWidget(2).getParent().setVisible(true);
                tabPanel.getTabWidget(3).getParent().setVisible(true);
                tabPanel.getTabWidget(4).getParent().setVisible(true);
                break;
            default:
                GWT.log("not sure what to do with " + type);
        }
        reselectSubTab();


    }

    private static void reselectSubTab() {
        // attempt to selectt the last tab
        if(tabPanel.getSelectedIndex()!=selectedSubTabIndex){
            tabPanel.selectTab(selectedSubTabIndex);
        }

        // if current tab is not visible, then select tab 0
        while(!tabPanel.getTabWidget(selectedSubTabIndex).getParent().isVisible() && selectedSubTabIndex>=0){
            --selectedSubTabIndex ;
            tabPanel.selectTab(selectedSubTabIndex);
        }
    }

    public static void fireAnnotationInfoChangeEvent(AnnotationInfo annotationInfo) {
        // this method is for firing AnnotationInfoChangeEvent for single level features such as transposable_element and repeat_region
        AnnotationInfoChangeEvent annotationInfoChangeEvent = new AnnotationInfoChangeEvent(annotationInfo, AnnotationInfoChangeEvent.Action.SET_FOCUS);
        Annotator.eventBus.fireEvent(annotationInfoChangeEvent);
    }

    public void toggleOpen(int index, AnnotationInfo annotationInfo) {
        if (showingTranscripts.contains(annotationInfo.getUniqueName())) {
            showingTranscripts.remove(annotationInfo.getUniqueName());
        } else {
            showingTranscripts.add(annotationInfo.getUniqueName());
        }

        // Redraw the modified row.
        if(index < dataGrid.getRowCount()){
            dataGrid.redrawRow(index);
        }
    }

    private void initializeTable() {
        // View friends.
        SafeHtmlRenderer<String> anchorRenderer = new AbstractSafeHtmlRenderer<String>() {
            @Override
            public SafeHtml render(String object) {
                SafeHtmlBuilder sb = new SafeHtmlBuilder();
                sb.appendHtmlConstant("<a href=\"javascript:;\">").appendEscaped(object)
                        .appendHtmlConstant("</a>");
                return sb.toSafeHtml();
            }
        };


        nameColumn = new Column<AnnotationInfo, String>(new ClickableTextCell(anchorRenderer)) {
            @Override
            public String getValue(AnnotationInfo annotationInfo) {
                return annotationInfo.getName();
            }
        };
        nameColumn.setSortable(true);

        showHideColumn = new Column<AnnotationInfo, String>(new ClickableTextCell(anchorRenderer)) {
            @Override
            public String getValue(AnnotationInfo annotationInfo) {
                if (annotationInfo.getType().equals("gene") || annotationInfo.getType().equals("pseudogene")) {
                    SafeHtmlBuilder sb = new SafeHtmlBuilder();
                    if (showingTranscripts.contains(annotationInfo.getUniqueName())) {
                        sb.appendHtmlConstant(COLLAPSE_ICON_UNICODE);
                    } else {
                        sb.appendHtmlConstant(EXPAND_ICON_UNICODE);
                    }

                    return sb.toSafeHtml().asString();
                }
                return " ";
            }
        };
        showHideColumn.setSortable(false);

        showHideColumn.setFieldUpdater(new FieldUpdater<AnnotationInfo, String>() {
            @Override
            public void update(int index, AnnotationInfo annotationInfo, String value) {
                toggleOpen(index, annotationInfo);
            }
        });

        sequenceColumn = new TextColumn<AnnotationInfo>() {
            @Override
            public String getValue(AnnotationInfo annotationInfo) {
                return annotationInfo.getSequence();
            }
        };
        sequenceColumn.setSortable(true);
        sequenceColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        typeColumn = new TextColumn<AnnotationInfo>() {
            @Override
            public String getValue(AnnotationInfo annotationInfo) {

                String type = annotationInfo.getType();
                switch (type) {
                    case "repeat_region":
                        return "repeat rgn";
                    case "transposable_element":
                        return "transp elem";
                    default:
                        return type;
                }
            }
        };
        typeColumn.setSortable(false);
        typeColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        lengthColumn = new Column<AnnotationInfo, Number>(new NumberCell()) {
            @Override
            public Integer getValue(AnnotationInfo annotationInfo) {
                return annotationInfo.getLength();
            }
        };
        lengthColumn.setSortable(true);
        lengthColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        lengthColumn.setCellStyleNames("dataGridLastColumn");

        // unused?
        dateColumn = new Column<AnnotationInfo, String>(new TextCell()) {
            @Override
            public String getValue(AnnotationInfo annotationInfo) {
                return annotationInfo.getDate();
            }
        };
        dateColumn.setSortable(true);
        dateColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        dateColumn.setCellStyleNames("dataGridLastColumn");

        dataGrid.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                AnnotationInfo annotationInfo = singleSelectionModel.getSelectedObject();
                int index = dataGrid.getKeyboardSelectedRow();
                index += pager.getPage() * pager.getPageSize();
                toggleOpen(index, annotationInfo);

            }
        }, DoubleClickEvent.getType());

        singleSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                selectedAnnotationInfo = singleSelectionModel.getSelectedObject();
                if (selectedAnnotationInfo != null) {
                    exonDetailPanel.updateData(selectedAnnotationInfo);
                    gotoAnnotation.setEnabled(true);
                } else {
                    exonDetailPanel.updateData();
                    gotoAnnotation.setEnabled(false);
                }
            }
        });

        dataGrid.addColumn(nameColumn, "Name");
        dataGrid.addColumn(sequenceColumn, "Seq");
        dataGrid.addColumn(typeColumn, "Type");
        dataGrid.addColumn(lengthColumn, "Length");
        dataGrid.addColumn(dateColumn, "Updated");
        dataGrid.addColumn(showHideColumn, "");
        dataGrid.setColumnWidth(0, 75, Unit.PCT);
        dataGrid.setColumnWidth(1, 25, Unit.PCT);
        dataGrid.setColumnWidth(2, 45.0, Unit.PX);
        dataGrid.setColumnWidth(3, 65.0, Unit.PX);
        dataGrid.setColumnWidth(4, 100.0, Unit.PX);
        dataGrid.setColumnWidth(5, 30.0, Unit.PX);

        dataGrid.setSelectionModel(singleSelectionModel);
    }

    private String getType(JSONObject internalData) {
        return internalData.get("type").isObject().get("name").isString().stringValue();
    }

    public void reload(Boolean forceReload) {
        if (MainPanel.annotatorPanel.isVisible() || forceReload) {
            hideDetailPanels();
            pager.setPageStart(0);
            dataGrid.setVisibleRangeAndClearData(dataGrid.getVisibleRange(), true);
        }
    }

    public void reload() {
        reload(false);
    }


    @UiHandler(value = {"typeList", "userField"})
    public void searchType(ChangeEvent changeEvent) {
        reload();
    }

    @UiHandler("nameSearchBox")
    public void searchName(KeyUpEvent keyUpEvent) {
        reload();
    }

    @UiHandler("showAllSequences")
    public void setShowAllSequences(ClickEvent clickEvent) {
        nameSearchBox.setText("");
        sequenceList.setText("");
        reload();
    }

    @UiHandler("gotoAnnotation")
    void gotoAnnotation(ClickEvent clickEvent) {
        Integer min = selectedAnnotationInfo.getMin() - 50;
        Integer max = selectedAnnotationInfo.getMax() + 50;
        min = min < 0 ? 0 : min;
        MainPanel.updateGenomicViewerForLocation(selectedAnnotationInfo.getSequence(), min, max, false, false);
    }


    private static AnnotationInfo getChildAnnotation(AnnotationInfo annotationInfo, String uniqueName) {
        for (AnnotationInfo childAnnotation : annotationInfo.getAnnotationInfoSet()) {
            if (childAnnotation.getUniqueName().equalsIgnoreCase(uniqueName)) {
                return childAnnotation;
            }
        }
        return null;
    }


    // used by javascript function
    public void enableGoto(int geneIndex, String uniqueName) {
        AnnotationInfo annotationInfo = dataGrid.getVisibleItem(Math.abs(dataGrid.getVisibleRange().getStart() - geneIndex));
        selectedAnnotationInfo = getChildAnnotation(annotationInfo, uniqueName);
        exonDetailPanel.updateData(selectedAnnotationInfo);
        updateAnnotationInfo(selectedAnnotationInfo);
        gotoAnnotation.setEnabled(true);
        selectedChildUniqueName = selectedAnnotationInfo.getUniqueName();
    }

    // used by javascript function
    public void displayTranscript(int geneIndex, String uniqueName) {

        // for some reason doesn't like call enableGoto
        AnnotationInfo annotationInfo = dataGrid.getVisibleItem(Math.abs(dataGrid.getVisibleRange().getStart() - geneIndex));
        selectedAnnotationInfo = getChildAnnotation(annotationInfo, uniqueName);
        exonDetailPanel.updateData(selectedAnnotationInfo);
        gotoAnnotation.setEnabled(true);
        selectedChildUniqueName = selectedAnnotationInfo.getUniqueName();

        // for some reason doesn't like call gotoAnnotation
        Integer min = selectedAnnotationInfo.getMin() - 50;
        Integer max = selectedAnnotationInfo.getMax() + 50;
        min = min < 0 ? 0 : min;
        MainPanel.updateGenomicViewerForLocation(selectedAnnotationInfo.getSequence(), min, max, false, false);
    }

    // also used by javascript function
    public void displayFeature(int featureIndex) {
        AnnotationInfo annotationInfo = dataGrid.getVisibleItem(Math.abs(dataGrid.getVisibleRange().getStart() - featureIndex));
        String type = annotationInfo.getType();
        if (type.equals("transposable_element") || type.equals("repeat_region")) {
            // do nothing
        } else {
            exonDetailPanel.updateData(annotationInfo);
        }
        gotoAnnotation.setEnabled(true);
        Integer min = selectedAnnotationInfo.getMin() - 50;
        Integer max = selectedAnnotationInfo.getMax() + 50;
        min = min < 0 ? 0 : min;
        MainPanel.updateGenomicViewerForLocation(selectedAnnotationInfo.getSequence(), min, max, false, false);
    }

    public static native void exportStaticMethod(AnnotatorPanel annotatorPanel) /*-{
        var that = this;
        $wnd.displayTranscript = $entry(annotatorPanel.@org.bbop.apollo.gwt.client.AnnotatorPanel::displayTranscript(ILjava/lang/String;));
        $wnd.displayFeature = $entry(annotatorPanel.@org.bbop.apollo.gwt.client.AnnotatorPanel::displayFeature(I));
        $wnd.enableGoto = $entry(annotatorPanel.@org.bbop.apollo.gwt.client.AnnotatorPanel::enableGoto(ILjava/lang/String;));
//        $wnd.showInAnnotatorPanel = $entry(@org.bbop.apollo.gwt.client.AnnotatorPanel::showInAnnotatorPanel(Ljava/lang/String;Ljava/lang/String;));
    }-*/;

    private class CustomTableBuilder extends AbstractCellTableBuilder<AnnotationInfo> {

        public CustomTableBuilder() {
            super(dataGrid);
        }


        @Override
        protected void buildRowImpl(AnnotationInfo rowValue, int absRowIndex) {
            buildAnnotationRow(rowValue, absRowIndex, false);

            if (showingTranscripts.contains(rowValue.getUniqueName())) {
                // add some random rows
                Set<AnnotationInfo> annotationInfoSet = rowValue.getAnnotationInfoSet();
                if (annotationInfoSet.size() > 0) {
                    for (AnnotationInfo annotationInfo : annotationInfoSet) {
                        buildAnnotationRow(annotationInfo, absRowIndex, true);
                    }
                }
            }
        }

        private void buildAnnotationRow(final AnnotationInfo rowValue, int absRowIndex, boolean showTranscripts) {

            TableRowBuilder row = startRow();
            TableCellBuilder td = row.startTD();

            td.style().outlineStyle(Style.OutlineStyle.NONE).endStyle();
            if (showTranscripts) {
                // TODO: this is ugly, but it works
                // a custom cell rendering might work as well, but not sure

                String transcriptStyle = "margin-left: 10px; color: green; padding-left: 5px; padding-right: 5px; border-radius: 15px; background-color: #EEEEEE;";
                String htmlString = "<a style='" + transcriptStyle + "' onclick=\"enableGoto(" + absRowIndex + ",'" + rowValue.getUniqueName() + "');\">" + rowValue.getName() + "</a>";
                htmlString += "  <button type='button' class='btn btn-primary' onclick=\"displayTranscript(" + absRowIndex + ",'" + rowValue.getUniqueName() + "')\" style=\"line-height: 0; margin-bottom: 5px;\" ><i class='fa fa-arrow-circle-o-right fa-lg'></i></a>";
                HTML html = new HTML(htmlString);
                SafeHtml safeHtml = new SafeHtmlBuilder().appendHtmlConstant(html.getHTML()).toSafeHtml();
                td.html(safeHtml);
            } else {
                String type = rowValue.getType();
                if (type.equals("gene") || type.equals("pseudogene")) {
                    renderCell(td, createContext(0), nameColumn, rowValue);
                } else {
                    // handles singleton features
                    String featureStyle = "color: #800080;";
                    HTML html = new HTML("<a style='" + featureStyle + "' ondblclick=\"displayFeature(" + absRowIndex + ")\");\">" + rowValue.getName() + "</a>");
                    SafeHtml htmlString = new SafeHtmlBuilder().appendHtmlConstant(html.getHTML()).toSafeHtml();
                    td.html(htmlString);
                }
            }
            td.endTD();

            // Sequence column.
            td = row.startTD();
            td.style().outlineStyle(Style.OutlineStyle.NONE).endStyle();
            if (showTranscripts) {
                DivBuilder div = td.startDiv();
                div.style().trustedColor("green").endStyle();
                td.endDiv();
            } else {
                renderCell(td, createContext(1), sequenceColumn, rowValue);
            }
            td.endTD();

            // Type column.
            td = row.startTD();
            td.style().outlineStyle(Style.OutlineStyle.NONE).endStyle();
            if (showTranscripts) {
                DivBuilder div = td.startDiv();
                div.style().trustedColor("green").endStyle();
                div.text(rowValue.getType());
                td.endDiv();
            } else {
                renderCell(td, createContext(1), typeColumn, rowValue);
            }
            td.endTD();


            // Length column.
            td = row.startTD();
            td.style().outlineStyle(Style.OutlineStyle.NONE).endStyle();
            if (showTranscripts) {
                DivBuilder div = td.startDiv();
                div.style().trustedColor("green").endStyle();
                div.text(NumberFormat.getDecimalFormat().format(rowValue.getLength()));
                td.endDiv();
                td.endTD();

            } else {
                td.text(NumberFormat.getDecimalFormat().format(rowValue.getLength())).endTD();
            }

            // Date column
            td = row.startTD();
            td.style().outlineStyle(Style.OutlineStyle.NONE).endStyle();
            if (showTranscripts) {
                DivBuilder div = td.startDiv();
                div.style().trustedColor("green").endStyle();
                Date date = new Date(Long.parseLong(rowValue.getDate()));
                div.text(outputFormat.format(date));
                td.endDiv();
            } else {
                Date date = new Date(Long.parseLong(rowValue.getDate()));
                td.text(outputFormat.format(date));
            }
            td.endTD();

            // this is the "warning" column, which isn't used
            td = row.startTD();
            td.style().outlineStyle(Style.OutlineStyle.NONE).endStyle();

            renderCell(td, createContext(4), showHideColumn, rowValue);

            td.endTD();

            row.endTR();
        }
    }
}
