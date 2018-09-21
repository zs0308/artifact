package com.leco.ykg.ykgble;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.widget.Toast;

import com.leco.ykg.ykgbaseble.YKGBLEDevice;
import com.leco.ykg.ykgbaseble.YKGBLEManager;
import com.leco.ykg.ykgbaseble.YKGBLEManager.JCBLEManagerListener;
import com.leco.ykg.ykgbaseble.YKGBLEResponse;
import com.leco.ykg.ykgbaseble.YKGBLEWriteRequest;
import com.leco.ykg.ykgbaseble.YKGUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by zs on 2018/7/9.
 */
public class YKGLockerManager {
    public static int JCLockerDeviceStateUp = 1;
    public static int JCLockerDeviceStateDown = 2;
    public static int JCLockerDeviceStateError = 3;
    public static int JCLockerDeviceStateBlock = 4;
    public static int JCLockerDeviceStateTimeOut = 5;
    public static int JCMagneticStateInit = 0;
    public static int JCMagneticStateFree = 1;
    public static int JCMagneticStateInUse = 2;
    public static int JCMagneticStateBlock = 3;
    public static int JCMagneticStateInterfere = 4;
    public static int JCMagneticStateError = 5;
    public static int JCMagneticStateUnInit = 6;
    public static int ActionUp = 1;
    public static int ActionDown = 2;
    private static int JCLockerConnectStateDisconnect = 0;
    private static int JCLockerConnectStateConnecting = 1;
    private static int JCLockerConnectStateConnected = 2;
    private String JCLockerDisHost = "https://paas.apis.jcbel.com";
    private static YKGLockerManager mLockerManager;
    private YKGBLEManager bleManager;
    private YKGLockerManager.YKGLockerManagerListener listener;
    private YKGLockerManager.YKGLockerManagerExtendListener extendListener;
    private String targetSn = "";
    private String registerSn = "";
    private HashMap<String, YKGBLEDevice> mDevices = new HashMap();
    private YKGLockerDevice currentDevice;
    private boolean autoReconnect;
    private Context context;
    private int connState;
    private int juraState;
    private int rcState;
    private JCBLEManagerListener bleManagerListener;

    public YKGLockerManager() {
        this.connState = JCLockerConnectStateDisconnect;
        this.bleManagerListener = new JCBLEManagerListener() {
            public void bleManagerDidDiscoverDevice(YKGBLEDevice device) {
                YKGUtils.logi(device.toString());
                if (device.sn.startsWith("01")) {
                    YKGLockerDevice locker = new YKGLockerDevice();
                    locker.name = device.name;
                    locker.sn = device.sn;
                    locker.battery = device.battery;
                    locker.rssi = device.rssi;
                    locker.version = device.version;
                    locker.bleDevice = device;
                    if (locker.sn.equalsIgnoreCase(YKGLockerManager.this.targetSn)) {
                        if (YKGLockerManager.this.currentDevice == null) {
                            YKGLockerManager.this.currentDevice = locker;
                            YKGLockerManager.this.bleManager.connect(YKGLockerManager.this.currentDevice.bleDevice);
                            YKGLockerManager.this.connState = YKGLockerManager.JCLockerConnectStateConnecting;
                        } else if (YKGLockerManager.this.autoReconnect && YKGLockerManager.this.connState == YKGLockerManager.JCLockerConnectStateConnecting) {
                            YKGLockerManager.this.bleManager.connect(device);
                        }
                    }
                    if (YKGLockerManager.mLockerManager.getListener() != null) {
                        YKGLockerManager.mLockerManager.getListener().lockerManagerDidDiscoverDevice(locker);
                    }

                }
            }

            public void bleManagerDidConnectDevice(YKGBLEDevice device) {
                YKGLockerManager.this.connState = YKGLockerManager.JCLockerConnectStateConnected;
                if (YKGLockerManager.this.currentDevice != null && YKGLockerManager.this.targetSn.equals(YKGLockerManager.this.currentDevice.sn) && YKGLockerManager.this.currentDevice.sn.equals(device.sn) && YKGLockerManager.mLockerManager.getListener() != null) {
                    YKGLockerManager.mLockerManager.getListener().lockerManagerDidConnectDevice(YKGLockerManager.this.currentDevice);
                }

            }

            public void bleManagerDidDisConnectDevice(YKGBLEDevice device, int errorCode) {
                YKGLockerManager.this.connState = YKGLockerManager.JCLockerConnectStateDisconnect;
                if (YKGLockerManager.this.currentDevice != null && YKGLockerManager.this.targetSn.equals(YKGLockerManager.this.currentDevice.sn) && YKGLockerManager.this.currentDevice.sn.equals(device.sn) && YKGLockerManager.mLockerManager.getListener() != null) {
                    YKGLockerManager.mLockerManager.getListener().lockerManagerDidDisConnectDevice(YKGLockerManager.this.currentDevice, errorCode);
                    if (!YKGLockerManager.this.autoReconnect) {
                        YKGLockerManager.this.currentDevice = null;
                        YKGLockerManager.this.targetSn = "";
                    } else {
                        YKGBLEDevice tempBle = YKGLockerManager.this.bleManager.getDevice(YKGLockerManager.this.targetSn);
                        if (tempBle != null) {
                            YKGLockerDevice locker = new YKGLockerDevice();
                            locker.name = tempBle.name;
                            locker.sn = tempBle.sn;
                            locker.battery = tempBle.battery;
                            locker.rssi = tempBle.rssi;
                            locker.version = tempBle.version;
                            locker.bleDevice = tempBle;
                            YKGLockerManager.this.currentDevice = locker;
                            YKGLockerManager.this.bleManager.connect(locker.bleDevice);
                            YKGLockerManager.this.connState = YKGLockerManager.JCLockerConnectStateConnecting;
                        } else {
                            YKGLockerManager.this.currentDevice = null;
                        }
                    }
                }

            }

            public void bleManagerDidRecieveResponse(YKGBLEDevice device, YKGBLEResponse response) {
                byte[] value;
                byte tempValue;
                if (response.getCharacterUUID().equalsIgnoreCase("f000eff2-0451-4000-b000-000000000000")) {
                    value = response.getData();
                    if (value.length > 0) {
                        tempValue = value[0];
                        YKGLockerManager.this.bleManager.readRssi();
                        if (YKGLockerManager.mLockerManager.getExtendListener() != null) {
                            YKGLockerManager.mLockerManager.getExtendListener().lockerManagerDidUpdateState(YKGLockerManager.this.currentDevice, tempValue);
                        }
                    }
                } else if (response.getCharacterUUID().equalsIgnoreCase("f000eff3-0451-4000-b000-000000000000")) {
                    value = response.getData();
                    if (value.length > 0) {
                        tempValue = value[0];
                        YKGLockerManager.this.rcState = tempValue & 1;
                    }
                } else if (response.getCharacterUUID().equalsIgnoreCase("f000eff4-0451-4000-b000-000000000000")) {
                    value = response.getData();
                    if (value.length > 0) {
                        tempValue = value[0];
                        if (YKGLockerManager.mLockerManager.getExtendListener() != null) {
                            YKGLockerManager.mLockerManager.getExtendListener().lockerManagerDidUpdateBattery(YKGLockerManager.this.currentDevice, tempValue);
                        }
                    }
                } else if (device.sn.equals(YKGLockerManager.this.targetSn) && response.getType() == YKGBLEResponse.JCBLEResponseType_F1) {
//                    byte commandTypex = false;
                    byte[] values = response.getData();
                    if (values.length > 0) {
                        byte commandType = (byte) (values[0] & 255);
                        boolean min;
                        boolean maxx;
                        byte status;
                        byte max;
                        if (commandType == 3) {
                            if (values.length == 3) {
                                min = false;
                                maxx = false;
                                status = values[1];
                                max = values[2];
                                int lockState = status & 7;
                                if (null != YKGLockerManager.mLockerManager.getExtendListener()) {
                                    YKGLockerManager.mLockerManager.getExtendListener().lockerManagerDidUpdateState(YKGLockerManager.this.currentDevice, lockState);
                                    YKGLockerManager.mLockerManager.getExtendListener().lockerManagerDidUpdateBattery(YKGLockerManager.this.currentDevice, max);
                                }

                                YKGLockerManager.this.bleManager.readRssi();
                                if (YKGUtils.compareVersion(YKGLockerManager.this.currentDevice.version, "2.1") != -1) {
                                    int magneticisOn = (status & 64) >> 6;
                                    int magneticState = (status & 56) >> 3;
                                    if (null != YKGLockerManager.mLockerManager.getExtendListener()) {
                                        YKGLockerManager.mLockerManager.getExtendListener().lockerManagerDidUpdateMagneticState(YKGLockerManager.this.currentDevice, magneticisOn == 1, magneticState);
                                    }
                                }
                            }
                        } else if (commandType == 8) {
                            if (values.length == 3) {
                                min = false;
                                maxx = false;
                                status = values[1];
                                max = values[2];
                                if (YKGLockerManager.mLockerManager.getExtendListener() != null) {
                                    YKGLockerManager.mLockerManager.getExtendListener().lockerManagerDidUpdateThreshold(YKGLockerManager.this.currentDevice, status, max);
                                }
                            }
                        } else if (commandType == 11 && values.length == 3) {
                            min = false;
                            status = values[2];
                            if (YKGLockerManager.mLockerManager.getExtendListener() != null) {
                                YKGLockerManager.mLockerManager.getExtendListener().lockerManagerDidUpdateAutoFunctionTime(YKGLockerManager.this.currentDevice, status);
                            }
                        }
                    }
                }

            }

            public void bleManagerDidUpdateRssi(YKGBLEDevice device, int rssi) {
                if (YKGLockerManager.mLockerManager.getExtendListener() != null) {
                    YKGLockerManager.mLockerManager.getExtendListener().lockerManagerDidUpdateRssi(YKGLockerManager.this.currentDevice, rssi);
                }

            }
        };
    }

    public static YKGLockerManager getInstance(Context context) {
        if (mLockerManager == null) {
            Class var1 = YKGLockerManager.class;
            synchronized (YKGLockerManager.class) {
                mLockerManager = new YKGLockerManager();
                mLockerManager.setBleManager(YKGBLEManager.getInstance(context));
                mLockerManager.context = context;
                mLockerManager.checkSignature();
            }
        }

        return mLockerManager;
    }

    public void setListener(YKGLockerManager.YKGLockerManagerListener listener) {
        this.listener = listener;
    }

    public void setExtendListener(YKGLockerManager.YKGLockerManagerExtendListener extendListener) {
        this.extendListener = extendListener;
    }

    public void startScan() {
        ArrayList<String> list = new ArrayList();
        list.add("01");
        this.bleManager.startScan(list);
    }

    public void stopScan() {
        this.bleManager.stopScan();
    }

    public boolean isConnected(String sn) {
        return this.currentDevice != null && this.currentDevice.sn.equals(sn) && this.connState == JCLockerConnectStateConnected && this.bleManager.isConnected(this.currentDevice.bleDevice);
    }

    public void registerDevice(String sn) {
        this.registerDevicePri(sn);
    }

    public void deregisterDevice(String sn) {
        this.deregisterDevicePri(sn);
    }

    public void connectDevice(String sn, boolean autoReconnect) {
        if (this.currentDevice != null) {
            if (this.currentDevice.sn.equals(sn)) {
                if (this.connState == JCLockerConnectStateConnected && this.getListener() != null) {
                    this.getListener().lockerManagerDidConnectDevice(this.currentDevice);
                    return;
                }
            } else {
                this.disconnect();
            }
        }

        this.targetSn = this.checkSN(sn);
        if (this.targetSn != null) {
            YKGBLEDevice device = this.bleManager.getDevice(this.targetSn);
            this.autoReconnect = autoReconnect;
            if (device != null) {
                YKGLockerDevice locker = new YKGLockerDevice();
                locker.name = device.name;
                locker.sn = device.sn;
                locker.battery = device.battery;
                locker.rssi = device.rssi;
                locker.version = device.version;
                locker.bleDevice = device;
                this.currentDevice = locker;
                this.connState = JCLockerConnectStateConnecting;
                this.bleManager.connect(locker.bleDevice);
                this.startScan();
            } else {
                this.startScan();
            }

        }
    }

    public void disconnect() {
        boolean hasConnect = false;
        if (this.currentDevice != null && this.isConnected(this.currentDevice.sn)) {
            hasConnect = true;
        }

        this.targetSn = "";
        this.autoReconnect = false;
        this.currentDevice = null;
        this.bleManager.disconnect();
        if (hasConnect) {
            mLockerManager.getListener().lockerManagerDidDisConnectDevice(this.currentDevice, 0);
        }

    }

    public void sendAction(int action) {
        if (this.currentDevice != null) {
            if (action != ActionDown && action != ActionUp) {
                YKGUtils.loge("action not found :" + action);
            } else {
                YKGBLEWriteRequest writeRequest;
                byte[] temp;
                if (this.currentDevice.version.equals("1.0")) {
                    writeRequest = new YKGBLEWriteRequest();
                    temp = new byte[]{(byte) (action + 6)};
                    writeRequest.setData(temp);
                    writeRequest.setServiceUUID("f000eff0-0451-4000-b000-000000000000");
                    writeRequest.setCharacterUUID("f000eff1-0451-4000-b000-000000000000");
                    this.bleManager.addRequest(writeRequest);
                } else {
                    writeRequest = new YKGBLEWriteRequest();
                    writeRequest.setType(YKGBLEWriteRequest.JCBLERequestType_F1);
                    temp = new byte[]{(byte) action, 0, 0};
                    writeRequest.setData(temp);
                    this.bleManager.addRequest(writeRequest);
                }
            }
        }

    }

    public void switchAutoFunction(boolean auto) {
        if (this.currentDevice != null && YKGUtils.compareVersion(this.currentDevice.version, "2.1") != -1) {
            YKGBLEWriteRequest writeRequest = new YKGBLEWriteRequest();
            writeRequest.setType(YKGBLEWriteRequest.JCBLERequestType_F1);
            byte[] temp = new byte[]{5, 0, (byte) (auto ? 1 : 0)};
            writeRequest.setData(temp);
            this.bleManager.addRequest(writeRequest);
        }

    }

    public void calibrationGeomagnetic() {
        if (this.currentDevice != null) {
            if (YKGUtils.compareVersion(this.currentDevice.version, "2.1") != -1) {
                YKGBLEWriteRequest writeRequest = new YKGBLEWriteRequest();
                writeRequest.setType(YKGBLEWriteRequest.JCBLERequestType_F1);
                byte[] temp = new byte[]{6, 0, 0};
                writeRequest.setData(temp);
                this.bleManager.addRequest(writeRequest);
            } else {
                YKGUtils.loge("当前版本不支持");
            }
        }

    }

    public void getThreshold() {
        if (this.currentDevice != null) {
            if (YKGUtils.compareVersion(this.currentDevice.version, "2.1") != -1) {
                YKGBLEWriteRequest writeRequest = new YKGBLEWriteRequest();
                writeRequest.setType(YKGBLEWriteRequest.JCBLERequestType_F1);
                byte[] temp = new byte[]{7, 0, 0};
                writeRequest.setData(temp);
                this.bleManager.addRequest(writeRequest);
            } else {
                YKGUtils.loge("当前版本不支持");
            }
        }

    }

    public void setThreshold(int min, int max) {
        if (this.currentDevice != null) {
            if (YKGUtils.compareVersion(this.currentDevice.version, "2.1") != -1) {
                if (max <= min) {
                    YKGUtils.loge("max必须大于min");
                    return;
                }

                YKGBLEWriteRequest writeRequest = new YKGBLEWriteRequest();
                writeRequest.setType(YKGBLEWriteRequest.JCBLERequestType_F1);
                byte[] temp = new byte[]{9, (byte) min, (byte) max};
                writeRequest.setData(temp);
                this.bleManager.addRequest(writeRequest);
            } else {
                YKGUtils.loge("当前版本不支持");
            }
        }

    }

    public void getAutoFunctionTime() {
        if (this.currentDevice != null) {
            if (YKGUtils.compareVersion(this.currentDevice.version, "2.1") != -1) {
                YKGBLEWriteRequest writeRequest = new YKGBLEWriteRequest();
                writeRequest.setType(YKGBLEWriteRequest.JCBLERequestType_F1);
                byte[] temp = new byte[]{10, 0, 0};
                writeRequest.setData(temp);
                this.bleManager.addRequest(writeRequest);
            } else {
                YKGUtils.loge("当前版本不支持");
            }
        }

    }

    public void setAutoFunctionTime(int seconds) {
        if (this.currentDevice != null) {
            if (YKGUtils.compareVersion(this.currentDevice.version, "2.1") != -1) {
                YKGBLEWriteRequest writeRequest = new YKGBLEWriteRequest();
                writeRequest.setType(YKGBLEWriteRequest.JCBLERequestType_F1);
                byte[] temp = new byte[]{12, 0, (byte) seconds};
                writeRequest.setData(temp);
                this.bleManager.addRequest(writeRequest);
            } else {
                YKGUtils.loge("当前版本不支持");
            }
        }

    }

    private String getTokenLocal() {
        SharedPreferences preferences = this.context.getSharedPreferences("JCLockerContent", 0);
        String token = preferences.getString("JCToken", "");
        return token.length() > 0 ? token : "";
    }

    //    @SuppressLint("WrongConstant")
    private void checkSignature() {
        try {
//            ApplicationInfo appInfo = this.context.getPackageManager().getApplicationInfo(this.context.getPackageName(), 128);
            ApplicationInfo appInfo = this.context.getPackageManager().getApplicationInfo(this.context.getPackageName(), PackageManager.GET_META_DATA);
            String access_key = appInfo.metaData.getString("com.ykg.api.access_key");
            String access_secret = appInfo.metaData.getString("com.ykg.api.access_secret");
            if (access_key != null && access_secret != null) {
                if (access_key.length() != 0 && access_secret.length() != 0) {
                    JSONObject jsonObject = new JSONObject();

                    try {
                        jsonObject.put("grant_type", "client_credentials");
                        jsonObject.put("access_key", access_key);
                        jsonObject.put("access_secret", access_secret);
                    } catch (JSONException var6) {
                        var6.printStackTrace();
                    }

                    (new YKGLockerManager.signatureAsyncTask()).execute(new JSONObject[]{jsonObject});
                } else {
                    YKGUtils.loge("请在AndroidManifest中填入自己在平台申请的KEY进行鉴权,否则不能对车位锁进行控制操作!");
                    Toast.makeText(this.context, "请在AndroidManifest中填入自己在平台申请的KEY进行鉴权,否则不能对车位锁进行控制操作!", Toast.LENGTH_SHORT).show();
                }
            } else {
                YKGUtils.loge("请在AndroidManifest中填入自己在平台申请的KEY进行鉴权,否则不能对车位锁进行控制操作!");
                Toast.makeText(this.context, "请在AndroidManifest中填入自己在平台申请的KEY进行鉴权,否则不能对车位锁进行控制操作!", Toast.LENGTH_SHORT).show();
            }
        } catch (NameNotFoundException var7) {
            var7.printStackTrace();
        }
    }

    private void registerDevicePri(String sn) {
        if (this.getTokenLocal() != null) {
            SharedPreferences preferences = this.context.getSharedPreferences("JCLockerContent", 0);
            boolean ret = preferences.getBoolean(sn, false);
            if (ret) {
                YKGUtils.loge("------ 设备已注册！--------");
                if (this.getListener() != null) {
                    this.getListener().lockerManagerDidRegisterDevice(true);
                }
            } else if (this.checkSN(sn) != null) {
                YKGUtils.loge("------ 设备已注册！--------");
                if (this.getListener() != null) {
                    this.getListener().lockerManagerDidRegisterDevice(true);
                }
            } else {
                (new YKGLockerManager.devicesAsyncTask()).execute(new String[]{sn});
            }
        } else {
            YKGUtils.loge("---- 未鉴权，请先初始化SDK进行鉴权-------");
        }

    }

    private void deregisterDevicePri(String sn) {
        SharedPreferences preferences = this.context.getSharedPreferences("JCLockerContent", 0);
        boolean id = preferences.getBoolean(sn, false);
        if (id) {
            preferences.edit().remove(sn).commit();
        }

    }

    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;

        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "/n");
            }
        } catch (IOException var14) {
            var14.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException var13) {
                var13.printStackTrace();
            }

        }

        return sb.toString();
    }

    private String getDeviceConnect(String sn, String token) {
        String result = null;
        InputStream stream = null;
        InputStreamReader inReader = null;
        HttpURLConnection conn = null;

        Object var8;
        try {
            URL url = new URL(this.getHost() + "/devices/" + sn);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(12000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            if (token != null) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            int statusCode = conn.getResponseCode();
            if (statusCode == 200) {
                result = "";
                stream = conn.getInputStream();
                inReader = new InputStreamReader(stream);
                BufferedReader buffer = new BufferedReader(inReader);

                for (String strLine = null; (strLine = buffer.readLine()) != null; result = result + strLine) {
                    ;
                }
            } else {
                YKGUtils.loge("注册设备请求返回code：" + statusCode);
            }

            String var30 = result;
            return var30;
        } catch (MalformedURLException var26) {
            YKGUtils.loge("getFromWebByHttpUrlCOnnection:" + var26.getMessage());
            var26.printStackTrace();
            var8 = null;
        } catch (IOException var27) {
            YKGUtils.loge("getFromWebByHttpUrlCOnnection:" + var27.getMessage());
            var27.printStackTrace();
            var8 = null;
            return (String) var8;
        } finally {
            if (inReader != null) {
                try {
                    inReader.close();
                } catch (IOException var25) {
                    var25.printStackTrace();
                }
            }

            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException var24) {
                    var24.printStackTrace();
                }
            }

            if (conn != null) {
                conn.disconnect();
            }

        }

        return (String) var8;
    }

    private String postUrlConnect(String strUrl, String params) {
        String result = null;
        InputStream inputStream = null;
        InputStreamReader inStream = null;
        HttpURLConnection conn = null;

        DataOutputStream wr;
        try {
            URL url = new URL(strUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(60000);
            conn.setReadTimeout(60000);
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(params);
            wr.flush();
            wr.close();
            int statusCode = conn.getResponseCode();
            YKGUtils.loge("鉴权请求返回code：" + statusCode);
            if (statusCode == 200) {
                result = "";
                inputStream = conn.getInputStream();
                inStream = new InputStreamReader(inputStream);
                BufferedReader buffer = new BufferedReader(inStream);

                for (String strLine = null; (strLine = buffer.readLine()) != null; result = result + strLine) {
                    ;
                }
            }

            String var26 = result;
            return var26;
        } catch (IOException var24) {
            YKGUtils.loge("PostFromWebByHttpURLConnection：" + var24.getMessage());
            var24.printStackTrace();
            wr = null;
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException var23) {
                    var23.printStackTrace();
                }
            }

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException var22) {
                    var22.printStackTrace();
                }
            }

            if (conn != null) {
                conn.disconnect();
            }

        }

        return String.valueOf(wr);
    }

    private void setBleManager(YKGBLEManager bleManager) {
        if (this.bleManager == null && bleManager != null) {
            bleManager.addListener(this.bleManagerListener);
        }

        this.bleManager = bleManager;
    }

    private String getHost() {
        return this.JCLockerDisHost;
    }

    private YKGLockerManager.YKGLockerManagerListener getListener() {
        if (this.listener == null) {
            return null;
        } else {
            YKGLockerManager.YKGLockerManagerListener listener = this.listener;
            return listener;
        }
    }

    private YKGLockerManager.YKGLockerManagerExtendListener getExtendListener() {
        if (this.extendListener == null) {
            return null;
        } else {
            YKGLockerManager.YKGLockerManagerExtendListener extendListener = this.extendListener;
            YKGUtils.logi("extendListener " + extendListener);
            return extendListener;
        }
    }

    //    @SuppressLint("WrongConstant")
    private String checkSN(String sn) {
        SharedPreferences preferences = this.context.getSharedPreferences("JCLockerContent", 0);
        boolean contains = preferences.getBoolean(sn, false);
        if (contains) {
            return sn;
        } else {
            ApplicationInfo appInfo = null;

            try {
                appInfo = this.context.getPackageManager().getApplicationInfo(this.context.getPackageName(), PackageManager.GET_META_DATA);
                String access_key = appInfo.metaData.getString("com.ykg.api.access_key");
                if (access_key != null && (this.context.getPackageName().equals("com.ykg.ykgsdkdemo") || access_key.equals("439395f54ca04dd3"))) {
                    return sn;
                }
            } catch (NameNotFoundException var6) {
                var6.printStackTrace();
            }

            YKGUtils.loge("设备尚未注册");
            return null;
        }
    }

    class devicesAsyncTask extends AsyncTask<String, Void, Boolean> {
        devicesAsyncTask() {
        }

        protected Boolean doInBackground(String... params) {
            try {
                String result = YKGLockerManager.this.getDeviceConnect(params[0], YKGLockerManager.this.getTokenLocal());
                if (result != null && !result.equals("")) {
                    JSONObject jsonObject = new JSONObject(result);
                    boolean error = jsonObject.getBoolean("error");
                    if (!error) {
                        SharedPreferences preferences = YKGLockerManager.this.context.getSharedPreferences("JCLockerContent", 0);
                        preferences.edit().putBoolean(params[0], true).commit();
                        return true;
                    }

                    YKGUtils.loge(jsonObject.toString());
                }
            } catch (Exception var6) {
                var6.printStackTrace();
            }

            return false;
        }

        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean) {
                if (YKGLockerManager.this.getListener() != null) {
                    YKGLockerManager.this.getListener().lockerManagerDidRegisterDevice(true);
                }
            } else {
                if (YKGLockerManager.this.getListener() != null) {
                    YKGLockerManager.this.getListener().lockerManagerDidRegisterDevice(false);
                }

                YKGUtils.loge("------ 注册设备失败，您没有权限使用该车位锁！");
            }

        }
    }

    class signatureAsyncTask extends AsyncTask<JSONObject, Void, Boolean> {
        signatureAsyncTask() {
        }

        protected Boolean doInBackground(JSONObject... params) {
            String url = YKGLockerManager.this.getHost();

            try {
                String result = YKGLockerManager.this.postUrlConnect(url + "/auth/token", params[0].toString());
                if (result != null && !result.equals("")) {
                    JSONObject jsonObject = new JSONObject(result);
                    boolean error = jsonObject.getBoolean("error");
                    if (!error) {
                        JSONObject jsonObject1 = jsonObject.getJSONObject("payload");
                        String access_token = jsonObject1.getString("access_token");
                        SharedPreferences preferences = YKGLockerManager.this.context.getSharedPreferences("JCLockerContent", 0);
                        Editor editor = preferences.edit();
                        editor.putString("JCToken", access_token);
                        editor.commit();
                        return true;
                    }
                }
            } catch (Exception var10) {
                var10.printStackTrace();
            }

            return false;
        }

        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean) {
                YKGUtils.loge("------ 鉴权成功！--------");
            } else {
                YKGUtils.loge("------ 鉴权失败，请输入正确的accessKey、secretKey！");
                YKGLockerManager.this.checkSignature();
            }

        }
    }

    public interface YKGLockerManagerExtendListener {
        void lockerManagerDidUpdateBattery(YKGLockerDevice var1, int var2);

        void lockerManagerDidUpdateState(YKGLockerDevice var1, int var2);

        void lockerManagerDidUpdateMagneticState(YKGLockerDevice var1, boolean var2, int var3);

        void lockerManagerDidUpdateRssi(YKGLockerDevice var1, int var2);

        void lockerManagerDidUpdateThreshold(YKGLockerDevice var1, int var2, int var3);

        void lockerManagerDidUpdateAutoFunctionTime(YKGLockerDevice var1, int var2);
    }

    public interface YKGLockerManagerListener {
        void lockerManagerDidRegisterDevice(boolean var1);

        void lockerManagerDidDiscoverDevice(YKGLockerDevice var1);

        void lockerManagerDidConnectDevice(YKGLockerDevice var1);

        void lockerManagerDidDisConnectDevice(YKGLockerDevice var1, int var2);
    }
}
