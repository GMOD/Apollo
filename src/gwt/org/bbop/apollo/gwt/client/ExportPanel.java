package org.bbop.apollo.gwt.client;


import com.google.gwt.core.client.GWT;
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
import org.gwtbootstrap3.client.ui.html.Div;
import org.gwtbootstrap3.client.ui.html.Paragraph;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;

import java.util.List;

/**
 * Created by ndunn on 1/27/15.
 */
public class ExportPanel extends Modal {
    private String type;
    private List<SequenceInfo> sequenceList;
    private Boolean exportAll = false;
    private OrganismInfo currentOrganismInfo;
    private Boolean exportAllSequencesToChado = false;
    HTML sequenceInfoLabel = new HTML();
    HTML typeLabel = new HTML();
    HTML sequenceTypeLabel = new HTML();
    Button closeButton = new Button("Cancel");
    Button exportButton = new Button("Export");
    RadioButton gff3Button = new RadioButton("GFF3", "GFF3", true);
    RadioButton gff3WithFastaButton = new RadioButton("GFF3 with FASTA", "GFF3 with FASTA", true);
    RadioButton vcfButton = new RadioButton("VCF", "VCF", true);
    RadioButton genomicRadioButton = new RadioButton("Genomic", "Genomic", true);
    RadioButton cdnaRadioButton = new RadioButton("cDNA", "cDNA", true);
    RadioButton cdsRadioButton = new RadioButton("CDS", "CDS", true);
    RadioButton peptideRadioButton = new RadioButton("Peptide", "Peptide", true);
    RadioButton chadoExportButton1 = new RadioButton("chadoExportOption1", "Export all sequences (that have annotations) to Chado", true);
    RadioButton chadoExportButton2 = new RadioButton("chadoExportOption2", "Export all sequences to Chado", true);

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
        currentOrganismInfo = organismInfo;

        modalHeader.add(new HTML("Export " + countText + " sequence(s) from " + organismInfo.getName() + " as " + type));


        add(modalHeader);


        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.setDataToggle(Toggle.BUTTONS);
        if (type.equals(FeatureStringEnum.TYPE_FASTA.getValue())) {
            buttonGroup.add(genomicRadioButton);
            buttonGroup.add(cdnaRadioButton);
            buttonGroup.add(cdsRadioButton);
            buttonGroup.add(peptideRadioButton);
        }
        else
        if (type.equals(FeatureStringEnum.TYPE_GFF3.getValue())) {
            buttonGroup.add(gff3Button);
            buttonGroup.add(gff3WithFastaButton);
        }
        else
        if (type.equals(FeatureStringEnum.TYPE_VCF.getValue())) {
            buttonGroup.add(vcfButton);
        }
        else
        if (type.equals(FeatureStringEnum.TYPE_CHADO.getValue())) {
            buttonGroup.add(chadoExportButton1);
            buttonGroup.add(chadoExportButton2);
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

        vcfButton.addClickHandler(exportClickHandler);

        chadoExportButton1.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                setExportAllSequencesToChado(false);
                exportButton.setEnabled(true);
            }
        });

        chadoExportButton2.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                setExportAllSequencesToChado(true);
                exportButton.setEnabled(true);
            }
        });
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

    public void showExportStatus(String exportStatus) {
        this.chadoExportButton1.setVisible(false);
        this.chadoExportButton2.setVisible(false);
        this.exportButton.setVisible(false);
        this.closeButton.setText("OK");
        this.closeButton.setWidth("45px");
        Div status = new Div();

        exportButton.setIconSpin(false);
        if (exportStatus.contains("error")) {
            Span span = new Span();
            span.add(new Label(LabelType.DANGER, "Error"));
            status.add(span);
        }
        else {
            Span span = new Span();
            span.add(new Label(LabelType.SUCCESS, "Success"));
            status.add(span);
        }
        Div div = parseStatus(exportStatus);
        status.add(div);
        this.modalBody.add(status);
    }

    public Div parseStatus(String status) {
        Div div = new Div();
        JSONObject jsonObject = JSONParser.parseStrict(status).isObject();
        GWT.log(jsonObject.toString());
        for(String key : jsonObject.keySet()) {
            div.add(new Paragraph(key + ": " + jsonObject.get(key).toString().replaceAll("\"", "")));
        }
        return div;
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
        else
        if(chadoExportButton1.isActive()){
            return FeatureStringEnum.TYPE_CHADO.getValue();
        }
        else
        if(chadoExportButton2.isActive()) {
            return FeatureStringEnum.TYPE_CHADO.getValue();
        }
        // this is the default . . . may handle to GFF3 with FASTA
        else{
            return FeatureStringEnum.TYPE_GENOMIC.getValue();
        }
    }

    public String getChadoExportType() {
        String exportType = null;
        if (type.equals(FeatureStringEnum.TYPE_CHADO.getValue())) {
            if (chadoExportButton1.isActive()) {
                exportType = FeatureStringEnum.EXPORT_CHADO_CLEAN.getValue();
            }
            else if (chadoExportButton2.isActive()) {
                exportType = FeatureStringEnum.EXPORT_CHADO_UPDATE.getValue();
            }
        }
        return exportType;
    }

    public String getOrganismName() {
        return this.currentOrganismInfo.getName();
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

    public Boolean getExportAllSequencesToChado() {
        return this.exportAllSequencesToChado;
    }

    public void setExportAllSequencesToChado(Boolean value) {
        this.exportAllSequencesToChado = value;
    }
}