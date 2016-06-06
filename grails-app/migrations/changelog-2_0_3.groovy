databaseChangeLog = {

    changeSet(author: "deepak.unni3", id: "1459788030174-1") {
        modifyDataType(columnName: "value", newDataType: "text", tableName: "feature_property")
    }

    changeSet(author: "nathandunn", id: "1459788030174-2") {
        preConditions(onFail: 'MARK_RAN', onError: "HALT") {
            not {
                columnExists(columnName: "client_token", tableName: "preference")
			}
        } 
        addColumn(tableName: "preference"){
           column(name:"client_token",type:"varchar(255)",value:"GENERATED DEFAULT TOKEN")
        }
        addColumn(tableName: "preference"){
           column(name:"date_created",type:"timestamp")
        }
        addColumn(tableName: "preference"){
           column(name:"last_updated",type:"timestamp")
        }
        addNotNullConstraint(tableName: "preference",columnName:"client_token", columnDataType:"varchar(255)")
    }
}
