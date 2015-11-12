package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.rest.SequenceRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

import java.util.List;

/**
 * Created by Nathan Dunn on 1/27/15.
 */
public class ExportPanel extends DialogBox{
    private String type;
    private String url;
    private OrganismInfo organismInfo ;
    private List<SequenceInfo> sequenceList ;
    private String sequenceType = "genomic";
    private Boolean exportGff3Fasta = false;
    private Boolean exportAll = false;


    interface ExportPanelUiBinder extends UiBinder<Widget, ExportPanel> {
    }

    private static ExportPanelUiBinder ourUiBinder = GWT.create(ExportPanelUiBinder.class);
    @UiField
    HTML organismLabel;
    @UiField
    HTML sequenceInfoLabel;
    @UiField
    HTML typeLabel;
//    @UiField
//    HTML urlLink;
    @UiField
    HTML sequenceTypeLabel;
    @UiField
    Button closeButton;
    @UiField
    Button exportButton;
    @UiField
    RadioButton gff3Button;
    @UiField
    RadioButton gff3WithFastaButton;
    @UiField
    RadioButton genomicRadioButton;
    @UiField
    RadioButton cdnaRadioButton;
    @UiField
    RadioButton cdsRadioButton;
    @UiField
    RadioButton peptideRadioButton;

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
        if(exportAll){
            sequenceInfoLabel.setHTML("All exported ");
        }
        else{
            sequenceInfoLabel.setHTML(this.sequenceList.size() + " exported ");
        }
    }
    
    public void setExportAll(Boolean exportAll) {
        this.exportAll = exportAll;
        this.sequenceInfoLabel.setHTML("All exported ");
    }

    public Boolean getExportAll() {
        return exportAll;
    }
    
    public void setType(String type) {
        this.type = type;
        typeLabel.setHTML("Type: " + this.type);
    }

    public void setExportUrl(String exportUrlString) {
        Window.Location.assign(exportUrlString);
        this.closeButton.click();
    }

    public String getType() {
        return type;
    }
    
    public String getSequenceType() { return sequenceType; }
    
    public Boolean getExportGff3Fasta() { return exportGff3Fasta; }
    
    @UiHandler("closeButton")
    public void closeExportPanel(ClickEvent clickEvent){
        hide();
    }
    
    public void enableCloseButton(){
        closeButton.setEnabled(true);
    }

    @UiHandler("exportButton")
    public void doExport(ClickEvent clickEvent) {
        if(type.equals("FASTA")) {
            genomicRadioButton.setVisible(false);
            cdnaRadioButton.setVisible(false);
            cdsRadioButton.setVisible(false);
            peptideRadioButton.setVisible(false);
            showSequenceTypeLabel();
        } else if (type.equals("GFF3")) {
            gff3Button.setVisible(false);
            gff3WithFastaButton.setVisible(false);
        }
        exportButton.setEnabled(false);
        generateLink();
    }
    public void generateLink() {
        SequenceRestService.generateLink(this);
    }

    public void showSequenceTypeLabel() {
        sequenceTypeLabel.setHTML("Sequence Type: " + this.sequenceType);
        sequenceTypeLabel.setVisible(true);
    }
    
    public void renderFastaSelection() {
        genomicRadioButton.setVisible(true);
        cdnaRadioButton.setVisible(true);
        cdsRadioButton.setVisible(true);
        peptideRadioButton.setVisible(true);
    }
    
    public void renderGff3Selection() {
        gff3Button.setVisible(true);
        gff3WithFastaButton.setVisible(true);
    }
    
    @UiHandler("genomicRadioButton")
    public void selectGenomic(ClickEvent clickEvent) {
        sequenceType = FeatureStringEnum.TYPE_GENOMIC.getValue();
    }

    @UiHandler("cdnaRadioButton")
    public void selectCDNA(ClickEvent clickEvent) {
        sequenceType = FeatureStringEnum.TYPE_CDNA.getValue();
    }

    @UiHandler("cdsRadioButton")
    public void selectCDS(ClickEvent clickEvent) {
        sequenceType = FeatureStringEnum.TYPE_CDS.getValue();
    }

    @UiHandler("peptideRadioButton")
    public void selectPeptide(ClickEvent clickEvent) {
        sequenceType = FeatureStringEnum.TYPE_PEPTIDE.getValue();
    }
    
    @UiHandler("gff3Button")
    public void selectOnlyGff3(ClickEvent clickEvent) { exportGff3Fasta = false; }

    @UiHandler("gff3WithFastaButton")
    public void selectGff3WithFasta(ClickEvent clickEvent) { exportGff3Fasta = true; }

    public List<SequenceInfo> getSequenceList() {
        return sequenceList;
    }
}