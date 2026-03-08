package org.bbop.apollo.gwt.client.event;

import com.google.gwt.event.shared.GwtEvent;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.dto.SequenceInfo;

import java.util.List;

/**
 * Created by ndunn on 1/19/15.
 */
public class ExportEvent extends GwtEvent<ExportEventHandler> {

    public static Type<ExportEventHandler> TYPE = new Type<ExportEventHandler>();

    public enum Action {
        EXPORT_READY,
        EXPORT_FINISHED,
    }

    public enum Flavor{
        GFF3,
        FASTA,
        CHADO,
    }

    private Action thisAction;
    private Flavor thisFlavor;
    private OrganismInfo organismInfo ;
    private List<SequenceInfo> sequenceInfoList ;

    @Override
    public Type<ExportEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ExportEventHandler handler) {
        handler.onExport(this);
    }


    public ExportEvent(Action action,Flavor flavor,OrganismInfo organismInfo,List<SequenceInfo> sequenceInfoList) {
        this.thisAction = action;
        this.thisFlavor = flavor ;
        this.organismInfo = organismInfo ;
        this.sequenceInfoList = sequenceInfoList ;
    }
}
