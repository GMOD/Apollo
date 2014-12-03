package app

import com.vaadin.ui.CustomComponent
import com.vaadin.ui.HorizontalSplitPanel
import com.vaadin.ui.Label
import com.vaadin.ui.VerticalSplitPanel

/**
 * Created by ndunn on 12/3/14.
 */
//@CompileStatic
class ValidationPanel extends CustomComponent{




    ValidationPanel(){

        HorizontalSplitPanel verticalSplitPanel = new HorizontalSplitPanel( )

        verticalSplitPanel.setFirstComponent(new Label("stuff"))
        verticalSplitPanel.setSecondComponent(new Label("list stuff"))

        verticalSplitPanel.setSizeFull()

        setCompositionRoot(verticalSplitPanel)
        setSizeFull();


    }

}
