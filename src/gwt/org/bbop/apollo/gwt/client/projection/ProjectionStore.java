package org.bbop.apollo.gwt.client.projection;

import org.bbop.apollo.gwt.shared.projection.DiscontinuousProjection;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nathandunn on 2/16/16.
 */
public class ProjectionStore {

//    private Storage preferenceStore = Storage.getLocalStorageIfSupported();

    private Map<String, DiscontinuousProjection> projectionMap = new HashMap<>();

    public DiscontinuousProjection getProjection(String refSeqLabel) {
        return projectionMap.get(refSeqLabel);
    }

    public void storeProjection(String refSeqLabel, DiscontinuousProjection discontinuousProjection) {
        projectionMap.put(refSeqLabel,discontinuousProjection);
    }
}
