package org.bbop.apollo

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray

@Transactional
class CannedCommentService {

    JSONArray getCannedComments(Organism organism, List<FeatureType> featureTypeList) {

        JSONArray cannedComments = new JSONArray();

        List<CannedComment> cannedCommentList = new ArrayList<>()
        if (featureTypeList) {
            cannedCommentList.addAll(CannedComment.executeQuery("select cc from CannedComment cc join cc.featureTypes ft where ft in (:featureTypeList)", [featureTypeList: featureTypeList]))
        }
        cannedCommentList.addAll(CannedComment.executeQuery("select cc from CannedComment cc where cc.featureTypes is empty"))

        // if there are organism filters for these canned comments for this organism, then apply them
        List<CannedCommentOrganismFilter> cannedCommentOrganismFilters = CannedCommentOrganismFilter.findAllByCannedCommentInList(cannedCommentList)
        if (cannedCommentOrganismFilters) {
            CannedCommentOrganismFilter.findAllByOrganismAndCannedCommentInList(organism, cannedCommentList).each {
                cannedComments.put(it.cannedComment.comment)
            }
        }
        // otherwise ignore them
        else {
            cannedCommentList.each {
                cannedComments.put(it.comment)
            }
        }
        return cannedComments
    }
}
