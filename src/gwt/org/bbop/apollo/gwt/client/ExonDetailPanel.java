package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.http.client.*;
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
import org.bbop.apollo.gwt.client.dto.AnnotationInfoConverter;
import org.bbop.apollo.gwt.client.event.AnnotationInfoChangeEvent;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Container;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.Comparator;
import java.util.List;

/**
 * Created by Nathan Dunn on 1/9/15.
 */
public class ExonDetailPanel extends Composite {


    interface ExonDetailPanelUiBinder extends UiBinder<Widget, ExonDetailPanel> {
    }

    int inputFmin, inputFmax;
    int fivePrimeValue, threePrimeValue;
    private AnnotationInfo internalAnnotationInfo;
    private AnnotationInfo annotationInfoWithTopLevelFeature;
    private static ExonDetailPanelUiBinder ourUiBinder = GWT.create(ExonDetailPanelUiBinder.class);

    @UiField
    Button positiveStrandValue;
    @UiField
    Button negativeStrandValue;
    @UiField
    TextBox fivePrimeField;
    @UiField
    TextBox threePrimeField;
    @UiField
    Button increaseFivePrime;
    @UiField
    Button decreaseFivePrime;
    @UiField
    Button increaseThreePrime;
    @UiField
    Button decreaseThreePrime;
    @UiField
    Container exonEditContainer;
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
                if(selectionModel.getSelectedSet().isEmpty()){
                    exonEditContainer.setVisible(false);
                }
                else{
                    exonEditContainer.setVisible(true);
                    updateDetailData(selectionModel.getSelectedObject());
                }
            }
        });


        initWidget(ourUiBinder.createAndBindUi(this));
    }

    private void initializeTable() {
        typeColumn = new TextColumn<AnnotationInfo>() {
            @Override
            public String getValue(AnnotationInfo annotationInfo) {
                String annotationTypeString = annotationInfo.getType();
                if(annotationTypeString.equals("non_canonical_five_prime_splice_site")){
                    annotationTypeString = "NC 5' splice";
                }
                else
                if(annotationTypeString.equals("non_canonical_three_prime_splice_site")){
                    annotationTypeString = "NC 3' splice";
                }
                return annotationTypeString;
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
        if(annotationInfo==null) return ;
        //displayAnnotationInfo(annotationInfo);
        getAnnotationInfoWithTopLevelFeature(annotationInfo);
        annotationInfoList.clear();
        exonEditContainer.setVisible(false);
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
        // updates the detail section (3' and 5' coordinates) when user clicks on any of the types in the table.
        // mRNA information is not available
        this.internalAnnotationInfo = annotationInfo;
        GWT.log("updating exon detail panel");
//        GWT.log(internalData.toString());
//        nameField.setText(internalData.get("name").isString().stringValue());

//        JSONObject locationObject = this.internalData.get("location").isObject();
        coordinatesToPrime(annotationInfo.getMin(), annotationInfo.getMax());
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
        String url = Annotator.getRootUrl() + "annotator/updateFeatureLocation";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
//        sb.append("data=" + internalData.toString());
        sb.append("data=" + AnnotationInfoConverter.convertAnnotationInfoToJSONObject(this.internalAnnotationInfo).toString());
        final AnnotationInfo updatedInfo = this.internalAnnotationInfo ;
        builder.setRequestData(sb.toString());
//        enableFields(false);
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                GWT.log("return value: "+returnValue.toString());
                Annotator.eventBus.fireEvent(new AnnotationInfoChangeEvent(updatedInfo, AnnotationInfoChangeEvent.Action.UPDATE));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error updating exon: " + exception);
//                enableFields(true);
            }
        };
        try {
            builder.setCallback(requestCallback);
            builder.send();
//            enableFields(true);
        } catch (RequestException e) {
            // Couldn't connect to server
            Bootbox.alert(e.getMessage());
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
        String url = Annotator.getRootUrl()+ "annotator/updateFeatureLocation";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
        sb.append("data=" + AnnotationInfoConverter.convertAnnotationInfoToJSONObject(this.internalAnnotationInfo).toString());
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
                Bootbox.alert("Error updating exon: " + exception.toString());
                coordinatesToPrime(originalInfo.getMin(), originalInfo.getMax());
                enableFields(true);
            }
        };
        try {
            builder.setCallback(requestCallback);
            builder.send();
        } catch (RequestException e) {
            Bootbox.alert(e.getMessage());
            enableFields(true);
        }
    }
    
    private int getDisplayMin(int min) {
        // increases the fmin by 1 for display since coordinates are handled as zero-based on server-side
        return min + 1;
    }

    @UiHandler("decreaseFivePrime")
    public void decreaseFivePrimePosition(ClickEvent e) {
        try {
            fivePrimeValue = Integer.parseInt(fivePrimeField.getText()) - 1; // intended action
            threePrimeValue = Integer.parseInt(threePrimeField.getText());
        } catch (Exception error) {
            coordinatesToPrime(this.internalAnnotationInfo.getMin(), this.internalAnnotationInfo.getMax());
            return;
        }
        
        if (verifyOperation()) {
            triggerUpdate(fivePrimeValue, threePrimeValue);
        }
    }
    
    @UiHandler("increaseFivePrime")
    public void increaseFivePrimePosition(ClickEvent e) {
        try {
            fivePrimeValue = Integer.parseInt(fivePrimeField.getText()) + 1; // intended action
            threePrimeValue = Integer.parseInt(threePrimeField.getText());
        } catch (Exception error) {
            coordinatesToPrime(this.internalAnnotationInfo.getMin(), this.internalAnnotationInfo.getMax());
            return;
        }

        if (verifyOperation()) {
            triggerUpdate(fivePrimeValue, threePrimeValue);
        }
    }
    
    @UiHandler("fivePrimeField")
    public void fivePrimeTextEntry(KeyDownEvent k) {
        if (k.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            try {
                fivePrimeValue = Integer.parseInt(fivePrimeField.getText());
                threePrimeValue = Integer.parseInt(threePrimeField.getText());
            } catch (Exception error) {
                coordinatesToPrime(this.internalAnnotationInfo.getMin(), this.internalAnnotationInfo.getMax());
                return;
            }

            if (verifyOperation()) {
                triggerUpdate(fivePrimeValue, threePrimeValue);
            } else {
                coordinatesToPrime(this.internalAnnotationInfo.getMin(), this.internalAnnotationInfo.getMax());
            }
        }
    }
    
    @UiHandler("fivePrimeField")
    public void fivePrimeTextFocus(BlurEvent b) {
        try {
            fivePrimeValue = Integer.parseInt(fivePrimeField.getText());
            threePrimeValue = Integer.parseInt(threePrimeField.getText());
        } catch (Exception error) {
            coordinatesToPrime(this.internalAnnotationInfo.getMin(), this.internalAnnotationInfo.getMax());
            return;
        }

        if (verifyOperation()) {
            triggerUpdate(fivePrimeValue, threePrimeValue);
        }
        else {
            coordinatesToPrime(this.internalAnnotationInfo.getMin(), this.internalAnnotationInfo.getMax());
        }
    }
    
    @UiHandler("decreaseThreePrime")
    public void decreaseThreePrimePosition(ClickEvent e) {
        try {
            fivePrimeValue = Integer.parseInt(fivePrimeField.getText());
            threePrimeValue = Integer.parseInt(threePrimeField.getText()) - 1; // intended action
        } catch (Exception error) {
            coordinatesToPrime(this.internalAnnotationInfo.getMin(), this.internalAnnotationInfo.getMax());
            return;
        }

        if (verifyOperation()) {
            triggerUpdate(fivePrimeValue, threePrimeValue);
        }
    }
    
    @UiHandler("increaseThreePrime")
    public void increaseThreePrime(ClickEvent e) {
        try {
            fivePrimeValue = Integer.parseInt(fivePrimeField.getText());
            threePrimeValue = Integer.parseInt(threePrimeField.getText()) + 1; // intended action
        } catch (Exception error) {
            coordinatesToPrime(this.internalAnnotationInfo.getMin(), this.internalAnnotationInfo.getMax());
            return;
        }

        if (verifyOperation()) {
            triggerUpdate(fivePrimeValue, threePrimeValue);
        }
    }
    
    @UiHandler("threePrimeField")
    public void threePrimeTextEntry(KeyDownEvent k) {
        if (k.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            try {
                fivePrimeValue = Integer.parseInt(fivePrimeField.getText());
                threePrimeValue = Integer.parseInt(threePrimeField.getText());
            } catch (Exception error) {
                coordinatesToPrime(this.internalAnnotationInfo.getMin(), this.internalAnnotationInfo.getMax());
                return;
            }

            if (verifyOperation()) {
                triggerUpdate(fivePrimeValue, threePrimeValue);
            }
            else {
                coordinatesToPrime(this.internalAnnotationInfo.getMin(), this.internalAnnotationInfo.getMax());
            }
        }
    }
    
    @UiHandler("threePrimeField")
    public void threePrimeTextFocus(BlurEvent b) {
        try {
            fivePrimeValue = Integer.parseInt(fivePrimeField.getText());
            threePrimeValue = Integer.parseInt(threePrimeField.getText());
        } catch (Exception error) {
            coordinatesToPrime(this.internalAnnotationInfo.getMin(), this.internalAnnotationInfo.getMax());
            return;
        }

        if (verifyOperation()) {
            triggerUpdate(fivePrimeValue, threePrimeValue);
        }
        else {
            coordinatesToPrime(this.internalAnnotationInfo.getMin(), this.internalAnnotationInfo.getMax());
        }
    }
    
    public boolean verifyOperation() {
        if (!isEditableType(this.internalAnnotationInfo.getType())) { return false; }
        primeToCoordinates(this.fivePrimeValue, this.threePrimeValue);
        if (!(this.inputFmin < this.internalAnnotationInfo.getMax()) || !(this.inputFmax > this.internalAnnotationInfo.getMin())) {
            return false;
        }
        if (!verifyBoundaries(this.internalAnnotationInfo)) { return false; };
        return true;
    }
    
    public void triggerUpdate(int fivePrimeValue, int threePrimeValue) {
        final AnnotationInfo originalInfo = this.internalAnnotationInfo;
        fivePrimeField.setText(Integer.toString(fivePrimeValue));
        this.internalAnnotationInfo.setMin(this.inputFmin);
        threePrimeField.setText(Integer.toString(threePrimeValue));
        this.internalAnnotationInfo.setMax(this.inputFmax);
        updateFeatureLocation(originalInfo);
    }

    public void primeToCoordinates(int fivePrimeFieldValue, int threePrimeFieldValue) {
        if (this.internalAnnotationInfo.getStrand() == 1) {
            this.inputFmin = fivePrimeFieldValue - 1;
            this.inputFmax = threePrimeFieldValue;
        }
        else {
            this.inputFmin = threePrimeFieldValue - 1;
            this.inputFmax = fivePrimeFieldValue;
        }
    }
    
    public void coordinatesToPrime(int fmin, int fmax) {
        if (this.internalAnnotationInfo.getStrand() == 1) {
            this.fivePrimeField.setText(Integer.toString(fmin + 1));
            this.threePrimeField.setText(Integer.toString(fmax));
        }
        else {
            this.fivePrimeField.setText(Integer.toString(fmax));
            this.threePrimeField.setText(Integer.toString(fmin + 1));
        }
    }

    private void getAnnotationInfoWithTopLevelFeature(AnnotationInfo annotationInfo) {
        this.annotationInfoWithTopLevelFeature = annotationInfo;
    }
    
    private boolean verifyBoundaries(AnnotationInfo annotationInfo) {
        if (this.inputFmin >= annotationInfoWithTopLevelFeature.getMin() && this.inputFmax <= annotationInfoWithTopLevelFeature.getMax()) {
            return true;
        }
        else {
            GWT.log("Cannot extend beyond the boundaries of the mRNA");
            return false;
        }
    }
}
