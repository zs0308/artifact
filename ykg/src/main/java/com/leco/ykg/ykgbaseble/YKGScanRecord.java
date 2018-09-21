package com.leco.ykg.ykgbaseble;

import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by zs on 2018/7/9.
 */
class YKGScanRecord {
    private static final String TAG = "ScanRecordD";
    private static final int DATA_TYPE_FLAGS = 1;
    private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL = 2;
    private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE = 3;
    private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL = 4;
    private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE = 5;
    private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL = 6;
    private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE = 7;
    private static final int DATA_TYPE_LOCAL_NAME_SHORT = 8;
    private static final int DATA_TYPE_LOCAL_NAME_COMPLETE = 9;
    private static final int DATA_TYPE_TX_POWER_LEVEL = 10;
    private static final int DATA_TYPE_SERVICE_DATA = 22;
    private static final int DATA_TYPE_MANUFACTURER_SPECIFIC_DATA = 255;
    private final int mAdvertiseFlags;
    @Nullable
    private final List<ParcelUuid> mServiceUuids;
    private final byte[] mManufacturerSpecificData;
    private final Map<ParcelUuid, byte[]> mServiceData;
    private final int mTxPowerLevel;
    private final String mDeviceName;
    private final byte[] mBytes;

    protected int getAdvertiseFlags() {
        return this.mAdvertiseFlags;
    }

    protected List<ParcelUuid> getServiceUuids() {
        return this.mServiceUuids;
    }

    protected JSONArray getServiceJsonArray() {
        JSONArray array = new JSONArray();
        if (null != this.mServiceUuids) {
            Iterator var2 = this.mServiceUuids.iterator();

            while (var2.hasNext()) {
                ParcelUuid uuid = (ParcelUuid) var2.next();
                array.put(uuid.toString());
            }
        }

        return array;
    }

    protected byte[] getManufacturerSpecificData() {
        return this.mManufacturerSpecificData;
    }

    protected Map<ParcelUuid, byte[]> getServiceData() {
        return this.mServiceData;
    }

    @Nullable
    protected byte[] getServiceData(ParcelUuid serviceDataUuid) {
        return serviceDataUuid == null ? null : (byte[]) this.mServiceData.get(serviceDataUuid);
    }

    protected int getTxPowerLevel() {
        return this.mTxPowerLevel;
    }

    @Nullable
    protected String getDeviceName() {
        return this.mDeviceName;
    }

    protected byte[] getBytes() {
        return this.mBytes;
    }

    private YKGScanRecord(List<ParcelUuid> serviceUuids, byte[] manufacturerData, Map<ParcelUuid, byte[]> serviceData, int advertiseFlags, int txPowerLevel, String localName, byte[] bytes) {
        this.mServiceUuids = serviceUuids;
        this.mManufacturerSpecificData = manufacturerData;
        this.mServiceData = serviceData;
        this.mDeviceName = localName;
        this.mAdvertiseFlags = advertiseFlags;
        this.mTxPowerLevel = txPowerLevel;
        this.mBytes = bytes;
    }

    protected static YKGScanRecord parseFromBytes(byte[] ScanRecordD) {
        if (ScanRecordD == null) {
            return null;
        } else {
            int currentPos = 0;
            int advertiseFlag = -1;
            List<ParcelUuid> serviceUuids = new ArrayList();
            String localName = null;
            int txPowerLevel = -2147483648;
            byte[] manufacturerData = null;
            HashMap serviceData = new HashMap();

            try {
                int dataLength;
                for (; currentPos < ScanRecordD.length; currentPos += dataLength) {
                    int length = ScanRecordD[currentPos++] & 255;
                    if (length == 0) {
                        break;
                    }

                    dataLength = length - 1;
                    int fieldType = ScanRecordD[currentPos++] & 255;
                    switch (fieldType) {
                        case 1:
                            advertiseFlag = ScanRecordD[currentPos] & 255;
                            break;
                        case 2:
                        case 3:
                            parseServiceUuid(ScanRecordD, currentPos, dataLength, 2, serviceUuids);
                            break;
                        case 4:
                        case 5:
                            parseServiceUuid(ScanRecordD, currentPos, dataLength, 4, serviceUuids);
                            break;
                        case 6:
                        case 7:
                            parseServiceUuid(ScanRecordD, currentPos, dataLength, 16, serviceUuids);
                            break;
                        case 8:
                        case 9:
                            localName = new String(extractBytes(ScanRecordD, currentPos, dataLength));
                            break;
                        case 10:
                            txPowerLevel = ScanRecordD[currentPos];
                            break;
                        case 22:
                            int serviceUuidLength = 2;
                            byte[] serviceDataUuidBytes = extractBytes(ScanRecordD, currentPos, serviceUuidLength);
                            ParcelUuid serviceDataUuid = YKGBluetoothUuid.parseUuidFrom(serviceDataUuidBytes);
                            byte[] serviceDataArray = extractBytes(ScanRecordD, currentPos + serviceUuidLength, dataLength - serviceUuidLength);
                            serviceData.put(serviceDataUuid, serviceDataArray);
                            break;
                        case 255:
                            manufacturerData = extractBytes(ScanRecordD, currentPos, dataLength);
                    }
                }

                if (serviceUuids.isEmpty()) {
                    serviceUuids = null;
                }

                return new YKGScanRecord(serviceUuids, manufacturerData, serviceData, advertiseFlag, txPowerLevel, localName, ScanRecordD);
            } catch (Exception var15) {
                Log.e("ScanRecordD", "unable to parse scan record: " + Arrays.toString(ScanRecordD));
                return new YKGScanRecord((List) null, (byte[]) null, (Map) null, -1, -2147483648, (String) null, ScanRecordD);
            }
        }
    }

    private static int parseServiceUuid(byte[] ScanRecordD, int currentPos, int dataLength, int uuidLength, List<ParcelUuid> serviceUuids) {
        while (dataLength > 0) {
            byte[] uuidBytes = extractBytes(ScanRecordD, currentPos, uuidLength);
            serviceUuids.add(YKGBluetoothUuid.parseUuidFrom(uuidBytes));
            dataLength -= uuidLength;
            currentPos += uuidLength;
        }

        return currentPos;
    }

    private static byte[] extractBytes(byte[] ScanRecordD, int start, int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(ScanRecordD, start, bytes, 0, length);
        return bytes;
    }

}
