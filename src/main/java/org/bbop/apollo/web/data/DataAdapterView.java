package org.bbop.apollo.web.data;

import org.bbop.apollo.web.config.ServerConfiguration;

import java.io.Serializable;

/**
* Created by NathanDunn on 10/8/14.
*/
public class DataAdapterView implements Serializable{
    private String key;
    private String className;
    private String permission;
    private String configFileName;
    private String options;

    public DataAdapterView(){}


    public DataAdapterView(ServerConfiguration.DataAdapterConfiguration dataAdapterConfiguration) {
        this.key = dataAdapterConfiguration.getKey();
        this.className = dataAdapterConfiguration.getClassName();
        this.permission = dataAdapterConfiguration.getPermission();
        this.configFileName = dataAdapterConfiguration.getConfigFileName();
        this.options = dataAdapterConfiguration.getOptions();
    }

    public String getKey() {
        return key;
    }

    public String getClassName() {
        return className;
    }

    public String getPermission() {
        return permission;
    }

    public String getConfigFileName() {
        return configFileName;
    }

    public String getOptions() {
        return options;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public void setConfigFileName(String configFileName) {
        this.configFileName = configFileName;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    @Override
    public String toString() {
        return "DataAdapterView{" +
                "key='" + key + '\'' +
                ", className='" + className + '\'' +
                ", permission='" + permission + '\'' +
                ", configFileName='" + configFileName + '\'' +
                ", options='" + options + '\'' +
                '}';
    }
}
