package org.bbop.apollo

import grails.transaction.Transactional

@Transactional
class FeaturePropertyService {




/** Get comments for this feature.
 *
 * @return Comments for this feature
 */
    public Collection<Comment> getComments(Feature feature) {
//        CVTerm commentCvTerm = cvTermService.getTerm(FeatureStringEnum.COMMENT)
//        Collection<CVTerm> commentCvterms = conf.getCVTermsForClass("Comment");
        List<Comment> comments = new ArrayList<Comment>();

        // TODO: move out of loop and into own service method
        for (FeatureProperty fp : feature.getFeatureProperties()) {
            if (Comment.ontologyId == fp.ontologyId) {
                comments.add((Comment) fp);
            }
        }
//        FeatureProperty.findByFeaturesAndOntologyId()


        Collections.sort(comments, new Comparator<Comment>() {

//            @Override
            public int compare(Comment comment1, Comment comment2) {
                if (comment1.getType().equals(comment2.getType())) {
                    return new Integer(comment1.getRank()).compareTo(comment2.getRank());
                }
                return new Integer(comment1.hashCode()).compareTo(comment2.hashCode());
            }
        });
        return comments;
    }

    def addComment(Feature feature, Comment comment) {
        addProperty(feature, comment)
    }

    def addComment(Feature feature, String commentString) {
        Comment comment = new Comment(
                feature: feature
//                type: cvTermService.getTerm(FeatureStringEnum.COMMENT.value),
                ,value: commentString
        ).save()
        feature.addToFeatureProperties(comment)
        feature.save()

//        addComment(feature, comment)
    }

    boolean deleteComment(Feature feature, String commentString) {
//        CVTerm commentCVTerm = cvTermService.getTerm(FeatureStringEnum.COMMENT.value)
//        Comment comment =  Comment.findByTypeAndFeatureAndValue(commentCVTerm,feature,commentString)
        Comment comment = Comment.findByFeatureAndValue(feature, commentString)
        if (comment) {
            feature.removeFromFeatureProperties(comment)
            Comment.deleteAll(comment)
            return true
        }
        return false
    }

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

    def addProperty(Feature feature, FeatureProperty property) {
        int rank = 0;
        println "value of FP to add: ${property.value} ${property.tag}"
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

    public boolean deleteProperty(Feature feature, FeatureProperty property) {
        for (FeatureProperty fp : feature.getFeatureProperties()) {
            if (fp.getType().equals(property.getType()) && fp.getValue().equals(property.getValue())) {
                feature.getFeatureProperties().remove(fp);
                return true;
            }
        }
    }

    /**
     * Is there a feature property called "owner"
     * @param feature
     * @return
     */
//    public User getOwner(Feature feature) {
////        Collection<CVTerm> ownerCvterms = conf.getCVTermsForClass("Owner");
//        List<CVTerm> ownerCvTerms = CVTerm.findAllByName("Owner")
//
//        for (FeatureProperty fp : feature.getFeatureProperties()) {
//            if (fp.type in ownerCvTerms) {
//                return User.findByUsername(fp.type.name)
//            }
////            if (ownerCvterms.contains(fp.getType())) {
////                return new User(fp, conf);
////            }
//        }
//
//        // if no owner found, try to get the first owner found in an ancestor
//        for (FeatureRelationship fr : feature.getParentFeatureRelationships()) {
////            Feature parent = (AbstractBioFeature)BioObjectUtil.createBioObject(fr.getObjectFeature(), getConfiguration());
//            Feature parent = fr.parentFeature // may be subject Feature . . not sure
//            User parentOwner = getOwner(parent)
//            if (parentOwner != null) {
//                return parentOwner;
//            }
//        }
//
//        return null;
//    }

//    public void setUser(Feature feature, User owner) {
////        Collection<CVTerm> ownerCvterms = conf.getCVTermsForClass("User");
//        List<CVTerm> ownerCvTerms = CVTerm.findAllByName("Owner")
//
//        for (FeatureProperty fp : feature.getFeatureProperties()) {
////            if (ownerCvterms.contains(fp.getType())) {
//            if (fp.type in ownerCvTerms) {
//                feature.getFeatureProperties().remove(fp);
//                break;
//            }
//        }
//        addProperty(feature, owner);
//    }

//    /** Set the owner of this feature.
//     *
//     * @param owner - User of this feature
//     */
    public void setOwner(Feature feature, String owner) {

        log.debug "looking for owner ${owner}"
        User user = User.findByUsername(owner)
        log.debug "owner ${owner} found ${user}"
        log.debug "feature ${feature}"

        if (user) {
            setOwner(feature, user)
        } else {
            throw new AnnotationException("User ${owner} not found")
        }
//        setOwner(new User(owner));
    }

    public void setOwner(Feature feature, User user) {
        FeatureProperty featureProperty = new FeatureProperty(feature:feature,value:user.username).save()
//        addProperty(feature, user)
        addProperty(feature,featureProperty)
    }
}
