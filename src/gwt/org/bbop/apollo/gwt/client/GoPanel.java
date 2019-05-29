package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.*;
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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.GoAnnotationConverter;
import org.bbop.apollo.gwt.client.oracles.BiolinkOntologyOracle;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.GoRestService;
import org.bbop.apollo.gwt.shared.go.Aspect;
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
    private final String RO_BASE = "http://www.ontobee.org/ontology/RO?iri=http://purl.obolibrary.org/obo/";

    interface GoPanelUiBinder extends UiBinder<Widget, GoPanel> {
    }

    private static GoPanelUiBinder ourUiBinder = GWT.create(GoPanelUiBinder.class);

    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<GoAnnotation> dataGrid = new DataGrid<>(200, tablecss);
    @UiField
    TextBox noteField;
    @UiField(provided = true)
    SuggestBox goTermField;
//    @UiField(provided = true)
    @UiField
    org.gwtbootstrap3.client.ui.ListBox geneProductRelationshipField;
    @UiField(provided = true)
    SuggestBox evidenceCodeField;
    @UiField
    TextBox withFieldPrefix;
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
    FlexTable notesFlexTable = new FlexTable();
    @UiField
    Button addWithButton;
    @UiField
    Button addNoteButton;
    @UiField
    org.gwtbootstrap3.client.ui.CheckBox notQualifierCheckBox;
    @UiField
    Anchor goTermLink;
    @UiField
    Anchor geneProductRelationshipLink;
    @UiField
    Anchor evidenceCodeLink;
    @UiField
    TextBox referenceFieldPrefix;
    @UiField
    FlexTable annotationsFlexTable;
//    @UiField
//    Button addExtensionButton;
//    @UiField
//    TextBox annotationsField;
    @UiField
    TextBox withFieldId;
    @UiField
    TextBox referenceFieldId;
//    @UiField
//    Button referenceValidateButton;
    @UiField
    HTML goAnnotationTitle;
    @UiField
    org.gwtbootstrap3.client.ui.ListBox aspectField;
    @UiField
    HTML aspectLabel;

    private static ListDataProvider<GoAnnotation> dataProvider = new ListDataProvider<>();
    private static List<GoAnnotation> annotationInfoList = dataProvider.getList();
    private SingleSelectionModel<GoAnnotation> selectionModel = new SingleSelectionModel<>();

    private AnnotationInfo annotationInfo;

    public GoPanel() {

        initLookups();
        dataGrid.setWidth("100%");
        initializeTable();
        dataProvider.addDataDisplay(dataGrid);
        dataGrid.setSelectionModel(selectionModel);

        initWidget(ourUiBinder.createAndBindUi(this));

        for(Aspect aspect : Aspect.values()){
            aspectField.addItem(aspect.name(),aspect.getLookup());
        }

        aspectField.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                goTermField.setText("");
                goTermLink.setText("");
                aspectLabel.setText(aspectField.getSelectedValue());

                // TODO: set constrainted RO values
                setRelationValues(aspectField.getSelectedItemText());
            }
        });

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
                goAnnotationTitle.setText("Edit GO Annotation for "+AnnotatorPanel.selectedAnnotationInfo.getName());
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

        geneProductRelationshipField.addChangeHandler(new ChangeHandler() {

            @Override
            public void onChange(ChangeEvent event) {
                String selectedItemText  = geneProductRelationshipField.getSelectedItemText();
                geneProductRelationshipLink.setHTML(selectedItemText);
                geneProductRelationshipLink.setHref(RO_BASE + selectedItemText.replace(":", "_"));
            }
        });

        redraw();
    }

    private void setRelationValues(String selectedItemText) {
        Aspect aspect = Aspect.valueOf(selectedItemText);
        geneProductRelationshipField.clear();
        switch(aspect.getLookup()){
            case Aspect.BP.getLookup():
                geneProductRelationshipField.addItem("RO:0002331","involved in");
                geneProductRelationshipField.addItem("RO:0002263","acts upstream of");
                geneProductRelationshipField.addItem("RO:0004034","acts upstream of positive effect");
                geneProductRelationshipField.addItem("RO:0004035","acts upstream of negative effect");
                geneProductRelationshipField.addItem("RO:0002264","acts upstream of or within");
                geneProductRelationshipField.addItem("RO:0004032","acts upstream of or within positive effect");
                geneProductRelationshipField.addItem("RO:0004033","acts upstream of or within negative effect");
                break;
            case Aspect.MF.getLookup():
                geneProductRelationshipField.addItem("RO:0002327","enables");
                geneProductRelationshipField.addItem("RO:0002326","contributes to");
                break;
            case Aspect.CC.getLookup():
                geneProductRelationshipField.addItem("BFO:0000050","part of");
                geneProductRelationshipField.addItem("RO:0002325","colocalizes with");
                geneProductRelationshipField.addItem("RO:0002432","is active in");
                break;
            default:
                Bootbox.alert("A problem has occurred");


        }

    }

    private void initLookups() {
        goTermField = new SuggestBox(new BiolinkOntologyOracle("GO"));

//        BiolinkOntologyOracle roLookup = new BiolinkOntologyOracle("RO");
//        roLookup.addPreferredSuggestion("enables", "http://purl.obolibrary.org/obo/RO_0002327", "RO:0002327");
//        roLookup.addPreferredSuggestion("involved in", "http://purl.obolibrary.org/obo/RO_0002331", "RO:0002331");
//        roLookup.addPreferredSuggestion("part of", "http://purl.obolibrary.org/obo/BFO_0000050", "BFO:0000050");
//        roLookup.addPreferredSuggestion("enabled by", "http://purl.obolibrary.org/obo/RO_0002333", "RO:0002333");
//        roLookup.addPreferredSuggestion("occurs in", "http://purl.obolibrary.org/obo/BFO_0000066", "BFO:0000066");
//        roLookup.addPreferredSuggestion("causally upstream of or within", "http://purl.obolibrary.org/obo/RO_0002418", "RO:0002418");
//        roLookup.addPreferredSuggestion("has participant", "http://purl.obolibrary.org/obo/RO_0000057", "RO:0000057");
//        geneProductRelationshipField = new SuggestBox(roLookup);


        // most from here: http://geneontology.org/docs/guide-go-evidence-codes/
        BiolinkOntologyOracle ecoLookup = new BiolinkOntologyOracle("ECO");
        ecoLookup.addPreferredSuggestion("experimental evidence used in manual assertion (EXP)", "http://www.evidenceontology.org/term/ECO:0000269/", "ECO:0000269");
        ecoLookup.addPreferredSuggestion("direct assay evidence used in manual assertion (IDA)", "http://www.evidenceontology.org/term/ECO:0000314/", "ECO:0000314");
        ecoLookup.addPreferredSuggestion("physical interaction evidence used in manual assertion (IPI)", "http://www.evidenceontology.org/term/ECO:0000353/", "ECO:0000353");
        ecoLookup.addPreferredSuggestion("mutant phenotype evidence used in manual assertion (IMP)", "http://www.evidenceontology.org/term/ECO:0000315/", "ECO:0000315");
        ecoLookup.addPreferredSuggestion("genetic interaction evidence used in manual assertion (IGI)", "http://www.evidenceontology.org/term/ECO:0000316/", "ECO:0000316");
        ecoLookup.addPreferredSuggestion("biological aspect of ancestor evidence used in manual assertion (IBA)", "http://www.evidenceontology.org/term/ECO:0000318/", "ECO:0000318");
        ecoLookup.addPreferredSuggestion("biological aspect of descendant evidence used in manual assertion (IBD)", "http://www.evidenceontology.org/term/ECO:0000319/", "ECO:0000319");
        ecoLookup.addPreferredSuggestion("sequence similarity evidence used in manual assertion (ISS)", "http://www.evidenceontology.org/term/ECO:0000250/", "ECO:0000250");
        ecoLookup.addPreferredSuggestion("sequence orthology evidence used in manual assertion (ISO)", "http://www.evidenceontology.org/term/ECO:0000266/", "ECO:0000266");
        ecoLookup.addPreferredSuggestion("sequence alignment evidence used in manual assertion (ISA)", "http://www.evidenceontology.org/term/ECO:0000247/", "ECO:0000247");
        ecoLookup.addPreferredSuggestion("no biological data found used in manual assertion (ND)", "http://www.evidenceontology.org/term/ECO:0000307/", "ECO:0000307");
        ecoLookup.addPreferredSuggestion("curator inference used in manual assertion (IC)", "http://www.evidenceontology.org/term/ECO:0000305/", "ECO:0000305");
        ecoLookup.addPreferredSuggestion("evidence used in automatic assertion (IEA)", "http://www.evidenceontology.org/term/ECO:0000501/", "ECO:0000501");
        evidenceCodeField = new SuggestBox(ecoLookup);
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
    private void addWithSelection(WithOrFrom withOrFrom) {
        withEntriesFlexTable.insertRow(0);
        withEntriesFlexTable.setHTML(0, 0, withOrFrom.getDisplay());
        withEntriesFlexTable.setWidget(0, 1, new RemoveTableEntryButton(withOrFrom.getDisplay(), withEntriesFlexTable));
    }

    private void addWithSelection(String prefixWith, String idWith) {
        withEntriesFlexTable.insertRow(0);
        withEntriesFlexTable.setHTML(0, 0, prefixWith + ":" + idWith);
        withEntriesFlexTable.setWidget(0, 1, new RemoveTableEntryButton(prefixWith + ":" + idWith, withEntriesFlexTable));
    }

    private void addReferenceSelection(String referenceString) {
        notesFlexTable.insertRow(0);
        notesFlexTable.setHTML(0, 0, referenceString);
        notesFlexTable.setWidget(0, 1, new RemoveTableEntryButton(referenceString, notesFlexTable));
    }

    private void clearModal() {
        goTermField.setText("");
        goTermLink.setText("");
//        geneProductRelationshipField.setText("");
        geneProductRelationshipField.clear();
        geneProductRelationshipLink.setText("");
        evidenceCodeField.setText("");
        evidenceCodeLink.setText("");
        withFieldPrefix.setText("");
        withFieldId.setText("");
        withEntriesFlexTable.removeAllRows();
        noteField.setText("");
        notesFlexTable.removeAllRows();
        referenceFieldPrefix.setText("");
        referenceFieldId.setText("");
        notQualifierCheckBox.setValue(false);
    }

    private void handleSelection() {
        if (selectionModel.getSelectedSet().isEmpty()) {
            clearModal();
        } else {
            GoAnnotation selectedGoAnnotation = selectionModel.getSelectedObject();
            goTermField.setText(selectedGoAnnotation.getGoTerm());
            goTermLink.setHref(GO_BASE + selectedGoAnnotation.getGoTerm());
            GoRestService.lookupTerm(goTermLink,selectedGoAnnotation.getGoTerm());


//            geneProductRelationshipField.setText(selectedGoAnnotation.getGeneRelationship());
//            geneProductRelationshipField.setSelectedIndex(selectedGoAnnotation.getGeneRelationship());

            // TODO: set the proper one when it comes back
//            String selectedGeneRelationship = selectedGoAnnotation.getGeneRelationship();
//            for(int i = 0 ; i < geneProductRelationshipField.getItemCount() ; i++){
////                if(geneProductRelationshipField.setItemSelected(i,);)
//            }


            geneProductRelationshipLink.setHref(RO_BASE + selectedGoAnnotation.getGeneRelationship().replaceAll(":", "_"));
            GoRestService.lookupTerm(geneProductRelationshipLink,selectedGoAnnotation.getGeneRelationship());

            evidenceCodeField.setText(selectedGoAnnotation.getEvidenceCode());
            evidenceCodeLink.setHref(ECO_BASE + selectedGoAnnotation.getEvidenceCode());
            GoRestService.lookupTerm(evidenceCodeLink,selectedGoAnnotation.getEvidenceCode());

            notQualifierCheckBox.setValue(selectedGoAnnotation.isNegate());

            withEntriesFlexTable.removeAllRows();
            for (WithOrFrom withOrFrom : selectedGoAnnotation.getWithOrFromList()) {
                addWithSelection(withOrFrom);
            }
            withFieldPrefix.setText("");

            referenceFieldPrefix.setText(selectedGoAnnotation.getReference().getPrefix());
            referenceFieldId.setText(selectedGoAnnotation.getReference().getLookupId());

            notesFlexTable.removeAllRows();
            for (String noteString : selectedGoAnnotation.getNoteList()) {
                addReferenceSelection(noteString);
            }
            noteField.setText("");


        }

    }

    public void redraw() {
        dataGrid.redraw();
    }

    @UiHandler("newGoButton")
    public void newGoAnnotation(ClickEvent e) {
        goAnnotationTitle.setText("Add new GO Annotation to "+AnnotatorPanel.selectedAnnotationInfo.getName());
        withEntriesFlexTable.removeAllRows();
        notesFlexTable.removeAllRows();
        selectionModel.clear();
        editGoModal.show();
    }

    @UiHandler("editGoButton")
    public void editGoAnnotation(ClickEvent e) {
        editGoModal.show();
    }

    @UiHandler("addWithButton")
    public void addWith(ClickEvent e) {
        addWithSelection(withFieldPrefix.getText(), withFieldId.getText());
        withFieldPrefix.clear();
        withFieldId.clear();
    }

    @UiHandler("addNoteButton")
    public void addNote(ClickEvent e) {
        String noteText = noteField.getText();
        notesFlexTable.insertRow(0);
        notesFlexTable.setHTML(0, 0, noteText);
        notesFlexTable.setWidget(0, 1, new RemoveTableEntryButton(noteText, notesFlexTable));
        noteField.clear();
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

        if (goAnnotation.getReference().getReferenceString().length()==0 ) {
            validationErrors.add("You must provide at least one reference");
        }
        return validationErrors;
    }

    private GoAnnotation getEditedGoAnnotation() {
        GoAnnotation goAnnotation = new GoAnnotation();
        goAnnotation.setGene(annotationInfo.getUniqueName());
        goAnnotation.setGoTerm(goTermField.getText());
        goAnnotation.setGeneRelationship(geneProductRelationshipField.getSelectedItemText());
        goAnnotation.setEvidenceCode(evidenceCodeField.getText());
        goAnnotation.setNegate(notQualifierCheckBox.getValue());
        goAnnotation.setWithOrFromList(getWithList());
        Reference reference = new Reference(referenceFieldPrefix.getText(), referenceFieldId.getText());
        goAnnotation.setReference(reference);
        goAnnotation.setNoteList(getNoteList());
        return goAnnotation;
    }

    private List<WithOrFrom> getWithList() {
        List<WithOrFrom> withOrFromList = new ArrayList<>();
        for (int i = 0; i < withEntriesFlexTable.getRowCount(); i++) {
            withOrFromList.add(new WithOrFrom(withEntriesFlexTable.getHTML(i, 0)));
        }
        String withPrefixText = withFieldPrefix.getText();
        String withIdText = withFieldId.getText();
        if (withPrefixText.length() > 0 && withIdText.length() > 0) {
            withFieldPrefix.clear();
            withOrFromList.add(new WithOrFrom(withPrefixText, withIdText));
        }

        return withOrFromList;
    }

    private List<String> getNoteList() {
        List<String> noteList = new ArrayList<>();
        for (int i = 0; i < notesFlexTable.getRowCount(); i++) {
            noteList.add(notesFlexTable.getHTML(i, 0));
        }
        String noteFieldText = noteField.getText();
        if (noteFieldText.length() > 0) {
            noteField.clear();
            noteList.add(noteFieldText);
        }
        return noteList;
    }

//    @UiHandler("referenceValidateButton")
//    public void validateReference(ClickEvent clickEvent) {
//        GWT.log("not sure what to do here ");
//    }

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
                return annotationInfo.getReference().getReferenceString();
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
