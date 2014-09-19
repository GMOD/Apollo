package org.gmod.gbol.bioObject.util;

import org.gmod.gbol.bioObject.AbstractBioObject;
import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.conf.BioObjectConfigurationException;
import org.gmod.gbol.simpleObject.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/** Class providing utility methods for Bio objects.
 * 
 * @author elee
 *
 */
public class BioObjectUtil {

    private BioObjectUtil()
    {
    }
    
    /** Remove the package identifier from the fully qualified class name
     *  (e.g. my.package.className returns className).  If a class name is passed
     *  with no package identifier, it just returns the class name.
     * 
     * @param className - Class name with fully qualified package identifier
     * @return Class name without fully qualified package identifier
     */
    public static String stripPackageNameFromClassName(String className)
    {
        return className.substring(className.lastIndexOf('.') + 1);
    }
    
    /** Generic method for creating AbstractBioObjects from AbstractSimpleObjects.  Makes use of 
     *  reflection and the passed configuration to figure out how to instantiate the AbstractBioObject.
     * 
     * @param simpleObject - AbstractSimpleObject to create the AbstractBioObject from
     * @param conf - BioObjectConfiguration containing information on how to map Simple->Bio objects
     * @return AbstractBioObject corresponding to the AbstractSimpleObject
     */
    public static AbstractBioObject createBioObject(AbstractSimpleObject simpleObject,
            BioObjectConfiguration conf) {
        if (!(simpleObject instanceof Feature) && !(simpleObject instanceof FeatureRelationship) &&
                !(simpleObject instanceof FeatureProperty)) {
            return null;
        }
        CVTerm cvterm;
        if (simpleObject instanceof Feature) {
            cvterm = ((Feature)simpleObject).getType();
        }
        else if (simpleObject instanceof FeatureRelationship) {
            cvterm = ((FeatureRelationship)simpleObject).getType();
        }
        else {
            cvterm = ((FeatureProperty)simpleObject).getType();
        }
        String className = conf.getClassForCVTerm(cvterm);
        if (className == null) {
            //throw new BioObjectConfigurationException(cvterm.getName() + " does not exist in configuration");
            // TODO: Use log4j or something smart here
            System.err.println(cvterm.getName() + " does not exist in configuration, implicitly casting to Region");
            className = "Region";
        }
        String pkg = AbstractBioObject.class.getPackage().getName();
        try {
            Class<?> clazz = Class.forName(pkg + "." + className);
            Class<? extends AbstractSimpleObject> simpleObjectClass = null;
            if (simpleObject instanceof Feature) {
                simpleObjectClass = Feature.class;
            }
            else if (simpleObject instanceof FeatureRelationship) {
                simpleObjectClass = FeatureRelationship.class;
            }
            else {
                simpleObjectClass = FeatureProperty.class;
            }
            return (AbstractBioObject)clazz.getConstructor(simpleObjectClass,
                    BioObjectConfiguration.class).newInstance(simpleObject, conf);
        }
        catch (ClassNotFoundException e) {
            throw new BioObjectConfigurationException(className + " is not a valid GBOL class type");
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException("Constructor for GBOL object not found: " + e.getMessage());
        }
        catch (InstantiationException e) {
            throw new RuntimeException("Instantiating GBOL object failed: " + e.getMessage());
        }
        catch (IllegalArgumentException e) {
            throw new RuntimeException("Instantiating GBOL object failed: " + e.getMessage());
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException("Instantiating GBOL object failed: " + e.getMessage());
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException("Instantiating GBOL object failed: " + e.getMessage());
        }
    }

    /** Creates a sorted list for AbstractSingleLocationBioFeature objects.  Sorts by location.
     *  The list returned is newly instantiated when this method is called.
     * 
     * @param features - Collection of AbstractSingleLocationBioFeature objects to be sorted
     * @return Sorted list of AbstractSingleLocationBioFeature objects
     */
    public static <T extends AbstractSingleLocationBioFeature> List<T> createSortedFeatureListByLocation(Collection<T> features) {
        return createSortedFeatureListByLocation(features, true);
    }

    public static <T extends AbstractSingleLocationBioFeature> List<T> createSortedFeatureListByLocation(Collection<T> features, boolean sortByStrand) {
        List<T> sortedFeatures = new LinkedList<T>(features);
        Collections.sort(sortedFeatures, new FeaturePositionComparator<T>(sortByStrand));
        return sortedFeatures;
    }

    public static class FeaturePositionComparator<T extends AbstractSingleLocationBioFeature> implements Comparator<T> {
        
        private boolean sortByStrand;
        
        public FeaturePositionComparator() {
            this(true);
        }
        
        public FeaturePositionComparator(boolean sortByStrand) {
            this.sortByStrand = sortByStrand;
        }
        
        public int compare(T feature1, T feature2) {
            
            if (feature1 == null || feature2 == null) {
                System.out.println();
            }
            
            int retVal = 0;
            if (feature1.getFeatureLocation().getFmin() < feature2.getFeatureLocation().getFmin()) {
                retVal = -1;
            }
            else if (feature1.getFeatureLocation().getFmin() > feature2.getFeatureLocation().getFmin()) {
                retVal = 1;
            }
            else if (feature1.getFeatureLocation().getFmax() < feature2.getFeatureLocation().getFmax()) {
                retVal = -1;
            }
            else if (feature1.getFeatureLocation().getFmax() > feature2.getFeatureLocation().getFmax()) {
                retVal = 1;
            }
            else if (feature1.getLength() != feature2.getLength()) {
                retVal = feature1.getLength() < feature2.getLength() ? -1 : 1;
            }
            if (sortByStrand && feature1.getFeatureLocation().getStrand() == -1) {
                retVal *= -1;
            }
            return retVal;
        }
    }

}
