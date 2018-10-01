package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.Window;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by ndunn on 9/30/18.
 */

//Adding
public class GroupInfoConverter {

    public static List<GroupInfo> convertFromJsonArray(JSONArray jsonArray) {

        List<GroupInfo> groupInfoList = new ArrayList<>();
        for(int i = 0 ; i < jsonArray.size() ; i++){
            groupInfoList.add(convertToGroupInfoFromJSON(jsonArray.get(i).isObject()));
        }

        return groupInfoList;
    }

    public static GroupInfo convertToGroupInfoFromJSON(JSONObject object){
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setId((long) object.get("id").isNumber().doubleValue());
        groupInfo.setName(object.get("name").isString().stringValue());
        groupInfo.setNumberOfUsers((int) object.get("numberOfUsers").isNumber().doubleValue());
        groupInfo.setNumberOfAdmin((int) object.get("numberOfAdmin").isNumber().doubleValue());
        groupInfo.setNumberOrganisms((int) object.get("numberOrganisms").isNumber().doubleValue());
        groupInfo.setNumberSequences((int) object.get("numberSequences").isNumber().doubleValue());

        // if(object.get("users")!=null){
        //     JSONArray userInfoArray = object.get("users").isArray();
        //     List<UserInfo> userInfoList = new ArrayList<>();
        //     for (int j = 0; j < userInfoArray.size(); j++) {
        //         UserInfo userValue = userInfoArray.get(j).isObject().objectValue();
        //         userInfoList.add(userValue);
        //     }
        //     groupInfo.setUserInfoList(userInfoList);
        // }

        // if(object.get("admin")!=null) {
        //     JSONArray adminInfoArray = object.get("admin").isArray();
        //     List<UserInfo> adminInfoList = new ArrayList<>();
        //     for (int j = 0; j < adminInfoArray.size(); j++) {
        //         UserInfo adminValue = adminInfoArray.get(j).isObject().objectValue();
        //         adminInfoList.add(adminValue);
        //     }
        //     groupInfo.setAdminInfoList(adminInfoList);
        // }


        // TODO: use shared permission enums
        if(object.get("organismPermissions")!=null) {
            JSONArray organismArray = object.get("organismPermissions").isArray();
            Map<String, GroupOrganismPermissionInfo> organismPermissionMap = new TreeMap<>();
            for (int j = 0; j < organismArray.size(); j++) {
                JSONObject organismPermissionJsonObject = organismArray.get(j).isObject();
                GroupOrganismPermissionInfo groupOrganismPermissionInfo = new GroupOrganismPermissionInfo();
                if (organismPermissionJsonObject.get("id") != null) {
                    groupOrganismPermissionInfo.setId((long) organismPermissionJsonObject.get("id").isNumber().doubleValue());
                }
                if (organismPermissionJsonObject.get("groupid") != null) {
                    groupOrganismPermissionInfo.setGroupId((long) organismPermissionJsonObject.get("groupid").isNumber().doubleValue());
                }
//                if (organismPermissionJsonObject.get("groupId") != null) {
//                    userOrganismPermissionInfo.setUserId((long) organismPermissionJsonObject.get("userId").isNumber().doubleValue());
//                }
                //userOrganismPermissionInfo.setOrganismName(organismPermissionJsonObject.get("organism").isString().stringValue());
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
                                Bootbox.alert("Unrecognized permission '" + permission+"'");
                        }
                    }
                }


                organismPermissionMap.put(groupOrganismPermissionInfo.getOrganismName(), groupOrganismPermissionInfo);
            }
            groupInfo.setOrganismPermissionMap(organismPermissionMap);
        }
        return groupInfo ;
    }

}
