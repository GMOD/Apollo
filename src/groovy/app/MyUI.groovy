package app

import com.vaadin.data.Property
import com.vaadin.server.Sizeable
import com.vaadin.ui.HorizontalSplitPanel
import com.vaadin.ui.UI
import com.vaadin.ui.VerticalLayout
import com.vaadin.server.VaadinRequest
import com.vaadin.ui.Label

/**
 *
 *
 * @author
 */
class Annotator extends UI {

    @Override
    protected void init(VaadinRequest vaadinRequest) {

        HorizontalSplitPanel mainLayout = new HorizontalSplitPanel();
        mainLayout.setSizeFull()

        HorizontalSplitPanel appLayout = new HorizontalSplitPanel();
        appLayout.setSizeFull()

        Label detailPanel =  new Label("Details")
        FilterPanel filterPanel = new FilterPanel()
        filterPanel.setSizeFull()
        appLayout.setFirstComponent(filterPanel)
        appLayout.setSecondComponent(detailPanel)

        Label jbrowseComponent = new Label("JBrowse")


        mainLayout.setFirstComponent(appLayout)
        mainLayout.setSecondComponent(jbrowseComponent)

		setContent(mainLayout)
    }


}
