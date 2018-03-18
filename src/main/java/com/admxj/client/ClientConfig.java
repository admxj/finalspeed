package com.admxj.client;

public class ClientConfig {
    String serverAddress = "";
    int serverPort;
    int remotePort;
    int downloadSpeed;
    int uploadSpeed;
    boolean direct_cn = true;
    int socks5Port = 1083;
    String remoteAddress;
    String protocal = "tcp";

    public String getServerAddress()
    {
        return this.serverAddress;
    }

    public void setServerAddress(String serverAddress)
    {
        this.serverAddress = serverAddress;
    }

    public int getServerPort()
    {
        return this.serverPort;
    }

    public void setServerPort(int serverPort)
    {
        this.serverPort = serverPort;
    }

    public int getRemotePort()
    {
        return this.remotePort;
    }

    public void setRemotePort(int remotePort)
    {
        this.remotePort = remotePort;
    }

    public boolean isDirect_cn()
    {
        return this.direct_cn;
    }

    public void setDirect_cn(boolean direct_cn)
    {
        this.direct_cn = direct_cn;
    }

    public int getDownloadSpeed()
    {
        return this.downloadSpeed;
    }

    public void setDownloadSpeed(int downloadSpeed)
    {
        this.downloadSpeed = downloadSpeed;
    }

    public int getUploadSpeed()
    {
        return this.uploadSpeed;
    }

    public void setUploadSpeed(int uploadSpeed)
    {
        this.uploadSpeed = uploadSpeed;
    }

    public int getSocks5Port()
    {
        return this.socks5Port;
    }

    public void setSocks5Port(int socks5Port)
    {
        this.socks5Port = socks5Port;
    }

    public String getRemoteAddress()
    {
        return this.remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress)
    {
        this.remoteAddress = remoteAddress;
    }

    public String getProtocal()
    {
        return this.protocal;
    }

    public void setProtocal(String protocal)
    {
        this.protocal = protocal;
    }
}
