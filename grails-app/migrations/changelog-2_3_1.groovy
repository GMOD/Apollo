databaseChangeLog = {

    changeSet(author: "nathandunn", id: "1459788030178-1") {

        preConditions(onFail: 'MARK_RAN', onError: "HALT") {
            not {
                columnExists(columnName: "obsolete", tableName: "organism")
            }
        }
        addColumn(tableName: "organism"){
            column(name:"obsolete",type:"BOOLEAN",defaultValueBoolean:false){
                constraints(nullable:"true")
            }
        }
    }
}