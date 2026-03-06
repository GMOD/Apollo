databaseChangeLog = {
	changeSet(author: "cmdcolin (generated)", id: "1454711582784-1") {
        preConditions(onFail: 'MARK_RAN', onError: "HALT") {
            not {
                indexExists(indexName:"feature_uniqueName")
            }
        }
        createIndex(indexName: "feature_uniqueName", tableName: "feature_event", unique: "false") {
            column(name: "unique_name")
        }
    }
}
