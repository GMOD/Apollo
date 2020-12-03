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
import org.bbop.apollo.gwt.client.dto.ProvenanceConverter;
import org.bbop.apollo.gwt.client.oracles.BiolinkLookup;
import org.bbop.apollo.gwt.client.oracles.BiolinkOntologyOracle;
import org.bbop.apollo.gwt.client.oracles.BiolinkSuggestBox;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.ProvenanceRestService;
import org.bbop.apollo.gwt.shared.provenance.Provenance;
import org.bbop.apollo.gwt.shared.provenance.ProvenanceField;
import org.bbop.apollo.gwt.shared.provenance.Reference;
import org.bbop.apollo.gwt.shared.provenance.WithOrFrom;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.bootbox.client.callback.ConfirmCallback;

import java.util.ArrayList;
import java.util.List;

import static org.bbop.apollo.gwt.client.oracles.BiolinkOntologyOracle.ECO_BASE;

/**
 * Created by ndunn on 1/9/15.
 */
public class ProvenancePanel extends Composite {

  interface ProvenancePanelUiBinder extends UiBinder<Widget, ProvenancePanel> {
  }

  private static ProvenancePanelUiBinder ourUiBinder = GWT.create(ProvenancePanelUiBinder.class);

  DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
  @UiField(provided = true)
  DataGrid<Provenance> dataGrid = new DataGrid<>(200, tablecss);
  @UiField
  TextBox noteField;
  @UiField
  ListBox provenanceField;
  @UiField(provided = true)
  BiolinkSuggestBox evidenceCodeField;
  @UiField
  TextBox withFieldPrefix;
  @UiField
  Button deleteGoButton;
  @UiField
  Button newGoButton;
  @UiField
  Modal provenanceModal;
  @UiField
  Button saveNewProvenance;
  @UiField
  Button cancelNewProvenance;
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
  Anchor evidenceCodeLink;
  @UiField
  TextBox referenceFieldPrefix;
  @UiField
  FlexTable annotationsFlexTable;
  @UiField
  TextBox withFieldId;
  @UiField
  TextBox referenceFieldId;
  //    @UiField
//    Button referenceValidateButton;
  @UiField
  HTML provenanceTitle;
  @UiField
  org.gwtbootstrap3.client.ui.CheckBox allEcoCheckBox;
  @UiField
  Anchor helpLink;

  private static ListDataProvider<Provenance> dataProvider = new ListDataProvider<>();
  private static List<Provenance> annotationInfoList = dataProvider.getList();
  private SingleSelectionModel<Provenance> selectionModel = new SingleSelectionModel<>();
  private BiolinkOntologyOracle ecoLookup = new BiolinkOntologyOracle(BiolinkLookup.ECO);
  private Boolean editable  = false ;

  private AnnotationInfo annotationInfo;

  public ProvenancePanel() {

    initLookups();
    dataGrid.setWidth("100%");
    initializeTable();
    dataProvider.addDataDisplay(dataGrid);
    dataGrid.setSelectionModel(selectionModel);

    initWidget(ourUiBinder.createAndBindUi(this));
    initFields();
    allEcoCheckBox(null);


    selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      @Override
      public void onSelectionChange(SelectionChangeEvent event) {
        handleSelection();
      }
    });

    selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      @Override
      public void onSelectionChange(SelectionChangeEvent event) {
        if (selectionModel.getSelectedObject() != null && editable) {
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
        if(editable) {
          provenanceTitle.setText("Edit Annotations for " + AnnotatorPanel.selectedAnnotationInfo.getName());
          handleSelection();
          provenanceModal.show();
        }
      }
    }, DoubleClickEvent.getType());


    evidenceCodeField.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {
      @Override
      public void onSelection(SelectionEvent<SuggestOracle.Suggestion> event) {
        SuggestOracle.Suggestion suggestion = event.getSelectedItem();
        evidenceCodeLink.setHTML(suggestion.getDisplayString());
        evidenceCodeLink.setHref(ECO_BASE + suggestion.getReplacementString()+"/");
      }
    });

    provenanceField.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        String selectedItemText = provenanceField.getSelectedItemText();
      }
    });

    redraw();
  }

  private void enableFields(boolean enabled) {
    saveNewProvenance.setEnabled(enabled);
    evidenceCodeField.setEnabled(enabled);
    provenanceField.setEnabled(enabled);
    referenceFieldPrefix.setEnabled(enabled);
    referenceFieldId.setEnabled(enabled);
    withFieldPrefix.setEnabled(enabled);
    withFieldId.setEnabled(enabled);
    noteField.setEnabled(enabled);
  }

  private void initFields() {
    for( ProvenanceField field : ProvenanceField.values()){
        provenanceField.addItem(field.name());
    }
  }

  private void initLookups() {
    evidenceCodeField = new BiolinkSuggestBox(ecoLookup);
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
      ProvenanceRestService.getProvenance(requestCallback, annotationInfo,MainPanel.getInstance().getCurrentOrganism());
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
    provenanceField.setSelectedIndex(0);
    evidenceCodeField.setText("");
    evidenceCodeLink.setText("");
    withFieldPrefix.setText("");
    withFieldId.setText("");
    withEntriesFlexTable.removeAllRows();
    noteField.setText("");
    notesFlexTable.removeAllRows();
    referenceFieldPrefix.setText("");
    referenceFieldId.setText("");
  }

  private void handleSelection() {
    if (selectionModel.getSelectedSet().isEmpty()) {
      clearModal();
    } else {
      Provenance selectedProvenance = selectionModel.getSelectedObject();

      int indexForField = getFieldIndex(selectedProvenance.getField());
      provenanceField.setSelectedIndex(indexForField);
      evidenceCodeField.setText(selectedProvenance.getEvidenceCode());
      evidenceCodeLink.setHref(ECO_BASE + selectedProvenance.getEvidenceCode()+"/");
      ProvenanceRestService.lookupTerm(evidenceCodeLink, selectedProvenance.getEvidenceCode());

      withEntriesFlexTable.removeAllRows();
      for (WithOrFrom withOrFrom : selectedProvenance.getWithOrFromList()) {
        addWithSelection(withOrFrom);
      }
      withFieldPrefix.setText("");

      referenceFieldPrefix.setText(selectedProvenance.getReference().getPrefix());
      referenceFieldId.setText(selectedProvenance.getReference().getLookupId());

      notesFlexTable.removeAllRows();
      for (String noteString : selectedProvenance.getNoteList()) {
        addReferenceSelection(noteString);
      }
      noteField.setText("");

    }

  }

  private int getFieldIndex(String field) {
    for(int i = 0 ; i < provenanceField.getItemCount() ; i++){
        if(provenanceField.getValue(i).equals(field)) return i ;
    }
    return 0;
  }

  public void redraw() {
    dataGrid.redraw();
  }

  @UiHandler("newGoButton")
  public void newProvenance(ClickEvent e) {
    provenanceTitle.setText("Add provenance to field " + AnnotatorPanel.selectedAnnotationInfo.getName());
    withEntriesFlexTable.removeAllRows();
    notesFlexTable.removeAllRows();
    selectionModel.clear();
    provenanceModal.show();
  }

  @UiHandler("editGoButton")
  public void editProvenance(ClickEvent e) {
    provenanceModal.show();
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
      Provenance provenanceInstance = ProvenanceConverter.convertFromJson(annotationsArray.get(i).isObject());
      annotationInfoList.add(provenanceInstance);
    }
  }

  @UiHandler("allEcoCheckBox")
  public void allEcoCheckBox(ClickEvent e) {
    ecoLookup.setUseAllEco(allEcoCheckBox.getValue());
    if(allEcoCheckBox.getValue()){
      helpLink.setHref("https://www.ebi.ac.uk/ols/ontologies/eco");
    }
    else {
      helpLink.setHref("http://geneontology.org/docs/guide-go-evidence-codes/");
    }
  }


  @UiHandler("saveNewProvenance")
  public void saveNewProvenanceButton(ClickEvent e) {
    Provenance provenance = getEditedProvenance();
    Provenance selectedProvenance = selectionModel.getSelectedObject();
    if (selectedProvenance != null) {
      provenance.setId(selectedProvenance.getId());
    }
    List<String> validationErrors = validateProvenance(provenance);
    if (validationErrors.size() > 0) {
      String errorString = "Invalid Annotation <br/>";
      for (String error : validationErrors) {
        errorString += "&bull; " + error + "<br/>";
      }
      Bootbox.alert(errorString);
      return;
    }
    withFieldPrefix.clear();
    withFieldId.clear();
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
    if (provenance.getId() != null) {
      ProvenanceRestService.updateProvenance(requestCallback, provenance);
    } else {
      ProvenanceRestService.saveProvenance(requestCallback, provenance);
    }
    provenanceModal.hide();
  }

  private List<String> validateProvenance(Provenance provenance) {
    List<String> validationErrors = new ArrayList<>();
    if (provenance.getFeature() == null) {
      validationErrors.add("You must provide a gene name");
    }
    if (provenance.getField() == null) {
      validationErrors.add("You must provide a field to describe");
    }
    if (provenance.getEvidenceCode() == null) {
      validationErrors.add("You must provide an ECO term");
    }
    if (!provenance.getEvidenceCode().contains(":")) {
      validationErrors.add("You must provide a prefix and suffix for the ECO term");
    }
    return validationErrors;
  }

  private Provenance getEditedProvenance() {
    Provenance provenance = new Provenance();
    provenance.setFeature(annotationInfo.getUniqueName());
    provenance.setField(provenanceField.getSelectedValue().trim());
    provenance.setEvidenceCode(evidenceCodeField.getText());
    provenance.setEvidenceCodeLabel(evidenceCodeLink.getText());
    provenance.setWithOrFromList(getWithList());
    Reference reference = new Reference(referenceFieldPrefix.getText(), referenceFieldId.getText());
    provenance.setReference(reference);
    provenance.setNoteList(getNoteList());
    return provenance;
  }

  private List<WithOrFrom> getWithList() {
    List<WithOrFrom> withOrFromList = new ArrayList<>();
    for (int i = 0; i < withEntriesFlexTable.getRowCount(); i++) {
      withOrFromList.add(new WithOrFrom(withEntriesFlexTable.getHTML(i, 0)));
    }
    String withPrefixText = withFieldPrefix.getText();
    String withIdText = withFieldId.getText();
    if (withPrefixText.length() > 0 && withIdText.length() > 0) {
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

  @UiHandler("cancelNewProvenance")
  public void cancelNewProvenanceButton(ClickEvent e) {
    clearModal();
    provenanceModal.hide();
  }

  @UiHandler("deleteGoButton")
  public void deleteProvenance(ClickEvent e) {
    final Provenance provenance = selectionModel.getSelectedObject();
    Bootbox.confirm("Remove Annotation: " + provenance.getField(), new ConfirmCallback() {
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
            Bootbox.alert("Failed to DELETE new Annotation");
          }
        };
        if(result){
          ProvenanceRestService.deleteProvenance(requestCallback, provenance);
        }
      }
    });
  }

  /**
   * Finds code inbetween paranthesis.  Returns null if nothing is found.
   *
   * @param inputString
   * @return
   */
  String getInnerCode(String inputString) {
    if (inputString.contains("(") && inputString.contains(")")) {
      int start = inputString.indexOf("(");
      int end = inputString.indexOf(")");
      return inputString.substring(start+1,end);
    }
    return null;
  }

  private void initializeTable() {
    // TODO: probably want a link here
//      curl -X GET "http://api.geneontology.org/api/bioentity/GO%3A0008015?rows=1&facet=false&unselect_evidence=false&exclude_automatic_assertions=false&fetch_objects=false&use_compact_associations=false" -H "accept: application/json"
//      GO:0008015
    TextColumn<Provenance> provenanceTextColumn = new TextColumn<Provenance>() {
      @Override
      public String getValue(Provenance annotationInfo) {
        return annotationInfo.getField() != null ? annotationInfo.getField() : annotationInfo.getField();
      }
    };
    provenanceTextColumn.setSortable(true);

    TextColumn<Provenance> withColumn = new TextColumn<Provenance>() {
      @Override
      public String getValue(Provenance annotationInfo) {
        return annotationInfo.getWithOrFromString();
      }
    };
    withColumn.setSortable(true);

    TextColumn<Provenance> referenceColumn = new TextColumn<Provenance>() {
      @Override
      public String getValue(Provenance annotationInfo) {
        return annotationInfo.getReference().getReferenceString();
      }
    };
    referenceColumn.setSortable(true);

    TextColumn<Provenance> evidenceColumn = new TextColumn<Provenance>() {
      @Override
      public String getValue(Provenance annotationInfo) {
        if (annotationInfo.getEvidenceCodeLabel() != null) {
          String label = annotationInfo.getEvidenceCodeLabel();
          String substring = getInnerCode(label);
          if (substring != null) {
            return substring;
          }
        }
        return annotationInfo.getEvidenceCode();
      }
    };
    evidenceColumn.setSortable(true);


    dataGrid.addColumn(provenanceTextColumn, "Field");
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
    if((selectedAnnotationInfo==null && this.annotationInfo!=null) ||
      (selectedAnnotationInfo!=null && this.annotationInfo==null) ||
      selectedAnnotationInfo!=null && !selectedAnnotationInfo.equals(this.annotationInfo)){
      this.annotationInfo = selectedAnnotationInfo;
      loadData();
    }
  }

  public void setEditable(boolean editable) {
    this.editable = editable;
//    dataGrid.setEnabled(editable);
    newGoButton.setEnabled(editable);
    editGoButton.setEnabled(editable);
    deleteGoButton.setEnabled(editable);
  }

}
