package org.bbop.apollo.web.tools;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bbop.apollo.web.datastore.JEDatabase;
import org.gmod.gbol.simpleObject.Feature;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class RemoveTopLevelFeaturesById {

    private final static Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);

    public static void removeFeatures(String inputDb, Set<String> idsToRemove) {
        JEDatabase in = new JEDatabase(inputDb, false);
        for (Iterator<Feature> iter = in.getFeatureIterator(); iter.hasNext(); ) {
            Feature feature = iter.next();
            if (idsToRemove.contains(feature.getUniqueName())) {
                iter.remove();
                logger.info("Removing " + feature);
            }
        }
    }
    
    public static CommandLine parseOptions(String[] args) {
        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption("i", "input", true, "input JE database");
        options.addOption("I", "id", true, "ID to remove (can be called multiple times)");
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
                logger.error("Missing required input database");
                System.exit(1);
            }
            else if (!line.hasOption('I')) {
                logger.error("Missing required ID(s) to remove");
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
            CommandLine line = parseOptions(args);
            removeFeatures(line.getOptionValue('i'), new HashSet<String>(Arrays.asList(line.getOptionValues('I'))));
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            System.exit(1);
        }
    }
    
}
