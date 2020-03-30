databaseChangeLog = {

    changeSet(author: "nathandunn", id: "1459788030180-1") {
        preConditions(onFail: 'MARK_RAN', onError: "HALT") {
            columnExists(columnName: "type_id", tableName: "synonym")
            columnExists(columnName: "synonymsgml", tableName: "synonym")
        }
        dropColumn(tableName: "synonym",columnName: "type_id")
        dropColumn(tableName: "synonym",columnName: "synonymsgml")
    }

    changeSet(author: "nathandunn", id: "1459788030180-2") {
        dropNotNullConstraint(tableName: "feature_synonym",columnName:"publication_id", columnDataType:"bigint")
        dropNotNullConstraint(tableName: "feature_synonym",columnName:"is_internal", columnDataType:"bigint")
        dropNotNullConstraint(tableName: "feature_synonym",columnName:"is_current", columnDataType:"bigint")
    }

    changeSet(author: "nathandunn", id: "1459788030180-3") {
        preConditions(onFail: 'MARK_RAN', onError: "HALT") {
            not {
                indexExists(indexName:"feature_event_name")
            }
        }
        createIndex(indexName: "feature_event_name", tableName: "feature_event", unique: "false") {
            column(name: "name")
        }
    }

    changeSet(author: "nathandunn", id: "1459788030180-4") {
        preConditions(onFail: 'MARK_RAN', onError: "HALT") {
            not {
                indexExists(indexName:"feature_name")
            }
        }
        createIndex(indexName: "feature_name", tableName: "feature", unique: "false") {
            column(name: "name")
        }
    }
}