package org.bbop.apollo;

import org.hibernate.dialect.PostgreSQLDialect;

// BACKWARDS INCOMPATIBILITY: PostgresPlusDialect was removed/renamed in
// Hibernate 5.6+. Using PostgreSQLDialect instead.
// The custom getDropSequenceString() override is no longer needed as
// modern PostgreSQL dialects handle this correctly.
// If compilation fails, simply remove this class and use PostgreSQLDialect directly.
public class ImprovedPostgresDialect extends PostgreSQLDialect {
}
