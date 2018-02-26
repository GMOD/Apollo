package org.bbop.apollo

import grails.transaction.Transactional
import grails.transaction.NotTransactional

class FeaturePropertyService {


    public static List<String> nonReservedClasses = ["Comment","Owner","Description","Symbol","Status",StopCodonReadThrough.cvTerm]


/** Get comments for this feature.
 *
 * @return Comments for this feature
 */
    @Transactional
    public Collection<Comment> getComments(Feature feature) {
        List<Comment> comments = new ArrayList<Comment>();

        // TODO: move out of loop and into own service method
        for (FeatureProperty fp : feature.getFeatureProperties()) {
            if (Comment.ontologyId == fp.ontologyId) {
                comments.add((Comment) fp);
            }
        }

        Collections.sort(comments, new Comparator<Comment>() {
            public int compare(Comment comment1, Comment comment2) {
                if (comment1.getType().equals(comment2.getType())) {
                    return new Integer(comment1.getRank()).compareTo(comment2.getRank());
                }
                return new Integer(comment1.hashCode()).compareTo(comment2.hashCode());
            }
        });
        return comments;
    }

    @Transactional
    def addComment(Feature feature, Comment comment) {
        addProperty(feature, comment)
    }

    @Transactional
    def addComment(Feature feature, String commentString) {
        Comment comment = new Comment(
                feature: feature
                ,value: commentString
        ).save()
        feature.addToFeatureProperties(comment)
        feature.save()
    }

    @Transactional
    boolean deleteComment(Feature feature, String commentString) {
        Comment comment = Comment.findByFeatureAndValue(feature, commentString)
        if (comment) {
            feature.removeFromFeatureProperties(comment)
            Comment.deleteAll(comment)
            return true
        }
        return false
    }

    @Transactional
    def setFeatureProperty(Feature feature,String type,String tag,String value){

        for(FeatureProperty featureProperty in feature.featureProperties){
            if(featureProperty.type.name == type){
                featureProperty.tag = tag
                featureProperty.value = value
                featureProperty.save(flush: true)
                return true
            }
        }

        feature.addToFeatureProperties()

        return false

    }

    @Transactional
    def addProperty(Feature feature, FeatureProperty property) {
        int rank = 0;
        log.debug "value of FP to add: ${property.value} ${property.tag}"
        for (FeatureProperty fp : feature.getFeatureProperties()) {
            if (fp.getType().equals(property.getType())) {
                if (fp.getRank() > rank) {
                    rank = fp.getRank();
                }
            }
        }
        property.setRank(rank + 1);
        boolean ok = feature.addToFeatureProperties(property);

    }

    @Transactional
    public boolean deleteProperty(Feature feature, FeatureProperty property) {
        for (FeatureProperty fp : feature.getFeatureProperties()) {
            if (fp.getType().equals(property.getType()) && fp.getValue().equals(property.getValue())) {
                feature.getFeatureProperties().remove(fp);
                return true;
            }
        }
    }

    @Transactional
    def getNonReservedProperties(Feature feature) {
        return FeatureProperty.findAllByFeature(feature).findAll(){
            return !nonReservedClasses.contains(it.cvTerm)
        }
    }
}
