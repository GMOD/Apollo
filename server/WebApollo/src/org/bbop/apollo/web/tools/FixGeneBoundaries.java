package org.bbop.apollo.web.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.bbop.apollo.web.datastore.JEDatabase;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.FeatureRelationship;

public class FixGeneBoundaries {
	
	private static Set<String> geneTypes;
	
	static {
		geneTypes = new HashSet<String>();
		geneTypes.addAll(Arrays.asList("gene", "pseudogene"));
	}
	
	public static void fixGeneBoundaries(String inputDb) {
		JEDatabase db = new JEDatabase(inputDb, false);
		List<Feature> featuresToUpdate = new ArrayList<Feature>();
		for (Iterator<Feature> iter = db.getFeatureIterator(); iter.hasNext(); ) {
			Feature feature = iter.next();
			if (geneTypes.contains(feature.getType().getName())) {
				int fmin = Integer.MAX_VALUE;
				int fmax = Integer.MIN_VALUE;
				for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
					Feature child = fr.getSubjectFeature();
					FeatureLocation childLoc = child.getFeatureLocations().iterator().next();
					if (childLoc.getFmin() < fmin) {
						fmin = childLoc.getFmin();
					}
					if (childLoc.getFmax() > fmax) {
						fmax = childLoc.getFmax();
					}
				}
				FeatureLocation featureLoc = feature.getFeatureLocations().iterator().next();
				if (featureLoc.getFmin() != fmin || featureLoc.getFmax() != fmax) {
					System.out.println("Updating " + feature.getUniqueName());
					featureLoc.setFmin(fmin);
					featureLoc.setFmax(fmax);
					featuresToUpdate.add(feature);
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
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}
	
}
