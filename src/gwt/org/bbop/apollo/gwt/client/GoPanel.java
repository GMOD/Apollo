package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.oracles.BiolinkOntologyOracle;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.GoRestService;
import org.bbop.apollo.gwt.shared.go.*;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.SuggestBox;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.bootbox.client.callback.ConfirmCallback;

import java.util.ArrayList;
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
    @UiField(provided = true)
    SuggestBox goTermField;
    @UiField(provided = true)
    SuggestBox geneProductRelationshipField;
    @UiField(provided = true)
    SuggestBox evidenceCodeField;
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
    Anchor goTermLink;
    @UiField
    Anchor geneProductRelationshipLink;
    @UiField
    Anchor evidenceCodeLink;
    private static ListDataProvider<GoAnnotation> dataProvider = new ListDataProvider<>();
    private static List<GoAnnotation> annotationInfoList = dataProvider.getList();
    private SingleSelectionModel<GoAnnotation> selectionModel = new SingleSelectionModel<>();

    private AnnotationInfo annotationInfo ;

    public GoPanel() {
        goTermField = new SuggestBox(new BiolinkOntologyOracle("GO"));
        geneProductRelationshipField = new SuggestBox(new BiolinkOntologyOracle("RO"));
        evidenceCodeField = new SuggestBox(new BiolinkOntologyOracle("ECO"));
        dataGrid.setWidth("100%");
        initializeTable();
        dataProvider.addDataDisplay(dataGrid);
        dataGrid.setSelectionModel(selectionModel);

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


        goTermField.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {
            @Override
            public void onSelection(SelectionEvent<SuggestOracle.Suggestion> event) {
                SuggestOracle.Suggestion suggestion = event.getSelectedItem();
                goTermLink.setHTML(suggestion.getDisplayString());
                goTermLink.setHref("http://amigo.geneontology.org/amigo/term/"+suggestion.getReplacementString());
            }
        });

        evidenceCodeField.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {
            @Override
            public void onSelection(SelectionEvent<SuggestOracle.Suggestion> event) {
                SuggestOracle.Suggestion suggestion = event.getSelectedItem();
                evidenceCodeLink.setHTML(suggestion.getDisplayString());
                evidenceCodeLink.setHref("http://www.evidenceontology.org/term/"+suggestion.getReplacementString());
            }
        });

        geneProductRelationshipField.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {
            @Override
            public void onSelection(SelectionEvent<SuggestOracle.Suggestion> event) {
                SuggestOracle.Suggestion suggestion = event.getSelectedItem();
                geneProductRelationshipLink.setHTML(suggestion.getDisplayString());
                geneProductRelationshipLink.setHref("http://purl.obolibrary.org/obo/"+suggestion.getReplacementString().replace(":","_"));
            }
        });

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

    private void clearModal() {
        goTermField.setText("");
        goTermLink.setText("");
        geneProductRelationshipField.setText("");
        geneProductRelationshipLink.setText("");
        withField.setText("");
        withEntriesFlexTable.removeAllRows();
        referenceField.setText("");
        referencesFlexTable.removeAllRows();
        notQualifierCheckBox.setValue(false);
    }

    private void handleSelection() {
        if (selectionModel.getSelectedSet().isEmpty()) {
            clearModal();
        } else {
            GoAnnotation selectedGoAnnotation = selectionModel.getSelectedObject();
            goTermField.setText(selectedGoAnnotation.getGoTerm());
//            int withRow = 0;
            for (WithOrFrom withOrFrom : selectedGoAnnotation.getWithOrFromList()) {
                addWithSelection(withOrFrom.getDisplay());
//                ++withRow;
            }

            evidenceCodeField.setText(selectedGoAnnotation.getEvidenceCode());

            notQualifierCheckBox.setValue(selectedGoAnnotation.isNegate());

            withField.setText("");

            for (Reference reference : selectedGoAnnotation.getReferenceList()) {
                addReferenceSelection(reference.getReferenceString());
            }
            referenceField.setText("");

        }

    }

    public void redraw() {
        dataGrid.redraw();
    }

    private void addFakeData(int amountOfData) {
        annotationInfoList.clear();
        for (int i = 0; i < amountOfData; i++) {
            GoAnnotation goAnnotation = new GoAnnotation();
            goAnnotation.setGoTerm("GO:12312");
//            goAnnotation.setEvidenceCode(EvidenceCode.IEA);
            goAnnotation.setNegate(true);
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
        withEntriesFlexTable.removeAllRows();
        referencesFlexTable.removeAllRows();
        selectionModel.clear();
        editGoModal.show();
//        evidenceCodeField.setText("");
//        evidenceCodeField.setSelectedIndex(0);
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
                Window.alert("Sucessfull save : TODO update model: " + response.getText());
                clearModal();
            }


            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Failed to save new go annotation: " + exception.getMessage());
            }
        };
        GoRestService.saveGoAnnotation(requestCallback, goAnnotation);
        editGoModal.hide();
    }

    private GoAnnotation getEditedGoAnnotation() {
        GoAnnotation goAnnotation = new GoAnnotation();
        goAnnotation.setGene(annotationInfo.getUniqueName());
        goAnnotation.setGoTerm(goTermField.getText());
        goAnnotation.setGeneRelationship(geneProductRelationshipField.getText());
        goAnnotation.setEvidenceCode(evidenceCodeField.getText());
        goAnnotation.setNegate(notQualifierCheckBox.getValue());
        goAnnotation.setWithOrFromList(getWithList());
        goAnnotation.setReferenceList(getReferenceList());
        return goAnnotation;
    }

    private List<WithOrFrom> getWithList() {
        List<WithOrFrom> withOrFromList = new ArrayList<>();
        for(int i = 0 ; i < withEntriesFlexTable.getRowCount() ; i++){
            withOrFromList.add(new WithOrFrom(withEntriesFlexTable.getHTML(i,0)));
        }
        return  withOrFromList ;
    }

    private List<Reference> getReferenceList() {
        List<Reference> referenceList = new ArrayList<>();
        for(int i = 0 ; i < referencesFlexTable.getRowCount() ; i++){
            referenceList.add(new Reference(referencesFlexTable.getHTML(i,0)));
        }
        return  referenceList ;
    }

    @UiHandler("cancelNewGoAnnotation")
    public void cancelNewGoAnnotationButton(ClickEvent e) {
        editGoModal.hide();
    }

    @UiHandler("deleteGoButton")
    public void deleteGoAnnotation(ClickEvent e) {
        GoAnnotation goAnnotation = selectionModel.getSelectedObject();
        Bootbox.confirm("Remove GO Annotation: " + goAnnotation.getGoTerm(), new ConfirmCallback() {
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
                String returnValue = annotationInfo.getGoTerm();
                if(annotationInfo.isNegate()){
                    returnValue += " (not) ";
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
                return annotationInfo.getEvidenceCode();
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
//        updateData(null);
    }

    public void updateData(AnnotationInfo selectedAnnotationInfo) {
        this.annotationInfo = selectedAnnotationInfo ;

//        addFakeData(50);
//        if(selectedAnnotationInfo==null){
//            dataProvider.setList(new ArrayList<GoAnnotation>());
//        }
//        else{
//            dataProvider.setList(selectedAnnotationInfo.getGoAnnotations());
//        }
    }

}
