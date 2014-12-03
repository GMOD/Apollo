package app

import com.vaadin.ui.CustomComponent
import com.vaadin.ui.Label
import com.vaadin.ui.VerticalSplitPanel

/**
 * Created by ndunn on 12/3/14.
 */
//@CompileStatic
class BrowserPanel extends CustomComponent{




    BrowserPanel(){

        VerticalSplitPanel verticalSplitPanel = new VerticalSplitPanel()
        verticalSplitPanel.setSizeFull()

        verticalSplitPanel.setFirstComponent(new Label("stuff"))
        verticalSplitPanel.setSecondComponent(new Label("list stuff"))

        setCompositionRoot(verticalSplitPanel)

    }

}
