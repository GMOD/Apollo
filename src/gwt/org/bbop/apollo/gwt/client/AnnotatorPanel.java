package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.cell.client.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.builder.shared.DivBuilder;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
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
import org.bbop.apollo.gwt.client.dto.*;
import org.bbop.apollo.gwt.client.dto.assemblage.*;
import org.bbop.apollo.gwt.client.event.*;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.AssemblageRestService;
import org.bbop.apollo.gwt.client.rest.UserRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.bbop.apollo.gwt.shared.PermissionEnum;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Label;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.*;

/**
 * Created by Nathan Dunn on 12/17/14.
 */
public class AnnotatorPanel extends Composite {

    interface AnnotatorPanelUiBinder extends UiBinder<com.google.gwt.user.client.ui.Widget, AnnotatorPanel> {
    }

    private static AnnotatorPanelUiBinder ourUiBinder = GWT.create(AnnotatorPanelUiBinder.class);
    // Tue Jan 05 09:51:38 GMT-800 2016
    DateTimeFormat outputFormat = DateTimeFormat.getFormat("MMM dd, yyyy");
    private Column<AnnotationInfo, String> nameColumn;
    private TextColumn<AnnotationInfo> typeColumn;
    private TextColumn<AnnotationInfo> sequenceColumn;
    private Column<AnnotationInfo, Number> lengthColumn;
    private Column<AnnotationInfo, String> dateColumn;
    long requestIndex = 0;

    @UiField
    static
    TextBox nameSearchBox;
    @UiField(provided = true)
    static
    org.gwtbootstrap3.client.ui.SuggestBox sequenceList;

    static DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);

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
    static TabLayoutPanel tabPanel;
    @UiField
    ListBox userField;
    @UiField
    DockLayoutPanel splitPanel;
    @UiField
    Container northPanelContainer;
    @UiField
    static Button addNewAssemblage;
    @UiField
    static Button addToView;
    @UiField
    static Button viewAnnotation;
    @UiField
    Button showAllSequences;

    private MultiWordSuggestOracle sequenceOracle = new ReferenceSequenceOracle();

    private static AsyncDataProvider<AnnotationInfo> dataProvider;
    private static AnnotationInfo currentAnnotationInfo = null;
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
                            return ;
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
                            dataGrid.setRowData(start, AnnotationInfoConverter.convertFromJsonArray(jsonArray));
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
                exonDetailPanel.redrawExonTable();
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
        Annotator.eventBus.addHandler(OrganismChangeEvent.TYPE, new OrganismChangeEventHandler() {
            @Override
            public void onOrganismChanged(OrganismChangeEvent organismChangeEvent) {
                if (organismChangeEvent.getAction() == OrganismChangeEvent.Action.LOADED_ORGANISMS) {
                    sequenceList.setText(organismChangeEvent.getCurrentSequence());
                    reload();
                }
            }
        });

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                userField.setVisible(true);
            }
        });

    }

    @UiHandler("addToView")
    void addToView(ClickEvent clickEvent) {
        AssemblageInfo assemblageInfo = collectAssemblageFromSelectedFeature(currentAnnotationInfo);
        AssemblageInfo currentAssemblage = MainPanel.getInstance().getCurrentAssemblage();
        currentAssemblage = currentAssemblage.addAssemblageToEnd(assemblageInfo);
        AssemblageRestService.addAssemblageAndView(currentAssemblage);
    }

    @UiHandler("viewAnnotation")
    void viewAnnotation(ClickEvent clickEvent) {
        AssemblageInfo assemblageInfo = collectAssemblageFromSelectedFeature(currentAnnotationInfo);
        expandAssemblage(assemblageInfo,2d);
        AssemblageRestService.addAssemblageAndView(assemblageInfo);
    }

    static AssemblageInfo collectAssemblageFromSelectedFeature(AnnotationInfo annotationInfo){

        AssemblageInfo assemblageInfo = new AssemblageInfo();
        AssemblageSequenceList sequenceArray = new AssemblageSequenceList();

        assemblageInfo.setPadding(50);
        assemblageInfo.setType("Exon");

        SequenceFeatureInfo sequenceObject = new SequenceFeatureInfo();
//        sequenceObject.setReverseComplement(false);
        sequenceObject.setName(annotationInfo.getSequence());
        sequenceObject.setStart(annotationInfo.getMin());
        sequenceObject.setEnd(annotationInfo.getMax());

        SequenceFeatureInfo featuresObject = new SequenceFeatureInfo() ;
        featuresObject.setName(annotationInfo.getName());

        sequenceObject.setFeature(featuresObject);
        sequenceArray.set(sequenceArray.size(), sequenceObject);

        assemblageInfo.setSequenceList(sequenceArray);
        assemblageInfo.setStart(annotationInfo.getMin());
        assemblageInfo.setEnd(annotationInfo.getMax());

        return assemblageInfo;
    }

    void expandAssemblage(AssemblageInfo assemblageInfo, Double expansionFactor){
        AssemblageSequenceList assemblageSequenceList = assemblageInfo.getSequenceList();
        AssemblageSequence assemblageSequence = assemblageSequenceList.getSequence(0);
        Long start = assemblageSequence.getStart();
        Long end = assemblageSequence.getEnd();
//        Long width = end - start ;
        // we must now double the size
//        Double desiredWidth = width * expansionFactor ;
//        Long desiredStart = start - (long) (desiredWidth / 2.0) ;
//        Long desiredEnd = end + (long) (desiredWidth / 2.0) ;
        Long buffer = 200L ;
        Long desiredStart = start - (long) (buffer ) ;
        Long desiredEnd = end + (long) (buffer) ;
        start = desiredStart < 0 ? 0 : desiredStart ;
        end = desiredEnd ;  // can we maximize this?
        assemblageSequence.setStart(start);
        assemblageSequence.setEnd(end);
        assemblageSequenceList.set(0, assemblageSequence);
        assemblageInfo.setSequenceList(assemblageSequenceList);
    }

    @UiHandler("addNewAssemblage")
    void addNewAssemblage(ClickEvent clickEvent) {
        AssemblageInfo assemblageInfo = collectAssemblageFromSelectedFeature(currentAnnotationInfo);
        expandAssemblage(assemblageInfo,2d);


        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                new InfoDialog("Added Assemblage", "Added assemblage for " + currentAnnotationInfo.getName(), true);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error adding assemblage: "+exception);
            }
        };

        MainPanel.getInstance().addAssemblage(requestCallback, assemblageInfo);
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
    }

    private static void updateAnnotationInfo(AnnotationInfo annotationInfo) {
        currentAnnotationInfo = annotationInfo;
        addNewAssemblage.setEnabled(currentAnnotationInfo != null);
        viewAnnotation.setEnabled(currentAnnotationInfo != null);
        addToView.setEnabled(currentAnnotationInfo != null);
        if (currentAnnotationInfo == null) {
            return;
        }

        String type = annotationInfo.getType();
        GWT.log("annotation type: " + type);
        geneDetailPanel.setVisible(false);
        transcriptDetailPanel.setVisible(false);
        repeatRegionDetailPanel.setVisible(false);
        switch (type) {
            case "gene":
            case "pseudogene":
                geneDetailPanel.updateData(annotationInfo);
                tabPanel.getTabWidget(1).getParent().setVisible(false);
                tabPanel.selectTab(0);
                break;
            case "Transcript":
                transcriptDetailPanel.updateData(annotationInfo);
                tabPanel.getTabWidget(1).getParent().setVisible(true);
                exonDetailPanel.updateData(annotationInfo);
                break;
            case "mRNA":
            case "miRNA":
            case "tRNA":
            case "rRNA":
            case "snRNA":
            case "snoRNA":
            case "ncRNA":
                transcriptDetailPanel.updateData(annotationInfo);
                tabPanel.getTabWidget(1).getParent().setVisible(true);
                exonDetailPanel.updateData(annotationInfo);
                break;
            case "transposable_element":
            case "repeat_region":
                fireAnnotationInfoChangeEvent(annotationInfo);
                repeatRegionDetailPanel.updateData(annotationInfo);
                tabPanel.getTabWidget(1).getParent().setVisible(false);
                break;
            default:
                GWT.log("not sure what to do with " + type);
        }
    }

    public static void fireAnnotationInfoChangeEvent(AnnotationInfo annotationInfo) {
        // this method is for firing AnnotationInfoChangeEvent for single level features such as transposable_element and repeat_region
        AnnotationInfoChangeEvent annotationInfoChangeEvent = new AnnotationInfoChangeEvent(annotationInfo, AnnotationInfoChangeEvent.Action.SET_FOCUS);
        Annotator.eventBus.fireEvent(annotationInfoChangeEvent);
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

        nameColumn.setFieldUpdater(new FieldUpdater<AnnotationInfo, String>() {
            @Override
            public void update(int index, AnnotationInfo annotationInfo, String value) {
                if (showingTranscripts.contains(annotationInfo.getUniqueName())) {
                    showingTranscripts.remove(annotationInfo.getUniqueName());
                } else {
                    showingTranscripts.add(annotationInfo.getUniqueName());
                }

                // Redraw the modified row.
                dataGrid.redrawRow(index);
            }
        });
        nameColumn.setSortable(true);

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
            public Long getValue(AnnotationInfo annotationInfo) {
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

        dataGrid.addColumn(nameColumn, "Name");
        dataGrid.addColumn(sequenceColumn, "Seq");
        dataGrid.addColumn(typeColumn, "Type");
        dataGrid.addColumn(lengthColumn, "Length");
        dataGrid.addColumn(dateColumn, "Updated");
        dataGrid.setColumnWidth(0, 75, Unit.PCT);
        dataGrid.setColumnWidth(1, 25, Unit.PCT);
        dataGrid.setColumnWidth(2, 45.0, Unit.PX);
        dataGrid.setColumnWidth(3, 65.0, Unit.PX);
        dataGrid.setColumnWidth(4, 100.0, Unit.PX);
    }

    private String getType(JSONObject internalData) {
        return internalData.get("type").isObject().get("name").isString().stringValue();
    }

    public static void reload() {
        updateAnnotationInfo(null);
//        pager.setPageStart(0);
        dataGrid.setVisibleRangeAndClearData(dataGrid.getVisibleRange(), true);
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
    public void setShowAllSequences(ClickEvent clickEvent){
        sequenceList.setText("");
        reload();
    }
    public static void showInAnnotatorPanel(String featureName,String scaffold){
        Window.alert(featureName + " " + scaffold);

        sequenceList.setText(scaffold);
        nameSearchBox.setText(featureName);
        reload();
    }

    // TODO: need to cache these or retrieve from the backend
    public static void displayTranscript(String geneIndex, String uniqueName,String displayString) {
        int geneInt = Integer.parseInt(geneIndex);
        AnnotationInfo annotationInfo = dataGrid.getVisibleItem(Math.abs(dataGrid.getVisibleRange().getStart() - geneInt));

        for (AnnotationInfo childAnnotation : annotationInfo.getAnnotationInfoSet()) {
            if (childAnnotation.getUniqueName().equalsIgnoreCase(uniqueName)) {
                exonDetailPanel.updateData(childAnnotation);
                updateAnnotationInfo(childAnnotation);
                return;
            }
        }
    }

    public static native void exportStaticMethod(AnnotatorPanel annotatorPanel) /*-{
        $wnd.displayTranscript = $entry(@org.bbop.apollo.gwt.client.AnnotatorPanel::displayTranscript(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;));
        $wnd.showInAnnotatorPanel = $entry(@org.bbop.apollo.gwt.client.AnnotatorPanel::showInAnnotatorPanel(Ljava/lang/String;Ljava/lang/String;));
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
                String viewString = "<a href='' onclick=\"displayTranscript(" + absRowIndex + ",'" + rowValue.getUniqueName() + "',true);\"><i class='fa fa-eye'></i></a>";
                String transcriptStyle = "margin-left: 10px; color: green; padding-left: 5px; padding-right: 5px; border-radius: 15px; background-color: #EEEEEE;";
                String linkString = "<a style='" + transcriptStyle + "' onclick=\"displayTranscript(" + absRowIndex + ",'" + rowValue.getUniqueName() + "',false);\">" + rowValue.getName() + "</a>";
                HTML html = new HTML(linkString);
                SafeHtml htmlString = new SafeHtmlBuilder().appendHtmlConstant(html.getHTML()).toSafeHtml();
                td.html(htmlString);
            } else {
                renderCell(td, createContext(0), nameColumn, rowValue);
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
//                div.text(date.toString());
                td.endDiv();
            } else {
                Date date = new Date(Long.parseLong(rowValue.getDate()));
                td.text(outputFormat.format(date));
//                td.text(date.toString());
            }
            td.endTD();

            td = row.startTD();
            td.style().outlineStyle(Style.OutlineStyle.NONE).endStyle();

            DivBuilder div = td.startDiv();
            SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder();

            for (String error : rowValue.getNoteList()) {
                safeHtmlBuilder.appendHtmlConstant("<div class='label label-warning'>" + error + "</div>");
            }


            div.html(safeHtmlBuilder.toSafeHtml());
            td.endDiv();
            td.endTD();

            row.endTR();
        }
    }
}
