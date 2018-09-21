package com.leco.ykg.ykgbaseble;

import com.leco.ykg.ykgbaseble.YKGUtils.JCCrc8;

/**
 * Created by zs on 2018/7/9.
 */
public class YKGBLEWriteRequest extends YKGBLERequest {
    public static byte JCBLERequestType_Unknown = 0;
    public static byte JCBLERequestType_SN = 1;
    public static byte JCBLERequestType_Version = 2;
    public static byte JCBLERequestType_Power = 3;
    public static byte JCBLERequestType_Beacon = 4;
    public static byte JCBLERequestType_JURA = 5;
    public static byte JCBLERequestType_Reset = 6;
    public static byte JCBLERequestType_Disconnect = 7;
    public static byte JCBLERequestType_Uart = 8;
    public static byte JCBLERequestType_F0 = -16;
    public static byte JCBLERequestType_F1 = -15;
    public static byte JCBLERequestType_F2 = -14;
    public static byte JCBLERequestType_F3 = -13;
    public static byte JCBLERequestType_F4 = -12;
    public static byte JCBLERequestType_F5 = -11;
    private static byte RequestSubPacketType_None = 0;
    private static byte RequestSubPacketType_Start = 1;
    private static byte RequestSubPacketType_Stop = 2;
    private static int MAXSUBPACKETLENGTH = 19;
    private byte type;
    private int nblock;
    public int iblock;
    private byte[] data;
    private byte[] originalData;
    private String serviceUUID = "f000fff0-0451-4000-b000-000000000000";
    private String characterUUID = "f000fff1-0451-4000-b000-000000000000";

    public YKGBLEWriteRequest() {
    }

    public void setServiceUUID(String serviceUUID) {
        this.serviceUUID = serviceUUID;
        if (this.originalData != null) {
            this.setData(this.originalData);
        }

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

    public int getNblock() {
        if (this.data == null) {
            return 1;
        } else {
            this.nblock = this.data.length / MAXSUBPACKETLENGTH + (this.data.length % MAXSUBPACKETLENGTH == 0 ? 0 : 1);
            return this.nblock;
        }
    }

    public void setType(byte type) {
        this.type = type;
        this.setData(this.originalData);
    }

    public void setData(byte[] data) {
        if (this.originalData == null) {
            this.originalData = data;
        }

        if (this.characterUUID.equalsIgnoreCase("f000fff1-0451-4000-b000-000000000000")) {
            byte len = this.originalData == null ? 0 : (byte) this.originalData.length;
            byte[] resultData;
            if (len > 0) {
                resultData = new byte[len + 3];
                resultData[0] = this.type;
                resultData[1] = len;
                System.arraycopy(this.originalData, 0, resultData, 2, len);
            } else {
                resultData = new byte[]{this.type, 0};
            }

            resultData[resultData.length - 1] = JCCrc8.calcCrc8(resultData, 0, resultData.length - 1);
            this.data = resultData;
        } else {
            this.data = this.originalData;
        }

        this.iblock = 0;
    }

    public byte[] getBlock(int iblock, String version, String sn) {
        if (version != null && version.equals("1.0") & sn.startsWith("01")) {
            return this.originalData;
        } else {
            int startpos = iblock * MAXSUBPACKETLENGTH;
            int dataLength = 0;
            if (this.data != null) {
                dataLength = this.data.length;
            }

            int length = dataLength - iblock * MAXSUBPACKETLENGTH > MAXSUBPACKETLENGTH ? MAXSUBPACKETLENGTH : dataLength - iblock * MAXSUBPACKETLENGTH;
            byte[] tempData = new byte[0];
            if (length > 0) {
                tempData = new byte[length];
                System.arraycopy(this.data, startpos, tempData, 0, length);
            }

            if (this.characterUUID.equalsIgnoreCase("F000DFF1-0451-4000-B000-000000000000")) {
                return tempData;
            } else {
                byte head = RequestSubPacketType_None;
                if (iblock == 0) {
                    head = RequestSubPacketType_Start;
                }

                if (iblock >= this.getNblock() - 1) {
                    head |= RequestSubPacketType_Stop;
                }

                byte[] resultData = new byte[tempData.length + 1];
                resultData[0] = head;
                if (tempData.length > 0) {
                    System.arraycopy(tempData, 0, resultData, 1, tempData.length);
                }

                return resultData;
            }
        }
    }
}
