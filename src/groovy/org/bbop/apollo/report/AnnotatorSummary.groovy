package org.bbop.apollo.report

import org.bbop.apollo.User

/**
 * Created by nathandunn on 7/19/15.
 */
class AnnotatorSummary extends OrganismSummary{
    User annotator
    List<OrganismPermissionSummary> userOrganismPermissionList

    String getUsername(){annotator.username}
    String getFirstName(){annotator.firstName}
    String getLastName(){annotator.lastName}
}
