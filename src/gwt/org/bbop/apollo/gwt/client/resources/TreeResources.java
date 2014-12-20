package org.bbop.apollo.gwt.client.resources;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.user.cellview.client.CellTree;

public interface TreeResources extends CellTree.Resources {
    @ClientBundle.Source("org/bbop/apollo/gwt/client/resources/Tree.css")
    public CellTree.Style cellTreeStyle();
}
