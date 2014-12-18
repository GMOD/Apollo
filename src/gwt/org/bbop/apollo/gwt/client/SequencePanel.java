package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
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
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import org.bbop.apollo.gwt.client.demo.DataGenerator;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;

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
    @UiField
    DataGrid<SequenceInfo> dataGrid;
    @UiField
    HTML sequenceName;
    @UiField
    HTML sequenceFile;
    @UiField
    HTML sequenceStart;
    @UiField
    HTML sequenceStop;

    public SequencePanel() {
        initWidget(ourUiBinder.createAndBindUi(this));

//        for(int i = 1; i < 22 ; i++){
//            DataGenerator.generateSequenceRow(dataGrid, i);
//        }



        dataGrid.setWidth("100%");
        dataGrid.setEmptyTableWidget(new Label("Loading"));

        TextColumn<SequenceInfo> firstNameColumn = new TextColumn<SequenceInfo>() {
            @Override
            public String getValue(SequenceInfo employee) {
                return employee.getName();
            }
        };
        firstNameColumn.setSortable(true);

        Column<SequenceInfo,Number> secondNameColumn = new Column<SequenceInfo,Number>(new NumberCell()) {
            @Override
            public Integer getValue(SequenceInfo object) {
                return object.getLength();
            }
        };
        secondNameColumn.setSortable(true);

        SafeHtmlRenderer<String> anchorRenderer = new AbstractSafeHtmlRenderer<String>() {
            @Override
            public SafeHtml render(String object) {
                SafeHtmlBuilder sb = new SafeHtmlBuilder();
                sb.appendHtmlConstant("(<a href=\"javascript:;\">").appendEscaped(object)
                        .appendHtmlConstant("</a>)");
                return sb.toSafeHtml();
            }
        };

        Column<SequenceInfo,String> thirdNameColumn = new Column<SequenceInfo, String>(new ClickableTextCell(anchorRenderer)) {
            @Override
            public String getValue(SequenceInfo employee) {
                return "Select";
            }
        };
//        thirdNameColumn.setSortable(true);



        dataGrid.addColumn(firstNameColumn, "Name");
        dataGrid.addColumn(secondNameColumn, "Length");
        dataGrid.addColumn(thirdNameColumn, "Type");


        ListDataProvider<SequenceInfo> dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(dataGrid);

        List<SequenceInfo> trackInfoList = dataProvider.getList();

        for(int i = 0 ; i < 50 ; i++){
            trackInfoList.add(new SequenceInfo(DataGenerator.SEQUENCE_PREFIX + i));
        }

        ColumnSortEvent.ListHandler<SequenceInfo> sortHandler = new ColumnSortEvent.ListHandler<SequenceInfo>(trackInfoList);
        dataGrid.addColumnSortHandler(sortHandler);
        sortHandler.setComparator(firstNameColumn, new Comparator<SequenceInfo>() {
            @Override
            public int compare(SequenceInfo o1, SequenceInfo o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        sortHandler.setComparator(secondNameColumn, new Comparator<SequenceInfo>() {
            @Override
            public int compare(SequenceInfo o1, SequenceInfo o2) {
                return o1.getLength()-o2.getLength();
            }
        });


//        sortHandler.setComparator(thirdNameColumn, new Comparator<SequenceInfo>() {
//            @Override
//            public int compare(SequenceInfo o1, SequenceInfo o2) {
//                return o1.getType().compareTo(o2.getType());
//            }
//        });

        sequenceName.setHTML("Group1.1");
        sequenceFile.setHTML("/data/apollo/annotations1/");
        sequenceStart.setHTML("100");
        sequenceStop.setHTML("4234");


        DataGenerator.populateOrganismList(organismList);


    }
}