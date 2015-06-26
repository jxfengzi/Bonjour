package ouyang.bonjourdemo;

//import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.util.Base64;

import java.util.Map;

import com.miui.bonjour.Bonjour;
import com.miui.bonjour.BonjourFactory;
import com.miui.bonjour.BonjourListener;
import com.miui.bonjour.serviceinfo.BonjourServiceInfo;
import com.miui.bonjour.serviceinfo.impl.BonjourServiceInfoImpl;


public class MainActivity extends Activity implements BonjourListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Bonjour bonjour = null;
    BonjourServiceInfoImpl serviceInfo = new BonjourServiceInfoImpl();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new BonjourTask().execute();
    }

    @Override
    protected void onDestroy() {
        bonjour.stop();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class BonjourTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            Log.d(TAG, "doInBackground");
            bonjour = BonjourFactory.create(MainActivity.this);
            bonjour.setListener(MainActivity.this);
            bonjour.start();
            return Boolean.TRUE;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            Log.d(TAG, "onPostExecute");
            super.onPostExecute(aBoolean);
        }
    }

    public void onButtonStart(View button) {
        Log.d(TAG, "onButtonStart");

        bonjour.startDiscovery("_airplay._tcp");
//        bonjour.startDiscovery("_raop._tcp");

//        bonjour.startDiscovery("_airplay._tcp.local.");
//        bonjour.startDiscovery("_raop._tcp.local.");


//        String t1 = "hi=hello";
//        String t2 = "ver=123";
//        byte b1[] = t1.getBytes();
//        byte b2[] = t2.getBytes();
//
//        byte test[] = new byte[1 + b1.length + 1 + b2.length];
//
//        test[0] = (byte)(b1.length);
//        System.arraycopy(b1, 0, test, 1, b1.length);
//
//        test[1 + b1.length] = (byte)(b2.length);
//        System.arraycopy(b2, 0, test, 1 + b1.length + 1, b2.length);
//
//        setTxtRecord(test.length, test);

//        byte[] txtRecord = getTxtRecord();
//        setTxtRecord(txtRecord.length, txtRecord);
    }

    public void onButtonStop(View button) {
        Log.d(TAG, "onButtonStop");

        bonjour.stopAllDiscovery();
    }

    public void onButtonReg(View button) {
        Log.d(TAG, "onButtonReg");

        serviceInfo.setName("OuyangHelloDevice");
        serviceInfo.setType("_http._tcp");
//        serviceInfo.setType("_http._tcp.local.");
        serviceInfo.setPort(8080);
        serviceInfo.getProperties().put("ver", "123");
        serviceInfo.getProperties().put("hello", "hello, world");

        bonjour.registerService(serviceInfo);
    }

    public void onButtonUnreg(View button) {
        Log.d(TAG, "onButtonUnreg");
        bonjour.unregisterService(serviceInfo);
    }

    @Override
    public void onStarted() {
        Log.d(TAG, "onStarted");
    }

    @Override
    public void onStartFailed() {
        Log.d(TAG, "onStartFailed");
    }

    @Override
    public void onStopped() {
        Log.d(TAG, "onStopped");
     }

    @Override
    public void onServiceFound(BonjourServiceInfo serviceInfo) {
        Log.d(TAG, String.format("onServiceFound: %s %s %s %d", serviceInfo.getName(), serviceInfo.getType(), serviceInfo.getIp(), serviceInfo.getPort()));

        if (serviceInfo.getProperties() != null) {
            for (Map.Entry<String, String> v : serviceInfo.getProperties().entrySet()) {
                Log.e(TAG, String.format("%s=%s", v.getKey(), v.getValue()));
            }
        }
    }

    @Override
    public void onServiceLost(BonjourServiceInfo serviceInfo) {
        Log.d(TAG, String.format("onServiceLost: %s %s %s %d", serviceInfo.getName(), serviceInfo.getType(), serviceInfo.getIp(), serviceInfo.getPort()));
    }

    private byte[] getTxtRecord() {
        String base64 = "GmRldmljZWlkPTI4OkNGOkRBOjI3OkU2OjUxF2ZlYXR1cmVzPTB4NEE3RkZGRjcsMHhFCmZsYWdzPTB4NDQQbW9kZWw9QXBwbGVUVjIsMUNwaz1hMWZiMDY0MjU3YTY2ZTQ3YjY1N2ViNTdjZmJlM2U4ZjE1MmQ5NjlkNTdlZmQ2YzE0NjI2MzEyNTliZWQxZWYxDnNyY3ZlcnM9MjAwLjU0BHZ2PTI=";
        return Base64.decode(base64.getBytes(), Base64.DEFAULT);
    }

    public void setTxtRecord(int txtLen, byte[] txtRecord) {
        do {
            if (txtLen < 2) {
                Log.w(TAG, "txtLen < 2");
                break;
            }

            if (txtRecord == null) {
                Log.w(TAG, "txtRecord is null");
                break;
            }

            if (txtRecord.length != txtLen) {
                Log.w(TAG, "txtRecord.length != txtLen");
                break;
            }

            int i = 0;
            while (i < txtLen) {
                byte length = txtRecord[i];
                if (length >= txtLen) {
                    Log.w(TAG, String.format("invalid length: %d", length));
                    break;
                }

                int start = i + 1;
                byte buf[] = new byte[length];
                try {
                    System.arraycopy(txtRecord, start, buf, 0, length);
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }

                String v = new String(buf);
                String a[] = v.split("=");
                if (a.length == 2) {
                    String key = a[0];
                    String value = a[1];
//                mTxtRecord.put(key, value.getBytes());
                    Log.i(TAG, String.format("%s=%s", key ,value));
                }

                i = start + length;
            }
        } while (false);
    }
}