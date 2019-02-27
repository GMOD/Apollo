package org.bbop.apollo.gwt.shared.track;

public enum TrackTypeEnum {
    BAM("bam","bam.bai"),
    BAM_CANVAS("bam","bam.bai"),
    BIGWIG_HEAT_MAP("bw"),
    BIGWIG_XY("bw"),
    VCF("vcf.gz","vcf.gz.tbi"),
    VCF_CANVAS("vcf.gz","vcf.gz.tbi"),
    GFF3("gff"),
    GFF3_CANVAS("gff"),
    GFF3_TABIX("gff.gz","gff.gz.tbi"),
    GFF3_TABIX_CANVAS("gff.gz","gff.gz.tbi");

    private String suffix ;
    private String suffixIndex ;

    TrackTypeEnum(String suffix){
        this.suffix = suffix;
        this.suffixIndex = null ;
    }

    TrackTypeEnum(String suffix,String suffixIndex){
        this.suffix = suffix;
        this.suffixIndex = suffixIndex;
    }

    public boolean isIndexed() {
        return this.suffixIndex!=null ;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getSuffixIndex() {
        return suffixIndex;
    }

    @Override
    public String toString() {
        return name().replaceAll("_"," ");
    }
}
