package com.miui.bonjour.serviceinfo;

import java.util.Map;

/**
 * Created by ouyang on 15-5-14.
 */
public interface BonjourServiceInfo {

    String getName();

    void setName(String name);

    String getType();

    void setType(String type);

    String getIp();

    void setIp(String ip);

    int getPort();

    void setPort(int port);

    Map<String, String> getProperties();

    void setProperties(Map<String, String> properties);
}
