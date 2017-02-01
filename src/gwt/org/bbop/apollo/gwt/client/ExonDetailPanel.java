package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
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
import org.bbop.apollo.gwt.client.rest.RestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Container;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.Comparator;
import java.util.List;

/**
 * Created by ndunn on 1/9/15.
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
    private static ListDataProvider<AnnotationInfo> dataProvider = new ListDataProvider<>();
    private static List<AnnotationInfo> annotationInfoList = dataProvider.getList();
    private SingleSelectionModel<AnnotationInfo> selectionModel = new SingleSelectionModel<>();

    private TextColumn<AnnotationInfo> typeColumn;
    private Column<AnnotationInfo, Number> startColumn;
    private Column<AnnotationInfo, Number> stopColumn;
    private Column<AnnotationInfo, Number> lengthColumn;

    private Boolean editable = false;

    public ExonDetailPanel() {
        dataGrid.setWidth("100%");
        initializeTable();
        dataProvider.addDataDisplay(dataGrid);
        dataGrid.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if (selectionModel.getSelectedSet().isEmpty()) {
                    exonEditContainer.setVisible(false);
                } else {
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
                if (annotationTypeString.equals("non_canonical_five_prime_splice_site")) {
                    annotationTypeString = "NC 5' splice";
                } else if (annotationTypeString.equals("non_canonical_three_prime_splice_site")) {
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

    public boolean updateData() {
        return updateData(null,null);
    }

    public boolean updateData(AnnotationInfo annotationInfo) {
        return updateData(annotationInfo,null);
    }

    public boolean updateData(AnnotationInfo annotationInfo,AnnotationInfo selectedAnnotationInfo) {
        if (annotationInfo == null) {
            return false;
        }
        exonEditContainer.setVisible(false);
        //displayAnnotationInfo(annotationInfo);
        getAnnotationInfoWithTopLevelFeature(annotationInfo);
        annotationInfoList.clear();
        GWT.log("sublist: " + annotationInfo.getAnnotationInfoSet().size());
        for (AnnotationInfo annotationInfo1 : annotationInfo.getAnnotationInfoSet()) {
            GWT.log("adding: " + annotationInfo1.getName());
            annotationInfoList.add(annotationInfo1);
        }

        dataGrid.setVisibleRangeAndClearData(dataGrid.getVisibleRange(), true);
        if(selectedAnnotationInfo==null){
            exonEditContainer.setVisible(true);
            return false ;
        }

        return true ;
    }

    private void updateDetailData(AnnotationInfo annotationInfo) {
        // updates the detail section (3' and 5' coordinates) when user clicks on any of the types in the table.
        // mRNA information is not available
        this.internalAnnotationInfo = annotationInfo;
        GWT.log("updating exon detail panel");
        coordinatesToPrime(annotationInfo.getMin(), annotationInfo.getMax());
        if (internalAnnotationInfo.getStrand() > 0) {
            positiveStrandValue.setType(ButtonType.PRIMARY);
            negativeStrandValue.setType(ButtonType.DEFAULT);
        } else {
            positiveStrandValue.setType(ButtonType.DEFAULT);
            negativeStrandValue.setType(ButtonType.PRIMARY);
        }

        String type = this.internalAnnotationInfo.getType();
        if (type.equals("exon")) {
            enableFields(true);
        }
        else {
            enableFields(false);
        }

        SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder();
        for (String note : annotationInfo.getNoteList()) {
            safeHtmlBuilder.appendHtmlConstant("<div class='label label-warning'>" + note + "</div>");
        }
        notePanel.setHTML(safeHtmlBuilder.toSafeHtml());

        setVisible(true);
    }

    public void redrawExonTable() {
        dataGrid.redraw();
    }

    private void enableFields(boolean enabled) {
        decreaseFivePrime.setEnabled(enabled);
        increaseFivePrime.setEnabled(enabled);
        decreaseThreePrime.setEnabled(enabled);
        increaseThreePrime.setEnabled(enabled);
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    private boolean isEditableType(String type) {
        return type.equals("exon");
    }

    private void updateFeatureLocation(final AnnotationInfo originalInfo) {
        final AnnotationInfo updatedInfo = this.internalAnnotationInfo;
        enableFields(false);
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
//                JSONValue returnValue = JSONParser.parseStrict(response.getText());

//                GWT.log("return value: " + returnValue.toString());
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
        RestService.sendRequest(requestCallback, "annotator/setExonBoundaries/", AnnotationRestService.convertAnnotationInfoToJSONObject(this.internalAnnotationInfo));
    }

    private int getDisplayMin(int min) {
        // increases the fmin by 1 for display since coordinates are handled as zero-based on server-side
        return min + 1;
    }

    private boolean collectFieldValues(int threePrimeDelta, int fivePrimeDelta) {
        try {
            fivePrimeValue = Integer.parseInt(fivePrimeField.getText()) + fivePrimeDelta;
            threePrimeValue = Integer.parseInt(threePrimeField.getText()) + threePrimeDelta;
        } catch (Exception error) {
            coordinatesToPrime(this.internalAnnotationInfo.getMin(), this.internalAnnotationInfo.getMax());
            return false;
        }
        return true;
    }


    private void trasformOperation() {
        if (verifyOperation()) {
            triggerUpdate(fivePrimeValue, threePrimeValue);
        } else {
            coordinatesToPrime(this.internalAnnotationInfo.getMin(), this.internalAnnotationInfo.getMax());
        }
    }

    private void handleExonUpdates() {
        handleExonUpdates(0, 0);
    }

    private void handleExonUpdates(int threePrimeDelta, int fivePrimeDelta) {
        if (!collectFieldValues(threePrimeDelta, fivePrimeDelta)) {
            return;
        }
        trasformOperation();
    }

    @UiHandler("decreaseFivePrime")
    public void decreaseFivePrimePosition(ClickEvent e) {
        handleExonUpdates(0, -1);
    }

    @UiHandler("increaseFivePrime")
    public void increaseFivePrimePosition(ClickEvent e) {
        handleExonUpdates(0, 1);
    }

    @UiHandler("decreaseThreePrime")
    public void decreaseThreePrime(ClickEvent e) {
        handleExonUpdates(-1, 0);
    }

    @UiHandler("increaseThreePrime")
    public void increaseThreePrime(ClickEvent e) {
        handleExonUpdates(1, 0);
    }


    @UiHandler(value = {"fivePrimeField", "threePrimeField"})
    public void fivePrimeTextEntry(KeyDownEvent k) {
        if (k.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            handleExonUpdates();
        }
    }

    @UiHandler(value = {"fivePrimeField", "threePrimeField"})
    public void fivePrimeTextFocus(BlurEvent b) {
        handleExonUpdates();
    }


    private boolean verifyOperation() {
        if (!isEditableType(this.internalAnnotationInfo.getType())) {
            return false;
        }
        primeToCoordinates(this.fivePrimeValue, this.threePrimeValue);
        if (!(this.inputFmin < this.internalAnnotationInfo.getMax()) || !(this.inputFmax > this.internalAnnotationInfo.getMin())) {
            return false;
        }

        return true;
    }

    private void triggerUpdate(int fivePrimeValue, int threePrimeValue) {
        final AnnotationInfo originalInfo = this.internalAnnotationInfo;
        fivePrimeField.setText(Integer.toString(fivePrimeValue));
        this.internalAnnotationInfo.setMin(this.inputFmin);
        threePrimeField.setText(Integer.toString(threePrimeValue));
        this.internalAnnotationInfo.setMax(this.inputFmax);
        updateFeatureLocation(originalInfo);
    }

    private void primeToCoordinates(int fivePrimeFieldValue, int threePrimeFieldValue) {
        if (this.internalAnnotationInfo.getStrand() == 1) {
            this.inputFmin = fivePrimeFieldValue - 1;
            this.inputFmax = threePrimeFieldValue;
        } else {
            this.inputFmin = threePrimeFieldValue - 1;
            this.inputFmax = fivePrimeFieldValue;
        }
    }

    private void coordinatesToPrime(int fmin, int fmax) {
        if (this.internalAnnotationInfo.getStrand() == 1) {
            this.fivePrimeField.setText(Integer.toString(fmin + 1));
            this.threePrimeField.setText(Integer.toString(fmax));
        } else {
            this.fivePrimeField.setText(Integer.toString(fmax));
            this.threePrimeField.setText(Integer.toString(fmin + 1));
        }
    }

    private void getAnnotationInfoWithTopLevelFeature(AnnotationInfo annotationInfo) {
        this.annotationInfoWithTopLevelFeature = annotationInfo;
    }
}
