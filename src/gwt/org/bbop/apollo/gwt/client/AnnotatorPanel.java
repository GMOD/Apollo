package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.demo.DataGenerator;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.rest.SequenceRestService;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;

import java.util.Comparator;
import java.util.List;

/**
 * Created by ndunn on 12/17/14.
 */
public class AnnotatorPanel extends Composite {
    interface AnnotatorPanelUiBinder extends UiBinder<com.google.gwt.user.client.ui.Widget, AnnotatorPanel> {
    }

    private static AnnotatorPanelUiBinder ourUiBinder = GWT.create(AnnotatorPanelUiBinder.class);

    Dictionary dictionary = Dictionary.getDictionary("Options");
    String rootUrl = dictionary.get("rootUrl");
    private String selectedSequenceName = null  ;

    @UiField
    TextBox nameSearchBox;
    @UiField(provided = true)
    SuggestBox sequenceList;
    @UiField
    CheckBox cdsFilter;
    @UiField
    CheckBox stopCodonFilter;


    Tree.Resources tablecss = GWT.create(Tree.Resources.class);
//    @UiField(provided = true)
    Tree features = new Tree(tablecss);

    @UiField(provided = true)
    DataGrid<AnnotationInfo> dataGrid = new DataGrid<>();


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

    public AnnotatorPanel() {
        sequenceList = new SuggestBox(sequenceOracle);
        dataGrid.setWidth("100%");
        dataGrid.setEmptyTableWidget(new Label("Loading"));


        TextColumn<AnnotationInfo> nameColumn = new TextColumn<AnnotationInfo>() {
            @Override
            public String getValue(AnnotationInfo annotationInfo) {
                return annotationInfo.getName();
            }
        };
        nameColumn.setSortable(true);

        TextColumn<AnnotationInfo> typeColumn = new TextColumn<AnnotationInfo>() {
            @Override
            public String getValue(AnnotationInfo annotationInfo) {
                return annotationInfo.getType();
            }
        };
        typeColumn.setSortable(true);

        Column<AnnotationInfo,Number> lengthColumn = new Column<AnnotationInfo,Number>(new NumberCell()) {
            @Override
            public Integer getValue(AnnotationInfo annotationInfo) {
                return annotationInfo.getLength();
            }
        };
        lengthColumn.setSortable(true);


        dataGrid.addColumn(nameColumn,"Name");
        dataGrid.addColumn(typeColumn,"Type");
        dataGrid.addColumn(lengthColumn,"Length");


        ColumnSortEvent.ListHandler<AnnotationInfo> sortHandler = new ColumnSortEvent.ListHandler<AnnotationInfo>(filteredAnnotationList);
        dataGrid.addColumnSortHandler(sortHandler);

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
                return o1.getLength()- o2.getLength() ;
            }
        });

        dataGrid.setSelectionModel(annotationInfoSingleSelectionModel);
        annotationInfoSingleSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler(){
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                GWT.log("something selected");
            }
        });

        dataProvider.addDataDisplay(dataGrid);
//        pager.setDisplay(dataGrid);


        Widget rootElement = ourUiBinder.createAndBindUi(this);

        initWidget(rootElement);

        stopCodonFilter.setValue(false);

        features.setAnimationEnabled(true);

        features.addSelectionHandler(new SelectionHandler<TreeItem>() {
            @Override
            public void onSelection(SelectionEvent<TreeItem> event) {
                JSONObject internalData = ((AnnotationContainerWidget) event.getSelectedItem().getWidget()).getInternalData();
                String type = getType(internalData);
                geneDetailPanel.setVisible(false);
                transcriptDetailPanel.setVisible(false);
                exonDetailPanel.setVisible(false);
                switch (type) {
                    case "gene":
                    case "pseduogene":
                        geneDetailPanel.updateData(internalData);
                        break;
                    case "mRNA":
                    case "tRNA":
                        transcriptDetailPanel.updateData(internalData);
                        break;
                    case "exon":
                        exonDetailPanel.updateData(internalData);
                        break;
                    case "CDS":
                        exonDetailPanel.updateData(internalData);
                        break;
                    default:
                        GWT.log("not sure what to do with " + type);
                }
//                annotationName.setText(internalData.get("name").isString().stringValue());
            }
        });
//        annotationName.setText("sox9a-000-00-0");


    }

    private void loadSequences() {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();

                if(selectedSequenceName==null && array.size()>0){
                    selectedSequenceName = array.get(0).isObject().get("name").isString().stringValue();
                }
                sequenceOracle.clear();
                for(int i = 0 ; i < array.size() ; i++){
                    JSONObject object = array.get(i).isObject();
                    SequenceInfo sequenceInfo = new SequenceInfo();
                    sequenceInfo.setName(object.get("name").isString().stringValue());
                    sequenceInfo.setLength((int) object.get("length").isNumber().isNumber().doubleValue());
                    sequenceOracle.add(sequenceInfo.getName());
//                    sequenceList.addItem(sequenceInfo.getName());
                    if(selectedSequenceName.equals(sequenceInfo.getName())){
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
        SequenceRestService.loadSequences(requestCallback,MainPanel.currentOrganismId);
    }

    private String getType(JSONObject internalData) {
        return internalData.get("type").isObject().get("name").isString().stringValue();
    }

    public void reload() {
        if(selectedSequenceName==null) {
            selectedSequenceName = MainPanel.currentSequenceId;
            loadSequences();
        }
        features.setAnimationEnabled(false);

        String url = rootUrl + "/annotator/findAnnotationsForSequence/?sequenceName="+selectedSequenceName;
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isObject().get("features").isArray();
                features.clear();
                filteredAnnotationList.clear();


                for (int i = 0; i < array.size(); i++) {
                    JSONObject object = array.get(i).isObject();
                    TreeItem treeItem = processFeatureEntry(object);
                    features.addItem(treeItem);
                    GWT.log(object.toString());


                    AnnotationInfo annotationInfo = new AnnotationInfo();
                    annotationInfo.setName(object.get("name").isString().stringValue());
                    annotationInfo.setType(object.get("type").isObject().get("name").isString().stringValue());
                    annotationInfo.setMin((int) object.get("location").isObject().get("fmin").isNumber().doubleValue());
                    annotationInfo.setMax((int) object.get("location").isObject().get("fmax").isNumber().doubleValue());
                    filteredAnnotationList.add(annotationInfo);
                }

                features.setAnimationEnabled(true);
                GWT.log("# of annoations: "+filteredAnnotationList.size());
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

    @UiHandler("sequenceList")
    public void changeRefSequence(KeyUpEvent changeEvent){
        selectedSequenceName = sequenceList.getText() ;
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
}