package com.leco.ykg.ykgbaseble;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.ParcelUuid;

import android.app.ActivityManager.RunningAppProcessInfo;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by zs on 2018/7/9.
 */
class YKGBLEScanner implements LeScanCallback {
    private BluetoothAdapter mAdapter;
    protected HashMap<String, YKGBLEDevice> mDevices;
    private ArrayList<String> scanTypes;
    private YKGBLEScanner.JCBLEScannerListener listener;
    private Context _context;
    private boolean scanning;
    private boolean scanEnable;
    private static YKGBLEScanner mBleScanner;
    Timer timer;
    boolean hasDevice;

    YKGBLEScanner() {
    }

    static YKGBLEScanner getInstance(Context context) {
        if (mBleScanner == null) {
            Class var1 = YKGBLEScanner.class;
            synchronized(YKGBLEScanner.class) {
                if (mBleScanner == null) {
                    mBleScanner = new YKGBLEScanner();
                    mBleScanner.mDevices = new HashMap();
                    mBleScanner._context = context;
                    if (mBleScanner.check()) {
                        mBleScanner.turnOnBluetooth();
                    }
                }
            }
        }

        return mBleScanner;
    }

    YKGBLEDevice getDevice(String sn) {
        return this.mDevices.containsKey(sn) ? (YKGBLEDevice)this.mDevices.get(sn) : null;
    }

    public void setListener(YKGBLEScanner.JCBLEScannerListener listener) {
        this.listener = listener;
    }

    private void resetTimer() {
        if (this.timer != null) {
            this.timer.cancel();
        }

        this.timer = new Timer();
        TimerTask task = new TimerTask() {
            public void run() {
                YKGUtils.logi("hasDevice " + YKGBLEScanner.this.hasDevice);
                if (YKGBLEScanner.this.scanning && !YKGBLEScanner.this.hasDevice) {
                    YKGBLEScanner.this.mAdapter.stopLeScan(YKGBLEScanner.this);
                    YKGBLEScanner.this.mAdapter.startLeScan(YKGBLEScanner.this);
                }

                YKGBLEScanner.this.resetTimer();
            }
        };
        this.hasDevice = false;
        this.timer.schedule(task, 3000L);
    }

    protected void startScan(ArrayList<String> types) {
        try {
            if (this.check()) {
                this.scanTypes = types;
                boolean ret = this.mAdapter.startLeScan(this);
                this.scanEnable = true;
                this.resetTimer();
                this.scanning = true;
                if (!ret) {
                    ;
                }
            }
        } catch (Exception var3) {
            ;
        }

    }

    protected void stopScan() {
        if (this.check()) {
            this.mAdapter.stopLeScan(this);
            this.scanning = false;
        }

    }

    void cleanDevices() {
        this.mDevices.clear();
    }

    public void onLeScan(BluetoothDevice device, int rssi, byte[] record) {
        YKGScanRecord scanrecord = YKGScanRecord.parseFromBytes(record);
        YKGBLEDevice tempDevice = this.parseDevice(device, scanrecord, rssi);
        if (tempDevice != null) {
            this.hasDevice = true;
            boolean contains = false;
            if (this.scanTypes != null) {
                if (this.scanTypes.contains(tempDevice.sn.substring(0, 2))) {
                    contains = true;
                }
            } else {
                contains = true;
            }

            if (!contains) {
                return;
            }

            YKGBLEDevice bleDevice;
            if (this.mDevices.containsKey(tempDevice.sn)) {
                bleDevice = (YKGBLEDevice)this.mDevices.get(tempDevice.sn);
            } else {
                bleDevice = new YKGBLEDevice();
            }

            bleDevice.sn = tempDevice.sn;
            bleDevice.name = tempDevice.name;
            bleDevice.version = tempDevice.version;
            bleDevice.temperature = tempDevice.temperature;
            bleDevice.battery = tempDevice.battery;
            bleDevice.bluetoothDevice = tempDevice.bluetoothDevice;
            bleDevice.rssi = tempDevice.rssi;
            bleDevice.lastUpdateDate = new Date();
            if (!this.mDevices.containsKey(tempDevice.sn)) {
                this.mDevices.put(tempDevice.sn, bleDevice);
            }

            if (this.listener != null) {
                this.listener.bleScannerDidDiscoverDevice(bleDevice);
            }
        }

    }

    private YKGBLEDevice parseDevice(BluetoothDevice device, YKGScanRecord scanrecord, int rssi) {
        Map<ParcelUuid, byte[]> deviceInfos = scanrecord.getServiceData();
        String deviceName = scanrecord.getDeviceName() != null ? scanrecord.getDeviceName() : device.getName();
        if (deviceName == null) {
            deviceName = "";
        }

        if (!deviceName.toLowerCase().startsWith("j")) {
            return null;
        } else {
            YKGBLEDevice parker = this.parseParker(device, scanrecord, rssi);
            if (parker != null) {
                return parker;
            } else if (deviceInfos.size() == 0) {
                return null;
            } else {
                ParcelUuid deviceUuid = (ParcelUuid)deviceInfos.keySet().toArray()[0];
                String deviceUuidStr = deviceUuid.toString().substring(4, 8);
                if (deviceUuidStr.equals("180a")) {
                    return null;
                } else {
                    byte[] infoByte = (byte[])deviceInfos.get(deviceUuid);
                    if (infoByte == null) {
                        return null;
                    } else if (infoByte.length >= 9) {
                        String sn = null;
                        sn = String.format("%02x%02x%02x%02x%02x%02x", infoByte[0], infoByte[1], infoByte[2], infoByte[3], infoByte[4], infoByte[5]);
                        String versionStr = null;
                        sn = String.format("%s%s", deviceUuidStr, sn);
                        int temperature;
                        if (versionStr == null) {
                            int version = infoByte[8];
                            temperature = version >> 4;
                            int minVersion = version & 15;
                            versionStr = String.format("%d.%d", temperature, minVersion);
                        }

                        int battery = infoByte[6] & 255;
                        temperature = infoByte[7] & 255;
                        if (versionStr.equals("1.0")) {
                            battery = infoByte[7] & 255;
                            temperature = infoByte[8] & 255;
                        }

                        YKGBLEDevice bleDevice = new YKGBLEDevice();
                        bleDevice.sn = sn.toLowerCase();
                        bleDevice.version = versionStr;
                        bleDevice.name = deviceName;
                        bleDevice.temperature = temperature;
                        bleDevice.battery = battery;
                        bleDevice.bluetoothDevice = device;
                        bleDevice.rssi = rssi;
                        return bleDevice;
                    } else {
                        return null;
                    }
                }
            }
        }
    }

    private YKGBLEDevice parseParker(BluetoothDevice device, YKGScanRecord scanrecord, int rssi) {
        String deviceName = scanrecord.getDeviceName() != null ? scanrecord.getDeviceName() : device.getName();
        if (deviceName == null) {
            deviceName = "";
        }

        ParcelUuid cbuuid = null;
        if (scanrecord.getServiceUuids() != null && scanrecord.getServiceUuids().size() > 0) {
            cbuuid = (ParcelUuid)scanrecord.getServiceUuids().get(0);
        }

        Map<ParcelUuid, byte[]> serviceData = scanrecord.getServiceData();
        byte[] deviceInfoData = scanrecord.getManufacturerSpecificData();
        if (serviceData != null && cbuuid != null || deviceInfoData != null && cbuuid != null) {
            String cbuuidStr = cbuuid.toString().substring(4, 8);
            ParcelUuid infoUuid = ParcelUuid.fromString("0000180a-0000-1000-8000-00805f9b34fb");
            byte[] infoByte;
            if (serviceData != null && serviceData.get(infoUuid) != null) {
                infoByte = (byte[])serviceData.get(infoUuid);
            } else {
                infoByte = deviceInfoData;
            }

            if (infoByte == null) {
                return null;
            }

            if (infoByte.length >= 9) {
                String sn = null;
                sn = String.format("%02x%02x%02x%02x%02x%02x", infoByte[0], infoByte[1], infoByte[2], infoByte[3], infoByte[4], infoByte[5]);
                String versionStr = null;
                if (cbuuidStr.equals("f001")) {
                    sn = String.format("%s%s", "0101", sn);
                    versionStr = "1.0";
                } else {
                    sn = String.format("%s%s", cbuuidStr.toLowerCase(), sn);
                }

                int temperature;
                if (versionStr == null) {
                    int version = infoByte[8];
                    temperature = version >> 4;
                    int minVersion = version & 15;
                    versionStr = String.format("%d.%d", temperature, minVersion);
                }

                int battery = infoByte[6] & 255;
                temperature = infoByte[7] & 255;
                if (versionStr.equals("1.0")) {
                    battery = infoByte[7] & 255;
                    temperature = infoByte[8] & 255;
                }

                YKGBLEDevice bleDevice = new YKGBLEDevice();
                bleDevice.sn = sn.toLowerCase();
                bleDevice.version = versionStr;
                bleDevice.name = deviceName;
                bleDevice.temperature = temperature;
                bleDevice.battery = battery;
                bleDevice.bluetoothDevice = device;
                bleDevice.rssi = rssi;
                return bleDevice;
            }
        }

        return null;
    }

    private boolean check() {
        if (this.isBluetoothEnable()) {
            ;
        }

        if (this.mAdapter == null) {
            YKGUtils.loge("蓝牙未打开或者不支持");
            return false;
        } else {
            return true;
        }
    }

    public boolean isBluetoothEnable() {
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        return this.mAdapter != null ? this.mAdapter.isEnabled() : false;
    }

    public boolean turnOnBluetooth() {
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        return this.mAdapter != null && !this.isBackground(this._context) ? this.mAdapter.enable() : false;
    }

    public boolean isBackground(Context context) {
        @SuppressLint("WrongConstant") ActivityManager activityManager = (ActivityManager)context.getSystemService("activity");
        List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        Iterator var4 = appProcesses.iterator();

        RunningAppProcessInfo appProcess;
        do {
            if (!var4.hasNext()) {
                return false;
            }

            appProcess = (RunningAppProcessInfo)var4.next();
        } while(!appProcess.processName.equals(context.getPackageName()));

        if (appProcess.importance == 100) {
            return false;
        } else {
            return true;
        }
    }

    interface JCBLEScannerListener {
        void bleScannerDidDiscoverDevice(YKGBLEDevice var1);
    }
}
