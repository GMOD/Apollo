package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.bbop.apollo.gwt.client.demo.DataGenerator;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.eclipse.jdt.internal.compiler.classfmt.AnnotationInfo;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;

/**
 * Created by ndunn on 12/17/14.
 */
public class AnnotatorPanel extends Composite {
    interface AnnotatorPanelUiBinder extends UiBinder<com.google.gwt.user.client.ui.Widget, AnnotatorPanel> {
    }

    private static AnnotatorPanelUiBinder ourUiBinder = GWT.create(AnnotatorPanelUiBinder.class);

    @UiField
    TextBox nameSearchBox;
    @UiField
    ListBox sequenceList;
    @UiField
    CheckBox cdsFilter;
    @UiField
    CheckBox stopCodonFilter;


    Tree.Resources tablecss = GWT.create(Tree.Resources.class);
    //    @UiField(provided=true) DataGrid<> dataGrid = new DataGrid<SequenceInfo>( 10, tablecss );
    @UiField(provided = true)
    Tree features = new Tree(tablecss);

    //    @UiField HTML annotationName;
    @UiField
    TextBox annotationName;
    //    @UiField HTML annotationDescription;
    @UiField
    TextBox annotationDescription;
    @UiField
    ListBox typeList;

//    TreeItem selectedItem ;

    public AnnotatorPanel() {
//        initWidget(ourUiBinder.createAndBindUi(this));
        Widget rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);

//        stopCodonFilter.setValue(true);


        loadAnnotations();

        features.setAnimationEnabled(true);


        features.addSelectionHandler(new SelectionHandler<TreeItem>() {
            @Override
            public void onSelection(SelectionEvent<TreeItem> event) {
                GWT.log("selected a tree item " + event.getSelectedItem().getText());
            }
        });
        annotationName.setText("sox9a-000-00-0");

        DataGenerator.populateSequenceList(sequenceList);
        DataGenerator.populateTypeList(typeList);
    }

    public void loadAnnotations() {

        features.clear();

        String url = "/apollo/annotator/findAnnotationsForSequence";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        builder.setHeader("Content-type", "application/x-www-form-urlencoded");
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
//                GWT.log("success returned");
                JSONValue returnValue = JSONParser.parseStrict(response.getText());
//                GWT.log("JOSN value: "+returnValue.toString());
                JSONArray array = returnValue.isObject().get("features").isArray();
//                JSONArray array = returnValue.isArray();
//                GWT.log("#  of genes: " + array.size());

//                Window.alert("array size: "+array.size());

                for (int i = 0; i < array.size(); i++) {

                    JSONObject object = array.get(i).isObject();

                    TreeItem treeItem = processFeatureEntry(object);
                    features.addItem(treeItem);

                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Window.alert("Error loading organisms");
            }
        };
        try {
            builder.setCallback(requestCallback);
            builder.send();
        } catch (RequestException e) {
            // Couldn't connect to server
            Window.alert(e.getMessage());
        }


//        features.addItem(DataGenerator.generateTreeItem("sox9a"));
//        features.addItem(DataGenerator.generateTreeItem("sox9b"));
//        features.addItem(DataGenerator.generateTreeItem("pax6a"));
//        features.addItem(DataGenerator.generateTreeItem("pax6b"));

    }

    private TreeItem processFeatureEntry(JSONObject object) {
        TreeItem treeItem = new TreeItem();

//        GWT.log("getting object: " + object);

        String featureName = object.get("name").isString().stringValue();
        String featureType = object.get("type").isObject().get("name").isString().stringValue();
        int lastFeature = featureType.lastIndexOf(".");
        featureType = featureType.substring(lastFeature + 1);
        HTML html = new HTML(featureName + " <div class='label label-success'>" + featureType + "</div>");
//                    TreeItem treeItem = new TreeItem();
        treeItem.setHTML(html.getHTML());

        if(object.get("children")!=null){
            JSONArray childArray = object.get("children").isArray();
            for (int i = 0; childArray!=null && i < childArray.size(); i++) {
                JSONObject childObject = childArray.get(i).isObject();
//                GWT.log(childObject.toString());
                treeItem.addItem(processFeatureEntry(childObject));
//            GWT.log(relationshipObject.toString());
//            if (relationshipObject.get("childFeature") != null) {
//                JSONObject childObject = relationshipObject.get("childFeature").isObject();
//            }
            }
        }

        return treeItem;
    }
}