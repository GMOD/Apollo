package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.*;
import org.bbop.apollo.gwt.client.dto.UserInfo;
import org.bbop.apollo.gwt.client.dto.UserInfoConverter;
import org.bbop.apollo.gwt.client.dto.UserOrganismPermissionInfo;
import org.bbop.apollo.gwt.client.event.UserChangeEvent;
import org.bbop.apollo.gwt.client.event.UserChangeEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.UserRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.bbop.apollo.gwt.shared.GlobalPermissionEnum;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.Row;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.bootbox.client.callback.ConfirmCallback;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by ndunn on 12/17/14.
 */
public class UserPanel extends Composite {

    interface UserBrowserPanelUiBinder extends UiBinder<Widget, UserPanel> {
    }

    private static UserBrowserPanelUiBinder ourUiBinder = GWT.create(UserBrowserPanelUiBinder.class);
    @UiField
    org.gwtbootstrap3.client.ui.TextBox firstName;
    @UiField
    org.gwtbootstrap3.client.ui.TextBox lastName;
    @UiField
    org.gwtbootstrap3.client.ui.TextBox email;

    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<UserInfo> dataGrid = new DataGrid<UserInfo>(20, tablecss);
    @UiField
    org.gwtbootstrap3.client.ui.Button createButton;
    @UiField
    org.gwtbootstrap3.client.ui.Button cancelButton;
    @UiField
    org.gwtbootstrap3.client.ui.Button deleteButton;
    @UiField
    org.gwtbootstrap3.client.ui.Button saveButton;
    @UiField
    Input passwordTextBox;
    @UiField
    Row passwordRow;
    @UiField
    ListBox roleList;
    @UiField
    FlexTable groupTable;
    @UiField
    org.gwtbootstrap3.client.ui.ListBox availableGroupList;
    @UiField
    org.gwtbootstrap3.client.ui.Button addGroupButton;
    @UiField
    TabLayoutPanel userDetailTab;
    @UiField(provided = true)
    DataGrid<UserOrganismPermissionInfo> organismPermissionsGrid = new DataGrid<>(4, tablecss);
    @UiField(provided = true)
    WebApolloSimplePager pager = new WebApolloSimplePager(WebApolloSimplePager.TextLocation.CENTER);
    @UiField(provided = true)
    WebApolloSimplePager organismPager = new WebApolloSimplePager(WebApolloSimplePager.TextLocation.CENTER);
    @UiField
    org.gwtbootstrap3.client.ui.TextBox nameSearchBox;
    @UiField
    Row userRow1;
    @UiField
    Row userRow2;
    @UiField
    org.gwtbootstrap3.client.ui.Label saveLabel;


    private AsyncDataProvider<UserInfo> dataProvider;
    private List<UserInfo> userInfoList = new ArrayList<>();
    private SingleSelectionModel<UserInfo> selectionModel = new SingleSelectionModel<>();
    private UserInfo selectedUserInfo;

    private ListDataProvider<UserOrganismPermissionInfo> permissionProvider = new ListDataProvider<>();
    private List<UserOrganismPermissionInfo> permissionProviderList = permissionProvider.getList();
    private ColumnSortEvent.ListHandler<UserOrganismPermissionInfo> sortHandler = new ColumnSortEvent.ListHandler<UserOrganismPermissionInfo>(permissionProviderList);


    public UserPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));


        TextColumn<UserInfo> firstNameColumn = new TextColumn<UserInfo>() {
            @Override
            public String getValue(UserInfo user) {
                return user.getName();
            }
        };
        firstNameColumn.setSortable(true);

        SafeHtmlRenderer<String> anchorRenderer = new AbstractSafeHtmlRenderer<String>() {
            @Override
            public SafeHtml render(String object) {
                SafeHtmlBuilder sb = new SafeHtmlBuilder();
                sb.appendHtmlConstant("<a href=\"javascript:;\">").appendEscaped(object)
                        .appendHtmlConstant("</a>");
                return sb.toSafeHtml();
            }
        };

        Column<UserInfo, String> secondNameColumn = new Column<UserInfo, String>(new ClickableTextCell(anchorRenderer)) {
            @Override
            public String getValue(UserInfo user) {
                return user.getEmail();
            }
        };
        secondNameColumn.setSortable(true);

        TextColumn<UserInfo> thirdNameColumn = new TextColumn<UserInfo>() {
            @Override
            public String getValue(UserInfo user) {
                return user.getRole();
            }
        };
        thirdNameColumn.setSortable(false);

        dataGrid.addColumn(firstNameColumn, "Name");
        dataGrid.addColumn(secondNameColumn, "Email");
        dataGrid.addColumn(thirdNameColumn, "Global Role");

        dataGrid.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                selectedUserInfo = selectionModel.getSelectedObject();
                updateUserInfo();
            }
        });
        createOrganismPermissionsTable();

        dataProvider = new AsyncDataProvider<UserInfo>() {
            @Override
            protected void onRangeChanged(HasData<UserInfo> display) {
                final Range range = display.getVisibleRange();
                final ColumnSortList sortList = dataGrid.getColumnSortList();
                final int start = range.getStart();
                final int length = range.getLength();

                RequestCallback requestCallback = new RequestCallback() {
                    @Override
                    public void onResponseReceived(Request request, Response response) {
                        JSONArray jsonArray = JSONParser.parseLenient(response.getText()).isArray();
                        Integer userCount = 0;
                        if (jsonArray != null && jsonArray.size() > 0) {
                            JSONObject jsonObject = jsonArray.get(0).isObject();
                            userCount = (int) jsonObject.get("userCount").isNumber().doubleValue();
                            if (jsonObject.containsKey("searchName") && jsonObject.get("searchName").isString() != null) {
                                String searchName = jsonObject.get("searchName").isString().stringValue();
                                if (searchName.trim().length() > 0 && !searchName.trim().equals(nameSearchBox.getText().trim())) {
                                    return;
                                }
                            }
                        }
                        dataGrid.setRowCount(userCount, true);
                        dataGrid.setRowData(start, UserInfoConverter.convertFromJsonArray(jsonArray));
                    }

                    @Override
                    public void onError(Request request, Throwable exception) {
                        Bootbox.alert("error getting sequence info: " + exception);
                    }
                };


                ColumnSortList.ColumnSortInfo nameSortInfo = sortList.get(0);
                if (nameSortInfo.getColumn().isSortable()) {
                    Column<UserInfo, ?> sortColumn = (Column<UserInfo, ?>) sortList.get(0).getColumn();
                    Integer columnIndex = dataGrid.getColumnIndex(sortColumn);
                    String searchColumnString = columnIndex == 0 ? "name" : columnIndex == 1 ? "email" : "";
                    Boolean sortNameAscending = nameSortInfo.isAscending();
                    UserRestService.loadUsers(requestCallback, start, length, nameSearchBox.getText(), searchColumnString, sortNameAscending);
                }
            }
        };


        ColumnSortEvent.AsyncHandler columnSortHandler = new ColumnSortEvent.AsyncHandler(dataGrid);
        dataGrid.addColumnSortHandler(columnSortHandler);
        dataGrid.getColumnSortList().push(firstNameColumn);
        dataGrid.getColumnSortList().push(secondNameColumn);

        dataProvider.addDataDisplay(dataGrid);
        pager.setDisplay(dataGrid);

        Annotator.eventBus.addHandler(UserChangeEvent.TYPE, new UserChangeEventHandler() {
            @Override
            public void onUserChanged(UserChangeEvent userChangeEvent) {
                switch (userChangeEvent.getAction()) {
                    case ADD_USER_TO_GROUP:
                        availableGroupList.removeItem(availableGroupList.getSelectedIndex());
                        if (availableGroupList.getItemCount() > 0) {
                            availableGroupList.setSelectedIndex(0);
                        }
                        addGroupButton.setEnabled(availableGroupList.getItemCount() > 0);

                        String group = userChangeEvent.getGroup();
                        addGroupToUi(group);
                        break;
                    case RELOAD_USERS:
                        reload();
                        break;
                    case REMOVE_USER_FROM_GROUP:
                        removeGroupFromUI(userChangeEvent.getGroup());
                        addGroupButton.setEnabled(availableGroupList.getItemCount() > 0);
                        break;
                    case USERS_RELOADED:
                        selectionModel.clear();
                        reload();
                        break;

                }
            }
        });


        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (MainPanel.getInstance().getCurrentUser() != null) {
                    if (MainPanel.getInstance().isCurrentUserInstructorOrBetter()) {
                        reload();
                    }
                    return false;
                }
                return true;
            }
        }, 100);
    }

    @UiHandler("userDetailTab")
    void onTabSelection(SelectionEvent<Integer> event) {
        organismPermissionsGrid.redraw();
    }


    private void createOrganismPermissionsTable() {
        TextColumn<UserOrganismPermissionInfo> organismNameColumn = new TextColumn<UserOrganismPermissionInfo>() {
            @Override
            public String getValue(UserOrganismPermissionInfo userOrganismPermissionInfo) {
                return userOrganismPermissionInfo.getOrganismName();
            }
        };
        organismNameColumn.setSortable(true);
        organismNameColumn.setDefaultSortAscending(true);


        Column<UserOrganismPermissionInfo, Boolean> adminColumn = new Column<UserOrganismPermissionInfo, Boolean>(new CheckboxCell(true, true)) {
            @Override
            public Boolean getValue(UserOrganismPermissionInfo object) {
                return object.isAdmin();
            }
        };
        adminColumn.setSortable(true);
        adminColumn.setFieldUpdater(new FieldUpdater<UserOrganismPermissionInfo, Boolean>() {
            @Override
            public void update(int index, UserOrganismPermissionInfo object, Boolean value) {
                object.setAdmin(value);
                UserRestService.updateOrganismPermission(object);
            }
        });
        sortHandler.setComparator(adminColumn, new Comparator<UserOrganismPermissionInfo>() {
            @Override
            public int compare(UserOrganismPermissionInfo o1, UserOrganismPermissionInfo o2) {
                return o1.isAdmin().compareTo(o2.isAdmin());
            }
        });

        organismPermissionsGrid.addColumnSortHandler(sortHandler);
        organismPermissionsGrid.setEmptyTableWidget(new Label("Please select a user to view organism permissions"));
        organismPager.setDisplay(organismPermissionsGrid);

        sortHandler.setComparator(organismNameColumn, new Comparator<UserOrganismPermissionInfo>() {
            @Override
            public int compare(UserOrganismPermissionInfo o1, UserOrganismPermissionInfo o2) {
                return o1.getOrganismName().compareTo(o2.getOrganismName());
            }
        });

        Column<UserOrganismPermissionInfo, Boolean> writeColumn = new Column<UserOrganismPermissionInfo, Boolean>(new CheckboxCell(true, true)) {
            @Override
            public Boolean getValue(UserOrganismPermissionInfo object) {
                return object.isWrite();
            }
        };
        writeColumn.setSortable(true);
        writeColumn.setFieldUpdater(new FieldUpdater<UserOrganismPermissionInfo, Boolean>() {
            @Override
            public void update(int index, UserOrganismPermissionInfo object, Boolean value) {
                object.setWrite(value);
                object.setUserId(selectedUserInfo.getUserId());
                UserRestService.updateOrganismPermission(object);
            }
        });
        sortHandler.setComparator(writeColumn, new Comparator<UserOrganismPermissionInfo>() {
            @Override
            public int compare(UserOrganismPermissionInfo o1, UserOrganismPermissionInfo o2) {
                return o1.isWrite().compareTo(o2.isWrite());
            }
        });

        Column<UserOrganismPermissionInfo, Boolean> exportColumn = new Column<UserOrganismPermissionInfo, Boolean>(new CheckboxCell(true, true)) {
            @Override
            public Boolean getValue(UserOrganismPermissionInfo object) {
                return object.isExport();
            }
        };
        exportColumn.setSortable(true);
        exportColumn.setFieldUpdater(new FieldUpdater<UserOrganismPermissionInfo, Boolean>() {
            @Override
            public void update(int index, UserOrganismPermissionInfo object, Boolean value) {
                object.setExport(value);
                UserRestService.updateOrganismPermission(object);
            }
        });
        sortHandler.setComparator(exportColumn, new Comparator<UserOrganismPermissionInfo>() {
            @Override
            public int compare(UserOrganismPermissionInfo o1, UserOrganismPermissionInfo o2) {
                return o1.isExport().compareTo(o2.isExport());
            }
        });

        Column<UserOrganismPermissionInfo, Boolean> readColumn = new Column<UserOrganismPermissionInfo, Boolean>(new CheckboxCell(true, true)) {
            @Override
            public Boolean getValue(UserOrganismPermissionInfo object) {
                return object.isRead();
            }
        };
        readColumn.setSortable(true);
        readColumn.setFieldUpdater(new FieldUpdater<UserOrganismPermissionInfo, Boolean>() {
            @Override
            public void update(int index, UserOrganismPermissionInfo object, Boolean value) {
                object.setRead(value);
                UserRestService.updateOrganismPermission(object);
            }
        });
        sortHandler.setComparator(readColumn, new Comparator<UserOrganismPermissionInfo>() {
            @Override
            public int compare(UserOrganismPermissionInfo o1, UserOrganismPermissionInfo o2) {
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

    private Boolean isEmail(String emailString) {
        if (!emailString.contains("@") || !emailString.contains(".")) {
            return false;
        }
        if (emailString.indexOf("@") >= emailString.lastIndexOf(".")) {
            return false;
        }
        return true;
    }

    private Boolean setCurrentUserInfoFromUI() {
        String emailString = email.getText().trim();
        final MutableBoolean mutableBoolean = new MutableBoolean(true);
        if (!isEmail(emailString)) {
            mutableBoolean.setBooleanValue(Window.confirm("'" + emailString + "' does not appear to be a valid email.  Use anyway?"));
        }
        if (mutableBoolean.getBooleanValue()) {
            selectedUserInfo.setEmail(emailString);
            selectedUserInfo.setFirstName(firstName.getText());
            selectedUserInfo.setLastName(lastName.getText());
            selectedUserInfo.setPassword(passwordTextBox.getText());
            selectedUserInfo.setRole(roleList.getSelectedItemText());
        }

        return mutableBoolean.getBooleanValue();
    }


    @UiHandler("createButton")
    public void create(ClickEvent clickEvent) {
        selectedUserInfo = null;
        selectionModel.clear();
        saveButton.setVisible(true);
        saveButton.setEnabled(true);
        updateUserInfo();
        cancelButton.setVisible(true);
        cancelButton.setEnabled(true);
        createButton.setEnabled(false);
        passwordRow.setVisible(true);
        userDetailTab.selectTab(0);
    }

    @UiHandler("addGroupButton")
    public void addGroupToUser(ClickEvent clickEvent) {
        String selectedGroup = availableGroupList.getSelectedItemText();
        UserRestService.addUserToGroup(selectedGroup, selectedUserInfo);
    }


    @UiHandler("cancelButton")
    public void cancel(ClickEvent clickEvent) {
        saveButton.setVisible(false);
        updateUserInfo();
        cancelButton.setVisible(false);
        cancelButton.setEnabled(false);
        createButton.setEnabled(true);
        passwordRow.setVisible(false);
    }

    @UiHandler("deleteButton")
    public void delete(ClickEvent clickEvent) {
        Bootbox.confirm("Delete user " + selectedUserInfo.getName() + "?", new ConfirmCallback() {
            @Override
            public void callback(boolean result) {
                if (result) {
                    UserRestService.deleteUser(userInfoList, selectedUserInfo);
                    selectedUserInfo = null;
                    updateUserInfo();
                }
            }
        });
    }

    @UiHandler(value = {"nameSearchBox"})
    public void handleNameSearch(KeyUpEvent keyUpEvent) {
        pager.setPageStart(0);
        dataGrid.setVisibleRangeAndClearData(dataGrid.getVisibleRange(), true);
    }


    @UiHandler(value = {"firstName", "lastName", "email", "passwordTextBox"})
    public void updateInterface(KeyUpEvent keyUpEvent) {
        userIsSame();
    }

    @UiHandler(value = {"roleList"})
    public void handleRole(ChangeEvent changeEvent) {
        userIsSame();
    }


    @UiHandler("cancelButton")
    public void handleCancel(ClickEvent clickEvent) {
        updateUserInfo();
    }

    private void userIsSame() {
        if (selectedUserInfo == null) {
            return;
        }
        if (selectedUserInfo.getEmail().equals(email.getText().trim())
                && selectedUserInfo.getFirstName().equals(firstName.getText().trim())
                && selectedUserInfo.getLastName().equals(lastName.getText().trim())
                && selectedUserInfo.getRole().equals(roleList.getSelectedValue())
                && passwordTextBox.getText().trim().length() == 0  // we don't the password back here . . !!
                ) {
            saveButton.setEnabled(false);
            cancelButton.setEnabled(false);
        } else {
            saveButton.setEnabled(true);
            cancelButton.setEnabled(true);
        }
    }

    public void updateUser() {
        // assume an edit operation
        if (selectedUserInfo != null) {
            if (!setCurrentUserInfoFromUI()) {
                handleCancel(null);
                return;
            }
            RequestCallback requestCallback = new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    JSONValue v = JSONParser.parseStrict(response.getText());
                    JSONObject o = v.isObject();
                    if (o.containsKey(FeatureStringEnum.ERROR.getValue())) {
                        new ErrorDialog("Error Updating User", o.get(FeatureStringEnum.ERROR.getValue()).isString().stringValue(), true, true);
                    } else {
                        Bootbox.alert("Saved changes to user " + selectedUserInfo.getName() + "!");
                        selectedUserInfo = null;
                        updateUserInfo();
                        saveButton.setEnabled(false);
                        cancelButton.setEnabled(false);
                        reload(true);
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    Bootbox.alert("Error updating user: " + exception);
                }
            };
            UserRestService.updateUser(requestCallback, selectedUserInfo);
        }
    }

    private void saveNewUser() {
        selectedUserInfo = new UserInfo();
        if (setCurrentUserInfoFromUI()) {
            UserRestService.createUser(userInfoList, selectedUserInfo);
        } else {
            handleCancel(null);
        }
        createButton.setEnabled(true);

        selectedUserInfo = null;
        selectionModel.clear();
        updateUserInfo();
        saveButton.setVisible(false);
        cancelButton.setVisible(false);
        passwordRow.setVisible(false);
    }

    @UiHandler("saveButton")
    public void save(ClickEvent clickEvent) {
        if (selectedUserInfo == null) {
            saveNewUser();
        } else {
            updateUser();
        }
    }

    private void updateUserInfo() {
        passwordTextBox.setText("");
        groupTable.removeAllRows();
        if (selectedUserInfo == null) {
            addGroupButton.setEnabled(false);
            addGroupButton.setColor("gray");
            firstName.setText("");
            lastName.setText("");
            email.setText("");

            deleteButton.setEnabled(false);
            deleteButton.setVisible(false);
            roleList.setVisible(false);
            permissionProviderList.clear();


            if (saveButton.isVisible()) {
                roleList.setVisible(true);
                UserInfo currentUser = MainPanel.getInstance().getCurrentUser();
                roleList.setSelectedIndex(0);
                roleList.setEnabled(currentUser.getRole().equalsIgnoreCase(GlobalPermissionEnum.ADMIN.getLookupKey()));

                userRow1.setVisible(true);
                userRow2.setVisible(true);
                passwordRow.setVisible(true);
            } else {
                userRow1.setVisible(false);
                userRow2.setVisible(false);
                passwordRow.setVisible(false);
            }

        } else {
            createButton.setEnabled(true);
            addGroupButton.setEnabled(true);
            addGroupButton.setColor("blue");
            firstName.setText(selectedUserInfo.getFirstName());
            lastName.setText(selectedUserInfo.getLastName());
            email.setText(selectedUserInfo.getEmail());
            cancelButton.setVisible(true);
            saveButton.setVisible(true);
            saveButton.setEnabled(false);
            cancelButton.setEnabled(false);
            deleteButton.setVisible(true);
            deleteButton.setEnabled(true);
            userRow1.setVisible(true);
            userRow2.setVisible(true);
            passwordRow.setVisible(true);

            UserInfo currentUser = MainPanel.getInstance().getCurrentUser();

            passwordRow.setVisible(currentUser.getRole().equals("admin") || selectedUserInfo.getEmail().equals(currentUser.getEmail()));

            roleList.setVisible(true);
            for (int i = 0; i < roleList.getItemCount(); i++) {
                roleList.setItemSelected(i, selectedUserInfo.getRole().equals(roleList.getItemText(i)));
            }

            // if user is "user" then make uneditable
            // if user is admin AND self then make uneditable
            // if user is admin, but not self, then make editable
            roleList.setEnabled(currentUser.getRole().equalsIgnoreCase("admin") && currentUser.getUserId() != selectedUserInfo.getUserId());

            List<String> groupList = selectedUserInfo.getGroupList();
            for (String group : groupList) {
                addGroupToUi(group);
            }


            availableGroupList.clear();
            List<String> localAvailableGroupList = selectedUserInfo.getAvailableGroupList();
            for (String availableGroup : localAvailableGroupList) {
                availableGroupList.addItem(availableGroup);
            }

            permissionProviderList.clear();
            // only show organisms that this user is an admin on . . . https://github.com/GMOD/Apollo/issues/540
            if (MainPanel.getInstance().isCurrentUserAdmin()) {
                permissionProviderList.addAll(selectedUserInfo.getOrganismPermissionMap().values());
            } else {
                List<String> organismsToShow = new ArrayList<>();
                for (UserOrganismPermissionInfo userOrganismPermission : MainPanel.getInstance().getCurrentUser().getOrganismPermissionMap().values()) {
                    if (userOrganismPermission.isAdmin()) {
                        organismsToShow.add(userOrganismPermission.getOrganismName());
                    }
                }

                for (UserOrganismPermissionInfo userOrganismPermission : selectedUserInfo.getOrganismPermissionMap().values()) {
                    if (organismsToShow.contains(userOrganismPermission.getOrganismName())) {
                        permissionProviderList.add(userOrganismPermission);
                    }
                }
            }
        }
    }


    private void addGroupToUi(String group) {
        int i = groupTable.getRowCount();
        groupTable.setWidget(i, 0, new RemoveGroupButton(group));
        groupTable.setWidget(i, 1, new HTML(group));
    }

    public void reload() {
        reload(false);
    }

    public void reload(Boolean forceReload) {
        if (MainPanel.getInstance().getUserPanel().isVisible() || forceReload) {
            updateAvailableRoles();
            pager.setPageStart(0);
            dataGrid.setVisibleRangeAndClearData(dataGrid.getVisibleRange(), true);
            dataGrid.redraw();
        }
    }

    private void updateAvailableRoles() {
        roleList.clear();
        roleList.addItem(GlobalPermissionEnum.USER.getLookupKey());
        if (MainPanel.getInstance().isCurrentUserInstructorOrBetter()) {
            roleList.addItem(GlobalPermissionEnum.INSTRUCTOR.getLookupKey());
        }
        if (MainPanel.getInstance().isCurrentUserAdmin()) {
            roleList.addItem(GlobalPermissionEnum.ADMIN.getLookupKey());
        }
    }

    private void removeGroupFromUI(String group) {
        int rowToRemove = -1;
        for (int row = 0; rowToRemove < 0 && row < groupTable.getRowCount(); ++row) {
            RemoveGroupButton removeGroupButton = (RemoveGroupButton) groupTable.getWidget(row, 0);
            if (removeGroupButton.getGroupName().equals(group)) {
                rowToRemove = row;
            }
        }
        if (rowToRemove >= 0) {
            groupTable.removeRow(rowToRemove);
            availableGroupList.addItem(group);
        }
    }

    private class RemoveGroupButton extends org.gwtbootstrap3.client.ui.Button {

        private String groupName;

        public RemoveGroupButton(final String groupName) {
            this.groupName = groupName;
            setIcon(IconType.REMOVE);
            setColor("red");
            addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    UserRestService.removeUserFromGroup(groupName, userInfoList, selectedUserInfo);
                }
            });
        }

        String getGroupName() {
            return groupName;
        }
    }

}
