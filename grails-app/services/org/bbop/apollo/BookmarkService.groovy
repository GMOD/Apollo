package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class BookmarkService {

    def permissionService
    def preferenceService

    /**
     * Gets the unique feature locations from the feature in order and the corresponding sequences.
     * Order from 5' to 3'
     * Order by fmin partial = true, fmax partial = true, fmin
     * @param feature
     * @return
     */
    Bookmark generateBookmarkForFeature(Feature feature) {
        List<Sequence> sequenceList = new ArrayList<>()
        feature.featureLocations.sort(){ a,b ->
            a.isFmaxPartial <=> b.isFmaxPartial ?: b.isFminPartial <=> a.isFminPartial ?: a.fmin <=> b.fmin
        }.each {
            if(!sequenceList.contains(it.sequence)){
                sequenceList.add(it.sequence)
            }
        }
        return generateBookmarkForSequence(sequenceList)
    }


    Bookmark generateBookmarkForFeatures(Feature... features) {
        List<Sequence> sequenceList = new ArrayList<>()
        List<FeatureLocation> featureLocationList = new ArrayList<>()
        features.each { feature ->
            feature.featureLocations.each { featureLocation ->
                if(!featureLocationList.contains(featureLocation)){
                    featureLocationList.add(featureLocation)
                }
            }
        }

        featureLocationList.sort(){ a,b ->
            a.isFmaxPartial <=> b.isFmaxPartial ?: b.isFminPartial <=> a.isFminPartial ?: a.fmin <=> b.fmin
        }.each {
            if(!sequenceList.contains(it.sequence)){
                sequenceList.add(it.sequence)
            }
        }

        // TODO: validate sequenceList against each feature and their location
        features.each {
            validateFeatureVsSequenceList(it,sequenceList)
        }

        return generateBookmarkForSequence(sequenceList)
    }

    /**
     * Here we want to guarantee that the sequence list exists in the same order as the
     * feature's feature locations.
     * @param feature
     * @param sequences
     * @return
     */
    def validateFeatureVsSequenceList(Feature feature, List<Sequence> sequences) {
        int lastRank = 0
        feature.featureLocations.sort(){ it.rank }.each {
            int sequenceIndex = sequences.indexOf(it.sequence)
            if(sequenceIndex<lastRank || sequenceIndex < 0 ){
                throw new AnnotationException("Sequence list does not match feature arrangement ${feature.name}")
            }
            lastRank = sequenceIndex
        }
        return true
    }

    Bookmark generateBookmarkForSequence(Sequence... sequences) {
        List<Sequence> sequenceList = new ArrayList<>()
        for(s in sequences){
            sequenceList.add(s)
        }
        return generateBookmarkForSequence(sequenceList)
    }

    Bookmark generateBookmarkForSequence(List<Sequence> sequences) {
        Organism organism = null
        JSONArray sequenceArray = new JSONArray()
        int end = 0;
        for (Sequence seq in sequences) {
            // note this creates the proper JSON string
            JSONObject sequenceObject = JSON.parse( (seq as JSON).toString()) as JSONObject
            sequenceArray.add(sequenceObject)
            organism = organism ?: seq.organism
            end += seq.end
        }
        Bookmark bookmark = Bookmark.findByOrganismAndSequenceList(organism, sequenceArray.toString()) ?: new Bookmark(
                organism: organism
                , sequenceList: sequenceArray.toString()
                , start: 0
                , end: end
        ).save(flush: true, failOnError: true)

        return bookmark
    }

    List<Sequence> getSequencesFromBookmark(Organism organism,String sequenceListString) {
        JSONArray sequenceArray = JSON.parse(sequenceListString) as JSONArray
        List<Sequence> sequenceList = []

        for (int i = 0; i < sequenceArray.size(); i++) {
            String sequenceName = sequenceArray.getJSONObject(i).name
            if (organism) {
                sequenceList << Sequence.findByOrganismAndName(organism, sequenceName)
            } else {
                sequenceList << Sequence.findByName(sequenceName)
            }
        }
        return sequenceList
    }

    List<Sequence> getSequencesFromBookmark(Bookmark bookmark) {

        return getSequencesFromBookmark(bookmark.organism,bookmark.sequenceList)
    }

    // should match ProjectionDescription
    JSONObject convertBookmarkToJson(Bookmark bookmark) {
        JSONObject jsonObject = new JSONObject()
        jsonObject.id = bookmark.id
        jsonObject.projection = bookmark.projection ?: "NONE"


        jsonObject.padding = bookmark.padding ?: 0
//        jsonObject.referenceTrack = bookmark.referenceTrack

        jsonObject.payload = bookmark.payload ?: "{}"
        jsonObject.organism = bookmark.organism.commonName
        jsonObject.start = bookmark.start
        jsonObject.end = bookmark.end
        // in theory these should be the same
        jsonObject.sequenceList = JSON.parse(bookmark.sequenceList) as JSONArray

        return jsonObject
    }

    JSONObject standardizeSequenceList(JSONObject inputObject) {
        JSONArray sequenceArray = JSON.parse(inputObject.getString(FeatureStringEnum.SEQUENCE_LIST.value)) as JSONArray
        Organism organism = null
        if(inputObject.containsKey(FeatureStringEnum.ORGANISM.value)){
            organism = preferenceService.getOrganismForToken(inputObject.getString(FeatureStringEnum.ORGANISM.value))
        }
        if(!organism){
            UserOrganismPreference userOrganismPreference = preferenceService.getCurrentOrganismPreference(inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
            organism = userOrganismPreference?.organism
        }
        Map<String,Sequence> sequenceMap = getSequencesFromBookmark(organism,sequenceArray.toString()).collectEntries(){
            [it.name,it]
        }

        for(int i = 0 ; i < sequenceArray.size() ; i++){
            JSONObject sequenceObject = sequenceArray.getJSONObject(i)
            Sequence sequence = sequenceMap.get(sequenceObject.name)
            sequenceObject.id = sequence.id
            sequenceObject.start = sequenceObject.start ?: sequence.start
            sequenceObject.end = sequenceObject.end ?: sequence.end
            sequenceObject.length = sequenceObject.length ?: sequence.length
        }
        inputObject.put(FeatureStringEnum.SEQUENCE_LIST.value,sequenceArray.toString())

        return inputObject
    }

    def getBookmarksForUserAndOrganism(User user,Organism organism){
        def bookmarks = user.bookmarks.findAll(){
            it.organism==organism
        }
        return bookmarks
    }

    Bookmark convertJsonToBookmark(JSONObject jsonObject) {
        standardizeSequenceList(jsonObject)
        JSONArray sequenceListArray = JSON.parse(jsonObject.getString(FeatureStringEnum.SEQUENCE_LIST.value)) as JSONArray
        Bookmark bookmark = Bookmark.findBySequenceList(sequenceListArray.toString())
        if(bookmark==null){
            log.info "creating bookmark from ${jsonObject as JSON} "
            bookmark = new Bookmark()
            bookmark.projection = jsonObject.projection
            bookmark.sequenceList = sequenceListArray.toString()

            bookmark.start = jsonObject.containsKey(FeatureStringEnum.START.value) ? jsonObject.getLong(FeatureStringEnum.START.value): sequenceListArray.getJSONObject(0).getInt(FeatureStringEnum.START.value)
            bookmark.end = jsonObject.containsKey(FeatureStringEnum.END.value) ? jsonObject.getLong(FeatureStringEnum.END.value) : sequenceListArray.getJSONObject(sequenceListArray.size()-1).getInt(FeatureStringEnum.END.value)

            bookmark.organism = preferenceService.getOrganismFromInput(jsonObject)
            if(!bookmark.organism){
                bookmark.organism = preferenceService.getCurrentOrganismForCurrentUser(jsonObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
            }
            bookmark.save(insert: true,flush:true)
        }
        return bookmark
    }

    @NotTransactional
    static Boolean isProjectionReferer(String inputString ){
        return inputString.contains("(")&&inputString.contains("):")&&inputString.contains('..')
    }

    @NotTransactional
    static Boolean isProjectionString(String inputString ){
        return ( (inputString.startsWith("{") && inputString.contains(FeatureStringEnum.SEQUENCE_LIST.value)) || (inputString.startsWith("[") && inputString.endsWith("]")) )

    }

    /**
     * We want the minimimum location of a feature in the context of its bookmark
     * @param feature
     * @param bookmark
     * @return
     */
    int getMinForFeatureFullScaffold(Feature feature, Bookmark bookmark) {
        Integer calculatedMin = feature.fmin
        List<Sequence> sequencesList = getSequencesFromBookmark(bookmark)

        Sequence firstSequence = feature.getFirstSequence()
        Integer sequenceOrder = sequencesList.indexOf(firstSequence)

        // add the entire length of each sequence in view
        for(int i = 0 ; i < sequenceOrder ; i++){
            calculatedMin += sequencesList.get(i).length
        }
        return calculatedMin
    }

    /**
     * We want the maximum location of a feature in the context of its bookmark
     * @param feature
     * @param bookmark
     * @return
     */
    int getMaxForFeatureFullScaffold(Feature feature, Bookmark bookmark) {
        Integer calculatedMax = feature.fmax
        List<Sequence> sequencesList = getSequencesFromBookmark(bookmark)

        // we use the first sequence here, since fmax uses prior sequences
        Sequence firstSequence = feature.getFirstSequence()
        Integer sequenceOrder = sequencesList.indexOf(firstSequence)

        // add the entire length of each sequence in view
        for(int i = 0 ; i < sequenceOrder ; i++){
            calculatedMax += sequencesList.get(i).length
        }
        return calculatedMax
    }

    def removeBookmarkById(Long id,User user) {
        def bookmark = Bookmark.findById(id)
        if(bookmark){
            def uops = UserOrganismPreference.findAllByBookmark(bookmark)
            Boolean canDelete = uops.find(){ it.currentOrganism } == null
            if(canDelete){
                user.removeFromBookmarks(bookmark)
                uops.each {
                    it.delete()
                }
                bookmark.delete(flush: true)
                return true
            }
            else{
                log.error("Preference is still current, ignoring ${id}")
                return false
            }
        }
        else{
            log.error("No bookmark found to delete for ${id} and ${user.username}")
            return false
        }
    }
}
