package org.bbop.apollo.gwt.client;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;
import org.bbop.apollo.gwt.client.rest.SequenceRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.constants.*;

import java.util.List;

/**
 * Created by Nathan Dunn on 1/27/15.
 */
public class ExportPanel extends Modal {
    private String type;
    private List<SequenceInfo> sequenceList;
    private Boolean exportAll = false;


    HTML sequenceInfoLabel = new HTML();
    HTML typeLabel = new HTML();
    HTML sequenceTypeLabel = new HTML();
    Button closeButton = new Button("Cancel");
    Button exportButton = new Button("Export");
    RadioButton gff3Button = new RadioButton("GFF3", "GFF3", true);
    RadioButton gff3WithFastaButton = new RadioButton("GFF3 with FASTA", "GFF3 with FASTA", true);
    RadioButton genomicRadioButton = new RadioButton("Genomic", "Genomic", true);
    RadioButton cdnaRadioButton = new RadioButton("cDNA", "cDNA", true);
    RadioButton cdsRadioButton = new RadioButton("CDS", "CDS", true);
    RadioButton peptideRadioButton = new RadioButton("Peptide", "Peptide", true);

    ModalBody modalBody = new ModalBody();
    ModalHeader modalHeader = new ModalHeader();
    ModalFooter modalFooter = new ModalFooter();


    public ExportPanel(OrganismInfo organismInfo, String type, Boolean exportAll, List<SequenceInfo> sequenceInfoList) {
        setTitle("Export");
        setClosable(true);
        setRemoveOnHide(true);
        setDataBackdrop(ModalBackdrop.FALSE);

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
        exportButton.setEnabled(false);
        modalFooter.add(exportButton);
        modalFooter.add(closeButton);
        add(modalFooter);

        setType(type);
        setExportAll(exportAll);
        setSequenceList(sequenceInfoList);

        setUiHandlers();
    }

    private class ExportClickHandler implements ClickHandler{
        @Override
        public void onClick(ClickEvent event) {
            exportButton.setEnabled(true);
        }
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

        ExportClickHandler exportClickHandler = new ExportClickHandler();

        genomicRadioButton.addClickHandler(exportClickHandler);
        cdnaRadioButton.addClickHandler(exportClickHandler);
        cdsRadioButton.addClickHandler(exportClickHandler);
        peptideRadioButton.addClickHandler(exportClickHandler);

        gff3WithFastaButton.addClickHandler(exportClickHandler);
        gff3Button.addClickHandler(exportClickHandler);
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
        // this is the default . . . may handle to GFF3 with FASTA
        else{
            return FeatureStringEnum.TYPE_GENOMIC.getValue();
        }
    }

    public Boolean getExportGff3Fasta() {
        return gff3WithFastaButton.isActive();
    }

    public void doExport() {
        exportButton.setEnabled(false);
        exportButton.setIcon(IconType.REFRESH);
        exportButton.setIconSpin(true);
        generateLink();
    }

    public void generateLink() {
        SequenceRestService.generateLink(this);
    }


    public List<SequenceInfo> getSequenceList() {
        return sequenceList;
    }
}