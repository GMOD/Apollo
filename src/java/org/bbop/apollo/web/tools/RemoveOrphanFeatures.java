package org.bbop.apollo.web.tools;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.bbop.apollo.web.datastore.JEDatabase;
import org.gmod.gbol.simpleObject.Feature;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class RemoveOrphanFeatures {

    private final static Logger logger = Logger.getLogger(RemoveOrphanFeatures.class);

    public static void fixDbs(String[] inputDbs, String outputDb, boolean forceOverwrite, Set<String> typesToRemove) {
        for (String inputDb : inputDbs) {
            JEDatabase in = new JEDatabase(inputDb, false);
            for (Iterator<Feature> iter = in.getFeatureIterator(); iter.hasNext(); ) {
                Feature feature = iter.next();
                if (typesToRemove.contains(feature.getType().toString())) {
                    iter.remove();
                    logger.info("Removing " + feature);
                }
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
                formatter.printHelp("RemoveOrphanFeatures", options);
                System.exit(1);
            }
            else if (!line.hasOption('i')) {
                logger.error("Missing required input database(s)");
                System.exit(1);
            }
        }
        catch( ParseException exp ) {
            logger.error( "Unexpected exception:" + exp.getMessage() );
            System.exit(1);
        }
        return line;
    }
    
    public static void main(String[] args) {
        try {
            String[] typesToRemove = new String[] {
                    "sequence:transcript",
                    "sequence:mRNA",
                    "sequence:tRNA",
                    "sequence:snRNA",
                    "sequence:snoRNA",
                    "sequence:ncRNA",
                    "sequence:miRNA",
                    "sequence:rRNA",
                    "sequence:exon",
                    "sequence:CDS"
            };
            CommandLine line = parseOptions(args);
            fixDbs(line.getOptionValues('i'), line.getOptionValue('o'), line.hasOption('f'), new HashSet<String>(Arrays.asList(typesToRemove)));
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            System.exit(1);
        }
    }
    
}
