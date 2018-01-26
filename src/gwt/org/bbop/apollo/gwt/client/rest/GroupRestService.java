package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.client.Annotator;
import org.bbop.apollo.gwt.client.AnnotatorPanel;
import org.bbop.apollo.gwt.client.dto.GroupInfo;
import org.bbop.apollo.gwt.client.dto.GroupOrganismPermissionInfo;
import org.bbop.apollo.gwt.client.dto.UserInfo;
import org.bbop.apollo.gwt.client.event.GroupChangeEvent;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.select.client.ui.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by ndunn on 3/30/15.
 */
public class GroupRestService {


    public static void loadGroups(final List<GroupInfo> groupInfoList) {

        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                groupInfoList.clear();
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                JSONArray array = returnValue.isArray();

                for (int i = 0; array != null && i < array.size(); i++) {
                    JSONObject object = array.get(i).isObject();

                    GroupInfo groupInfo = new GroupInfo();
                    groupInfo.setId((long) object.get("id").isNumber().doubleValue());
                    groupInfo.setName(object.get("name").isString().stringValue());
                    groupInfo.setNumberOfUsers((int) object.get("numberOfUsers").isNumber().doubleValue());
                    Integer numberOfAdmin = 0;
                    if (object.get("numberOfAdmin") != null) {
                        numberOfAdmin = (int) object.get("numberOfAdmin").isNumber().doubleValue();
                    }

                    groupInfo.setNumberOfAdmin(numberOfAdmin);

                    List<UserInfo> userInfoList = new ArrayList<>();
                    List<UserInfo> adminInfoList = new ArrayList<>();

                    if (object.get("users") != null) {
                        JSONArray usersArray = object.get("users").isArray();
                        for (int j = 0; j < usersArray.size(); j++) {
                            JSONObject userObject = usersArray.get(j).isObject();
                            UserInfo userInfo = new UserInfo(userObject);
                            userInfoList.add(userInfo);
                        }
                    }


                    groupInfo.setUserInfoList(userInfoList);

                    if (object.get("admin") != null) {
                        JSONArray adminArray = object.get("admin").isArray();
                        for (int j = 0; j < adminArray.size(); j++) {
                            JSONObject userObject = adminArray.get(j).isObject();
                            UserInfo adminInfo = new UserInfo(userObject);
                            adminInfoList.add(adminInfo);
                        }
                    }


                    groupInfo.setAdminInfoList(adminInfoList);



                    // TODO: use shared permission enums
                    JSONArray organismArray = object.get("organismPermissions").isArray();
                    Map<String, GroupOrganismPermissionInfo> organismPermissionMap = new TreeMap<>();
                    for (int j = 0; j < organismArray.size(); j++) {
                        JSONObject organismPermissionJsonObject = organismArray.get(j).isObject();
                        GroupOrganismPermissionInfo groupOrganismPermissionInfo = new GroupOrganismPermissionInfo();
                        if (organismPermissionJsonObject.get("id") != null) {
                            groupOrganismPermissionInfo.setId((long) organismPermissionJsonObject.get("id").isNumber().doubleValue());
                        }
                        groupOrganismPermissionInfo.setGroupId((long) organismPermissionJsonObject.get("groupId").isNumber().doubleValue());
                        groupOrganismPermissionInfo.setOrganismName(organismPermissionJsonObject.get("organism").isString().stringValue());
                        if (organismPermissionJsonObject.get("permissions") != null) {
                            JSONArray permissionsArray = JSONParser.parseStrict(organismPermissionJsonObject.get("permissions").isString().stringValue()).isArray();
                            for (int permissionIndex = 0; permissionIndex < permissionsArray.size(); ++permissionIndex) {
                                String permission = permissionsArray.get(permissionIndex).isString().stringValue();
                                switch (permission) {
                                    case "ADMINISTRATE":
                                        groupOrganismPermissionInfo.setAdmin(true);
                                        break;
                                    case "WRITE":
                                        groupOrganismPermissionInfo.setWrite(true);
                                        break;
                                    case "EXPORT":
                                        groupOrganismPermissionInfo.setExport(true);
                                        break;
                                    case "READ":
                                        groupOrganismPermissionInfo.setRead(true);
                                        break;

                                    default:
                                        Bootbox.alert("Unsure how to handle this permission '" + permission + "'");
                                }
                            }
                        }


                        organismPermissionMap.put(groupOrganismPermissionInfo.getOrganismName(), groupOrganismPermissionInfo);
                    }
                    groupInfo.setOrganismPermissionMap(organismPermissionMap);


                    groupInfoList.add(groupInfo);
                }
                Annotator.eventBus.fireEvent(new GroupChangeEvent(GroupChangeEvent.Action.GROUPS_RELOADED));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("error retrieving groups");
            }
        };

        RestService.sendRequest(requestCallback, "group/loadGroups/");
    }

    public static void updateGroup(final GroupInfo selectedGroupInfo) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                Annotator.eventBus.fireEvent(new GroupChangeEvent(GroupChangeEvent.Action.RELOAD_GROUPS));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("error updating group " + selectedGroupInfo.getName() + " " + exception);
            }
        };
        RestService.sendRequest(requestCallback, "group/updateGroup/", "data=" + selectedGroupInfo.toJSON().toString());
    }

    public static void deleteGroup(final GroupInfo selectedGroupInfo) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                Annotator.eventBus.fireEvent(new GroupChangeEvent(GroupChangeEvent.Action.RELOAD_GROUPS));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("error updating group " + selectedGroupInfo.getName() + " " + exception);
            }
        };
        RestService.sendRequest(requestCallback, "group/deleteGroup/", "data=" + selectedGroupInfo.toJSON().toString());
    }

    public static void addNewGroup(final GroupInfo selectedGroupInfo) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                Annotator.eventBus.fireEvent(new GroupChangeEvent(GroupChangeEvent.Action.ADD_GROUP));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("error updating group " + selectedGroupInfo.getName() + " " + exception);
            }
        };
        RestService.sendRequest(requestCallback, "group/createGroup/", "data=" + selectedGroupInfo.toJSON().toString());
    }

    public static void updateOrganismPermission(GroupOrganismPermissionInfo object) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                GWT.log("success");
//                loadUsers(userInfoList);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Error updating permissions: " + exception);
            }
        };
        RestService.sendRequest(requestCallback, "group/updateOrganismPermission", "data=" + object.toJSON());
    }

    public static void updateUserGroups(RequestCallback requestCallback, GroupInfo selectedGroupInfo, List<Option> selectedValues) {
//        RestService.sendRequest(requestCallback, "group/updateMembership", "data=" + object.toJSON());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("groupId", new JSONNumber(selectedGroupInfo.getId()));
        JSONArray userArray = new JSONArray();
        for (Option userData : selectedValues) {
            String emailValue = userData.getValue().split("\\(")[1].trim();
            emailValue = emailValue.substring(0, emailValue.length() - 1);
            userArray.set(userArray.size(), new JSONString(emailValue));
        }
        jsonObject.put("users", userArray);
        RestService.sendRequest(requestCallback, "group/updateMembership", "data=" + jsonObject);
    }

    public static void updateGroupAdmin(RequestCallback requestCallback, GroupInfo selectedGroupInfo, List<Option> selectedValues) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("groupId", new JSONNumber(selectedGroupInfo.getId()));
        JSONArray userArray = new JSONArray();
        for (Option userData : selectedValues) {
            String emailValue = userData.getValue().split("\\(")[1].trim();
            emailValue = emailValue.substring(0, emailValue.length() - 1);
            userArray.set(userArray.size(), new JSONString(emailValue));
        }
        jsonObject.put("users", userArray);
        RestService.sendRequest(requestCallback, "group/updateGroupAdmin", "data=" + jsonObject);
    }
}


