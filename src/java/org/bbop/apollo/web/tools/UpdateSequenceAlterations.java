package org.bbop.apollo.web.tools;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.bbop.apollo.web.datastore.JEDatabase;
import org.gmod.gbol.simpleObject.Feature;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UpdateSequenceAlterations {

    private final static Logger logger = Logger.getLogger(UpdateSequenceAlterations.class);

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
                logger.error("Missing required input database(s)");
                System.exit(1);
            }
        }
        catch( ParseException exp ) {
            logger.error("Unexpected exception:" + exp.getMessage());
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
            logger.error(e.getMessage());
            System.exit(1);
        }
    }
    
}
