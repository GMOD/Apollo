package org.bbop.apollo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;
import org.bbop.apollo.gwt.client.demo.DataGenerator;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.constants.IconType;

/**
 * Created by ndunn on 12/18/14.
 */
public class MainPanel extends Composite {


    interface MainPanelUiBinder extends UiBinder<Widget, MainPanel> {
    }

    private static MainPanelUiBinder ourUiBinder = GWT.create(MainPanelUiBinder.class);

    private boolean toggleOpen = true;
    private String rootUrl;

    @UiField
    Button dockOpenClose;
    @UiField
    Frame frame;
    @UiField
    AnnotatorPanel annotatorPanel;
    @UiField
    TrackPanel trackPanel;
    @UiField
    SequencePanel sequencePanel;
    @UiField
    OrganismPanel organismPanel;
    @UiField
    UserPanel userPanel;
    @UiField
    UserGroupPanel userGroupPanel;
    @UiField
    DockLayoutPanel eastDockPanel;
    @UiField
    SplitLayoutPanel mainSplitPanel;
    @UiField
    TabLayoutPanel detailTabs;
    @UiField
    ListBox organismList;
    @UiField
    ListBox sequenceList;

    public MainPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));

//        frame.setUrl();
//        AnnotatorWidget3 annotatorWidget3 = new AnnotatorWidget3();
        Dictionary dictionary = Dictionary.getDictionary("Options");
        rootUrl = dictionary.get("rootUrl");
        frame.setUrl(rootUrl + "/jbrowse/?loc=Group1.3%3A14865..15198&tracks=DNA%2CAnnotations%2COfficial%20Gene%20Set%20v3.2%2CGeneID%2CCflo_OGSv3.3&highlight=");

        DataGenerator.populateOrganismList(organismList);
        DataGenerator.populateSequenceList(sequenceList);
    }


    @UiHandler("dockOpenClose")
    void handleClick(ClickEvent event) {
        toggleOpen();
    }

    @UiHandler("detailTabs")
    public void onSelection(SelectionEvent<Integer> event) {
        switch (event.getSelectedItem()) {
            case 0:
                trackPanel.dataGrid.redraw();
                break;
            case 1:
                trackPanel.dataGrid.redraw();
            case 2:
                sequencePanel.dataGrid.redraw();
                break;
            case 3:
                organismPanel.organismTable.redraw();
                break;
            case 4:
                userPanel.dataGrid.redraw();
                break;
            case 5:
                userGroupPanel.dataGrid.redraw();
                break;
            default:
                break;
        }

    }

    private void toggleOpen() {
        if (mainSplitPanel.getWidgetSize(eastDockPanel) < 100) {
            toggleOpen = false;
        }

        if (toggleOpen) {
            mainSplitPanel.setWidgetSize(eastDockPanel, 20);
            dockOpenClose.setIcon(IconType.CARET_LEFT);
        } else {
            mainSplitPanel.setWidgetSize(eastDockPanel, 500);
            dockOpenClose.setIcon(IconType.CARET_RIGHT);
        }

        mainSplitPanel.animate(400);

        toggleOpen = !toggleOpen;
    }


    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }
}