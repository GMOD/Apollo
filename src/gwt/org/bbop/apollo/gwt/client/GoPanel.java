package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.shared.go.*;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Container;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.TextBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 1/9/15.
 */
public class GoPanel extends Composite {


    interface GoPanelUiBinder extends UiBinder<Widget, GoPanel> {
    }

    private GoAnnotation internalGoAnnotation;
    private static GoPanelUiBinder ourUiBinder = GWT.create(GoPanelUiBinder.class);

    @UiField
    Container goEditContainer;
    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<GoAnnotation> dataGrid = new DataGrid<>(200, tablecss);
    @UiField
    TextBox referenceField;
    @UiField
    TextBox goTermField;
    @UiField
    TextBox evidenceCodeField;
    @UiField
    TextBox withField;
    @UiField
    Button deleteGoButton;
    @UiField
    Button newGoButton;
    @UiField
    Modal editGoModal;
    //    @UiField
//    HTML notePanel;
    private static ListDataProvider<GoAnnotation> dataProvider = new ListDataProvider<>();
    private static List<GoAnnotation> annotationInfoList = dataProvider.getList();
    private SingleSelectionModel<GoAnnotation> selectionModel = new SingleSelectionModel<>();

    // TODO: probably want a link here
    private TextColumn<GoAnnotation> goTermColumn;
    private TextColumn<GoAnnotation> evidenceColumn;
    private TextColumn<GoAnnotation> withColumn;
    private TextColumn<GoAnnotation> referenceColumn;

    public GoPanel() {
        dataGrid.setWidth("100%");
        initializeTable();
        dataProvider.addDataDisplay(dataGrid);
        dataGrid.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if (selectionModel.getSelectedSet().isEmpty()) {
                    goTermField.setText("");
                    withField.setText("");
                    referenceField.setText("");
                    evidenceCodeField.setText("");
                    goEditContainer.setVisible(false);
                } else {
                    goTermField.setText(selectionModel.getSelectedObject().getGoTerm().getName());
                    withField.setText(selectionModel.getSelectedObject().getWithOrFromString());
                    referenceField.setText(selectionModel.getSelectedObject().getReferenceString());

                    evidenceCodeField.setText(selectionModel.getSelectedObject().getEvidenceCode().name());
                    goEditContainer.setVisible(true);
                }
            }
        });


        initWidget(ourUiBinder.createAndBindUi(this));

        addFakeData();
        redraw();

    }

    public void redraw() {
        dataGrid.redraw();
    }

    private void addFakeData() {

        GoAnnotation goAnnotation = new GoAnnotation();
        goAnnotation.setGoTerm(new GoTerm("GO:12312"));
        goAnnotation.setEvidenceCode(EvidenceCode.IEA);
        goAnnotation.addQualifier(Qualifier.NOT);
        goAnnotation.addWithOrFrom(new WithOrFrom("UniProtKB-KW:KW-0067"));
        goAnnotation.addWithOrFrom(new WithOrFrom("InterPro:IPR000719"));
        goAnnotation.addReference(new Reference("PMID:21873635"));
        goAnnotation.addReference(new Reference("GO_REF:0000002"));

        annotationInfoList.add(goAnnotation);
    }

    private void initializeTable() {
        goTermColumn = new TextColumn<GoAnnotation>() {
            @Override
            public String getValue(GoAnnotation annotationInfo) {
                return annotationInfo.getGoTerm().getName();
            }
        };
        goTermColumn.setSortable(true);

        withColumn = new TextColumn<GoAnnotation>(){
            @Override
            public String getValue(GoAnnotation annotationInfo) {
                return annotationInfo.getWithOrFromString();
            }
        };
        withColumn.setSortable(true);

        referenceColumn = new TextColumn<GoAnnotation>(){
            @Override
            public String getValue(GoAnnotation annotationInfo) {
                return annotationInfo.getReferenceString();
            }
        };
        referenceColumn.setSortable(true);

        evidenceColumn = new TextColumn<GoAnnotation>(){
            @Override
            public String getValue(GoAnnotation annotationInfo) {
                return annotationInfo.getEvidenceCode().name();
            }
        };
        evidenceColumn.setSortable(true);



        dataGrid.addColumn(goTermColumn, "Name");
        dataGrid.addColumn(withColumn, "With");
        dataGrid.addColumn(referenceColumn, "Ref");
        dataGrid.addColumn(evidenceColumn, "Evidence");

//        ColumnSortEvent.ListHandler<GoAnnotation> sortHandler = new ColumnSortEvent.ListHandler<GoAnnotation>(annotationInfoList);
//        dataGrid.addColumnSortHandler(sortHandler);

//        sortHandler.setComparator(goTermColumn, new Comparator<GoAnnotation>() {
//            @Override
//            public int compare(GoAnnotation o1, GoAnnotation o2) {
//                return o1.getType().compareTo(o2.getType());
//            }
//        });
//
//        sortHandler.setComparator(withColumn, new Comparator<GoAnnotation>() {
//            @Override
//            public int compare(GoAnnotation o1, GoAnnotation o2) {
//                return o1.getMin() - o2.getMin();
//            }
//        });
//
//        sortHandler.setComparator(referenceColumn, new Comparator<GoAnnotation>() {
//            @Override
//            public int compare(GoAnnotation o1, GoAnnotation o2) {
//                return o1.getMax() - o2.getMax();
//            }
//        });
//
//        sortHandler.setComparator(lengthColumn, new Comparator<GoAnnotation>() {
//            @Override
//            public int compare(GoAnnotation o1, GoAnnotation o2) {
//                return o1.getLength() - o2.getLength();
//            }
//        });
    }

    public void updateData() {
        updateData(null);
    }

    public void updateData(AnnotationInfo selectedAnnotationInfo) {
        if(selectedAnnotationInfo==null){
            dataProvider.setList(new ArrayList<GoAnnotation>());
        }
        else{
            dataProvider.setList(selectedAnnotationInfo.getGoAnnotations());
        }
    }

}
