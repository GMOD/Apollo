package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.builder.shared.DivBuilder;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
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
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.event.*;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.SequenceRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.bbop.apollo.gwt.shared.PermissionEnum;
import org.gwtbootstrap3.client.shared.event.TabEvent;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.gwtbootstrap3.client.ui.constants.IconType;

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
//    private TextColumn<AnnotationInfo> filterColumn;
    private TextColumn<AnnotationInfo> typeColumn;
    private Column<AnnotationInfo, Number> lengthColumn;
    long requestIndex = 0 ;

    @UiField
    TextBox nameSearchBox;
    @UiField(provided = true)
    SuggestBox sequenceList;


//    Tree.Resources tablecss = GWT.create(Tree.Resources.class);
    //    @UiField(provided = true)
//    Tree features = new Tree(tablecss);

    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<AnnotationInfo> dataGrid = new DataGrid<>(20, tablecss);
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
    static TabLayoutPanel tabPanel;
    @UiField
    Button cdsButton;
    @UiField
    Button stopCodonButton;
//    @UiField
//    ListBox userField;
//    @UiField
//    ListBox groupField;


    private MultiWordSuggestOracle sequenceOracle = new MultiWordSuggestOracle();

    private static ListDataProvider<AnnotationInfo> dataProvider = new ListDataProvider<>();
    private static List<AnnotationInfo> annotationInfoList = new ArrayList<>();
    private static List<AnnotationInfo> filteredAnnotationList = dataProvider.getList();
    //    private List<AnnotationInfo> filteredAnnotationList = dataProvider.getList();
    private final Set<String> showingTranscripts = new HashSet<String>();
    private SingleSelectionModel<AnnotationInfo> selectionModel = new SingleSelectionModel<>();
    private static Boolean transcriptSelected ;


    public AnnotatorPanel() {
        pager = new SimplePager(SimplePager.TextLocation.CENTER);
        sequenceList = new SuggestBox(sequenceOracle);
        sequenceList.getElement().setAttribute("placeHolder", "All Reference Sequences");
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
                if (transcriptSelected) {
                    transcriptSelected = false;
                    return;
                }
                AnnotationInfo annotationInfo = selectionModel.getSelectedObject();
                GWT.log(selectionModel.getSelectedObject().getName());
                updateAnnotationInfo(annotationInfo);
            }
        });


        exportStaticMethod(this);


        initWidget(ourUiBinder.createAndBindUi(this));



        initializeTypes();
        initializeUsers();
        initializeGroups();


        tabPanel.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> event) {
                exonDetailPanel.redrawExonTable();
            }
        });

        Annotator.eventBus.addHandler(ContextSwitchEvent.TYPE, new ContextSwitchEventHandler() {
            @Override
            public void onContextSwitched(ContextSwitchEvent contextSwitchEvent) {
                if(contextSwitchEvent.getSequenceInfo()!=null){
                    selectedSequenceName = contextSwitchEvent.getSequenceInfo().getName();
                    sequenceList.setText(selectedSequenceName);
                }
                loadSequences();
                annotationInfoList.clear();
                filterList();
//                sequenceList.setText(contextSwitchEvent.getSequenceInfo().getName());
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
                        switch(authenticationEvent.getAction()){
                            case PERMISSION_CHANGED:
                                PermissionEnum hiPermissionEnum = authenticationEvent.getHighestPermission();
                                if(MainPanel.isCurrentUserAdmin()){
                                    hiPermissionEnum = PermissionEnum.ADMINISTRATE;
                                }
                                boolean editable = false;
                                switch(hiPermissionEnum){
                                    case ADMINISTRATE:
                                    case WRITE:
                                        editable = true ;
                                        break;
                                    // default is false
                                }
                                transcriptDetailPanel.setEditable(editable);
                                geneDetailPanel.setEditable(editable);
                                exonDetailPanel.setEditable(editable);
                                reload();
                                break;
                        }
                    }
                }
        );
    }

    private void initializeGroups() {
//        groupField.addItem("All Groups");
    }

    private void initializeUsers() {
//        userField.addItem("All Users");
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
        switch (type) {
            case "gene":
            case "pseduogene":
                geneDetailPanel.updateData(annotationInfo);
//                exonDetailPanel.setVisible(false);
                tabPanel.getTabWidget(1).getParent().setVisible(false);
                tabPanel.selectTab(0);
                break;
            case "mRNA":
            case "tRNA":
                transcriptDetailPanel.updateData(annotationInfo);
                tabPanel.getTabWidget(1).getParent().setVisible(true);
                exonDetailPanel.updateData(annotationInfo);
//                exonDetailPanel.setVisible(true);
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
    }

    @UiHandler("stopCodonButton")
    // switch betwen states
    public void handleStopCodonStuff(ClickEvent clickEvent){
        if(stopCodonButton.getIcon().equals(IconType.BAN)){
            stopCodonButton.setIcon(IconType.WARNING);
            stopCodonButton.setType(ButtonType.WARNING);
        }
        else
        if(stopCodonButton.getIcon().equals(IconType.WARNING)){
            stopCodonButton.setIcon(IconType.FILTER);
            stopCodonButton.setType(ButtonType.PRIMARY);
        }
        else{
            stopCodonButton.setIcon(IconType.BAN);
            stopCodonButton.setType(ButtonType.DEFAULT);
        }
        filterList();
    }

    @UiHandler("cdsButton")
    // switch betwen states
    public void handleCdsStuff(ClickEvent clickEvent){
        if(cdsButton.getIcon().equals(IconType.BAN)){
            cdsButton.setIcon(IconType.WARNING);
            cdsButton.setType(ButtonType.WARNING);
        }
        else
        if(cdsButton.getIcon().equals(IconType.WARNING)){
            cdsButton.setIcon(IconType.FILTER);
            cdsButton.setType(ButtonType.PRIMARY);
        }
        else{
            cdsButton.setIcon(IconType.BAN);
            cdsButton.setType(ButtonType.DEFAULT);
        }
        filterList();
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


//        dataGrid.addColumn(nameColumn, SafeHtmlUtils.fromSafeConstant("<br/>"));
        dataGrid.addColumn(nameColumn, "Name");
        dataGrid.addColumn(typeColumn, "Type");
        dataGrid.addColumn(lengthColumn, "Length");
//        dataGrid.addColumn(filterColumn, "Warnings");

        dataGrid.setColumnWidth(0, "70%");
        dataGrid.setColumnWidth(1, "15%");
        dataGrid.setColumnWidth(2, "15%");


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

        String url = rootUrl + "/annotator/findAnnotationsForSequence/?sequenceName=" + selectedSequenceName+"&request="+requestIndex;
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                long localRequestValue = (long) returnValue.isObject().get(FeatureStringEnum.REQUEST_INDEX.getValue()).isNumber().doubleValue();
                // returns
                if(localRequestValue<=requestIndex){
                    return;
                }
                else{
                    requestIndex = localRequestValue ;
                }

                JSONArray array = returnValue.isObject().get("features").isArray();
                annotationInfoList.clear();

                for (int i = 0; i < array.size(); i++) {
                    JSONObject object = array.get(i).isObject();
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
        annotationInfo.setSequence(object.get("sequence").isString().stringValue());
        if(object.get("owner")!=null){
            annotationInfo.setOwner(object.get("owner").isString().stringValue());
        }

        List<String> noteList = new ArrayList<>();
        if(object.get("notes")!=null){
            JSONArray jsonArray = object.get("notes").isArray();
            for(int i = 0 ; i< jsonArray.size() ; i++){
                String note = jsonArray.get(i).isString().stringValue();
                noteList.add(note) ;
            }
        }
        annotationInfo.setNoteList(noteList);

        if (processChildren && object.get("children") != null) {
            JSONArray jsonArray = object.get("children").isArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                AnnotationInfo childAnnotation = generateAnnotationInfo(jsonArray.get(i).isObject(), true);
                annotationInfo.addChildAnnotation(childAnnotation);
            }
        }

        return annotationInfo;
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


    // TODO: need to cache these or retrieve from the backend
    public static void displayTranscript(int geneIndex, String uniqueName) {
        transcriptSelected = true ;

        // 1 - get the correct gene
        AnnotationInfo annotationInfo = filteredAnnotationList.get(geneIndex);
        AnnotationInfoChangeEvent annotationInfoChangeEvent = new AnnotationInfoChangeEvent(annotationInfo, AnnotationInfoChangeEvent.Action.SET_FOCUS);

        for (AnnotationInfo childAnnotation : annotationInfo.getAnnotationInfoSet()) {
            if (childAnnotation.getUniqueName().equalsIgnoreCase(uniqueName)) {
                exonDetailPanel.updateData(childAnnotation);
                updateAnnotationInfo(childAnnotation);
                Annotator.eventBus.fireEvent(annotationInfoChangeEvent);
                return;
            }
        }
    }

    public static native void exportStaticMethod(AnnotatorPanel annotatorPanel) /*-{
        $wnd.displayTranscript = $entry(@org.bbop.apollo.gwt.client.AnnotatorPanel::displayTranscript(ILjava/lang/String;));
    }-*/;

    private class CustomTableBuilder extends AbstractCellTableBuilder<AnnotationInfo> {

        // TODO: delete this .. just for demo version
        Random random = new Random();

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

            td = row.startTD();
            td.style().outlineStyle(Style.OutlineStyle.NONE).endStyle();

            // TODO: is it necessary to have two separte ones?
//            if(showTranscripts){
                DivBuilder div = td.startDiv();
                SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder();

                for(String error : rowValue.getNoteList()){
                    safeHtmlBuilder.appendHtmlConstant("<div class='label label-warning'>"+error+"</div>");
                }

//                if(random.nextBoolean()){
//                    safeHtmlBuilder.appendHtmlConstant("<div class='label label-warning'>CDS-3</div>");
//                }
//                else
//                if(random.nextBoolean()){
//                    safeHtmlBuilder.appendHtmlConstant("<div class='label label-warning'>Stop Codon</div>");
//                }
//                else{
////                    safeHtmlBuilder.appendHtmlConstant("<pre>abcd</pre>");
//                }

                div.html(safeHtmlBuilder.toSafeHtml());
                td.endDiv();
                td.endTD();
//            }
//            else{
//                DivBuilder div = td.startDiv();
//                SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder();
//
//                if(random.nextBoolean()){
//                    safeHtmlBuilder.appendHtmlConstant("<div class='label label-warning'>CDS-3</div>");
//                }
//                else
//                if(random.nextBoolean()){
//                    safeHtmlBuilder.appendHtmlConstant("<div class='label label-warning'>Stop Codon</div>");
//                }
//                else{
////                    safeHtmlBuilder.appendHtmlConstant("<pre>abcd</pre>");
//                }
//
//                div.html(safeHtmlBuilder.toSafeHtml());
//                td.endDiv();
//                td.endTD();
//            }
//            row.endTD();

            row.endTR();

        }


    }
}