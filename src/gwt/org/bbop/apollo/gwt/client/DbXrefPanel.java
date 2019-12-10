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
                    updateDbXref();
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
                    updateDbXref();
                }
            }
        });
        valueColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        valueColumn.setSortable(true);

        dataGrid.addColumn(tagColumn, "Prefix");
        dataGrid.setColumnWidth(0, "100px");
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
//        dbXrefInfoList.sort(new Comparator<DbXrefInfo>() {
//            @Override
//            public int compare(DbXrefInfo o1, DbXrefInfo o2) {
//                int tagCompare = o1.getTag().compareToIgnoreCase(o2.getTag());
//                if(tagCompare!=0) return tagCompare;
//                return o1.getValue().compareToIgnoreCase(o2.getValue());
//            }
//        });
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

    public void updateDbXref() {
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
                    redrawTable();
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    Bootbox.alert("Error updating variant info property: " + exception);
                    resetTags();
                    // TODO: reset data
                    redrawTable();
                }
            };
            DbXrefRestService.updateDbXref(requestCallBack,this.internalAnnotationInfo,new DbXrefInfo(this.oldTag,this.oldValue),new DbXrefInfo(this.tag,this.value));;
        }
        else{
            resetTags();
        }
    }

    private void resetTags() {
        GWT.log("reseting tag");
        this.tag = this.oldTag;
        this.value = this.oldValue;
        updateData(this.internalAnnotationInfo);
        redrawTable();
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
            RequestCallback requestCallBack = new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    JSONValue returnValue = JSONParser.parseStrict(response.getText());
                    redrawTable();
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    Bootbox.alert("Error updating variant info property: " + exception);
                    resetTags();
                    // TODO: reset data
                    redrawTable();
                }
            };
            DbXrefRestService.addDbXref(requestCallBack,this.internalAnnotationInfo,new DbXrefInfo(tag,value));;
        }
    }

    @UiHandler("deleteDbXrefButton")
    public void deleteDbXref(ClickEvent ce) {

        if (this.internalDbXrefInfo != null) {
            RequestCallback requestCallBack = new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    JSONValue returnValue = JSONParser.parseStrict(response.getText());
                    redrawTable();
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    Bootbox.alert("Error deleting variant info property: " + exception);
                    redrawTable();
                }
            };
            DbXrefRestService.deleteDbXref(requestCallBack,this.internalAnnotationInfo,this.internalDbXrefInfo);;
        }
    }
}