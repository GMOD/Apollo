databaseChangeLog = {

    changeSet(author: "deepak.unni3", id: "1459788030176-1") {

        dropNotNullConstraint(tableName: "sequence", columnName:"seq_chunk_size", columnDataType:"integer")
    }
}