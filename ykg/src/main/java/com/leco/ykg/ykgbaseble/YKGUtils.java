package com.leco.ykg.ykgbaseble;

import android.util.Log;

/**
 * Created by zs on 2018/7/9.
 */
public class YKGUtils {
    private static final String JC_TAG_ERROR = "YKG_ERROR";
    private static final String JC_TAG_INFO = "YKG_INFO";

    public YKGUtils() {
    }

    public static float getPower(float bettery) {
        float BattMaxLevel = 6.6F;
        float BattMinLevel = 5.4F;
        if (bettery > BattMaxLevel) {
            bettery = 100.0F;
        } else if (bettery < BattMinLevel) {
            bettery = 0.0F;
        } else {
            bettery = (bettery - BattMinLevel) * 100.0F / (BattMaxLevel - BattMinLevel);
        }

        return bettery;
    }

    public static String parseMac(String sn) {
        String lockMac = sn.substring(4, sn.length());
        StringBuilder sb = new StringBuilder("");

        for (int i = 0; i < lockMac.length() / 2; ++i) {
            sb.append(lockMac.charAt(i * 2));
            sb.append(lockMac.charAt(i * 2 + 1));
            sb.append(":");
        }

        String strBuil = sb.toString().trim();
        String deviceMac = sb.substring(0, strBuil.length() - 1);
        return deviceMac;
    }

    public static byte[] conformPwd(String sn) {
        String hander = sn.substring(0, 4);
        String authen = sn + hander;
        byte[] snByte = HexString2Bytes(authen, 10);
        YKGUtils.JCMD5 md5 = new YKGUtils.JCMD5();
        byte[] md5Pwd = md5.getMD5ofByte(snByte);
        byte[] buf = new byte[md5Pwd.length + 1];
        buf[0] = (byte) md5Pwd.length;

        for (int i = 0; i < md5Pwd.length; ++i) {
            buf[i + 1] = md5Pwd[i];
        }

        return buf;
    }

    public static byte[] HexString2Bytes(String src, int length) {
        byte[] ret = new byte[length];
        byte[] tmp = src.getBytes();

        for (int i = 0; i < length; ++i) {
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
        }

        return ret;
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src != null && src.length > 0) {
            for (int i = 0; i < src.length; ++i) {
                int v = src[i] & 255;
                String hv = Integer.toHexString(v);
                if (hv.length() < 2) {
                    stringBuilder.append(0);
                }

                stringBuilder.append(hv);
            }

            return stringBuilder.toString();
        } else {
            return null;
        }
    }

    public static byte uniteBytes(byte src0, byte src1) {
        byte _b0 = Byte.decode("0x" + new String(new byte[]{src0}));
        _b0 = (byte) (_b0 << 4);
        byte _b1 = Byte.decode("0x" + new String(new byte[]{src1}));
        byte ret = (byte) (_b0 ^ _b1);
        return ret;
    }

    public static long byteToLong(byte[] b, int offset) {
        long s = 0L;
        long s0 = (long) (b[offset + 7] & 255);
        long s1 = (long) (b[offset + 6] & 255);
        long s2 = (long) (b[offset + 5] & 255);
        long s3 = (long) (b[offset + 4] & 255);
        long s4 = (long) (b[offset + 3] & 255);
        long s5 = (long) (b[offset + 2] & 255);
        long s6 = (long) (b[offset + 1] & 255);
        long s7 = (long) (b[offset + 0] & 255);
        s1 <<= 8;
        s2 <<= 16;
        s3 <<= 24;
        s4 <<= 32;
        s5 <<= 40;
        s6 <<= 48;
        s7 <<= 56;
        s = s0 | s1 | s2 | s3 | s4 | s5 | s6 | s7;
        return s;
    }

    public static short byteToShort(byte[] b, int offset) {
//        short s = false;
        short s0 = (short) (b[offset + 1] & 255);
        short s1 = (short) (b[offset + 0] & 255);
        s1 = (short) (s1 << 8);
        short s = (short) (s0 | s1);
        return s;
    }

    public static int compareVersion(String version1, String version2) {
        if (version1.equals(version2)) {
            return 0;
        } else {
            String[] version1Array = version1.split("\\.");
            String[] version2Array = version2.split("\\.");

            for (int i = 0; i < version1Array.length; ++i) {
                if (!version1Array[i].equals(version2Array[i])) {
                    int v1 = Integer.parseInt(version1Array[i]);
                    int v2 = Integer.parseInt(version2Array[i]);
                    return v1 > v2 ? 1 : -1;
                }
            }

            return 0;
        }
    }

    public static byte[] longToByte(long bits) {
        byte[] bytes = new byte[8];
        byte s0 = (byte) ((int) (bits >> 56 & 255L));
        byte s1 = (byte) ((int) (bits >> 48 & 255L));
        byte s2 = (byte) ((int) (bits >> 40 & 255L));
        byte s3 = (byte) ((int) (bits >> 32 & 255L));
        byte s4 = (byte) ((int) (bits >> 24 & 255L));
        byte s5 = (byte) ((int) (bits >> 16 & 255L));
        byte s6 = (byte) ((int) (bits >> 8 & 255L));
        byte s7 = (byte) ((int) (bits & 255L));
        bytes[0] = s0;
        bytes[1] = s1;
        bytes[2] = s2;
        bytes[3] = s3;
        bytes[4] = s4;
        bytes[5] = s5;
        bytes[6] = s6;
        bytes[7] = s7;
        return bytes;
    }

    public static byte[] intToByte(int b) {
        byte[] bytes = new byte[2];
        byte tmp0 = (byte) (b >> 8 & 255);
        byte tmp1 = (byte) (b & 255);
        bytes[0] = tmp0;
        bytes[1] = tmp1;
        return bytes;
    }

    public static int changePowerToInt(byte b) {
        int power = 0;
        if (b == 0) {
            power = -21;
        } else if (b == 1) {
            power = -18;
        } else if (b == 2) {
            power = -15;
        } else if (b == 3) {
            power = -12;
        } else if (b == 4) {
            power = -9;
        } else if (b == 5) {
            power = -6;
        } else if (b == 6) {
            power = -3;
        } else if (b == 7) {
            power = 0;
        } else if (b == 8) {
            power = 1;
        } else if (b == 9) {
            power = 2;
        } else if (b == 10) {
            power = 3;
        } else if (b == 11) {
            power = 4;
        } else if (b == 12) {
            power = 5;
        }

        return power;
    }

    public static byte changePowerToByte(int b) {
        byte power = 0;
        if (b == -21) {
            power = 0;
        } else if (b == -18) {
            power = 1;
        } else if (b == -15) {
            power = 2;
        } else if (b == -12) {
            power = 3;
        } else if (b == -9) {
            power = 4;
        } else if (b == -6) {
            power = 5;
        } else if (b == -3) {
            power = 6;
        } else if (b == 0) {
            power = 7;
        } else if (b == 1) {
            power = 8;
        } else if (b == 2) {
            power = 9;
        } else if (b == 3) {
            power = 10;
        } else if (b == 4) {
            power = 11;
        } else if (b == 5) {
            power = 12;
        }

        return power;
    }

    static String getLineInfo(int index) {
        StackTraceElement[] elements = (new Throwable()).getStackTrace();
        if (elements.length > index) {
            StackTraceElement ste = elements[index];
            return ste.getFileName() + ": Line " + ste.getLineNumber();
        } else {
            return "Unknown Line";
        }
    }

    public static void logi(String str) {
        Log.i("YKG_INFO", getLineInfo(2) + "\n" + str);
    }

    public static void loge(String str) {
        Log.e("YKG_ERROR", getLineInfo(2) + "\n" + str);
    }

    public static class Constant {
        public static final int STATE_DISCONNECTED = 0;
        public static final int STATE_CONNECTING = 1;
        public static final String password_service_uuid = "f000dff0-0451-4000-b000-000000000000";
        public static final String password_character_uuid = "f000dff1-0451-4000-b000-000000000000";
        public static final String config_service_uuid = "f000fff0-0451-4000-b000-000000000000";
        public static final String config_character_uuid_writ = "f000fff1-0451-4000-b000-000000000000";
        public static final String config_character_uuid_read = "f000fff2-0451-4000-b000-000000000000";
        public static final String ACTION_GATT_CONNECTED = "com.jcble.api.lock.ACTION_GATT_CONNECTED";
        public static final String ACTION_GATT_DISCONNECTED = "com.jcble.api.lock.ACTION_GATT_DISCONNECTED";
        public static final String ACTION_GATT_SERVICES_DISCOVERED = "com.jcble.api.lock.ACTION_GATT_SERVICES_DISCOVERED";
        public static final String EXTRA_DATA = "com.jcble.api.lock.EXTRA_DATA";
        public static final String ACTION_DATA_READ_PASSWD = "com.jcble.api.lock.ACTION_DATA_READ_PASSWD";
        public static final String ACTION_DATA_READ_LIFT_STATUS = "com.jcble.api.lock.ACTION_DATA_LIFT_STATUS";
        public static final String ACTION_DATA_READ_JURA_STATUS = "com.jcble.api.lock.ACTION_DATA_JURA_STATUS";
        public static final String ACTION_DATA_WRITE_PASSWD = "com.jcble.api.lock.ACTION_DATA_PASSWD";
        public static final String ACTION_DATA_GET_RSSI = "com.jcble.api.lock.ACTION_DATA_GET_RSSI";
        public static final String ACTION_DATA_READ_SN = "com.jcble.api.device.ACTION_DATA_READ_SN";
        public static final String ACTION_DATA_READ_POWER = "com.jcble.api.device.ACTION_DATA_READ_POWER";
        public static final String ACTION_DATA_READ_BEACON_ONOROFF = "com.jcble.api.device.ACTION_DATA_READ_BEACON_ONOROFF";
        public static final String ACTION_DATA_SET_BEACON_PARAMETER = "com.jcble.api.device.ACTION_DATA_SET_BEACON_PARAMETER";
        public static final int SCANNER_TYPE_LOCKER = 1;
        public static final int SCANNER_TYPE_DATA_MARK = 3;
        public static final int SCANNER_TYPE_SENSOR = 4;
        public static final int SCANNER_TYPE_STREETLIGHT = 5;
        public static final int SCANNER_TYPE_GEOMAGNETIC = 6;
        public static final int SCANNER_TYPE_LED_LAMP = 7;
        public static final int SCANNER_TYPE_LOCATION_MARK = 8;

        public Constant() {
        }
    }

    public static class JCCrc8 {
        static byte[] crc8_tab = new byte[]{0, 94, -68, -30, 97, 63, -35, -125, -62, -100, 126, 32, -93, -3, 31, 65, -99, -61, 33, 127, -4, -94, 64, 30, 95, 1, -29, -67, 62, 96, -126, -36, 35, 125, -97, -63, 66, 28, -2, -96, -31, -65, 93, 3, -128, -34, 60, 98, -66, -32, 2, 92, -33, -127, 99, 61, 124, 34, -64, -98, 29, 67, -95, -1, 70, 24, -6, -92, 39, 121, -101, -59, -124, -38, 56, 102, -27, -69, 89, 7, -37, -123, 103, 57, -70, -28, 6, 88, 25, 71, -91, -5, 120, 38, -60, -102, 101, 59, -39, -121, 4, 90, -72, -26, -89, -7, 27, 69, -58, -104, 122, 36, -8, -90, 68, 26, -103, -57, 37, 123, 58, 100, -122, -40, 91, 5, -25, -71, -116, -46, 48, 110, -19, -77, 81, 15, 78, 16, -14, -84, 47, 113, -109, -51, 17, 79, -83, -13, 112, 46, -52, -110, -45, -115, 111, 49, -78, -20, 14, 80, -81, -15, 19, 77, -50, -112, 114, 44, 109, 51, -47, -113, 12, 82, -80, -18, 50, 108, -114, -48, 83, 13, -17, -79, -16, -82, 76, 18, -111, -49, 45, 115, -54, -108, 118, 40, -85, -11, 23, 73, 8, 86, -76, -22, 105, 55, -43, -117, 87, 9, -21, -75, 54, 104, -118, -44, -107, -53, 41, 119, -12, -86, 72, 22, -23, -73, 85, 11, -120, -42, 52, 106, 43, 117, -105, -55, 74, 20, -10, -88, 116, 42, -56, -106, 21, 75, -87, -9, -74, -24, 10, 84, -41, -119, 107, 53};

        public JCCrc8() {
        }

        public static byte calcCrc8(byte[] data) {
            return calcCrc8(data, 0, data.length, (byte) 0);
        }

        public static byte calcCrc8(byte[] data, int offset, int len) {
            return calcCrc8(data, offset, len, (byte) 0);
        }

        public static byte calcCrc8(byte[] data, int offset, int len, byte preval) {
            byte ret = preval;

            for (int i = offset; i < offset + len; ++i) {
                ret = crc8_tab[255 & (ret ^ data[i])];
            }

            ret = (byte) (ret ^ 255);
            return ret;
        }
    }

    public static class JCMD5 {
        static final int S11 = 7;
        static final int S12 = 12;
        static final int S13 = 17;
        static final int S14 = 22;
        static final int S21 = 5;
        static final int S22 = 9;
        static final int S23 = 14;
        static final int S24 = 20;
        static final int S31 = 4;
        static final int S32 = 11;
        static final int S33 = 16;
        static final int S34 = 23;
        static final int S41 = 6;
        static final int S42 = 10;
        static final int S43 = 15;
        static final int S44 = 21;
        static final byte[] PADDING = new byte[]{-128, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        private long[] state = new long[4];
        private long[] count = new long[2];
        private byte[] buffer = new byte[64];
        public String digestHexStr;
        private byte[] digest = new byte[16];

        public String getMD5ofStr(String inbuf) {
            this.md5Init();
            this.md5Update(inbuf.getBytes(), inbuf.length());
            this.md5Final();
            this.digestHexStr = "";

            for (int i = 0; i < 16; ++i) {
                this.digestHexStr = this.digestHexStr + byteHEX(this.digest[i]);
            }

            return this.digestHexStr;
        }

        public byte[] getMD5ofByte(byte[] bytes) {
            this.md5Init();
            this.md5Update(bytes, bytes.length);
            this.md5Final();
            return this.digest;
        }

        public JCMD5() {
        }

        private void md5Init() {
            this.count[0] = 0L;
            this.count[1] = 0L;
            this.state[0] = 1732584193L;
            this.state[1] = 4023233417L;
            this.state[2] = 2562383102L;
            this.state[3] = 271733878L;
        }

        private long F(long x, long y, long z) {
            return x & y | ~x & z;
        }

        private long G(long x, long y, long z) {
            return x & z | y & ~z;
        }

        private long H(long x, long y, long z) {
            return x ^ y ^ z;
        }

        private long I(long x, long y, long z) {
            return y ^ (x | ~z);
        }

        private long FF(long a, long b, long c, long d, long x, long s, long ac) {
            a += this.F(b, c, d) + x + ac;
            a = (long) ((int) a << (int) s | (int) a >>> (int) (32L - s));
            a += b;
            return a;
        }

        private long GG(long a, long b, long c, long d, long x, long s, long ac) {
            a += this.G(b, c, d) + x + ac;
            a = (long) ((int) a << (int) s | (int) a >>> (int) (32L - s));
            a += b;
            return a;
        }

        private long HH(long a, long b, long c, long d, long x, long s, long ac) {
            a += this.H(b, c, d) + x + ac;
            a = (long) ((int) a << (int) s | (int) a >>> (int) (32L - s));
            a += b;
            return a;
        }

        private long II(long a, long b, long c, long d, long x, long s, long ac) {
            a += this.I(b, c, d) + x + ac;
            a = (long) ((int) a << (int) s | (int) a >>> (int) (32L - s));
            a += b;
            return a;
        }

        private void md5Update(byte[] inbuf, int inputLen) {
            byte[] block = new byte[64];
            int index = (int) (this.count[0] >>> 3) & 63;
            if ((this.count[0] += (long) (inputLen << 3)) < (long) (inputLen << 3)) {
                ++this.count[1];
            }

            this.count[1] += (long) (inputLen >>> 29);
            int partLen = 64 - index;
            int i;
            if (inputLen >= partLen) {
                this.md5Memcpy(this.buffer, inbuf, index, 0, partLen);
                this.md5Transform(this.buffer);

                for (i = partLen; i + 63 < inputLen; i += 64) {
                    this.md5Memcpy(block, inbuf, 0, i, 64);
                    this.md5Transform(block);
                }

                index = 0;
            } else {
                i = 0;
            }

            this.md5Memcpy(this.buffer, inbuf, index, i, inputLen - i);
        }

        private void md5Final() {
            byte[] bits = new byte[8];
            this.Encode(bits, this.count, 8);
            int index = (int) (this.count[0] >>> 3) & 63;
            int padLen = index < 56 ? 56 - index : 120 - index;
            this.md5Update(PADDING, padLen);
            this.md5Update(bits, 8);
            this.Encode(this.digest, this.state, 16);
        }

        private void md5Memcpy(byte[] output, byte[] input, int outpos, int inpos, int len) {
            for (int i = 0; i < len; ++i) {
                output[outpos + i] = input[inpos + i];
            }

        }

        private void md5Transform(byte[] block) {
            long a = this.state[0];
            long b = this.state[1];
            long c = this.state[2];
            long d = this.state[3];
            long[] x = new long[16];
            this.Decode(x, block, 64);
            a = this.FF(a, b, c, d, x[0], 7L, 3614090360L);
            d = this.FF(d, a, b, c, x[1], 12L, 3905402710L);
            c = this.FF(c, d, a, b, x[2], 17L, 606105819L);
            b = this.FF(b, c, d, a, x[3], 22L, 3250441966L);
            a = this.FF(a, b, c, d, x[4], 7L, 4118548399L);
            d = this.FF(d, a, b, c, x[5], 12L, 1200080426L);
            c = this.FF(c, d, a, b, x[6], 17L, 2821735955L);
            b = this.FF(b, c, d, a, x[7], 22L, 4249261313L);
            a = this.FF(a, b, c, d, x[8], 7L, 1770035416L);
            d = this.FF(d, a, b, c, x[9], 12L, 2336552879L);
            c = this.FF(c, d, a, b, x[10], 17L, 4294925233L);
            b = this.FF(b, c, d, a, x[11], 22L, 2304563134L);
            a = this.FF(a, b, c, d, x[12], 7L, 1804603682L);
            d = this.FF(d, a, b, c, x[13], 12L, 4254626195L);
            c = this.FF(c, d, a, b, x[14], 17L, 2792965006L);
            b = this.FF(b, c, d, a, x[15], 22L, 1236535329L);
            a = this.GG(a, b, c, d, x[1], 5L, 4129170786L);
            d = this.GG(d, a, b, c, x[6], 9L, 3225465664L);
            c = this.GG(c, d, a, b, x[11], 14L, 643717713L);
            b = this.GG(b, c, d, a, x[0], 20L, 3921069994L);
            a = this.GG(a, b, c, d, x[5], 5L, 3593408605L);
            d = this.GG(d, a, b, c, x[10], 9L, 38016083L);
            c = this.GG(c, d, a, b, x[15], 14L, 3634488961L);
            b = this.GG(b, c, d, a, x[4], 20L, 3889429448L);
            a = this.GG(a, b, c, d, x[9], 5L, 568446438L);
            d = this.GG(d, a, b, c, x[14], 9L, 3275163606L);
            c = this.GG(c, d, a, b, x[3], 14L, 4107603335L);
            b = this.GG(b, c, d, a, x[8], 20L, 1163531501L);
            a = this.GG(a, b, c, d, x[13], 5L, 2850285829L);
            d = this.GG(d, a, b, c, x[2], 9L, 4243563512L);
            c = this.GG(c, d, a, b, x[7], 14L, 1735328473L);
            b = this.GG(b, c, d, a, x[12], 20L, 2368359562L);
            a = this.HH(a, b, c, d, x[5], 4L, 4294588738L);
            d = this.HH(d, a, b, c, x[8], 11L, 2272392833L);
            c = this.HH(c, d, a, b, x[11], 16L, 1839030562L);
            b = this.HH(b, c, d, a, x[14], 23L, 4259657740L);
            a = this.HH(a, b, c, d, x[1], 4L, 2763975236L);
            d = this.HH(d, a, b, c, x[4], 11L, 1272893353L);
            c = this.HH(c, d, a, b, x[7], 16L, 4139469664L);
            b = this.HH(b, c, d, a, x[10], 23L, 3200236656L);
            a = this.HH(a, b, c, d, x[13], 4L, 681279174L);
            d = this.HH(d, a, b, c, x[0], 11L, 3936430074L);
            c = this.HH(c, d, a, b, x[3], 16L, 3572445317L);
            b = this.HH(b, c, d, a, x[6], 23L, 76029189L);
            a = this.HH(a, b, c, d, x[9], 4L, 3654602809L);
            d = this.HH(d, a, b, c, x[12], 11L, 3873151461L);
            c = this.HH(c, d, a, b, x[15], 16L, 530742520L);
            b = this.HH(b, c, d, a, x[2], 23L, 3299628645L);
            a = this.II(a, b, c, d, x[0], 6L, 4096336452L);
            d = this.II(d, a, b, c, x[7], 10L, 1126891415L);
            c = this.II(c, d, a, b, x[14], 15L, 2878612391L);
            b = this.II(b, c, d, a, x[5], 21L, 4237533241L);
            a = this.II(a, b, c, d, x[12], 6L, 1700485571L);
            d = this.II(d, a, b, c, x[3], 10L, 2399980690L);
            c = this.II(c, d, a, b, x[10], 15L, 4293915773L);
            b = this.II(b, c, d, a, x[1], 21L, 2240044497L);
            a = this.II(a, b, c, d, x[8], 6L, 1873313359L);
            d = this.II(d, a, b, c, x[15], 10L, 4264355552L);
            c = this.II(c, d, a, b, x[6], 15L, 2734768916L);
            b = this.II(b, c, d, a, x[13], 21L, 1309151649L);
            a = this.II(a, b, c, d, x[4], 6L, 4149444226L);
            d = this.II(d, a, b, c, x[11], 10L, 3174756917L);
            c = this.II(c, d, a, b, x[2], 15L, 718787259L);
            b = this.II(b, c, d, a, x[9], 21L, 3951481745L);
            this.state[0] += a;
            this.state[1] += b;
            this.state[2] += c;
            this.state[3] += d;
        }

        private void Encode(byte[] output, long[] input, int len) {
            int i = 0;

            for (int j = 0; j < len; j += 4) {
                output[j] = (byte) ((int) (input[i] & 255L));
                output[j + 1] = (byte) ((int) (input[i] >>> 8 & 255L));
                output[j + 2] = (byte) ((int) (input[i] >>> 16 & 255L));
                output[j + 3] = (byte) ((int) (input[i] >>> 24 & 255L));
                ++i;
            }

        }

        private void Decode(long[] output, byte[] input, int len) {
            int i = 0;

            for (int j = 0; j < len; j += 4) {
                output[i] = b2iu(input[j]) | b2iu(input[j + 1]) << 8 | b2iu(input[j + 2]) << 16 | b2iu(input[j + 3]) << 24;
                ++i;
            }

        }

        public static long b2iu(byte b) {
            return b < 0 ? (long) (b & 255) : (long) b;
        }

        public static String byteHEX(byte ib) {
            char[] Digit = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
            char[] ob = new char[]{Digit[ib >>> 4 & 15], Digit[ib & 15]};
            String s = new String(ob);
            return s;
        }
    }
}
