databaseChangeLog = {
	changeSet(author: "cmdcolin (generated)", id: "1454711582784-1") {
        createIndex( indexName: "feature_event_s123", tableName: "feature_event", unique: "true" ) {
            column( name: "unique_name" )
        }
    }
}
