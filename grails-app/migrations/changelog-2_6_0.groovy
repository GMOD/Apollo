databaseChangeLog = {

    changeSet(author: "nathandunn", id: "1459788030180-1") {
        preConditions(onFail: 'MARK_RAN', onError: "HALT") {
            columnExists(columnName: "type_id", tableName: "synonym")
            columnExists(columnName: "synonymsgml", tableName: "synonym")
        }
        dropColumn(tableName: "synonym",columnName: "type_id", type:"varchar(255)")
        dropColumn(tableName: "synonym",columnName: "synonymsgml", type:"varchar(255)")
    }

    changeSet(author: "nathandunn", id: "1459788030180-2") {
        dropNotNullConstraint(tableName: "feature_synonym",columnName:"publication_id", type:"bigint")
        dropNotNullConstraint(tableName: "feature_synonym",columnName:"is_internal", type:"bigint")
        dropNotNullConstraint(tableName: "feature_synonym",columnName:"is_current", type:"bigint")
    }
}