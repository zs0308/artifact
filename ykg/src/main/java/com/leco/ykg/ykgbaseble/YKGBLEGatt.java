package com.leco.ykg.ykgbaseble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;
import com.leco.ykg.ykgbaseble.YKGUtils.JCMD5;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static android.content.Context.BLUETOOTH_SERVICE;

/**
 * Created by zs on 2018/7/9.
 */
 class YKGBLEGatt extends BluetoothGattCallback {
    private static final String auth_service_uuid = "f000dff0-0451-4000-b000-000000000000";
    private static final String auth_character_uuid = "f000dff1-0451-4000-b000-000000000000";
    private static final String io_service_uuid = "f000fff0-0451-4000-b000-000000000000";
    private static final String write_character_uuid = "f000fff1-0451-4000-b000-000000000000";
    private static final String read_character_uuid = "f000fff2-0451-4000-b000-000000000000";
    static final String ControlServiceUUIDStringOld = "f000eff0-0451-4000-b000-000000000000";
    static final String ControlCharacteristicUUIDStringOld = "f000eff1-0451-4000-b000-000000000000";
    static final String StateCharacteristicUUIDStringOld = "f000eff2-0451-4000-b000-000000000000";
    static final String JuraStateCharacteristicUUIDStringOld = "f000eff3-0451-4000-b000-000000000000";
    static final String PowerCharacteristicUUIDStringOld = "f000eff4-0451-4000-b000-000000000000";
    private static byte SubPacketType_None = 0;
    private static byte SubPacketType_Start = 1;
    private static byte SubPacketType_Stop = 2;
    private boolean mBusy = false;
    private Context _context;
    private BluetoothAdapter mBluetoothAdapter;
    private YKGBLEDevice mCurrentDevice;
    private YKGBLEDevice mTargetDevice;
    private BluetoothGatt mBluetoothGatt;
    private YKGBLEGatt.JCBLEGattListener listener;
    private HashMap<String, BluetoothGattCharacteristic> mCharacters = new HashMap();
    private ArrayList<YKGBLERequest> mRequests = new ArrayList();
    private YKGBLERequest currentRequest;
    private boolean isActive = false;
    private int mConnectionState = 0;
    protected static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private byte[] blocks = new byte[0];
    private static YKGBLEGatt mBleGatt;

    YKGBLEGatt() {
    }

    static YKGBLEGatt getInstance(Context context, BluetoothAdapter bluetoothAdapter) {
        if (mBleGatt == null) {
            Class var2 = YKGBLEGatt.class;
            synchronized(YKGBLEGatt.class) {
                if (mBleGatt == null) {
                    mBleGatt = new YKGBLEGatt();
                    mBleGatt.mBluetoothAdapter = bluetoothAdapter;
                }
            }
        }

        if (mBleGatt._context != null) {
            if (mBleGatt._context != context) {
                mBleGatt._context = context;
            }
        } else {
            mBleGatt._context = context;
        }

        return mBleGatt;
    }

    public BluetoothAdapter getmBluetoothAdapter() {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return this.mBluetoothAdapter;
    }

    public void setListener(YKGBLEGatt.JCBLEGattListener listener) {
        this.listener = listener;
    }

    public void setmBusy(boolean mBusy) {
        Log.i("mBusy", YKGUtils.getLineInfo(2));
        this.mBusy = mBusy;
    }

    public boolean connect(YKGBLEDevice device) {
        boolean ret = this.connectPriv(device);
        if (!ret) {
            YKGUtils.loge("connect false");
            if (this.listener != null) {
                this.listener.bleGattDidDisConnectDevice(device, 0);
            }
        }

        return ret;
    }

    public void disconnect() {
        if (this.mBluetoothAdapter == null) {
            YKGUtils.loge("BluetoothAdapter not initialized");
        } else {
            if (this.mBluetoothGatt != null) {
                YKGUtils.logi("disconnect");
                this.mBluetoothGatt.disconnect();
            }

            this.isActive = false;
        }
    }

    public void refreshDeviceCache() {
        if (this.mBluetoothAdapter != null && this.mBluetoothGatt != null) {
            try {
                BluetoothGatt localBluetoothGatt = this.mBluetoothGatt;
                Method localMethod = localBluetoothGatt.getClass().getMethod("refresh");
                if (localMethod != null) {
                    boolean var3 = (Boolean)localMethod.invoke(localBluetoothGatt);
                }
            } catch (Exception var4) {
                YKGUtils.loge("An exception occured while refreshing device");
            }

        } else {
            YKGUtils.loge("BluetoothAdapter not initialized");
        }
    }

    public boolean isConnected(YKGBLEDevice device) {
        if (device == null) {
            return false;
        } else if (device.bluetoothDevice == null) {
            return false;
        } else if (device.bluetoothDevice.getAddress() == null) {
            return false;
        } else {
//            BluetoothManager manager = (BluetoothManager)this._context.getSystemService("bluetooth");
            BluetoothManager manager = (BluetoothManager)this._context.getSystemService(BLUETOOTH_SERVICE);
            List<BluetoothDevice> connectedDevices = manager.getConnectedDevices(7);
            Iterator var4 = connectedDevices.iterator();

            BluetoothDevice connectedDevice;
            do {
                if (!var4.hasNext()) {
                    return false;
                }

                connectedDevice = (BluetoothDevice)var4.next();
            } while(!device.bluetoothDevice.getAddress().equalsIgnoreCase(connectedDevice.getAddress()));

            return true;
        }
    }

    public void close() {
        this.isActive = false;
        if (this.mBluetoothGatt != null) {
            if (this.mCurrentDevice != null) {
                BluetoothManager bluetoothManager = (BluetoothManager)this._context.getSystemService(BLUETOOTH_SERVICE);
                if (bluetoothManager.getConnectionState(this.mCurrentDevice.bluetoothDevice, 7) != 0) {
                    this.listener.bleGattDidDisConnectDevice(this.mCurrentDevice, 0);
                }
            }

            this.mBluetoothGatt.close();
            this.mBluetoothGatt = null;
            this.mRequests.clear();
        }
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        return this.mBluetoothGatt == null ? null : this.mBluetoothGatt.getServices();
    }

    public boolean readRssi() {
        return this.mBluetoothGatt == null ? false : this.mBluetoothGatt.readRemoteRssi();
    }

    public BluetoothGattCharacteristic getCharacteristerstic(String service_uuid, String char_uuid) {
        BluetoothGattCharacteristic characteristic = null;
        List servicesList = this.getSupportedGattServices();

        try {
            for(int i = 0; i < servicesList.size(); ++i) {
                int characteristic_size = ((BluetoothGattService)servicesList.get(i)).getCharacteristics().size();
                if (((BluetoothGattService)servicesList.get(i)).getUuid().toString().equalsIgnoreCase(service_uuid)) {
                    for(int j = 0; j < characteristic_size; ++j) {
                        UUID characteristic_uuid = ((BluetoothGattCharacteristic)((BluetoothGattService)servicesList.get(i)).getCharacteristics().get(j)).getUuid();
                        if (characteristic_uuid.toString().equalsIgnoreCase(char_uuid)) {
                            characteristic = (BluetoothGattCharacteristic)((BluetoothGattService)servicesList.get(i)).getCharacteristics().get(j);
                        }
                    }
                }
            }
        } catch (Exception var9) {
            ;
        }

        return characteristic;
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (this.mBluetoothAdapter != null && this.mBluetoothGatt != null) {
            if (characteristic == null) {
                YKGUtils.loge("null char");
            }

            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
            if (descriptor != null) {
                byte[] value = enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                descriptor.setValue(value);
                this.setmBusy(true);
                this.mBluetoothGatt.writeDescriptor(descriptor);
                boolean ret = this.mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
                YKGUtils.logi("setnotify ret " + ret);
            }

        } else {
            YKGUtils.loge("BluetoothAdapter not initialized");
        }
    }

    private boolean connectPriv(YKGBLEDevice device) {
        if (this.mBluetoothAdapter != null && device != null && device.bluetoothDevice != null) {
            this.mTargetDevice = device;
            if (this.mCurrentDevice != null && device.bluetoothDevice.getAddress().equals(this.mCurrentDevice.bluetoothDevice.getAddress())) {
                if (this.isConnected(device)) {
                    this.listener.bleGattDidConnectDevice(device);
                    return true;
                }
            } else if (this.mCurrentDevice != null) {
                this.close();
            }

            BluetoothDevice bledevice = this.getmBluetoothAdapter().getRemoteDevice(device.bluetoothDevice.getAddress());
            if (bledevice == null) {
                bledevice = device.bluetoothDevice;
            }

            this.isActive = false;
            this.mBluetoothGatt = bledevice.connectGatt(this._context, false, this);
            this.mCurrentDevice = device;
            this.mConnectionState = 1;
            return true;
        } else {
            YKGUtils.loge("BluetooothAdapter not initialize or unspecified address");
            return false;
        }
    }

    private byte[] getPassWord() {
        String sn = this.mCurrentDevice.sn;
        byte[] snByte;
        JCMD5 md5;
        byte[] md5Pwd;
        byte[] result;
        if (this.mCurrentDevice.version.equals("1.0") && this.mCurrentDevice.sn.startsWith("01")) {
            byte[] tempByte = YKGUtils.HexString2Bytes(sn, sn.length() / 2);
            byte[] strByte = "JParker".getBytes();
            snByte = new byte[tempByte.length + strByte.length];
            System.arraycopy(tempByte, 0, snByte, 0, tempByte.length);
            System.arraycopy(strByte, 0, snByte, tempByte.length, strByte.length);
            md5 = new JCMD5();
            md5Pwd = md5.getMD5ofByte(snByte);
            result = new byte[md5Pwd.length + 1];
            result[0] = (byte)md5Pwd.length;
            System.arraycopy(md5Pwd, 0, result, 1, md5Pwd.length);
            return result;
        } else {
            String hander = this.mCurrentDevice.sn.substring(0, 4);
            String authen = sn + hander;
            snByte = YKGUtils.HexString2Bytes(authen, authen.length() / 2);
            md5 = new JCMD5();
            md5Pwd = md5.getMD5ofByte(snByte);
            result = new byte[md5Pwd.length + 1];
            result[0] = (byte)md5Pwd.length;
            System.arraycopy(md5Pwd, 0, result, 1, md5Pwd.length);
            return result;
        }
    }

    private void authDevice() {
        if (null != this.mCurrentDevice) {
            YKGBLEWriteRequest writeRequest = new YKGBLEWriteRequest();
            writeRequest.setServiceUUID("f000dff0-0451-4000-b000-000000000000");
            writeRequest.setCharacterUUID("f000dff1-0451-4000-b000-000000000000");
            writeRequest.setData(this.getPassWord());
            YKGBLEReadRequest readRequest = new YKGBLEReadRequest();
            readRequest.setServiceUUID("f000dff0-0451-4000-b000-000000000000");
            readRequest.setCharacterUUID("f000dff1-0451-4000-b000-000000000000");
            synchronized(this) {
                this.mRequests.add(0, writeRequest);
                this.mRequests.add(1, readRequest);
            }

            this.sendRequest();
        }

    }

    public void addRequest(YKGBLERequest request) {
        if (this.isActive) {
            this.mRequests.add(request);
            this.sendRequest();
        }

    }

    private void sendRequest() {
        if (this.mRequests != null) {
            if (this.mRequests.size() != 0) {
                this.currentRequest = (YKGBLERequest)this.mRequests.get(0);
                if (this.isConnected(this.mCurrentDevice)) {
                    YKGUtils.logi("exeRequest");
                    this.exeRequest();
                } else {
                    this.connect(this.mCurrentDevice);
                }

            }
        }
    }

    private void exeRequest() {
        if (this.currentRequest != null) {
            if (!this.mBusy) {
                BluetoothGattCharacteristic character = this.getCharacteristerstic(this.currentRequest.getServiceUUID(), this.currentRequest.getCharacterUUID());
                if (null != character) {
                    if (this.currentRequest instanceof YKGBLEWriteRequest) {
                        YKGBLEWriteRequest writerequest = (YKGBLEWriteRequest)this.currentRequest;
                        if (writerequest.iblock >= writerequest.getNblock()) {
                            this.mRequests.remove(writerequest);
                            this.currentRequest = null;
                            this.sendRequest();
                            return;
                        }

                        byte[] data = writerequest.getBlock(writerequest.iblock, this.mCurrentDevice.version, this.mCurrentDevice.sn);
                        YKGUtils.logi("writeBegin " + YKGUtils.bytesToHexString(data));
                        character.setValue(data);
                        character.setWriteType(2);
                        boolean ret = this.mBluetoothGatt.writeCharacteristic(character);
                        YKGUtils.logi("write ret" + ret);
                        this.setmBusy(true);
                        YKGUtils.logi("write begin");
                        if (ret) {
                            ++writerequest.iblock;
                        } else {
                            this.setmBusy(false);
                            this.sendRequest();
                        }
                    } else {
                        YKGUtils.logi("read begin");
                        this.setmBusy(true);
                        boolean ret = this.mBluetoothGatt.readCharacteristic(character);
                        YKGUtils.logi("read ret :" + ret);
                        if (!ret) {
                            this.setmBusy(false);
                        }
                    }
                } else {
                    YKGUtils.logi("discoverServices");
                    this.mBluetoothGatt.discoverServices();
                }

            }
        }
    }

    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        String intentAction = null;
        YKGUtils.logi("status" + status + "     state" + newState);
        if (this.mBluetoothGatt == null) {
            gatt.close();
        } else {
            if (newState == 2) {
                this.authDevice();
                boolean var5 = this.mBluetoothGatt.discoverServices();
            } else if (newState == 0) {
                if (this.listener != null && this.mCurrentDevice != null) {
                    this.listener.bleGattDidDisConnectDevice(this.mCurrentDevice, status);
                }

                this.close();
            }

        }
    }

    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == 0) {
            Iterator var3 = gatt.getServices().iterator();

            while(var3.hasNext()) {
                BluetoothGattService service = (BluetoothGattService)var3.next();
                Iterator var5 = service.getCharacteristics().iterator();

                while(var5.hasNext()) {
                    BluetoothGattCharacteristic character = (BluetoothGattCharacteristic)var5.next();
                    this.mCharacters.put(character.getUuid().toString(), character);
                    if ((character.getProperties() & 16) == 16) {
                        this.setCharacteristicNotification(character, true);
                    }
                }
            }

            this.exeRequest();
        } else {
            YKGUtils.logi("onServicesDiscovered receiver:" + status);
        }

    }

    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        this.setmBusy(false);
        if (status == 0) {
            this.onCharacteristicChanged(gatt, characteristic);
        }

        if (this.currentRequest != null && this.currentRequest instanceof YKGBLEReadRequest) {
            this.mRequests.remove(this.currentRequest);
        }

        YKGUtils.logi("read end");
        this.sendRequest();
    }

    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        YKGUtils.logi("write end");
        this.setmBusy(false);
        if (status == 0) {
            YKGUtils.logi("CharacteristicWrite Success status" + status);
            if (this.currentRequest != null && this.currentRequest instanceof YKGBLEWriteRequest) {
                YKGBLEWriteRequest writeRequest = (YKGBLEWriteRequest)this.currentRequest;
                if (writeRequest.iblock >= writeRequest.getNblock()) {
                    this.mRequests.remove(this.currentRequest);
                }
            }
        } else {
            if (this.currentRequest != null && this.currentRequest instanceof YKGBLEWriteRequest) {
                this.mRequests.remove(this.currentRequest);
            }

            YKGUtils.logi("CharacteristicWrite Failure status" + status);
        }

        this.sendRequest();
    }

    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        this.setmBusy(false);
        YKGUtils.logi("descriptorWrite " + status);
        super.onDescriptorWrite(gatt, descriptor, status);
        this.sendRequest();
    }

    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        byte[] data;
        byte type;
        if (characteristic.getUuid().toString().equals("f000dff1-0451-4000-b000-000000000000")) {
            data = characteristic.getValue();
            if (data != null && data.length > 0) {
                type = data[0];
                if (type == 0) {
                    this.isActive = false;
                } else {
                    this.isActive = true;
                    this.listener.bleGattDidConnectDevice(this.mCurrentDevice);
                }

                if (this.mCurrentDevice.version.equals("1.0") && this.mCurrentDevice.sn.startsWith("01")) {
                    this.setCharacteristicNotification(this.getCharacteristerstic("f000eff0-0451-4000-b000-000000000000", "f000eff2-0451-4000-b000-000000000000"), true);
                    YKGBLEReadRequest powerrequest = new YKGBLEReadRequest();
                    powerrequest.setServiceUUID("f000eff0-0451-4000-b000-000000000000");
                    powerrequest.setCharacterUUID("f000eff4-0451-4000-b000-000000000000");
                    this.addRequest(powerrequest);
                    YKGBLEReadRequest staterequest = new YKGBLEReadRequest();
                    staterequest.setServiceUUID("f000eff0-0451-4000-b000-000000000000");
                    staterequest.setCharacterUUID("f000eff3-0451-4000-b000-000000000000");
                    this.addRequest(staterequest);
                } else {
                    this.setCharacteristicNotification(this.getCharacteristerstic("f000fff0-0451-4000-b000-000000000000", "f000fff2-0451-4000-b000-000000000000"), true);
                }
            }
        } else {
            data = characteristic.getValue();
            if (this.mCurrentDevice.version.equals("1.0") && this.mCurrentDevice.sn.startsWith("01")) {
                YKGBLEResponse response = YKGBLEResponse.init(data, this.mCurrentDevice.version);
                if (response != null) {
                    response.setServiceUUID(characteristic.getService().getUuid().toString());
                    response.setCharacterUUID(characteristic.getUuid().toString());
                    this.listener.bleGattDidRecieveResponse(this.mCurrentDevice, response);
                }
            } else if (data != null && data.length > 1) {
//                byte type = false;
                type = data[0];
                if ((type & SubPacketType_Start) == SubPacketType_Start) {
                    this.blocks = new byte[0];
                }

                byte[] tempblocks = new byte[this.blocks.length + data.length - 1];
                System.arraycopy(this.blocks, 0, tempblocks, 0, this.blocks.length);
                System.arraycopy(data, 1, tempblocks, this.blocks.length, data.length - 1);
                this.blocks = tempblocks;
                if ((type & SubPacketType_Stop) == SubPacketType_Stop) {
                    YKGBLEResponse response = YKGBLEResponse.init(this.blocks, (String)null);
                    if (response != null) {
                        response.setServiceUUID(characteristic.getService().getUuid().toString());
                        response.setCharacterUUID(characteristic.getUuid().toString());
                        this.listener.bleGattDidRecieveResponse(this.mCurrentDevice, response);
                    }
                }
            }
        }

    }

    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        if (this.listener != null) {
            this.listener.bleGattDidUpdateRssi(this.mCurrentDevice, rssi);
        }

    }

    public boolean waitIdle(int i) {
        i /= 10;

        while(true) {
            --i;
            if (i <= 0) {
                return i > 0;
            }

            try {
                Thread.sleep(10L);
            } catch (InterruptedException var3) {
                var3.printStackTrace();
            }
        }
    }

    interface JCBLEGattListener {
        void bleGattDidConnectDevice(YKGBLEDevice var1);

        void bleGattDidDisConnectDevice(YKGBLEDevice var1, int var2);

        void bleGattDidRecieveResponse(YKGBLEDevice var1, YKGBLEResponse var2);

        void bleGattDidUpdateRssi(YKGBLEDevice var1, int var2);
    }
}
