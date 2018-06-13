package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.EditTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextCell;
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
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.AllelePropertyInfo;
import org.bbop.apollo.gwt.client.dto.AlternateAlleleInfo;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
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


public class AlleleInfoPanel extends Composite {

    private AnnotationInfo internalAnnotationInfo = null;
    private AllelePropertyInfo internalAllelePropertyInfo = null;
    private String oldTag, oldValue;
    private String bases, tag, value;

    interface AlleleInfoPanelUiBinder extends UiBinder<Widget, AlleleInfoPanel> {
    }

    private static AlleleInfoPanelUiBinder ourUiBinder = GWT.create(AlleleInfoPanelUiBinder.class);

    private DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<AllelePropertyInfo> dataGrid = new DataGrid<>(10, tablecss);

    private static ListDataProvider<AllelePropertyInfo> dataProvider = new ListDataProvider<>();
    private static List<AllelePropertyInfo> allelePropertyInfoList = dataProvider.getList();
    private SingleSelectionModel<AllelePropertyInfo> selectionModel = new SingleSelectionModel<>();
    private Column<AllelePropertyInfo, String> alleleBaseColumn;
    private Column<AllelePropertyInfo, String> tagColumn;
    private Column<AllelePropertyInfo, String> valueColumn;

    @UiField
    ListBox alleleList;
    @UiField
    TextBox tagInputBox;
    @UiField
    TextBox valueInputBox;
    @UiField
    Button addAlleleInfoButton = new Button();
    @UiField
    Button deleteAlleleInfoButton = new Button();

    public AlleleInfoPanel() {
        dataGrid.setWidth("100%");
        initializeTable();
        dataProvider.addDataDisplay(dataGrid);
        dataGrid.setSelectionModel(selectionModel);

        selectionModel.clear();
        deleteAlleleInfoButton.setEnabled(false);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent selectionChangeEvent) {
                if (selectionModel.getSelectedSet().isEmpty()) {
                    deleteAlleleInfoButton.setEnabled(false);
                } else {
                    updateAlleleInfoData(selectionModel.getSelectedObject());
                    deleteAlleleInfoButton.setEnabled(true);
                }
            }
        });

        initWidget(ourUiBinder.createAndBindUi(this));
    }

    public void initializeTable() {
        TextCell alleleBaseCell = new TextCell();
        alleleBaseColumn = new Column<AllelePropertyInfo, String>(alleleBaseCell) {
            @Override
            public String getValue(AllelePropertyInfo allelePropertyInfo) { return allelePropertyInfo.getBases(); }
        };
        alleleBaseColumn.setSortable(true);

        EditTextCell tagCell = new EditTextCell();
        tagColumn = new Column<AllelePropertyInfo, String>(tagCell) {
            @Override
            public String getValue(AllelePropertyInfo allelePropertyInfo) {
                return allelePropertyInfo.getTag();
            }
        };
        tagColumn.setFieldUpdater(new FieldUpdater<AllelePropertyInfo, String>() {
            @Override
            public void update(int i, AllelePropertyInfo object, String s) {
                if (!object.getTag().equals(s)) {
                    GWT.log("Tag Changed");
                    object.setTag(s);
                    updateAlleleInfoData(object);
                    triggerUpdate();
                }
            }
        });
        tagColumn.setSortable(true);

        EditTextCell valueCell = new EditTextCell();
        valueColumn = new Column<AllelePropertyInfo, String>(valueCell) {
            @Override
            public String getValue(AllelePropertyInfo allelePropertyInfo) {
                return allelePropertyInfo.getValue();
            }
        };
        valueColumn.setFieldUpdater(new FieldUpdater<AllelePropertyInfo, String>() {
            @Override
            public void update(int i, AllelePropertyInfo object, String s) {
                if (!object.getValue().equals(s)) {
                    GWT.log("Value Changed");
                    object.setValue(s);
                    updateAlleleInfoData(object);
                    triggerUpdate();
                }
            }
        });
        valueColumn.setSortable(true);

        dataGrid.addColumn(alleleBaseColumn, "Allele");
        dataGrid.addColumn(tagColumn, "Tag");
        dataGrid.addColumn(valueColumn, "Value");

        ColumnSortEvent.ListHandler<AllelePropertyInfo> sortHandler = new ColumnSortEvent.ListHandler<AllelePropertyInfo>(allelePropertyInfoList);
        dataGrid.addColumnSortHandler(sortHandler);

        sortHandler.setComparator(alleleBaseColumn, new Comparator<AllelePropertyInfo>() {
            @Override
            public int compare(AllelePropertyInfo o1, AllelePropertyInfo o2) {
                return o1.getBases().compareTo(o2.getBases());
            }
        });

        sortHandler.setComparator(tagColumn, new Comparator<AllelePropertyInfo>() {
            @Override
            public int compare(AllelePropertyInfo o1, AllelePropertyInfo o2) {
                return o1.getTag().compareTo(o2.getTag());
            }
        });

        sortHandler.setComparator(valueColumn, new Comparator<AllelePropertyInfo>() {
            @Override
            public int compare(AllelePropertyInfo o1, AllelePropertyInfo o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
    }

    public void updateData(AnnotationInfo annotationInfo) {
        if (annotationInfo == null) {
            return;
        }
        this.internalAnnotationInfo = annotationInfo;
        allelePropertyInfoList.clear();

        for (AlternateAlleleInfo alternateAlleleInfo : this.internalAnnotationInfo.getAlternateAlleles()) {
            allelePropertyInfoList.addAll(alternateAlleleInfo.getAlleleInfo());
        }

        if (allelePropertyInfoList.size() > 0) {
            updateAlleleInfoData(allelePropertyInfoList.get(0));
        }

        alleleList.clear();
        for (AlternateAlleleInfo alternateAlleleInfo : this.internalAnnotationInfo.getAlternateAlleles()) {
            alleleList.addItem(alternateAlleleInfo.getBases());
        }

        redrawTable();
    }

    public void updateAlleleInfoData(AllelePropertyInfo v) {
        this.internalAllelePropertyInfo = v;

        // allele bases
        this.bases = v.getBases();

        // tag
        this.oldTag = this.tag;
        this.tag = this.internalAllelePropertyInfo.getTag();

        // value
        this.oldValue = this.value;
        this.value = this.internalAllelePropertyInfo.getValue();

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
            String url = Annotator.getRootUrl() + "annotator/updateAlleleInfo";
            RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
            builder.setHeader("Content-type", "application/x-www-form-urlencoded");
            StringBuilder sb = new StringBuilder();
            JSONArray featuresArray = new JSONArray();
            JSONObject featureObject = new JSONObject();
            String featureUniqueName = this.internalAnnotationInfo.getUniqueName();
            featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
            JSONObject alleleObject = new JSONObject();
            alleleObject.put(FeatureStringEnum.BASES.getValue(), new JSONString(this.bases));
            featureObject.put(FeatureStringEnum.ALLELE.getValue(), alleleObject);

            JSONArray oldAlleleInfoJsonArray = new JSONArray();
            JSONObject oldAlleleInfoJsonObject = new JSONObject();
            oldAlleleInfoJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(this.oldTag));
            oldAlleleInfoJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(this.oldValue));
            oldAlleleInfoJsonArray.set(0, oldAlleleInfoJsonObject);
            //featureObject.put(FeatureStringEnum.ALLELE.getValue(), )
            featureObject.put(FeatureStringEnum.OLD_ALLELE_INFO.getValue(), oldAlleleInfoJsonArray);

            JSONArray newAlleleInfoJsonArray = new JSONArray();
            JSONObject newAlleleInfoJsonObject = new JSONObject();
            newAlleleInfoJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(this.tag));
            newAlleleInfoJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(this.value));
            newAlleleInfoJsonArray.set(0, newAlleleInfoJsonObject);
            featureObject.put(FeatureStringEnum.NEW_ALLELE_INFO.getValue(), newAlleleInfoJsonArray);

            featuresArray.set(0, featureObject);

            JSONObject requestObject = new JSONObject();
            requestObject.put("operation", new JSONString("update_allele_info"));
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
                    Bootbox.alert("Error updating allele info property: " + exception);
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

    @UiHandler("addAlleleInfoButton")
    public void addAlleleInfo(ClickEvent ce) {
        String allele = alleleList.getSelectedValue();
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
            String url = Annotator.getRootUrl() + "annotator/addAlleleInfo";
            RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
            builder.setHeader("Content-type", "application/x-www-form-urlencoded");
            StringBuilder sb = new StringBuilder();
            JSONArray featuresArray = new JSONArray();
            JSONObject featureObject = new JSONObject();
            String featureUniqueName = this.internalAnnotationInfo.getUniqueName();
            featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));

            JSONArray alleleInfoJsonArray = new JSONArray();
            JSONObject alleleInfoJsonObject = new JSONObject();
            alleleInfoJsonObject.put(FeatureStringEnum.ALLELE.getValue(), new JSONString(allele));
            alleleInfoJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(tag));
            alleleInfoJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(value));
            alleleInfoJsonArray.set(0, alleleInfoJsonObject);
            featureObject.put(FeatureStringEnum.ALLELE_INFO.getValue(), alleleInfoJsonArray);

            featuresArray.set(0, featureObject);

            JSONObject requestObject = new JSONObject();
            requestObject.put("operation", new JSONString("add_allele_info"));
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
                    Bootbox.alert("Error adding allele info property: " + exception);
                    redrawTable();
                }
            };

            try {
                builder.setCallback(requestCallBack);
                builder.send();
            } catch(RequestException re) {
                Bootbox.alert("RequestException: " + re.getMessage());
            }
        }
    }

    @UiHandler("deleteAlleleInfoButton")
    public void deleteAlleleInfo(ClickEvent ce) {

        if (this.internalAllelePropertyInfo != null) {
            String url = Annotator.getRootUrl() + "annotator/deleteAlleleInfo";
            RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
            builder.setHeader("Content-type", "application/x-www-form-urlencoded");
            StringBuilder sb = new StringBuilder();
            JSONArray featuresArray = new JSONArray();
            JSONObject featureObject = new JSONObject();
            String featureUniqueName = this.internalAnnotationInfo.getUniqueName();
            featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));

            JSONArray alleleInfoJsonArray = new JSONArray();
            JSONObject alleleInfoJsonObject = new JSONObject();
            alleleInfoJsonObject.put(FeatureStringEnum.ALLELE.getValue(), new JSONString(this.internalAllelePropertyInfo.getBases()));
            alleleInfoJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(this.internalAllelePropertyInfo.getTag()));
            alleleInfoJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(this.internalAllelePropertyInfo.getValue()));
            alleleInfoJsonArray.set(0, alleleInfoJsonObject);
            featureObject.put(FeatureStringEnum.ALLELE_INFO.getValue(), alleleInfoJsonArray);

            featuresArray.set(0, featureObject);

            JSONObject requestObject = new JSONObject();
            requestObject.put("operation", new JSONString("delete_allele_info"));
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
                    Bootbox.alert("Error delete allele info property: " + exception);
                    redrawTable();
                }
            };

            try {
                builder.setCallback(requestCallBack);
                builder.send();
            } catch(RequestException re) {
                Bootbox.alert("RequestException: " + re.getMessage());
            }
        }
    }
}