package org.bbop.apollo.gwt.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.rest.GeneProductRestService;
import org.bbop.apollo.gwt.client.rest.GoRestService;
import org.bbop.apollo.gwt.client.rest.ProvenanceRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.constants.ModalBackdrop;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.bootbox.client.callback.ConfirmCallback;


/**
 * Created by ndunn on 4/30/15.
 */
public class UploadDialog extends Modal {

    final TextArea textArea = new TextArea();
    final String EXAMPLE_ANNOTATION = "\n" +
      "{\n" +
      " \"go_annotations\":[{\"reference\":\"PMID:Example\",\"geneRelationship\":\"RO:0002331\",\"goTerm\":\"GO:1901560\",\"notes\":\"[\\\"ExampleNote2\\\",\\\"ExampleNote1\\\"]\",\"evidenceCodeLabel\":\"HDA (ECO:0007005): inferred from high throughput direct assay\",\"negate\":false,\"aspect\":\"BP\",\"goTermLabel\":\"response to purvalanol A (GO:1901560) \",\"evidenceCode\":\"ECO:0007005\",\"id\":1,\"withOrFrom\":\"[\\\"Uniprot:Example2\\\",\\\"UniProt:Example1\\\"]\"},{\"reference\":\"PMID:Example\",\"geneRelationship\":\"RO:0002327\",\"goTerm\":\"GO:0051018\",\"notes\":\"[\\\"ExampleNote\\\"]\",\"evidenceCodeLabel\":\"TAS (ECO:0000304): traceable author statement\",\"negate\":false,\"aspect\":\"MF\",\"goTermLabel\":\"protein kinase A binding (GO:0051018) \",\"evidenceCode\":\"ECO:0000304\",\"id\":2,\"withOrFrom\":\"[\\\"Uniprot:Example3\\\"]\"}],    \n" +
      " \"gene_product\": [{\"reference\":\"PMID:21873635\",\"notes\":\"[\\\"Sample\\\"]\",\"evidenceCodeLabel\":\"IBA (ECO:0000318): inferred from biological aspect of ancestor\",\"alternate\":true,\"evidenceCode\":\"ECO:0000318\",\"id\":1,\"productName\":\"AQP1\",\"withOrFrom\":\"[\\\"UniProtKB:P29972\\\",\\\"RGD:2141\\\"]\"},{\"reference\":\"PMID:21873635\",\"notes\":\"[]\",\"evidenceCodeLabel\":\"IBA (ECO:0000318): inferred from biological aspect of ancestor\",\"alternate\":false,\"evidenceCode\":\"ECO:0000318\",\"id\":2,\"productName\":\"FAM20A\",\"withOrFrom\":\"[\\\"PANTHER:PTN000966558\\\"]\"}],\n" +
      " \"provenance\": [{\"reference\":\"PMID:21873635\",\"notes\":\"[]\",\"field\":\"TYPE\",\"evidenceCodeLabel\":\"HDA (ECO:0007005): inferred from high throughput direct assay\",\"evidenceCode\":\"ECO:0007005\",\"id\":1,\"withOrFrom\":\"[\\\"UniProtKB:P29972\\\",\\\"RGD:2141\\\"]\"},{\"reference\":\"PMID:79972\",\"notes\":\"[\\\"test\\\",\\\"note\\\"]\",\"field\":\"SYNONYM\",\"evidenceCodeLabel\":\"IEP (ECO:0000270): inferred from expression pattern\",\"evidenceCode\":\"ECO:0000270\",\"id\":2,\"withOrFrom\":\"[\\\"TEST2:DEF123\\\",\\\"TEST:ABC123\\\"]\"}]\n" +
      "}" ;
    final String EXAMPLE_ANNOTATION_EMPTY_REF = "\n" +
      "{\n" +
      " \"go_annotations\":[{\"geneRelationship\":\"RO:0002331\",\"goTerm\":\"GO:1901560\",\"notes\":\"[\\\"ExampleNote2\\\",\\\"ExampleNote1\\\"]\",\"evidenceCodeLabel\":\"HDA (ECO:0007005): inferred from high throughput direct assay\",\"negate\":false,\"aspect\":\"BP\",\"goTermLabel\":\"response to purvalanol A (GO:1901560) \",\"evidenceCode\":\"ECO:0007005\",\"id\":1},{\"reference\":\"PMID:Example\",\"geneRelationship\":\"RO:0002327\",\"goTerm\":\"GO:0051018\",\"notes\":\"[\\\"ExampleNote\\\"]\",\"evidenceCodeLabel\":\"TAS (ECO:0000304): traceable author statement\",\"negate\":false,\"aspect\":\"MF\",\"goTermLabel\":\"protein kinase A binding (GO:0051018) \",\"evidenceCode\":\"ECO:0000304\",\"id\":2}],    \n" +
      " \"gene_product\": [{\"notes\":\"[\\\"Sample\\\"]\",\"evidenceCodeLabel\":\"IBA (ECO:0000318): inferred from biological aspect of ancestor\",\"alternate\":true,\"evidenceCode\":\"ECO:0000318\",\"id\":1,\"productName\":\"AQP1\"},{\"reference\":\"PMID:21873635\",\"notes\":\"[]\",\"evidenceCodeLabel\":\"IBA (ECO:0000318): inferred from biological aspect of ancestor\",\"alternate\":false,\"evidenceCode\":\"ECO:0000318\",\"id\":2,\"productName\":\"FAM20A\"}],\n" +
      " \"provenance\": [{\"notes\":\"[]\",\"field\":\"TYPE\",\"evidenceCodeLabel\":\"HDA (ECO:0007005): inferred from high throughput direct assay\",\"evidenceCode\":\"ECO:0007005\",\"id\":1},{\"reference\":\"PMID:79972\",\"notes\":\"[\\\"test\\\",\\\"note\\\"]\",\"field\":\"SYNONYM\",\"evidenceCodeLabel\":\"IEP (ECO:0000270): inferred from expression pattern\",\"evidenceCode\":\"ECO:0000270\",\"id\":2}]\n" +
      "}" ;

    public UploadDialog(final AnnotationInfo annotationInfo) {
        setSize(ModalSize.LARGE);
        setHeight("500px");
        setClosable(true);
        setFade(true);
        setDataBackdrop(ModalBackdrop.STATIC);
        setDataKeyboard(true);
        setRemoveOnHide(true);

        ModalBody modalBody = new ModalBody();
        modalBody.setHeight("300px");
        textArea.setStyleName("");
        textArea.setHeight("250px");
        textArea.setWidth("100%");
        modalBody.add(textArea);

        ModalHeader modalHeader = new ModalHeader();
        modalHeader.setTitle("Upload annotation for " + annotationInfo.getType() + " named: "+annotationInfo.getName());
        Button exampleLink = new Button("Example Annotation");
        exampleLink.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                textArea.setText(EXAMPLE_ANNOTATION);
            }
        });
        modalHeader.add(exampleLink);
        Button exampleLinkEmptyRef = new Button("Example Annotation Empty Ref");
        exampleLinkEmptyRef.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                textArea.setText(EXAMPLE_ANNOTATION_EMPTY_REF);
            }
        });
        modalHeader.add(exampleLinkEmptyRef);


        Button applyAnnotationsButton = new Button("Apply Annotations");
        applyAnnotationsButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                // TODO: convert and put in REST services with a nice return message.
                JSONObject reportObject = validateJson();
                Bootbox.confirm("Add: "+reportObject.toString(), new ConfirmCallback() {
                    @Override
                    public void callback(boolean result) {
                        if(result){
                            JSONObject annotationsObject = JSONParser.parseStrict(textArea.getText()).isObject();
                            JSONArray goAnnotations = annotationsObject.get(FeatureStringEnum.GO_ANNOTATIONS.getValue()).isArray();
                            GoRestService.addGoAnnotations(annotationInfo,goAnnotations);
                            JSONArray geneProducts = annotationsObject.get(FeatureStringEnum.GENE_PRODUCT.getValue()).isArray();
                            GeneProductRestService.addGeneProducts(annotationInfo,geneProducts);
                            JSONArray provenances = annotationsObject.get(FeatureStringEnum.PROVENANCE.getValue()).isArray();
                            ProvenanceRestService.addProvenances(annotationInfo,provenances);
                        }
                    }
                });
                hide();
            }
        });

        Button validateButton = new Button("Validate");
        validateButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                try {
                    JSONObject reportObject = validateJson();
                    Bootbox.alert(reportObject.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    Bootbox.alert("There was a problem: "+e.getMessage());
                }
            }
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                hide();
            }
        });

        ModalFooter modalFooter = new ModalFooter();
        modalFooter.add(cancelButton);
        modalFooter.add(validateButton);
        modalFooter.add(applyAnnotationsButton);

        add(modalHeader);
        add(modalBody);
        add(modalFooter);
        show();
    }

    private JSONObject validateJson() {
        String jsonData = textArea.getText().trim();
        JSONObject reportObject = new JSONObject();
        JSONObject jsonObject = JSONParser.parseStrict(jsonData).isObject();
        if(jsonObject.containsKey(FeatureStringEnum.GO_ANNOTATIONS.getValue())){
            reportObject.put(FeatureStringEnum.GO_ANNOTATIONS.getValue(),new JSONNumber(jsonObject.get(FeatureStringEnum.GO_ANNOTATIONS.getValue()).isArray().size()));
        }
        else{
            reportObject.put(FeatureStringEnum.GO_ANNOTATIONS.getValue(),new JSONNumber(0));
        }
        if(jsonObject.containsKey(FeatureStringEnum.PROVENANCE.getValue())){
            reportObject.put(FeatureStringEnum.PROVENANCE.getValue(),new JSONNumber(jsonObject.get(FeatureStringEnum.PROVENANCE.getValue()).isArray().size()));
        }
        else{
            reportObject.put(FeatureStringEnum.PROVENANCE.getValue(),new JSONNumber(0));
        }
        if(jsonObject.containsKey(FeatureStringEnum.GENE_PRODUCT.getValue())){
            reportObject.put(FeatureStringEnum.GENE_PRODUCT.getValue(),new JSONNumber(jsonObject.get(FeatureStringEnum.GENE_PRODUCT.getValue()).isArray().size()));
        }
        else{
            reportObject.put(FeatureStringEnum.GENE_PRODUCT.getValue(),new JSONNumber(0));
        }
        return reportObject;
    }
}
