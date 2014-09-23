package org.gmod.gbol.hibernate;

import junit.framework.TestCase;
//import org.apache.log4j.PropertyConfigurator;
import org.gmod.gbol.bioObject.*;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.io.BioObjectHandler;
import org.gmod.gbol.simpleObject.io.SimpleObjectIOInterface;
import org.gmod.gbol.simpleObject.io.impl.HibernateHandler;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

@Ignore
public class BioObjectHandlerTest extends TestCase {

    private BioObjectHandler handler;
    
    public BioObjectHandlerTest() throws Exception
    {
//        PropertyConfigurator.configure("src/test/resources/testSupport/log4j.properties");
        BioObjectConfiguration conf = new BioObjectConfiguration("src/test/resources/testSupport/gbolTwo.mapping.xml");
        SimpleObjectIOInterface h = new HibernateHandler("src/test/resources/testSupport/gbolTwo.cfg.xml");
        handler = new BioObjectHandler(conf, h);
    }
    
    public void testGetAllFeatures() throws Exception
    {
        Collection<AbstractBioFeature> features = new ArrayList<AbstractBioFeature>();
        for (Iterator<AbstractBioFeature> iter = handler.getAllFeatures(); iter.hasNext();) {
            features.add(iter.next());
        }
        assertEquals(8, features.size());
        int numGenes = 0;
        int numTranscripts = 0;
        int numExons = 0;
        for (AbstractBioFeature f : features) {
            if (f instanceof Gene) {
                ++numGenes;
            }
            if (f instanceof Transcript) {
                ++numTranscripts;
            }
            if (f instanceof Exon) {
                ++numExons;
            }
        }
        assertEquals("Expected number of genes", 2, numGenes);
        assertEquals("Expected number of transcripts", 2, numTranscripts);
        assertEquals("Expected number of exons", 3, numExons);
    }
    
    public void testGetGenes() throws Exception
    {
        Collection<Gene> genes = new ArrayList<Gene>();
        for (Iterator<Gene> iter = handler.getAllGenes(); iter.hasNext();) {
            genes.add(iter.next());
        }
        assertEquals(genes.size(), 2);
        for (Gene gene : genes) {
            printFeatureInfo(gene, 0);
            Collection<Transcript> transcripts = gene.getTranscripts();
            assertEquals(transcripts.size(), 1);
            for (Transcript transcript : transcripts) {
                printFeatureInfo(transcript, 1);
                Collection<Exon> exons = transcript.getExons();
                for (Exon exon : exons) {
                    printFeatureInfo(exon, 2);
                }
            }
        }
    }
    
    public void testWrite() throws Exception
    {
        Collection<Gene> genes = new ArrayList<Gene>();
        for (Iterator<Gene> iter = handler.getAllGenes(); iter.hasNext();) {
            genes.add(iter.next());
        }
        BioObjectConfiguration destConf = new BioObjectConfiguration("src/test/resources/testSupport/gbolThree.mapping.xml");
        SimpleObjectIOInterface h = new HibernateHandler("src/test/resources/testSupport/gbolThree.cfg.xml");
        BioObjectHandler destHandler = new BioObjectHandler(destConf, h);
        destHandler.write(genes);
    }
    
    private void printFeatureInfo(AbstractSingleLocationBioFeature feature, int indent)
    {
        for (int i = 0; i < indent; ++i) {
            System.out.print("\t");
        }
        System.out.printf("%s\t(%d,%d)%n", feature.getUniqueName(), feature.getFeatureLocation().getFmin(),
                feature.getFeatureLocation().getFmax());
    }

}
