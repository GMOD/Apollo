package org.bbop.apollo;

import org.hibernate.dialect.H2Dialect;

// BACKWARDS INCOMPATIBILITY: Hibernate 5.6+ (used in Grails 7) changed the
// Dialect API. getDropSequenceString() and dropConstraints() may no longer
// be overridable in the same way. H2Dialect in modern Hibernate already
// handles "IF EXISTS" for sequences. This class may be unnecessary.
// If compilation fails, simply remove this class and use H2Dialect directly.
public class ImprovedH2Dialect extends H2Dialect {
}
