package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.shared.PermissionEnum;

/**
 * Created by ndunn on 3/24/15.
 */
abstract class OrganismPermissionInfo implements HasJSON{

    String organismName;
    Long id;
    Boolean admin = false ;
    Boolean write= false ;
    Boolean export= false ;
    Boolean read= false ;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrganismName() {
        return organismName;
    }

    public void setOrganismName(String organismName) {
        this.organismName = organismName;
    }

    public Boolean isAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public Boolean isWrite() {
        return write;
    }

    public void setWrite(Boolean write) {
        this.write = write;
    }

    public Boolean isExport() {
        return export;
    }

    public void setExport(Boolean export) {
        this.export = export;
    }

    public Boolean isRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public PermissionEnum getHighestPermission() {
        if(admin) return PermissionEnum.ADMINISTRATE;
        if(write) return PermissionEnum.WRITE;
        if(export) return PermissionEnum.EXPORT;
        if(read) return PermissionEnum.READ ;
        return PermissionEnum.NONE;
    }
}
