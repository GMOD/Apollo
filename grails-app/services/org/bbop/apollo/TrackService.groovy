package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FileFilterUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class TrackService {

    def serviceMethod() {

    }


    public static String TRACK_NAME_SPLITTER = "::"

    String getTracks(User user, Organism organism) {
        String trackList = ""
        for (UserPermission userPermission in UserPermission.findAllByUserAndOrganism(user, organism)) {
            trackList += userPermission.trackNames // TODO: add properly
        }
        for (UserGroup userGroup in user.userGroups) {
            trackList += getTracks(userGroup, organism)
        }
        return trackList
    }

    String getTracks(UserGroup group, Organism organism) {
        String trackList = ""
        JSONArray jsonArray = new JSONArray()
        for (GroupPermission groupPermission in GroupPermission.findAllByGroupAndOrganism(group, organism)) {
            trackList += "||" + groupPermission.trackNames // TODO: add properly
        }
        return trackList.trim()
    }
    String getTrackPermissions(UserGroup userGroup, Organism organism) {
        JSONArray jsonArray = new JSONArray()
        for (GroupPermission groupPermission in GroupPermission.findAllByGroupAndOrganism(userGroup, organism)) {
            jsonArray.add(groupPermission as JSON)
        }
        return jsonArray.toString()
    }

    String getTrackPermissions(User user, Organism organism) {
        JSONArray jsonArray = new JSONArray()
        for (UserPermission userPermission in UserPermission.findAllByUserAndOrganism(user, organism)) {
            jsonArray.add(userPermission as JSON)
        }
        String returnString = jsonArray.toString()
        for (UserGroup userGroup in user.userGroups) {
            returnString += getTrackPermissions(userGroup, organism)
        }
        return returnString
    }

    static Map<String, Boolean> mergeTrackVisibilityMaps(Map<String, Boolean> mapA, Map<String, Boolean> mapB) {
        Map<String, Boolean> returnMap = new HashMap<>()
        mapA.keySet().each { it ->
            returnMap.put(it, mapA.get(it))
        }

        mapB.keySet().each { it ->
            if (returnMap.containsKey(it)) {
                returnMap.put(it, returnMap.get(it) || mapB.get(it))
            } else {
                returnMap.put(it, mapB.get(it))
            }
        }
        return returnMap
    }

    public Map<String, Boolean> getTracksVisibleForOrganismAndGroup(Organism organism, UserGroup userGroup) {
        Map<String, Boolean> trackVisibilityMap = new HashMap<>()

        List<GroupTrackPermission> groupPermissions = GroupTrackPermission.findAllByOrganismAndGroup(organism, userGroup)
        for (GroupTrackPermission groupPermission in groupPermissions) {
            JSONObject jsonObject = JSON.parse(groupPermission.trackVisibilities) as JSONObject

            // this should make it default to true if a true is ever given
            jsonObject.keySet().each {
                Boolean visible = trackVisibilityMap.get(it)
                // if null or false, can over-ride to true
                if (!visible) {
                    trackVisibilityMap.put(it, jsonObject.get(it))
                }
            }
        }

        return trackVisibilityMap
    }

    /**
     *
     * * @param trackVisibilityMap  Map of track names and visibility.
     * @param user
     * @param organism
     */
    public void setTracksVisibleForOrganismAndUser(Map<String, Boolean> trackVisibilityMap, Organism organism, User user) {
        UserTrackPermission userTrackPermission = UserTrackPermission.findByOrganismAndUser(organism, user)
        String jsonString = convertHashMapToJsonString(trackVisibilityMap)
        if (!userTrackPermission) {
            userTrackPermission = new UserTrackPermission(
                    user: user
                    , organism: organism
                    , trackVisibilities: jsonString
            ).save(insert: true)
        } else {
            userTrackPermission.trackVisibilities = jsonString
            userTrackPermission.save()
        }
    }

    /**
     * *
     * * @param trackVisibilityMap  Map of track names and visibility.
     * @param group
     * @param organism
     */
    public void setTracksVisibleForOrganismAndGroup(Map<String, Boolean> trackVisibilityMap, Organism organism, UserGroup group) {

        GroupTrackPermission groupTrackPermission = GroupTrackPermission.findByOrganismAndGroup(organism, group)
        String jsonString = convertHashMapToJsonString(trackVisibilityMap)
        if (!groupTrackPermission) {
            groupTrackPermission = new GroupTrackPermission(
                    group: group
                    , organism: organism
                    , trackVisibilities: jsonString
            ).save(insert: true)
        } else {
            groupTrackPermission.trackVisibilities = jsonString
            groupTrackPermission.save()
        }


    }

    public Map<String, Boolean> getTracksVisibleForOrganismAndUser(Organism organism, User user) {
        Map<String, Boolean> trackVisibilityMap = new HashMap<>()

        List<UserTrackPermission> userPermissionList = UserTrackPermission.findAllByOrganismAndUser(organism, user)
        for (UserTrackPermission userPermission in userPermissionList) {
            JSONObject jsonObject = JSON.parse(userPermission.trackVisibilities) as JSONObject

            jsonObject.keySet().each {
                Boolean visible = trackVisibilityMap.get(it)
                // if null or false, can over-ride to true
                if (!visible) {
                    trackVisibilityMap.put(it, jsonObject.get(it))
                }
            }
        }

        for (UserGroup group in user.userGroups) {
            Map<String, Boolean> specificMap = getTracksVisibleForOrganismAndGroup(organism, group)
            trackVisibilityMap = mergeTrackVisibilityMaps(specificMap, trackVisibilityMap)
        }

        return trackVisibilityMap
    }



    private String convertHashMapToJsonString(Map map) {
        JSONObject jsonObject = new JSONObject()
        map.keySet().each {
            jsonObject.put(it, map.get(it))
        }
        return jsonObject.toString()
    }

    /**
     * For the track in question, grab the array that matches the top-level name.
     * @param sequence
     * @param trackName
     * @param nameLookup
     * @return
     */
    JSONArray getTrackData(Sequence sequence, String trackName, String nameLookup) {
        String jbrowseDirectory = sequence.organism.directory + "/tracks/" + trackName
        File trackDirectory = new File(jbrowseDirectory)
        println "track directory ${trackDirectory.absolutePath}"
        String sequenceDirectory = jbrowseDirectory + "/" + sequence.name
        File trackDataFile = new File(sequenceDirectory+"/trackData.json")
        println "track data file ${trackDataFile.absolutePath}"
        assert trackDataFile.exists()
        println "looking up ${nameLookup}"

        JSONObject referenceJsonObject = new JSONObject(trackDataFile.text)
        JSONArray coordinateReferenceJsonArray = referenceJsonObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        for (int coordIndex = 0; coordIndex < coordinateReferenceJsonArray.size(); ++coordIndex) {
            JSONArray coordinate = coordinateReferenceJsonArray.getJSONArray(coordIndex)
            // TODO: use enums to better track format
            if (coordinate.getInt(0) == 4 || coordinate.getInt(0) == 3 ) {
                // projecess the file lf-${coordIndex} instead
                File chunkFile = new File(trackDataFile.parent + "/lf-${coordIndex + 1}.json")
                JSONArray chunkReferenceJsonArray = new JSONArray(chunkFile.text)

                for (int chunkArrayIndex = 0; chunkArrayIndex < chunkReferenceJsonArray.size(); ++chunkArrayIndex) {
                    JSONArray chunkArrayCoordinate = chunkReferenceJsonArray.getJSONArray(chunkArrayIndex)
                    JSONArray returnArray = findCoordinateName(chunkArrayCoordinate,nameLookup)
                    if(returnArray) return returnArray
                }

            } else {
                JSONArray returnArray = findCoordinateName(coordinate,nameLookup)
                if(returnArray) return returnArray
//                discontinuousProjection.addInterval(coordinate.getInt(1), coordinate.getInt(2),padding)
            }
        }



        // return an empty array if not found
        return new JSONArray()
    }

    JSONArray findCoordinateName(JSONArray coordinate, String nameLookup) {
        String name = nameLookup.toLowerCase()

        int classType = coordinate.getInt(0)
        for(int i = 0 ; i < coordinate.size() ; i++){
            switch (classType){
                case 0:
                    if(coordinate.getString(6).toLowerCase().contains(name)){
                        return coordinate
                    }
                    // search sublist
                    if(coordinate.size()>11){
                        println "coordinate > 11 ${coordinate as JSON}"
                        JSONObject subList = coordinate.getJSONObject(11)
//                        println "subList ${subList as JSON}"
//                        JSONObject subOject = subList.getJSONObject("Sublist")
                        JSONArray subArray = subList.getJSONArray("Sublist")
                        for(int subIndex  = 0 ; subIndex < subArray.size() ; ++subIndex){
                            println "subArray ${subArray as JSON}"

                            JSONArray subSubArray = subArray.getJSONArray(subIndex)
                            if(subSubArray.getInt(0)==0){
                                if(subSubArray.getString(6).toLowerCase().contains(name)){
                                    return subSubArray
                                }
                            }
                        }
                    }
                    break
                case 1:
                    if(coordinate.getString(8).toLowerCase().contains(name)){
                        return coordinate
                    }
                    break
                case 2:
                case 3:
                    if(coordinate.getString(8).toLowerCase().contains(name)){
                        return coordinate
                    }
                    break
                case 4:
                    println "can not process case 4 ${coordinate as JSON}"
                    break
            }


        }


        return null
    }
}
