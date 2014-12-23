package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.*;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.AbstractSafeHtmlRenderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import org.bbop.apollo.gwt.client.demo.DataGenerator;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.resources.TableResources;

import java.util.Comparator;
import java.util.List;

/**
 * Created by ndunn on 12/17/14.
 */
public class OrganismPanel extends Composite {



    interface OrganismBrowserPanelUiBinder extends UiBinder<Widget, OrganismPanel> {
    }

    private static OrganismBrowserPanelUiBinder ourUiBinder = GWT.create(OrganismBrowserPanelUiBinder.class);
    @UiField
    HTML organismName;
    @UiField
    HTML trackCount;
    @UiField
    HTML annotationCount;
    @UiField
    HTML sequenceFile;

    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<OrganismInfo> dataGrid = new DataGrid<OrganismInfo>(10, tablecss);


    private ListDataProvider<OrganismInfo> dataProvider = new ListDataProvider<>();

    public OrganismPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));

        TextColumn<OrganismInfo> organismNameColumn = new TextColumn<OrganismInfo>() {
            @Override
            public String getValue(OrganismInfo employee) {
                return employee.getName();
            }
        };
        organismNameColumn.setSortable(true);

        Column<OrganismInfo, Number> annotationsNameColumn = new Column<OrganismInfo, Number>(new NumberCell()) {
            @Override
            public Integer getValue(OrganismInfo object) {
                return object.getNumFeatures();
            }
        };
        annotationsNameColumn.setSortable(true);
        Column<OrganismInfo, Number> sequenceColumn = new Column<OrganismInfo, Number>(new NumberCell()) {
            @Override
            public Integer getValue(OrganismInfo object) {
                return object.getNumSequences();
            }
        };
        sequenceColumn.setSortable(true);
        Column<OrganismInfo, Number> tracksColumn = new Column<OrganismInfo, Number>(new NumberCell()) {
            @Override
            public Integer getValue(OrganismInfo object) {
                return object.getNumTracks();
            }
        };
        tracksColumn.setSortable(true);

        SafeHtmlRenderer<String> anchorRenderer = new AbstractSafeHtmlRenderer<String>() {
            @Override
            public SafeHtml render(String object) {
                SafeHtmlBuilder sb = new SafeHtmlBuilder();
                sb.appendHtmlConstant("<a href=\"javascript:;\">Select</a>");
//                sb.appendHtmlConstant("&nbsp;|&nbsp;<a href=\"javascript:;\">Export</a>");


//                sb.appendHtmlConstant("<div class='btn-group'>" +
//                        "  <button type='button' class='btn btn-default dropdown-toggle' data-toggle='dropdown' aria-expanded='false'>" +
//                        "    Action <span class='caret'></span>" +
//                        "  </button>" +
//                        "  <ul class='dropdown-menu' role='menu'>" +
//                        "    <li><a href='#'>Action</a></li>" +
//                        "    <li><a href='#'>Another action</a></li>" +
//                        "    <li><a href='#'>Something else here</a></li>" +
//                        "    <li class='divider'></li>" +
//                        "    <li><a href='#'>Separated link</a></li>" +
//                        "  </ul>" +
//                        "</div>");
                return sb.toSafeHtml();
            }
        };

        Column<OrganismInfo, String> actionColumn = new Column<OrganismInfo, String>(new ClickableTextCell(anchorRenderer)) {
            @Override
            public String getValue(OrganismInfo employee) {
                return "Select";
            }
        };

//        Column<OrganismInfo, org.gwtbootstrap3.client.ui.ButtonGroup> actionColumn =new Column<OrganismInfo, ButtonGroup>(new AbstractSafeHtmlCell(anchorRenderer)) {
//            @Override
//            public ButtonGroup getValue(OrganismInfo object) {
//                ButtonGroup buttonGroup = new ButtonGroup();
//                org.gwtbootstrap3.client.ui.Button actionButton = new Button();
//                actionButton.setText("Action");
//                return buttonGroup;
//            }
//        };


        dataGrid.addColumn(organismNameColumn, "Name");
        dataGrid.addColumn(annotationsNameColumn, "Annotations");
        dataGrid.addColumn(tracksColumn, "Tracks");
        dataGrid.addColumn(sequenceColumn, "Sequences");
        dataGrid.addColumn(actionColumn, "Action");


        dataProvider.addDataDisplay(dataGrid);

        List<OrganismInfo> trackInfoList = reloadOrganism();

        ColumnSortEvent.ListHandler<OrganismInfo> sortHandler = new ColumnSortEvent.ListHandler<OrganismInfo>(trackInfoList);
        dataGrid.addColumnSortHandler(sortHandler);
        sortHandler.setComparator(organismNameColumn, new Comparator<OrganismInfo>() {
            @Override
            public int compare(OrganismInfo o1, OrganismInfo o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        sortHandler.setComparator(annotationsNameColumn, new Comparator<OrganismInfo>() {
            @Override
            public int compare(OrganismInfo o1, OrganismInfo o2) {
                return o1.getNumFeatures() - o2.getNumFeatures();
            }
        });
        sortHandler.setComparator(sequenceColumn, new Comparator<OrganismInfo>() {
            @Override
            public int compare(OrganismInfo o1, OrganismInfo o2) {
                return o1.getNumSequences() - o2.getNumSequences();
            }
        });
        sortHandler.setComparator(tracksColumn, new Comparator<OrganismInfo>() {
            @Override
            public int compare(OrganismInfo o1, OrganismInfo o2) {
                return o1.getNumTracks() - o2.getNumTracks();
            }
        });

        organismName.setHTML("Zebrafish (Danio rerio)");
        trackCount.setHTML("30");
        annotationCount.setHTML("1223");
        sequenceFile.setHTML("/data/apollo/Zebrafish/jbrowse/data");

//        DataGenerator.populateOrganismTable(dataGrid);

    }

    public void loadOrganisms(final List<OrganismInfo> trackInfoList) {
        String url = "/apollo/organism/findAllOrganisms";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();
//                Window.alert("array size: "+array.size());

                for(int i = 0 ; i < array.size() ; i++){
                    JSONObject object = array.get(i).isObject();
//                    GWT.log(object.toString());
                    OrganismInfo organismInfo = new OrganismInfo();
                    organismInfo.setId(object.get("id").isNumber().toString());
                    organismInfo.setName(object.get("commonName").isString().stringValue());
                    organismInfo.setNumSequences(object.get("sequences").isArray().size());
                    organismInfo.setNumFeatures(0);
                    organismInfo.setNumTracks(0);
                    GWT.log(object.toString());
//                    object.isObject().get("")
//                    organismInfo.setName();

//                    Window.alert(object.toString());
                    trackInfoList.add(organismInfo);
                }

//                JSONObject jsonObject = returnValue.isObject();
//                Window.alert(response.getText());
//                        String queryString = jsonObject.get("query").isString().stringValue();

                // TODO: use proper array parsing
//                String resultString = jsonObject.get("result").isString().stringValue();
//                resultString = resultString.replace("[", "");
//                resultString = resultString.replace("]", "");
//                        searchResult.setText(" asdflkj asdflkjdas fsearch for " + queryString + " yields [" + resultString + "]");
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("ow");
            }
        };
        try {
            builder.setCallback(requestCallback);
            builder.send();
        } catch (RequestException e) {
            // Couldn't connect to server
            Window.alert(e.getMessage());
        }

    }

    public List<OrganismInfo> reloadOrganism() {
        List<OrganismInfo> trackInfoList = dataProvider.getList();
        trackInfoList.clear();
        loadOrganisms(trackInfoList);

        return trackInfoList;

//        for(String organism : DataGenerator.getOrganisms()){
//            trackInfoList.add(new OrganismInfo(organism));
//        }

    }
}