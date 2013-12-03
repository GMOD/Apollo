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
import org.gmod.gbol.simpleObject.Feature;

public class UpdateSequenceAlterations {
	
	public static void fixDbs(String[] inputDbs, String outputDb, boolean forceOverwrite) {
		for (String inputDb : inputDbs) {
			JEDatabase in = new JEDatabase(inputDb, false);
			List<Feature> sequenceAlterations = new ArrayList<Feature>();
			/*
			in.readSequenceAlterations(sequenceAlterations);
			for (Feature sequenceAlteration : sequenceAlterations) {
				in.deleteSequenceAlteration(sequenceAlteration);
			}
			*/
			for (Iterator<Feature> iter = in.getSequenceAlterationIterator(); iter.hasNext(); ) {
				iter.remove();
				sequenceAlterations.add(iter.next());
			}
			for (Feature sequenceAlteration : sequenceAlterations) {
				in.writeSequenceAlteration(sequenceAlteration);
			}
		}
	}
	
	public static CommandLine parseOptions(String[] args) {
		CommandLineParser parser = new GnuParser();
		Options options = new Options();
		options.addOption("i", "input", true, "input JE database (can be called multiple times)");
		options.addOption("h", "help", false, "print help");
		CommandLine line = null;
		try {
			line = parser.parse(options, args);
			if (line.hasOption('h')) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("UpdateSequenceAlterations", options);
				System.exit(1);
			}
			else if (!line.hasOption('i')) {
				System.err.println("Missing required input database(s)");
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
			fixDbs(line.getOptionValues('i'), line.getOptionValue('o'), line.hasOption('f'));
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}
	
}
