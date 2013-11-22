package org.gmod.gbol.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.PropertyConfigurator;
import org.gmod.gbol.bioObject.AbstractBioFeature;
import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.Exon;
import org.gmod.gbol.bioObject.Gene;
import org.gmod.gbol.bioObject.Transcript;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.io.BioObjectHandler;
import org.gmod.gbol.simpleObject.io.SimpleObjectIOInterface;
import org.gmod.gbol.simpleObject.io.impl.HibernateHandler;

import junit.framework.TestCase;

public class BioObjectHandlerTestGeneDB extends TestCase {

	private BioObjectHandler handler;
	
	public BioObjectHandlerTestGeneDB() throws Exception
	{
		PropertyConfigurator.configure("testSupport/log4j.properties");
		BioObjectConfiguration conf = new BioObjectConfiguration("testSupport/gbolTwo.mapping.xml");
		SimpleObjectIOInterface h = new HibernateHandler("testSupport/gbolTwo.cfg.xml");
		handler = new BioObjectHandler(conf, h);
	}
	
	public void testGetAllFeatures() throws Exception
	{
		Collection<AbstractBioFeature> features = new ArrayList<AbstractBioFeature>();
		for (Iterator<AbstractBioFeature> iter = handler.getAllFeatures(); iter.hasNext(); ) {
			features.add(iter.next());
		}
		assertEquals(features.size(), 8);
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
		for (Iterator<Gene> iter = handler.getAllGenes(); iter.hasNext(); ) {
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
		for (Iterator<Gene> iter = handler.getAllGenes(); iter.hasNext(); ) {
			genes.add(iter.next());
		}
		BioObjectConfiguration destConf = new BioObjectConfiguration("testSupport/gbolThree.mapping.xml");
		SimpleObjectIOInterface h = new HibernateHandler("testSupport/gbolThree.cfg.xml");
		BioObjectHandler destHandler = new BioObjectHandler(destConf, h);
		destHandler.write(genes);
	}
	
	private void printFeatureInfo(AbstractSingleLocationBioFeature feature, int indent)
	{
		for (int i = 0; i < indent; ++i) {
			System.out.print("\t");
		}
		System.out.printf("%s\t(%d,%d)%n", feature.getName(), feature.getFeatureLocation().getFmin(),
				feature.getFeatureLocation().getFmax());
	}

}
