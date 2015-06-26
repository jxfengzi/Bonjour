package com.miui.bonjour.impl;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import com.miui.bonjour.Bonjour;
import com.miui.bonjour.BonjourListener;
import com.miui.bonjour.serviceinfo.BonjourServiceInfo;
import com.miui.bonjour.serviceinfo.impl.BonjourServiceInfoImpl;

/**
 * Created by ouyang on 15-5-14.
 */
public class JavaBonjourImpl implements Bonjour {

    private static final String TAG = "JavaBonjourImpl";
    private static JavaBonjourImpl instance = null;
    private static Object classLock = AndroidBonjourImpl.class;

    private Context context;
    private JobHandler jobHandler = new JobHandler();
    private BonjourListener listener = null;

    public static JavaBonjourImpl getInstance(Context context) {
        synchronized (classLock) {
            if (instance == null) {
                instance = new JavaBonjourImpl(context);
            }

            return instance;
        }
    }

    private JavaBonjourImpl(Context context) {
        this.context = context;
    }

    @Override
    public void setListener(BonjourListener listener) {
        this.listener = listener;
    }

    @Override
    public void start() {
        jobHandler.start();
    }

    @Override
    public void stop() {
        jobHandler.stop();
    }

    @Override
    public void startDiscovery(String type) {
        Job job = new Job(JobType.SERVICE_DISCOVERY_START);
        job.setServiceType(type + "local.");

        jobHandler.put(job);
    }

    @Override
    public void stopAllDiscovery() {
        jobHandler.put(new Job(JobType.SERVICE_DISCOVERY_STOP));
    }

    @Override
    public void registerService(BonjourServiceInfo serviceInfo) {
        ServiceInfo info = ServiceInfo.create(serviceInfo.getType() + "local.",
                serviceInfo.getName(),
                serviceInfo.getPort(),
                0,
                0,
                serviceInfo.getProperties());

        Job job = new Job(JobType.SERVICE_REG);
        job.setServiceInfo(info);

        jobHandler.put(job);
    }

    @Override
    public void unregisterService(BonjourServiceInfo serviceInfo) {
        ServiceInfo info = ServiceInfo.create(serviceInfo.getType() + "local.",
                serviceInfo.getName(),
                serviceInfo.getPort(),
                0,
                0,
                serviceInfo.getProperties());

        Job job = new Job(JobType.SERVICE_UNREG);
        job.setServiceInfo(info);

        jobHandler.put(job);
    }

    private enum JobType {
        START,
        STOP,
        SERVICE_DISCOVERY_START,
        SERVICE_DISCOVERY_STOP,
        SERVICE_FOUND,
        SERVICE_LOST,
        SERVICE_RESOLVED,
        SERVICE_REG,
        SERVICE_UNREG,
    }

    private class Job {
        private JobType type;
        private String serviceType;
        private ServiceInfo serviceInfo;
        private ServiceEvent serviceEvent;

        public Job(JobType type) {
            this.type = type;
        }

        public JobType getType() {
            return type;
        }

        public void setType(JobType type) {
            this.type = type;
        }

        public String getServiceType() {
            return serviceType;
        }

        public void setServiceType(String serviceType) {
            this.serviceType = serviceType;
        }

        public ServiceEvent getServiceEvent() {
            return serviceEvent;
        }

        public void setServiceEvent(ServiceEvent serviceEvent) {
            this.serviceEvent = serviceEvent;
        }

        public ServiceInfo getServiceInfo() {
            return serviceInfo;
        }

        public void setServiceInfo(ServiceInfo serviceInfo) {
            this.serviceInfo = serviceInfo;
        }
    }

    private class JobHandler implements Runnable {
        private static final int MAX_SERVICE_INFO = 255 * 3;
        private static final int STOP_TIMEOUT = 1000 * 5;
        private WifiManager.MulticastLock wifiLock = null;
        private JmDNS jmdns = null;
        private Thread thread = null;
        private BlockingQueue<Job> jobQueue = null;
        private Map<String, MyServiceListener> myListeners = new HashMap<String, MyServiceListener>();
        private Map<String, BonjourServiceInfo> foundServices = new HashMap<String, BonjourServiceInfo>();
        private Map<String, ServiceInfo> regServices = new HashMap<String, ServiceInfo>();

        public JobHandler() {
            jobQueue = new ArrayBlockingQueue<Job>(MAX_SERVICE_INFO);
        }

        public synchronized void start() {
            if (thread == null) {
                Log.d(TAG, "JobHandler start");
                thread = new Thread(this);
                thread.start();

                jobQueue.add(new Job(JobType.START));
            }
        }

        public synchronized void stop() {
            if (thread != null) {
                Log.d(TAG, "JobHandler stop");
                jobQueue.clear();
                jobQueue.add(new Job(JobType.STOP));

                try {
                    thread.join(STOP_TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                thread = null;
            }
        }

        public synchronized void put(Job job) {
            try {
                jobQueue.add(job);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            Log.d(TAG, "JobHandler running ...");

            while (true) {
                Job job = null;

                try {
                    job = jobQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }

                if (job.getType() == JobType.STOP) {
                    this.doStopJmdns();
                    break;
                }

                switch (job.getType()) {
                    case START:
                        doStartJmdns();
                        break;

                    case SERVICE_DISCOVERY_START:
                        doStartDiscovery(job.getServiceType());
                        break;

                    case SERVICE_DISCOVERY_STOP:
                        doStopDiscovery();
                        break;

                    case SERVICE_FOUND:
                        doServiceFound(job.getServiceEvent());
                        break;

                    case SERVICE_LOST:
                        doServiceLost(job.getServiceEvent());
                        break;

                    case SERVICE_RESOLVED:
                        doServiceResolved(job.getServiceEvent());
                        break;

                    case SERVICE_REG:
                        doServiceReg(job.getServiceInfo());
                        break;

                    case SERVICE_UNREG:
                        doServiceUnreg(job.getServiceInfo().getType());
                        break;
                }
            }

            jobQueue.clear();
            myListeners.clear();
            foundServices.clear();
            regServices.clear();

            Log.d(TAG, "JobHandler run over");
        }

        private void doStartJmdns() {
            Log.v(TAG, "doStartJmdns");

            do {
                if (jmdns != null) {
                    Log.d(TAG, "jmdns already started");
                    if (listener != null) {
                        listener.onStartFailed();
                    }
                    break;
                }

                WifiManager wifi = (WifiManager) context.getSystemService(android.content.Context.WIFI_SERVICE);
                wifiLock = wifi.createMulticastLock("wifilock");
                wifiLock.setReferenceCounted(true);
                wifiLock.acquire();

                byte[] ip = getLocalIpInt(context);
                if (ip == null) {
                    Log.d(TAG, "local ip is null");
                    if (listener != null) {
                        listener.onStartFailed();
                    }
                    break;
                }

                InetAddress addr = null;

                try {
                    addr = InetAddress.getByAddress(ip);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    if (listener != null) {
                        listener.onStartFailed();
                    }
                    break;
                }

                try {
                    jmdns = JmDNS.create(addr);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "JmDNS.create() failed!");
                    if (listener != null) {
                        listener.onStartFailed();
                    }
                    break;
                }

                Log.d(TAG, String.format("JmDNS version: %s (%s)", JmDNS.VERSION, addr.getHostAddress()));

                if (listener != null) {
                    listener.onStarted();
                }
            } while (false);

            if (jmdns == null) {
                wifiLock.setReferenceCounted(false);
                wifiLock.release();
                wifiLock = null;
            }
        }

        private void doStopJmdns() {
            Log.v(TAG, "doStopJmdns");

            do {
                if (jmdns == null) {
                    Log.d(TAG, "jmdns not start");
                    break;
                }

                jmdns.unregisterAllServices();

                try {
                    jmdns.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                jmdns = null;

                if (wifiLock != null) {
                    wifiLock.setReferenceCounted(false);
                    wifiLock.release();
                    wifiLock = null;
                    break;
                }

                if (listener != null) {
                    listener.onStopped();
                }
            } while (false);
        }

        private void doStartDiscovery(String serviceType) {
            Log.v(TAG, String.format("doStartDiscovery: %s", serviceType));

            do {
                if (jmdns == null) {
                    Log.d(TAG, "jmdns not started");
                    break;
                }

                if (myListeners.containsKey(serviceType)) {
                    Log.d(TAG, String.format("discovery is started: %s", serviceType));
                    break;
                }

                MyServiceListener myListener = new MyServiceListener();
                myListeners.put(serviceType, myListener);
                jmdns.addServiceListener(serviceType, myListener);
            } while (false);
        }

        private void doStopDiscovery() {
            Log.v(TAG, "doStopDiscovery");

            do {
                if (jmdns == null) {
                    Log.d(TAG, "jmdns not started");
                    break;
                }

                for (Map.Entry<String, MyServiceListener> l : myListeners.entrySet()) {
                    jmdns.removeServiceListener(l.getKey(), l.getValue());
                }
            } while (false);
        }

        private void doServiceFound(ServiceEvent event) {
            Log.v(TAG, "doServiceFound");

            do {
                if (jmdns == null) {
                    Log.d(TAG, "jmdns not started");
                    break;
                }

                jmdns.requestServiceInfo(event.getType(), event.getName());
            } while (false);
        }

        private void doServiceLost(ServiceEvent event) {
            Log.v(TAG, "doServiceLost");

            do {
                if (jmdns == null) {
                    Log.d(TAG, "jmdns not started");
                    break;
                }

                BonjourServiceInfo s = null;
                synchronized (foundServices) {
                    s = foundServices.get(event.getName());
                }

                if (s == null) {
                    Log.d(TAG, "service not exist");
                    break;
                }

                if (listener == null) {
                    break;
                }

                listener.onServiceLost(s);
            } while (false);
        }

        private void doServiceResolved(ServiceEvent event) {
            Log.v(TAG, "doServiceResolved");

            do {
                String name = event.getName();
                String type = event.getType();
                int port = event.getInfo().getPort();
                String ip = null;

                Inet4Address[] addresses = event.getInfo().getInet4Addresses();
                for (int i = 0; i < addresses.length; ++i) {
                    ip = addresses[i].getHostAddress();

                    Log.d(TAG,
                            String.format("serviceResolved: %s.%s %s:%d",
                                    event.getName(), event.getType(), ip, port));
                }

                if (ip == null) {
                    break;
                }

                Map<String, String> properties = new HashMap<String, String>();
                Enumeration<String> propertyNames = event.getInfo().getPropertyNames();
                while (propertyNames.hasMoreElements()) {
                    String key = propertyNames.nextElement();
                    String value = event.getInfo().getPropertyString(key);
                    properties.put(key, value);
                }

                BonjourServiceInfo s = new BonjourServiceInfoImpl();
                s.setName(name);
                s.setType(type);
                s.setIp(ip);
                s.setPort(port);
                s.getProperties();

                synchronized (foundServices) {
                    foundServices.put(name, s);
                    Log.d(TAG, String.format("foundServices is: %d", foundServices.size()));
                }

                if (listener == null) {
                    break;
                }

                listener.onServiceFound(s);
            } while (false);
        }

        private void doServiceReg(ServiceInfo info) {
            Log.v(TAG, "doServiceReg");

            do {
                if (jmdns == null) {
                    Log.d(TAG, "jmdns not started");
                    break;
                }

                if (regServices.containsKey(info.getType())) {
                    Log.d(TAG, String.format("%s already registered", info.getType()));
                    break;
                }

                regServices.put(info.getType(), info);

                try {
                    jmdns.registerService(info);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while (false);
        }

        private void doServiceUnreg(String serviceType) {
            Log.v(TAG, "doServiceUnreg");

            do {
                if (jmdns == null) {
                    Log.d(TAG, "jmdns not started");
                    break;
                }

                if (!regServices.containsKey(serviceType)) {
                    Log.d(TAG, String.format("%s not registered", serviceType));
                    break;
                }

                ServiceInfo serviceInfo = regServices.get(serviceType);
                jmdns.unregisterService(serviceInfo);
                regServices.remove(serviceInfo);
            } while (false);
        }

        private byte[] getLocalIpInt(Context context) {
            byte[] value = null;

            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

            do {
                if (!wm.isWifiEnabled()) {
                    break;
                }

                WifiInfo wi = wm.getConnectionInfo();
                if (wi == null) {
                    break;
                }

                value = intToBytes(wi.getIpAddress());
            } while (false);

            return value;
        }

        private byte[] intToBytes(int i) {
            byte[] ip = new byte[4];
            ip[0] = (byte) (i & 0xFF);
            ip[1] = (byte) ((i >> 8) & 0xFF);
            ip[2] = (byte) ((i >> 16) & 0xFF);
            ip[3] = (byte) ((i >> 24) & 0xFF);

            return ip;
        }
    }

    private class MyServiceListener implements ServiceListener {

        @Override
        public void serviceAdded(ServiceEvent event) {
            Job job = new Job(JobType.SERVICE_FOUND);
            job.setServiceEvent(event);
            jobHandler.put(job);
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            Job job = new Job(JobType.SERVICE_LOST);
            job.setServiceEvent(event);
            jobHandler.put(job);
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            Job job = new Job(JobType.SERVICE_RESOLVED);
            job.setServiceEvent(event);
            jobHandler.put(job);
        }
    }
}
