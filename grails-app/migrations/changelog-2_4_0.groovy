databaseChangeLog = {

    changeSet(author: "nathandunn", id: "1459788030178-1") {
        preConditions(onFail: 'MARK_RAN', onError: "HALT") {
            not {
                tableExists(tableName: "application_preference")
            }
        }
        createTable(tableName: "application_preference") {
            column(autoIncrement: "true", name: "id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "analysisPK")
            }
            column(name: "version", type: "bigint") {
                constraints(nullable: "false")
            }
            column(name: "name", type: "string") {
                constraints(nullable: "false",unique:"true")
            }
            column(name: "value", type: "string") {
                constraints(nullable: "true")
            }
        }

    }

    changeSet(author: "nathandunn", id: "1459788030178-2") {
        dropNotNullConstraint(tableName: "variant_info", columnName:"value", columnDataType:"text")
    }

}