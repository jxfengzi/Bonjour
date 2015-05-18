package ouyang.bonjour;

import ouyang.bonjour.serviceinfo.BonjourServiceInfo;

/**
 * Created by ouyang on 15-5-11.
 */
public interface Bonjour {

    void setListener(BonjourListener listener);

    void start();

    void stop();

    void startDiscovery(String type);

    void stopAllDiscovery();

    void registerService(BonjourServiceInfo serviceInfo);

    void unregisterService(BonjourServiceInfo serviceInfo);
}