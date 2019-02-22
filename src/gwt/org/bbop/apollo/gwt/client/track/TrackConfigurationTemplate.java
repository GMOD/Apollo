package org.bbop.apollo.gwt.client.track;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

public class TrackConfigurationTemplate {

    String storeClass;
    String urlTemplate;
    String label;
    String type;
    String key;
    // key is entered

    public enum TrackType {
        BAM,
        BAM_CANVAS,
        BIGWIG_HEAT_MAP,
        BIGWIG_XY,
        VCF,
        VCF_CANVAS,
        GFF3,
        GFF3_CANVAS,
    }

    public TrackConfigurationTemplate() {
    }

    public TrackConfigurationTemplate(String storeClass,
                                      String urlTemplate,
                                      String label,
                                      String type,
                                      String key) {
        this.storeClass = storeClass;
        this.urlTemplate = urlTemplate;
        this.label = label;
        this.type = type;
        this.key = key;
    }


    public static TrackConfigurationTemplate generateForType(String type) {
        TrackConfigurationTemplate trackConfigurationTemplate = new TrackConfigurationTemplate();


        return trackConfigurationTemplate;
    }

    JSONObject toJSON() {
        JSONObject returnObject = new JSONObject();
        returnObject.put("storeClass", new JSONString(this.storeClass));
        returnObject.put("urlTemplate", new JSONString(this.urlTemplate));
        returnObject.put("label", new JSONString(this.label));
        returnObject.put("type", new JSONString(this.type));
        returnObject.put("key", new JSONString(this.key));
        return returnObject;
    }

    static String generateString() {
        String returnString = "";
        for (int i = 0; i < 10; i++) {
            returnString += String.valueOf(Math.round(Math.random() * 26));
        }

        return returnString;
    }

    public static JSONObject generateForType(TrackType type) {
        return generateForTypeAndKey(type, generateString());
    }

    public static JSONObject generateForTypeAndKey(TrackType type, String key) {
        String randomFileName = key!=null && key.trim().length()>1 ? key : generateString() ;
        switch (type) {
            case BAM:
                return new TrackConfigurationTemplate(
                        "JBrowse/Store/SeqFeature/BAM",
                        "raw/"+randomFileName+".bam",
                        randomFileName,
                        "JBrowse/View/Track/Alignments",
                        randomFileName
                ).toJSON();
            case BAM_CANVAS:
                return new TrackConfigurationTemplate(
                        "JBrowse/Store/SeqFeature/BAM",
                        "raw/"+randomFileName+".bam",
                        randomFileName,
                        "JBrowse/View/Track/Alignments2",
                        randomFileName
                ).toJSON();
            case BIGWIG_HEAT_MAP:
                return new TrackConfigurationTemplate(
                        "JBrowse/Store/BigWig",
                        "raw/"+randomFileName+".bw",
                        randomFileName,
                        "JBrowse/View/Track/Wiggle/Density",
                        randomFileName
                ).toJSON();
            case BIGWIG_XY:
                return new TrackConfigurationTemplate(
                        "JBrowse/Store/BigWig",
                        "raw/"+randomFileName+".bw",
                        randomFileName,
                        "JBrowse/View/Track/Wiggle/XYPlot",
                        randomFileName
                ).toJSON();
            case VCF:
                return new TrackConfigurationTemplate(
                        " JBrowse/Store/SeqFeature/VCFTabix",
                        "raw/"+randomFileName+".vcf.gz",
                        randomFileName,
                        "JBrowse/View/Track/HTMLVariants",
                        randomFileName
                ).toJSON();
            case VCF_CANVAS:
                return new TrackConfigurationTemplate(
                        " JBrowse/Store/SeqFeature/VCFTabix",
                        "raw/"+randomFileName+".vcf.gz",
                        randomFileName,
                        "JBrowse/View/Track/CanvasVariants",
                        randomFileName
                ).toJSON();
            case GFF3:
                return new TrackConfigurationTemplate(
                        " JBrowse/Store/SeqFeature/GFF3Tabix",
                        "raw/"+randomFileName+".gff.gz",
                        randomFileName,
                        "JBrowse/View/Track/HTMLFeatures",
                        randomFileName
                ).toJSON();
            case GFF3_CANVAS:
                return new TrackConfigurationTemplate(
                        " JBrowse/Store/SeqFeature/GFF3Tabix",
                        "raw/"+randomFileName+".gff.gz",
                        randomFileName,
                        "JBrowse/View/Track/CanvasFeatures",
                        randomFileName
                ).toJSON();
        }

        return null;
    }

    public final static String bamDefault = "" +
            "{\n" +
            "  \"key\":\"<change me>\",\n" +
            " \"storeClass\" : \"JBrowse/Store/SeqFeature/BAM\",\n" +
            "\"urlTemplate\" : \"raw/<autogenerated unique>.bam\",\n" +
            " \"label\" : \"<autogenerated unique>\",\n" +
            " \"type\" : \"JBrowse/View/Track/Alignments2\"\n" +
            "}\n" +
            "\n";
    public final static String bamCanvasDefault = "" +
            "{\n" +
            "  \"key\":\"<change me>\",\n" +
            " \"storeClass\" : \"JBrowse/Store/SeqFeature/BAM\",\n" +
            "\"urlTemplate\" : \"raw/<autogenerated unique>.bam\",\n" +
            " \"label\" : \"<autogenerated unique>\",\n" +
            " \"type\" : \"JBrowse/View/Track/Alignments2\"\n" +
            "}\n" +
            "\n";
    public final static String bigWig = "";
    public final static String bigWigXY = "";
    public final static String vcf = "";
    public final static String vcfCanvas = "";
    public final static String gff3Default = "";
    public final static String gff3CanvasDefault = "";

}
