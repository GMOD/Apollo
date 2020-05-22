package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.cell.client.SelectionCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
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
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.AnnotationRestService;
import org.bbop.apollo.gwt.client.rest.SearchRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.bbop.apollo.gwt.shared.sequence.SearchHit;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.bootbox.client.callback.ConfirmCallback;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by ndunn on 12/17/14.
 */
public class SearchPanel extends Composite {

  interface SearchPanelUiBinder extends UiBinder<Widget, SearchPanel> {
  }


  private static SearchPanelUiBinder ourUiBinder = GWT.create(SearchPanelUiBinder.class);
  DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
  @UiField(provided = true)
  DataGrid<SearchHit> dataGrid = new DataGrid<>(50, tablecss);
  @UiField(provided = true)
  WebApolloSimplePager pager = new WebApolloSimplePager(WebApolloSimplePager.TextLocation.CENTER);
  @UiField
  CheckBox searchAllGenomes;
  @UiField
  Button searchGenomesButton;
  @UiField
  TextArea sequenceSearchBox;
  @UiField
  ListBox searchTypeList;

  final private LoadingDialog loadingDialog;

  static private ListDataProvider<SearchHit> dataProvider = new ListDataProvider<>();
  private static List<SearchHit> searchHitList = dataProvider.getList();


  private final SingleSelectionModel<SearchHit> singleSelectionModel = new SingleSelectionModel<>();

  private Column<SearchHit, Number> scoreColumn;

  public SearchPanel() {
    initWidget(ourUiBinder.createAndBindUi(this));
    loadingDialog = new LoadingDialog("Searching ...", null, false);

    searchTypeList.addItem("Blat nucl", "nucl");
    searchTypeList.addItem("Blat pept", "peptide");

    TextColumn<SearchHit> idColumn = new TextColumn<SearchHit>() {
      @Override
      public String getValue(SearchHit searchHit) {
        return searchHit.getId();
      }
    };
    Column<SearchHit, Number> strandColumn = new Column<SearchHit, Number>(new NumberCell()) {
      @Override
      public Integer getValue(SearchHit object) {
        return object.getStrand();
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
    scoreColumn = new Column<SearchHit, Number>(new NumberCell()) {
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
    List<String> options = new ArrayList<>();
    options.add("--");
    options.add("Save sequence");
    options.add("Create annotation");

    final SelectionCell selectionCell = new SelectionCell(options);
    Column<SearchHit, String> commandColumn = new Column<SearchHit, String>(selectionCell) {
      @Override
      public String getValue(SearchHit object) {
        return null;
      }
    };
    commandColumn.setFieldUpdater(new FieldUpdater<SearchHit, String>() {
      @Override
      public void update(int index, final SearchHit searchHit, String actionValue) {
        if(actionValue.toLowerCase().contains("save")){
          MainPanel.updateGenomicViewerForLocation(searchHit.getId(), searchHit.getStart().intValue(), searchHit.getEnd().intValue());
          MainPanel.highlightRegion(searchHit.getId(), searchHit.getStart().intValue(), searchHit.getEnd().intValue());
          // versus
          List<SequenceInfo> sequenceInfoList = new ArrayList<>();
          sequenceInfoList.add(MainPanel.getCurrentSequence());
          ExportPanel exportPanel = new ExportPanel(
            MainPanel.getInstance().getCurrentOrganism(),
            FeatureStringEnum.TYPE_FASTA.getValue(),
            false,
            sequenceInfoList,
            searchHit.getLocation()
          );
          exportPanel.show();
        }
        else
        if(actionValue.toLowerCase().contains("create")){
          MainPanel.updateGenomicViewerForLocation(searchHit.getId(), searchHit.getStart().intValue(), searchHit.getEnd().intValue());
          MainPanel.highlightRegion(searchHit.getId(), searchHit.getStart().intValue(), searchHit.getEnd().intValue());

          RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
              try {
                JSONValue jsonValue = JSONParser.parseStrict(response.getText());
                JSONArray features = jsonValue.isObject().get(FeatureStringEnum.FEATURES.getValue()).isArray();
                final String parentName = features.get(0).isObject().get(FeatureStringEnum.PARENT_NAME.getValue()).isString().stringValue();
                Bootbox.confirm("Transcript added from search hit with strand "+ searchHit.getStrand() +".  Verify details now?", new ConfirmCallback() {
                  @Override
                  public void callback(boolean result) {
                    if(result){
                      MainPanel.viewInAnnotationPanel(parentName,null);
                    }
                  }
                });
              } catch (Exception e) {
                Bootbox.alert("There was a problem adding the search hit: "+e.getMessage());
              }
            }

            @Override
            public void onError(Request request, Throwable exception) {
              Bootbox.alert("Problem adding search hit: "+exception.getMessage());
            }
          };
          AnnotationInfo annotationInfo = new AnnotationInfo();
          annotationInfo.setMin(searchHit.getStart().intValue());
          annotationInfo.setMax(searchHit.getEnd().intValue());
          annotationInfo.setSequence(searchHit.getId());
          annotationInfo.setStrand(searchHit.getStrand().intValue()); // should we set this explicitly?
          annotationInfo.setType("mRNA"); // this is just the default for now
          AnnotationRestService.createTranscriptWithExon(requestCallback,annotationInfo);
        }
      }
    });

    idColumn.setSortable(true);
    strandColumn.setSortable(true);
    startColumn.setSortable(true);
    endColumn.setSortable(true);
    scoreColumn.setSortable(true);
    significanceColumn.setSortable(true);
    identityColumn.setSortable(true);
    scoreColumn.setDefaultSortAscending(false);

    ColumnSortEvent.ListHandler<SearchHit> sortHandler = new ColumnSortEvent.ListHandler<SearchHit>(searchHitList);
    dataGrid.addColumnSortHandler(sortHandler);
    sortHandler.setComparator(idColumn, new Comparator<SearchHit>() {
      @Override
      public int compare(SearchHit o1, SearchHit o2) {
        return o1.getId().compareTo(o2.getId());
      }
    });
    sortHandler.setComparator(strandColumn, new Comparator<SearchHit>() {
      @Override
      public int compare(SearchHit o1, SearchHit o2) {
        return o1.getStrand().compareTo(o2.getStrand());
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

    sortHandler.setComparator(significanceColumn, new Comparator<SearchHit>() {
      @Override
      public int compare(SearchHit o1, SearchHit o2) {
        if (o1.getSignificance() == o2.getSignificance()) return 0;
        return o1.getSignificance() < o2.getSignificance() ? -1 : 1;
      }
    });

    sortHandler.setComparator(scoreColumn, new Comparator<SearchHit>() {
      @Override
      public int compare(SearchHit o1, SearchHit o2) {
        return (int) (o1.getScore() - o2.getScore());
      }
    });

    dataGrid.setLoadingIndicator(new HTML("Searching ... "));
    dataGrid.addColumn(idColumn, "ID");
    dataGrid.setColumnWidth(0, "10px");

    dataGrid.addColumn(startColumn, "Start");
    dataGrid.setColumnWidth(1, "10px");

    dataGrid.addColumn(endColumn, "End");
    dataGrid.setColumnWidth(2, "10px");

    dataGrid.addColumn(strandColumn, "Strand");
    dataGrid.setColumnWidth(3, "10px");

    dataGrid.addColumn(scoreColumn, "Score");
    dataGrid.setColumnWidth(4, "10px");

    dataGrid.addColumn(significanceColumn, "Significance");
    dataGrid.setColumnWidth(5, "10px");

    dataGrid.addColumn(identityColumn, "Identity");
    dataGrid.setColumnWidth(6, "10px");

    dataGrid.addColumn(commandColumn, "Action");

    dataGrid.setColumnWidth(7, "10px");

    dataGrid.setEmptyTableWidget(new Label(""));


    singleSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      @Override
      public void onSelectionChange(SelectionChangeEvent event) {
        SearchHit searchHit = singleSelectionModel.getSelectedObject();
        MainPanel.updateGenomicViewerForLocation(searchHit.getId(), searchHit.getStart().intValue(), searchHit.getEnd().intValue());
        MainPanel.highlightRegion(searchHit.getId(), searchHit.getStart().intValue(), searchHit.getEnd().intValue());
      }
    });
    dataGrid.setSelectionModel(singleSelectionModel);

    dataProvider.addDataDisplay(dataGrid);
    pager.setDisplay(dataGrid);


  }

  void setSearch(String residues, String searchType) {
    sequenceSearchBox.setText(residues);
    for (int i = 0; i < searchTypeList.getItemCount(); i++) {
      // blat_nuc peptide
      // blat_prot peptide
      if (searchType.equalsIgnoreCase("peptide")
        && searchTypeList.getValue(i).equalsIgnoreCase("blat_prot")) {
//        searchTypeList.setSelectedIndex(i);
        searchTypeList.setItemSelected(i, true);
      } else if (searchType.equalsIgnoreCase("nucleotide")
        && searchTypeList.getValue(i).equalsIgnoreCase("blat_nuc")) {
        searchTypeList.setSelectedIndex(i);
      }
    }
  }

  @UiHandler("clearButton")
  public void clearSearch(ClickEvent clickEvent) {
    sequenceSearchBox.setText("");
    searchHitList.clear();
  }


  @UiHandler("searchGenomesButton")
  public void doSearch(ClickEvent clickEvent) {
    RequestCallback requestCallback = new RequestCallback() {
      @Override
      public void onResponseReceived(Request request, Response response) {
        searchHitList.clear();
        loadingDialog.hide();
        try {
          JSONObject responseObject = JSONParser.parseStrict(response.getText()).isObject();
          if(responseObject.get("matches")==null){
            String errorString = responseObject.get("error").isString().stringValue();
            Bootbox.alert("Error: "+errorString);
            return ;
          }
          JSONArray hitArray = responseObject.get("matches").isArray();
          for (int i = 0; i < hitArray.size(); i++) {
            JSONObject hit = hitArray.get(i).isObject();
            SearchHit searchHit = new SearchHit();
            // {"matches":[{"identity":100.0,"significance":3.2E-52,"subject":{"location":{"fmin":3522507,"fmax":3522788,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":1,"fmax":94,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":203.0},{"identity":100.0,"significance":2.4E-48,"subject":{"location":{"fmin":3522059,"fmax":3522334,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":95,"fmax":186,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":190.0},{"identity":100.0,"significance":1.1E-31,"subject":{"location":{"fmin":3483437,"fmax":3483637,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":279,"fmax":345,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":134.0},{"identity":100.0,"significance":1.2E-28,"subject":{"location":{"fmin":3481625,"fmax":3481807,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":345,"fmax":405,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":124.0},{"identity":100.0,"significance":6.7E-25,"subject":{"location":{"fmin":3462508,"fmax":3462660,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":552,"fmax":602,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":112.0},{"identity":100.0,"significance":4.4E-24,"subject":{"location":{"fmin":3510265,"fmax":3510420,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":229,"fmax":280,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":109.0},{"identity":100.0,"significance":3.8E-21,"subject":{"location":{"fmin":3464816,"fmax":3464956,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":505,"fmax":551,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":99.0},{"identity":100.0,"significance":5.0E-21,"subject":{"location":{"fmin":3468605,"fmax":3468748,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":457,"fmax":504,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":99.0},{"identity":100.0,"significance":9.7E-20,"subject":{"location":{"fmin":3521640,"fmax":3521768,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":186,"fmax":228,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":95.0},{"identity":100.0,"significance":5.3E-12,"subject":{"location":{"fmin":3474164,"fmax":3474262,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":424,"fmax":456,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":69.0},{"identity":95.24,"significance":0.0025,"subject":{"location":{"fmin":3474468,"fmax":3474530,"strand":0},"feature":{"uniquename":"Group11.18","type":{"name":"region","cv":{"name":"sequence"}}}},"query":{"location":{"fmin":406,"fmax":426,"strand":0},"feature":{"uniquename":"query","type":{"name":"region","cv":{"name":"sequence"}}}},"rawscore":40.0}]}
            searchHit.setId(hit.get("subject").isObject().get("feature").isObject().get("uniquename").isString().stringValue());
            searchHit.setStart(Math.round(hit.get("subject").isObject().get("location").isObject().get("fmin").isNumber().doubleValue()));
            searchHit.setEnd(Math.round(hit.get("subject").isObject().get("location").isObject().get("fmax").isNumber().doubleValue()));
            searchHit.setStrand((int) Math.round(hit.get("subject").isObject().get("location").isObject().get("strand").isNumber().doubleValue()));
            searchHit.setScore(hit.get("rawscore").isNumber().doubleValue());
            searchHit.setSignificance(hit.get("significance").isNumber().doubleValue());
            searchHit.setIdentity(hit.get("identity").isNumber().doubleValue());
            searchHitList.add(searchHit);
          }
        } catch (Exception e) {
          Bootbox.alert("Unable to perform search" + e.getMessage() + " " + response.getText() + " " + response.getStatusCode());
        }
        dataGrid.getColumnSortList().clear();
        dataGrid.getColumnSortList().push(scoreColumn);
        ColumnSortEvent.fire(dataGrid, dataGrid.getColumnSortList());


      }

      @Override
      public void onError(Request request, Throwable exception) {
        Bootbox.alert("Problem doing search: " + exception.getMessage());
      }
    };
    String databaseId = null;
    if (!searchAllGenomes.getValue()) {
      databaseId = MainPanel.getCurrentSequence().getName();
    }
    String searchString = "";
    String[] lines = sequenceSearchBox.getValue().split("\n");
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].trim();
      if (line.indexOf('>') < 0) {
        searchString += line.trim();
      }
    }
    if (searchString.length() == 0) {
      Bootbox.alert("No sequence entered");
      return;
    }
    if (searchString.toUpperCase().matches(".*[^ACDEFGHIKLMNPQRSTVWXY].*")) {
      Bootbox.alert("The sequence should only contain non redundant IUPAC nucleotide or amino acid codes (except for N/X)");
      return;
    }

    loadingDialog.show();
    SearchRestService.searchSequence(requestCallback, searchTypeList.getSelectedValue(), searchString, databaseId);

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
          for (String key : searchTools.keySet()) {
            String name = searchTools.get(key).isObject().get("name").isString().stringValue();
            searchTypeList.addItem(name, key);
          }
        } catch (Exception e) {
          GWT.log("unable to find search tools " + e.getMessage() + " " + response.getText() + " " + response.getStatusCode());
        }

      }

      @Override
      public void onError(Request request, Throwable exception) {
        Bootbox.alert("Problem getting search tools: " + exception.getMessage());
      }
    });
    dataGrid.redraw();
  }

}
