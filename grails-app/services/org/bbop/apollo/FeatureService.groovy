package org.bbop.apollo

import grails.transaction.Transactional
import org.json.JSONObject

/**
 * taken from AbstractBioFeature
 */
@Transactional
class FeatureService {

    def addProperty(Feature feature, FeatureProperty property) {
        int rank = 0;
        for (FeatureProperty fp : feature.getFeatureProperties()) {
            if (fp.getType().equals(property.getFeatureProperty().getType())) {
                if (fp.getRank() > rank) {
                    rank = fp.getRank();
                }
            }
        }
        property.setRank(rank + 1);
        boolean ok = feature.getFeatureProperties().add(property);

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
    public User getOwner(Feature feature) {
//        Collection<CVTerm> ownerCvterms = conf.getCVTermsForClass("Owner");
        List<CVTerm> ownerCvTerms = CVTerm.findAllByName("Owner")

        for (FeatureProperty fp : feature.getFeatureProperties()) {
            if (fp.type in ownerCvTerms) {
                return User.findByUsername(fp.type.name)
            }
//            if (ownerCvterms.contains(fp.getType())) {
//                return new User(fp, conf);
//            }
        }

        // if no owner found, try to get the first owner found in an ancestor
        for (FeatureRelationship fr : feature.getParentFeatureRelationships()) {
//            Feature parent = (AbstractBioFeature)BioObjectUtil.createBioObject(fr.getObjectFeature(), getConfiguration());
            Feature parent = fr.objectFeature // may be subject Feature . . not sure
            User parentOwner = getOwner(parent)
            if (parentOwner != null) {
                return parentOwner;
            }
        }

        return null;
    }

    public void setUser(Feature feature, User owner) {
//        Collection<CVTerm> ownerCvterms = conf.getCVTermsForClass("User");
        List<CVTerm> ownerCvTerms = CVTerm.findAllByName("Owner")

        for (FeatureProperty fp : feature.getFeatureProperties()) {
//            if (ownerCvterms.contains(fp.getType())) {
            if (fp.type in ownerCvTerms ) {
                feature.getFeatureProperties().remove(fp);
                break;
            }
        }
        addProperty(owner);
    }

    /** Set the owner of this feature.
     *
     * @param owner - User of this feature
     */
    public void setOwner(String owner) {
        setOwner(new User(this, owner));
    }

    def generateTranscript(JSONObject jsonTranscript,String trackName){
        Transcript transcript = new Transcript()

        return transcript
    }

}
