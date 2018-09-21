package com.leco.ykg.ykgble;


import android.os.Parcel;
import android.os.Parcelable;

import com.leco.ykg.ykgbaseble.YKGBLEDevice;


/**
 * Created by zs on 2018/7/9.
 */
public class YKGLockerDevice implements Parcelable {
    public String sn;
    public String name;
    public int battery;
    public int rssi;
    public int temperature;
    public String version;
    YKGBLEDevice bleDevice;
    public static final Creator<YKGLockerDevice> CREATOR = new Creator<YKGLockerDevice>() {
        public YKGLockerDevice createFromParcel(Parcel in) {
            return new YKGLockerDevice(in);
        }

        public YKGLockerDevice[] newArray(int size) {
            return new YKGLockerDevice[size];
        }
    };

    protected YKGLockerDevice() {
    }

    protected YKGLockerDevice(Parcel in) {
        this.sn = in.readString();
        this.name = in.readString();
        this.battery = in.readInt();
        this.rssi = in.readInt();
        this.temperature = in.readInt();
        this.version = in.readString();
        this.bleDevice = (YKGBLEDevice)in.readParcelable(YKGBLEDevice.class.getClassLoader());
    }

    public String toString() {
        return String.format("\nsn:%s\nversion:%s\nname:%s\nbettery:%d\ntemperature:%d\nrssi:%d\nmacAddress:%s", this.sn, this.version, this.name, this.battery, this.temperature, this.rssi, this.bleDevice.bluetoothDevice.getAddress());
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.sn);
        dest.writeString(this.name);
        dest.writeInt(this.battery);
        dest.writeInt(this.rssi);
        dest.writeInt(this.temperature);
        dest.writeString(this.version);
        dest.writeParcelable(this.bleDevice, flags);
    }
}
