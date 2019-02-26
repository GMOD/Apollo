package org.bbop.apollo.gwt.client.track;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.shared.track.TrackTypeEnum;

public class TrackConfigurationTemplate {

    String storeClass;
    String urlTemplate;
    String label;
    String type;
    String key;
    String category;
    TrackTypeEnum typeEnum;
    // key is entered


    public TrackConfigurationTemplate(String storeClass,
                                      String urlTemplate,
                                      String label,
                                      String type,
                                      String key,
                                      String category,
                                      TrackTypeEnum typeEnum
    ) {
        this.storeClass = storeClass;
        this.urlTemplate = urlTemplate;
        this.label = label;
        this.type = type;
        this.key = key;
        this.category = category ;
        this.typeEnum = typeEnum ;
    }


    JSONObject toJSON() {
        JSONObject returnObject = new JSONObject();
        returnObject.put("storeClass", new JSONString(this.storeClass));
        returnObject.put("urlTemplate", new JSONString(this.urlTemplate));
        returnObject.put("label", new JSONString(this.label));
        returnObject.put("type", new JSONString(this.type));
        returnObject.put("key", new JSONString(this.key));
        if(category!=null && category.trim().length()>0){
            returnObject.put("category", new JSONString(this.category));
        }
        JSONObject apolloDetails = new JSONObject();
        apolloDetails.put("source", new JSONString("upload"));
        apolloDetails.put("type", new JSONString(this.typeEnum.name()));
        returnObject.put("apollo", apolloDetails);
        return returnObject;
    }

    static String generateString() {
        String returnString = "";
        for (int i = 0; i < 10; i++) {
            returnString += String.valueOf(Math.round(Math.random() * 26));
        }

        return returnString;
    }


    public static JSONObject generateForTypeAndKeyAndCategory(TrackTypeEnum type, String key,String category) {
        String randomFileName = key!=null && key.trim().length()>1 ? key : generateString() ;
        switch (type) {
            case BAM:
                return new TrackConfigurationTemplate(
                        "JBrowse/Store/SeqFeature/BAM",
                        "raw/"+randomFileName.replaceAll(" ","_")+".bam",
                        randomFileName,
                        "JBrowse/View/Track/Alignments",
                        randomFileName,
                        category,
                        type
                ).toJSON();
            case BAM_CANVAS:
                return new TrackConfigurationTemplate(
                        "JBrowse/Store/SeqFeature/BAM",
                        "raw/"+randomFileName.replaceAll(" ","_")+".bam",
                        randomFileName,
                        "JBrowse/View/Track/Alignments2",
                        randomFileName,
                        category,
                        type
                ).toJSON();
            case BIGWIG_HEAT_MAP:
                return new TrackConfigurationTemplate(
                        "JBrowse/Store/BigWig",
                        "raw/"+randomFileName.replaceAll(" ","_")+".bw",
                        randomFileName,
                        "JBrowse/View/Track/Wiggle/Density",
                        randomFileName,
                        category,
                        type
                ).toJSON();
            case BIGWIG_XY:
                return new TrackConfigurationTemplate(
                        "JBrowse/Store/BigWig",
                        "raw/"+randomFileName.replaceAll(" ","_")+".bw",
                        randomFileName,
                        "JBrowse/View/Track/Wiggle/XYPlot",
                        randomFileName,
                        category,
                        type
                ).toJSON();
            case VCF:
                return new TrackConfigurationTemplate(
                        "JBrowse/Store/SeqFeature/VCFTabix",
                        "raw/"+randomFileName.replaceAll(" ","_")+".vcf.gz",
                        randomFileName,
                        "JBrowse/View/Track/HTMLVariants",
                        randomFileName,
                        category,
                        type
                ).toJSON();
            case VCF_CANVAS:
                return new TrackConfigurationTemplate(
                        "JBrowse/Store/SeqFeature/VCFTabix",
                        "raw/"+randomFileName.replaceAll(" ","_")+".vcf.gz",
                        randomFileName,
                        "JBrowse/View/Track/CanvasVariants",
                        randomFileName,
                        category,
                        type
                ).toJSON();
            case GFF3:
                return new TrackConfigurationTemplate(
                        "JBrowse/Store/SeqFeature/GFF3",
                        "raw/"+randomFileName.replaceAll(" ","_")+".gff",
                        randomFileName,
                        "JBrowse/View/Track/HTMLFeatures",
                        randomFileName,
                        category,
                        type
                ).toJSON();
            case GFF3_CANVAS:
                return new TrackConfigurationTemplate(
                        "JBrowse/Store/SeqFeature/GFF3",
                        "raw/"+randomFileName.replaceAll(" ","_")+".gff",
                        randomFileName,
                        "JBrowse/View/Track/CanvasFeatures",
                        randomFileName,
                        category,
                        type
                ).toJSON();
            case GFF3_TABIX:
                return new TrackConfigurationTemplate(
                        "JBrowse/Store/SeqFeature/GFF3Tabix",
                        "raw/"+randomFileName.replaceAll(" ","_")+".gff.gz",
                        randomFileName,
                        "JBrowse/View/Track/HTMLFeatures",
                        randomFileName,
                        category,
                        type
                ).toJSON();
            case GFF3_TABIX_CANVAS:
                return new TrackConfigurationTemplate(
                        "JBrowse/Store/SeqFeature/GFF3Tabix",
                        "raw/"+randomFileName.replaceAll(" ","_")+".gff.gz",
                        randomFileName,
                        "JBrowse/View/Track/CanvasFeatures",
                        randomFileName,
                        category,
                        type
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
