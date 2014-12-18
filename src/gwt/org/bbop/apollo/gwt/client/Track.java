package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.EditTextCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.ui.*;
//import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.ListDataProvider;
import org.bbop.apollo.gwt.client.demo.DataGenerator;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
//import org.gwtbootstrap3.client.ui.gwt.DataGrid;

/**
 * Created by ndunn on 12/16/14.
 */
public class Track extends Composite {
    interface TrackUiBinder extends UiBinder<Widget, Track> {
    }

    private static TrackUiBinder ourUiBinder = GWT.create(TrackUiBinder.class);

    @UiField
    FlexTable configurationTable;
//    @UiField FlexTable trackTable;
    @UiField
    ListBox organismList;
    @UiField
    TextBox nameSearchBox;
    @UiField
    HTML trackName;
    @UiField
    HTML trackType;
    @UiField
    HTML trackCount;
    @UiField
    HTML trackDensity;
    @UiField(provided = false)
    CellTable<TrackInfo> dataGrid;



    //    @UiField(provided = true) org.gwtbootstrap3.client.ui.gwt.DataGrid<TrackInfo> dataGrid;
//    @UiField
//    DataGrid dataGrid;
//    @UiField(provided = true)
//    org.gwtbootstrap3.client.ui.gwt.DataGrid<TrackInfo> dataGrid;

//    private ListDataProvider<TrackInfo> dataProvider = new ListDataProvider<TrackInfo>();


    public Track() {

        Widget rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);

        configurationTable.setHTML(0, 0, "maxHeight"); ;
        configurationTable.setHTML(0, 1, "1000");
        configurationTable.setHTML(1, 0, "maxFeatureScreenDensity");
        configurationTable.setHTML(1, 1, "0.5");
        configurationTable.setHTML(2, 0, "maxDescriptionLength");
        configurationTable.setHTML(2, 1, "70");
        configurationTable.setHTML(3, 0, "label");
        configurationTable.setHTML(3, 1, "Cflo_OGSv3.3");

        configurationTable.setWidth("100%");



//        dataGrid = new CellTable<>();
        dataGrid.setWidth("100%");
        dataGrid.setAutoHeaderRefreshDisabled(true);

        // Set the message to display when the table is empty.
        dataGrid.setEmptyTableWidget(new Label("Loading"));

        Column<TrackInfo,Boolean> firstNameColumn = new Column<TrackInfo,Boolean>(new CheckboxCell(true,false)) {
            @Override
            public Boolean getValue(TrackInfo employee) {
                return employee.getVisible();
            }
        };

        firstNameColumn.setSortable(false);

        TextColumn<TrackInfo> secondNameColumn = new TextColumn<TrackInfo>() {
            @Override
            public String getValue(TrackInfo employee) {
                return employee.getName();
            }
        };

        secondNameColumn.setSortable(true);



        TextColumn<TrackInfo> thirdNameColumn = new TextColumn<TrackInfo>() {
            @Override
            public String getValue(TrackInfo employee) {
                return employee.getType();
            }
        };
        thirdNameColumn.setSortable(true);

        dataGrid.addColumn(firstNameColumn, "Visible");
        dataGrid.addColumn(secondNameColumn, "Name");
        dataGrid.addColumn(thirdNameColumn, "Type");


        ListDataProvider<TrackInfo> dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(dataGrid);

        List<TrackInfo> trackInfoList = dataProvider.getList();

        for(int i = 0 ; i < 30 ; i++){
            trackInfoList.add(new TrackInfo("Track"+i));
        }

        ColumnSortEvent.ListHandler<TrackInfo> sortHandler = new ColumnSortEvent.ListHandler<TrackInfo>(trackInfoList);
        dataGrid.addColumnSortHandler(sortHandler);
        sortHandler.setComparator(secondNameColumn, new Comparator<TrackInfo>() {
            @Override
            public int compare(TrackInfo o1, TrackInfo o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        sortHandler.setComparator(thirdNameColumn, new Comparator<TrackInfo>() {
            @Override
            public int compare(TrackInfo o1, TrackInfo o2) {
                return o1.getType().compareTo(o2.getType());
            }
        });

//
//        dataGrid.setRowCount(data.size(),true);
//        dataGrid.setRowData(0,data);

//        trackTable.setHTML(0, 0, "Show");
//        trackTable.setHTML(0,1,"Name");
//        trackTable.setHTML(0, 2, "Type");

//        for(int i = 0 ; i < 20 ; i++){
//            CheckBox checkBox = new CheckBox();
//            trackTable.setWidget(i,0, checkBox);
//            Hyperlink html = new Hyperlink();
//            html.setText("Track"+i);
//            trackTable.setWidget(i,1, html);
//            HTML typeHTML = new HTML("CanvasFeature");
//            trackTable.setWidget(i,2, typeHTML);
//        }
//
//        trackTable.getColumnFormatter().setWidth(0,"30px");
//        trackTable.getColumnFormatter().setWidth(1,"50%");
//        trackTable.setWidth("100%");
//        trackTable.setStyleName("trackTable");


        DataGenerator.populateOrganismList(organismList);

        trackName.setHTML("Track3");
        trackType.setHTML("CanvasFeature");
        trackCount.setHTML("34");
        trackDensity.setHTML("0.000123");

    }

    private IsWidget generateCheckBox(int i) {
        HorizontalPanel panel = new HorizontalPanel();
        CheckBox checkBox = new CheckBox();
        HTML html = new HTML("Track" + i);
        panel.add(checkBox);
        panel.add(html);
        return panel;
    }

}