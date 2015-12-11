package org.bbop.apollo.gwt.client;

//import com.google.gwt.core.client.GWT;
//import com.google.gwt.dom.client.Document;

import com.google.gwt.event.dom.client.ClickEvent;
//import com.google.gwt.uibinder.client.UiBinder;
//import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.rest.SequenceRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.constants.Toggle;

import java.util.List;

/**
 * Created by ndunn on 1/27/15.
 */
public class ExportPanel extends Modal {
    private String type;
    private String url;
    private OrganismInfo organismInfo;
    private List<SequenceInfo> sequenceList;
    private String sequenceType = "genomic";
    private Boolean exportGff3Fasta = false;
    private Boolean exportAll = false;


//    interface ExportPanelUiBinder extends UiBinder<Widget, ExportPanel> {
//    }

    //    private static ExportPanelUiBinder ourUiBinder = GWT.create(ExportPanelUiBinder.class);
//    @UiField
    HTML organismLabel = new HTML();
    //    @UiField
    HTML sequenceInfoLabel = new HTML();
    //    @UiField
    HTML typeLabel = new HTML();
    //    @UiField
//    HTML urlLink;
//    @UiField
    HTML sequenceTypeLabel = new HTML();
    //    @UiField
    Button closeButton = new Button("Cancel");
    //    @UiField
    Button exportButton = new Button("Export");
    //    @UiField
    RadioButton gff3Button = new RadioButton("GFF3", "GFF3", true);
    //    @UiField
    RadioButton gff3WithFastaButton = new RadioButton("GFF3 with FASTA", "GFF3 with FASTA", true);
    //    @UiField
    RadioButton genomicRadioButton = new RadioButton("Genomic", "Genomic", true);
    //    @UiField
    RadioButton cdnaRadioButton = new RadioButton("cDNA", "cDNA", true);
    //    @UiField
    RadioButton cdsRadioButton = new RadioButton("CDS", "CDS", true);
    //    @UiField
    RadioButton peptideRadioButton = new RadioButton("Peptide", "Peptide", true);
    //    @UiField
//    HTML exportHeader =new ;

    ModalBody modalBody = new ModalBody();
    ModalHeader modalHeader = new ModalHeader();
    ModalFooter modalFooter = new ModalFooter();

//    public ExportPanel() {
////        show();
////        ourUiBinder.createAndBindUi(this);
////        exportHeader.setHTML("asdfdasf");
////        setAutoHideEnabled(true);
////        setText("Export");
////        setGlassEnabled(true);
////        center();
//
//    }

    public ExportPanel(OrganismInfo organismInfo, String type, Boolean exportAll, List<SequenceInfo> sequenceInfoList) {
        setTitle("Export");
        setClosable(true);
        setRemoveOnHide(true);


        Integer count = exportAll ? -1 : sequenceInfoList.size();
        String countText = count < 0 ? "all" : count + "";

        modalHeader.add(new HTML("Export " + countText + " sequences from " + organismInfo.getName() + " as " + type));


        add(modalHeader);


        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.setDataToggle(Toggle.BUTTONS);
        if (type.equals("FASTA")) {
            buttonGroup.add(genomicRadioButton);
            buttonGroup.add(cdnaRadioButton);
            buttonGroup.add(cdsRadioButton);
            buttonGroup.add(peptideRadioButton);
        } else if (type.equals("GFF3")) {
            buttonGroup.add(gff3Button);
            buttonGroup.add(gff3WithFastaButton);
        }
        modalBody.add(buttonGroup);

        modalBody.add(sequenceTypeLabel);


        add(modalBody);

        exportButton.setIcon(IconType.DOWNLOAD);
        exportButton.setType(ButtonType.PRIMARY);
        modalFooter.add(exportButton);
        modalFooter.add(closeButton);
        add(modalFooter);

        setOrganismInfo(organismInfo);
        setType(type);
        setExportAll(exportAll);
        setSequenceList(sequenceInfoList);

        setUiHandlers();
    }

    @Override
    public void show() {
        super.show();
    }

    private void setUiHandlers() {
        closeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                hide();
            }
        });

        exportButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                doExport();
            }
        });

    }


    public void setOrganismInfo(OrganismInfo organismInfo) {
        this.organismInfo = organismInfo;
        organismLabel.setHTML(organismInfo.getName());
    }

    public void setSequenceList(List<SequenceInfo> sequenceList) {
        this.sequenceList = sequenceList;
        if (exportAll) {
            sequenceInfoLabel.setHTML("All exported ");
        } else {
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

    public String getSequenceType() {
        if(genomicRadioButton.isActive()){
            return FeatureStringEnum.TYPE_GENOMIC.getValue();
        }
        else
        if(cdnaRadioButton.isActive()){
            return FeatureStringEnum.TYPE_CDNA.getValue();
        }
        else
        if(cdsRadioButton.isActive()){
            return FeatureStringEnum.TYPE_CDS.getValue();
        }
        else
        if(peptideRadioButton.isActive()){
            return FeatureStringEnum.TYPE_PEPTIDE.getValue();
        }
        return null ;
    }

    public Boolean getExportGff3Fasta() {
        return gff3WithFastaButton.isActive();
    }

//    @UiHandler("closeButton")
//    public void closeExportPanel(ClickEvent clickEvent) {
//        hide();
//    }

//    public void enableCloseButton() {
//        closeButton.setEnabled(true);
//    }

    //    @UiHandler("exportButton")
    public void doExport() {
        exportButton.setEnabled(false);
        generateLink();
    }

    public void generateLink() {
        SequenceRestService.generateLink(this);
    }

//    public void showSequenceTypeLabel() {
//        sequenceTypeLabel.setHTML("Sequence Type: " + this.sequenceType);
//        sequenceTypeLabel.setVisible(true);
//    }
//
//    public void renderFastaSelection() {
//        genomicRadioButton.setVisible(true);
//        cdnaRadioButton.setVisible(true);
//        cdsRadioButton.setVisible(true);
//        peptideRadioButton.setVisible(true);
//
//        // hide these
//        gff3Button.setVisible(false);
//        gff3WithFastaButton.setVisible(false);
//    }
//
//    public void renderGff3Selection() {
//        genomicRadioButton.setVisible(false);
//        cdnaRadioButton.setVisible(false);
//        cdsRadioButton.setVisible(false);
//        peptideRadioButton.setVisible(false);
//
//        // show these
//        gff3Button.setVisible(true);
//        gff3WithFastaButton.setVisible(true);
//    }

//    @UiHandler("genomicRadioButton")
//    public void selectGenomic(ClickEvent clickEvent) {
//        sequenceType = FeatureStringEnum.TYPE_GENOMIC.getValue();
//    }
//
//    @UiHandler("cdnaRadioButton")
//    public void selectCDNA(ClickEvent clickEvent) {
//        sequenceType = FeatureStringEnum.TYPE_CDNA.getValue();
//    }
//
//    @UiHandler("cdsRadioButton")
//    public void selectCDS(ClickEvent clickEvent) {
//        sequenceType = FeatureStringEnum.TYPE_CDS.getValue();
//    }
//
//    @UiHandler("peptideRadioButton")
//    public void selectPeptide(ClickEvent clickEvent) {
//        sequenceType = FeatureStringEnum.TYPE_PEPTIDE.getValue();
//    }

//    @UiHandler("gff3Button")
//    public void selectOnlyGff3(ClickEvent clickEvent) {
//        exportGff3Fasta = false;
//    }
//
//    @UiHandler("gff3WithFastaButton")
//    public void selectGff3WithFasta(ClickEvent clickEvent) {
//        exportGff3Fasta = true;
//    }

    public List<SequenceInfo> getSequenceList() {
        return sequenceList;
    }
}