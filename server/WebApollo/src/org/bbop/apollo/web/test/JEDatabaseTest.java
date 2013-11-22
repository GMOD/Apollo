package org.bbop.apollo.web.test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bbop.apollo.web.config.ServerConfiguration;
import org.bbop.apollo.web.datastore.AbstractDataStore;
import org.bbop.apollo.web.datastore.JEDatabase;
import org.gmod.gbol.bioObject.Chromosome;
import org.gmod.gbol.bioObject.Exon;
import org.gmod.gbol.bioObject.Gene;
import org.gmod.gbol.bioObject.Insertion;
import org.gmod.gbol.bioObject.Transcript;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.simpleObject.SimpleObjectIteratorInterface;

import junit.framework.TestCase;

public class JEDatabaseTest extends TestCase {

	private static ServerConfiguration serverConfig;
	static {
		try {
			serverConfig = new ServerConfiguration("WebContent/config/config.xml");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	private String databaseDir = serverConfig.getDataStoreDirectory() + "/test";
	private String mappingFile = System.getProperty("user.dir") + "/WebContent/config/mapping.xml";
	private BioObjectConfiguration conf = new BioObjectConfiguration(mappingFile);
	
	public void testWriteFeature() {
//		JEDatabase db = new JEDatabase(databaseDir);
//		db.writeFeature(createGene(0, 100, 1, 1));
//		db.writeFeature(createGene(200, 300, 1, 2));
//		db.close();
		
		List<Feature> features = new ArrayList<Feature>();
		features.add(createGene(0, 100, 1, 1));
		features.add(createGene(200, 300, 1, 2));
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(databaseDir + "/data.bin")));
			oos.writeObject(features);
			oos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void testReadFeatures() {
//		List<Feature> features = new ArrayList<Feature>();
//		AbstractDataStore db = new JEDatabase(databaseDir);
//		db.readFeatures(features);
		
		List<Feature> features = null;
		try {
			ObjectInputStream iis = new ObjectInputStream(new BufferedInputStream(new FileInputStream(databaseDir + "/data.bin")));
			features = (List<Feature>)iis.readObject();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		assertEquals("Number of features: ", new Integer(2), new Integer(features.size()));
		
		for (Feature gene : features) {
			Feature transcript = gene.getChildFeatureRelationships().iterator().next().getSubjectFeature();
			System.out.println("Number of exons (before): " + transcript.getChildFeatureRelationships().size());
			Iterator<FeatureRelationship> iter = transcript.getChildFeatureRelationships().iterator();
			if (iter.hasNext()) {
				iter.next();
				iter.remove();
			}
			System.out.println("Number of exons (before): " + transcript.getChildFeatureRelationships().size());
			System.out.println();
		}
		
	}
	
	public void testWriteSequenceAlteration() {
		JEDatabase db = new JEDatabase(databaseDir);
		db.writeSequenceAlteration(createInsertion(0, 100, 1, 1));
		db.close();
	}

	public void testReadSequenceAlterations() {
		List<Feature> sequenceAlterations = new ArrayList<Feature>();
		AbstractDataStore db = new JEDatabase(databaseDir);
		db.readSequenceAlterations(sequenceAlterations);
		assertEquals("Number of sequence alterations: ", new Integer(1), new Integer(sequenceAlterations.size()));
	}
	
	private Feature createGene(int fmin, int fmax, int strand, int geneNum) {
		BioObjectConfiguration conf = new BioObjectConfiguration(mappingFile);
		Chromosome chromosome = new Chromosome(null, "chromosome", false, false, null, conf);
		Gene gene = new Gene(null, "gene-" + geneNum, false, false, null, conf);
		gene.setFeatureLocation(fmin, fmax, strand, chromosome);
		Transcript transcript = new Transcript(null, "transcript-" + geneNum, false, false, null, conf);
		transcript.setFeatureLocation(fmin, fmax, strand, chromosome);
		gene.addTranscript(transcript);
		Exon exon = new Exon(null, "exon-" + geneNum, false, false, null, conf);
		exon.setFeatureLocation(fmin, fmax, strand, chromosome);
		transcript.addExon(exon);
		return (Feature)((SimpleObjectIteratorInterface)gene.getWriteableSimpleObjects(conf)).next();
	}

	private Feature createInsertion(int fmin, int fmax, int strand, int insertionNum) {
		Chromosome chromosome = new Chromosome(null, "chromosome", false, false, null, conf);
		Insertion insertion = new Insertion(null, "insertion-" + insertionNum, false, false, null, conf);
		insertion.setFeatureLocation(fmin, fmax, strand, chromosome);
		return (Feature)((SimpleObjectIteratorInterface)insertion.getWriteableSimpleObjects(conf)).next();
	}

}
