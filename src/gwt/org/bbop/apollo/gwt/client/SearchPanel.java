package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.SearchRestService;
import org.bbop.apollo.gwt.shared.sequence.SearchHit;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by ndunn on 12/17/14.
 */
public class SearchPanel extends Composite {

  private JSONObject searchToolData = new JSONObject();

  interface SearchPanelUiBinder extends UiBinder<Widget, SearchPanel> {
  }


  private static SearchPanelUiBinder ourUiBinder = GWT.create(SearchPanelUiBinder.class);
  DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
  @UiField(provided = true)
  DataGrid<SearchHit> dataGrid = new DataGrid<>(20, tablecss);
  @UiField(provided = true)
  WebApolloSimplePager pager = new WebApolloSimplePager(WebApolloSimplePager.TextLocation.CENTER);
  @UiField
  CheckBox searchAllGenomes;
  @UiField
  Button searchGenomesButton;
  @UiField
  static TextArea sequenceSearchBox;
  @UiField
  ListBox searchTypeList;

  //  private boolean creatingNewOrganism = false; // a special flag for handling the clearSelection event when filling out new organism info
//    private boolean savingNewOrganism = false; // a special flag for handling the clearSelection event when filling out new organism info
//
  final private LoadingDialog loadingDialog;
  final private ErrorDialog errorDialog;

  static private ListDataProvider<SearchHit> dataProvider = new ListDataProvider<>();
  private static List<SearchHit> searchHitList = new ArrayList<>();
  private static List<SearchHit> filteredSearchHitList = dataProvider.getList();


  private final SingleSelectionModel<SearchHit> singleSelectionModel = new SingleSelectionModel<>();

  public SearchPanel() {
    initWidget(ourUiBinder.createAndBindUi(this));
    loadingDialog = new LoadingDialog("Processing ...", null, false);
    errorDialog = new ErrorDialog("Error", "Organism directory must be an absolute path pointing to 'trackList.json'", false, true);


    searchTypeList.addItem("Blat nucl", "nucl");
    searchTypeList.addItem("Blat pept", "peptide");

    TextColumn<SearchHit> idColumn = new TextColumn<SearchHit>() {
      @Override
      public String getValue(SearchHit searchHit) {
        return searchHit.getId();
      }
    };
    Column<SearchHit, Number> startColumn = new Column<SearchHit, Number>(new NumberCell()) {
      @Override
      public Long getValue(SearchHit object) {
        return object.getStart();
      }
    };
    Column<SearchHit, Number> endColumn = new Column<SearchHit, Number>(new NumberCell()) {
      @Override
      public Long getValue(SearchHit object) {
        return object.getEnd();
      }
    };
    Column<SearchHit, Number> scoreColumn = new Column<SearchHit, Number>(new NumberCell()) {
      @Override
      public Double getValue(SearchHit object) {
        return object.getScore();
      }
    };
    Column<SearchHit, Number> significanceColumn = new Column<SearchHit, Number>(new NumberCell()) {
      @Override
      public Double getValue(SearchHit object) {
        return object.getSignificance();
      }
    };
    Column<SearchHit, Number> identityColumn = new Column<SearchHit, Number>(new NumberCell()) {
      @Override
      public Double getValue(SearchHit object) {
        return object.getIdentity();
      }
    };
    ButtonCell buttonCell = new ButtonCell();
    Column<SearchHit, String> commandColumn = new Column<SearchHit, String>(buttonCell) {
      @Override
      public String getValue(SearchHit object) {
        return "Create";
      }
    };

    idColumn.setSortable(true);
    startColumn.setSortable(true);
    endColumn.setSortable(true);
    scoreColumn.setSortable(true);
    significanceColumn.setSortable(true);
    identityColumn.setSortable(true);


//        Annotator.eventBus.addHandler(OrganismChangeEvent.TYPE, new OrganismChangeEventHandler() {
//            @Override
//            public void onOrganismChanged(OrganismChangeEvent organismChangeEvent) {
//                searchHitList.clear();
////                organismInfoList.addAll(MainPanel.getInstance().getOrganismInfoList());
//                filterList();
//            }
//        });

    dataGrid.setLoadingIndicator(new HTML("Searching ... "));
    dataGrid.addColumn(idColumn, "ID");
    dataGrid.setColumnWidth(0, "10px");

    dataGrid.addColumn(startColumn, "Start");
    dataGrid.setColumnWidth(1, "10px");

    dataGrid.addColumn(endColumn, "End");
    dataGrid.setColumnWidth(2, "10px");

    dataGrid.addColumn(scoreColumn, "Score");
    dataGrid.setColumnWidth(3, "10px");

    dataGrid.addColumn(identityColumn, "Identity");
    dataGrid.setColumnWidth(4, "10px");

    dataGrid.addColumn(commandColumn, "Action");
    commandColumn.setFieldUpdater(new FieldUpdater<SearchHit, String>() {
      @Override
      public void update(int index, SearchHit object, String value) {
//          for (Category category : categories) {
//            if (category.getDisplayName().equals(value)) {
//              object.setCategory(category);
//            }
//          }
//          ContactDatabase.get().refreshDisplays();
      }
    });
//      dataGrid.setColumnWidth(categoryColumn, 130, Unit.PX);

    dataGrid.setColumnWidth(5, "10px");

    dataGrid.setEmptyTableWidget(new Label(""));


      singleSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
          @Override
          public void onSelectionChange(SelectionChangeEvent event) {
            SearchHit searchHit = singleSelectionModel.getSelectedObject();
            MainPanel.updateGenomicViewerForLocation(searchHit.getId(),searchHit.getStart().intValue(),searchHit.getEnd().intValue());
          }
      });
      dataGrid.setSelectionModel(singleSelectionModel);

    dataProvider.addDataDisplay(dataGrid);
    pager.setDisplay(dataGrid);


    dataGrid.addDomHandler(new DoubleClickHandler() {
      @Override
      public void onDoubleClick(DoubleClickEvent event) {
        if (singleSelectionModel.getSelectedObject() != null) {
          Bootbox.alert("navigate to the annotation");
//                    OrganismInfo organismInfo = singleSelectionModel.getSelectedObject();
//                    if (organismInfo.getObsolete()) {
//                        Bootbox.alert("You will have to make this organism 'active' by unselecting the 'Obsolete' checkbox in the Organism Details panel at the bottom.");
//                        return;
//                    }
//                    String orgId = organismInfo.getId();
//                    if (!MainPanel.getInstance().getCurrentOrganism().getId().equals(orgId)) {
//                        OrganismRestService.switchOrganismById(orgId);
//                    }
        }
      }
    }, DoubleClickEvent.getType());

    ColumnSortEvent.ListHandler<SearchHit> sortHandler = new ColumnSortEvent.ListHandler<SearchHit>(searchHitList);
    dataGrid.addColumnSortHandler(sortHandler);
    sortHandler.setComparator(idColumn, new Comparator<SearchHit>() {
      @Override
      public int compare(SearchHit o1, SearchHit o2) {
        return o1.getId().compareTo(o2.getId());
      }
    });
    sortHandler.setComparator(startColumn, new Comparator<SearchHit>() {
      @Override
      public int compare(SearchHit o1, SearchHit o2) {
        return (int) (o1.getStart() - o2.getStart());
      }
    });
    sortHandler.setComparator(endColumn, new Comparator<SearchHit>() {
      @Override
      public int compare(SearchHit o1, SearchHit o2) {
        return (int) (o1.getEnd() - o2.getEnd());
      }
    });

    sortHandler.setComparator(identityColumn, new Comparator<SearchHit>() {
      @Override
      public int compare(SearchHit o1, SearchHit o2) {
        return (int) (o1.getIdentity() - o2.getIdentity());
      }
    });

    sortHandler.setComparator(scoreColumn, new Comparator<SearchHit>() {
      @Override
      public int compare(SearchHit o1, SearchHit o2) {
        return (int) (o1.getScore() - o2.getScore());
      }
    });

  }


  @UiHandler("sequenceSearchBox")
  public void doSearch(KeyUpEvent keyUpEvent) {
    filterList();
  }

  static void filterList() {
    String text = sequenceSearchBox.getText();
    filteredSearchHitList.clear();
    if (text.trim().length() == 0) {
      filteredSearchHitList.addAll(searchHitList);
      return;
    }
    for (SearchHit organismInfo : searchHitList) {
      if (organismInfo.getId().toLowerCase().contains(text.toLowerCase())) {
        filteredSearchHitList.add(organismInfo);
      }
    }
  }

  public void loadOrganismInfo() {
//        loadOrganismInfo(singleSelectionModel.getSelectedObject());
  }

//    public void loadOrganismInfo(OrganismInfo organismInfo) {
//        if (organismInfo == null) {
//            setNoSelection();
//            return;
//        }
//
//        setTextEnabled(organismInfo.isEditable());
//
//        GWT.log("loadOrganismInfo setValue " + organismInfo.getPublicMode());
//        Boolean isEditable = organismInfo.isEditable() || MainPanel.getInstance().isCurrentUserAdmin();
//
//        organismName.setText(organismInfo.getName());
//        organismName.setEnabled(isEditable);
//
//        blatdb.setText(organismInfo.getBlatDb());
//        blatdb.setEnabled(isEditable);
//
//        genus.setText(organismInfo.getGenus());
//        genus.setEnabled(isEditable);
//
//        species.setText(organismInfo.getSpecies());
//        species.setEnabled(isEditable);
//
//        if (organismInfo.getNumFeatures() == 0) {
//          sequenceFile.setText(organismInfo.getDirectory() );
//          sequenceFile.setEnabled(isEditable);
//        }
//        else{
//          sequenceFile.setText(organismInfo.getDirectory() + " (remove " + organismInfo.getNumFeatures() + "annotations to change)" );
//          sequenceFile.setEnabled(false);
//        }
//
//        publicMode.setValue(organismInfo.getPublicMode());
//        publicMode.setEnabled(isEditable);
//
//        obsoleteButton.setValue(organismInfo.getObsolete());
//        obsoleteButton.setEnabled(isEditable);
//
//        organismIdLabel.setHTML("Internal ID: " + organismInfo.getId());
//
//        nonDefaultTranslationTable.setText(organismInfo.getNonDefaultTranslationTable());
//        nonDefaultTranslationTable.setEnabled(isEditable);
//
//        downloadOrganismButton.setVisible(false);
//    }

//    private class UpdateInfoListCallback implements RequestCallback {
//
//        @Override
//        public void onResponseReceived(Request request, Response response) {
//            JSONValue j = JSONParser.parseStrict(response.getText());
//            JSONObject obj = j.isObject();
//            if (obj != null && obj.containsKey("error")) {
//                Bootbox.alert(obj.get("error").isString().stringValue());
//                changeButtonSelection();
//                setTextEnabled(false);
//                clearTextBoxes();
//                singleSelectionModel.clear();
//            } else {
//                List<OrganismInfo> organismInfoList = OrganismInfoConverter.convertJSONStringToOrganismInfoList(response.getText());
//                dataGrid.setSelectionModel(singleSelectionModel);
//                MainPanel.getInstance().getOrganismInfoList().clear();
//                MainPanel.getInstance().getOrganismInfoList().addAll(organismInfoList);
//                changeButtonSelection();
//                OrganismChangeEvent organismChangeEvent = new OrganismChangeEvent(organismInfoList);
//                organismChangeEvent.setAction(OrganismChangeEvent.Action.LOADED_ORGANISMS);
//                Annotator.eventBus.fireEvent(organismChangeEvent);
//
//                // in the case where we just add one . . .we should refresh the app state
//                if (organismInfoList.size() == 1) {
//                    MainPanel.getInstance().getAppState();
//                }
//            }
//            if (savingNewOrganism) {
//                savingNewOrganism = false;
//                setNoSelection();
//                changeButtonSelection(false);
//                loadingDialog.hide();
//                Window.Location.reload();
//            }
//        }
//
//        @Override
//        public void onError(Request request, Throwable exception) {
//            loadingDialog.hide();
//            Bootbox.alert("Error: " + exception);
//        }
//    }

  @UiHandler("searchGenomesButton")
  public void doSearch(ClickEvent clickEvent) {
    GWT.log("searching with: "+searchTypeList.getSelectedValue()+ " and "+ sequenceSearchBox.getValue() + " " + searchAllGenomes.getValue());
    JSONObject searchData = searchToolData.get(searchTypeList.getSelectedValue()).isObject();
    RequestCallback requestCallback = new RequestCallback() {
      @Override
      public void onResponseReceived(Request request, Response response) {
//        GWT.log("response: "+response.getText());
        searchHitList.clear();
        filteredSearchHitList.clear();
        try {
          JSONArray hitArray = JSONParser.parseStrict(response.getText()).isObject().get("matches").isArray();
          for(int i = 0 ; i < hitArray.size() ; i++){
            JSONObject hit = hitArray.get(i).isObject();
            SearchHit searchHit = new SearchHit();
            // {"matches":[{"identity":100.0,"significance":3.2E-52,"subject":{"location":{"fmin":3522507,"fmax":3522788,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":1,"fmax":94,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":203.0},{"identity":100.0,"significance":2.4E-48,"subject":{"location":{"fmin":3522059,"fmax":3522334,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":95,"fmax":186,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":190.0},{"identity":100.0,"significance":1.1E-31,"subject":{"location":{"fmin":3483437,"fmax":3483637,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":279,"fmax":345,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":134.0},{"identity":100.0,"significance":1.2E-28,"subject":{"location":{"fmin":3481625,"fmax":3481807,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":345,"fmax":405,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":124.0},{"identity":100.0,"significance":6.7E-25,"subject":{"location":{"fmin":3462508,"fmax":3462660,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":552,"fmax":602,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":112.0},{"identity":100.0,"significance":4.4E-24,"subject":{"location":{"fmin":3510265,"fmax":3510420,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":229,"fmax":280,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":109.0},{"identity":100.0,"significance":3.8E-21,"subject":{"location":{"fmin":3464816,"fmax":3464956,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":505,"fmax":551,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":99.0},{"identity":100.0,"significance":5.0E-21,"subject":{"location":{"fmin":3468605,"fmax":3468748,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":457,"fmax":504,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":99.0},{"identity":100.0,"significance":9.7E-20,"subject":{"location":{"fmin":3521640,"fmax":3521768,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":186,"fmax":228,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":95.0},{"identity":100.0,"significance":5.3E-12,"subject":{"location":{"fmin":3474164,"fmax":3474262,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":424,"fmax":456,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":69.0},{"identity":95.24,"significance":0.0025,"subject":{"location":{"fmin":3474468,"fmax":3474530,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":406,"fmax":426,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":40.0}]}
            searchHit.setId(hit.get("subject").isObject().get("feature").isObject().get("uniquename").isString().stringValue());
            searchHit.setStart(Math.round(hit.get("subject").isObject().get("location").isObject().get("fmin").isNumber().doubleValue()));
            searchHit.setEnd(Math.round(hit.get("subject").isObject().get("location").isObject().get("fmax").isNumber().doubleValue()));
            searchHit.setScore(hit.get("rawscore").isNumber().doubleValue());
            searchHit.setSignificance(hit.get("significance").isNumber().doubleValue());
            searchHit.setIdentity(hit.get("identity").isNumber().doubleValue());
            searchHitList.add(searchHit);
            filteredSearchHitList.add(searchHit);
          }
          GWT.log("adding hits: "+hitArray.size()+ " "+ searchHitList.size());
        } catch (Exception e) {
          GWT.log("unable to to do search"+e.getMessage() + " "+response.getText() + " " + response.getStatusCode());
        }

      }

      @Override
      public void onError(Request request, Throwable exception) {
        Bootbox.alert("Problem doing search: " + exception.getMessage());
      }
    };
    String databaseId = null;
    if(!searchAllGenomes.getValue()){
      databaseId = MainPanel.getCurrentSequence().getName();
    }
    SearchRestService.searchSequence(requestCallback,searchTypeList.getSelectedValue(),sequenceSearchBox.getValue(),databaseId);

  }

  public void reload() {
    // {"sequence_search_tools":{"blat_nuc":{"search_exe":"/usr/local/bin/blat","search_class":"org.bbop.apollo.sequence.search.blat.BlatCommandLineNucleotideToNucleotide","name":"Blat nucleotide","params":""},"blat_prot":{"search_exe":"/usr/local/bin/blat","search_class":"org.bbop.apollo.sequence.search.blat.BlatCommandLineProteinToNucleotide","name":"Blat protein","params":""}}}
    SearchRestService.getTools(new RequestCallback() {
      @Override
      public void onResponseReceived(Request request, Response response) {
        searchTypeList.clear();
        try {
          JSONValue jsonValue = JSONParser.parseStrict(response.getText());
          JSONObject searchTools = jsonValue.isObject().get("sequence_search_tools").isObject();
          searchToolData = searchTools;
          for (String key : searchTools.keySet()) {
            String name = searchTools.get(key).isObject().get("name").isString().stringValue();
            searchTypeList.addItem(name,key);
          }
        } catch (Exception e) {
          GWT.log("unable to find search tools "+e.getMessage() + " "+response.getText() + " " + response.getStatusCode());
        }

      }

      @Override
      public void onError(Request request, Throwable exception) {
        Bootbox.alert("Problem getting search tools: " + exception.getMessage());
      }
    });
    dataGrid.redraw();
  }

//    // Clear textboxes and make them unselectable
//    private void setNoSelection() {
//        clearTextBoxes();
//        setTextEnabled(false);
//        downloadOrganismButton.setVisible(false);
//    }
//
//    private void changeButtonSelection() {
//        changeButtonSelection(singleSelectionModel.getSelectedObject() != null);
//    }
//
//    // Set the button states/visibility depending on whether there is a selection or not
//    private void changeButtonSelection(boolean selection) {
//        //Boolean isAdmin = MainPanel.getInstance().isCurrentUserAdmin();
//        boolean isAdmin = MainPanel.getInstance().isCurrentUserInstructorOrBetter();
//        if (selection) {
//            downloadOrganismButton.setVisible(false);
//            publicMode.setVisible(isAdmin);
//            obsoleteButton.setVisible(isAdmin);
//        } else {
//            downloadOrganismButton.setVisible(false);
//            publicMode.setVisible(false);
//            obsoleteButton.setVisible(false);
//        }
//    }
//
//    //Utility function for toggling the textboxes (gray out)
//    private void setTextEnabled(boolean enabled) {
//        sequenceFile.setEnabled(enabled);
//        organismName.setEnabled(enabled);
//        genus.setEnabled(enabled);
//        species.setEnabled(enabled);
//        blatdb.setEnabled(enabled);
//        nonDefaultTranslationTable.setEnabled(enabled);
//        publicMode.setEnabled(enabled);
//        obsoleteButton.setEnabled(enabled);
//    }
//
//    //Utility function for clearing the textboxes ("")
//    private void clearTextBoxes() {
//        organismName.setText("");
//        sequenceFile.setText("");
//        genus.setText("");
//        species.setText("");
//        blatdb.setText("");
//        nonDefaultTranslationTable.setText("");
//        publicMode.setValue(false);
//        obsoleteButton.setValue(false);
//    }

}
