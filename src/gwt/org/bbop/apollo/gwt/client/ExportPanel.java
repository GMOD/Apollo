package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.rest.SequenceRestService;

import java.util.List;

/**
 * Created by ndunn on 1/27/15.
 */
public class ExportPanel extends DialogBox{
    private String type;
    private String url;
    private OrganismInfo organismInfo ;
    private List<SequenceInfo> sequenceList ;


    interface ExportPanelUiBinder extends UiBinder<Widget, ExportPanel> {
    }

    private static ExportPanelUiBinder ourUiBinder = GWT.create(ExportPanelUiBinder.class);
    @UiField
    HTML organismLabel;
    @UiField
    HTML sequenceInfoLabel;
    @UiField
    HTML typeLabel;
    @UiField
    HTML urlLink;
    @UiField
    Button closeButton;

    public ExportPanel() {
        setWidget(ourUiBinder.createAndBindUi(this));
        setAutoHideEnabled(true);
        setText("Export");
        setGlassEnabled(true);
        center();

    }

    public void setOrganismInfo(OrganismInfo organismInfo) {
        this.organismInfo = organismInfo;
        organismLabel.setHTML(organismInfo.getName());
    }

    public void setSequenceList(List<SequenceInfo> sequenceList) {
        this.sequenceList = sequenceList;
        sequenceInfoLabel.setHTML(this.sequenceList.size() + " exported ");
    }

    public void setType(String type) {
        this.type = type;
        typeLabel.setHTML("Type: "+this.type);
    }

    public void setUrl(String url) {
        this.url = url;
        urlLink.setHTML("<a href=" + url + ">Download Annotations (GFF3)</a>");
    }
    
    public String getType() {
        return type;
    }

    @UiHandler("closeButton")
    public void closeExportPanel(ClickEvent clickEvent){
        hide();
    }


    public void generateLink() {
        SequenceRestService.generateLink(this);
    }

    public List<SequenceInfo> getSequenceList() {
        return sequenceList;
    }
}