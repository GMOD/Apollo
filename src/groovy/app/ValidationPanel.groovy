package app

import com.vaadin.ui.CustomComponent
import com.vaadin.ui.HorizontalSplitPanel
import com.vaadin.ui.Label
import com.vaadin.ui.VerticalSplitPanel

/**
 * Created by ndunn on 12/3/14.
 */
//@CompileStatic
class ValidationPanel extends VerticalSplitPanel{




    ValidationPanel(){


        setFirstComponent(new Label("stuff"))
        setSecondComponent(new Label("list stuff"))

        setSizeFull();


    }

}
