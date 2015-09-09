package org.bbop.apollo

import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FileFilterUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.projection.DiscontinuousProjection
import org.bbop.apollo.projection.ProjectionInterface
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional(readOnly = true)
class ProjectionService {

    // TODO: move to database as JSON
    // track, sequence, projection
    // TODO: should also include organism at some point as well
    // TODO: just turn this into a cache file
    private Map<String, Map<String, ProjectionInterface>> projectionMap = new HashMap<>()

    // TODO: should do an actual lookup / query in cache and DB
    @NotTransactional
    Boolean hasProjection(Organism organism, String trackName) {
        return projectionMap.size() > 0
    }

    // TODO: do re-lookup
    /**
     *
     * @param organism
     * @param trackName  TODO: this is the REFERENCE track!! .. might be too specific
     * @param sequenceName
     * @return
     */
    @NotTransactional
    ProjectionInterface getProjection(Organism organism, String trackName, String sequenceName) {
        return projectionMap ? projectionMap.values()?.iterator()?.next()?.get(sequenceName) : null
    }


    @NotTransactional
    String getTrackName(String fileName) {
        String[] tokens = fileName.split("/")
        return tokens[tokens.length - 3]
    }

    @NotTransactional
    String getSequenceName(String fileName) {
        String[] tokens = fileName.split("/")
        return tokens[tokens.length - 2]
    }

    // TODO: do re-lookup
    def createProjection(Organism organism, JSONArray tracksArray) {
        // TODO: this is only here for debugging . .
        projectionMap.clear()
        long startTime = System.currentTimeMillis()
        for (int i = 0; i < tracksArray.size(); i++) {
            JSONObject trackObject = tracksArray.getJSONObject(i)
            if (trackObject.containsKey("OGS") && trackObject.getBoolean("OGS") && !projectionMap.containsKey(trackObject.keys())) {
                println "tring to generate projection for ${trackObject.key}"
                String jbrowseDirectory = organism.directory + "/tracks/" + trackObject.key
                File trackDirectory = new File(jbrowseDirectory)
                println "track directory ${trackDirectory.absolutePath}"

                File[] files = FileUtils.listFiles(trackDirectory, FileFilterUtils.nameFileFilter("trackData.json"), TrueFileFilter.INSTANCE)

                println "# of files ${files.length}"

                Map<String, ProjectionInterface> sequenceProjectionMap = new HashMap<>()

                for (File trackDataFile in files) {
//                    println "file ${trackDataFile.absolutePath}"

//                    String sequenceFileName = trackDataFile.absolutePath.substring(trackDirectory.absolutePath.length(),trackDataFile.absolutePath.length()-"trackData.json".length()).replaceAll("/","")
                    String sequenceFileName = getSequenceName(trackDataFile.absolutePath)

//                    println "sequencefileName [${sequenceFileName}]"

                    JSONObject referenceJsonObject = new JSONObject(trackDataFile.text)

                    // TODO: interpret the format properly
                    DiscontinuousProjection discontinuousProjection = new DiscontinuousProjection()
                    JSONArray coordinateReferenceJsonArray = referenceJsonObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
                    for (int coordIndex = 0; coordIndex < coordinateReferenceJsonArray.size(); ++coordIndex) {
                        JSONArray coordinate = coordinateReferenceJsonArray.getJSONArray(coordIndex)
                        // TODO: use enums to better track format
                        if (coordinate.getInt(0) == 4) {
                            // projecess the file lf-${coordIndex} instead
                            File chunkFile = new File(trackDataFile.parent + "/lf-${coordIndex + 1}.json")
                            JSONArray chunkReferenceJsonArray = new JSONArray(chunkFile.text)

                            for (int chunkArrayIndex = 0; chunkArrayIndex < chunkReferenceJsonArray.size(); ++chunkArrayIndex) {
                                JSONArray chunkArrayCoordinate = chunkReferenceJsonArray.getJSONArray(chunkArrayIndex)
                                discontinuousProjection.addInterval(chunkArrayCoordinate.getInt(1), chunkArrayCoordinate.getInt(2))
                            }

                        } else {
                            discontinuousProjection.addInterval(coordinate.getInt(1), coordinate.getInt(2))
                        }
                    }

                    sequenceProjectionMap.put(sequenceFileName, discontinuousProjection)
                }

                println "final size: ${trackObject.key} -> ${sequenceProjectionMap.size()}"

                projectionMap.put(trackObject.key, sequenceProjectionMap)
            }
        }
        println "total time ${System.currentTimeMillis() - startTime}"
    }
}
