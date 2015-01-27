package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.AbstractSafeHtmlRenderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.demo.DataGenerator;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.event.SequenceLoadEvent;
import org.bbop.apollo.gwt.client.event.SequenceLoadEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.OrganismRestService;
import org.bbop.apollo.gwt.client.rest.RestService;
import org.bbop.apollo.gwt.client.rest.SequenceRestService;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by ndunn on 12/17/14.
 */
public class SequencePanel extends Composite {
    interface SequencePanelUiBinder extends UiBinder<Widget, SequencePanel> {
    }

    private static SequencePanelUiBinder ourUiBinder = GWT.create(SequencePanelUiBinder.class);
    @UiField
    TextBox minFeatureLength;
    @UiField
    TextBox maxFeatureLength;
    @UiField
    ListBox organismList;

    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<SequenceInfo> dataGrid = new DataGrid<SequenceInfo>(10, tablecss);

    @UiField
    HTML sequenceName;
    @UiField
    HTML sequenceStart;
    @UiField
    HTML sequenceStop;
    @UiField
    Button exportGffButton;
    @UiField
    Button exportChadoButton;
    @UiField
    Button exportFastaButton;
    @UiField
    TextBox nameSearchBox;

    private ListDataProvider<SequenceInfo> dataProvider = new ListDataProvider<>();
    private List<SequenceInfo> sequenceInfoList = new ArrayList<>();
    private List<SequenceInfo> filteredSequenceList = dataProvider.getList();

    public SequencePanel() {
        initWidget(ourUiBinder.createAndBindUi(this));

        dataGrid.setWidth("100%");
        dataGrid.setEmptyTableWidget(new Label("Loading"));

//        final SelectionModel<SequenceInfo> selectionModel = new SingleSelectionModel<SequenceInfo>();

        TextColumn<SequenceInfo> nameColumn = new TextColumn<SequenceInfo>() {
            @Override
            public String getValue(SequenceInfo employee) {
                return employee.getName();
            }
        };
        nameColumn.setSortable(true);

        Column<SequenceInfo, Number> lengthColumn = new Column<SequenceInfo, Number>(new NumberCell()) {
            @Override
            public Integer getValue(SequenceInfo object) {
                return object.getLength();
            }
        };
        lengthColumn.setSortable(true);

        Column<SequenceInfo, Boolean> selectColumn = new Column<SequenceInfo, Boolean>(new CheckboxCell(true, false)) {

            @Override
            public Boolean getValue(SequenceInfo object) {
                return object.getSelected();
            }
        };
        selectColumn.setSortable(true);
//        thirdNameColumn.setSortable(true);


//        dataGrid.addColumn(selectColumn, "Select");
        dataGrid.addColumn(selectColumn, "");
        dataGrid.addColumn(nameColumn, "Name");
        dataGrid.addColumn(lengthColumn, "Length");

        dataGrid.setColumnWidth(0, "30px");

        dataProvider.addDataDisplay(dataGrid);


        SequenceRestService.loadSequences(sequenceInfoList);

        ColumnSortEvent.ListHandler<SequenceInfo> sortHandler = new ColumnSortEvent.ListHandler<SequenceInfo>(filteredSequenceList);
        dataGrid.addColumnSortHandler(sortHandler);
        sortHandler.setComparator(nameColumn, new Comparator<SequenceInfo>() {
            @Override
            public int compare(SequenceInfo o1, SequenceInfo o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        sortHandler.setComparator(lengthColumn, new Comparator<SequenceInfo>() {
            @Override
            public int compare(SequenceInfo o1, SequenceInfo o2) {
                return o1.getLength() - o2.getLength();
            }
        });

//        sortHandler.setComparator(thirdNameColumn, new Comparator<SequenceInfo>() {
//            @Override
//            public int compare(SequenceInfo o1, SequenceInfo o2) {
//                return o1.getType().compareTo(o2.getType());
//            }
//        });

        sequenceName.setHTML("LG1");
        sequenceStart.setHTML("100");
        sequenceStop.setHTML("4234");


//        DataGenerator.populateOrganismList(organismList);
        loadOrganisms(organismList);

        Annotator.eventBus.addHandler(SequenceLoadEvent.TYPE, new SequenceLoadEventHandler() {
            @Override
            public void onSequenceLoaded(SequenceLoadEvent sequenceLoadEvent) {
                filterSequences();
            }
        });

    }

    @UiHandler("nameSearchBox")
    public void handleNameSearch(KeyUpEvent keyUpEvent) {
        filterSequences();
    }

    private void filterSequences() {
        GWT.log("original size: "+sequenceInfoList.size());
        filteredSequenceList.clear();

        String nameText = nameSearchBox.getText().toLowerCase();

        if (nameText.length() > 0) {
            for (SequenceInfo sequenceInfo : sequenceInfoList) {
                if (sequenceInfo.getName().toLowerCase().contains(nameText)) {
                    filteredSequenceList.add(sequenceInfo);
                }
            }
        }
        else {
            filteredSequenceList.addAll(sequenceInfoList);
        }

        GWT.log("filtered size: " + filteredSequenceList.size());

    }

    /**
     * could use an organism callback . . . however, this element needs to use the callback directly.
     *
     * @param trackInfoList
     */
    public void loadOrganisms(final ListBox trackInfoList) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();

                trackInfoList.clear();
                for (int i = 0; i < array.size(); i++) {
                    JSONObject object = array.get(i).isObject();
//                    GWT.log(object.toString());
                    OrganismInfo organismInfo = new OrganismInfo();
                    organismInfo.setId(object.get("id").isNumber().toString());
                    organismInfo.setName(object.get("commonName").isString().stringValue());
                    organismInfo.setNumSequences((int) Math.round(object.get("sequences").isNumber().doubleValue()));
                    organismInfo.setDirectory(object.get("directory").isString().stringValue());
                    organismInfo.setNumFeatures(0);
                    organismInfo.setNumTracks(0);
//                    GWT.log(object.toString());
                    trackInfoList.addItem(organismInfo.getName(), organismInfo.getId());
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error loading organisms");
            }
        };

        OrganismRestService.loadOrganisms(requestCallback);

    }

    public void reload() {
        SequenceRestService.loadSequences(sequenceInfoList);
        dataGrid.redraw();
    }

}