package com.admxj.client;

public interface ClientUII {
    public abstract void setMessage(String paramString);

    public abstract void updateUISpeed(int paramInt1, int paramInt2, int paramInt3);

    public abstract boolean login();

    public abstract boolean updateNode(boolean paramBoolean);

    public abstract boolean isOsx_fw_pf();

    public abstract boolean isOsx_fw_ipfw();
}
