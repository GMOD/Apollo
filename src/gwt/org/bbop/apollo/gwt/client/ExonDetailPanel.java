package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.json.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.event.AnnotationInfoChangeEvent;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.AnnotationRestService;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.InputGroupAddon;
import org.gwtbootstrap3.client.ui.TextBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 1/9/15.
 */
public class ExonDetailPanel extends Composite {

    interface ExonDetailPanelUiBinder extends UiBinder<Widget, ExonDetailPanel> {
    }

    Dictionary dictionary = Dictionary.getDictionary("Options");
    String rootUrl = dictionary.get("rootUrl");
//    private JSONObject internalData;
    private AnnotationInfo internalAnnotationInfo;

    private static ExonDetailPanelUiBinder ourUiBinder = GWT.create(ExonDetailPanelUiBinder.class);
    @UiField
    TextBox maxField;
    @UiField
    TextBox minField;
    @UiField
    Button positiveStrandValue;
    @UiField
    Button negativeStrandValue;

    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<AnnotationInfo> dataGrid = new DataGrid<>(10, tablecss);
    private static ListDataProvider<AnnotationInfo> dataProvider = new ListDataProvider<>();
    private static List<AnnotationInfo> annotationInfoList = dataProvider.getList();


    public ExonDetailPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));


        dataProvider.addDataDisplay(dataGrid);
    }

    private void enableFields(boolean enabled) {
        minField.setEnabled(enabled);
        maxField.setEnabled(enabled);
        positiveStrandValue.setEnabled(enabled);
        negativeStrandValue.setEnabled(enabled);
    }

    public void updateData(AnnotationInfo annotationInfo){
        Window.alert("updating data: "+annotationInfo);
        if(annotationInfo==null) return ;
        annotationInfoList.clear();
        Window.alert("sublist: "+annotationInfo.getAnnotationInfoSet().size());
        for(AnnotationInfo annotationInfo1 : annotationInfo.getAnnotationInfoSet()){
            annotationInfoList.add(annotationInfo1);
        }

        if(annotationInfoList.size()>0){
            updateDetailData(annotationInfoList.get(0));
        }
    }

    public void updateDetailData(AnnotationInfo annotationInfo) {
        this.internalAnnotationInfo = annotationInfo;
        GWT.log("updating exon detail panel");
//        GWT.log(internalData.toString());
//        nameField.setText(internalData.get("name").isString().stringValue());

//        JSONObject locationObject = this.internalData.get("location").isObject();
        minField.setText(internalAnnotationInfo.getMin().toString());
        maxField.setText(internalAnnotationInfo.getMax().toString());

        if (internalAnnotationInfo.getStrand() > 0) {
            positiveStrandValue.setActive(true);
            negativeStrandValue.setActive(false);
        } else {
            positiveStrandValue.setActive(false);
            negativeStrandValue.setActive(true);
        }


        setVisible(true);
    }

    @UiHandler("minField")
    void handleMinChange(ChangeEvent e) {
        internalAnnotationInfo.setMin(Integer.parseInt(minField.getText()));
        updateExon();
    }

    @UiHandler("maxField")
    void handleMaxChange(ChangeEvent e) {
        internalAnnotationInfo.setMin(Integer.parseInt(minField.getText()));
        updateExon();
    }

    @UiHandler("positiveStrandValue")
    void handlePositiveStrand(ClickEvent e) {
        if (negativeStrandValue.isActive()) {
            internalAnnotationInfo.setStrand(1);
            positiveStrandValue.setActive(true);
            negativeStrandValue.setActive(false);
            updateExon();
        }
    }

    @UiHandler("negativeStrandValue")
    void handleNegativeStrand(ClickEvent e) {
        if (positiveStrandValue.isActive()) {
            internalAnnotationInfo.setStrand(-1);
            positiveStrandValue.setActive(false);
            negativeStrandValue.setActive(true);
            updateExon();
        }
    }


    private void updateExon() {
        String url = rootUrl + "/annotator/updateExon";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
//        sb.append("data=" + internalData.toString());
        sb.append("data=" + AnnotationRestService.convertAnnotationInfoToJSONObject(this.internalAnnotationInfo).toString());
        final AnnotationInfo updatedInfo = this.internalAnnotationInfo ;
        builder.setRequestData(sb.toString());
        enableFields(false);
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
                GWT.log("return value: "+returnValue.toString());
//                Window.alert("successful update: "+returnValue);
                enableFields(true);
                Annotator.eventBus.fireEvent(new AnnotationInfoChangeEvent(updatedInfo, AnnotationInfoChangeEvent.Action.UPDATE));
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error updating exon: " + exception);
                enableFields(true);
            }
        };
        try {
            builder.setCallback(requestCallback);
            builder.send();
            enableFields(true);
        } catch (RequestException e) {
            // Couldn't connect to server
            Window.alert(e.getMessage());
            enableFields(true);
        }

    }
}
