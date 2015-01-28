package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.client.Style;
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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.AnnotationRestService;
import org.bbop.apollo.gwt.client.rest.SequenceRestService;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;

import java.util.Comparator;
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
    DataGrid<AnnotationInfo> dataGrid = new DataGrid<>(10,tablecss);
    @UiField(provided = true)
    SimplePager pager = null;


    @UiField
    ListBox typeList;
    @UiField
    GeneDetailPanel geneDetailPanel;
    @UiField
    TranscriptDetailPanel transcriptDetailPanel;
    @UiField
    ExonDetailPanel exonDetailPanel;
    @UiField
    CDSDetailPanel cdsDetailPanel;

    private MultiWordSuggestOracle sequenceOracle = new MultiWordSuggestOracle();

    private ListDataProvider<AnnotationInfo> dataProvider = new ListDataProvider<>();
    private List<AnnotationInfo> filteredAnnotationList = dataProvider.getList();
    //    private List<AnnotationInfo> filteredAnnotationList = dataProvider.getList();
    private SingleSelectionModel<AnnotationInfo> annotationInfoSingleSelectionModel = new SingleSelectionModel<>();
    private final Set<String> showingTranscripts = new HashSet<String>();
    private SingleSelectionModel<AnnotationInfo> selectionModel = new SingleSelectionModel<>();


    public AnnotatorPanel() {
        pager = new SimplePager(SimplePager.TextLocation.CENTER);
        sequenceList = new SuggestBox(sequenceOracle);
        dataGrid.setWidth("100%");

        dataGrid.setEmptyTableWidget(new Label("Loading"));
        initializeTable();

        dataGrid.setTableBuilder(new CustomTableBuilder());


        dataProvider.addDataDisplay(dataGrid);
        pager.setDisplay(dataGrid);

        dataGrid.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                AnnotationInfo annotationInfo = selectionModel.getSelectedObject();
                updateAnnotationInfo(annotationInfo);
            }
        });


        Widget rootElement = ourUiBinder.createAndBindUi(this);

        initWidget(rootElement);

        stopCodonFilter.setValue(false);

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
//                        geneDetailPanel.updateData(internalData);
//                        break;
//                    case "mRNA":
//                    case "tRNA":
//                        transcriptDetailPanel.updateData(internalData);
//                        break;
//                    case "exon":
//                        exonDetailPanel.updateData(internalData);
//                        break;
//                    case "CDS":
//                        exonDetailPanel.updateData(internalData);
//                        break;
//                    default:
//                        GWT.log("not sure what to do with " + type);
//                }
////                annotationName.setText(internalData.get("name").isString().stringValue());
//            }
//        });
    }

    private void updateAnnotationInfo(AnnotationInfo annotationInfo){
        String type = annotationInfo.getType();
        geneDetailPanel.setVisible(false);
        transcriptDetailPanel.setVisible(false);
        exonDetailPanel.setVisible(false);
        cdsDetailPanel.setVisible(false);
        switch (type) {
            case "gene":
            case "pseduogene":
                geneDetailPanel.updateData(annotationInfo);
                break;
            case "mRNA":
            case "tRNA":
                transcriptDetailPanel.updateData(AnnotationRestService.convertAnnotationInfoToJSONObject(annotationInfo));
                break;
            case "exon":
                exonDetailPanel.updateData(AnnotationRestService.convertAnnotationInfoToJSONObject(annotationInfo));
                break;
            case "CDS":
                cdsDetailPanel.updateData(AnnotationRestService.convertAnnotationInfoToJSONObject(annotationInfo));
                break;
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

        dataGrid.setSelectionModel(annotationInfoSingleSelectionModel);
        annotationInfoSingleSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                GWT.log("something selected");
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
                filteredAnnotationList.clear();


                for (int i = 0; i < array.size(); i++) {
                    JSONObject object = array.get(i).isObject();
//                    TreeItem treeItem = processFeatureEntry(object);
//                    features.addItem(treeItem);
                    GWT.log(object.toString());


                    AnnotationInfo annotationInfo = generateAnnotationInfo(object);
                    filteredAnnotationList.add(annotationInfo);
                }

//                features.setAnimationEnabled(true);
                GWT.log("# of annoations: " + filteredAnnotationList.size());
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

    private AnnotationInfo generateAnnotationInfo(JSONObject object) {
        return generateAnnotationInfo(object, true);
    }

    private AnnotationInfo generateAnnotationInfo(JSONObject object, boolean processChildren) {
        AnnotationInfo annotationInfo = new AnnotationInfo();
        annotationInfo.setName(object.get("name").isString().stringValue());
        GWT.log("top-level processing: " + annotationInfo.getName());
        annotationInfo.setType(object.get("type").isObject().get("name").isString().stringValue());
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

    @UiHandler("sequenceList")
    public void changeRefSequence(KeyUpEvent changeEvent) {
        selectedSequenceName = sequenceList.getText();
        reload();
    }

    private TreeItem processFeatureEntry(JSONObject object) {
        TreeItem treeItem = new TreeItem();

        String featureName = object.get("name").isString().stringValue();
        String featureType = object.get("type").isObject().get("name").isString().stringValue();
        int lastFeature = featureType.lastIndexOf(".");
        featureType = featureType.substring(lastFeature + 1);
//        HTML html = new HTML(featureName + " <div class='label label-success'>" + featureType + "</div>");
        treeItem.setWidget(new AnnotationContainerWidget(object));

        if (object.get("children") != null) {
            JSONArray childArray = object.get("children").isArray();
            for (int i = 0; childArray != null && i < childArray.size(); i++) {
                JSONObject childObject = childArray.get(i).isObject();
                treeItem.addItem(processFeatureEntry(childObject));
            }
        }

        return treeItem;
    }

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

        private void buildAnnotationRow(AnnotationInfo rowValue, int absRowIndex, boolean showTranscripts) {
            SelectionModel<? super AnnotationInfo> selectionModel = dataGrid.getSelectionModel();
            boolean isSelected =
                    (selectionModel == null || rowValue == null) ? false : selectionModel
                            .isSelected(rowValue);
            boolean isEven = absRowIndex % 2 == 0;
//            StringBuilder trClasses = new StringBuilder(rowStyle);
//            if (isSelected) {
//                trClasses.append(selectedRowStyle);
//            }

            // Calculate the cell styles.
//            String cellStyles = cellStyle;
//            if (isSelected) {
//                cellStyles += selectedCellStyle;
//            }
//            if (showTranscripts) {
//                cellStyles += childCell;
//            }

            TableRowBuilder row = startRow();
//            row.className(trClasses.toString());

      /*
       * Checkbox column.
       *
       * This table will uses a checkbox column for selection. Alternatively,
       * you can call dataGrid.setSelectionEnabled(true) to enable mouse
       * selection.
       */
//            td.className(cellStyles);
//            td.style().outlineStyle(Style.OutlineStyle.NONE).endStyle();
//            if (!showTranscripts) {
//                renderCell(td, createContext(0), nameColumn, rowValue);
//            }
//            td.endTD();

      /*
       * View friends column.
       *
       * Displays a link to "show friends". When clicked, the list of friends is
       * displayed below the contact.
       */
//            td = row.startTD();
////            td.className(cellStyles);
//            if (!showTranscripts) {
//                td.style().outlineStyle(Style.OutlineStyle.NONE).endStyle();
//                renderCell(td, createContext(1), typeColumn, rowValue);
//            }
//            td.endTD();

            TableCellBuilder td = row.startTD();

            // First name column.
//            td = row.startTD();
//            td.className(cellStyles);
            td.style().outlineStyle(Style.OutlineStyle.NONE).endStyle();
            if (showTranscripts) {
                td.text(rowValue.getName());
            } else {
                renderCell(td, createContext(2), nameColumn, rowValue);
            }
            td.endTD();

            // Last name column.
            td = row.startTD();
//            td.className(cellStyles);
            td.style().outlineStyle(Style.OutlineStyle.NONE).endStyle();
            if (showTranscripts) {
                td.text(rowValue.getType());
            } else {
                renderCell(td, createContext(3), typeColumn, rowValue);
            }
            td.endTD();

            // Age column.
            td = row.startTD();
//            td.className(cellStyles);
            td.style().outlineStyle(Style.OutlineStyle.NONE).endStyle();
            td.text(NumberFormat.getDecimalFormat().format(rowValue.getLength())).endTD();

            // Category column.
//            td = row.startTD();
//            td.className(cellStyles);
//            td.style().outlineStyle(OutlineStyle.NONE).endStyle();
//            if (showTranscripts) {
//                td.text(rowValue.getCategory().getDisplayName());
//            } else {
//                renderCell(td, createContext(5), categoryColumn, rowValue);
//            }
//            td.endTD();
//
//            // Address column.
//            td = row.startTD();
//            td.className(cellStyles);
//            DivBuilder div = td.startDiv();
//            div.style().outlineStyle(Style.OutlineStyle.NONE).endStyle();
//            div.text(rowValue.getAddress()).endDiv();
//            td.endTD();

            row.endTR();

        }


    }
}