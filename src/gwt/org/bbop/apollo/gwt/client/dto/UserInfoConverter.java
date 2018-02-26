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
 * Created by ndunn on 3/31/15.
 */
public class UserInfoConverter {

    public static List<UserInfo> convertFromJsonArray(JSONArray jsonArray) {

        List<UserInfo> userInfoList = new ArrayList<>();
        for(int i = 0 ; i < jsonArray.size() ; i++){
            userInfoList.add(convertToUserInfoFromJSON(jsonArray.get(i).isObject()));
        }

        return userInfoList;
    }

    public static UserInfo convertToUserInfoFromJSON(JSONObject object){
        UserInfo userInfo = new UserInfo();

        userInfo.setUserId((long) object.get(FeatureStringEnum.USER_ID.getValue()).isNumber().doubleValue());
        userInfo.setFirstName(object.get("firstName").isString().stringValue());
        userInfo.setLastName(object.get("lastName").isString().stringValue());
        userInfo.setEmail(object.get("username").isString().stringValue());
        if (object.get("role") != null && object.get("role").isString() != null) {
            userInfo.setRole(object.get("role").isString().stringValue().toLowerCase());
        } else {
            userInfo.setRole("user");
        }

        if(object.get("groups")!=null){
            JSONArray groupArray = object.get("groups").isArray();
            List<String> groupList = new ArrayList<>();
            for (int j = 0; j < groupArray.size(); j++) {
                String groupValue = groupArray.get(j).isObject().get("name").isString().stringValue();
                groupList.add(groupValue);
            }
            userInfo.setGroupList(groupList);
        }

        if(object.get("availableGroups")!=null) {
            JSONArray availableGroupArray = object.get("availableGroups").isArray();
            List<String> availableGroupList = new ArrayList<>();
            for (int j = 0; j < availableGroupArray.size(); j++) {
                String availableGroupValue = availableGroupArray.get(j).isObject().get("name").isString().stringValue();
                availableGroupList.add(availableGroupValue);
            }
            userInfo.setAvailableGroupList(availableGroupList);
        }


        // TODO: use shared permission enums
        if(object.get("organismPermissions")!=null) {
            JSONArray organismArray = object.get("organismPermissions").isArray();
            Map<String, UserOrganismPermissionInfo> organismPermissionMap = new TreeMap<>();
            for (int j = 0; j < organismArray.size(); j++) {
                JSONObject organismPermissionJsonObject = organismArray.get(j).isObject();
                UserOrganismPermissionInfo userOrganismPermissionInfo = new UserOrganismPermissionInfo();
                if (organismPermissionJsonObject.get("id") != null) {
                    userOrganismPermissionInfo.setId((long) organismPermissionJsonObject.get("id").isNumber().doubleValue());
                }
                if (organismPermissionJsonObject.get(FeatureStringEnum.USER_ID.getValue()) != null) {
                    userOrganismPermissionInfo.setUserId((long) organismPermissionJsonObject.get(FeatureStringEnum.USER_ID.getValue()).isNumber().doubleValue());
                }
//                if (organismPermissionJsonObject.get("groupId") != null) {
//                    userOrganismPermissionInfo.setUserId((long) organismPermissionJsonObject.get("userId").isNumber().doubleValue());
//                }
                userOrganismPermissionInfo.setOrganismName(organismPermissionJsonObject.get("organism").isString().stringValue());
                if (organismPermissionJsonObject.get("permissions") != null) {
                    JSONArray permissionsArray = JSONParser.parseStrict(organismPermissionJsonObject.get("permissions").isString().stringValue()).isArray();
                    for (int permissionIndex = 0; permissionIndex < permissionsArray.size(); ++permissionIndex) {
                        String permission = permissionsArray.get(permissionIndex).isString().stringValue();
                        switch (permission) {
                            case "ADMINISTRATE":
                                userOrganismPermissionInfo.setAdmin(true);
                                break;
                            case "WRITE":
                                userOrganismPermissionInfo.setWrite(true);
                                break;
                            case "EXPORT":
                                userOrganismPermissionInfo.setExport(true);
                                break;
                            case "READ":
                                userOrganismPermissionInfo.setRead(true);
                                break;

                            default:
                                Bootbox.alert("Unrecognized permission '" + permission+"'");
                        }
                    }
                }


                organismPermissionMap.put(userOrganismPermissionInfo.getOrganismName(), userOrganismPermissionInfo);
            }
            userInfo.setOrganismPermissionMap(organismPermissionMap);
        }
        return userInfo ;
    }

}
