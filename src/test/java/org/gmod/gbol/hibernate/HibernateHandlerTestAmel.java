package org.gmod.gbol.hibernate;

import junit.framework.TestCase;
import org.apache.log4j.PropertyConfigurator;
import org.gmod.gbol.simpleObject.*;
import org.gmod.gbol.simpleObject.io.impl.HibernateHandler;
import org.junit.Ignore;

import java.util.Iterator;

@Ignore
public class HibernateHandlerTestAmel extends TestCase {

    private HibernateHandler handler;
    private Organism organism;
    
    public HibernateHandlerTestAmel() throws Exception
    {
        PropertyConfigurator.configure("testSupport/log4j.properties");
        try {
            handler = new HibernateHandler("testSupport/amel.cfg.xml");
        }
        catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
            throw e;
        }
        organism = new Organism("Apis", "mellifera");
    }

    public void testGetAnalysesForOrganism() throws Exception {
        int counter = 0;
        for(Iterator<? extends Analysis> analyses = handler.getAnalysesForOrganism(organism); analyses.hasNext();) {
            analyses.next();
            counter++;
        }
        assertEquals("Number of genes", 27, counter);
    }
    
    public void testGetFeaturesByCVTerm() throws Exception {
        int counter = 0;
        CV cv = new CV("sequence");
        CVTerm type = new CVTerm("scaffold", cv);
        for (Iterator<? extends Feature> features = handler.getFeaturesByCVTermAndOrganism(type, organism); features.hasNext();) {
            features.next();
            counter++;
        }
        assertEquals("Number of scaffolds", 5644, counter);
        
    }
    
    public void testGetTopLevelFeaturesByOverlappingRangeAndAnalysis() throws Exception {
        int counter = 0;

        Analysis analysis = null;
        for(Iterator<? extends Analysis> analyses = handler.getAnalysesForOrganism(organism); analyses.hasNext();) {
            Analysis a = analyses.next();
            if (a.getProgram().equals("Sanger_EST")) {
                analysis = a;
                break;
            }
        }
        
        CV cv = new CV("sequence");
        CVTerm type = new CVTerm("scaffold", cv);
        Feature srcFeature = handler.getFeature(organism, type, "Group1.1");
        FeatureLocation loc = new FeatureLocation();
        loc.setFmin(382750);
        loc.setFmax(383375);
        loc.setSourceFeature(srcFeature);
        for (Iterator<? extends Feature> features = handler.getTopLevelFeaturesByOverlappingRangeAndAnalysis(loc, analysis); features.hasNext();) {
            Feature f = features.next();
            counter++;
        }
        
        assertEquals("Number of top level features", 8, counter);
    }
}
