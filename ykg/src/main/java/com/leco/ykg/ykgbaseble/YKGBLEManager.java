package com.leco.ykg.ykgbaseble;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;


import com.leco.ykg.ykgbaseble.YKGBLEGatt.JCBLEGattListener;
import com.leco.ykg.ykgbaseble.YKGBLEScanner.JCBLEScannerListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by zs on 2018/7/9.
 */
public class YKGBLEManager implements JCBLEScannerListener,JCBLEGattListener{
    private static YKGBLEManager mBleManager;
    private HashMap mListeners;
    private YKGBLEScanner mScanner;
    private YKGBLEGatt mGatt;
    private Handler mHandle;
    private Context _context;
    private boolean scanning = false;
    private YKGBLEDevice mCurrentDevice;
    private ArrayList<String> scanTypes;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String var3 = intent.getAction();
            byte var4 = -1;
            switch(var3.hashCode()) {
                case -1530327060:
                    if (var3.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
                        var4 = 0;
                    }
                default:
                    switch(var4) {
                        case 0:
                            int blueState = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", 0);
                            switch(blueState) {
                                case 10:
                                    YKGUtils.logi("onReceive---------STATE_OFF");
                                    break;
                                case 11:
                                    YKGUtils.logi("onReceive---------STATE_TURNING_ON");
                                    break;
                                case 12:
                                    YKGUtils.logi("onReceive---------STATE_ON");
                                    if (YKGBLEManager.this.scanning) {
                                        YKGBLEManager.this.startScan(YKGBLEManager.this.scanTypes);
                                    }
                                    break;
                                case 13:
                                    YKGUtils.logi("onReceive---------STATE_TURNING_OFF");
                                    YKGBLEManager.this.mGatt.close();
                                    YKGBLEManager.this.mCurrentDevice = null;
                                    YKGBLEManager.this.mScanner.cleanDevices();
                                    YKGBLEManager.this.mScanner.stopScan();
                            }
                        default:
                    }
            }
        }
    };

    public YKGBLEManager() {
    }

    public static YKGBLEManager getInstance(Context context) {
        if (mBleManager == null) {
            Class var1 = YKGBLEManager.class;
            synchronized(YKGBLEManager.class) {
                if (mBleManager == null) {
                    mBleManager = new YKGBLEManager();
                    context = context.getApplicationContext();
                    mBleManager.mScanner = YKGBLEScanner.getInstance(context);
                    mBleManager.mScanner.setListener(mBleManager);
                    mBleManager.mGatt = YKGBLEGatt.getInstance(context, BluetoothAdapter.getDefaultAdapter());
                    mBleManager.mListeners = new HashMap();
                    mBleManager.mGatt.setListener(mBleManager);
                    mBleManager.mHandle = new Handler(Looper.getMainLooper());
                    mBleManager._context = context;
                    mBleManager._context.registerReceiver(mBleManager.mReceiver, mBleManager.makeFilter());
                }
            }
        }

        return mBleManager;
    }

    protected void finalize() throws Throwable {
        if (this._context != null && this.mReceiver != null) {
            this._context.unregisterReceiver(this.mReceiver);
        }

        super.finalize();
    }

    public void addListener(Object listener) {
        synchronized(this) {
            if (this.mListeners.containsKey("" + listener.hashCode())) {
                this.mListeners.remove("" + listener.hashCode());
            }

            this.mListeners.put("" + listener.hashCode(), listener);
        }
    }

    public void removeListener(Object listener) {
        synchronized(this) {
            if (this.mListeners.containsKey("" + listener.hashCode())) {
                this.mListeners.remove("" + listener.hashCode());
            }

        }
    }

    public void addRequest(YKGBLERequest request) {
        this.mGatt.addRequest(request);
    }

    public void startScan(ArrayList<String> types) {
        this.scanning = true;
        this.scanTypes = types;
        this.mScanner.startScan(types);
    }

    public void stopScan() {
        this.scanning = false;
        this.mScanner.stopScan();
    }

    public YKGBLEDevice getDevice(String sn) {
        return this.mScanner.getDevice(sn);
    }

    public boolean connect(YKGBLEDevice device) {
        this.mCurrentDevice = device;
        return this.mGatt.connect(device);
    }

    public void disconnect() {
        this.mGatt.disconnect();
    }

    public void readRssi() {
        this.mGatt.readRssi();
    }

    public boolean isConnected(YKGBLEDevice device) {
        return this.mGatt.isConnected(device);
    }

    public void bleScannerDidDiscoverDevice(final YKGBLEDevice device) {
        this.mHandle.post(new Runnable() {
            public void run() {
                synchronized(this) {
                    Iterator var2 = YKGBLEManager.this.mListeners.values().iterator();

                    while(var2.hasNext()) {
                        Object listener = var2.next();
                        ((YKGBLEManager.JCBLEManagerListener)listener).bleManagerDidDiscoverDevice(device);
                    }

                }
            }
        });
    }

    public void bleGattDidConnectDevice(final YKGBLEDevice device) {
        this.mHandle.post(new Runnable() {
            public void run() {
                synchronized(this) {
                    Iterator var2 = YKGBLEManager.this.mListeners.values().iterator();

                    while(var2.hasNext()) {
                        Object listener = var2.next();
                        ((YKGBLEManager.JCBLEManagerListener)listener).bleManagerDidConnectDevice(device);
                    }

                }
            }
        });
    }

    public void bleGattDidDisConnectDevice(final YKGBLEDevice device, final int errorCode) {
        this.mHandle.post(new Runnable() {
            public void run() {
                synchronized(this) {
                    Iterator var2 = YKGBLEManager.this.mListeners.values().iterator();

                    while(var2.hasNext()) {
                        Object listener = var2.next();
                        ((YKGBLEManager.JCBLEManagerListener)listener).bleManagerDidDisConnectDevice(device, errorCode);
                    }

                }
            }
        });
    }

    public void bleGattDidRecieveResponse(final YKGBLEDevice device, final YKGBLEResponse response) {
        this.mHandle.post(new Runnable() {
            public void run() {
                synchronized(this) {
                    YKGUtils.logi(Thread.currentThread() + "");
                    Iterator var2 = YKGBLEManager.this.mListeners.values().iterator();

                    while(var2.hasNext()) {
                        Object listener = var2.next();
                        ((YKGBLEManager.JCBLEManagerListener)listener).bleManagerDidRecieveResponse(device, response);
                    }

                }
            }
        });
    }

    public void bleGattDidUpdateRssi(final YKGBLEDevice device, final int rssi) {
        this.mHandle.post(new Runnable() {
            public void run() {
                synchronized(this) {
                    YKGUtils.logi(Thread.currentThread() + "");
                    Iterator var2 = YKGBLEManager.this.mListeners.values().iterator();

                    while(var2.hasNext()) {
                        Object listener = var2.next();
                        ((YKGBLEManager.JCBLEManagerListener)listener).bleManagerDidUpdateRssi(device, rssi);
                    }

                }
            }
        });
    }

    private IntentFilter makeFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        return filter;
    }

    public interface JCBLEManagerListener {
        void bleManagerDidDiscoverDevice(YKGBLEDevice var1);

        void bleManagerDidConnectDevice(YKGBLEDevice var1);

        void bleManagerDidDisConnectDevice(YKGBLEDevice var1, int var2);

        void bleManagerDidRecieveResponse(YKGBLEDevice var1, YKGBLEResponse var2);

        void bleManagerDidUpdateRssi(YKGBLEDevice var1, int var2);
    }
}
