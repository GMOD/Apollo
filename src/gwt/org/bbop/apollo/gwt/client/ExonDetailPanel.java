package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.*;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.event.AnnotationInfoChangeEvent;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.AnnotationRestService;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.InputGroupAddon;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ButtonType;

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
    Button positiveStrandValue;
    @UiField
    Button negativeStrandValue;
    @UiField
    InputGroupAddon fivePrimeField;
    @UiField
    InputGroupAddon threePrimeField;
    @UiField
    Button increaseFivePrime;
    @UiField
    Button decreaseFivePrime;
    @UiField
    Button increaseThreePrime;
    @UiField
    Button decreaseThreePrime;
    
    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<AnnotationInfo> dataGrid = new DataGrid<>(200, tablecss);
    @UiField
    HTML notePanel;
//    @UiField
//    Button phaseButton;
    private static ListDataProvider<AnnotationInfo> dataProvider = new ListDataProvider<>();
    private static List<AnnotationInfo> annotationInfoList = dataProvider.getList();
    private SingleSelectionModel<AnnotationInfo> selectionModel = new SingleSelectionModel<>();

    private TextColumn<AnnotationInfo> typeColumn;
    private Column<AnnotationInfo, Number> startColumn;
    private Column<AnnotationInfo, Number> stopColumn;
    private Column<AnnotationInfo, Number> lengthColumn;

    private Boolean editable = false ;

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
                return getDisplayMin(annotationInfo.getMin());
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


    public void updateData(AnnotationInfo annotationInfo){
        GWT.log("updating data: " + annotationInfo.getName());
        if(annotationInfo==null) return ;
        annotationInfoList.clear();
        GWT.log("sublist: " + annotationInfo.getAnnotationInfoSet().size());
        for(AnnotationInfo annotationInfo1 : annotationInfo.getAnnotationInfoSet()){
            GWT.log("adding: "+annotationInfo1.getName());
            annotationInfoList.add(annotationInfo1);
        }

        // TODO: calculate phases
//        calculatePhaseOnList(annotationInfoList);

        GWT.log("should be showing: " + annotationInfoList.size());

        if(annotationInfoList.size()>0){
            updateDetailData(annotationInfoList.get(0));
        }
        dataGrid.redraw();
    }
    
    private void calculatePhaseOnList(List<AnnotationInfo> annotationInfoList) {
        // get the CDS annotionInfo . .
//        int length = 0;
//        for (Exon exon : exons) {
//            if (!exon.overlaps(cds)) {
//                continue;
//            }
//            int fmin = exon.getFmin() < cds.getFmin() ? cds.getFmin() : exon.getFmin();
//            int fmax = exon.getFmax() > cds.getFmax() ? cds.getFmax() : exon.getFmax();
//            String phase;
//            if (length % 3 == 0) {
//                phase = "0";
//            }
//            else if (length % 3 == 1) {
//                phase = "2";
//            }
//            else {
//                phase = "1";
//            }
//            length += fmax - fmin;
//            GFF3Entry entry = new GFF3Entry(seqId, source, type, fmin + 1, fmax, score, strand, phase);
//            entry.setAttributes(extractAttributes(cds));
//            gffEntries.add(entry);
//        }

    }

    public void updateDetailData(AnnotationInfo annotationInfo) {
        this.internalAnnotationInfo = annotationInfo;
        GWT.log("updating exon detail panel");
//        GWT.log(internalData.toString());
//        nameField.setText(internalData.get("name").isString().stringValue());

//        JSONObject locationObject = this.internalData.get("location").isObject();
        fivePrimeField.setText(Integer.toString(getFivePrimeField(annotationInfo)));
        threePrimeField.setText(Integer.toString(getThreePrimeField(annotationInfo)));
        if (internalAnnotationInfo.getStrand() > 0) {
            positiveStrandValue.setType(ButtonType.PRIMARY);
            negativeStrandValue.setType(ButtonType.DEFAULT);
        } else {
            positiveStrandValue.setType(ButtonType.DEFAULT);
            negativeStrandValue.setType(ButtonType.PRIMARY);
        }

//        phaseButton.setText(internalAnnotationInfo.getPhase());

        SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder();
        for(String note : annotationInfo.getNoteList()){
            safeHtmlBuilder.appendHtmlConstant("<div class='label label-warning'>" + note + "</div>");
        }
        notePanel.setHTML(safeHtmlBuilder.toSafeHtml());

//        safeHtmlBuilder.appendHtmlConstant("<div class='label label-warning'>"+error+"</div>");


        setVisible(true);
    }

    public void redrawExonTable(){
        dataGrid.redraw();
    }
    
    // we would only ever enable these for the gene . . . not sure if we want this here
//    @UiHandler("positiveStrandValue")
    void handlePositiveStrand(ClickEvent e) {
        if (negativeStrandValue.isActive()) {
            internalAnnotationInfo.setStrand(1);
            positiveStrandValue.setActive(true);
            negativeStrandValue.setActive(false);
            updateFeatureLocation();
        }
    }

//    @UiHandler("negativeStrandValue")
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
//        enableFields(false);
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                GWT.log("return value: "+returnValue.toString());
//                Window.alert("successful update: "+returnValue);
//                enableFields(true);
                Annotator.eventBus.fireEvent(new AnnotationInfoChangeEvent(updatedInfo, AnnotationInfoChangeEvent.Action.UPDATE));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error updating exon: " + exception);
//                enableFields(true);
            }
        };
        try {
            builder.setCallback(requestCallback);
            builder.send();
//            enableFields(true);
        } catch (RequestException e) {
            // Couldn't connect to server
            Window.alert(e.getMessage());
//            enableFields(true);
        }

    }

//    private void enableFields(boolean enabled) {
////        minField.setEnabled(enabled && editable);
//        maxField.setEnabled(enabled && editable);
////        positiveStrandValue.setEnabled(enabled);
////        negativeStrandValue.setEnabled(enabled);
//    }
//
//
    private void enableFields(boolean enabled) {
        GWT.log("triggered");
        decreaseFivePrime.setEnabled(enabled);
        increaseFivePrime.setEnabled(enabled);
        decreaseThreePrime.setEnabled(enabled);
        increaseThreePrime.setEnabled(enabled);
    }
    
    public void setEditable(boolean editable) {
        this.editable = editable ;

//        maxField.setEnabled(this.editable);
//        minField.setEnabled(this.editable);
    }
    
    public boolean isEditableType(String type) {
        if (type.equals("CDS") || type.equals("exon")) {
            return true; 
        }
        else {
            return false;
        }
    }

    private void updateFeatureLocation(final AnnotationInfo originalInfo) {
        String url = rootUrl + "/annotator/updateFeatureLocation";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
        sb.append("data=" + AnnotationRestService.convertAnnotationInfoToJSONObject(this.internalAnnotationInfo).toString());
        final AnnotationInfo updatedInfo = this.internalAnnotationInfo;
        builder.setRequestData(sb.toString());
        enableFields(false);
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                GWT.log("return value: " + returnValue.toString());
                enableFields(true);
                Annotator.eventBus.fireEvent(new AnnotationInfoChangeEvent(updatedInfo, AnnotationInfoChangeEvent.Action.UPDATE));
                updateDetailData(updatedInfo);
                redrawExonTable();
                
            }

            @Override
            public void onError(Request request, Throwable exception) {
                //todo: handling different types of errors
                Window.alert("Error updating exon: " + exception);
                fivePrimeField.setText(Integer.toString(getFivePrimeField(originalInfo)));
                threePrimeField.setText(Integer.toString(getThreePrimeField(originalInfo)));
                enableFields(true);
            }
        };
        try {
            builder.setCallback(requestCallback);
            builder.send();
        } catch (RequestException e) {
            Window.alert(e.getMessage());
            enableFields(true);
        }
    }
    
    private int getDisplayMin(int min) {
        // increases the fmin by 1 for display since coordinates are handled as zero-based on server-side
        return min + 1;
    }

    @UiHandler("decreaseFivePrime")
    public void setDecreaseFivePrime(ClickEvent e) {
        String type = internalAnnotationInfo.getType();
        if (!isEditableType(type)) {
            return;
        }
        final AnnotationInfo beforeUpdate = this.internalAnnotationInfo;
        if (internalAnnotationInfo.getStrand() == 1) {
            // fmin is 5' ; fmax is 3'
            internalAnnotationInfo.setMin(internalAnnotationInfo.getMin() - 1);
            fivePrimeField.setText(Integer.toString(getDisplayMin(internalAnnotationInfo.getMin())));
            updateFeatureLocation(beforeUpdate);
        }
        else {
            // fmin is 3' ; fmax is 5'
            GWT.log("getMax(): " + internalAnnotationInfo.getMax());
            GWT.log("getMin(): " + internalAnnotationInfo.getMin());
            if (internalAnnotationInfo.getMax() - 1 <= internalAnnotationInfo.getMin()) {
                GWT.log("Cannot decrease 5' end to be less than 3' end");
                return;
            }
            internalAnnotationInfo.setMax(internalAnnotationInfo.getMax() - 1);
            fivePrimeField.setText(Integer.toString(internalAnnotationInfo.getMax()));
            updateFeatureLocation(beforeUpdate);
        }
    }
    
    @UiHandler("increaseFivePrime")
    public void setIncreaseFivePrime(ClickEvent e) {
        String type = internalAnnotationInfo.getType();
        if (!isEditableType(type)) {
            return;
        }
        final AnnotationInfo beforeUpdate = this.internalAnnotationInfo;
        if (internalAnnotationInfo.getStrand() == 1) {
            // fmin is 5' ; fmax is 3'
            if (internalAnnotationInfo.getMin() + 1 >= internalAnnotationInfo.getMax()) {
                GWT.log("Cannot increase 5' end to be greater than 3' end");
                return;
            }
            internalAnnotationInfo.setMin(internalAnnotationInfo.getMin() + 1);
            fivePrimeField.setText(Integer.toString(getDisplayMin(internalAnnotationInfo.getMin())));
            updateFeatureLocation(beforeUpdate);
        }
        else {
            // fmin is 3' ; fmax is 5'
            internalAnnotationInfo.setMax(internalAnnotationInfo.getMax() + 1);
            fivePrimeField.setText(Integer.toString(internalAnnotationInfo.getMax()));
            updateFeatureLocation(beforeUpdate);
        }
    }
    
    @UiHandler("decreaseThreePrime")
    public void setDecreaseThreePrime(ClickEvent e) {
        String type = internalAnnotationInfo.getType();
        if (!isEditableType(type)) {
            return;
        }
        final AnnotationInfo beforeUpdate = this.internalAnnotationInfo;
        if (internalAnnotationInfo.getStrand() == 1) {
            // fmin is 5' ; fmax is 3'
            if (internalAnnotationInfo.getMax() - 1 <= internalAnnotationInfo.getMin()) {
                GWT.log("Cannot decrease 3' end to be less than 5' end");
                return;
            }
            internalAnnotationInfo.setMax(internalAnnotationInfo.getMax() - 1);
            threePrimeField.setText(Integer.toString(internalAnnotationInfo.getMax()));
            updateFeatureLocation(beforeUpdate);
        }
        else {
             // fmin is 3' ; fmax is 5'
            internalAnnotationInfo.setMin(internalAnnotationInfo.getMin() - 1);
            threePrimeField.setText(Integer.toString(getDisplayMin(internalAnnotationInfo.getMin())));
            updateFeatureLocation(beforeUpdate);
        }
    }

    @UiHandler("increaseThreePrime")
    public void setIncreaseThreePrime(ClickEvent e) {
        String type = internalAnnotationInfo.getType();
        if (!isEditableType(type)) {
            return;
        }
        final AnnotationInfo beforeUpdate = this.internalAnnotationInfo;
        if (internalAnnotationInfo.getStrand() == 1) {
            // fmin is 5' ; fmax is 3'
            internalAnnotationInfo.setMax(internalAnnotationInfo.getMax() + 1);
            threePrimeField.setText(Integer.toString(internalAnnotationInfo.getMax()));
            updateFeatureLocation(beforeUpdate);
        }
        else {
            // fmin is 3' ; fmax is 5'
            if(internalAnnotationInfo.getMin() + 1 >= internalAnnotationInfo.getMax()) {
                GWT.log("Cannot increase 3' end to be greater than 5' end");
                return;
            }
            internalAnnotationInfo.setMin(internalAnnotationInfo.getMin() + 1);
            threePrimeField.setText(Integer.toString(getDisplayMin(internalAnnotationInfo.getMin())));
            updateFeatureLocation(beforeUpdate);
        }
    }
    
    private int getFivePrimeField(AnnotationInfo annotationInfo) {
        if (annotationInfo.getStrand() == 1) {
            return getDisplayMin(annotationInfo.getMin());
        }
        else {
            return  annotationInfo.getMax();
        }
    }
    
    private int getThreePrimeField(AnnotationInfo annotationInfo) {
        if (annotationInfo.getStrand() == 1) {
            return annotationInfo.getMax();
        }
        else {
            return getDisplayMin(annotationInfo.getMin());
        }
    }
}
