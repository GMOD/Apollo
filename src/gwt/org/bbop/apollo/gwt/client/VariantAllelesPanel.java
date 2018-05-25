package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.EditTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.*;
import org.bbop.apollo.gwt.client.dto.AllelePropertyInfo;
import org.bbop.apollo.gwt.client.event.AnnotationInfoChangeEvent;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.client.ui.TextBox;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.AlternateAlleleInfo;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.gwtbootstrap3.client.ui.Container;
import org.gwtbootstrap3.client.ui.Pager;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Created by deepak.unni3 on 9/12/16.
 */
public class VariantAllelesPanel extends Composite {

    private AnnotationInfo internalAnnotationInfo = null;
    private AlternateAlleleInfo internalAlterateAlleleInfo = null;
    private String oldBases, oldProvenance;
    private String bases, provenance;
    private Float oldAlleleFrequency;
    private Float alleleFrequency;

    interface VariantAllelePanelUiBinder extends UiBinder<Widget, VariantAllelesPanel> {
    }

    private static VariantAllelePanelUiBinder ourUiBinder = GWT.create(VariantAllelePanelUiBinder.class);


    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<AlternateAlleleInfo> dataGrid = new DataGrid<>(10, tablecss);

    private static ListDataProvider<AlternateAlleleInfo> dataProvider = new ListDataProvider<>();
    private static List<AlternateAlleleInfo> alternateAlleleInfoList = dataProvider.getList();
    private SingleSelectionModel<AlternateAlleleInfo> selectionModel = new SingleSelectionModel<>();
    private Column<AlternateAlleleInfo, String> basesColumn;
    private Column<AlternateAlleleInfo, String> alleleFrequencyColumn;
    private Column<AlternateAlleleInfo, String> provenanceColumn;

//    @UiField(provided = true)
//    DataGrid<AllelePropertyInfo> allelePropertyDataGrid = new DataGrid<>(10, tablecss);
//
//    private static ListDataProvider<AllelePropertyInfo> allelePropertyDataProvider = new ListDataProvider<>();
//    private static List<AllelePropertyInfo> allelePropertyInfoList = allelePropertyDataProvider.getList();
//    private SingleSelectionModel<AllelePropertyInfo> allelePropertySelectionModel = new SingleSelectionModel<>();
//    private Column<AllelePropertyInfo, String> tagColumn;
//    private Column<AllelePropertyInfo, String> valueColumn;

    public VariantAllelesPanel() {
        dataGrid.setWidth("100%");
        initializeTable();
        dataProvider.addDataDisplay(dataGrid);
        dataGrid.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent selectionChangeEvent) {
                if (selectionModel.getSelectedSet().isEmpty()) {
                } else {
                    updateAlleleData(selectionModel.getSelectedObject());
                }
            }
        });

//        allelePropertyDataGrid.setWidth("100%");
//        initializeAllelePropertyTable();
//        allelePropertyDataProvider.addDataDisplay(allelePropertyDataGrid);
//        allelePropertyDataGrid.setSelectionModel(allelePropertySelectionModel);
//        allelePropertySelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
//            @Override
//            public void onSelectionChange(SelectionChangeEvent selectionChangeEvent) {
//                if (allelePropertySelectionModel.getSelectedSet().isEmpty()) {
//                } else {
//                }
//            }
//        });

        initWidget(ourUiBinder.createAndBindUi(this));
    }

    public void initializeTable() {
        TextCell basesCell = new TextCell();
        basesColumn = new Column<AlternateAlleleInfo, String>(basesCell) {
            @Override
            public String getValue(AlternateAlleleInfo object) {
                if (object.getBases() != null) {
                    return object.getBases();
                } else {
                    return "";
                }
            }
        };
        basesColumn.setFieldUpdater(new FieldUpdater<AlternateAlleleInfo, String>() {
            @Override
            public void update(int i, AlternateAlleleInfo alternateAlleleInfo, String newValue) {
                if (!alternateAlleleInfo.getBases().equals(newValue.toUpperCase())) {
                    //GWT.log("update event on bases");
                    //alternateAlleleInfo.setBases(newValue);
                    //updateAlleleData(alternateAlleleInfo);
                    //triggerUpdate();
                }
            }
        });
        basesColumn.setSortable(true);

//        EditTextCell frequencyCell = new EditTextCell();
//        alleleFrequencyColumn = new Column<AlternateAlleleInfo, String>(frequencyCell) {
//            @Override
//            public String getValue(AlternateAlleleInfo object) {
//                if (object.getAlleleFrequency() != null) {
//                    return String.valueOf(object.getAlleleFrequency());
//                } else {
//                    return "";
//                }
//            }
//        };
//        alleleFrequencyColumn.setFieldUpdater(new FieldUpdater<AlternateAlleleInfo, String>() {
//            @Override
//            public void update(int i, AlternateAlleleInfo alternateAlleleInfo, String newValue) {
//                GWT.log(alternateAlleleInfo.getAlleleFrequencyAsString() + " vs " + newValue);
//                if (!String.valueOf(alternateAlleleInfo.getAlleleFrequencyAsString()).equals(newValue)) {
//                    GWT.log("update event on allele frequency");
//                    try {
//                        Float newFrequency = Float.parseFloat(newValue);
//                        if (newFrequency >= 0.0 && newFrequency <= 1.0) {
//                            alternateAlleleInfo.setAlleleFrequency(newFrequency);
//                            updateAlleleData(alternateAlleleInfo);
//                            triggerUpdate();
//                        } else {
//                            Bootbox.alert("Allele Frequency must be within the range 0.0 - 1.0");
//                        }
//                    } catch (NumberFormatException e) {
//                        Bootbox.alert("Allele Frequency must be a number and within the range 0.0 - 1.0");
//                    }
//                }
//
//            }
//        });
//        alleleFrequencyColumn.setSortable(true);
//
//        EditTextCell provenanceCell = new EditTextCell();
//        provenanceColumn = new Column<AlternateAlleleInfo, String>(provenanceCell) {
//            @Override
//            public String getValue(AlternateAlleleInfo object) {
//                if (object.getProvenance() != null) {
//                    return object.getProvenance();
//                } else {
//                    return "";
//                }
//            }
//        };
//        provenanceColumn.setFieldUpdater(new FieldUpdater<AlternateAlleleInfo, String>() {
//            @Override
//            public void update(int i, AlternateAlleleInfo alternateAlleleInfo, String newValue) {
//                if (!alternateAlleleInfo.getProvenance().equals(newValue)) {
//                    GWT.log("update event on provenance");
//                    alternateAlleleInfo.setProvenance(newValue);
//                    updateAlleleData(alternateAlleleInfo);
//                    triggerUpdate();
//                }
//            }
//        });
//        provenanceColumn.setSortable(true);

        dataGrid.addColumn(basesColumn, "Bases");
//        dataGrid.addColumn(alleleFrequencyColumn, "AF");
//        dataGrid.addColumn(provenanceColumn, "Provenance");

        ColumnSortEvent.ListHandler<AlternateAlleleInfo> sortHandler = new ColumnSortEvent.ListHandler<AlternateAlleleInfo>(alternateAlleleInfoList);
        dataGrid.addColumnSortHandler(sortHandler);

        sortHandler.setComparator(basesColumn, new Comparator<AlternateAlleleInfo>() {
            @Override
            public int compare(AlternateAlleleInfo o1, AlternateAlleleInfo o2) {
                return o1.getBases().compareTo(o2.getBases());
            }
        });

//        sortHandler.setComparator(alleleFrequencyColumn, new Comparator<AlternateAlleleInfo>() {
//            @Override
//            public int compare(AlternateAlleleInfo o1, AlternateAlleleInfo o2) {
//                return o1.getAlleleFrequency().compareTo(o2.getAlleleFrequency());
//            }
//        });
//
//        sortHandler.setComparator(provenanceColumn, new Comparator<AlternateAlleleInfo>() {
//            @Override
//            public int compare(AlternateAlleleInfo o1, AlternateAlleleInfo o2) {
//                return o1.getProvenance().compareTo(o2.getProvenance());
//            }
//        });
    }

//    public void initializeAllelePropertyTable() {
//        EditTextCell tagCell = new EditTextCell();
//        tagColumn = new Column<AllelePropertyInfo, String>(tagCell) {
//            @Override
//            public String getValue(AllelePropertyInfo object) {
//                return object.getTag();
//            }
//        };
//        tagColumn.setFieldUpdater(new FieldUpdater<AllelePropertyInfo, String>() {
//            @Override
//            public void update(int i, AllelePropertyInfo object, String s) {
//                String tag = object.getTag();
//                if (!tag.equals(s)) {
//                    GWT.log("Tag changed");
//                }
//            }
//        });
//        tagColumn.setSortable(true);
//
//        EditTextCell valueCell = new EditTextCell();
//        valueColumn = new Column<AllelePropertyInfo, String>(valueCell) {
//            @Override
//            public String getValue(AllelePropertyInfo object) {
//                return object.getValue();
//            }
//        };
//        valueColumn.setFieldUpdater(new FieldUpdater<AllelePropertyInfo, String>() {
//            @Override
//            public void update(int i, AllelePropertyInfo object, String s) {
//                String value = object.getValue();
//                if (!value.equals(s)) {
//                    GWT.log("Value changed");
//                }
//            }
//        });
//        valueColumn.setSortable(true);
//
//        allelePropertyDataGrid.addColumn(tagColumn, "Tag");
//        allelePropertyDataGrid.addColumn(valueColumn, "Value");
//
//        ColumnSortEvent.ListHandler<AllelePropertyInfo> sortHandler = new ColumnSortEvent.ListHandler<>(allelePropertyInfoList);
//        allelePropertyDataGrid.addColumnSortHandler(sortHandler);
//
//        sortHandler.setComparator(tagColumn, new Comparator<AllelePropertyInfo>() {
//            @Override
//            public int compare(AllelePropertyInfo o1, AllelePropertyInfo o2) {
//                return o1.getTag().compareTo(o2.getTag());
//            }
//        });
//
//        sortHandler.setComparator(valueColumn, new Comparator<AllelePropertyInfo>() {
//            @Override
//            public int compare(AllelePropertyInfo o1, AllelePropertyInfo o2) {
//                return o1.getValue().compareTo(o2.getValue());
//            }
//        });
//    }

    public void updateData(AnnotationInfo annotationInfo) {
        if (annotationInfo == null) {
            return;
        }
        this.internalAnnotationInfo = annotationInfo;
        alternateAlleleInfoList.clear();
        //allelePropertyInfoList.clear();
        alternateAlleleInfoList.addAll(annotationInfo.getAlternateAlleles());
        //ArrayList<AllelePropertyInfo> allelePropertyInfoArray = alternateAlleleInfo1.getAlleleInfo();
        //allelePropertyInfoList.addAll(allelePropertyInfoArray);

        if (alternateAlleleInfoList.size() > 0) {
            updateAlleleData(alternateAlleleInfoList.get(0));
        }
        redrawTable();
    }

    public void updateAlleleData(AlternateAlleleInfo a) {
//        this.internalAlterateAlleleInfo = a;
//        // bases
//        this.oldBases = this.bases;
//        this.bases = this.internalAlterateAlleleInfo.getBases();
//
//        // allele frequency
//        this.oldAlleleFrequency = this.alleleFrequency;
//        this.alleleFrequency = this.internalAlterateAlleleInfo.getAlleleFrequency();
//
//        // provenance
//        this.oldProvenance = this.provenance;
//        this.provenance = this.internalAlterateAlleleInfo.getProvenance();

//        redrawTable();
//        setVisible(true);

    }

    public void triggerUpdate() {
        GWT.log("@triggerUpdate");
        GWT.log("old vs new bases: " + this.oldBases + "|" + this.bases);
        GWT.log("old vs new AF: " + this.oldAlleleFrequency + "|" + this.alleleFrequency);
        GWT.log("old vs new provenance: " + this.oldProvenance + "|" + this.provenance);

        boolean baseValidated = false;
        boolean alleleFrequencyValidated = false;
        boolean provenanceValidated = false;

        if (this.bases != null) {
            if (VariantDetailPanel.isValidDNA(this.bases)) {
                baseValidated = true;
            } else {
                Bootbox.alert("Bases should only contain A, T, C, G or N");
                baseValidated = false;
            }
        }

        if (alleleFrequency != null) {
            if (alleleFrequency >= 0.0 && alleleFrequency <= 1.0) {
                alleleFrequencyValidated = true;
            } else {
                Bootbox.alert("Allele Frequency for an allele must be within the range 0.0 - 1.0");
                alleleFrequencyValidated = false;
            }
        } else {
            alleleFrequencyValidated = true;
        }

        if (alleleFrequency != null && (provenance == null || provenance.isEmpty())) {
            Bootbox.alert("Provenance cannot be empty when Allele Frequency is provided");
            provenanceValidated = false;
        } else {
            provenanceValidated = true;
        }

        if (baseValidated && alleleFrequencyValidated && provenanceValidated) {
            String url = Annotator.getRootUrl() + "annotator/updateAlternateAlleles";
            RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(url));
            builder.setHeader("Content-type", "application/x-www-form-urlencoded");
            StringBuilder sb = new StringBuilder();

            JSONArray featuresArray = new JSONArray();
            JSONObject featuresObject = new JSONObject();
            String featureUniqueName = this.internalAnnotationInfo.getUniqueName();
            featuresObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));

            JSONArray oldAlternateAllelesJsonArray = new JSONArray();
            JSONObject oldAlternateAllelesJsonObject = new JSONObject();
            oldAlternateAllelesJsonObject.put(FeatureStringEnum.BASES.getValue(), new JSONString(this.oldBases));
            if (oldAlleleFrequency != null)
                oldAlternateAllelesJsonObject.put(FeatureStringEnum.ALLELE_FREQUENCY.getValue(), new JSONString(String.valueOf(this.oldAlleleFrequency)));
            if (provenance != null)
                oldAlternateAllelesJsonObject.put(FeatureStringEnum.PROVENANCE.getValue(), new JSONString(String.valueOf(this.oldProvenance)));
            oldAlternateAllelesJsonArray.set(0, oldAlternateAllelesJsonObject);
            featuresObject.put(FeatureStringEnum.OLD_ALTERNATE_ALLELES.getValue(), oldAlternateAllelesJsonArray);

            JSONArray newAlternateAllelesJsonArray = new JSONArray();
            JSONObject newAlternateAllelesJsonObject = new JSONObject();
            newAlternateAllelesJsonObject.put(FeatureStringEnum.BASES.getValue(), new JSONString(this.bases));
            if (alleleFrequency != null)
                newAlternateAllelesJsonObject.put(FeatureStringEnum.ALLELE_FREQUENCY.getValue(), new JSONString(String.valueOf(this.alleleFrequency)));
            if (provenance != null)
                newAlternateAllelesJsonObject.put(FeatureStringEnum.PROVENANCE.getValue(), new JSONString(this.provenance));
            newAlternateAllelesJsonArray.set(0, newAlternateAllelesJsonObject);
            featuresObject.put(FeatureStringEnum.NEW_ALTERNATE_ALLELES.getValue(), newAlternateAllelesJsonArray);

            featuresArray.set(0, featuresObject);

            JSONObject requestObject = new JSONObject();
            requestObject.put("operation", new JSONString("update_alternate_alleles"));
            requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(this.internalAnnotationInfo.getSequence()));
            requestObject.put(FeatureStringEnum.CLIENT_TOKEN.getValue(), new JSONString(Annotator.getClientToken()));
            requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
            sb.append("data=" + requestObject.toString());
            final AnnotationInfo updatedInfo = this.internalAnnotationInfo;
            builder.setRequestData(sb.toString());
            RequestCallback requestCallback = new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    JSONValue returnValue = JSONParser.parseStrict(response.getText());
                    Annotator.eventBus.fireEvent(new AnnotationInfoChangeEvent(updatedInfo, AnnotationInfoChangeEvent.Action.UPDATE));
                    redrawTable();
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    Bootbox.alert("Error updating alternate allele: " + exception);
                    redrawTable();
                }
            };

            try {
                builder.setCallback(requestCallback);
                builder.send();
                // TODO: dataGrid.setLoadingIndicator()
                //setLoadingIndicator(dataGrid);
            } catch (RequestException e) {
                Bootbox.alert("RequestException: " + e.getMessage());
            }
        }
    }

    private void setLoadingIndicator(DataGrid grid) {
        VerticalPanel vp = new VerticalPanel();
        AbsolutePanel ap = new AbsolutePanel();
        HorizontalPanel hp = new HorizontalPanel();
        AbsolutePanel ap1 = new AbsolutePanel();
        ap1.setWidth("30px");
        hp.add(ap1);
        ap.setHeight("50px");
        vp.add(ap);
        vp.add(new Label("Loading..."));
        vp.add(hp);
        vp.setSpacing(10);

        grid.setLoadingIndicator(vp);
    }

    public void redrawTable() {
        this.dataGrid.redraw();
    }
}