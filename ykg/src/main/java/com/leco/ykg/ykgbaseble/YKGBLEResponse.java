package com.leco.ykg.ykgbaseble;

import com.leco.ykg.ykgbaseble.YKGUtils.JCCrc8;
/**
 * Created by zs on 2018/7/9.
 */
public class YKGBLEResponse {
    public static int JCBLEResponseType_Unknown = 0;
    public static int JCBLEResponseType_SN = 1;
    public static int JCBLEResponseType_Version = 2;
    public static int JCBLEResponseType_Power = 3;
    public static int JCBLEResponseType_Beacon = 4;
    public static int JCBLEResponseType_JURA = 5;
    public static int JCBLEResponseType_Reset = 6;
    public static int JCBLEResponseType_Disconnect = 7;
    public static int JCBLEResponseType_Uart = 8;
    public static int JCBLEResponseType_F0 = 240;
    public static int JCBLEResponseType_F1 = 241;
    public static int JCBLEResponseType_F2 = 242;
    public static int JCBLEResponseType_F3 = 243;
    public static int JCBLEResponseType_F4 = 244;
    public static int JCBLEResponseType_F5 = 245;
    private int type;
    private byte[] data;
    private byte[] originalData;
    private String serviceUUID;
    private String characterUUID;
    static YKGBLEResponse response;

    public YKGBLEResponse() {
    }

    public void setServiceUUID(String serviceUUID) {
        this.serviceUUID = serviceUUID;
    }

    public String getServiceUUID() {
        return this.serviceUUID;
    }

    public void setCharacterUUID(String characterUUID) {
        this.characterUUID = characterUUID;
    }

    public String getCharacterUUID() {
        return this.characterUUID;
    }

    static YKGBLEResponse init(byte[] data, String version) {
        if (data == null) {
            return null;
        } else if (version != null && version.equals("1.0")) {
            YKGBLEResponse response = new YKGBLEResponse();
            response.data = new byte[data.length];
            response.originalData = new byte[data.length];
            System.arraycopy(data, 0, response.data, 0, data.length);
            System.arraycopy(data, 0, response.originalData, 0, data.length);
            return response;
        } else if (data.length < 2) {
            return null;
        } else {
            byte[] tdata = new byte[data.length - 1];
            System.arraycopy(data, 0, tdata, 0, data.length - 1);
            int dcrc = data[data.length - 1];
            int ccrc = JCCrc8.calcCrc8(tdata);
            if (ccrc == dcrc) {
                response = new YKGBLEResponse();
                response.type = data[0] & 255;
                response.originalData = new byte[data.length];
                response.data = new byte[data.length - 3];
                System.arraycopy(data, 2, response.data, 0, data.length - 3);
                System.arraycopy(data, 0, response.originalData, 0, data.length);
                YKGUtils.logi(YKGUtils.bytesToHexString(response.data));
                return response;
            } else {
                return null;
            }
        }
    }

    public byte[] getData() {
        return this.data;
    }

    public int getType() {
        return this.type;
    }
}
