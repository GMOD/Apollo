package org.bbop.apollo.gwt.shared.track;


public enum SequenceTypeEnum {

    FA("fa"),
    FNA("fna"),
    FA_GZ("fa.gz","gz"),
    FNA_GZ("fna.gz","gz"),
    FA_ZIP("fa.zip","zip"),
    FNA_ZIP("fna.zip","zip"),
    FA_TGZ("fa.tgz","tar.gz"),
    FNA_TGZ("fna.tgz","tar.gz"),
    FA_TAR_GZ("fa.tar.gz","tar.gz"),
    FNA_TAR_GZ("fna.tar.gz","tar.gz");

    private String suffix ;
    private String compression;

    SequenceTypeEnum(String suffix){
        this(suffix,null);
    }

    SequenceTypeEnum(String suffix, String compression){
        this.suffix = suffix;
        this.compression = compression ;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getCompression() {
        return compression;
    }

    public static SequenceTypeEnum getSequenceTypeForFile(String fileName){
        for(SequenceTypeEnum sequenceTypeEnum : values()){
            if(fileName.endsWith(sequenceTypeEnum.suffix)){
                return sequenceTypeEnum ;
            }
        }
        return null ;
    }

    public static String generateSuffixDescription(){
        String returnString = "";
        for(SequenceTypeEnum s: values()){
            returnString += s.suffix + " " ;
        }
        returnString = returnString.trim();
        returnString = returnString.replaceAll(" ",", ");
        return returnString;
    }

    @Override
    public String toString() {
        return "SequenceTypeEnum{" +
                "suffix='" + suffix + '\'' +
                ", compression='" + compression + '\'' +
                '}';
    }
}
