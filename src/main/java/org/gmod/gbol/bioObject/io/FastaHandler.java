package org.gmod.gbol.bioObject.io;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.gmod.gbol.bioObject.AbstractBioFeature;
import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.CDS;
import org.gmod.gbol.bioObject.Comment;
import org.gmod.gbol.bioObject.Exon;
import org.gmod.gbol.bioObject.GenericFeatureProperty;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.DBXref;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.simpleObject.FeatureSynonym;
import org.gmod.gbol.simpleObject.SimpleObjectIteratorInterface;

public class FastaHandler {

    private File file;
    private PrintWriter out;
    private Mode mode;
    private int numResiduesPerLine;
    
    public enum Mode {
        READ,
        WRITE
    }
    
    public enum Format {
        TEXT,
        GZIP
    }
    
    public FastaHandler(String path, Mode mode) throws IOException {
        this(path, mode, Format.TEXT);
    }
    
    public FastaHandler(String path, Mode mode, Format format) throws IOException {
        numResiduesPerLine = 60;
        this.mode = mode;
        file = new File(path);
        file.createNewFile();
        if (mode == Mode.READ) {
            if (!file.canRead()) {
                throw new IOException("Cannot read FASTA file: " + file.getAbsolutePath());
            }
        }
        if (mode == Mode.WRITE) {
            if (!file.canWrite()) {
                throw new IOException("Cannot write FATA to: " + file.getAbsolutePath());
            }
            switch (format) {
            case TEXT:
                out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
                break;
            case GZIP:
                out = new PrintWriter(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file))));
            }
        }
    }
    
    public void close() {
        if (mode == Mode.READ) {
            //TODO
        }
        else if (mode == Mode.WRITE) { 
            out.close();
        }
    }
    
    public void writeFeatures(Collection<? extends AbstractSingleLocationBioFeature> features, String seqType, Set<String> metaDataToExport) throws IOException {
        writeFeatures(features.iterator(), seqType, metaDataToExport);
    }
    
    public void writeFeatures(Iterator<? extends AbstractSingleLocationBioFeature> iterator, String seqType, Set<String> metaDataToExport) throws IOException {
        if (mode != Mode.WRITE) {
            throw new IOException("Cannot write to file in READ mode");
        }
        while (iterator.hasNext()) {
            AbstractSingleLocationBioFeature feature = iterator.next();
            writeFeature(feature, seqType, metaDataToExport);
        }
    }
    
    public void writeFeature(AbstractSingleLocationBioFeature feature, String seqType, Set<String> metaDataToExport) {
        String defline = String.format(">%s (%s) %d residues [%s]", feature.getUniqueName(), feature.getType(), feature.getResidues().length(), seqType);
        if (!metaDataToExport.isEmpty()) {
            boolean first = true;
            if (metaDataToExport.contains("name") && feature.getName() != null) {
                if (!first) {
                    defline += ";";
                }
                else {
                    defline += " ";
                }
                first = false;
                defline += "name=" + feature.getName();
            }
            if (metaDataToExport.contains("symbol") && feature.getSymbol() != null) {
                if (!first) {
                    defline += ";";
                }
                else {
                    defline += " ";
                }
                first = false;
                defline += "symbol=" + feature.getSymbol().getSymbol();
            }
            if (metaDataToExport.contains("description") && feature.getDescription() != null) {
                if (!first) {
                    defline += ";";
                }
                else {
                    defline += " ";
                }
                first = false;
                defline += "description=" + feature.getDescription().getDescription();
            }
            if (metaDataToExport.contains("status") && feature.getStatus() != null) {
                if (!first) {
                    defline += ";";
                }
                else {
                    defline += " ";
                }
                first = false;
                defline += "status=" + feature.getStatus().getStatus();
            }
            if (metaDataToExport.contains("dbxrefs")) {
                Iterator<DBXref> dbxrefIter = feature.getNonPrimaryDBXrefs().iterator();
                if (dbxrefIter.hasNext()) {
                    if (!first) {
                        defline += ";";
                    }
                    else {
                        defline += " ";
                    }
                    first = false;
                    StringBuilder dbxrefs = new StringBuilder();
                    DBXref dbxref = dbxrefIter.next();
                    dbxrefs.append(dbxref.getDb().getName() + ":" + dbxref.getAccession());
                    while (dbxrefIter.hasNext()) {
                        dbxrefs.append(",");
                        dbxref = dbxrefIter.next();
                        dbxrefs.append(dbxref.getDb().getName() + ":" + dbxref.getAccession());
                    }
                    defline += "dbxrefs=" + dbxrefs.toString();
                }
            }
            if (metaDataToExport.contains("attributes")) {
                Iterator<GenericFeatureProperty> propertyIter = feature.getNonReservedProperties().iterator();
                if (propertyIter.hasNext()) {
                    Map<String, StringBuilder> properties = new HashMap<String, StringBuilder>();
                    do {
                        GenericFeatureProperty prop = propertyIter.next();
                        StringBuilder props = properties.get(prop.getTag());
                        if (props == null) {
                            props = new StringBuilder();
                            properties.put(prop.getTag(), props);
                        }
                        else {
                            props.append(",");
                        }
                        props.append(prop.getValue());
                    }
                    while (propertyIter.hasNext());
                    if (!first) {
                        defline += ";";
                    }
                    else {
                        defline += " ";
                    }
                    first = false;
                    for (Map.Entry<String, StringBuilder> iter : properties.entrySet()) {
                        defline += iter.getKey() + "=" + iter.getValue().toString();
                    }
                }
            }
            if (metaDataToExport.contains("comments")) {
                Iterator<Comment> commentIter = feature.getComments().iterator();
                if (commentIter.hasNext()) {
                    StringBuilder comments = new StringBuilder();
                    comments.append(commentIter.next().getComment());
                    while (commentIter.hasNext()) {
                        comments.append(",");
                        comments.append(commentIter.next().getComment());
                    }
                    if (!first) {
                        defline += ";";
                    }
                    else {
                        defline += " ";
                    }
                    first = false;
                    defline += "comments=" + comments.toString();
                }
            }
            if (metaDataToExport.contains("owner") && feature.getOwner() != null) {
                if (!first) {
                    defline += ";";
                }
                else {
                    defline += " ";
                }
                first = false;
                defline += "owner=" + feature.getOwner().getOwner();
            }
            if (metaDataToExport.contains("date_creation") && feature.getTimeAccessioned() != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(feature.getTimeAccessioned());
                if (!first) {
                    defline += ";";
                }
                else {
                    defline += " ";
                }
                first = false;
                defline += "date_last_modified=" + String.format("%d-%02d-%d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            }
            if (metaDataToExport.contains("date_last_modified") && feature.getTimeLastModified() != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(feature.getTimeLastModified());
                if (!first) {
                    defline += ";";
                }
                else {
                    defline += " ";
                }
                first = false;
                defline += "date_last_modified=" + String.format("%d-%02d-%d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            }
        }
        out.println(defline);
        String seq = feature.getResidues();
        for (int i = 0; i < seq.length(); i += numResiduesPerLine) {
            int endIdx = i + numResiduesPerLine;
            out.println(seq.substring(i, endIdx > seq.length() ? seq.length() : endIdx));
        }
    }
    
}
