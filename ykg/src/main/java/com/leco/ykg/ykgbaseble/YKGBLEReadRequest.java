package com.leco.ykg.ykgbaseble;

/**
 * Created by zs on 2018/7/9.
 */
public class YKGBLEReadRequest extends YKGBLERequest{
    private String serviceUUID = "f000fff0-0451-4000-b000-000000000000";
    private String characterUUID = "f000fff1-0451-4000-b000-000000000000";

    public YKGBLEReadRequest() {
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

}
