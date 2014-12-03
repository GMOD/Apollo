package app

import com.vaadin.server.Sizeable
import com.vaadin.ui.CustomComponent
import com.vaadin.ui.Label
import com.vaadin.ui.Panel
import com.vaadin.ui.VerticalSplitPanel

/**
 * Created by ndunn on 12/3/14.
 */
class SearchPanel extends VerticalSplitPanel{



    SearchPanel(){
        super()

//        Panel mainPanel = new Panel()
//        VerticalSplitPanel verticalSplitPanel = new VerticalSplitPanel()
//        verticalSplitPanel.setSizeFull()

        Label labelA = new Label("asdf")
        Label labelB = new Label("bbbb")
        setFirstComponent(labelA)
        setSecondComponent(labelB)
//        setHeight("200px")
        setSplitPosition(100,Sizeable.Unit.PIXELS)
//        verticalSplitPanel.setSizeFull()


//        verticalSplitPanel.setHeight(800,Sizeable.Unit.PIXELS)
//        verticalSplitPanel.setHeight("100%")

//        mainPanel.setContent(verticalSplitPanel)

//        mainPanel.setWidth("600px");
//        mainPanel.setHeight("200px");
//        mainPanel.setSizeFull()
//        mainPanel.setHeight("100%");

//        mainPanel.setContent(labelA)
//        setCompositionRoot(mainPanel)
//        setSizeFull()

    }
}
