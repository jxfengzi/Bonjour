
package com.miui.bonjour.serviceinfo.impl;

import com.miui.bonjour.serviceinfo.BonjourServiceInfo;

import java.util.HashMap;
import java.util.Map;

public class DacpServiceInfo implements BonjourServiceInfo {

    public static final String SERVICE_TYPE = "_dacp._tcp";
    private String name = null;
    private String type = null;
    private String ip = null;
    private int port = 0;
    private Map<String, String> mProperties = new HashMap<String, String>();

    public DacpServiceInfo(String name, int port) {
        this.type = SERVICE_TYPE;
        this.name = name;
        this.port = port;

        mProperties.put("txtvers", "1");
        mProperties.put("Ver", "131075");
        mProperties.put("DbId", "63B5E5C0C201542E");
        mProperties.put("OSsi", "0x1F5");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getIp() {
        return ip;
    }

    @Override
    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public Map<String, String> getProperties() {
        return mProperties;
    }

    @Override
    public void setProperties(Map<String, String> properties) {
        this.mProperties = properties;
    }
}
