package org.bbop.apollo.web.dataadapter.gff3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletResponse;

import org.bbop.apollo.web.config.ServerConfiguration;
import org.bbop.apollo.web.data.FeatureLazyResidues;
import org.bbop.apollo.web.data.FeatureSequenceChunkManager;
import org.bbop.apollo.web.dataadapter.DataAdapter;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.io.GFF3Handler;
import org.gmod.gbol.simpleObject.CV;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Gff3DataAdapter extends DataAdapter {
    private static final long serialVersionUID = 1L;

    private String dataStoreDirectory;
    private Map<String, Feature> trackToSourceFeature;
    private BioObjectConfiguration bioObjectConfiguration;
    private String realPath;
    private String path;
    private String source;
    private Set<String> metaDataToExport;
    private boolean exportSourceGenomicSequence;

    private enum Output {
        DISPLAY,
        FILE
    }

    public Gff3DataAdapter() {
        super();
        trackToSourceFeature = new HashMap<String, Feature>();
        metaDataToExport = new HashSet<String>();
    }

    @Override
    public void init(ServerConfiguration serverConfig, String configPath, String basePath) throws DataAdapterException {
        try {
            Document doc = getXMLDocument(basePath, configPath);
            Node tmpDirNode = doc.getElementsByTagName("tmp_dir").item(0);
            if (tmpDirNode == null) {
                throw new DataAdapterException("Configuration missing required 'tmp_dir' element");
            }
            path = tmpDirNode.getTextContent();
            realPath = basePath + "/" + path;
            Node sourceNode = doc.getElementsByTagName("source").item(0);
            source = sourceNode != null ? source = sourceNode.getTextContent() : ".";
            Node metaDataNode = doc.getElementsByTagName("metadata_to_export").item(0);
            if (metaDataNode != null) {
                NodeList metaDataList = ((Element) metaDataNode).getElementsByTagName("metadata");
                for (int i = 0; i < metaDataList.getLength(); ++i) {
                    String type = ((Element) metaDataList.item(i)).getAttribute("type");
                    if (type.length() > 0) {
                        metaDataToExport.add(type);
                    }
                }
            } else {
                metaDataToExport.add("name");
                metaDataToExport.add("symbol");
                metaDataToExport.add("description");
                metaDataToExport.add("status");
                metaDataToExport.add("dbxrefs");
                metaDataToExport.add("attributes");
                metaDataToExport.add("pubmed_ids");
                metaDataToExport.add("go_ids");
                metaDataToExport.add("comments");
            }
            Node exportSourceGenomicSequenceNode = doc.getElementsByTagName("export_source_genomic_sequence").item(0);
            exportSourceGenomicSequence = exportSourceGenomicSequenceNode != null ? Boolean.parseBoolean(exportSourceGenomicSequenceNode.getTextContent()) : true;
            bioObjectConfiguration = new BioObjectConfiguration(basePath + serverConfig.getGBOLMappingFile());
            dataStoreDirectory = serverConfig.getDataStoreDirectory();
            for (ServerConfiguration.TrackConfiguration track : serverConfig.getTracks().values()) {
                FeatureSequenceChunkManager chunkManager = FeatureSequenceChunkManager.getInstance(track.getName());
                chunkManager.setSequenceDirectory(track.getSourceFeature().getSequenceDirectory());
                chunkManager.setChunkSize(track.getSourceFeature().getSequenceChunkSize());
                chunkManager.setChunkPrefix(track.getSourceFeature().getSequenceChunkPrefix());
                chunkManager.setSequenceLength(track.getSourceFeature().getEnd());
                Feature sourceFeature = new FeatureLazyResidues(track.getName());
                sourceFeature.setUniqueName(track.getSourceFeature().getUniqueName());
                String[] type = track.getSourceFeature().getType().split(":");
                sourceFeature.setType(new CVTerm(type[1], new CV(type[0])));
                FeatureLocation loc = new FeatureLocation();
                loc.setFmin(track.getSourceFeature().getStart());
                loc.setFmax(track.getSourceFeature().getEnd());
                sourceFeature.addFeatureLocation(new FeatureLocation(loc));
                trackToSourceFeature.put(track.getName(), sourceFeature);
            }
        } catch (Exception e) {
            throw new DataAdapterException(e.getMessage());
        }
    }

    @Override
    public void write(List<String> tracks, Map<String, String[]> parameters, HttpServletResponse response) throws IOException {
        try {
            if (tracks.size() == 0) {
                return;
            }
            Output output = Output.DISPLAY;
            String outputParameter = getParameter(parameters, "output");
            if (outputParameter != null) {
                if (outputParameter.equals("display")) {
                    output = Output.DISPLAY;
                } else if (outputParameter.equals("file")) {
                    output = Output.FILE;
                }
            }
            GFF3Handler.Format format = GFF3Handler.Format.TEXT;
            String formatParameter = getParameter(parameters, "format");
            if (formatParameter != null) {
                if (formatParameter.equals("text")) {
                    format = GFF3Handler.Format.TEXT;
                } else if (formatParameter.equals("gzip")) {
                    format = GFF3Handler.Format.GZIP;
                }
            }

            String uniquePath = getParameter(parameters, "session_id") + "/" + new Date().getTime();
            File outputDir = new File(realPath + "/" + uniquePath);
            outputDir.mkdirs();
            Gff3JEDatabaseIO gff3IO = null;
            String filename = tracks.size() > 1 ? "annotations.gff" : tracks.get(0) + ".gff";
            if (format.equals(GFF3Handler.Format.GZIP)) {
                filename += ".gz";
            }

            File tmpFile = new File(outputDir.getAbsolutePath() + "/" + filename + ".tmp");
            File doneFile = new File(outputDir.getAbsolutePath() + "/" + filename);

            List<Feature> sourceFeatures = new ArrayList<Feature>();
            List<Feature> sequenceAlterations = new ArrayList<Feature>();
            for (String track : tracks) {
                String jePath = dataStoreDirectory + "/" + track;
                File dataPath = new File(jePath);
                if (!dataPath.exists()) {
                    continue;
                    //dataPath.mkdirs();
                }
                if (gff3IO == null) {
                    gff3IO = new Gff3JEDatabaseIO(jePath, tmpFile.getAbsolutePath(), source, bioObjectConfiguration, false, format, metaDataToExport);
                } else {
                    gff3IO.setJeDatabase(jePath, false);
                }
                Feature sourceFeature = trackToSourceFeature.get(track);
                sourceFeatures.add(sourceFeature);
                sequenceAlterations.addAll(gff3IO.writeFeatures(sourceFeature, source));
            }
            if (gff3IO != null) {
                if (exportSourceGenomicSequence) {
                    gff3IO.writeFasta(sourceFeatures);
                    gff3IO.writeFasta(sequenceAlterations, false, false);
                } else {
                    gff3IO.writeFasta(sequenceAlterations, true, false);
                }
                gff3IO.close();
            }

            tmpFile.renameTo(doneFile);

            PrintWriter out = response.getWriter();
            switch (output) {
                case DISPLAY: {
                    response.setContentType("text/plain");
                    BufferedReader reader = null;
                    if (format.equals(GFF3Handler.Format.TEXT)) {
                        reader = new BufferedReader(new InputStreamReader(new FileInputStream(outputDir.getAbsolutePath() + "/" + filename)));
                    } else if (format.equals(GFF3Handler.Format.GZIP)) {
                        reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(outputDir.getAbsolutePath() + "/" + filename))));
                    }
                    String line;
                    while ((line = reader.readLine()) != null) {
                        out.println(line);
                    }
                    reader.close();
                    break;
                }
                case FILE: {
                    response.setContentType("text/html");
                    out.println("<html><head></head><body><iframe name='hidden_iframe' style='display:none'></iframe><a href='" + path + "/" + uniquePath + "/" + filename + "' target='hidden_iframe'>Download " + filename + "</a></body></html>");
                }
            }
        } catch (Exception e) {
            StringWriter buf = new StringWriter();
            e.printStackTrace(new PrintWriter(buf));
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error writing GFF3" + "\n" + buf.toString());
        }
    }


    @Override
    public void read(List<String> tracks, Map<String, String[]> parameters, HttpServletResponse response) throws IOException {
        // TODO Auto-generated method stub

    }

}
