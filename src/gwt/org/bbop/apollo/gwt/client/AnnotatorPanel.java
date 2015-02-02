package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.builder.shared.DivBuilder;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.event.AnnotationInfoChangeEvent;
import org.bbop.apollo.gwt.client.event.AnnotationInfoChangeEventHandler;
import org.bbop.apollo.gwt.client.event.ContextSwitchEvent;
import org.bbop.apollo.gwt.client.event.ContextSwitchEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.SequenceRestService;
import org.gwtbootstrap3.client.shared.event.TabEvent;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;

import java.util.*;

/**
 * Created by ndunn on 12/17/14.
 */
public class AnnotatorPanel extends Composite {
    interface AnnotatorPanelUiBinder extends UiBinder<com.google.gwt.user.client.ui.Widget, AnnotatorPanel> {
    }

    private static AnnotatorPanelUiBinder ourUiBinder = GWT.create(AnnotatorPanelUiBinder.class);

    Dictionary dictionary = Dictionary.getDictionary("Options");
    String rootUrl = dictionary.get("rootUrl");
    private String selectedSequenceName = null;

    private Column<AnnotationInfo, String> nameColumn;
    private TextColumn<AnnotationInfo> typeColumn;
    private Column<AnnotationInfo, Number> lengthColumn;

    @UiField
    TextBox nameSearchBox;
    @UiField(provided = true)
    SuggestBox sequenceList;
    @UiField
    CheckBox cdsFilter;
    @UiField
    CheckBox stopCodonFilter;


//    Tree.Resources tablecss = GWT.create(Tree.Resources.class);
    //    @UiField(provided = true)
//    Tree features = new Tree(tablecss);

    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<AnnotationInfo> dataGrid = new DataGrid<>(10, tablecss);
    @UiField(provided = true)
    SimplePager pager = null;


    @UiField
    ListBox typeList;
    @UiField
    static GeneDetailPanel geneDetailPanel;
    @UiField
    static TranscriptDetailPanel transcriptDetailPanel;
    @UiField
    static ExonDetailPanel exonDetailPanel;
    @UiField
    TabLayoutPanel tabPanel;
//    @UiField
//    static CDSDetailPanel cdsDetailPanel;

    private MultiWordSuggestOracle sequenceOracle = new MultiWordSuggestOracle();

    private static ListDataProvider<AnnotationInfo> dataProvider = new ListDataProvider<>();
    private static List<AnnotationInfo> annotationInfoList = new ArrayList<>();
    private static List<AnnotationInfo> filteredAnnotationList = dataProvider.getList();
    //    private List<AnnotationInfo> filteredAnnotationList = dataProvider.getList();
    private final Set<String> showingTranscripts = new HashSet<String>();
    private SingleSelectionModel<AnnotationInfo> selectionModel = new SingleSelectionModel<>();


    public AnnotatorPanel() {
        pager = new SimplePager(SimplePager.TextLocation.CENTER);
        sequenceList = new SuggestBox(sequenceOracle);
        dataGrid.setWidth("100%");

//        dataGrid.setEmptyTableWidget(new Label("Loading"));
        initializeTable();

        dataGrid.setTableBuilder(new CustomTableBuilder());


        dataProvider.addDataDisplay(dataGrid);
        pager.setDisplay(dataGrid);

        dataGrid.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                AnnotationInfo annotationInfo = selectionModel.getSelectedObject();
                GWT.log(selectionModel.getSelectedObject().getName());
                updateAnnotationInfo(annotationInfo);
            }
        });
        exportStaticMethod(this);


        Widget rootElement = ourUiBinder.createAndBindUi(this);

        initWidget(rootElement);

        initializeTypes();
        stopCodonFilter.setValue(false);

        Annotator.eventBus.addHandler(ContextSwitchEvent.TYPE, new ContextSwitchEventHandler() {
            @Override
            public void onContextSwitched(ContextSwitchEvent contextSwitchEvent) {
                selectedSequenceName = contextSwitchEvent.getSequenceInfo().getName();
                loadSequences();
//                sequenceList.setText(contextSwitchEvent.getSequenceInfo().getName());
            }
        });

        Annotator.eventBus.addHandler(AnnotationInfoChangeEvent.TYPE, new AnnotationInfoChangeEventHandler() {
            @Override
            public void onAnnotationChanged(AnnotationInfoChangeEvent annotationInfoChangeEvent) {
                reload();
            }
        });

//        features.setAnimationEnabled(true);

//        features.addSelectionHandler(new SelectionHandler<TreeItem>() {
//            @Override
//            public void onSelection(SelectionEvent<TreeItem> event) {
//                JSONObject internalData = ((AnnotationContainerWidget) event.getSelectedItem().getWidget()).getInternalData();
//                String type = getType(internalData);
//                geneDetailPanel.setVisible(false);
//                transcriptDetailPanel.setVisible(false);
//                exonDetailPanel.setVisible(false);
//                switch (type) {
//                    case "gene":
//                    case "pseduogene":
//                        geneDetailPanel.updateDetailData(internalData);
//                        break;
//                    case "mRNA":
//                    case "tRNA":
//                        transcriptDetailPanel.updateDetailData(internalData);
//                        break;
//                    case "exon":
//                        exonDetailPanel.updateDetailData(internalData);
//                        break;
//                    case "CDS":
//                        exonDetailPanel.updateDetailData(internalData);
//                        break;
//                    default:
//                        GWT.log("not sure what to do with " + type);
//                }
////                annotationName.setText(internalData.get("name").isString().stringValue());
//            }
//        });
    }

    private void initializeTypes() {
        typeList.addItem("All Types", "");
        typeList.addItem("Gene");
        typeList.addItem("Pseudogene");
        typeList.addItem("mRNA");
        typeList.addItem("ncRNA");
        typeList.addItem("tRNA");
        // TODO: add rest
    }

    private static void updateAnnotationInfo(AnnotationInfo annotationInfo) {
        String type = annotationInfo.getType();
        GWT.log("annoation type: " + type);
        geneDetailPanel.setVisible(false);
        transcriptDetailPanel.setVisible(false);
        exonDetailPanel.setVisible(false);
//        cdsDetailPanel.setVisible(false);
        switch (type) {
            case "gene":
            case "pseduogene":
                geneDetailPanel.updateData(annotationInfo);
                break;
            case "mRNA":
            case "tRNA":
                transcriptDetailPanel.updateData(annotationInfo);
                break;
//            case "exon":
//                exonDetailPanel.updateData(annotationInfo);
//                break;
//            case "CDS":
//                cdsDetailPanel.updateDetailData(AnnotationRestService.convertAnnotationInfoToJSONObject(annotationInfo));
//                break;
            default:
                GWT.log("not sure what to do with " + type);
        }
//                annotationName.setText(internalData.get("name").isString().stringValue());


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

        typeColumn = new TextColumn<AnnotationInfo>() {
            @Override
            public String getValue(AnnotationInfo annotationInfo) {
                return annotationInfo.getType();
            }
        };
        typeColumn.setSortable(true);

        lengthColumn = new Column<AnnotationInfo, Number>(new NumberCell()) {
            @Override
            public Integer getValue(AnnotationInfo annotationInfo) {
                return annotationInfo.getLength();
            }
        };
        lengthColumn.setSortable(true);


//        dataGrid.addColumn(nameColumn, SafeHtmlUtils.fromSafeConstant("<br/>"));
        dataGrid.addColumn(nameColumn, "Name");
        dataGrid.addColumn(typeColumn, "Type");
        dataGrid.addColumn(lengthColumn, "Length");

        dataGrid.setColumnWidth(0, "70%");


        ColumnSortEvent.ListHandler<AnnotationInfo> sortHandler = new ColumnSortEvent.ListHandler<AnnotationInfo>(filteredAnnotationList);
        dataGrid.addColumnSortHandler(sortHandler);

        // Specify a custom table.
//        dataGrid.setTableBuilder(new AnnotationInfoTableBuilder(dataGrid,sortHandler,showingTranscripts));

        sortHandler.setComparator(nameColumn, new Comparator<AnnotationInfo>() {
            @Override
            public int compare(AnnotationInfo o1, AnnotationInfo o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });


        sortHandler.setComparator(typeColumn, new Comparator<AnnotationInfo>() {
            @Override
            public int compare(AnnotationInfo o1, AnnotationInfo o2) {
                return o1.getType().compareTo(o2.getType());
            }
        });

        sortHandler.setComparator(lengthColumn, new Comparator<AnnotationInfo>() {
            @Override
            public int compare(AnnotationInfo o1, AnnotationInfo o2) {
                return o1.getLength() - o2.getLength();
            }
        });


    }

    private void loadSequences() {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();

                if (selectedSequenceName == null && array.size() > 0) {
                    selectedSequenceName = array.get(0).isObject().get("name").isString().stringValue();
                }
                sequenceOracle.clear();
                for (int i = 0; i < array.size(); i++) {
                    JSONObject object = array.get(i).isObject();
                    SequenceInfo sequenceInfo = new SequenceInfo();
                    sequenceInfo.setName(object.get("name").isString().stringValue());
                    sequenceInfo.setLength((int) object.get("length").isNumber().isNumber().doubleValue());
                    sequenceOracle.add(sequenceInfo.getName());
//                    sequenceList.addItem(sequenceInfo.getName());
                    if (selectedSequenceName.equals(sequenceInfo.getName())) {
                        sequenceList.setText(sequenceInfo.getName());
//                        sequenceList.setSelectedIndex(i);
                    }
                }

//                reload();
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error loading organisms");
            }
        };
        SequenceRestService.loadSequences(requestCallback, MainPanel.currentOrganismId);
    }

    private String getType(JSONObject internalData) {
        return internalData.get("type").isObject().get("name").isString().stringValue();
    }

    public void reload() {
        if (selectedSequenceName == null) {
            selectedSequenceName = MainPanel.currentSequenceId;
            loadSequences();
        }
//        features.setAnimationEnabled(false);

        String url = rootUrl + "/annotator/findAnnotationsForSequence/?sequenceName=" + selectedSequenceName;
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isObject().get("features").isArray();
//                features.clear();
                annotationInfoList.clear();

                for (int i = 0; i < array.size(); i++) {
                    JSONObject object = array.get(i).isObject();
//                    TreeItem treeItem = processFeatureEntry(object);
//                    features.addItem(treeItem);
                    GWT.log(object.toString());


                    AnnotationInfo annotationInfo = generateAnnotationInfo(object);
                    annotationInfoList.add(annotationInfo);
                }

//                features.setAnimationEnabled(true);
                GWT.log("# of annoations: " + filteredAnnotationList.size());

                filterList();
                dataGrid.redraw();
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error loading organisms");
            }
        };
        try {
            builder.setCallback(requestCallback);
            builder.send();
        } catch (RequestException e) {
            // Couldn't connect to server
            Window.alert(e.getMessage());
        }
    }

    private void filterList() {
        filteredAnnotationList.clear();
        for(int i = 0 ; i < annotationInfoList.size() ; i++){
            AnnotationInfo annotationInfo = annotationInfoList.get(i);
            if(searchMatches(annotationInfo)){
                filteredAnnotationList.add(annotationInfo);
            }
            else{
                if(searchMatches(annotationInfo.getAnnotationInfoSet())){
                    filteredAnnotationList.add(annotationInfo);
                }
            }
        }
    }

    private boolean searchMatches(Set<AnnotationInfo> annotationInfoSet) {
        for(AnnotationInfo annotationInfo : annotationInfoSet){
            if(searchMatches(annotationInfo)){
                return true ;
            }
        }
        return false;
    }

    private boolean searchMatches(AnnotationInfo annotationInfo) {
        String nameText = nameSearchBox.getText() ;
        String typeText = typeList.getSelectedValue();
        return (
                (annotationInfo.getName().toLowerCase().contains(nameText.toLowerCase()))
                        &&
                        annotationInfo.getType().toLowerCase().contains(typeText.toLowerCase())
                );

    }

    private AnnotationInfo generateAnnotationInfo(JSONObject object) {
        return generateAnnotationInfo(object, true);
    }

    private AnnotationInfo generateAnnotationInfo(JSONObject object, boolean processChildren) {
        AnnotationInfo annotationInfo = new AnnotationInfo();
        annotationInfo.setName(object.get("name").isString().stringValue());
        GWT.log("top-level processing: " + annotationInfo.getName());
        annotationInfo.setType(object.get("type").isObject().get("name").isString().stringValue());
        if(object.get("symbol")!=null){
            annotationInfo.setSymbol(object.get("symbol").isString().stringValue());
        }
        if(object.get("description")!=null){
            annotationInfo.setDescription(object.get("description").isString().stringValue());
        }
        annotationInfo.setMin((int) object.get("location").isObject().get("fmin").isNumber().doubleValue());
        annotationInfo.setMax((int) object.get("location").isObject().get("fmax").isNumber().doubleValue());
        annotationInfo.setStrand((int) object.get("location").isObject().get("strand").isNumber().doubleValue());
        annotationInfo.setUniqueName(object.get("uniquename").isString().stringValue());

        if (processChildren && object.get("children") != null) {
            JSONArray jsonArray = object.get("children").isArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                AnnotationInfo childAnnotation = generateAnnotationInfo(jsonArray.get(i).isObject(), true);
                annotationInfo.addChildAnnotation(childAnnotation);
            }
        }

        return annotationInfo;
    }

    @UiHandler("tabPanel")
    public void handleTabChange(SelectionEvent<Integer> event){
        Window.alert("tab changed: "+tabPanel.getSelectedIndex());
        switch (tabPanel.getSelectedIndex()){
            case 1: exonDetailPanel.updateData(selectionModel.getSelectedObject());
                break;
            default:
                break ;
        }
    }

    @UiHandler("typeList")
    public void searchType(ChangeEvent changeEvent){
        filterList();
    }

    @UiHandler("nameSearchBox")
    public void searchName(KeyUpEvent keyUpEvent){
        filterList();
    }


    @UiHandler("sequenceList")
    public void changeRefSequence(KeyUpEvent changeEvent) {
        selectedSequenceName = sequenceList.getText();
        reload();
    }

//    private TreeItem processFeatureEntry(JSONObject object) {
//        TreeItem treeItem = new TreeItem();
//
//        String featureName = object.get("name").isString().stringValue();
//        String featureType = object.get("type").isObject().get("name").isString().stringValue();
//        int lastFeature = featureType.lastIndexOf(".");
//        featureType = featureType.substring(lastFeature + 1);
////        HTML html = new HTML(featureName + " <div class='label label-success'>" + featureType + "</div>");
//        treeItem.setWidget(new AnnotationContainerWidget(object));
//
//        if (object.get("children") != null) {
//            JSONArray childArray = object.get("children").isArray();
//            for (int i = 0; childArray != null && i < childArray.size(); i++) {
//                JSONObject childObject = childArray.get(i).isObject();
//                treeItem.addItem(processFeatureEntry(childObject));
//            }
//        }
//
//        return treeItem;
//    }

    // TODO: need to cache these or retrieve from the backend
    public static void displayTranscript(int geneIndex, String uniqueName) {
        AnnotationInfo annotationInfo = filteredAnnotationList.get(geneIndex);

//        Iterator<AnnotationInfo> annotationInfoIterator = filteredAnnotationList.iterator();
//        for(int i = 0 ; i<filteredAnnotationList.size() && annotationInfo==null  ; i++){
//            if(i==geneIndex){
//                annotationInfo = annotationInfoIterator.next();
//            }
//            else{
//                annotationInfoIterator.next();
//            }
//        }

        for (AnnotationInfo childAnnotation : filteredAnnotationList.get(geneIndex).getAnnotationInfoSet()) {
            if (childAnnotation.getUniqueName().equalsIgnoreCase(uniqueName)) {
                updateAnnotationInfo(childAnnotation);
                return;
            }
        }
    }

    public static native void exportStaticMethod(AnnotatorPanel annotatorPanel) /*-{
        $wnd.displayTranscript = $entry(@org.bbop.apollo.gwt.client.AnnotatorPanel::displayTranscript(ILjava/lang/String;));
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
//            final SingleSelectionModel<AnnotationInfo> selectionModel = (SingleSelectionModel<AnnotationInfo>) dataGrid.getSelectionModel();

            TableRowBuilder row = startRow();
            TableCellBuilder td = row.startTD();

            td.style().outlineStyle(Style.OutlineStyle.NONE).endStyle();
            if (showTranscripts) {
                // TODO: this is ugly, but it works
                // a custom cell rendering might work as well, but not sure

                String transcriptStyle = "margin-left: 10px; color: green; padding-left: 5px; padding-right: 5px; border-radius: 15px; background-color: #EEEEEE;";
                HTML html = new HTML("<a style='" + transcriptStyle + "' onclick=\"displayTranscript(" + absRowIndex + ",'" + rowValue.getUniqueName() + "');\">" + rowValue.getName() + "</a>");
                SafeHtml htmlString = new SafeHtmlBuilder().appendHtmlConstant(html.getHTML()).toSafeHtml();
//                updateAnnotationInfo(rowValue);
                td.html(htmlString);
            } else {
                renderCell(td, createContext(0), nameColumn, rowValue);
            }
            td.endTD();

            // Type column.
            td = row.startTD();
//            td.className(cellStyles);
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

            row.endTR();

        }


    }
}