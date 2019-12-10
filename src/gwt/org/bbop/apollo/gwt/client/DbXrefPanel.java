package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.EditTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.DbXrefInfo;
import org.bbop.apollo.gwt.client.event.AnnotationInfoChangeEvent;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.DbXrefRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.Comparator;
import java.util.List;

/**
 * Created by deepak.unni3 on 9/16/16.
 */
public class DbXrefPanel extends Composite {


    interface DbXrefPanelUiBinder extends UiBinder<Widget, DbXrefPanel> { }

    private static DbXrefPanelUiBinder ourUiBinder = GWT.create(DbXrefPanelUiBinder.class);

    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<DbXrefInfo> dataGrid = new DataGrid<>(10, tablecss);
    @UiField
    TextBox tagInputBox;
    @UiField
    TextBox valueInputBox;
    @UiField
    Button addDbXrefButton ;
    @UiField
    Button deleteDbXrefButton ;

    private AnnotationInfo internalAnnotationInfo = null;
    private DbXrefInfo internalDbXrefInfo = null;
    private String oldTag, oldValue;
    private String tag, value;

    private static ListDataProvider<DbXrefInfo> dataProvider = new ListDataProvider<>();
    private static List<DbXrefInfo> dbXrefInfoList = dataProvider.getList();
    private SingleSelectionModel<DbXrefInfo> selectionModel = new SingleSelectionModel<>();

    public DbXrefPanel() {

        initWidget(ourUiBinder.createAndBindUi(this));

        dataGrid.setWidth("100%");
        initializeTable();
        dataProvider.addDataDisplay(dataGrid);
        dataGrid.setSelectionModel(selectionModel);
        selectionModel.clear();
        deleteDbXrefButton.setEnabled(false);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent selectionChangeEvent) {
                if (selectionModel.getSelectedSet().isEmpty()) {
                    deleteDbXrefButton.setEnabled(false);
                } else {
                    selectDbXrefData(selectionModel.getSelectedObject());
                    deleteDbXrefButton.setEnabled(true);
                }
            }
        });

    }

    public void initializeTable() {
        EditTextCell tagCell = new EditTextCell();
        Column<DbXrefInfo, String> tagColumn = new Column<DbXrefInfo, String>(tagCell) {
            @Override
            public String getValue(DbXrefInfo dbXrefInfo) {
                return dbXrefInfo.getTag();
            }
        };
        tagColumn.setFieldUpdater(new FieldUpdater<DbXrefInfo, String>() {
            @Override
            public void update(int i, DbXrefInfo object, String s) {
                if (!object.getTag().equals(s)) {
                    GWT.log("Tag Changed");
                    object.setTag(s);
                    selectDbXrefData(object);
                    triggerUpdate();
                }
            }
        });
        tagColumn.setSortable(true);

        EditTextCell valueCell = new EditTextCell();
        Column<DbXrefInfo, String> valueColumn = new Column<DbXrefInfo, String>(valueCell) {
            @Override
            public String getValue(DbXrefInfo dbXrefInfo) {
                return dbXrefInfo.getValue();
            }
        };
        valueColumn.setFieldUpdater(new FieldUpdater<DbXrefInfo, String>() {
            @Override
            public void update(int i, DbXrefInfo object, String s) {
                if (!object.getValue().equals(s)) {
                    GWT.log("Value Changed");
                    object.setValue(s);
                    selectDbXrefData(object);
                    triggerUpdate();
                }
            }
        });
        valueColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        valueColumn.setSortable(true);

        dataGrid.addColumn(tagColumn, "Prefix");
        dataGrid.setColumnWidth(0, "50px");
        dataGrid.addColumn(valueColumn, "Accession");
        dataGrid.setColumnWidth(1, "100%");

        ColumnSortEvent.ListHandler<DbXrefInfo> sortHandler = new ColumnSortEvent.ListHandler<DbXrefInfo>(dbXrefInfoList);
        dataGrid.addColumnSortHandler(sortHandler);

        sortHandler.setComparator(tagColumn, new Comparator<DbXrefInfo>() {
            @Override
            public int compare(DbXrefInfo o1, DbXrefInfo o2) {
                return o1.getTag().compareTo(o2.getTag());
            }
        });

        sortHandler.setComparator(valueColumn, new Comparator<DbXrefInfo>() {
            @Override
            public int compare(DbXrefInfo o1, DbXrefInfo o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
    }

    public void updateData(AnnotationInfo annotationInfo) {
        GWT.log("updating annotation info: "+annotationInfo);
        if (annotationInfo == null) {
            return;
        }
        this.internalAnnotationInfo = annotationInfo;
//        dbXrefInfoList.clear();
        dbXrefInfoList.clear();
        dbXrefInfoList.addAll(annotationInfo.getDbXrefList());
        GWT.log("List size: "+dbXrefInfoList.size());
        redrawTable();
        setVisible(true);
    }

    public void updateData() {
        updateData(null);
    }

    public void selectDbXrefData(DbXrefInfo v) {
        this.internalDbXrefInfo = v;
        // tag
        this.oldTag = this.tag;
        this.tag = this.internalDbXrefInfo.getTag();

        // value
        this.oldValue = this.value;
        this.value = this.internalDbXrefInfo.getValue();

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

            RequestCallback requestCallBack = new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    JSONValue returnValue = JSONParser.parseStrict(response.getText());
                    GWT.log("worked!");
//                    Annotator.eventBus.fireEvent(new AnnotationInfoChangeEvent(updatedInfo, AnnotationInfoChangeEvent.Action.UPDATE));
                    redrawTable();
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    Bootbox.alert("Error updating variant info property: " + exception);
//                    updateData();
                    // TODO: reset data
                    redrawTable();
                }
            };
//            0: "SEND↵destination:/app/AnnotationNotification↵content-length:310↵↵"{\"track\":\"ctgA\",\"features\":[{\"uniquename\":\"fd57cc6a-8e29-4a48-9832-82c06bcc869c\",\"old_dbxrefs\":[{\"db\":\"aasd\",\"accession\":\"12312\"}],\"new_dbxrefs\":[{\"db\":\"asdfasdfaaeee\",\"accession\":\"12312\"}]}],\"operation\":\"update_non_primary_dbxrefs\",\"clientToken\":\"18068643442091616983\"}""
//
//            JSONArray featuresArray = new JSONArray();
//            JSONObject featureObject = new JSONObject();
//            String featureUniqueName = this.internalAnnotationInfo.getUniqueName();
//            featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
//            JSONArray oldDbXrefJsonArray = new JSONArray();
//            JSONObject oldDbXrefJsonObject = new JSONObject();
//            oldDbXrefJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(this.oldTag));
//            oldDbXrefJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(this.oldValue));
//            oldDbXrefJsonArray.set(0, oldDbXrefJsonObject);
//            featureObject.put(FeatureStringEnum.OLD_DBXREFS.getValue(), oldDbXrefJsonArray);
//
//            JSONArray newDbXrefJsonArray = new JSONArray();
//            JSONObject newDbXrefJsonObject = new JSONObject();
//            newDbXrefJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(this.tag));
//            newDbXrefJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(this.value));
//            newDbXrefJsonArray.set(0, newDbXrefJsonObject);
//            featureObject.put(FeatureStringEnum.NEW_DBXREFS.getValue(), newDbXrefJsonArray);
//            featuresArray.set(0, featureObject);
//
//            JSONObject requestObject = new JSONObject();
//            requestObject.put("operation", new JSONString("update_variant_info"));
//            requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(this.internalAnnotationInfo.getSequence()));
////            requestObject.put(FeatureStringEnum.CLIENT_TOKEN.getValue(), new JSONString(Annotator.getClientToken()));
//            requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);

            DbXrefRestService.updateDbXref(requestCallBack,this.internalAnnotationInfo,new DbXrefInfo(this.oldTag,this.oldValue),new DbXrefInfo(this.tag,this.value));;

//            String url = Annotator.getRootUrl() + "annotationEditorController/updateDbXref";
//            RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
//            builder.setHeader("Content-type", "application/x-www-form-urlencoded");
//            StringBuilder sb = new StringBuilder();
//            JSONArray featuresArray = new JSONArray();
//            JSONObject featureObject = new JSONObject();
//            String featureUniqueName = this.internalAnnotationInfo.getUniqueName();
//            featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
//
//            JSONArray oldDbXrefJsonArray = new JSONArray();
//            JSONObject oldDbXrefJsonObject = new JSONObject();
//            oldDbXrefJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(this.oldTag));
//            oldDbXrefJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(this.oldValue));
//            oldDbXrefJsonArray.set(0, oldDbXrefJsonObject);
//            featureObject.put(FeatureStringEnum.OLD_VARIANT_INFO.getValue(), oldDbXrefJsonArray);
//
//            JSONArray newDbXrefJsonArray = new JSONArray();
//            JSONObject newDbXrefJsonObject = new JSONObject();
//            newDbXrefJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(this.tag));
//            newDbXrefJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(this.value));
//            newDbXrefJsonArray.set(0, newDbXrefJsonObject);
//            featureObject.put(FeatureStringEnum.NEW_VARIANT_INFO.getValue(), newDbXrefJsonArray);
//
//            featuresArray.set(0, featureObject);
//
//            JSONObject requestObject = new JSONObject();
//            requestObject.put("operation", new JSONString("update_variant_info"));
//            requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(this.internalAnnotationInfo.getSequence()));
//            requestObject.put(FeatureStringEnum.CLIENT_TOKEN.getValue(), new JSONString(Annotator.getClientToken()));
//            requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
//            sb.append("data=" + requestObject.toString());
//
//            final AnnotationInfo updatedInfo = this.internalAnnotationInfo;
//            builder.setRequestData(sb.toString());
        }
    }

    public void redrawTable() {
        this.dataGrid.redraw();
    }

    @UiHandler("addDbXrefButton")
    public void addDbXrefButton(ClickEvent ce) {
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
            String url = Annotator.getRootUrl() + "annotator/addDbXref";
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

    @UiHandler("deleteDbXrefButton")
    public void deleteDbXref(ClickEvent ce) {

        if (this.internalDbXrefInfo != null) {
            String url = Annotator.getRootUrl() + "annotator/deleteDbXref";
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