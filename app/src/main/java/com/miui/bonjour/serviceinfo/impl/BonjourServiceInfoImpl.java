package com.miui.bonjour.serviceinfo.impl;

import java.util.HashMap;
import java.util.Map;

import com.miui.bonjour.serviceinfo.BonjourServiceInfo;

/**
 * Created by ouyang on 15-5-11.
 */
public class BonjourServiceInfoImpl implements BonjourServiceInfo {

    private String name;
    private String type;
    private String ip;
    private int port;
    private Map<String, String> properties = new HashMap<String, String>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}