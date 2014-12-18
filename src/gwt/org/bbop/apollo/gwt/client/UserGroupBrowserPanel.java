package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import org.bbop.apollo.gwt.client.demo.DataGenerator;
import org.bbop.apollo.gwt.client.dto.UserGroupInfo;

import java.util.Comparator;
import java.util.List;

/**
 * Created by ndunn on 12/17/14.
 */
public class UserGroupBrowserPanel extends Composite {
    interface UserGroupBrowserPanelUiBinder extends UiBinder<Widget, UserGroupBrowserPanel> {
    }

    private static UserGroupBrowserPanelUiBinder ourUiBinder = GWT.create(UserGroupBrowserPanelUiBinder.class);
    @UiField
    HTML name;
    @UiField
    DataGrid dataGrid;

    public UserGroupBrowserPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));

        name.setHTML("USDA #1");


        TextColumn<UserGroupInfo> firstNameColumn = new TextColumn<UserGroupInfo>() {
            @Override
            public String getValue(UserGroupInfo employee) {
                return employee.getName();
            }
        };
        firstNameColumn.setSortable(true);

        Column<UserGroupInfo,Number> secondNameColumn = new Column<UserGroupInfo,Number>(new NumberCell()) {
            @Override
            public Integer getValue(UserGroupInfo object) {
                return object.getNumberOfUsers();
            }
        };
        secondNameColumn.setSortable(true);

        dataGrid.addColumn(firstNameColumn, "Name");
        dataGrid.addColumn(secondNameColumn, "Users");

        ListDataProvider<UserGroupInfo> dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(dataGrid);

        List<UserGroupInfo> trackInfoList = dataProvider.getList();

        for(String user : DataGenerator.getGroups()){
            trackInfoList.add(new UserGroupInfo(user));
        }

        ColumnSortEvent.ListHandler<UserGroupInfo> sortHandler = new ColumnSortEvent.ListHandler<UserGroupInfo>(trackInfoList);
        dataGrid.addColumnSortHandler(sortHandler);
        sortHandler.setComparator(firstNameColumn, new Comparator<UserGroupInfo>() {
            @Override
            public int compare(UserGroupInfo o1, UserGroupInfo o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        sortHandler.setComparator(secondNameColumn, new Comparator<UserGroupInfo>() {
            @Override
            public int compare(UserGroupInfo o1, UserGroupInfo o2) {
                return o1.getNumberOfUsers()-o2.getNumberOfUsers();
            }
        });
    }
}