package org.bbop.apollo.web.data;

import org.bbop.apollo.web.config.ServerConfiguration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by NathanDunn on 10/8/14.
 */
public class DataAdapterGroupView implements Serializable{

    private Collection<DataAdapterView> dataAdapters;
    private boolean isGroup;
    private String key;
    private String permission;

    public DataAdapterGroupView(){}

    public DataAdapterGroupView(ServerConfiguration.DataAdapterGroupConfiguration groupConf) {
        this.isGroup = groupConf.isGroup();
        this.key = groupConf.getKey();
        this.permission = groupConf.getPermission();

        this.dataAdapters = new ArrayList<>();
        for(ServerConfiguration.DataAdapterConfiguration dataAdapterConfiguration : groupConf.getDataAdapters()){
            dataAdapters.add(new DataAdapterView(dataAdapterConfiguration));
        }
    }

    public Collection<DataAdapterView> getDataAdapters() {
        return dataAdapters;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public String getKey() {
        return key;
    }

    public String getPermission() {
        return permission;
    }

    public void addDataAdapter(DataAdapterView dataAdapter) {
        dataAdapters.add(dataAdapter);
    }

    public void setDataAdapters(Collection<DataAdapterView> dataAdapters) {
        this.dataAdapters = dataAdapters;
    }

    public void setGroup(boolean isGroup) {
        this.isGroup = isGroup;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }
}
