package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
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
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
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
    DataGrid<SequenceInfo> dataGrid = new DataGrid<SequenceInfo>(20, tablecss);
    @UiField(provided = true)
    SimplePager pager = null;

    @UiField
    HTML sequenceName;
//    @UiField
//    HTML sequenceStart;
//    @UiField
//    HTML sequenceStop;
    @UiField
    Button exportGffButton;
    @UiField
    Button exportChadoButton;
    @UiField
    Button exportFastaButton;
    @UiField
    TextBox nameSearchBox;
    @UiField
    org.gwtbootstrap3.client.ui.Label viewableLabel;
    @UiField
    HTML sequenceLength;

    private ListDataProvider<SequenceInfo> dataProvider = new ListDataProvider<>();
    private List<SequenceInfo> sequenceInfoList = new ArrayList<>();
    private List<SequenceInfo> filteredSequenceList = dataProvider.getList();
    private SingleSelectionModel<SequenceInfo> singleSelectionModel = new SingleSelectionModel<SequenceInfo>();
    private SequenceInfo selectedSequenceInfo = null;

    public SequencePanel() {
        pager = new SimplePager(SimplePager.TextLocation.CENTER);
        initWidget(ourUiBinder.createAndBindUi(this));

        dataGrid.setWidth("100%");
        dataGrid.setEmptyTableWidget(new Label("Loading"));

        Column<SequenceInfo, Boolean> selectColumn = new Column<SequenceInfo, Boolean>(new CheckboxCell(true, false)) {
            @Override
            public Boolean getValue(SequenceInfo object) {
                return object.getSelected();
            }
        };
        selectColumn.setSortable(true);

        selectColumn.setFieldUpdater(new FieldUpdater<SequenceInfo, Boolean>() {
            @Override
            public void update(int index, SequenceInfo object, Boolean value) {
                object.setSelected(value);
            }
        });

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


        dataGrid.addColumn(selectColumn, "Selected");
        dataGrid.addColumn(nameColumn, "Name");
        dataGrid.addColumn(lengthColumn, "Length");

        dataGrid.setColumnWidth(0, "50px");

        dataGrid.setSelectionModel(singleSelectionModel);
        singleSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                setSequenceInfo(singleSelectionModel.getSelectedObject());
            }
        });

        dataProvider.addDataDisplay(dataGrid);
        pager.setDisplay(dataGrid);


        SequenceRestService.loadSequences(sequenceInfoList, MainPanel.currentOrganismId);

        ColumnSortEvent.ListHandler<SequenceInfo> sortHandler = new ColumnSortEvent.ListHandler<SequenceInfo>(filteredSequenceList);
        dataGrid.addColumnSortHandler(sortHandler);

        sortHandler.setComparator(selectColumn, new Comparator<SequenceInfo>() {
                    @Override
                    public int compare(SequenceInfo o1, SequenceInfo o2) {
                        return o1.getSelected().compareTo(o2.getSelected());
                    }
                }
        );
        sortHandler.setComparator(nameColumn, new Comparator<SequenceInfo>()  {
                    @Override
                    public int compare(SequenceInfo o1, SequenceInfo o2) {
                        return o1.compareTo(o2);
                    }
                }
        );
        sortHandler.setComparator(lengthColumn, new Comparator<SequenceInfo>()  {
                    @Override
                    public int compare(SequenceInfo o1, SequenceInfo o2) {
                        return o1.getLength() - o2.getLength();
                    }
                }
        );

//        sortHandler.setComparator(thirdNameColumn, new Comparator<SequenceInfo>() {
//            @Override
//            public int compare(SequenceInfo o1, SequenceInfo o2) {
//                return o1.getType().compareTo(o2.getType());
//            }
//        });

//        sequenceName.setHTML("LG1");
//        sequenceStart.setHTML("100");
//        sequenceStop.setHTML("4234");


        //        DataGenerator.populateOrganismList(organismList);
        loadOrganisms(organismList);

        Annotator.eventBus.addHandler(SequenceLoadEvent.TYPE, new

                        SequenceLoadEventHandler() {
                            @Override
                            public void onSequenceLoaded(SequenceLoadEvent sequenceLoadEvent) {
                                filterSequences();
//                dataGrid.redraw();
                            }
                        }

        );

    }

    private void setSequenceInfo(SequenceInfo selectedObject) {
        selectedSequenceInfo = selectedObject;
        if (selectedSequenceInfo == null) {
            sequenceName.setText("");
//            sequenceStart.setText("");
//            sequenceStop.setText("");
            sequenceLength.setText("");
//            trackCount.setText("");
//            trackDensity.setText("");
        } else {
            sequenceName.setHTML(selectedSequenceInfo.getName());
//            sequenceStart.setHTML(selectedSequenceInfo.getStart().toString());
//            sequenceStop.setHTML(selectedSequenceInfo.getEnd().toString());
            sequenceLength.setText(selectedSequenceInfo.getLength().toString());
        }
    }

    @UiHandler(value = {"nameSearchBox", "minFeatureLength", "maxFeatureLength"})
    public void handleNameSearch(KeyUpEvent keyUpEvent) {
        filterSequences();
    }


    @UiHandler(value = {"organismList"})
    public void handleOrganismChange(ChangeEvent changeEvent) {
        reload();
    }


    private void filterSequences() {
        GWT.log("original size: " + sequenceInfoList.size());
        filteredSequenceList.clear();

        String nameText = nameSearchBox.getText().toLowerCase();
        String minLengthText = minFeatureLength.getText();
        String maxLengthText = maxFeatureLength.getText();
        Long minLength = Long.MIN_VALUE;
        Long maxLength = Long.MAX_VALUE;


        if (minLengthText.length() > 0) {
            minLength = Long.parseLong(minLengthText);
        }

        if (maxLengthText.length() > 0) {
            maxLength = Long.parseLong(maxLengthText);
        }

        for (SequenceInfo sequenceInfo : sequenceInfoList) {
            if (sequenceInfo.getName().toLowerCase().contains(nameText)
                    && sequenceInfo.getLength() >= minLength
                    && sequenceInfo.getLength() <= maxLength
                    ) {
                filteredSequenceList.add(sequenceInfo);
            }
        }
//        else {
//            filteredSequenceList.addAll(sequenceInfoList);
//        }

        GWT.log("filtered size: " + filteredSequenceList.size());
        viewableLabel.setText(filteredSequenceList.size() + "");

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
        GWT.log("item count: " + organismList.getItemCount());
        if (organismList.getItemCount() > 0) {
            Long organismListId = Long.parseLong(organismList.getSelectedValue());
            GWT.log("list id: " + organismListId);
            SequenceRestService.loadSequences(sequenceInfoList, organismListId);
        } else {
            SequenceRestService.loadSequences(sequenceInfoList, MainPanel.currentOrganismId);
        }
    }

}