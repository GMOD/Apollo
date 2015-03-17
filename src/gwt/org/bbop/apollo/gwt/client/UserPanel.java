package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
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
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.UserInfo;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.UserRestService;

import java.util.Comparator;
import java.util.List;

/**
 * Created by ndunn on 12/17/14.
 */
public class UserPanel extends Composite {
    interface UserBrowserPanelUiBinder extends UiBinder<Widget, UserPanel> {
    }

    private static UserBrowserPanelUiBinder ourUiBinder = GWT.create(UserBrowserPanelUiBinder.class);
    //    @UiField
//    HTML userGroupHTML;
    @UiField
    TextBox firstName;
    @UiField
    TextBox lastName;
    @UiField
    TextBox email;

    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<UserInfo> dataGrid = new DataGrid<UserInfo>(10, tablecss);
    @UiField
    Button createButton;
    @UiField
    Button cancelButton;
    @UiField
    Button deleteButton;
    @UiField
    Button saveButton;


    private ListDataProvider<UserInfo> dataProvider = new ListDataProvider<>();
    private List<UserInfo> userInfoList = dataProvider.getList();
    private SingleSelectionModel<UserInfo> selectionModel = new SingleSelectionModel<>();
    private UserInfo selectedUserInfo;


    public UserPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));

//        userGroupHTML.setHTML("<div class='label label-default'>USDA i5K</div>");


        TextColumn<UserInfo> firstNameColumn = new TextColumn<UserInfo>() {
            @Override
            public String getValue(UserInfo employee) {
                return employee.getName();
            }
        };
        firstNameColumn.setSortable(true);

//        Column<UserInfo,Number> secondNameColumn = new Column<UserInfo,Number>(new NumberCell()) {
//            @Override
//            public Integer getValue(UserInfo object) {
//                return object.getNumberUserGroups();
//            }
//        };
//        secondNameColumn.setSortable(true);

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
            public String getValue(UserInfo employee) {
                return employee.getEmail();
            }
        };
        secondNameColumn.setSortable(true);

        dataGrid.addColumn(firstNameColumn, "Name");
        dataGrid.addColumn(secondNameColumn, "Email");

        dataGrid.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                selectedUserInfo = selectionModel.getSelectedObject();
                updateUserInfo();
            }
        });

        dataProvider.addDataDisplay(dataGrid);


        ColumnSortEvent.ListHandler<UserInfo> sortHandler = new ColumnSortEvent.ListHandler<UserInfo>(userInfoList);
        dataGrid.addColumnSortHandler(sortHandler);
        sortHandler.setComparator(firstNameColumn, new Comparator<UserInfo>() {
            @Override
            public int compare(UserInfo o1, UserInfo o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        sortHandler.setComparator(secondNameColumn, new Comparator<UserInfo>() {
            @Override
            public int compare(UserInfo o1, UserInfo o2) {
                return o1.getEmail().compareTo(o2.getEmail());
            }
        });
    }

    private void setCurrentUserInfoFromUI() {
        selectedUserInfo.setEmail(email.getText());
        selectedUserInfo.setFirstName(firstName.getText());
        selectedUserInfo.setLastName(lastName.getText());
    }

    @UiHandler(value = {"firstName", "lastName", "email"})
    public void updateName(ChangeEvent changeHandler) {
        // assume an edit operation
        if (selectedUserInfo != null) {
            setCurrentUserInfoFromUI();
            UserRestService.updateUser(selectedUserInfo);
        }
        // if it is to be created then we don't care
    }

    @UiHandler("createButton")
    public void create(ClickEvent clickEvent) {
        selectedUserInfo = null;
        selectionModel.clear();
        updateUserInfo();
        saveButton.setVisible(true);
        cancelButton.setEnabled(true);
        createButton.setEnabled(false);

    }

    @UiHandler("cancelButton")
    public void cancel(ClickEvent clickEvent) {
        updateUserInfo();
        saveButton.setVisible(false);
        cancelButton.setEnabled(false);
        createButton.setEnabled(true);
    }

    @UiHandler("deleteButton")
    public void delete(ClickEvent clickEvent) {
        UserRestService.deleteUser(selectedUserInfo);
    }

    @UiHandler("saveButton")
    public void save(ClickEvent clickEvent) {
        selectedUserInfo = new UserInfo();
        setCurrentUserInfoFromUI();
        UserRestService.createUser(selectedUserInfo);
        createButton.setEnabled(true);
       
        selectedUserInfo = null ; 
        selectionModel.clear();
        updateUserInfo();
        saveButton.setVisible(false);
        cancelButton.setEnabled(false);
    }

    private void updateUserInfo() {
        if (selectedUserInfo == null) {
            firstName.setText("");
            lastName.setText("");
            email.setText("");
            deleteButton.setEnabled(false);
        } else {
            firstName.setText(selectedUserInfo.getFirstName());
            lastName.setText(selectedUserInfo.getLastName());
            email.setText(selectedUserInfo.getEmail());
            deleteButton.setEnabled(true);
        }

    }

    public void reload() {
        UserRestService.loadUsers(userInfoList);
        dataGrid.redraw();
    }
}