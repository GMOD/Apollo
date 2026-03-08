package org.bbop.apollo.gwt.shared.track;


public enum TrackTypeEnum {

    BAM("bam","bam.bai"),
    BAM_CANVAS("bam","bam.bai"),
    BIGWIG_HEAT_MAP("bw"),
    BIGWIG_XY("bw"),
    VCF("vcf.gz","vcf.gz.tbi"),
    VCF_CANVAS("vcf.gz","vcf.gz.tbi"),
//    GFF3_JSON(new String[]{"gff","gff3","gff.gz","gff3.gz"}),
//    GFF3_JSON_CANVAS(new String[]{"gff","gff3","gff.gz","gff3.gz"}),
    GFF3_JSON(new String[]{"gff","gff3"}),
    GFF3_JSON_CANVAS(new String[]{"gff","gff3"}),
    GFF3(new String[]{"gff","gff3","gff.gz","gff3.gz"}),
    GFF3_CANVAS(new String[]{"gff","gff3","gff.gz","gff3.gz"}),
    GFF3_TABIX(new String[]{"gff.gz","gff3.gz"},new String[]{"gff.gz.tbi","gff3.gz.tbi"}),
    GFF3_TABIX_CANVAS(new String[]{"gff.gz","gff3.gz"},new String[]{"gff.gz.tbi","gff3.gz.tbi"});

    private String[] suffix ;
    private String[] suffixIndex ;

    TrackTypeEnum(String suffix){
        this(new String[]{suffix},null);
    }

    TrackTypeEnum(String suffix,String suffixIndex){
        this(new String[]{suffix},new String[]{suffixIndex});
    }

    TrackTypeEnum(String[] suffix){
        this.suffix = suffix;
        this.suffixIndex = null ;
    }


    TrackTypeEnum(String[] suffix,String[] suffixIndex){
        this.suffix = suffix;
        this.suffixIndex = suffixIndex;
    }

    public boolean isIndexed() {
        return this.suffixIndex!=null ;
    }

    public String[] getSuffix() {
        return suffix;
    }

    public String[] getSuffixIndex() {
        return suffixIndex;
    }

    @Override
    public String toString() {
        return name().replaceAll("_"," ");
    }

    public boolean hasSuffix(String input){
        for(String s : suffix){
            if(input.endsWith(s)) return true ;
        }
        return false ;
    }

    public boolean hasSuffixIndex(String input){
        for(String s : suffixIndex){
            if(input.endsWith(s)) return true ;
        }
        return false ;
    }

    public String getSuffixString() {
        String returnString = "";
        for(String s : suffix){
            returnString += "*."+s+" ";
        }
        return returnString;
    }

    public String getSuffixIndexString() {
        String returnString = "";
        for(String s : suffixIndex){
            returnString += "*."+s+" ";
        }
        return returnString;
    }
}
