package org.gmod.gbol.hibernate;

import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gmod.gbol.simpleObject.*;
import org.gmod.gbol.simpleObject.io.impl.HibernateHandler;
import org.junit.Ignore;

import java.util.Iterator;

//import org.apache.log4j.PropertyConfigurator;

@Ignore
public class HibernateHandlerTest extends TestCase {

    private final Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);

    private HibernateHandler handler;
    
    public HibernateHandlerTest() throws Exception
    {
//        PropertyConfigurator.configure("src/test/resources/testSupport/log4j.properties");
        try {
            handler = new HibernateHandler("src/test/resources/testSupport/gbolOne.cfg.xml");
        }
        catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
            throw e;
        }
    }
    
    public void testGetFeaturesByCVTerm() throws Exception
    {
        int counter = 0;
        for( Iterator<? extends Feature> genes = handler.getFeaturesByCVTerm(new CVTerm("gene", new CV("SO"))); genes.hasNext();) {
            Feature f = genes.next();
            logger.info(f.getName());
            counter++;
        }
        assertEquals("Number of genes", 2, counter);
    }
    
    public void testGetAllFeaturesByRangeWithSrcfeature() throws Exception 
    {
        Organism o = new Organism();
        o.setGenus("foomus");
        o.setSpecies("barius");
        CVTerm c = new CVTerm("chromosome", new CV("SO"));
        Feature chr = handler.getFeature(o, c, "2R");

        FeatureLocation loc = new FeatureLocation();
        loc.setFmin(1);
        loc.setFmax(1000);
        loc.setStrand(1);
        loc.setSourceFeature(chr);
        int counter = 0;
        for (Iterator<? extends Feature> features = handler.getAllFeaturesByRange(loc); features.hasNext();) {
            features.next();
            counter++;
        }
        
        assertEquals("Number of features on 2R from 1-1000", 1, counter);
        
    }
    
    public void testGetChromosomeByOrganismAndTypeAndName() throws Exception
    {
        Organism o = new Organism();
        o.setGenus("foomus");
        o.setSpecies("barius");
        CVTerm c = new CVTerm("chromosome", new CV("SO"));
        Feature chr = handler.getFeature(o, c, "2R");
        
        assertEquals("Chromosome feature is named 2R", "2R", chr.getUniqueName());
    }
    
    public void testGetFeaturesByCVTermAndRangeAndSrcfeature() throws Exception
    {
        CV cv = new CV("SO");
        Organism o = new Organism();
        o.setGenus("foomus");
        o.setSpecies("barius");
        CVTerm c = new CVTerm("chromosome", cv);
        Feature chr = handler.getFeature(o, c, "2R");

        FeatureLocation loc = new FeatureLocation();
        loc.setFmin(1);
        loc.setFmax(1000);
        loc.setStrand(1);
        loc.setSourceFeature(chr);
        int counter = 0;
        for (Iterator<? extends Feature> features = handler.getFeaturesByCVTermAndRange(new CVTerm("gene", cv), loc); features.hasNext();) {
        //for (Iterator<? extends Feature> features = handler.getAllFeaturesByRange(loc); features.hasNext();) {
            features.next();
            counter++;
        }
        assertEquals("Number of genes (1-1000)", 1, counter);
    }
    
    public void testGetAllFeatureByOverlappingRange() throws Exception {
        CV cv = new CV("SO");
        Organism o = new Organism();
        o.setGenus("foomus");
        o.setSpecies("barius");
        CVTerm c = new CVTerm("chromosome", cv);
        Feature chr = handler.getFeature(o, c, "2R");

        FeatureLocation loc = new FeatureLocation();
        loc.setFmin(150);
        loc.setFmax(250);
        loc.setStrand(1);
        loc.setSourceFeature(chr);
        int counter = 0;
        for (Iterator<? extends Feature> features = handler.getAllFeaturesByOverlappingRange(loc); features.hasNext();) {
            features.next();
            counter++;
        }
        assertEquals("Number of genes (1-1000)", 1, counter);
    }
    
    public void testGetAllFeatureBySourceFeature() throws Exception {
        CV cv = new CV("SO");
        Organism o = new Organism();
        o.setGenus("foomus");
        o.setSpecies("barius");
        CVTerm c = new CVTerm("chromosome", cv);
        Feature chr = handler.getFeature(o, c, "2R");

        int counter = 0;
        for (Iterator<? extends Feature> features = handler.getAllFeaturesBySourceFeature(chr); features.hasNext();) {
            features.next();
            counter++;
        }
        assertEquals("Number of genes (1-1000)", 1, counter);
        
        counter = 0;
        for (Iterator<? extends Feature> features = handler.getAllFeaturesBySourceFeature(chr, true); features.hasNext();) {
            features.next();
            counter++;
        }
        assertEquals("Number of genes (1-1000)", 1, counter);

    }    
    
    public void testGetFeaturesByCVTermAndOverlappingRange() throws Exception
    {
        CV cv = new CV("SO");
        Organism o = new Organism();
        o.setGenus("foomus");
        o.setSpecies("barius");
        CVTerm c = new CVTerm("chromosome", cv);
        Feature chr = handler.getFeature(o, c, "2R");

        FeatureLocation loc = new FeatureLocation();
        loc.setFmin(250);
        loc.setFmax(400);
        loc.setStrand(1);
        loc.setSourceFeature(chr);
        int counter = 0;
        for (Iterator<? extends Feature> features = handler.getFeaturesByCVTermAndOverlappingRange(new CVTerm("gene", cv), loc); features.hasNext();) {
            features.next();
            counter++;
        }
        assertEquals("Number of genes (1-1000)", 1, counter);
    }
    
    public void testGetOrganismsWithFeatures() throws Exception {
        int counter = 0;
        for (Iterator<? extends Organism> organisms = handler.getOrganismsWithFeatures(); organisms.hasNext();) {
            organisms.next();
            counter++;
        }
        assertEquals("Number of organisms with features: ", 1, counter);
    }
    
    protected void setUp()
    {
        //handler.beginTransaction();
    }
    
    protected void tearDown()
    {
        //handler.rollbackTransaction();
        //handler.closeSession();
    }
}
