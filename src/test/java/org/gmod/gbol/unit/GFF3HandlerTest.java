package org.gmod.gbol.unit;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gmod.gbol.simpleObject.AbstractSimpleObject;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.simpleObject.io.FileHandler;
import org.gmod.gbol.simpleObject.io.impl.GFF3Handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class GFF3HandlerTest extends TestCase {

    private final Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);

    FileHandler fileHandler;
    private final String filePath = "src/test/resources/testSupport/exampleGFF3.gff";
    @Override
    public void setUp(){
        try {
            this.fileHandler = new GFF3Handler(filePath);
            ((GFF3Handler)this.fileHandler).setSourceFeatureType("chromosome");
            ((GFF3Handler)this.fileHandler).setSequenceOntologyName("SO");
        } catch (IOException e) {
            logger.error("Unable to open " + filePath + " for GFF3HandlerTest support.");
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
    public void testGFF3Read(){
        Collection<Feature> features = new ArrayList<Feature>();
        
        try {
            for (Iterator<? extends AbstractSimpleObject> iter = this.fileHandler.readAll(); iter.hasNext();) {
                features.add((Feature)iter.next());
            }
            logger.info("Read " + features.size() + " features.");
            logger.info("Found " + ((GFF3Handler) this.fileHandler).getTopLevelFeatures().size() + " top level features.");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue("Failed to read " + filePath + ".\nERROR: " + e.getMessage() + "\n",false);
        }
        for (Feature f : ((GFF3Handler)this.fileHandler).getTopLevelFeatures()){
            printFeature(f,0);
        }
    }
    
    private void printFeature(Feature f, int level){
        for (int i=0;i<level;i++){
            System.out.print("\t");
        }
        logger.info(f.getType().getName() + ":\t" + f.getUniqueName());
        for (FeatureRelationship fr : f.getChildFeatureRelationships()){
            printFeature(fr.getSubjectFeature(),(level+1));
        }
    }
    
    
    
    
    
    
    
}
