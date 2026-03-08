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
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.*;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.AnnotationInfoConverter;
import org.bbop.apollo.gwt.client.dto.UserInfo;
import org.bbop.apollo.gwt.client.dto.UserInfoConverter;
import org.bbop.apollo.gwt.client.event.*;
import org.bbop.apollo.gwt.client.oracles.ReferenceSequenceOracle;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.AnnotationRestService;
import org.bbop.apollo.gwt.client.rest.AvailableStatusRestService;
import org.bbop.apollo.gwt.client.rest.UserRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.bbop.apollo.gwt.shared.PermissionEnum;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.Label;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.SuggestBox;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ndunn on 12/17/14.
 */
public class AnnotatorPanel extends Composite {

    interface AnnotatorPanelUiBinder extends UiBinder<Widget, AnnotatorPanel> {
    }

    private static AnnotatorPanelUiBinder ourUiBinder = GWT.create(AnnotatorPanelUiBinder.class);
    private DateTimeFormat outputFormat = DateTimeFormat.getFormat("MMM dd, yyyy");
    private Column<AnnotationInfo, String> nameColumn;
    private TextColumn<AnnotationInfo> typeColumn;
    private TextColumn<AnnotationInfo> sequenceColumn;
    private Column<AnnotationInfo, Number> lengthColumn;
    private Column<AnnotationInfo, String> dateColumn;
    private Column<AnnotationInfo, String> showHideColumn;
    private static long requestIndex = 0;
    String selectedChildUniqueName ;

    private static int selectedSubTabIndex = 0;
    private static int pageSize = 50;

    private final String COLLAPSE_ICON_UNICODE = "\u25BC";
    private final String EXPAND_ICON_UNICODE = "\u25C0";
    private boolean queryViewInRangeOnly = false;

    @UiField
    TextBox nameSearchBox;
    @UiField(provided = true)
    SuggestBox sequenceList;

    private static DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);

    @UiField(provided = true)
    static DataGrid<AnnotationInfo> dataGrid = new DataGrid<>(pageSize, tablecss);
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
    static DockLayoutPanel splitPanel;
    @UiField
    Container northPanelContainer;
    @UiField
    com.google.gwt.user.client.ui.ListBox pageSizeSelector;
    @UiField
    static GoPanel goPanel;
    @UiField
    static GeneProductPanel geneProductPanel;
    @UiField
    static ProvenancePanel provenancePanel;
    @UiField
    Button goOnlyCheckBox;
    @UiField
    static DbXrefPanel dbXrefPanel;
    @UiField
    static CommentPanel commentPanel;
    @UiField
    static AttributePanel attributePanel;
    @UiField
    CheckBox uniqueNameCheckBox;
    @UiField
    Button showAllSequences;
    @UiField
    Button showCurrentView;
    @UiField
    ListBox statusField;
    @UiField
    static HTML annotationDescription;
  @UiField
  static DockLayoutPanel annotatorDetailPanel;
  @UiField
  static Hyperlink closeDetailsButton;
  @UiField
  static Hyperlink annotationLinkButton;
    @UiField
    Button geneProductOnlyCheckBox;
    @UiField
    Button provenanceOnlyCheckBox;


    // manage UI-state
    static AnnotationInfo selectedAnnotationInfo;
    private SingleSelectionModel<AnnotationInfo> singleSelectionModel = new SingleSelectionModel<>();
    private final Set<String> showingTranscripts = new HashSet<String>();

    public enum TAB_INDEX {
        DETAILS(0),
        CODING(1),
        ALTERNATE_ALLELES(2),
        VARIANT_INFO(3),
        ALLELE_INFO(4),
        GO(5),
        GENE_PRODUCT(6),
        PROVENANCE(7),
        DB_XREF(8),
        COMMENT(9),
        ATTRIBUTES(10),
        ;

        public int index;

        TAB_INDEX(int index) {
            this.index = index;
        }

        public static TAB_INDEX getTabEnumForIndex(int selectedSubTabIndex) {
            for (TAB_INDEX value : values()) {
                if (value.index == selectedSubTabIndex) {
                    return value;
                }
            }
            return null;
        }

        public int getIndex() {
            return index;
        }
    }


    public AnnotatorPanel() {
        sequenceList = new SuggestBox(new ReferenceSequenceOracle());
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
                        updateAnnotationInfo(annotationInfo);
                    }
                }
            }
        });


        exportStaticMethod(this);


        initWidget(ourUiBinder.createAndBindUi(this));

        handleDetails();

        AsyncDataProvider<AnnotationInfo> dataProvider = new AsyncDataProvider<AnnotationInfo>() {
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
                url += "&annotationName=" + nameSearchBox.getText();
                url += "&type=" + typeList.getSelectedValue();
                url += "&user=" + userField.getSelectedValue();
                url += "&statusString=" + statusField.getSelectedValue();
                url += "&clientToken=" + Annotator.getClientToken();
                url += "&showOnlyGoAnnotations=" + goOnlyCheckBox.isActive();
                url += "&showOnlyGeneProductAnnotations=" + geneProductOnlyCheckBox.isActive();
                url += "&showOnlyProvenanceAnnotations=" + provenanceOnlyCheckBox.isActive();
                url += "&searchUniqueName=" + uniqueNameCheckBox.getValue();
                if (queryViewInRangeOnly) {
                    url += "&range=" + MainPanel.getRange();
                    queryViewInRangeOnly = false;
                }


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
                            int annotationCount = (int) returnValue.isObject().get(FeatureStringEnum.ANNOTATION_COUNT.getValue()).isNumber().doubleValue();
                            JSONArray jsonArray = returnValue.isObject().get(FeatureStringEnum.FEATURES.getValue()).isArray();

                            dataGrid.setRowCount(annotationCount, true);
                            final List<AnnotationInfo> annotationInfoList = AnnotationInfoConverter.convertFromJsonArray(jsonArray);
                            dataGrid.setRowData(start, annotationInfoList);

                            // if a single entry
                            if (annotationInfoList.size() == 1) {
                                String type = annotationInfoList.get(0).getType();
                                if ( (!type.equals("gene") && !type.equals("pseudogene")) || uniqueNameCheckBox.getValue()) {
                                    selectedAnnotationInfo = annotationInfoList.get(0);
                                        // if a child, we need to get the index I think?
                                    if(selectedChildUniqueName==null  || selectedAnnotationInfo.getChildAnnotations().size()==0) {
                                        updateAnnotationInfo(selectedAnnotationInfo);
                                        return ;
                                    }
                                    else{
                                        for (AnnotationInfo annotationInfoChild : selectedAnnotationInfo.getChildAnnotations()) {
                                            if (annotationInfoChild.getUniqueName().equals(selectedChildUniqueName)) {
                                                selectedAnnotationInfo = getChildAnnotation(selectedAnnotationInfo, selectedChildUniqueName);
                                                singleSelectionModel.clear();
                                                singleSelectionModel.setSelected(selectedAnnotationInfo, true);
                                                updateAnnotationInfo(selectedAnnotationInfo);
                                                return;
                                            }
                                        }
                                    }

                                }
                            }

                            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                                @Override
                                public void execute() {
                                    if (selectedAnnotationInfo != null) {
                                        // refind and update internally
                                        for (AnnotationInfo annotationInfo : annotationInfoList) {
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
                                            for (AnnotationInfo annotationInfoChild : annotationInfo.getChildAnnotations()) {
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

        pageSizeSelector.addItem("10");
        pageSizeSelector.addItem("25");
        pageSizeSelector.addItem("50");
        pageSizeSelector.addItem("100");
        pageSizeSelector.addItem("500");
        pageSizeSelector.setSelectedIndex(1);

        initializeTypes();

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
                switch (TAB_INDEX.getTabEnumForIndex(selectedSubTabIndex)) {
                    case DETAILS:
                        break;
                    case CODING:
                        exonDetailPanel.redrawExonTable();
                        break;
                    case ALTERNATE_ALLELES:
                        variantAllelesPanel.redrawTable();
                        break;
                    case VARIANT_INFO:
                        variantInfoPanel.redrawTable();
                    case ALLELE_INFO:
                        alleleInfoPanel.redrawTable();
                    case GO:
                        goPanel.redraw();
                    case GENE_PRODUCT:
                        geneProductPanel.redraw();
                    case PROVENANCE:
                        provenancePanel.redraw();
                    case DB_XREF:
                        dbXrefPanel.redrawTable();
                        break;
                    case COMMENT:
                        commentPanel.redrawTable();
                        break;
                    case ATTRIBUTES:
                        attributePanel.redrawTable();
                }
            }
        });

        Annotator.eventBus.addHandler(OrganismChangeEvent.TYPE, new OrganismChangeEventHandler() {
            @Override
            public void onOrganismChanged(OrganismChangeEvent authenticationEvent) {
                initializeStatus();
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
//                                variantAllelesPanel.setEditable(editable);
//                                variantInfoPanel.setEditable(editable);
//                                alleleInfoPanel.setEditable(editable);
                                goPanel.setEditable(editable);
                                geneProductPanel.setEditable(editable);
                                provenancePanel.setEditable(editable);
                                attributePanel.setEditable(editable);


                                dbXrefPanel.setEditable(editable);
                                commentPanel.setEditable(editable);
                                reload();
                                break;
                        }
                    }
                }
        );

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                initializeUsers();
                userField.setVisible(true);
                initializeStatus();
                statusField.setVisible(true);
            }
        });
    }

    AnnotationInfo getCurrentGene(){
        return geneDetailPanel.getInternalAnnotationInfo();
    }

  private static void closeAnnotatorDetailsPanels() {
    closeDetailsButton.setVisible(false);
    annotationLinkButton.setVisible(false);
    annotationDescription.setHTML("Select annotation to show details");
    splitPanel.setWidgetSize(annotatorDetailPanel,20);
    splitPanel.animate(200);
  }

  private static void openAnnotatorDetailsPanel() {
    closeDetailsButton.setVisible(true);
    annotationLinkButton.setVisible(true);
    splitPanel.setWidgetSize(annotatorDetailPanel,460);
    splitPanel.animate(200);
  }


  void selectTranscriptPanel() {
        AnnotationInfo selectedObject = singleSelectionModel.getSelectedObject();
        updateAnnotationInfo(selectedObject);
        tabPanel.selectTab(0);
    }

    void selectGoPanel() {
        goPanel.redraw();
        tabPanel.selectTab(5);
    }

    private void initializeStatus() {
        statusField.setEnabled(false);
        statusField.clear();
        statusField.addItem("Loading...", "");
        final RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                if (response.getStatusCode() == 401) {
                    return;
                }
                statusField.setEnabled(true);
                statusField.clear();
                statusField.addItem("All Statuses", "");
                statusField.addItem("No Status Assigned", FeatureStringEnum.NO_STATUS_ASSIGNED.getValue());
                statusField.addItem("Any Status Assigned", FeatureStringEnum.ANY_STATUS_ASSIGNED.getValue());
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();
                for (int i = 0; array != null && i < array.size(); i++) {
                    String status = array.get(i).isString().stringValue();
                    statusField.addItem(status, status);
                }
                for (int i = 0; array != null && i < array.size(); i++) {
                    String status = array.get(i).isString().stringValue();
                    statusField.addItem("Assigned NOT "+status, FeatureStringEnum.NOT.getValue()+":"+status);
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error retrieving users: " + exception.fillInStackTrace());
            }
        };
        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (MainPanel.getInstance().getCurrentOrganism() != null) {
                    AvailableStatusRestService.getAvailableStatuses(requestCallback);
                    return false;
                }
                return true;
            }
        }, 1000);
    }

    protected void initializeUsers() {
        userField.clear();
        userField.addItem("All Users", "");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {

                if (response.getStatusCode() == 401) {
                    return;
                }

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
        if(MainPanel.getInstance().isCurrentUserAdmin()){
            UserRestService.loadUsers(requestCallback);
        }
    }

    private void initializeTypes() {
        typeList.addItem("All Types", "");
        typeList.addItem("Gene");
        typeList.addItem("Pseudogene");
        typeList.addItem("Transposable Element", "transposable_element");
        typeList.addItem("Terminator", "terminator");
        typeList.addItem("Shine Dalgarno sequence", "Shine_Dalgarno_sequence");
        typeList.addItem("Repeat Region", "repeat_region");
        typeList.addItem("Variant", "sequence_alteration");
    }

    private static void hideDetailPanels() {
        geneDetailPanel.setVisible(false);
        transcriptDetailPanel.setVisible(false);
        repeatRegionDetailPanel.setVisible(false);
        variantDetailPanel.setVisible(false);
    }

    private static void updateAnnotationInfo(AnnotationInfo annotationInfo) {

        if(selectedAnnotationInfo!=null){
            setAnnotationDescription(annotationInfo);
        }
        else{
            setAnnotationDescription(null);
        }

        if (annotationInfo == null) {
            annotationDescription.setHTML("Nothing selected");
            return;
        }
        String type = annotationInfo.getType();
        hideDetailPanels();
        switch (type) {
            case "gene":
            case "pseudogene":
            case "pseudogenic_region":
            case "processed_pseudogene":
                geneDetailPanel.updateData(annotationInfo);
                goPanel.updateData(annotationInfo);
                dbXrefPanel.updateData(annotationInfo);
                commentPanel.updateData(annotationInfo);
                attributePanel.updateData(annotationInfo);
                geneProductPanel.updateData(annotationInfo);
                provenancePanel.updateData(annotationInfo);
                tabPanel.getTabWidget(TAB_INDEX.DETAILS.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.CODING.index).getParent().setVisible(false);
                tabPanel.getTabWidget(TAB_INDEX.ALTERNATE_ALLELES.index).getParent().setVisible(false);
                tabPanel.getTabWidget(TAB_INDEX.VARIANT_INFO.index).getParent().setVisible(false);
                tabPanel.getTabWidget(TAB_INDEX.ALLELE_INFO.index).getParent().setVisible(false);
                tabPanel.getTabWidget(TAB_INDEX.GO.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.GENE_PRODUCT.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.PROVENANCE.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.DB_XREF.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.COMMENT.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.ATTRIBUTES.index).getParent().setVisible(true);
                tabPanel.setVisible(true);
                break;
            case "transcript":
                transcriptDetailPanel.updateData(annotationInfo);
                goPanel.updateData(annotationInfo);
                dbXrefPanel.updateData(annotationInfo);
                commentPanel.updateData(annotationInfo);
                attributePanel.updateData(annotationInfo);
                tabPanel.getTabWidget(TAB_INDEX.CODING.index).getParent().setVisible(true);
                exonDetailPanel.updateData(annotationInfo, selectedAnnotationInfo);
                tabPanel.getTabWidget(TAB_INDEX.DETAILS.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.CODING.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.ALTERNATE_ALLELES.index).getParent().setVisible(false);
                tabPanel.getTabWidget(TAB_INDEX.VARIANT_INFO.index).getParent().setVisible(false);
                tabPanel.getTabWidget(TAB_INDEX.ALLELE_INFO.index).getParent().setVisible(false);
                tabPanel.getTabWidget(TAB_INDEX.GO.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.GENE_PRODUCT.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.PROVENANCE.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.DB_XREF.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.COMMENT.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.ATTRIBUTES.index).getParent().setVisible(true);
                tabPanel.setVisible(true);
                break;
            case "mRNA":
            case "miRNA":
            case "tRNA":
            case "rRNA":
            case "snRNA":
            case "snoRNA":
            case "ncRNA":
            case "guide_RNA":
            case "RNase_MRP_RNA":
            case "telomerase_RNA":
            case "SRP_RNA":
            case "lnc_RNA":
            case "RNase_P_RNA":
            case "scRNA":
            case "piRNA":
            case "tmRNA":
            case "enzymatic_RNA":
                transcriptDetailPanel.updateData(annotationInfo);
                exonDetailPanel.updateData(annotationInfo, selectedAnnotationInfo);
                goPanel.updateData(annotationInfo);
                dbXrefPanel.updateData(annotationInfo);
                commentPanel.updateData(annotationInfo);
                attributePanel.updateData(annotationInfo);
                geneProductPanel.updateData(annotationInfo);
                provenancePanel.updateData(annotationInfo);
                tabPanel.getTabWidget(TAB_INDEX.DETAILS.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.CODING.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.ALTERNATE_ALLELES.index).getParent().setVisible(false);
                tabPanel.getTabWidget(TAB_INDEX.VARIANT_INFO.index).getParent().setVisible(false);
                tabPanel.getTabWidget(TAB_INDEX.ALLELE_INFO.index).getParent().setVisible(false);
                tabPanel.getTabWidget(TAB_INDEX.GO.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.GENE_PRODUCT.index).getParent().setVisible(type.equals("mRNA"));
                tabPanel.getTabWidget(TAB_INDEX.PROVENANCE.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.DB_XREF.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.COMMENT.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.ATTRIBUTES.index).getParent().setVisible(true);
                tabPanel.setVisible(true);
                break;
            case "terminator":
            case "transposable_element":
            case "repeat_region":
                repeatRegionDetailPanel.updateData(annotationInfo);
                dbXrefPanel.updateData(annotationInfo);
                commentPanel.updateData(annotationInfo);
                attributePanel.updateData(annotationInfo);
                tabPanel.getTabWidget(TAB_INDEX.DETAILS.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.CODING.index).getParent().setVisible(false);
                tabPanel.getTabWidget(TAB_INDEX.ALTERNATE_ALLELES.index).getParent().setVisible(false);
                tabPanel.getTabWidget(TAB_INDEX.VARIANT_INFO.index).getParent().setVisible(false);
                tabPanel.getTabWidget(TAB_INDEX.ALLELE_INFO.index).getParent().setVisible(false);
                tabPanel.getTabWidget(TAB_INDEX.GO.index).getParent().setVisible(false);
                tabPanel.getTabWidget(TAB_INDEX.GENE_PRODUCT.index).getParent().setVisible(false);
                tabPanel.getTabWidget(TAB_INDEX.PROVENANCE.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.DB_XREF.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.COMMENT.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.ATTRIBUTES.index).getParent().setVisible(true);
                tabPanel.setVisible(true);
                break;
            case "deletion":
            case "insertion":
            case "SNV":
            case "SNP":
            case "MNV":
            case "MNP":
            case "indel":
                variantDetailPanel.updateData(annotationInfo);
                variantAllelesPanel.updateData(annotationInfo);
                variantInfoPanel.updateData(annotationInfo);
                alleleInfoPanel.updateData(annotationInfo);
                dbXrefPanel.updateData(annotationInfo);
                commentPanel.updateData(annotationInfo);
                attributePanel.updateData(annotationInfo);
                tabPanel.getTabWidget(TAB_INDEX.DETAILS.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.CODING.index).getParent().setVisible(false);
                tabPanel.getTabWidget(TAB_INDEX.ALTERNATE_ALLELES.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.VARIANT_INFO.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.ALLELE_INFO.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.GO.index).getParent().setVisible(false);
                tabPanel.getTabWidget(TAB_INDEX.GENE_PRODUCT.index).getParent().setVisible(false);
                // we aren't exporting it, so not going to track it
                tabPanel.getTabWidget(TAB_INDEX.PROVENANCE.index).getParent().setVisible(false);
                tabPanel.getTabWidget(TAB_INDEX.DB_XREF.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.COMMENT.index).getParent().setVisible(true);
                tabPanel.getTabWidget(TAB_INDEX.ATTRIBUTES.index).getParent().setVisible(true);
                tabPanel.setVisible(true);
                break;
            default:
                GWT.log("not sure what to do with " + type);
        }


        reselectSubTab();


    }

    private static void setAnnotationDescription(AnnotationInfo annotationInfo) {
        if(annotationInfo!=null){
            annotationDescription.setHTML("&nbsp;&nbsp;&nbsp;&nbsp;<b>"+annotationInfo.getType()  + "</b>:  " + annotationInfo.getName() +"");
          openAnnotatorDetailsPanel();
        }
        else{
            annotationDescription.setHTML("&nbsp;&nbsp;&nbsp;&nbsp;Select annotation to show details");
          closeAnnotatorDetailsPanels();
        }
    }

    private static void reselectSubTab() {
        // attempt to select the last tab
        if (tabPanel.getSelectedIndex() != selectedSubTabIndex) {
            tabPanel.selectTab(selectedSubTabIndex);
        }

        // if current tab is not visible, then select tab 0
        while (!tabPanel.getTabWidget(selectedSubTabIndex).getParent().isVisible() && selectedSubTabIndex >= 0) {
            --selectedSubTabIndex;
            tabPanel.selectTab(selectedSubTabIndex);
        }

    }

    public void toggleOpen(int index, AnnotationInfo annotationInfo) {
        if (showingTranscripts.contains(annotationInfo.getUniqueName())) {
            showingTranscripts.remove(annotationInfo.getUniqueName());
        } else {
            showingTranscripts.add(annotationInfo.getUniqueName());
        }

        // Redraw the modified row.
        if (index < dataGrid.getRowCount()) {
            dataGrid.redrawRow(index);
        }
    }

    public void addOpenTranscript(String uniqueName) {
        showingTranscripts.add(uniqueName);
    }

    public void removeOpenTranscript(String uniqueName) {
        showingTranscripts.remove(uniqueName);
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
                    case "terminator":
                        return "terminator";
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
                return annotationInfo.getDateLastModified();
            }
        };
        dateColumn.setSortable(true);
        dateColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        dateColumn.setCellStyleNames("dataGridLastColumn");
        dateColumn.setDefaultSortAscending(false);

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
                tabPanel.setVisible(selectedAnnotationInfo != null);
                if (selectedAnnotationInfo != null) {
                    exonDetailPanel.updateData(selectedAnnotationInfo);
                    goPanel.updateData(selectedAnnotationInfo);
                    geneProductPanel.updateData(selectedAnnotationInfo);
                    provenancePanel.updateData(selectedAnnotationInfo);
                    dbXrefPanel.updateData(selectedAnnotationInfo);
                    commentPanel.updateData(selectedAnnotationInfo);
                    attributePanel.updateData(selectedAnnotationInfo);
                } else {
                    exonDetailPanel.updateData();
                    goPanel.updateData();
                    geneProductPanel.updateData();
                    provenancePanel.updateData();
                    dbXrefPanel.updateData();
                    commentPanel.updateData();
                    attributePanel.updateData();
                }
                setAnnotationDescription(selectedAnnotationInfo);
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
        showAllSequences.setEnabled(true);
        showAllSequences.setType(ButtonType.DEFAULT);
        if (MainPanel.annotatorPanel.isVisible() || forceReload) {
            setAnnotationDescription(null);
            hideDetailPanels();
            pager.setPageStart(0);
            dataGrid.setVisibleRangeAndClearData(dataGrid.getVisibleRange(), true);
        }
    }

    public void reload() {
        reload(false);
    }

    @UiHandler(value = {"statusField"})
    public void updateStatus(ChangeEvent changeEvent){
        reload();
    }

  @UiHandler(value = {"annotationLinkButton"})
  public void showAnnotationLink(ClickEvent clickEvent){
    String link =MainPanel.getInstance().generateApolloLink(selectedAnnotationInfo.getUniqueName());
    new LinkDialog("Link to '"+selectedAnnotationInfo.getName()+"'",link,true);
  }

  @UiHandler(value = {"closeDetailsButton"})
  public void closeDetails(ClickEvent clickEvent){
    closeAnnotatorDetailsPanels();
  }

  @UiHandler(value = {"pageSizeSelector"})
    public void changePageSize(ChangeEvent changeEvent) {
        pageSize = Integer.parseInt(pageSizeSelector.getSelectedValue());
        dataGrid.setPageSize(pageSize);
        reload();
    }

    @UiHandler(value = {"goOnlyCheckBox","geneProductOnlyCheckBox","provenanceOnlyCheckBox"})
    public void handleToggle(ClickEvent clickEvent){
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                reload();
            }
        });
    }

    @UiHandler(value = {"typeList", "userField",  "uniqueNameCheckBox"})
    public void searchType(ChangeEvent changeEvent) {
        reload();
    }

    @UiHandler("nameSearchBox")
    public void searchName(KeyUpEvent keyUpEvent) {
        reload();
    }


    @UiHandler("showCurrentView")
    public void setShowCurrentView(ClickEvent clickEvent) {
        nameSearchBox.setText("");
        sequenceList.setText("");
        userField.setSelectedIndex(0);
        typeList.setSelectedIndex(0);
        uniqueNameCheckBox.setValue(false);
        goOnlyCheckBox.setActive(false);
        geneProductOnlyCheckBox.setActive(false);
        provenanceOnlyCheckBox.setActive(false);
        queryViewInRangeOnly = true;
        reload();
    }

    @UiHandler("showAllSequences")
    public void setShowAllSequences(ClickEvent clickEvent) {
        nameSearchBox.setText("");
        sequenceList.setText("");
        userField.setSelectedIndex(0);
        typeList.setSelectedIndex(0);
        uniqueNameCheckBox.setValue(false);
        goOnlyCheckBox.setActive(false);
        geneProductOnlyCheckBox.setActive(false);
        provenanceOnlyCheckBox.setActive(false);
        reload();
    }


    private void handleDetails() {
        tabPanel.setVisible(singleSelectionModel.getSelectedObject() != null);
    }

    private static AnnotationInfo getChildAnnotation(AnnotationInfo annotationInfo, String uniqueName) {
        for (AnnotationInfo childAnnotation : annotationInfo.getChildAnnotations()) {
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
        selectedChildUniqueName = selectedAnnotationInfo.getUniqueName();
    }

    public void setSelectedAnnotationInfo(AnnotationInfo annotationInfo) {
        selectedAnnotationInfo = annotationInfo;
        updateAnnotationInfo(selectedAnnotationInfo);
    }

    // used by javascript function
    public void displayTranscript(int geneIndex, String uniqueName) {

        // for some reason doesn't like call enableGoto
//        enableGoto(geneIndex, uniqueName);

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
        if (type.equals("transposable_element") || type.equals("repeat_region") || type.equals("terminator") || type.equals("Shine_Dalgarno_sequence") ) {            // do nothing
            // do nothing
        } else {
            exonDetailPanel.updateData(annotationInfo);
        }
//        gotoAnnotation.setEnabled(true);
//        deleteAnnotation.setEnabled(true);
        Integer min = selectedAnnotationInfo.getMin() - 50;
        Integer max = selectedAnnotationInfo.getMax() + 50;
        min = min < 0 ? 0 : min;
        MainPanel.updateGenomicViewerForLocation(selectedAnnotationInfo.getSequence(), min, max, false, false);
    }

    public static native void exportStaticMethod(AnnotatorPanel annotatorPanel) /*-{
        $wnd.displayTranscript = $entry(annotatorPanel.@org.bbop.apollo.gwt.client.AnnotatorPanel::displayTranscript(ILjava/lang/String;));
        $wnd.displayFeature = $entry(annotatorPanel.@org.bbop.apollo.gwt.client.AnnotatorPanel::displayFeature(I));
        $wnd.enableGoto = $entry(annotatorPanel.@org.bbop.apollo.gwt.client.AnnotatorPanel::enableGoto(ILjava/lang/String;));
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
                Set<AnnotationInfo> annotationInfoSet = rowValue.getChildAnnotations();
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
                Date date = new Date(Long.parseLong(rowValue.getDateLastModified()));
                div.text(outputFormat.format(date));
                td.endDiv();
            } else {
                Date date = new Date(Long.parseLong(rowValue.getDateLastModified()));
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

    public void setSelectedChildUniqueName(String selectedChildUniqueName) {
        this.selectedChildUniqueName = selectedChildUniqueName;
    }

    public void setSelectedGene(String parentName) {
        List<AnnotationInfo> annotationInfoList = dataGrid.getVisibleItems();
        // 1. let's look locally and see if its already loaded
        for(AnnotationInfo annotationInfo : annotationInfoList){
            if(annotationInfo.getUniqueName().equals(parentName)){
                geneDetailPanel.updateData(annotationInfo);
                return ;
            }
        }
        // 2. not found within the default page, so we'll check the server
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject returnValue = null;
                try {
                    returnValue = JSONParser.parseStrict(response.getText()).isObject();
                    JSONArray jsonArray = returnValue.get(FeatureStringEnum.FEATURES.getValue()).isArray();
                    if(jsonArray.size()==1){
                        AnnotationInfo annotationInfo = AnnotationInfoConverter.convertFromJsonObject(jsonArray.get(0).isObject(),true);
                        geneDetailPanel.updateData(annotationInfo);
                    }
                } catch (Exception e) {
                    Bootbox.alert(e.getMessage());
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert(exception.getMessage());
            }
        };
        AnnotationRestService.findAnnotationByUniqueName(requestCallback,parentName);
    }

    public static long getNextRequestIndex(){
        return requestIndex++ ;
    }
}
