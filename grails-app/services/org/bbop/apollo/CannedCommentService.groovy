package org.bbop.apollo

import grails.gorm.transactions.Transactional
import org.grails.web.json.JSONArray

@Transactional
class CannedCommentService {

    JSONArray getCannedComments(Organism organism, List<FeatureType> featureTypeList) {
        JSONArray cannedCommentsJSONArray = new JSONArray();
        // add all CC with no type and organism
        List<CannedComment> cannedCommentList = new ArrayList<>()
        if (featureTypeList) {
            cannedCommentList.addAll(CannedComment.executeQuery("select cc from CannedComment cc join cc.featureTypes ft where ft in (:featureTypeList)", [featureTypeList: featureTypeList]))
        }
        cannedCommentList.addAll(CannedComment.executeQuery("select cc from CannedComment cc where cc.featureTypes is empty"))

        // if there are organism filters for these canned comments for this organism, then apply them
        List<CannedCommentOrganismFilter> cannedCommentOrganismFilters = CannedCommentOrganismFilter.findAllByCannedCommentInList(cannedCommentList)
        if (cannedCommentOrganismFilters) {
            // if the organism is in the list, that is good
            CannedCommentOrganismFilter.findAllByOrganismAndCannedCommentInList(organism, cannedCommentList).each {
                cannedCommentsJSONArray.put(it.cannedComment.comment)
            }
            // we have to add anything from cannedCommentList that isn't in another one
            List<CannedComment> cannedCommentsToExclude = CannedCommentOrganismFilter.findAllByOrganismNotEqualAndCannedCommentInList(organism, cannedCommentList).cannedComment
            for(CannedComment cannedComment in cannedCommentList){
                if(!cannedCommentsJSONArray.contains(cannedComment.comment) && !cannedCommentsToExclude.contains(cannedComment)){
                    cannedCommentsJSONArray.put(cannedComment.comment)
                }
            }
        }
        // otherwise ignore them
        else {
            cannedCommentList.each {
                cannedCommentsJSONArray.put(it.comment)
            }
        }
        return cannedCommentsJSONArray
    }
}
