package com.leco.ykg.ykgbaseble;

/**
 * Created by zs on 2018/7/9.
 */
public abstract class YKGBLERequest {
    public YKGBLERequest() {
    }

    public abstract void setServiceUUID(String var1);

    public abstract String getServiceUUID();

    public abstract void setCharacterUUID(String var1);

    public abstract String getCharacterUUID();
}
