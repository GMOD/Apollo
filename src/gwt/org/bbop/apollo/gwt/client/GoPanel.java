package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.GoRestService;
import org.bbop.apollo.gwt.shared.go.*;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.bootbox.client.callback.ConfirmCallback;

import java.util.List;

/**
 * Created by ndunn on 1/9/15.
 */
public class GoPanel extends Composite {


    interface GoPanelUiBinder extends UiBinder<Widget, GoPanel> {
    }

    //    private GoAnnotation internalGoAnnotation;
    private static GoPanelUiBinder ourUiBinder = GWT.create(GoPanelUiBinder.class);

    //    @UiField
//    Container goEditContainer;
    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<GoAnnotation> dataGrid = new DataGrid<>(200, tablecss);
    @UiField
    TextBox referenceField;
    @UiField
    TextBox goTermField;
    @UiField
    ListBox evidenceCodeField;
    @UiField
    TextBox withField;
    @UiField
    Button deleteGoButton;
    @UiField
    Button newGoButton;
    @UiField
    Modal editGoModal;
    @UiField
    Button saveNewGoAnnotation;
    @UiField
    Button cancelNewGoAnnotation;
    @UiField
    Button editGoButton;
    @UiField
    FlexTable withEntriesFlexTable = new FlexTable();
    @UiField
    FlexTable referencesFlexTable = new FlexTable();
    @UiField
    Button addWithButton;
    @UiField
    Button addRefButton;
    @UiField
    org.gwtbootstrap3.client.ui.CheckBox notQualifierCheckBox;
    @UiField
    org.gwtbootstrap3.client.ui.CheckBox contributesToCheckBox;
    private static ListDataProvider<GoAnnotation> dataProvider = new ListDataProvider<>();
    private static List<GoAnnotation> annotationInfoList = dataProvider.getList();
    private SingleSelectionModel<GoAnnotation> selectionModel = new SingleSelectionModel<>();

    public GoPanel() {
        dataGrid.setWidth("100%");
        initializeTable();
        dataProvider.addDataDisplay(dataGrid);
        dataGrid.setSelectionModel(selectionModel);

        TextColumn<WithOrFrom> withOrFromTextColumn = new TextColumn<WithOrFrom>() {
            @Override
            public String getValue(WithOrFrom withOrFrom) {
                return withOrFrom.getDisplay();
            }
        };


        initWidget(ourUiBinder.createAndBindUi(this));

        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                handleSelection();
            }
        });

        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if (selectionModel.getSelectedObject() != null) {
                    deleteGoButton.setEnabled(true);
                    editGoButton.setEnabled(true);
                } else {
                    deleteGoButton.setEnabled(false);
                    editGoButton.setEnabled(false);
                }
            }
        });


        dataGrid.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                editGoModal.show();
            }
        }, DoubleClickEvent.getType());


//        List<EvidenceCode> evidenceItems = evidenceCodeField.get();
        evidenceCodeField.clear();
        evidenceCodeField.addItem("");
        for (EvidenceCode evidenceCode : EvidenceCode.values()) {
//            evidenceCodeField.getItems().set
            evidenceCodeField.addItem(evidenceCode.name());
        }

        addFakeData(50);
        redraw();

    }

    private class RemoveTableEntryButton extends Button {

        private final FlexTable parentTable;

        RemoveTableEntryButton(final String removeField, final FlexTable parent) {
            super("X");
            this.parentTable = parent;

            this.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    int foundRow = findEntryRow(removeField);
                    parentTable.removeRow(foundRow);
                }
            });
        }

        private int findEntryRow(String entry) {
            for (int i = 0; i < this.parentTable.getRowCount(); i++) {
                if (parentTable.getHTML(i, 0).equals(entry)) {
                    return i;
                }
            }
            return -1;
        }


    }

    private void addWithSelection(String with) {
        withEntriesFlexTable.insertRow(0);
        withEntriesFlexTable.setHTML(0, 0, with);
        withEntriesFlexTable.setWidget(0, 1, new RemoveTableEntryButton(with, withEntriesFlexTable));
    }

    private void addReferenceSelection(String referenceString) {
        referencesFlexTable.insertRow(0);
        referencesFlexTable.setHTML(0, 0, referenceString);
        referencesFlexTable.setWidget(0, 1, new RemoveTableEntryButton(referenceString, referencesFlexTable));
    }

    private void handleSelection() {
        if (selectionModel.getSelectedSet().isEmpty()) {
            goTermField.setText("");
            withField.setText("");
            referenceField.setText("");
            notQualifierCheckBox.setValue(false);
            contributesToCheckBox.setValue(false);
//            evidenceCodeField.setText("");
//                    goEditContainer.setVisible(false);
        } else {
            GoAnnotation selectedGoAnnotation = selectionModel.getSelectedObject();
            goTermField.setText(selectedGoAnnotation.getGoTerm().getName());
            int withRow = 0;
            for (WithOrFrom withOrFrom : selectedGoAnnotation.getWithOrFromList()) {
                addWithSelection(withOrFrom.getDisplay());
//                withEntriesFlexTable.setHTML(withRow,0,withOrFrom.getDisplay());
//                withEntriesFlexTable.setWidget(withRow,1,new Button("X"));
                ++withRow;
            }

            notQualifierCheckBox.setValue(selectedGoAnnotation.getQualifierList().contains(Qualifier.NOT));
            contributesToCheckBox.setValue(selectedGoAnnotation.getQualifierList().contains(Qualifier.CONTRIBUTES_TO));

            withField.setText("");

            for (Reference reference : selectedGoAnnotation.getReferenceList()) {
                addReferenceSelection(reference.getReferenceString());
            }
            referenceField.setText("");

            String evidenceName = selectedGoAnnotation.getEvidenceCode().name();
            for (int i = 0; i < evidenceCodeField.getItemCount(); i++) {
                if (evidenceCodeField.getItemText(i).equals(evidenceName)) {
                    evidenceCodeField.setSelectedIndex(i);
                    break;
                }
            }
        }

    }

    public void redraw() {
        dataGrid.redraw();
    }

    private void addFakeData(int amountOfData) {
        annotationInfoList.clear();
        for (int i = 0; i < amountOfData; i++) {
            GoAnnotation goAnnotation = new GoAnnotation();
            goAnnotation.setGoTerm(new GoTerm("GO:12312", "green blood"));
            goAnnotation.setEvidenceCode(EvidenceCode.IEA);
            goAnnotation.addQualifier(Qualifier.NOT);
            goAnnotation.addWithOrFrom(new WithOrFrom("UniProtKB-KW:KW-0067"));
            goAnnotation.addWithOrFrom(new WithOrFrom("InterPro:IPR000719"));
            goAnnotation.addReference(new Reference("PMID:21873635"));
            goAnnotation.addReference(new Reference("GO_REF:0000002"));
            annotationInfoList.add(goAnnotation);
        }
        GWT.log("fake data size: " + annotationInfoList.size());
    }

    @UiHandler("newGoButton")
    public void newGoAnnotation(ClickEvent e) {
        selectionModel.clear();
        editGoModal.show();
        withEntriesFlexTable.removeAllRows();
        referencesFlexTable.removeAllRows();
        evidenceCodeField.setSelectedIndex(0);
    }

    @UiHandler("editGoButton")
    public void editGoAnnotation(ClickEvent e) {
        editGoModal.show();
    }

    @UiHandler("addWithButton")
    public void addWith(ClickEvent e) {
        addWithSelection(withField.getText());
        withField.clear();
    }

    @UiHandler("addRefButton")
    public void addReference(ClickEvent e) {
        addReferenceSelection(referenceField.getText());
        referenceField.clear();
    }


    @UiHandler("saveNewGoAnnotation")
    public void saveNewGoAnnotationButton(ClickEvent e) {
        GoAnnotation goAnnotation = getEditedGoAnnotation();
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                Window.alert("Sucessfull save : TODO update model");
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Failed to save new go anntation");
            }
        };
        GoRestService.createGoAnnotation(requestCallback, goAnnotation);
        editGoModal.hide();
    }

    private GoAnnotation getEditedGoAnnotation() {
        GoAnnotation goAnnotation = new GoAnnotation();
//        goAnnotation.setGoTerm(goTermField.getText());
//        goAnnotation.setEvidenceCode(evidenceCodeField.getSelectedValue());
////        goAnnotation.setQualifierList(evidenceCodeField.getSelectedValue());
//        goAnnotation.setWithOrFromList(getWithList());
//        goAnnotation.setReferenceList(getReferencesValues());


        return goAnnotation;
    }

    @UiHandler("cancelNewGoAnnotation")
    public void cancelNewGoAnnotationButton(ClickEvent e) {
        editGoModal.hide();
    }

    @UiHandler("deleteGoButton")
    public void deleteGoAnnotation(ClickEvent e) {
        GoAnnotation goAnnotation = selectionModel.getSelectedObject();
        Bootbox.confirm("Remove GO Annotation: " + goAnnotation.getGoTerm().getName(), new ConfirmCallback() {
            @Override
            public void callback(boolean result) {
                Window.alert("removed: " + result);
                GoAnnotation goAnnotation = getEditedGoAnnotation();
                RequestCallback requestCallback = new RequestCallback() {
                    @Override
                    public void onResponseReceived(Request request, Response response) {
                        Window.alert("Sucessfull DELETE: TODO update model");
                    }

                    @Override
                    public void onError(Request request, Throwable exception) {
                        Bootbox.alert("Failed to DELETE new go anntation");
                    }
                };
                GoRestService.deleteGoAnnotation(requestCallback, goAnnotation);
            }
        });
    }


    private void initializeTable() {
        // TODO: probably want a link here
        TextColumn<GoAnnotation> goTermColumn = new TextColumn<GoAnnotation>() {
            @Override
            public String getValue(GoAnnotation annotationInfo) {
                String returnValue = annotationInfo.getGoTerm().getName();
                for (Qualifier qualifier : annotationInfo.getQualifierList()) {
                    returnValue += " ("+  qualifier.name().toLowerCase() +")" ;
                }

                return returnValue;
            }
        };
        goTermColumn.setSortable(true);

        TextColumn<GoAnnotation> withColumn = new TextColumn<GoAnnotation>() {
            @Override
            public String getValue(GoAnnotation annotationInfo) {
                return annotationInfo.getWithOrFromString();
            }
        };
        withColumn.setSortable(true);

        TextColumn<GoAnnotation> referenceColumn = new TextColumn<GoAnnotation>() {
            @Override
            public String getValue(GoAnnotation annotationInfo) {
                return annotationInfo.getReferenceString();
            }
        };
        referenceColumn.setSortable(true);

        TextColumn<GoAnnotation> evidenceColumn = new TextColumn<GoAnnotation>() {
            @Override
            public String getValue(GoAnnotation annotationInfo) {
                return annotationInfo.getEvidenceCode().name();
            }
        };
        evidenceColumn.setSortable(true);


        dataGrid.addColumn(goTermColumn, "Name");
        dataGrid.addColumn(evidenceColumn, "Evidence");
        dataGrid.addColumn(withColumn, "Based On");
        dataGrid.addColumn(referenceColumn, "Reference");

        dataGrid.setColumnWidth(0, "70px");
        dataGrid.setColumnWidth(1, "30px");
        dataGrid.setColumnWidth(2, "90px");
        dataGrid.setColumnWidth(3, "90px");

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
        addFakeData(50);
//        if(selectedAnnotationInfo==null){
//            dataProvider.setList(new ArrayList<GoAnnotation>());
//        }
//        else{
//            dataProvider.setList(selectedAnnotationInfo.getGoAnnotations());
//        }
    }

}
