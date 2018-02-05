package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.GroupInfo;
import org.bbop.apollo.gwt.client.dto.GroupOrganismPermissionInfo;
import org.bbop.apollo.gwt.client.dto.UserInfo;
import org.bbop.apollo.gwt.client.dto.UserOrganismPermissionInfo;
import org.bbop.apollo.gwt.client.event.GroupChangeEvent;
import org.bbop.apollo.gwt.client.event.GroupChangeEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.GroupRestService;
import org.bbop.apollo.gwt.client.rest.UserRestService;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.bootbox.client.callback.ConfirmCallback;
import org.gwtbootstrap3.extras.select.client.ui.MultipleSelect;
import org.gwtbootstrap3.extras.select.client.ui.Option;
import org.gwtbootstrap3.extras.select.client.ui.Select;

import java.util.ArrayList;
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
    Button createButton;
    @UiField
    TabLayoutPanel userDetailTab;
    //    @UiField
//    FlexTable userData;
    @UiField(provided = true)
    WebApolloSimplePager pager = new WebApolloSimplePager(WebApolloSimplePager.TextLocation.CENTER);
    @UiField(provided = true)
    WebApolloSimplePager organismPager = new WebApolloSimplePager(WebApolloSimplePager.TextLocation.CENTER);
    @UiField(provided = true)
    DataGrid<GroupOrganismPermissionInfo> organismPermissionsGrid = new DataGrid<>(4, tablecss);
    @UiField
    TextBox createGroupField;
    @UiField
    Button saveButton;
    @UiField
    Button cancelButton;
    @UiField
    Button updateButton;
    @UiField
    Button cancelUpdateButton;
    @UiField
    MultipleSelect availableUsers;
    @UiField
    Button updateUsers;
    @UiField
    MultipleSelect availableGroupAdmin;
    @UiField
    Button updateGroupAdmin;

    private ListDataProvider<GroupInfo> dataProvider = new ListDataProvider<>();
    private List<GroupInfo> groupInfoList = dataProvider.getList();
    private SingleSelectionModel<GroupInfo> selectionModel = new SingleSelectionModel<>();
    private GroupInfo selectedGroupInfo;
    private ColumnSortEvent.ListHandler<GroupInfo> groupSortHandler = new ColumnSortEvent.ListHandler<>(groupInfoList);
    private List<UserInfo> allUsersList = new ArrayList<>();


    private ListDataProvider<GroupOrganismPermissionInfo> permissionProvider = new ListDataProvider<>();
    private List<GroupOrganismPermissionInfo> permissionProviderList = permissionProvider.getList();
    private ColumnSortEvent.ListHandler<GroupOrganismPermissionInfo> sortHandler = new ColumnSortEvent.ListHandler<GroupOrganismPermissionInfo>(permissionProviderList);

    public GroupPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
        availableUsers.getElement().setAttribute("data-dropup-auto", Boolean.toString(false));
        availableGroupAdmin.getElement().setAttribute("data-dropup-auto", Boolean.toString(false));

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
        organismPager.setDisplay(organismPermissionsGrid);
        pager.setDisplay(dataGrid);


        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                setSelectedGroup();
            }
        });

        dataGrid.addColumnSortHandler(groupSortHandler);
        groupSortHandler.setComparator(firstNameColumn, new Comparator<GroupInfo>() {
            @Override
            public int compare(GroupInfo o1, GroupInfo o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        groupSortHandler.setComparator(secondNameColumn, new Comparator<GroupInfo>() {
            @Override
            public int compare(GroupInfo o1, GroupInfo o2) {
                return o1.getNumberOfUsers() - o2.getNumberOfUsers();
            }

        });


        createOrganismPermissionsTable();


        Annotator.eventBus.addHandler(GroupChangeEvent.TYPE, new GroupChangeEventHandler() {
            @Override
            public void onGroupChanged(GroupChangeEvent userChangeEvent) {
                switch (userChangeEvent.getAction()) {
                    case RELOAD_GROUPS:
                        selectedGroupInfo = null;
                        selectionModel.clear();
                        setSelectedGroup();
                        reload();
                        break;
                    case ADD_GROUP:
                    case REMOVE_GROUP:
                        selectedGroupInfo = null;
                        selectionModel.clear();
                        setSelectedGroup();
                        reload();
                        cancelAddState();
                        break;
                    case GROUPS_RELOADED:
                        selectedGroupInfo = null;
                        selectionModel.clear();
                        break;


                }
            }
        });

        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (MainPanel.getInstance().getCurrentUser() != null) {
                    if(MainPanel.getInstance().isCurrentUserInstructorOrBetter())  {
                        GroupRestService.loadGroups(groupInfoList);
                        UserRestService.loadUsers(allUsersList);
                    }
                    return false;
                }
                return true;
            }
        }, 100);
    }

    @UiHandler("updateUsers")
    public void updateUsers(ClickEvent clickEvent) {
        List<Option> selectedValues = availableUsers.getSelectedItems();
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                selectedGroupInfo = null;
                selectionModel.clear();
                setSelectedGroup();
                reload();
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Failed to update users: " + exception.fillInStackTrace().toString());
            }
        };
        GroupRestService.updateUserGroups(requestCallback, selectedGroupInfo, selectedValues);
    }

    @UiHandler("updateGroupAdmin")
    public void UpdateGroupAdmin(ClickEvent clickEvent) {
        List<Option> selectedValues = availableGroupAdmin.getSelectedItems();
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                selectedGroupInfo = null;
                selectionModel.clear();
                setSelectedGroup();
                reload();
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Failed to update group admin: " + exception.fillInStackTrace().toString());
            }
        };
        GroupRestService.updateGroupAdmin(requestCallback, selectedGroupInfo, selectedValues);
    }

    @UiHandler("deleteButton")
    public void deleteGroup(ClickEvent clickEvent) {
        Integer numberOfUsers = selectedGroupInfo.getNumberOfUsers();
        if (numberOfUsers > 0) {
            Bootbox.confirm("Group '" + selectedGroupInfo.getName() + "' has " + numberOfUsers + " associated with it.  Still remove?", new ConfirmCallback() {
                @Override
                public void callback(boolean result) {
                    if (result) {
                        GroupRestService.deleteGroup(selectedGroupInfo);
                        selectionModel.clear();
                    }
                }
            });
        } else {
            Bootbox.confirm("Remove group '" + selectedGroupInfo.getName() + "'?", new ConfirmCallback() {
                @Override
                public void callback(boolean result) {
                    if (result) {
                        GroupRestService.deleteGroup(selectedGroupInfo);
                        selectionModel.clear();
                    }
                }
            });
        }
    }


    private GroupInfo getGroupFromUI() {
        String groupName = name.getText().trim();
        if (groupName.length() < 3) {
            Bootbox.alert("Group must be at least 3 characters long");
            return null;
        }
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setName(groupName);
        return groupInfo;
    }

    @UiHandler("userDetailTab")
    void onTabSelection(SelectionEvent<Integer> event) {
        organismPermissionsGrid.redraw();

    }

    @UiHandler("cancelButton")
    public void cancelNewGroup(ClickEvent clickEvent) {
        cancelAddState();
    }

    @UiHandler("saveButton")
    public void saveNewGroup(ClickEvent clickEvent) {
        String groupName = createGroupField.getText().trim();
        if (validateName(groupName)) {
            GroupInfo groupInfo = new GroupInfo();
            groupInfo.setName(groupName);
            GroupRestService.addNewGroup(groupInfo);
        }
    }

    void cancelAddState() {
        createButton.setVisible(true);
        createGroupField.setVisible(false);
        saveButton.setVisible(false);
        cancelButton.setVisible(false);
        createGroupField.setText("");
    }

    void setAddState() {
        createButton.setVisible(false);
        createGroupField.setVisible(true);
        saveButton.setVisible(true);
        cancelButton.setVisible(true);
        createGroupField.setText("");
    }

    @UiHandler("createButton")
    public void createGroup(ClickEvent clickEvent) {
        setAddState();
    }

    private Boolean validateName(String groupName) {
        if (groupName.length() < 3) {
            Bootbox.alert("Group must be at least 3 characters long");
            return false;
        }
        for (GroupInfo groupInfo : groupInfoList) {
            if (groupName.equals(groupInfo.getName())) {
                Bootbox.alert("Group name must be unique");
                return false;
            }
        }

        return true;
    }

    @UiHandler("updateButton")
    public void updateGroupName(ClickEvent clickEvent) {
        if (selectedGroupInfo != null && selectedGroupInfo.getId() != null) {
            String groupName = name.getText().trim();
            if (validateName(groupName)) {
                selectedGroupInfo.setName(groupName);
                Bootbox.alert("Saving Group '" + groupName + "'");
                GroupRestService.updateGroup(selectedGroupInfo);
            }
        }
    }

    @UiHandler("cancelUpdateButton")
    public void cancelUpdateGroupName(ClickEvent clickEvent) {
        name.setText(selectedGroupInfo.getName());
        handleNameChange(null);
    }

    @UiHandler("name")
    public void handleNameChange(KeyUpEvent changeEvent) {
        String newName = name.getText().trim();
        String originalName = selectedGroupInfo.getName();

        updateButton.setEnabled(newName.length() >= 3 && !newName.equals(originalName));

    }

    private void setSelectedGroup() {
        selectedGroupInfo = selectionModel.getSelectedObject();

        permissionProviderList.clear();
        if (selectedGroupInfo != null) {
            name.setText(selectedGroupInfo.getName());
            deleteButton.setVisible(true);
            availableUsers.clear();
            availableGroupAdmin.clear();
//            userData.removeAllRows();

            List<String> optionsList = new ArrayList<>();
            for (UserInfo userInfo : selectedGroupInfo.getUserInfoList()) {
                Option option = new Option();
                option.setText(userInfo.getName() + " (" + userInfo.getEmail() + ")");
                optionsList.add(option.getValue());
            }

            List<String> adminOptionsList = new ArrayList<>();
            if (selectedGroupInfo.getAdminInfoList() != null) {
                for (UserInfo userInfo : selectedGroupInfo.getAdminInfoList()) {
                    Option option = new Option();
                    option.setText(userInfo.getName() + " (" + userInfo.getEmail() + ")");
                    adminOptionsList.add(option.getValue());
                }
            }

            for (UserInfo userInfo : allUsersList) {
                Option option = new Option();
                Option option2 = new Option();
                option.setText(userInfo.getName() + " (" + userInfo.getEmail() + ")");
                option2.setText(userInfo.getName() + " (" + userInfo.getEmail() + ")");
                availableUsers.add(option);
                availableGroupAdmin.add(option2);
            }


            availableUsers.setValue(optionsList);
            availableUsers.refresh();
            availableGroupAdmin.setValue(adminOptionsList);
            availableGroupAdmin.refresh();

            // only show organisms that this user is an admin on . . . https://github.com/GMOD/Apollo/issues/540
            if (MainPanel.getInstance().isCurrentUserAdmin()) {
                permissionProviderList.addAll(selectedGroupInfo.getOrganismPermissionMap().values());
            } else {
                List<String> organismsToShow = new ArrayList<>();
                for (UserOrganismPermissionInfo userOrganismPermission : MainPanel.getInstance().getCurrentUser().getOrganismPermissionMap().values()) {
                    if (userOrganismPermission.isAdmin()) {
                        organismsToShow.add(userOrganismPermission.getOrganismName());
                    }
                }

                for (GroupOrganismPermissionInfo userOrganismPermission : selectedGroupInfo.getOrganismPermissionMap().values()) {
                    if (organismsToShow.contains(userOrganismPermission.getOrganismName())) {
                        permissionProviderList.add(userOrganismPermission);
                    }
                }
            }
            userDetailTab.setVisible(true);
        } else {
            name.setText("");
            deleteButton.setVisible(false);
            userDetailTab.setVisible(false);
        }
    }


    public void reload() {
        if (MainPanel.getInstance().getCurrentUser() != null) {
            GroupRestService.loadGroups(groupInfoList);
        }
    }


    private void createOrganismPermissionsTable() {
        TextColumn<GroupOrganismPermissionInfo> organismNameColumn = new TextColumn<GroupOrganismPermissionInfo>() {
            @Override
            public String getValue(GroupOrganismPermissionInfo userOrganismPermissionInfo) {
                return userOrganismPermissionInfo.getOrganismName();
            }
        };
        organismNameColumn.setSortable(true);
        organismNameColumn.setDefaultSortAscending(true);


        Column<GroupOrganismPermissionInfo, Boolean> adminColumn = new Column<GroupOrganismPermissionInfo, Boolean>(new CheckboxCell(true, true)) {
            @Override
            public Boolean getValue(GroupOrganismPermissionInfo object) {
                return object.isAdmin();
            }
        };
        adminColumn.setSortable(true);
        adminColumn.setFieldUpdater(new FieldUpdater<GroupOrganismPermissionInfo, Boolean>() {
            @Override
            public void update(int index, GroupOrganismPermissionInfo object, Boolean value) {
                object.setAdmin(value);
                GroupRestService.updateOrganismPermission(object);
            }
        });
        sortHandler.setComparator(adminColumn, new Comparator<GroupOrganismPermissionInfo>() {
            @Override
            public int compare(GroupOrganismPermissionInfo o1, GroupOrganismPermissionInfo o2) {
                return o1.isAdmin().compareTo(o2.isAdmin());
            }
        });

        organismPermissionsGrid.setEmptyTableWidget(new Label("Please select a group to view the group's organism permissions"));
        organismPermissionsGrid.addColumnSortHandler(sortHandler);
        sortHandler.setComparator(organismNameColumn, new Comparator<GroupOrganismPermissionInfo>() {
            @Override
            public int compare(GroupOrganismPermissionInfo o1, GroupOrganismPermissionInfo o2) {
                return o1.getOrganismName().compareTo(o2.getOrganismName());
            }
        });

        Column<GroupOrganismPermissionInfo, Boolean> writeColumn = new Column<GroupOrganismPermissionInfo, Boolean>(new CheckboxCell(true, true)) {
            @Override
            public Boolean getValue(GroupOrganismPermissionInfo object) {
                return object.isWrite();
            }
        };
        writeColumn.setSortable(true);
        writeColumn.setFieldUpdater(new FieldUpdater<GroupOrganismPermissionInfo, Boolean>() {
            @Override
            public void update(int index, GroupOrganismPermissionInfo object, Boolean value) {
                object.setWrite(value);
                object.setGroupId(selectedGroupInfo.getId());
                GroupRestService.updateOrganismPermission(object);
            }
        });
        sortHandler.setComparator(writeColumn, new Comparator<GroupOrganismPermissionInfo>() {
            @Override
            public int compare(GroupOrganismPermissionInfo o1, GroupOrganismPermissionInfo o2) {
                return o1.isWrite().compareTo(o2.isWrite());
            }
        });

        Column<GroupOrganismPermissionInfo, Boolean> exportColumn = new Column<GroupOrganismPermissionInfo, Boolean>(new CheckboxCell(true, true)) {
            @Override
            public Boolean getValue(GroupOrganismPermissionInfo object) {
                return object.isExport();
            }
        };
        exportColumn.setSortable(true);
        exportColumn.setFieldUpdater(new FieldUpdater<GroupOrganismPermissionInfo, Boolean>() {
            @Override
            public void update(int index, GroupOrganismPermissionInfo object, Boolean value) {
                object.setExport(value);
                GroupRestService.updateOrganismPermission(object);
            }
        });
        sortHandler.setComparator(exportColumn, new Comparator<GroupOrganismPermissionInfo>() {
            @Override
            public int compare(GroupOrganismPermissionInfo o1, GroupOrganismPermissionInfo o2) {
                return o1.isExport().compareTo(o2.isExport());
            }
        });

        Column<GroupOrganismPermissionInfo, Boolean> readColumn = new Column<GroupOrganismPermissionInfo, Boolean>(new CheckboxCell(true, true)) {
            @Override
            public Boolean getValue(GroupOrganismPermissionInfo object) {
                return object.isRead();
            }
        };
        readColumn.setSortable(true);
        readColumn.setFieldUpdater(new FieldUpdater<GroupOrganismPermissionInfo, Boolean>() {
            @Override
            public void update(int index, GroupOrganismPermissionInfo object, Boolean value) {
                object.setRead(value);
                GroupRestService.updateOrganismPermission(object);
            }
        });
        sortHandler.setComparator(readColumn, new Comparator<GroupOrganismPermissionInfo>() {
            @Override
            public int compare(GroupOrganismPermissionInfo o1, GroupOrganismPermissionInfo o2) {
                return o1.isRead().compareTo(o2.isRead());
            }
        });


        organismPermissionsGrid.addColumn(organismNameColumn, "Name");
        organismPermissionsGrid.addColumn(adminColumn, "Admin");
        organismPermissionsGrid.addColumn(writeColumn, "Write");
        organismPermissionsGrid.addColumn(exportColumn, "Export");
        organismPermissionsGrid.addColumn(readColumn, "Read");
        permissionProvider.addDataDisplay(organismPermissionsGrid);

    }
}
