package org.bbop.apollo.gwt.client.resources;

import com.google.gwt.resources.client.ClientBundle;
import org.gwtbootstrap3.client.ui.gwt.DataGrid;

/**
 * Created by ndunn on 12/19/14.
 */
public class TableResources implements  ClientBundle{
    // TableCss cell table
    public interface TableCss extends DataGrid.Resources
    {
        @ClientBundle.Source({DataGrid.Style.DEFAULT_CSS,
                "org/bbop/apollo/gwt/client/resources/Table.css"})
        DataGridStyle dataGridStyle();
        interface DataGridStyle extends DataGrid.Style {}
    }
}
