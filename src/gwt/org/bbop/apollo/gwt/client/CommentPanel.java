package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.EditTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.CommentInfo;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.CommentRestService;
import org.gwtbootstrap3.client.ui.TextArea;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 */
public class CommentPanel extends Composite {

    interface CommentPanelUiBinder extends UiBinder<Widget, CommentPanel> {
    }

    private static CommentPanel.CommentPanelUiBinder ourUiBinder = GWT.create(CommentPanel.CommentPanelUiBinder.class);

    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<CommentInfo> dataGrid = new DataGrid<>(200, tablecss);
    @UiField
    TextArea commentInputBox;
    @UiField
    org.gwtbootstrap3.client.ui.Button addCommentButton;
    @UiField
    org.gwtbootstrap3.client.ui.Button deleteCommentButton;

    private AnnotationInfo internalAnnotationInfo = null;
    private CommentInfo internalCommentInfo = null;
    private String oldComment;
    private String tag, comment;

    private static ListDataProvider<CommentInfo> dataProvider = new ListDataProvider<>();
    private static List<CommentInfo> dbXrefInfoList = dataProvider.getList();
    private SingleSelectionModel<CommentInfo> selectionModel = new SingleSelectionModel<>();
    EditTextCell commentCell = new EditTextCell();

    public CommentPanel() {

        initWidget(ourUiBinder.createAndBindUi(this));

        dataGrid.setWidth("100%");
        initializeTable();
        dataProvider.addDataDisplay(dataGrid);
        dataGrid.setSelectionModel(selectionModel);
        selectionModel.clear();
        deleteCommentButton.setEnabled(false);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent selectionChangeEvent) {
                if (selectionModel.getSelectedSet().isEmpty()) {
                    deleteCommentButton.setEnabled(false);
                } else {
                    selectCommentData(selectionModel.getSelectedObject());
                    deleteCommentButton.setEnabled(true);
                }
            }
        });

    }

    public void initializeTable() {
        Column<CommentInfo, String> commentColumn = new Column<CommentInfo, String>(commentCell) {
            @Override
            public String getValue(CommentInfo dbXrefInfo) {
                return dbXrefInfo.getComment();
            }
        };
        commentColumn.setFieldUpdater(new FieldUpdater<CommentInfo, String>() {
            @Override
            public void update(int i, CommentInfo object, String s) {
                if (s == null || s.trim().length() == 0) {
                    Bootbox.alert("Accession can not be blank");
                    commentCell.clearViewData(object);
                    dataGrid.redrawRow(i);
                    redrawTable();
                } else if (!object.getComment().equals(s)) {
                    GWT.log("Value Changed");
                    object.setComment(s);
                    selectCommentData(object);
                    updateComment();
                }
            }
        });
        commentColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        commentColumn.setSortable(true);
        dataGrid.addColumn(commentColumn, "Comment");
        dataGrid.setColumnWidth(1, "100%");

        ColumnSortEvent.ListHandler<CommentInfo> sortHandler = new ColumnSortEvent.ListHandler<CommentInfo>(dbXrefInfoList);
        dataGrid.addColumnSortHandler(sortHandler);

        sortHandler.setComparator(commentColumn, new Comparator<CommentInfo>() {
            @Override
            public int compare(CommentInfo o1, CommentInfo o2) {
                return o1.getComment().compareTo(o2.getComment());
            }
        });
        // default is ascending
        dataGrid.getColumnSortList().push(commentColumn);
        ColumnSortEvent.fire(dataGrid, dataGrid.getColumnSortList());
    }

    public void updateData(AnnotationInfo annotationInfo) {
        GWT.log("updating annotation info: " + annotationInfo);
        if (annotationInfo == null) {
            return;
        }
        this.internalAnnotationInfo = annotationInfo;
        dbXrefInfoList.clear();
        dbXrefInfoList.addAll(annotationInfo.getCommentList());
        ColumnSortEvent.fire(dataGrid, dataGrid.getColumnSortList());
        GWT.log("List size: " + dbXrefInfoList.size());
        redrawTable();
        setVisible(true);
    }

    public void updateData() {
        updateData(null);
    }

    public void selectCommentData(CommentInfo v) {
        this.internalCommentInfo = v;
        // value
        this.oldComment = this.comment;
        this.comment = this.internalCommentInfo.getComment();

        redrawTable();
        setVisible(true);
    }

    public void updateComment() {
        if (validateTags()) {
            final CommentInfo newCommentInfo = new CommentInfo(this.comment);
            RequestCallback requestCallBack = new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    JSONValue returnValue = JSONParser.parseStrict(response.getText());
                    redrawTable();
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    dataGrid.redraw();
                    Bootbox.alert("Error updating variant info property: " + exception);
                    resetTags();
                    // TODO: reset data
                    redrawTable();
                }
            };
            CommentRestService.updateComment(requestCallBack, this.internalAnnotationInfo, new CommentInfo(this.oldComment), newCommentInfo);
            ;
        } else {
            resetTags();
        }
    }

    private void resetTags() {
        GWT.log("reseting tag");
        this.comment = this.oldComment;
        updateData(this.internalAnnotationInfo);
        redrawTable();
    }

    public void redrawTable() {
        this.dataGrid.redraw();
    }

    @UiHandler("commentInputBox")
    public void valueInputBoxType(KeyUpEvent event) {
        addCommentButton.setEnabled(validateTags());
    }


    private boolean validateTags() {
        collectTags();
        return this.comment != null && !this.comment.isEmpty();
    }

    private void collectTags() {
        this.comment = commentInputBox.getText();
    }



    @UiHandler("addCommentButton")
    public void addCommentButton(ClickEvent ce) {
        final AnnotationInfo internalAnnotationInfo = this.internalAnnotationInfo;
        if (validateTags()) {
            final CommentInfo newCommentInfo = new CommentInfo(this.comment);
            this.commentInputBox.clear();

            RequestCallback requestCallBack = new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    JSONValue returnValue = JSONParser.parseStrict(response.getText());
                    List<CommentInfo> newList = new ArrayList<>(dbXrefInfoList);
                    newList.add(newCommentInfo);
                    internalAnnotationInfo.setCommentList(newList);
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    Bootbox.alert("Error updating variant info property: " + exception);
                    resetTags();
                    // TODO: reset data
                    redrawTable();
                }
            };
            CommentRestService.addComment(requestCallBack, this.internalAnnotationInfo, newCommentInfo);
        }
    }

    @UiHandler("deleteCommentButton")
    public void deleteComment(ClickEvent ce) {

        if (this.internalCommentInfo != null) {
            RequestCallback requestCallBack = new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    JSONValue returnValue = JSONParser.parseStrict(response.getText());
                    deleteCommentButton.setEnabled(false);
                    redrawTable();
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    Bootbox.alert("Error deleting variant info property: " + exception);
                    redrawTable();
                }
            };
            CommentRestService.deleteComment(requestCallBack, this.internalAnnotationInfo, this.internalCommentInfo);
            ;
        }
    }
}