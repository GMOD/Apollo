package org.bbop.apollo

import org.codehaus.groovy.grails.web.json.JSONArray

/**
 * Created by nathandunn on 9/21/15.
 */
class JBrowseMapper {


    public static Boolean hasName(JSONArray inputArray) {
        return getName(inputArray) != null
    }

    public static String getName(JSONArray inputArray) {
        switch (inputArray.getInt(0)) {
            case 0:
                return inputArray.getString(6)
        }
        return null
    }

    public static String getType(JSONArray inputArray) {
        switch (inputArray.getInt(0)) {
            case 0:
                return inputArray.getString(9)
            case 1:
            case 2:
                return inputArray.getString(7)
            case 3:
                return inputArray.getString(6)
        }
        return null
    }

    public static Boolean hasScore(JSONArray inputArray) {
        return getScore(inputArray) != null
    }

    public static Double getScore(JSONArray inputArray) {
        switch (inputArray.getInt(0)) {
            case 0:
                return inputArray.getDouble(7)
            case 2:
                return inputArray.getDouble(6)
        }
        return null
    }


    public static Integer getStart(JSONArray inputArray) {
        return inputArray.getInt(1)
    }

    public static Integer getEnd(JSONArray inputArray) {
        return inputArray.getInt(2)
    }

    public static Integer getStrand(JSONArray inputArray) {
        return inputArray.getInt(3)
    }

    public static String getSequence(JSONArray inputArray) {
        return inputArray.getString(5)
    }

    public static String getId(JSONArray inputArray) {
        switch (inputArray.getInt(0)) {
            case 0:
                return inputArray.getString(6)
        }
        return null
    }

    public static String getSource(JSONArray inputArray) {
        return inputArray.getString(4)
    }

    public static String getLength(JSONArray inputArray) {
        Integer end = getEnd(inputArray)
        Integer start = getStart(inputArray)
        if (start && end) {
            return end - start
        }
        return null
    }

    static Integer getPhase(JSONArray inputArray) {
        switch (inputArray.getInt(0)) {
            case 1:
                return inputArray.getInt(6)
        }
        return null
    }

    static String getPosition(JSONArray data){
        return "${getSequence(data)}:${getStart(data)}..${getEnd(data)} (${getStrand(data) > 0 ? '+' : '-'} strand)"
    }

    static String getSequenceString(JSONArray jsonArray, String sequenceString,Integer rootStart,Integer rootEnd) {
        Integer start = getStart(jsonArray)
        Integer end = getEnd(jsonArray)
        String sequence = getSequence(jsonArray)
        String type = getType(jsonArray)


        Integer modifiedStart = start - rootStart
        Integer modifiedEnd = end - rootStart

        String header = ">${sequence} ${getPosition(jsonArray)} class=${type}\n"
        String subString  = sequenceString.subSequence(modifiedStart,modifiedEnd)

        return header + subString
    }
}
