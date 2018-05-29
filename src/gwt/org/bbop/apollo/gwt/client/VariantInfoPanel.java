package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.EditTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.VariantPropertyInfo;
import org.bbop.apollo.gwt.client.event.AnnotationInfoChangeEvent;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by deepak.unni3 on 9/16/16.
 */
public class VariantInfoPanel extends Composite {

    private AnnotationInfo internalAnnotationInfo = null;
    private VariantPropertyInfo internalVariantPropertyInfo = null;
    private String oldTag, oldValue;
    private String tag, value;

    interface VariantInfoPanelUiBinder extends UiBinder<Widget, VariantInfoPanel> {
    }

    private static VariantInfoPanelUiBinder ourUiBinder = GWT.create(VariantInfoPanelUiBinder.class);

    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<VariantPropertyInfo> dataGrid = new DataGrid<>(10, tablecss);

    private static ListDataProvider<VariantPropertyInfo> dataProvider = new ListDataProvider<>();
    private static List<VariantPropertyInfo> variantPropertyInfoList = dataProvider.getList();
    private SingleSelectionModel<VariantPropertyInfo> selectionModel = new SingleSelectionModel<>();
    private Column<VariantPropertyInfo, String> tagColumn;
    private Column<VariantPropertyInfo, String> valueColumn;

    @UiField
    TextBox tagInputBox;
    @UiField
    TextBox valueInputBox;
    @UiField
    Button addVariantInfoButton = new Button();
    @UiField
    Button deleteVariantInfoButton = new Button();

    public VariantInfoPanel() {
        dataGrid.setWidth("100%");
        initializeTable();
        dataProvider.addDataDisplay(dataGrid);
        dataGrid.setSelectionModel(selectionModel);
        tagInputBox = new TextBox();
        valueInputBox = new TextBox();

        selectionModel.clear();
        deleteVariantInfoButton.setEnabled(false);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent selectionChangeEvent) {
                if (selectionModel.getSelectedSet().isEmpty()) {
                    deleteVariantInfoButton.setEnabled(false);
                } else {
                    updateVariantInfoData(selectionModel.getSelectedObject());
                    deleteVariantInfoButton.setEnabled(true);
                }
            }
        });

        initWidget(ourUiBinder.createAndBindUi(this));
    }

    public void initializeTable() {
        EditTextCell tagCell = new EditTextCell();
        tagColumn = new Column<VariantPropertyInfo, String>(tagCell) {
            @Override
            public String getValue(VariantPropertyInfo variantPropertyInfo) {
                return variantPropertyInfo.getTag();
            }
        };
        tagColumn.setFieldUpdater(new FieldUpdater<VariantPropertyInfo, String>() {
            @Override
            public void update(int i, VariantPropertyInfo object, String s) {
                if (!object.getTag().equals(s)) {
                    GWT.log("Tag Changed");
                    object.setTag(s);
                    updateVariantInfoData(object);
                    triggerUpdate();
                }
            }
        });
        tagColumn.setSortable(true);

        EditTextCell valueCell = new EditTextCell();
        valueColumn = new Column<VariantPropertyInfo, String>(valueCell) {
            @Override
            public String getValue(VariantPropertyInfo variantPropertyInfo) {
                return variantPropertyInfo.getValue();
            }
        };
        valueColumn.setFieldUpdater(new FieldUpdater<VariantPropertyInfo, String>() {
            @Override
            public void update(int i, VariantPropertyInfo object, String s) {
                if (!object.getValue().equals(s)) {
                    GWT.log("Value Changed");
                    object.setValue(s);
                    updateVariantInfoData(object);
                    triggerUpdate();
                }
            }
        });
        valueColumn.setSortable(true);

        dataGrid.addColumn(tagColumn, "Tag");
        dataGrid.addColumn(valueColumn, "Value");

        ColumnSortEvent.ListHandler<VariantPropertyInfo> sortHandler = new ColumnSortEvent.ListHandler<VariantPropertyInfo>(variantPropertyInfoList);
        dataGrid.addColumnSortHandler(sortHandler);

        sortHandler.setComparator(tagColumn, new Comparator<VariantPropertyInfo>() {
            @Override
            public int compare(VariantPropertyInfo o1, VariantPropertyInfo o2) {
                return o1.getTag().compareTo(o2.getTag());
            }
        });

        sortHandler.setComparator(valueColumn, new Comparator<VariantPropertyInfo>() {
            @Override
            public int compare(VariantPropertyInfo o1, VariantPropertyInfo o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
    }

    public void updateData(AnnotationInfo annotationInfo) {
        if (annotationInfo == null) {
            return;
        }
        this.internalAnnotationInfo = annotationInfo;
        variantPropertyInfoList.clear();
        variantPropertyInfoList.addAll(annotationInfo.getVariantProperties());

        if (variantPropertyInfoList.size() > 0) {
            updateVariantInfoData(variantPropertyInfoList.get(0));
        }
        redrawTable();
    }

    public void updateVariantInfoData(VariantPropertyInfo v) {
        this.internalVariantPropertyInfo = v;
        // tag
        this.oldTag = this.tag;
        this.tag = this.internalVariantPropertyInfo.getTag();

        // value
        this.oldValue = this.value;
        this.value = this.internalVariantPropertyInfo.getValue();

        redrawTable();
        setVisible(true);
    }

    public void triggerUpdate() {
        boolean tagValidated = false;
        boolean valueValidated = false;

        if (this.tag != null && !this.tag.isEmpty()) {
            tagValidated = true;
        }
        if (this.value != null && !this.value.isEmpty()) {
            valueValidated = true;
        }

        if (tagValidated && valueValidated) {
            String url = Annotator.getRootUrl() + "annotator/updateVariantInfo";
            RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
            builder.setHeader("Content-type", "application/x-www-form-urlencoded");
            StringBuilder sb = new StringBuilder();
            JSONArray featuresArray = new JSONArray();
            JSONObject featureObject = new JSONObject();
            String featureUniqueName = this.internalAnnotationInfo.getUniqueName();
            featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));

            JSONArray oldVariantInfoJsonArray = new JSONArray();
            JSONObject oldVariantInfoJsonObject = new JSONObject();
            oldVariantInfoJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(this.oldTag));
            oldVariantInfoJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(this.oldValue));
            oldVariantInfoJsonArray.set(0, oldVariantInfoJsonObject);
            featureObject.put(FeatureStringEnum.OLD_VARIANT_INFO.getValue(), oldVariantInfoJsonArray);

            JSONArray newVariantInfoJsonArray = new JSONArray();
            JSONObject newVariantInfoJsonObject = new JSONObject();
            newVariantInfoJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(this.tag));
            newVariantInfoJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(this.value));
            newVariantInfoJsonArray.set(0, newVariantInfoJsonObject);
            featureObject.put(FeatureStringEnum.NEW_VARIANT_INFO.getValue(), newVariantInfoJsonArray);

            featuresArray.set(0, featureObject);

            JSONObject requestObject = new JSONObject();
            requestObject.put("operation", new JSONString("update_variant_info"));
            requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(this.internalAnnotationInfo.getSequence()));
            requestObject.put(FeatureStringEnum.CLIENT_TOKEN.getValue(), new JSONString(Annotator.getClientToken()));
            requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
            sb.append("data=" + requestObject.toString());

            final AnnotationInfo updatedInfo = this.internalAnnotationInfo;
            builder.setRequestData(sb.toString());
            RequestCallback requestCallBack = new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    JSONValue returnValue = JSONParser.parseStrict(response.getText());
                    Annotator.eventBus.fireEvent(new AnnotationInfoChangeEvent(updatedInfo, AnnotationInfoChangeEvent.Action.UPDATE));
                    redrawTable();
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    Bootbox.alert("Error updating variant info property: " + exception);
                    redrawTable();
                }
            };

            try {
                builder.setCallback(requestCallBack);
                builder.send();
            } catch(RequestException e) {
                Bootbox.alert("RequestException: " + e.getMessage());
            }
        }
    }

    public void redrawTable() {
        this.dataGrid.redraw();
    }

    @UiHandler("addVariantInfoButton")
    public void addVariantInfoButton(ClickEvent ce) {
        String tag = tagInputBox.getText();
        String value = valueInputBox.getText();

        boolean tagValidated = false;
        boolean valueValidated = false;

        if (this.tag != null && !this.tag.isEmpty()) {
            tagValidated = true;
        }
        if (this.value != null && !this.value.isEmpty()) {
            valueValidated = true;
        }

        if (tagValidated && valueValidated) {
            this.tagInputBox.clear();
            this.valueInputBox.clear();
            String url = Annotator.getRootUrl() + "annotator/addVariantInfo";
            RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
            builder.setHeader("Content-type", "application/x-www-form-urlencoded");
            StringBuilder sb = new StringBuilder();
            JSONArray featuresArray = new JSONArray();
            JSONObject featureObject = new JSONObject();
            String featureUniqueName = this.internalAnnotationInfo.getUniqueName();
            featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));

            JSONArray variantInfoJsonArray = new JSONArray();
            JSONObject variantInfoJsonObject = new JSONObject();
            variantInfoJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(tag));
            variantInfoJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(value));
            variantInfoJsonArray.set(0, variantInfoJsonObject);
            featureObject.put(FeatureStringEnum.VARIANT_INFO.getValue(), variantInfoJsonArray);

            featuresArray.set(0, featureObject);

            JSONObject requestObject = new JSONObject();
            requestObject.put("operation", new JSONString("add_variant_info"));
            requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(this.internalAnnotationInfo.getSequence()));
            requestObject.put(FeatureStringEnum.CLIENT_TOKEN.getValue(), new JSONString(Annotator.getClientToken()));
            requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
            sb.append("data=" + requestObject.toString());

            final AnnotationInfo updatedInfo = this.internalAnnotationInfo;
            builder.setRequestData(sb.toString());
            RequestCallback requestCallBack = new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    JSONValue returnValue = JSONParser.parseStrict(response.getText());
                    Annotator.eventBus.fireEvent(new AnnotationInfoChangeEvent(updatedInfo, AnnotationInfoChangeEvent.Action.UPDATE));
                    redrawTable();
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    Bootbox.alert("Error adding variant info property: " + exception);
                    redrawTable();
                }
            };

            try {
                builder.setCallback(requestCallBack);
                builder.send();
            } catch(RequestException e) {
                Bootbox.alert("RequestException: " + e.getMessage());
            }
        }
    }

    @UiHandler("deleteVariantInfoButton")
    public void deleteVariantInfo(ClickEvent ce) {

        if (this.internalVariantPropertyInfo != null) {
            String url = Annotator.getRootUrl() + "annotator/deleteVariantInfo";
            RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
            builder.setHeader("Content-type", "application/x-www-form-urlencoded");
            StringBuilder sb = new StringBuilder();
            JSONArray featuresArray = new JSONArray();
            JSONObject featureObject = new JSONObject();
            String featureUniqueName = this.internalAnnotationInfo.getUniqueName();
            featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));

            JSONArray variantInfoJsonArray = new JSONArray();
            JSONObject variantInfoJsonObject = new JSONObject();
            variantInfoJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(tag));
            variantInfoJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(value));
            variantInfoJsonArray.set(0, variantInfoJsonObject);
            featureObject.put(FeatureStringEnum.VARIANT_INFO.getValue(), variantInfoJsonArray);

            featuresArray.set(0, featureObject);

            JSONObject requestObject = new JSONObject();
            requestObject.put("operation", new JSONString("delete_variant_info"));
            requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(this.internalAnnotationInfo.getSequence()));
            requestObject.put(FeatureStringEnum.CLIENT_TOKEN.getValue(), new JSONString(Annotator.getClientToken()));
            requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
            sb.append("data=" + requestObject.toString());

            final AnnotationInfo updatedInfo = this.internalAnnotationInfo;
            builder.setRequestData(sb.toString());
            RequestCallback requestCallBack = new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    JSONValue returnValue = JSONParser.parseStrict(response.getText());
                    Annotator.eventBus.fireEvent(new AnnotationInfoChangeEvent(updatedInfo, AnnotationInfoChangeEvent.Action.UPDATE));
                    redrawTable();
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    Bootbox.alert("Error deleting variant info property: " + exception);
                    redrawTable();
                }
            };

            try {
                builder.setCallback(requestCallBack);
                builder.send();
            } catch(RequestException e) {
                Bootbox.alert("RequestException: " + e.getMessage());
            }
        }
    }
}