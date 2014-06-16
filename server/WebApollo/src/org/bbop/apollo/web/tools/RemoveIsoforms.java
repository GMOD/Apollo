package org.bbop.apollo.web.tools;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.bbop.apollo.web.datastore.JEDatabase;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.util.HashComparator;

public class RemoveIsoforms {
	
	private static Set<String> geneTypes;
	private static Random rng = new SecureRandom();
	private static MessageDigest digest;
	private static Comparator<FeatureRelationship> frComparator = new HashComparator<FeatureRelationship>();
	
	static {
		geneTypes = new HashSet<String>();
		geneTypes.addAll(Arrays.asList("gene", "pseudogene"));
		try {
			digest = MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	
	private static String generateId() {
		return DatatypeConverter.printHexBinary(digest.digest((Long.toString(System.nanoTime()) + rng.nextLong()).getBytes()));
	}
	
	public static void fixGeneBoundaries(String inputDb) {
		JEDatabase db = new JEDatabase(inputDb, false);
		List<Feature> featuresToUpdate = new ArrayList<Feature>();
		for (Iterator<Feature> iter = db.getFeatureIterator(); iter.hasNext(); ) {
			Feature feature = iter.next();
			if (geneTypes.contains(feature.getType().getName())) {
				if (feature.getChildFeatureRelationships().size() > 1) {
					System.out.println("Updating " + feature.getUniqueName());
					iter.remove();
					for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
						Feature newFeature = new Feature(feature);
						newFeature.setUniqueName(generateId());
						FeatureRelationship newFr = new FeatureRelationship(fr);
						newFr.setObjectFeature(newFeature);
						newFeature.setChildFeatureRelationships(new TreeSet<FeatureRelationship>(frComparator));
						newFeature.getChildFeatureRelationships().add(newFr);
						fr.getSubjectFeature().setParentFeatureRelationships(new TreeSet<FeatureRelationship>(frComparator));
						fr.getSubjectFeature().getParentFeatureRelationships().add(newFr);
						newFeature.setFeatureLocations(fr.getSubjectFeature().getFeatureLocations());
						featuresToUpdate.add(newFeature);
					}
				}
			}
		}
		for (Feature feature : featuresToUpdate) {
			db.writeFeature(feature);
		}
		db.close();
	}
	
	public static CommandLine parseOptions(String[] args) {
		CommandLineParser parser = new GnuParser();
		Options options = new Options();
		options.addOption("i", "input", true, "input JE database");
		options.addOption("h", "help", false, "print help");
		CommandLine line = null;
		try {
			line = parser.parse(options, args);
			if (line.hasOption('h')) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("RemoveTopLevelFeaturesById", options);
				System.exit(1);
			}
			else if (!line.hasOption('i')) {
				System.err.println("Missing required input database");
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
			fixGeneBoundaries(line.getOptionValue('i'));
		}
		catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}
	
}
