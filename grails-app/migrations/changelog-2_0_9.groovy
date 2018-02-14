databaseChangeLog = {

    changeSet(author: "nathandunn", id: "1459788030177-1") {
        preConditions(onFail: 'MARK_RAN', onError: "HALT") {
            not {
                columnExists(columnName: "rank", tableName: "role")
            }
        }

        addColumn(tableName: "role"){
            column(name:"rank",type:"BIGINT"){
                constraints(nullable: "true")
            }
        }
        addColumn(tableName: "user_group"){
            column(name:"metadata",type:"CLOB"){
                constraints(nullable: "true")
            }
        }

    }
}