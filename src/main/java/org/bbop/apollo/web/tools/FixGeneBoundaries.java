package org.bbop.apollo.web.tools;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bbop.apollo.web.datastore.JEDatabase;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.FeatureRelationship;

import java.util.*;

public class FixGeneBoundaries {

    private final static Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);

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
                    logger.info("Updating " + feature.getUniqueName());
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
                logger.error("Missing required input database");
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
            CommandLine line = parseOptions(args);
            fixGeneBoundaries(line.getOptionValue('i'));
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            System.exit(1);
        }
    }
    
}
