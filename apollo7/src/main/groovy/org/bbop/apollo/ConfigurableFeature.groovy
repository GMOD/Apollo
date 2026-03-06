package org.bbop.apollo

/**
 * Created by ndunn on 7/15/15.
 *
 * Used to customize a feature type
 * TODO: make sure that this goes into generic feature and generic transcipt or will ahve to over-ride
 */
trait ConfigurableFeature {

    abstract void setCvTerm(String cvTerm )
    abstract void setOntologyId(String ontologyId)
    abstract void setAlternateCvTerm(String alternateCvTerm)
    abstract void setClassName(String className)

    abstract String getCvTerm()
    abstract String getOntologyId()
    abstract String getAlternateCvTerm()
    abstract String getClassName()
}