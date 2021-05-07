package org.bbop.apollo.gwt.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.ui.HTML;
import grails.converters.JSON;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.json.JSONElement;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.constants.ModalBackdrop;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

/**
 * Created by ndunn on 4/30/15.
 */
public class UploadDialog extends Modal {

    final TextArea textArea = new TextArea();

    public UploadDialog(AnnotationInfo annotationInfo) {
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
        Button exampleLink = new Button("Example Text");
        exampleLink.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                textArea.setText("Example text here\nand here");
            }
        });
        modalHeader.add(exampleLink);


        Button applyAnnotationsButton = new Button("Apply Annotations");
        applyAnnotationsButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                // TODO: convert and put in REST services with a nice return message.
                Bootbox.alert("adding");
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
