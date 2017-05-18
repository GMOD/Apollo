package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.json.JSONWriter

@Transactional
class AssemblageService {

    def permissionService
    def preferenceService
    def projectionService

    /**
     * Gets the unique feature locations from the feature in order and the corresponding sequences.
     * Order from 5' to 3'
     * Order by fmin partial = true, fmax partial = true, fmin
     * @param feature
     * @return
     */
    Assemblage generateAssemblageForFeature(Feature feature) {
        List<Sequence> sequenceList = new ArrayList<>()
        feature.featureLocations.sort() { a, b ->
            a.rank <=> b.rank ?: a.isFmaxPartial <=> b.isFmaxPartial ?: b.isFminPartial <=> a.isFminPartial ?: a.fmin <=> b.fmin
        }.each {
            if (!sequenceList.contains(it.sequence)) {
                sequenceList.add(it.sequence)
            }
        }
        return generateAssemblageForSequence(sequenceList)
    }

    Assemblage generateAssemblageForFeatureRegions(List<Feature> features, Integer padding = org.bbop.apollo.gwt.shared.projection.ProjectionDefaults.DEFAULT_PADDING) {
        Map<Feature, Sequence> featureSequenceMap = new HashMap<>()
        List<FeatureLocation> featureLocationList = new ArrayList<>()
        features.each { feature ->
            feature.featureLocations.each { featureLocation ->
                // if the feature does not contain an identical feature location by feature and rank
//                if ( featureLocationList.find() { return it.rank == featureLocation.rank && it.featureId == featureLocation.featureId } == null ){
                featureLocationList.add(featureLocation)
//                }
            }
        }

        featureLocationList.sort() { a, b ->
            a.isFmaxPartial <=> b.isFmaxPartial ?: b.isFminPartial <=> a.isFminPartial ?: a.fmin <=> b.fmin
        }.each {
            if (!featureSequenceMap.containsKey(it.feature)) {
                featureSequenceMap.put(it.feature, it.sequence)
            }
        }

        // TODO: validate sequenceList against each feature and their location
//        features.each {
//            validateFeatureVsSequenceList(it, sequenceList)
//        }

        Organism organism = featureSequenceMap.values().first().organism
        JSONArray sequenceArray = new JSONArray()
        int end = 0;
        for (Feature feature in featureSequenceMap.keySet()) {
            Sequence seq = featureSequenceMap.get(feature)

            Integer minValue = feature.fmin - padding
            minValue = minValue >= seq.start ? minValue : seq.start
            Integer maxValue = feature.fmax + padding
            maxValue = maxValue < seq.end ? maxValue : seq.end

            // note this creates the proper JSON string
            JSONObject sequenceObject = JSON.parse((seq as JSON).toString()) as JSONObject
            sequenceObject.reverse = false

            JSONArray locationArray = new JSONArray()

            // TODO: for multiple locations or folding, you would add multiple of these
            JSONObject locationObject = new JSONObject()
            locationObject.fmin = minValue
            locationObject.fmax = maxValue
            locationObject.strand = feature.strand
            locationArray.add(locationObject)

            sequenceObject.put(FeatureStringEnum.LOCATION.value, locationArray)


            JSONObject featureObject = new JSONObject()
            featureObject.name = feature.name
            featureObject.fmin = minValue
            featureObject.fmax = maxValue
            sequenceObject.put(FeatureStringEnum.FEATURE.value, featureObject)


            sequenceObject.start = minValue
            sequenceObject.end = maxValue

            sequenceArray.add(sequenceObject)
            organism = organism ?: seq.organism
            end += maxValue - minValue
        }
        JSONObject testSequence = new JSONObject()
        testSequence.put(FeatureStringEnum.SEQUENCE_LIST.value, sequenceArray)
        testSequence.put(FeatureStringEnum.ORGANISM.value, organism.id)
        testSequence = standardizeSequenceList(testSequence)

        Assemblage assemblage = Assemblage.findByOrganismAndSequenceList(organism, sequenceArray.toString())
        assemblage = assemblage ?: new Assemblage(
                organism: organism
                , sequenceList: sequenceArray.toString()
                , start: 0
                , name: generateAssemblageName(sequenceArray)
                , end: end
        ).save(flush: true, failOnError: true)

        return assemblage
//        return generateAssemblageForSequence(sequenceList)
    }

    Assemblage generateAssemblageForFeatures(Feature... features) {
        List<Sequence> sequenceList = new ArrayList<>()
        List<FeatureLocation> featureLocationList = new ArrayList<>()
        features.each { feature ->
            feature.featureLocations.each { featureLocation ->
                if (!featureLocationList.contains(featureLocation)) {
                    featureLocationList.add(featureLocation)
                }
            }
        }

        featureLocationList.sort() { a, b ->
            a.isFmaxPartial <=> b.isFmaxPartial ?: b.isFminPartial <=> a.isFminPartial ?: a.fmin <=> b.fmin
        }.each {
            if (!sequenceList.contains(it.sequence)) {
                sequenceList.add(it.sequence)
            }
        }

        // TODO: validate sequenceList against each feature and their location
        features.each {
            validateFeatureVsSequenceList(it, sequenceList)
        }

        return generateAssemblageForSequence(sequenceList)
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
        feature.featureLocations.sort() { it.rank }.each {
            int sequenceIndex = sequences.indexOf(it.sequence)
            if (sequenceIndex < lastRank || sequenceIndex < 0) {
                throw new AnnotationException("Sequence list does not match feature arrangement ${feature.name}")
            }
            lastRank = sequenceIndex
        }
        return true
    }

    Assemblage generateAssemblageForSequence(Sequence... sequences) {
        List<Sequence> sequenceList = new ArrayList<>()
        for (s in sequences) {
            sequenceList.add(s)
        }
        return generateAssemblageForSequence(sequenceList)
    }

    Assemblage generateAssemblageForSequence(List<Sequence> sequences) {
        Organism organism = sequences.first().organism
        JSONArray sequenceArray = new JSONArray()
        int end = 0;
        for (Sequence seq in sequences) {
            // note this creates the proper JSON string
            JSONObject sequenceObject = JSON.parse((seq as JSON).toString()) as JSONObject
            sequenceObject.reverse = false
            sequenceArray.add(sequenceObject)
            organism = organism ?: seq.organism
            end += seq.end
        }
        JSONObject testSequence = new JSONObject()
        testSequence.put(FeatureStringEnum.SEQUENCE_LIST.value, sequenceArray)
        testSequence.put(FeatureStringEnum.ORGANISM.value, organism.id)
//        testSequence = standardizeSequenceList(testSequence)
//        sequenceArray = testSequence.getJSONArray(FeatureStringEnum.SEQUENCE_LIST.value)
//        String sanitizedSequenceArrayString = testSequence.getString(FeatureStringEnum.SEQUENCE_LIST.value)
        String sanitizedSequenceArrayString = getStandardizedSequenceString(testSequence)

        Assemblage assemblage = Assemblage.findByOrganismAndSequenceList(organism, sanitizedSequenceArrayString)
        assemblage = assemblage ?: new Assemblage(
                organism: organism
                , sequenceList: sanitizedSequenceArrayString
                , start: 0
                , name: generateAssemblageName(sequenceArray)
                , end: end
        ).save(flush: true, failOnError: true)

        return assemblage
    }

    String generateAssemblageName(JSONArray sequenceArray) {
        String name = ""

        for (int i = 0; i < sequenceArray.size(); i++) {
            JSONObject sequenceObject = sequenceArray.getJSONObject(i)
            name += sequenceObject.name
            if (sequenceObject.containsKey(FeatureStringEnum.FEATURE.value)) {
                name += sequenceObject.getJSONObject(FeatureStringEnum.FEATURE.value).name + " " + name
            }
        }

        return name
    }

    List<Sequence> getSequencesFromAssemblage(Organism organism, JSONArray sequenceArray) {
        List<Sequence> sequenceList = []

        for (int i = 0; i < sequenceArray.size(); i++) {
            String sequenceName = sequenceArray.getJSONObject(i).name
            Sequence sequence
            if (organism) {
                sequence = Sequence.findByOrganismAndName(organism, sequenceName)
            } else {
                sequence = Sequence.findByName(sequenceName)
            }
            assert sequence!=null
            sequenceList.add(sequence)
        }
        return sequenceList
    }

    List<Sequence> getSequencesFromAssemblage(Assemblage assemblage) {
        JSONArray sequenceArray = JSON.parse(assemblage.sequenceList) as JSONArray
        return getSequencesFromAssemblage(assemblage.organism, sequenceArray)
    }

    /**
     * TODO: does the automarshaller already do this?
     * @param assemblage
     * @return
     */
    // should match ProjectionDescription
    JSONObject convertAssemblageToJson(Assemblage assemblage) {
        JSONObject jsonObject = new JSONObject()
        jsonObject.id = assemblage.id
        jsonObject.payload = assemblage.payload ?: "{}"
        jsonObject.organism = assemblage.organism.commonName
        jsonObject.start = assemblage.start
        jsonObject.end = assemblage.end
        jsonObject.name = assemblage.name
        // in theory these should be the same
        jsonObject.sequenceList = JSON.parse(assemblage.sequenceList) as JSONArray
        return jsonObject
    }

    JSONObject standardizeSequenceList(JSONObject inputObject) {
        JSONArray sequenceArray = JSON.parse(inputObject.getString(FeatureStringEnum.SEQUENCE_LIST.value)) as JSONArray
        Organism organism = null
        if (inputObject.containsKey(FeatureStringEnum.ORGANISM.value)) {
            organism = preferenceService.getOrganismForToken(inputObject.getString(FeatureStringEnum.ORGANISM.value))
        }
        if (!organism && inputObject.containsKey(FeatureStringEnum.CLIENT_TOKEN.value)) {
            UserOrganismPreference userOrganismPreference = preferenceService.getCurrentOrganismPreference(permissionService.getCurrentUser(),null,inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
            organism = userOrganismPreference?.organism
        }
        List<Sequence> sequences1 = getSequencesFromAssemblage(organism, sequenceArray)
        Map<String, Sequence> sequenceMap = sequences1.collectEntries() {
            [it.name, it]
        }

        for (int i = 0; i < sequenceArray.size(); i++) {
            JSONObject sequenceObject = sequenceArray.getJSONObject(i)
            Sequence sequence = sequenceMap.get(sequenceObject.name)
            sequenceObject.id = sequence.id
            sequenceObject.start = sequenceObject.start ?: sequence.start
            sequenceObject.end = sequenceObject.end ?: sequence.end
            sequenceObject.length = sequenceObject.length ?: sequence.length
        }
        inputObject.put(FeatureStringEnum.SEQUENCE_LIST.value, sequenceArray.toString())

        return inputObject
    }

    def getAssemblagesForUserAndOrganism(User user, Organism organism) {
        def assemblages = user.assemblages.findAll() {
            it.organism == organism
        }
        return assemblages
    }


    String getStandardizedSequenceString(JSONObject jsonObject) {
        standardizeSequenceList(jsonObject)

        JSONArray sequenceListArray = JSON.parse(jsonObject.getString(FeatureStringEnum.SEQUENCE_LIST.value)) as JSONArray

        StringWriter stringWriter = new StringWriter()
        JSONWriter jsonWriter = new JSONWriter(stringWriter)
        jsonWriter.array()
        for (seqObj in sequenceListArray) {
            jsonWriter.object()
            seqObj.keys().sort() { a, b -> a <=> b }.each { String it ->
                jsonWriter.key(it).value(seqObj.get(it))
            }
            jsonWriter.endObject()
        }
        jsonWriter.endArray()
        return stringWriter.toString()
    }

    Assemblage convertJsonToAssemblage(JSONObject jsonObject) {
        String lookupString = getStandardizedSequenceString(jsonObject)

        Assemblage assemblage = Assemblage.findBySequenceList(lookupString)

        // now let's try it by ID
        if (assemblage == null && jsonObject.id) {
            assemblage = Assemblage.findById(jsonObject.id)
        }
        if (assemblage == null) {
            assemblage = Assemblage.findBySequenceList(lookupString)
        }
        if (assemblage == null) {
            log.info "creating assemblage from ${jsonObject as JSON} "
            assemblage = new Assemblage()
        }
        assemblage.sequenceList = lookupString
        assemblage.name = jsonObject.name ?: assemblage.name

        JSONArray sequenceListArray = JSON.parse(jsonObject.getString(FeatureStringEnum.SEQUENCE_LIST.value)) as JSONArray
        if (!assemblage.name) {
            assemblage.name = generateAssemblageName(sequenceListArray)
        }
        if (assemblage.name?.length() > 100) {
            assemblage.name = assemblage.name.substring(0, 99)
        }

        assemblage.start = jsonObject.containsKey(FeatureStringEnum.START.value) ? jsonObject.getLong(FeatureStringEnum.START.value) : sequenceListArray.getJSONObject(0).getInt(FeatureStringEnum.START.value)
        assemblage.end = jsonObject.containsKey(FeatureStringEnum.END.value) ? jsonObject.getLong(FeatureStringEnum.END.value) : sequenceListArray.getJSONObject(sequenceListArray.size() - 1).getInt(FeatureStringEnum.END.value)

        assemblage.organism = preferenceService.getOrganismFromInput(jsonObject)
        if (!assemblage.organism) {
            assemblage.organism = preferenceService.getCurrentOrganismForCurrentUser(jsonObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
        }
        assemblage.save()
        return assemblage
    }


    @NotTransactional
    static Boolean isProjectionReferer(String inputString) {
        return inputString.contains("(") && inputString.contains("):") && inputString.contains('..')
    }

    @NotTransactional
    static Boolean isProjectionString(String inputString) {
        return ((inputString.startsWith("{") && inputString.contains(FeatureStringEnum.SEQUENCE_LIST.value)) || (inputString.startsWith("[") && inputString.endsWith("]")))

    }

    /**
     * We want the minimimum location of a feature in the context of its assemblage
     *
     // get each feature location for the feature and create an offset for the "minimum" location
     // e.g., if the location doesn't start until the the second scaffold of the assemblage, that is the offset
     *
     * @param feature
     * @param assemblage
     * @return
     */
    int getMinForFeatureFullScaffold(Feature feature, Assemblage assemblage) {

        int offset = 0
        FeatureLocation firstFeatureLocation = feature.firstFeatureLocation
        List<Sequence> sequenceList = getSequencesFromAssemblage(assemblage)
        for(Sequence sequence in sequenceList){
            if(firstFeatureLocation.sequence==sequence){
                return getMinForFullScaffold(firstFeatureLocation.fmin + offset , assemblage)
            }
            else{
                offset += sequence.length
            }
        }
        throw new AnnotationException("Unable to find a feature min for feature ${feature} and assemblage ${assemblage}" )
    }

    int getMinForFullScaffold(Integer fmin, Assemblage assemblage) {
        List<Sequence> sequencesList = getSequencesFromAssemblage(assemblage)

        Sequence firstSequence = sequencesList.first()
        Integer sequenceOrder = sequencesList.indexOf(firstSequence)

        // add the entire length of each sequence in view
        for (int i = 0; i < sequenceOrder; i++) {
            fmin += sequencesList.get(i).length
        }
        return fmin
    }

    /**
     * We want the maximum location of a feature in the context of its assemblage
     * @param feature
     * @param assemblage
     * @return
     */
    int getMaxForFeatureFullScaffold(Feature feature, Assemblage assemblage) {

        int offset = 0
        FeatureLocation lastFeatureLocation = feature.lastFeatureLocation
        List<Sequence> sequenceList = getSequencesFromAssemblage(assemblage)
        for(Sequence sequence in sequenceList){
            if(lastFeatureLocation.sequence==sequence){
                return getMaxForFullScaffold(lastFeatureLocation.fmax + offset , assemblage)
            }
            else{
                offset += sequence.length
            }
        }
        throw new AnnotationException("Unable to find a feature max for feature ${feature} and assemblage ${assemblage}" )
    }

    /**
     * We want the maximum location of a feature in the context of its assemblage
     * @param feature
     * @param assemblage
     * @return
     */
    int getMaxForFullScaffold(Integer fmax, Assemblage assemblage) {
        List<Sequence> sequencesList = getSequencesFromAssemblage(assemblage)

        // we use the first sequence here, since fmax uses prior sequences
        Sequence firstSequence = sequencesList.first()
        Integer sequenceOrder = sequencesList.indexOf(firstSequence)

        // add the entire length of each sequence in view
        for (int i = 0; i < sequenceOrder; i++) {
            fmax += sequencesList.get(i).length
        }
        return fmax
    }

    def removeAssemblageById(Long id, User user) {
        def assemblage = Assemblage.findById(id)
        if (assemblage) {
            def uops = UserOrganismPreference.findAllByAssemblage(assemblage)
            Boolean canDelete = uops.find() { it.currentOrganism } == null
            if (canDelete) {
                user.removeFromAssemblages(assemblage)
                uops.each {
                    it.delete()
                }
                assemblage.delete(flush: true)
                return true
            } else {
                log.error("Preference is still current, ignoring ${id}")
                return false
            }
        } else {
            log.error("No assemblage found to delete for ${id} and ${user.username}")
            return false
        }
    }

}
