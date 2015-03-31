package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.demo.DataGenerator;
import org.bbop.apollo.gwt.client.dto.GroupInfo;
import org.bbop.apollo.gwt.client.dto.UserInfo;
import org.bbop.apollo.gwt.client.event.GroupChangeEvent;
import org.bbop.apollo.gwt.client.event.GroupChangeEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.GroupRestService;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.TextBox;

import java.util.Comparator;
import java.util.List;

/**
 * Created by ndunn on 12/17/14.
 */
public class GroupPanel extends Composite {
    interface UserGroupBrowserPanelUiBinder extends UiBinder<Widget, GroupPanel> {
    }

    private static UserGroupBrowserPanelUiBinder ourUiBinder = GWT.create(UserGroupBrowserPanelUiBinder.class);
    @UiField
    TextBox name;

    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<GroupInfo> dataGrid = new DataGrid<GroupInfo>(10, tablecss);
    @UiField
    Button deleteButton;
    @UiField
    Button saveButton;
    @UiField
    Button cancelButton;
    @UiField
    Button createButton;
    //    @UiField(provided = true)
//    FlexTable userData = new DataGrid<UserInfo>(10,tablecss);
    @UiField
    FlexTable userData;

    //    @UiField
//    FlexTable trackPermissions;
    private ListDataProvider<GroupInfo> dataProvider = new ListDataProvider<>();
    private List<GroupInfo> groupInfoList = dataProvider.getList();
    private SingleSelectionModel<GroupInfo> selectionModel = new SingleSelectionModel<>();
    private GroupInfo selectedGroupInfo;
    private ColumnSortEvent.ListHandler<GroupInfo> sortHandler = new ColumnSortEvent.ListHandler<>(groupInfoList);

//    private ListDataProvider<UserInfo> userDataProvider = new ListDataProvider<>();
//    private List<UserInfo> userInfoList = userDataProvider.getList();
//    private ColumnSortEvent.ListHandler<UserInfo> userSortHandler = new ColumnSortEvent.ListHandler<>(userInfoList);

    public GroupPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));


        TextColumn<GroupInfo> firstNameColumn = new TextColumn<GroupInfo>() {
            @Override
            public String getValue(GroupInfo employee) {
                return employee.getName();
            }
        };
        firstNameColumn.setSortable(true);

        Column<GroupInfo, Number> secondNameColumn = new Column<GroupInfo, Number>(new NumberCell()) {
            @Override
            public Integer getValue(GroupInfo object) {
                return object.getNumberOfUsers();
            }
        };
        secondNameColumn.setSortable(true);

        dataGrid.addColumn(firstNameColumn, "Name");
        dataGrid.addColumn(secondNameColumn, "Users");

        dataProvider.addDataDisplay(dataGrid);
        dataGrid.setSelectionModel(selectionModel);


        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                setSelectedGroup();
            }
        });

//        List<GroupInfo> trackInfoList = dataProvider.getList();
//
//        for (String user : DataGenerator.getGroups()) {
//            trackInfoList.add(new GroupInfo(user));
//        }

        dataGrid.addColumnSortHandler(sortHandler);
        sortHandler.setComparator(firstNameColumn, new Comparator<GroupInfo>() {
            @Override
            public int compare(GroupInfo o1, GroupInfo o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        sortHandler.setComparator(secondNameColumn, new Comparator<GroupInfo>() {
            @Override
            public int compare(GroupInfo o1, GroupInfo o2) {
                return o1.getNumberOfUsers() - o2.getNumberOfUsers();
            }

        });


//        TextColumn<UserInfo> usernameColumn = new TextColumn<UserInfo>() {
//            @Override
//            public String getValue(UserInfo employee) {
//                return "Bob";
//            }
//        };
//        usernameColumn.setSortable(true);


//        userSortHandler.setComparator(usernameColumn, new Comparator<UserInfo>() {
//            @Override
//            public int compare(UserInfo o1, UserInfo o2) {
//                return o1.getName().compareToIgnoreCase(o2.getEmail());
//            }
//        });

//        userData.addColumn(usernameColumn, "User");
//        userData.addColumnSortHandler(userSortHandler);
//
//        userDataProvider.addDataDisplay(userData);


        Annotator.eventBus.addHandler(GroupChangeEvent.TYPE, new GroupChangeEventHandler() {
            @Override
            public void onGroupChanged(GroupChangeEvent userChangeEvent) {
                switch (userChangeEvent.getAction()) {
//                    case ADD_USER_TO_GROUP:
//                        availableGroupList.removeItem(availableGroupList.getSelectedIndex());
//                        if (availableGroupList.getItemCount() > 0) {
//                            availableGroupList.setSelectedIndex(0);
//                        }
//                        String group = userChangeEvent.getGroup();
//                        addGroupToUi(group);
//                        break;
                    case RELOAD_GROUPS:
                        selectedGroupInfo = null;
                        selectionModel.clear();
                        setSelectedGroup();
                        reload();
                        break;
                    case ADD_GROUP:
                        selectedGroupInfo = null;
                        selectionModel.clear();
                        setSelectedGroup();
                        reload();
                        saveButton.setVisible(false);
                        cancelButton.setVisible(false);
                        createButton.setEnabled(true);
                        break;
//                    case REMOVE_USER_FROM_GROUP:
//                        removeGroupFromUI(userChangeEvent.getGroup());
//                        break;

                }
            }
        });

        GroupRestService.loadGroups(groupInfoList);
    }

    @UiHandler("deleteButton")
    public void deleteGroup(ClickEvent clickEvent) {
        if (Window.confirm("Delete group " + selectedGroupInfo.getName() + "?")) {
            GroupRestService.deleteGroup(selectedGroupInfo);
            selectionModel.clear();
        }
    }

    @UiHandler("saveButton")
    public void saveGroup(ClickEvent clickEvent) {
        GroupInfo groupInfo = getGroupFromUI();
        GroupRestService.addNewGroup(groupInfo);
    }

    private GroupInfo getGroupFromUI() {
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setName(name.getText());
        return groupInfo;
    }

    @UiHandler("createButton")
    public void createGroup(ClickEvent clickEvent) {
        selectedGroupInfo = null;
        selectionModel.clear();

        cancelButton.setVisible(true);
        deleteButton.setVisible(false);
        saveButton.setVisible(true);
        createButton.setEnabled(false);
    }

    @UiHandler("cancelButton")
    public void cancelCreate(ClickEvent clickEvent) {
        name.setText("");
        cancelButton.setVisible(false);
        saveButton.setVisible(false);
        createButton.setEnabled(true);
    }

    @UiHandler("name")
    public void handleNameChange(ChangeEvent changeEvent) {
        if (selectedGroupInfo != null && selectedGroupInfo.getId() != null) {
            selectedGroupInfo.setName(name.getText());
            GroupRestService.updateGroup(selectedGroupInfo);
        }
    }

    private void setSelectedGroup() {
        selectedGroupInfo = selectionModel.getSelectedObject();

        if (selectedGroupInfo != null) {
            name.setText(selectedGroupInfo.getName());
            deleteButton.setVisible(true);


            userData.removeAllRows();

            for(UserInfo userInfo : selectedGroupInfo.getUserInfoList()){
                int rowCount = userData.getRowCount() ;
                userData.setHTML(rowCount,0,userInfo.getName());
            }
        } else {
            name.setText("");
            deleteButton.setVisible(false);
            userData.removeAllRows();
        }
    }

    public void reload() {
        GroupRestService.loadGroups(groupInfoList);
        dataGrid.redraw();
    }
}