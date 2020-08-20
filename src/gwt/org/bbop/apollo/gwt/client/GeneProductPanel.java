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
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.GeneProductConverter;
import org.bbop.apollo.gwt.client.oracles.BiolinkLookup;
import org.bbop.apollo.gwt.client.oracles.BiolinkOntologyOracle;
import org.bbop.apollo.gwt.client.oracles.BiolinkSuggestBox;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.GeneProductRestService;
import org.bbop.apollo.gwt.shared.geneProduct.GeneProduct;
import org.bbop.apollo.gwt.shared.geneProduct.Reference;
import org.bbop.apollo.gwt.shared.geneProduct.WithOrFrom;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.SuggestBox;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.bootbox.client.callback.ConfirmCallback;

import java.util.ArrayList;
import java.util.List;

import static org.bbop.apollo.gwt.client.oracles.BiolinkOntologyOracle.ECO_BASE;

/**
 * Created by ndunn on 1/9/15.
 */
public class GeneProductPanel extends Composite {

  interface GeneProductUiBinder extends UiBinder<Widget, GeneProductPanel> {
  }

  private static GeneProductUiBinder ourUiBinder = GWT.create(GeneProductUiBinder.class);

  DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
  @UiField(provided = true)
  DataGrid<GeneProduct> dataGrid = new DataGrid<>(200, tablecss);
  @UiField
  TextBox noteField;
  @UiField(provided =  true)
  SuggestBox geneProductField;
  @UiField(provided = true)
  BiolinkSuggestBox evidenceCodeField;
  @UiField
  TextBox withFieldPrefix;
  @UiField
  Button deleteGoButton;
  @UiField
  Button newGoButton;
  @UiField
  Modal editGoModal;
  @UiField
  Button saveNewGeneProduct;
  @UiField
  Button cancelNewGeneProduct;
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
  org.gwtbootstrap3.client.ui.CheckBox alternateCheckBox;
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
  HTML geneProductTitle;
  @UiField
  org.gwtbootstrap3.client.ui.CheckBox allEcoCheckBox;
  @UiField
  Anchor helpLink;

  private static ListDataProvider<GeneProduct> dataProvider = new ListDataProvider<>();
  private static List<GeneProduct> annotationInfoList = dataProvider.getList();
  private SingleSelectionModel<GeneProduct> selectionModel = new SingleSelectionModel<>();
  private BiolinkOntologyOracle ecoLookup = new BiolinkOntologyOracle(BiolinkLookup.ECO);
  private SuggestedGeneProductOracle suggestedGeneProductOracle = new SuggestedGeneProductOracle();
  private Boolean editable  = false ;

  private AnnotationInfo annotationInfo;

  public GeneProductPanel() {
    geneProductField = new SuggestBox(suggestedGeneProductOracle);

    initLookups();
    dataGrid.setWidth("100%");
    initializeTable();
    dataProvider.addDataDisplay(dataGrid);
    dataGrid.setSelectionModel(selectionModel);

    initWidget(ourUiBinder.createAndBindUi(this));

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
          geneProductTitle.setText("Edit Gene Product for " + AnnotatorPanel.selectedAnnotationInfo.getName());
          handleSelection();
          editGoModal.show();
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

    redraw();
  }

  private void enableFields(boolean enabled) {
    saveNewGeneProduct.setEnabled(enabled);
    evidenceCodeField.setEnabled(enabled);
    geneProductField.setEnabled(enabled);
    referenceFieldPrefix.setEnabled(enabled);
    referenceFieldId.setEnabled(enabled);
    withFieldPrefix.setEnabled(enabled);
    withFieldId.setEnabled(enabled);
    noteField.setEnabled(enabled);
  }

  private void initLookups() {
    // most from here: http://geneontology.org/docs/guide-go-evidence-codes/
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
      GeneProductRestService.getGeneProduct(requestCallback, annotationInfo,MainPanel.getInstance().getCurrentOrganism());

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
    geneProductField.setText("");
    evidenceCodeField.setText("");
    evidenceCodeLink.setText("");
    withFieldPrefix.setText("");
    withFieldId.setText("");
    withEntriesFlexTable.removeAllRows();
    noteField.setText("");
    notesFlexTable.removeAllRows();
    referenceFieldPrefix.setText("");
    referenceFieldId.setText("");
    alternateCheckBox.setValue(false);
  }

  private void handleSelection() {
    if (selectionModel.getSelectedSet().isEmpty()) {
      clearModal();
    } else {
      GeneProduct selectedGeneProduct = selectionModel.getSelectedObject();

      geneProductField.setText(selectedGeneProduct.getProductName());
      alternateCheckBox.setValue(selectedGeneProduct.isAlternate());
      evidenceCodeField.setText(selectedGeneProduct.getEvidenceCode());
      evidenceCodeLink.setHref(ECO_BASE + selectedGeneProduct.getEvidenceCode()+"/");
      GeneProductRestService.lookupTerm(evidenceCodeLink, selectedGeneProduct.getEvidenceCode());

      withEntriesFlexTable.removeAllRows();
      for (WithOrFrom withOrFrom : selectedGeneProduct.getWithOrFromList()) {
        addWithSelection(withOrFrom);
      }
      withFieldPrefix.setText("");

      referenceFieldPrefix.setText(selectedGeneProduct.getReference().getPrefix());
      referenceFieldId.setText(selectedGeneProduct.getReference().getLookupId());

      notesFlexTable.removeAllRows();
      for (String noteString : selectedGeneProduct.getNoteList()) {
        addReferenceSelection(noteString);
      }
      noteField.setText("");

    }

  }

  public void redraw() {
    dataGrid.redraw();
  }

  @UiHandler("newGoButton")
  public void newGeneProduct(ClickEvent e) {
    geneProductTitle.setText("Add new Gene Product to " + AnnotatorPanel.selectedAnnotationInfo.getName());
    withEntriesFlexTable.removeAllRows();
    notesFlexTable.removeAllRows();
    selectionModel.clear();
    editGoModal.show();
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

  @UiHandler("editGoButton")
  public void editGeneProduct(ClickEvent e) {
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
      GeneProduct geneProductInstance = GeneProductConverter.convertFromJson(annotationsArray.get(i).isObject());
      annotationInfoList.add(geneProductInstance);
    }
  }


  @UiHandler("saveNewGeneProduct")
  public void saveNewGeneProductButton(ClickEvent e) {
    GeneProduct geneProduct = getEditedGeneProduct();
    GeneProduct selectedGeneProduct = selectionModel.getSelectedObject();
    if (selectedGeneProduct != null) {
      geneProduct.setId(selectedGeneProduct.getId());
    }
    List<String> validationErrors = validateGeneProduct(geneProduct);
    if (validationErrors.size() > 0) {
      String errorString = "Invalid Gene Product <br/>";
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
    if (geneProduct.getId() != null) {
      GeneProductRestService.updateGeneProduct(requestCallback, geneProduct);
    } else {
      GeneProductRestService.saveGeneProduct(requestCallback, geneProduct);
    }
    editGoModal.hide();
  }

  private List<String> validateGeneProduct(GeneProduct geneProduct) {
    List<String> validationErrors = new ArrayList<>();
    if (geneProduct.getFeature() == null) {
      validationErrors.add("You must provide a gene name");
    }
    if (geneProduct.getProductName() == null) {
      validationErrors.add("You must provide a product name");
    }
    if (geneProduct.getEvidenceCode() == null) {
      validationErrors.add("You must provide an ECO term");
    }
    if (!geneProduct.getEvidenceCode().contains(":")) {
      validationErrors.add("You must provide a prefix and suffix for the ECO term");
    }
    return validationErrors;
  }

  private GeneProduct getEditedGeneProduct() {
    GeneProduct geneProduct = new GeneProduct();
    geneProduct.setFeature(annotationInfo.getUniqueName());
    geneProduct.setProductName(geneProductField.getText().trim());
    geneProduct.setAlternate(alternateCheckBox.getValue());
    geneProduct.setEvidenceCode(evidenceCodeField.getText());
    geneProduct.setEvidenceCodeLabel(evidenceCodeLink.getText());
    geneProduct.setWithOrFromList(getWithList());
    Reference reference = new Reference(referenceFieldPrefix.getText(), referenceFieldId.getText());
    geneProduct.setReference(reference);
    geneProduct.setNoteList(getNoteList());
    return geneProduct;
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

  @UiHandler("cancelNewGeneProduct")
  public void cancelNewGeneProductButton(ClickEvent e) {
    clearModal();
    editGoModal.hide();
  }

  @UiHandler("deleteGoButton")
  public void deleteGeneProduct(ClickEvent e) {
    final GeneProduct geneProduct = selectionModel.getSelectedObject();
    Bootbox.confirm("Remove Gene Product: " + geneProduct.getProductName(), new ConfirmCallback() {
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
            Bootbox.alert("Failed to DELETE new Gene Product");
          }
        };
        if(result){
          GeneProductRestService.deleteGeneProduct(requestCallback, geneProduct);
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
    TextColumn<GeneProduct> geneProductTextColumn = new TextColumn<GeneProduct>() {
      @Override
      public String getValue(GeneProduct annotationInfo) {
        return annotationInfo.getProductName() != null ? annotationInfo.getProductName() : annotationInfo.getProductName();
      }
    };
    geneProductTextColumn.setSortable(true);

    TextColumn<GeneProduct> withColumn = new TextColumn<GeneProduct>() {
      @Override
      public String getValue(GeneProduct annotationInfo) {
        return annotationInfo.getWithOrFromString();
      }
    };
    withColumn.setSortable(true);

    TextColumn<GeneProduct> referenceColumn = new TextColumn<GeneProduct>() {
      @Override
      public String getValue(GeneProduct annotationInfo) {
        return annotationInfo.getReference().getReferenceString();
      }
    };
    referenceColumn.setSortable(true);

    TextColumn<GeneProduct> evidenceColumn = new TextColumn<GeneProduct>() {
      @Override
      public String getValue(GeneProduct annotationInfo) {
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


    dataGrid.addColumn(geneProductTextColumn, "Name");
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
    if(!selectedAnnotationInfo.equals(this.annotationInfo)){
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
