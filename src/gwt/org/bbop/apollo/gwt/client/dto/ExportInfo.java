package org.bbop.apollo.gwt.client.dto;

import java.util.List;

/**
 * Created by ndunn on 3/31/15.
 */
public class ExportInfo {

    private String type ;
    private List<SequenceInfo> sequenceInfoList ;
    private String generatedUrl ;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<SequenceInfo> getSequenceInfoList() {
        return sequenceInfoList;
    }

    public void setSequenceInfoList(List<SequenceInfo> sequenceInfoList) {
        this.sequenceInfoList = sequenceInfoList;
    }

    public String getGeneratedUrl() {
        return generatedUrl;
    }

    public void setGeneratedUrl(String generatedUrl) {
        this.generatedUrl = generatedUrl;
    }
}
