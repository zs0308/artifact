package com.leco.ykg.ykgbaseble;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Created by zs on 2018/7/9.
 */
public class YKGBLEDevice implements Parcelable {
    public String sn;
    public String name;
    public int battery;
    public int rssi;
    public int temperature;
    public String version;
    public Date lastUpdateDate;
    public BluetoothDevice bluetoothDevice;
    public static final Creator<YKGBLEDevice> CREATOR = new Creator<YKGBLEDevice>() {
        public YKGBLEDevice createFromParcel(Parcel in) {
            return new YKGBLEDevice(in);
        }

        public YKGBLEDevice[] newArray(int size) {
            return new YKGBLEDevice[size];
        }
    };

    protected YKGBLEDevice() {
    }

    protected YKGBLEDevice(Parcel in) {
        this.sn = in.readString();
        this.name = in.readString();
        this.battery = in.readInt();
        this.rssi = in.readInt();
        this.temperature = in.readInt();
        this.version = in.readString();
        this.bluetoothDevice = (BluetoothDevice)in.readParcelable(BluetoothDevice.class.getClassLoader());
    }

    public String toString() {
        return String.format("\nsn:%s\nversion:%s\nname:%s\nbettery:%d\ntemperature:%d\nrssi:%d\nmacAddress:%s", this.sn, this.version, this.name, this.battery, this.temperature, this.rssi, this.bluetoothDevice.getAddress());
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
        dest.writeParcelable(this.bluetoothDevice, flags);
    }
}
