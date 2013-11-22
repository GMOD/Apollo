package org.bbop.apollo.web.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.bbop.apollo.web.datastore.JEDatabase;
import org.bbop.apollo.web.datastore.history.JEHistoryDatabase;
import org.bbop.apollo.web.datastore.history.Transaction;
import org.bbop.apollo.web.datastore.history.TransactionList;
import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.Exon;
import org.gmod.gbol.bioObject.MRNA;
import org.gmod.gbol.bioObject.Transcript;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.simpleObject.SimpleObjectIteratorInterface;


public class UpdateTranscriptToMrna {

	private static AbstractSingleLocationBioFeature updateFeature(AbstractSingleLocationBioFeature feature, BioObjectConfiguration conf) {
		if (!feature.getType().equals("sequence:transcript")) {
			return feature;
		}
		Transcript transcript = (Transcript)feature;
		if (transcript.getGene() == null || transcript.getGene().getType().equals("sequence:gene")) {
			SimpleObjectIteratorInterface iterator = transcript.getWriteableSimpleObjects(conf);
			Feature f = (Feature)iterator.next();
			f.getType().setName("mRNA");
			return new MRNA(f, conf);
		}
		return feature;
	}
	
	private static boolean updateFeatures(Transaction transaction, boolean processOldFeatures, BioObjectConfiguration conf) {
		boolean updated = false;
		List<AbstractSingleLocationBioFeature> features = new ArrayList<AbstractSingleLocationBioFeature>();
		List<AbstractSingleLocationBioFeature> tFeatures = processOldFeatures ? transaction.getOldFeatures() : transaction.getNewFeatures();
		for (AbstractSingleLocationBioFeature feature : tFeatures) {
			AbstractSingleLocationBioFeature f = updateFeature(feature, conf);
			if (feature != f) {
				updated = true;
			}
			features.add(f);
			/*
			if (feature.getType().equals("sequence:exon")) {
				Exon exon  = (Exon)feature;
				if (exon.getTranscript() != null) {
					exon.setTranscript((Transcript)updateFeature(exon.getTranscript(), conf));
				}
			}
			*/
		}
		tFeatures.clear();
		tFeatures.addAll(features);
		return updated;
	}
	
	public static void updateHistoryDbs(String[] inputDbs, String mappingFile) {
		BioObjectConfiguration conf = new BioObjectConfiguration(mappingFile);
		for (String inputDb : inputDbs) {
			JEHistoryDatabase db = new JEHistoryDatabase(inputDb);
			List<TransactionList> newTransactionLists = new ArrayList<TransactionList>();
			Iterator<TransactionList> iter = db.getTransactionListIterator();
			while (iter.hasNext()) {
				boolean updated = false;
				TransactionList transactionList = iter.next();
				for (Transaction transaction : transactionList) {
					updated = updateFeatures(transaction, true, conf) || updated;
					updated = updateFeatures(transaction, false, conf) || updated;
				}
				if (updated) {
					newTransactionLists.add(transactionList);
				}
			}
			for (TransactionList transactionList : newTransactionLists) {
				System.out.println("Updating " + transactionList.get(0).getFeatureUniqueName());
				db.writeTransactionListForFeature(transactionList.get(0).getFeatureUniqueName(), transactionList);
			}
		}
	}
	
	public static void updateDbs(String[] inputDbs) {
		for (String inputDb : inputDbs) {
			JEDatabase db = new JEDatabase(inputDb, false);
			Iterator<Feature> iter = db.getFeatureIterator();
			List<Feature> featuresToUpdate = new ArrayList<Feature>();
			while (iter.hasNext()) {
				boolean updated = false;
				Feature feature = iter.next();
				if (feature.getType().getName().equals("gene")) {
					for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
						if (fr.getType().getName().equals("part_of")) {
							Feature subject = fr.getSubjectFeature();
							if (subject.getType().getName().equals("transcript")) {
								subject.getType().setName("mRNA");
								updated = true;
							}
						}
					}
				}
				if (updated) {
					featuresToUpdate.add(feature);
				}
			}
			for (Feature feature : featuresToUpdate) {
				System.out.println("Updating " + feature.getUniqueName());
				db.writeFeature(feature);
			}
			db.close();
		}
	}
	
	public static CommandLine parseOptions(String[] args) {
		CommandLineParser parser = new GnuParser();
		Options options = new Options();
		options.addOption("i", "input", true, "input JE database (can be called multiple times)");
		options.addOption("h", "help", false, "print help");
		options.addOption("H", "history", false, "input database(s) are history databases");
		options.addOption("m", "mapping", true, "mapping file for annotation types");
		CommandLine line = null;
		try {
			line = parser.parse(options, args);
			if (line.hasOption('h')) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("UpdateTranscriptToMrna", options);
				System.exit(1);
			}
			else if (!line.hasOption('i')) {
				System.err.println("Missing required input database(s)");
				System.exit(1);
			}
			else if (!line.hasOption('m')) {
				System.err.println("Missing required mapping file");
				System.exit(1);
			}
		}
		catch( ParseException exp ) {
			System.err.println( "Unexpected exception:" + exp.getMessage() );
			System.exit(1);
		}
		return line;
	}
	
	public static void main(String[] args) {
		try {
			CommandLine line = parseOptions(args);
			if (line.hasOption('H')) {
				updateHistoryDbs(line.getOptionValues('i'), line.getOptionValue('m'));
			}
			else {
				updateDbs(line.getOptionValues('i'));
			}
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}
	
}
