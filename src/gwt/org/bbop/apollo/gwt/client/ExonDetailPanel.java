package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.event.AnnotationInfoChangeEvent;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.AnnotationRestService;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.TextBox;

import java.util.Comparator;
import java.util.List;

/**
 * Created by ndunn on 1/9/15.
 */
public class ExonDetailPanel extends Composite {

    interface ExonDetailPanelUiBinder extends UiBinder<Widget, ExonDetailPanel> {
    }

    Dictionary dictionary = Dictionary.getDictionary("Options");
    String rootUrl = dictionary.get("rootUrl");
//    private JSONObject internalData;
    private AnnotationInfo internalAnnotationInfo;

    private static ExonDetailPanelUiBinder ourUiBinder = GWT.create(ExonDetailPanelUiBinder.class);
    @UiField
    TextBox maxField;
    @UiField
    TextBox minField;
    @UiField
    Button positiveStrandValue;
    @UiField
    Button negativeStrandValue;

    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<AnnotationInfo> dataGrid = new DataGrid<>(10, tablecss);
    private static ListDataProvider<AnnotationInfo> dataProvider = new ListDataProvider<>();
    private static List<AnnotationInfo> annotationInfoList = dataProvider.getList();
    private SingleSelectionModel<AnnotationInfo> selectionModel = new SingleSelectionModel<>();

    private TextColumn<AnnotationInfo> typeColumn;
    private Column<AnnotationInfo, Number> startColumn;
    private Column<AnnotationInfo, Number> stopColumn;
    private Column<AnnotationInfo, Number> lengthColumn;


    public ExonDetailPanel() {
        dataGrid.setWidth("100%");
        initializeTable();
        dataProvider.addDataDisplay(dataGrid);
        dataGrid.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                updateDetailData(selectionModel.getSelectedObject());
            }
        });


        initWidget(ourUiBinder.createAndBindUi(this));
    }

    private void initializeTable() {
        typeColumn = new TextColumn<AnnotationInfo>() {
            @Override
            public String getValue(AnnotationInfo annotationInfo) {
                return annotationInfo.getType();
            }
        };
        typeColumn.setSortable(true);

        startColumn = new Column<AnnotationInfo, Number>(new NumberCell()) {
            @Override
            public Integer getValue(AnnotationInfo annotationInfo) {
                return annotationInfo.getMin();
            }
        };
        startColumn.setSortable(true);

        stopColumn = new Column<AnnotationInfo, Number>(new NumberCell()) {
            @Override
            public Integer getValue(AnnotationInfo annotationInfo) {
                return annotationInfo.getMax();
            }
        };
        stopColumn.setSortable(true);

        lengthColumn = new Column<AnnotationInfo, Number>(new NumberCell()) {
            @Override
            public Integer getValue(AnnotationInfo annotationInfo) {
                return annotationInfo.getLength();
            }
        };
        lengthColumn.setSortable(true);

        dataGrid.addColumn(typeColumn, "Type");
        dataGrid.addColumn(startColumn, "Start");
//        dataGrid.addColumn(stopColumn, "Stop");
        dataGrid.addColumn(lengthColumn, "Length");

        ColumnSortEvent.ListHandler<AnnotationInfo> sortHandler = new ColumnSortEvent.ListHandler<AnnotationInfo>(annotationInfoList);
        dataGrid.addColumnSortHandler(sortHandler);

        sortHandler.setComparator(typeColumn, new Comparator<AnnotationInfo>() {
            @Override
            public int compare(AnnotationInfo o1, AnnotationInfo o2) {
                return o1.getType().compareTo(o2.getType());
            }
        });

        sortHandler.setComparator(startColumn, new Comparator<AnnotationInfo>() {
            @Override
            public int compare(AnnotationInfo o1, AnnotationInfo o2) {
                return o1.getMin() - o2.getMin();
            }
        });

        sortHandler.setComparator(stopColumn, new Comparator<AnnotationInfo>() {
            @Override
            public int compare(AnnotationInfo o1, AnnotationInfo o2) {
                return o1.getMax() - o2.getMax();
            }
        });

        sortHandler.setComparator(lengthColumn, new Comparator<AnnotationInfo>() {
            @Override
            public int compare(AnnotationInfo o1, AnnotationInfo o2) {
                return o1.getLength() - o2.getLength();
            }
        });
    }

    private void enableFields(boolean enabled) {
        minField.setEnabled(enabled);
        maxField.setEnabled(enabled);
        positiveStrandValue.setEnabled(enabled);
        negativeStrandValue.setEnabled(enabled);
    }

    public void updateData(AnnotationInfo annotationInfo){
        GWT.log("updating data: " + annotationInfo.getName());
        if(annotationInfo==null) return ;
        annotationInfoList.clear();
        GWT.log("sublist: " + annotationInfo.getAnnotationInfoSet().size());
        for(AnnotationInfo annotationInfo1 : annotationInfo.getAnnotationInfoSet()){
            GWT.log("adding: "+annotationInfo1.getName());
            annotationInfoList.add(annotationInfo1);
        }

        GWT.log("should be showing: "+annotationInfoList.size());

        if(annotationInfoList.size()>0){
            updateDetailData(annotationInfoList.get(0));
        }
        dataGrid.redraw();
    }

    public void updateDetailData(AnnotationInfo annotationInfo) {
        this.internalAnnotationInfo = annotationInfo;
        GWT.log("updating exon detail panel");
//        GWT.log(internalData.toString());
//        nameField.setText(internalData.get("name").isString().stringValue());

//        JSONObject locationObject = this.internalData.get("location").isObject();
        minField.setText(internalAnnotationInfo.getMin().toString());
        maxField.setText(internalAnnotationInfo.getMax().toString());

        if (internalAnnotationInfo.getStrand() > 0) {
            positiveStrandValue.setActive(true);
            negativeStrandValue.setActive(false);
        } else {
            positiveStrandValue.setActive(false);
            negativeStrandValue.setActive(true);
        }


        setVisible(true);
    }

    @UiHandler("minField")
    void handleMinChange(ChangeEvent e) {
        internalAnnotationInfo.setMin(Integer.parseInt(minField.getText()));
        updateFeatureLocation();
    }

    @UiHandler("maxField")
    void handleMaxChange(ChangeEvent e) {
        internalAnnotationInfo.setMax(Integer.parseInt(maxField.getText()));
        updateFeatureLocation();
    }

    @UiHandler("positiveStrandValue")
    void handlePositiveStrand(ClickEvent e) {
        if (negativeStrandValue.isActive()) {
            internalAnnotationInfo.setStrand(1);
            positiveStrandValue.setActive(true);
            negativeStrandValue.setActive(false);
            updateFeatureLocation();
        }
    }

    @UiHandler("negativeStrandValue")
    void handleNegativeStrand(ClickEvent e) {
        if (positiveStrandValue.isActive()) {
            internalAnnotationInfo.setStrand(-1);
            positiveStrandValue.setActive(false);
            negativeStrandValue.setActive(true);
            updateFeatureLocation();
        }
    }


    private void updateFeatureLocation() {
        String url = rootUrl + "/annotator/updateFeatureLocation";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
//        sb.append("data=" + internalData.toString());
        sb.append("data=" + AnnotationRestService.convertAnnotationInfoToJSONObject(this.internalAnnotationInfo).toString());
        final AnnotationInfo updatedInfo = this.internalAnnotationInfo ;
        builder.setRequestData(sb.toString());
        enableFields(false);
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                GWT.log("return value: "+returnValue.toString());
//                Window.alert("successful update: "+returnValue);
                enableFields(true);
                Annotator.eventBus.fireEvent(new AnnotationInfoChangeEvent(updatedInfo, AnnotationInfoChangeEvent.Action.UPDATE));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error updating exon: " + exception);
                enableFields(true);
            }
        };
        try {
            builder.setCallback(requestCallback);
            builder.send();
            enableFields(true);
        } catch (RequestException e) {
            // Couldn't connect to server
            Window.alert(e.getMessage());
            enableFields(true);
        }

    }
}
