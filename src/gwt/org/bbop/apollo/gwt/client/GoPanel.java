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
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.GoAnnotationConverter;
import org.bbop.apollo.gwt.client.oracles.BiolinkOntologyOracle;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.GoRestService;
import org.bbop.apollo.gwt.shared.go.GoAnnotation;
import org.bbop.apollo.gwt.shared.go.Reference;
import org.bbop.apollo.gwt.shared.go.WithOrFrom;
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

    private final String GO_BASE = "http://amigo.geneontology.org/amigo/term/";
    private final String ECO_BASE = "http://www.evidenceontology.org/term/";
    private final String RO_BASE = "http://purl.obolibrary.org/obo/";

    interface GoPanelUiBinder extends UiBinder<Widget, GoPanel> { }

    private static GoPanelUiBinder ourUiBinder = GWT.create(GoPanelUiBinder.class);

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

    private AnnotationInfo annotationInfo;

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
                goTermLink.setHref(GO_BASE + suggestion.getReplacementString());
            }
        });

        evidenceCodeField.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {
            @Override
            public void onSelection(SelectionEvent<SuggestOracle.Suggestion> event) {
                SuggestOracle.Suggestion suggestion = event.getSelectedItem();
                evidenceCodeLink.setHTML(suggestion.getDisplayString());
                evidenceCodeLink.setHref(ECO_BASE + suggestion.getReplacementString());
            }
        });

        geneProductRelationshipField.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {
            @Override
            public void onSelection(SelectionEvent<SuggestOracle.Suggestion> event) {
                SuggestOracle.Suggestion suggestion = event.getSelectedItem();
                geneProductRelationshipLink.setHTML(suggestion.getDisplayString());
                geneProductRelationshipLink.setHref(RO_BASE + suggestion.getReplacementString().replace(":", "_"));
            }
        });

        redraw();
    }

    private void loadData() {

        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject jsonObject = JSONParser.parseStrict(response.getText()).isObject();
                loadAnnotationsFromResponse(jsonObject);
                redraw();
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("A problem with request: " + request.toString() + " " + exception.getMessage());
            }
        };
        if (annotationInfo != null) {
            GoRestService.getGoAnnotation(requestCallback, annotationInfo.getUniqueName());
        }
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
        evidenceCodeField.setText("");
        evidenceCodeLink.setText("");
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
            goTermLink.setHref(GO_BASE+selectedGoAnnotation.getGoTerm());
            goTermLink.setHTML(selectedGoAnnotation.getGoTerm());
            geneProductRelationshipField.setText(selectedGoAnnotation.getGeneRelationship());
            geneProductRelationshipLink.setHref(RO_BASE+selectedGoAnnotation.getGeneRelationship());
            geneProductRelationshipLink.setHTML(selectedGoAnnotation.getGeneRelationship());
            goTermLink.setHTML(selectedGoAnnotation.getGoTerm());
            withEntriesFlexTable.removeAllRows();
            for (WithOrFrom withOrFrom : selectedGoAnnotation.getWithOrFromList()) {
                addWithSelection(withOrFrom.getDisplay());
            }

            evidenceCodeField.setText(selectedGoAnnotation.getEvidenceCode());
            evidenceCodeLink.setHref(ECO_BASE+selectedGoAnnotation.getEvidenceCode());
            evidenceCodeLink.setHTML(selectedGoAnnotation.getEvidenceCode());

            notQualifierCheckBox.setValue(selectedGoAnnotation.isNegate());

            withField.setText("");

            referencesFlexTable.removeAllRows();
            for (Reference reference : selectedGoAnnotation.getReferenceList()) {
                addReferenceSelection(reference.getReferenceString());
            }
            referenceField.setText("");

        }

    }

    public void redraw() {
        dataGrid.redraw();
    }

    @UiHandler("newGoButton")
    public void newGoAnnotation(ClickEvent e) {
        withEntriesFlexTable.removeAllRows();
        referencesFlexTable.removeAllRows();
        selectionModel.clear();
        editGoModal.show();
    }

    @UiHandler("editGoButton")
    public void editGoAnnotation(ClickEvent e) {
        editGoModal.show();
    }

    @UiHandler("addWithButton")
    public void addWith(ClickEvent e) {
        String withFieldString = withField.getText();
        if (!withFieldString.contains(":") || withFieldString.length() < 2) {
            Bootbox.alert("Invalid with/from value '" + withFieldString + "'");
            return;
        }
        addWithSelection(withField.getText());
        withField.clear();
    }

    @UiHandler("addRefButton")
    public void addReference(ClickEvent e) {
        String referenceFieldString = referenceField.getText();
        if (!referenceFieldString.contains(":") || referenceFieldString.length() < 2) {
            Bootbox.alert("Invalid reference value '" + referenceFieldString + "'");
            return;
        }
        addReferenceSelection(referenceFieldString);
        referenceField.clear();
    }

    /**
     * //                {
     * //                    "annotations":[{
     * //                    "geneRelationship":"RO:0002326", "goTerm":"GO:0031084", "references":"[\"ref:12312\"]", "gene":
     * //                    "1743ae6c-9a37-4a41-9b54-345065726d5f", "negate":false, "evidenceCode":"ECO:0000205", "withOrFrom":
     * //                    "[\"adf:12312\"]"
     * //                }]}
     *
     * @param inputObject
     */
    private void loadAnnotationsFromResponse(JSONObject inputObject) {

        JSONArray annotationsArray = inputObject.get("annotations").isArray();
        annotationInfoList.clear();
        for (int i = 0; i < annotationsArray.size(); i++) {
            GoAnnotation goAnnotationInstance = GoAnnotationConverter.convertFromJson(annotationsArray.get(i).isObject());
            annotationInfoList.add(goAnnotationInstance);
        }
    }


    @UiHandler("saveNewGoAnnotation")
    public void saveNewGoAnnotationButton(ClickEvent e) {
        GoAnnotation goAnnotation = getEditedGoAnnotation();
        GoAnnotation selectedGoAnnotation = selectionModel.getSelectedObject();
        if (selectedGoAnnotation != null) {
            goAnnotation.setId(selectedGoAnnotation.getId());
        }
        List<String> validationErrors = validateGoAnnotation(goAnnotation);
        if (validationErrors.size() > 0) {
            String errorString = "Invalid GO Annotation <br/>";
            for (String error : validationErrors) {
                errorString += "&bull; " + error + "<br/>";
            }
            Bootbox.alert(errorString);
            return;
        }
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject returnObject = JSONParser.parseStrict(response.getText()).isObject();
                loadAnnotationsFromResponse(returnObject);
                clearModal();
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Failed to save new go annotation: " + exception.getMessage());
            }
        };
        if (goAnnotation.getId() != null) {
            GoRestService.updateGoAnnotation(requestCallback, goAnnotation);
        } else {
            GoRestService.saveGoAnnotation(requestCallback, goAnnotation);
        }
        editGoModal.hide();
    }

    private List<String> validateGoAnnotation(GoAnnotation goAnnotation) {
        List<String> validationErrors = new ArrayList<>();
        if (goAnnotation.getGene() == null) {
            validationErrors.add("You must provide a gene name");
        }
        if (goAnnotation.getGoTerm() == null) {
            validationErrors.add("You must provide a GO term");
        }
        if (!goAnnotation.getGoTerm().contains(":")) {
            validationErrors.add("You must provide a prefix and suffix for the GO term");
        }
        if (goAnnotation.getEvidenceCode() == null) {
            validationErrors.add("You must provide an ECO evidence code ");
        }
        if (!goAnnotation.getEvidenceCode().contains(":")) {
            validationErrors.add("You must provide a prefix and suffix for the ECO evidence code");
        }
        if (goAnnotation.getGeneRelationship() == null) {
            validationErrors.add("You must provide a Gene Relationship");
        }
        if (!goAnnotation.getGeneRelationship().contains(":")) {
            validationErrors.add("You must provide a prefix and suffix for the Gene Relationship");
        }

        if (goAnnotation.getReferenceList().size() == 0) {
            validationErrors.add("You must provide at least one reference");
        }
//        for (Reference reference : goAnnotation.getReferenceList()) {
//
//            assert reference.getReferenceString() != null && reference.getReferenceString().contains(":");
//        }
//        for (WithOrFrom withOrFrom : goAnnotation.getWithOrFromList()) {
//            assert withOrFrom.getDisplay() != null && withOrFrom.getDisplay().contains(":");
//        }
        return validationErrors;
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
        for (int i = 0; i < withEntriesFlexTable.getRowCount(); i++) {
            withOrFromList.add(new WithOrFrom(withEntriesFlexTable.getHTML(i, 0)));
        }
        String withString = withField.getText();
        if (withString.length() > 0) {
            withField.clear();
            withOrFromList.add(new WithOrFrom(withString));
        }

        return withOrFromList;
    }

    private List<Reference> getReferenceList() {
        List<Reference> referenceList = new ArrayList<>();
        for (int i = 0; i < referencesFlexTable.getRowCount(); i++) {
            referenceList.add(new Reference(referencesFlexTable.getHTML(i, 0)));
        }
        String referenceString = referenceField.getText();
        if (referenceString.length() > 0) {
            referenceField.clear();
            referenceList.add(new Reference(referenceString));
        }
        return referenceList;
    }

    @UiHandler("cancelNewGoAnnotation")
    public void cancelNewGoAnnotationButton(ClickEvent e) {
        editGoModal.hide();
    }

    @UiHandler("deleteGoButton")
    public void deleteGoAnnotation(ClickEvent e) {
        final GoAnnotation goAnnotation = selectionModel.getSelectedObject();
        Bootbox.confirm("Remove GO Annotation: " + goAnnotation.getGoTerm(), new ConfirmCallback() {
            @Override
            public void callback(boolean result) {
                RequestCallback requestCallback = new RequestCallback() {
                    @Override
                    public void onResponseReceived(Request request, Response response) {
                        JSONObject jsonObject = JSONParser.parseStrict(response.getText()).isObject();
                        loadAnnotationsFromResponse(jsonObject);
                        redraw();
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
                if (annotationInfo.isNegate()) {
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

    }

    public void updateData() {
        updateData(null);
    }

    public void updateData(AnnotationInfo selectedAnnotationInfo) {
        this.annotationInfo = selectedAnnotationInfo;
        loadData();
    }

}
