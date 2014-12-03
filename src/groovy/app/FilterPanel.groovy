package app

import com.vaadin.ui.CustomComponent
import com.vaadin.ui.TabSheet


/**
 * Created by ndunn on 12/3/14.
 */
class FilterPanel extends CustomComponent{

    TabSheet tabSheet = new TabSheet()
//
    BrowserPanel browserPanel = new BrowserPanel()
    SearchPanel searchPanel = new SearchPanel()
    ValidationPanel validationPanel = new ValidationPanel()
//
//
//
    FilterPanel() {
//        tabSheet.setSizeFull()
        tabSheet.setHeight("100%")

//        tabSheet.setHeight("400px")
//        browserPanel.setSizeFull()
        tabSheet.addTab(searchPanel,"Search")
        tabSheet.addTab(browserPanel,"Browse B")
        tabSheet.addTab(validationPanel,"Validate")

        setCompositionRoot(tabSheet)
//        setCompositionRoot(tabSheet);
    }


}
