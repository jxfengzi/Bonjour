package ouyang.bonjourdemo;

//import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import ouyang.bonjour.Bonjour;
import ouyang.bonjour.BonjourFactory;
import ouyang.bonjour.BonjourListener;
import ouyang.bonjour.serviceinfo.BonjourServiceInfo;
import ouyang.bonjour.serviceinfo.impl.BonjourServiceInfoImpl;


public class MainActivity extends Activity implements BonjourListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Bonjour bonjour = null;
    BonjourServiceInfoImpl serviceInfo = new BonjourServiceInfoImpl();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bonjour = BonjourFactory.create(this);
        bonjour.setListener(this);
        bonjour.start();
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

    public void onButtonStart(View button) {
        Log.d(TAG, "onButtonStart");

        bonjour.startDiscovery("_airplay._tcp");
        bonjour.startDiscovery("_raop._tcp");

//        bonjour.startDiscovery("_airplay._tcp.local.");
//        bonjour.startDiscovery("_raop._tcp.local.");
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

    }

    @Override
    public void onServiceLost(BonjourServiceInfo serviceInfo) {
        Log.d(TAG, String.format("onServiceLost: %s %s %s %d", serviceInfo.getName(), serviceInfo.getType(), serviceInfo.getIp(), serviceInfo.getPort()));
    }
}