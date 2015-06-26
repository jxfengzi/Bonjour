package com.miui.bonjour.impl;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.miui.bonjour.Bonjour;
import com.miui.bonjour.BonjourListener;
import com.miui.bonjour.impl.getter.ExtraInfoGetter;
import com.miui.bonjour.impl.getter.ExtraInfoGetterFactory;
import com.miui.bonjour.impl.setter.ExtraInfoSetter;
import com.miui.bonjour.impl.setter.ExtraInfoSetterFactory;
import com.miui.bonjour.serviceinfo.BonjourServiceInfo;
import com.miui.bonjour.serviceinfo.impl.BonjourServiceInfoImpl;


/**
 * Created by ouyang on 15-5-11.
 */
public class AndroidBonjourImpl implements Bonjour {

    private static final String TAG = "AndroidBonjourImpl";
    private static AndroidBonjourImpl instance = null;
    private static Object classLock = AndroidBonjourImpl.class;

    private NsdManager nsdManager;
    private Context context;
    private JobHandler jobHandler = new JobHandler();
    private BonjourListener listener = null;

    public static AndroidBonjourImpl getInstance(Context context) {
        synchronized (classLock) {
            if (instance == null) {
                instance = new AndroidBonjourImpl(context);
            }

            return instance;
        }
    }

    private AndroidBonjourImpl(Context context) {
        this.context = context;
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
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
        job.setServiceType(type);
        jobHandler.put(job);
    }

    @Override
    public void stopAllDiscovery() {
        jobHandler.put(new Job(JobType.SERVICE_DISCOVERY_STOP));
    }

    @Override
    public void registerService(BonjourServiceInfo serviceInfo) {
        NsdServiceInfo info = new NsdServiceInfo();
        info.setServiceName(serviceInfo.getName());
        info.setServiceType(serviceInfo.getType());
        info.setPort(serviceInfo.getPort());
        ExtraInfoSetter setter = ExtraInfoSetterFactory.create();
        setter.set(info, serviceInfo.getProperties());

        Log.d(TAG, info.toString());

        Job job = new Job(JobType.SERVICE_REG);
        job.setServiceInfo(info);
        jobHandler.put(job);
    }

    @Override
    public void unregisterService(BonjourServiceInfo serviceInfo) {
        NsdServiceInfo info = new NsdServiceInfo();
        info.setServiceName(serviceInfo.getName());
        info.setServiceType(serviceInfo.getType());
        info.setPort(serviceInfo.getPort());
        ExtraInfoSetter setter = ExtraInfoSetterFactory.create();
        setter.set(info, serviceInfo.getProperties());

        Log.d(TAG, info.toString());

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
        SERVICE_REG,
        SERVICE_UNREG,
    }

    private class Job {
        private JobType type;
        private String serviceType;
        private NsdServiceInfo serviceInfo;

        public Job(JobType type) {
            this.type = type;
        }

        public JobType getType() {
            return type;
        }

        public void setType(JobType type) {
            this.type = type;
        }

        public NsdServiceInfo getServiceInfo() {
            return serviceInfo;
        }

        public void setServiceInfo(NsdServiceInfo serviceInfo) {
            this.serviceInfo = serviceInfo;
        }

        public String getServiceType() {
            return serviceType;
        }

        public void setServiceType(String serviceType) {
            this.serviceType = serviceType;
        }
    }

    private class JobHandler implements Runnable {
        private static final int MAX_SERVICE_INFO = 255 * 3;
        private static final int RESOLVE_TIMEOUT = 1000 * 5;
        private static final int STOP_TIMEOUT = 1000 * 5;
        private Thread thread = null;
        private BlockingQueue<Job> jobQueue = null;
        private Map<String, DiscoverHandler> discoveryHandlers = new HashMap<String, DiscoverHandler>();
        private Map<String, BonjourServiceInfo> foundServices = new HashMap<String, BonjourServiceInfo>();
        private Map<String, RegistrationHandler> registrationHandlers = new HashMap<String, RegistrationHandler>();

        public JobHandler() {
            jobQueue = new ArrayBlockingQueue<Job>(MAX_SERVICE_INFO);
        }

        public synchronized void start() {
            if (thread == null) {
                Log.d(TAG, "JobHandler start");
                thread = new Thread(this);
                thread.start();
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

            if (listener != null) {
                listener.onStarted();
            }

            while (true) {
                Job job = null;

                try {
                    job = jobQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }

                if (job.getType() == JobType.STOP) {
                    break;
                }

                switch (job.getType()) {
                    case SERVICE_DISCOVERY_START:
                        doStartDiscovery(job.getServiceType());
                        break;

                    case SERVICE_DISCOVERY_STOP:
                        doStopDiscovery();
                        break;

                    case SERVICE_FOUND:
                        doServiceFound(job);
                        break;

                    case SERVICE_LOST:
                        doServiceLost(job);
                        break;

                    case SERVICE_REG:
                        doServiceReg(job.getServiceInfo());
                        break;

                    case SERVICE_UNREG:
                        doServiceUnreg(job.getServiceInfo());
                        break;
                }
            }

            jobQueue.clear();
            discoveryHandlers.clear();
            foundServices.clear();
            registrationHandlers.clear();

            Log.d(TAG, "JobHandler run over");

            if (listener != null) {
                listener.onStopped();
            }
        }

        private void doStartDiscovery(String serviceType) {
            Log.d(TAG, String.format("doStartDiscovery: %s", serviceType));

            do {
                if (discoveryHandlers.containsKey(serviceType)) {
                    Log.d(TAG, String.format("%s already started", serviceType));
                    break;
                }

                DiscoverHandler handler = new DiscoverHandler();
                discoveryHandlers.put(serviceType, handler);

                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, handler);
            } while (false);
        }

        private void doStopDiscovery() {
            Log.d(TAG, "doStopDiscovery");

            do {
                for (DiscoverHandler handler : discoveryHandlers.values()) {
                    try {
                        nsdManager.stopServiceDiscovery(handler);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }

                discoveryHandlers.clear();
                foundServices.clear();
            } while (false);
        }

        private void doServiceFound(final Job job) {
            Log.d(TAG, "doServiceFound");

            nsdManager.resolveService(job.getServiceInfo(), new NsdManager.ResolveListener() {

                @Override
                public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    Log.d(TAG, String.format("onResolveFailed: %d", errorCode));
                }

                @Override
                public void onServiceResolved(NsdServiceInfo serviceInfo) {
                    String name = serviceInfo.getServiceName().replace("\\032", " ");
                    String type = serviceInfo.getServiceType();
                    String ip = serviceInfo.getHost().getHostAddress();
                    int port = serviceInfo.getPort();

                    Log.d(TAG, String.format("onServiceResolved: %s", name));

                    BonjourServiceInfo s = new BonjourServiceInfoImpl();
                    s.setName(name);
                    s.setType(type);
                    s.setIp(ip);
                    s.setPort(port);

                    ExtraInfoGetter getter = ExtraInfoGetterFactory.create();
                    Map<String, String> properties = getter.get(serviceInfo);

                    if (properties != null) {
                        Log.d(TAG, "properties: " + properties.toString());
                        s.setProperties(properties);
                    }

                    synchronized (foundServices) {
                        foundServices.put(name, s);
                        Log.d(TAG, String.format("foundServices is: %d", foundServices.size()));
                    }

                    if (listener != null) {
                        listener.onServiceFound(s);
                    }

                    synchronized (job) {
                        job.notify();
                    }
                }
            });

            synchronized (job) {
                try {
                    job.wait(RESOLVE_TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void doServiceLost(Job job) {
            Log.d(TAG, "doServiceLost");

            String name = job.getServiceInfo().getServiceName().replace("\\032", " ");
            BonjourServiceInfo s = null;

            synchronized (foundServices) {
                s = foundServices.get(name);
                foundServices.remove(name);
                Log.d(TAG, String.format("foundServices is: %d", foundServices.size()));
            }

            if (s != null) {
                if (listener != null) {
                    listener.onServiceLost(s);
                }
            }
        }

        private void doServiceReg(NsdServiceInfo info) {
            do {
                String id = String.format("%s@%s", info.getServiceName(), info.getServiceType());
                if (registrationHandlers.containsKey(id)) {
                    break;
                }

                RegistrationHandler handler = new RegistrationHandler();
                registrationHandlers.put(id, handler);

                nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, handler);
            } while (false);
        }

        private void doServiceUnreg(NsdServiceInfo info) {
            do {
                String id = String.format("%s@%s", info.getServiceName(), info.getServiceType());
                RegistrationHandler handler = registrationHandlers.get(id);
                if (handler == null) {
                    break;
                }

                registrationHandlers.remove(id);

                try {
                    nsdManager.unregisterService(handler);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            } while (false);
        }
    }

    private class DiscoverHandler implements NsdManager.DiscoveryListener {

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.d(TAG, String.format("onStartDiscoveryFailed: %s %d", serviceType, errorCode));
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.d(TAG, String.format("onStopDiscoveryFailed: %s %d", serviceType, errorCode));
        }

        @Override
        public void onDiscoveryStarted(String serviceType) {
            Log.d(TAG, "onDiscoveryStarted");
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.d(TAG, "onDiscoveryStopped");
        }

        @Override
        public void onServiceFound(NsdServiceInfo serviceInfo) {
            String name = serviceInfo.getServiceName().replace("\\032", " ");
            Log.d(TAG, String.format("onServiceFound: %s", name));

            Job job = new Job(JobType.SERVICE_FOUND);
            job.setServiceInfo(serviceInfo);

            jobHandler.put(job);
        }

        @Override
        public void onServiceLost(NsdServiceInfo serviceInfo) {
            String name = serviceInfo.getServiceName().replace("\\032", " ");
            Log.d(TAG, String.format("onServiceLost: %s", name));

            Job job = new Job(JobType.SERVICE_LOST);
            job.setServiceInfo(serviceInfo);

            jobHandler.put(job);
        }
    }

    private class RegistrationHandler implements NsdManager.RegistrationListener {
        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.d(TAG, String.format("onRegistrationFailed: %d", errorCode));
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.d(TAG, String.format("onUnregistrationFailed: %d", errorCode));
        }

        @Override
        public void onServiceRegistered(NsdServiceInfo serviceInfo) {
            Log.d(TAG, String.format("onServiceRegistered: %s", serviceInfo.getServiceName()));
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            Log.d(TAG, String.format("onServiceUnregistered: %s", serviceInfo.getServiceName()));
        }
    }
}
