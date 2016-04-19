package org.bbop.apollo.projection

import groovy.transform.ToString
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by nathandunn on 9/24/15.
 */
@ToString
class ProjectionSequence implements Comparable<ProjectionSequence>{

    String id
    String name
    String organism

    Integer order  // what order this should be processed as
    Integer offset  = 0  // projected offset from originalOffset
    Integer originalOffset = 0 // original incoming coordinates . .  0 implies order = 0, >0 implies that order > 0
    List<String> features// a list of Features  // default is a single entry ALL . . if empty then all
    Integer unprojectedLength = 0
    Integer start
    Integer end
    // the length of the sequence before projection . . the projected length comes from the associated discontinuous projection


    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        ProjectionSequence that = (ProjectionSequence) o

//        if (id != that.id) return false
//        if (order != that.order) return false
        if (name != that.name) return false
//        if (offset != that.offset) return false
//        if (organism != that.organism) return false

        return true
    }

    int hashCode() {
        int result
//        result = (id != null ? id.hashCode() : 0)
//        result = 31 * result + (order != null ? order.hashCode() : 0)
        result = 31 * result + (name != null ? name.hashCode() : 0)
//        result = 31 * result + (organism != null ? organism.hashCode() : 0)
//        result = 31 * result + (offset != null ? offset.hashCode() : 0)
        return result
    }

    Integer getLength(){
        end-start
    }

    @Override
    int compareTo(ProjectionSequence o) {
//        id <=> o.id
//        if(order==null || o.order==null){
//            return name <=> o.name
//        }
//        else{
            int test = order <=> o.order
            if(test!=0){ return test }

            test = name <=> o.name
            return test
//        }

//        organism <=> o.organism
    }

    JSONObject toJSONObject(){
        JSONObject jsonObject = new JSONObject()
        jsonObject.id = id
        jsonObject.name = name
        jsonObject.organism = organism
        jsonObject.order = order
        jsonObject.offset = offset
        jsonObject.originalOffset = originalOffset
        jsonObject.start = start
        jsonObject.end = end

        JSONArray featuresArray  = new JSONArray()
        features.each {
            featuresArray.add(it)
        }
        jsonObject.features = featuresArray


        return jsonObject
    }
}
