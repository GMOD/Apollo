package org.bbop.apollo.web.dataadapter.fasta;

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
import org.gmod.gbol.bioObject.io.FastaHandler;
import org.gmod.gbol.bioObject.io.GFF3Handler;
import org.gmod.gbol.simpleObject.CV;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.util.SequenceUtil;
import org.gmod.gbol.util.SequenceUtil.TranslationTable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class FastaDataAdapter extends DataAdapter {
    private static final long serialVersionUID = 1L;

    private String dataStoreDirectory;
    private Map<String, Feature> trackToSourceFeature;
    private BioObjectConfiguration bioObjectConfiguration;
    private String realPath;
    private String path;
    private Map<String, TranslationTable> trackToTranslationTable;
    private Set<String> featureTypes;
    private Set<String> metaDataToExport;
    
    private enum Output {
        DISPLAY,
        FILE
    }
    
    public FastaDataAdapter() {
        super();
        trackToSourceFeature = new HashMap<String, Feature>();
        trackToTranslationTable = new HashMap<String, TranslationTable>();
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
            featureTypes = new HashSet<String>();
            NodeList featureTypeNodes = doc.getElementsByTagName("feature_type");
            for (int i = 0; i < featureTypeNodes.getLength(); ++i) {
                featureTypes.add(featureTypeNodes.item(i).getTextContent());
            }
            metaDataToExport = new HashSet<String>();
            NodeList metaDataNodes = doc.getElementsByTagName("metadata");
            for (int i = 0; i < metaDataNodes.getLength(); ++i) {
                Node metaDataNode = metaDataNodes.item(i).getAttributes().getNamedItem("type");
                if (metaDataNode != null) {
                    metaDataToExport.add(metaDataNode.getTextContent());
                }
            }
            
            bioObjectConfiguration = new BioObjectConfiguration(basePath + serverConfig.getGBOLMappingFile());
            dataStoreDirectory = serverConfig.getDataStoreDirectory();
            Map<String, SequenceUtil.TranslationTable> ttables = new HashMap<String, SequenceUtil.TranslationTable>();
            for (ServerConfiguration.TrackConfiguration track : serverConfig.getTracks().values()) {
                FeatureSequenceChunkManager chunkManager = FeatureSequenceChunkManager.getInstance(track.getName());
                chunkManager.setSequenceDirectory(track.getSourceFeature().getSequenceDirectory());
                chunkManager.setChunkSize(track.getSourceFeature().getSequenceChunkSize());
                chunkManager.setChunkPrefix(track.getSourceFeature().getSequenceChunkPrefix());
                chunkManager.setSequenceLength(track.getSourceFeature().getEnd());
                Feature sourceFeature = new FeatureLazyResidues(track.getName());
                sourceFeature.setUniqueName(track.getSourceFeature().getUniqueName());
                String [] type = track.getSourceFeature().getType().split(":");
                sourceFeature.setType(new CVTerm(type[1], new CV(type[0])));
                FeatureLocation loc = new FeatureLocation();
                loc.setFmin(track.getSourceFeature().getStart());
                loc.setFmax(track.getSourceFeature().getEnd());
                sourceFeature.addFeatureLocation(new FeatureLocation(loc));
                trackToSourceFeature.put(track.getName(), sourceFeature);
                if (track.getTranslationTable() != null) {
                    TranslationTable ttable;
                    if (ttables.containsKey(track.getTranslationTable())) {
                        ttable = ttables.get(track.getTranslationTable());
                    }
                    else {
                        ttable = SequenceUtil.getDefaultTranslationTable().cloneTable();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(basePath + track.getTranslationTable())));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String []tokens = line.split("\t");
                            String codon = tokens[0].toUpperCase();
                            String aa = tokens[1].toUpperCase();
                            ttable.getTranslationTable().put(codon, aa);
                            if (aa.equals(TranslationTable.STOP)) {
                                ttable.getStopCodons().add(codon);
                                if (tokens.length == 3) {
                                    ttable.getAlternateTranslationTable().put(codon, tokens[2]);
                                }
                                else {
                                    ttable.getAlternateTranslationTable().remove(codon);
                                }
                            }
                            else {
                                ttable.getStopCodons().remove(codon);
                                ttable.getAlternateTranslationTable().remove(codon);
                            }
                            if (tokens.length == 3) {
                                if (tokens[2].equals("start")) {
                                    ttable.getStartCodons().add(codon);
                                }
                            }
                            else {
                                ttable.getStartCodons().remove(codon);
                            }
                        }
                        reader.close();
                    }
                    ttables.put(track.getTranslationTable(), ttable);
                    trackToTranslationTable.put(track.getName(), ttable);
                }
                else {
                    trackToTranslationTable.put(track.getName(), SequenceUtil.getDefaultTranslationTable());
                }
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
                }
                else if (outputParameter.equals("file")) {
                    output = Output.FILE;
                }
            }
            FastaHandler.Format format = FastaHandler.Format.TEXT;
            String formatParameter = getParameter(parameters, "format");
            if (formatParameter != null) {
                if (formatParameter.equals("text")) {
                    format = FastaHandler.Format.TEXT;
                }
                else if (formatParameter.equals("gzip")) {
                    format = FastaHandler.Format.GZIP;
                }
            }
            String seqTypeParameter = getParameter(parameters, "seqType");

            String uniquePath = getParameter(parameters, "session_id") + "/" + new Date().getTime();
            File outputDir = new File(realPath + "/" + uniquePath);
            outputDir.mkdirs();
            FastaJEDatabaseIO fastaIO = null;
            String filename = tracks.size() > 1 ? "annotations." + seqTypeParameter + ".fasta" : tracks.get(0) + "." + seqTypeParameter + ".fasta";
            if (format.equals(FastaHandler.Format.GZIP)) {
                filename += ".gz";
            }
            
            File tmpFile = new File(outputDir.getAbsolutePath() + "/" + filename + ".tmp");
            File doneFile = new File(outputDir.getAbsolutePath() + "/" + filename);
            
            for (String track : tracks) {
                String jePath = dataStoreDirectory + "/" + track;
                File dataPath = new File(jePath);
                if (!dataPath.exists()) {
                    continue;
                    //dataPath.mkdirs();
                }
                if (fastaIO == null) {
                    fastaIO = new FastaJEDatabaseIO(jePath, tmpFile.getAbsolutePath(), seqTypeParameter, bioObjectConfiguration, false, format, trackToTranslationTable.get(track));
                }
                else {
                    fastaIO.setJeDatabase(jePath, false);
                }
                Feature sourceFeature = trackToSourceFeature.get(track);
                fastaIO.writeFeatures(sourceFeature, seqTypeParameter, featureTypes, metaDataToExport);
            }
            if(fastaIO!=null){
                fastaIO.close();
            }

            tmpFile.renameTo(doneFile);
            
            PrintWriter out = response.getWriter();
            switch (output) {
            case DISPLAY:
            {
                response.setContentType("text/plain");
                BufferedReader reader = null;
                if (format.equals(FastaHandler.Format.TEXT)) {
                    reader = new BufferedReader(new InputStreamReader(new FileInputStream(outputDir.getAbsolutePath() + "/" + filename)));
                }
                else if (format.equals(FastaHandler.Format.GZIP)) {
                    reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(outputDir.getAbsolutePath() + "/" + filename))));
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    out.println(line);
                }
                reader.close();
                break;
            }
            case FILE:
            {
                response.setContentType("text/html");
                out.println("<html><head></head><body><iframe name='hidden_iframe' style='display:none'></iframe><a href='" + path + "/" + uniquePath + "/" + filename + "' target='hidden_iframe'>Download " + filename + "</a></body></html>");
            }
            }
        }
        catch (Exception e) {
            StringWriter buf = new StringWriter();
            e.printStackTrace(new PrintWriter(buf));
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error writing FASTA" + "\n" + buf.toString());
        }
    }


    @Override
    public void read(List<String> tracks, Map<String, String[]> parameters, HttpServletResponse response) throws IOException {
        // TODO Auto-generated method stub
        
    }

}
