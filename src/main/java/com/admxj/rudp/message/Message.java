package com.admxj.rudp.message;

import com.admxj.rudp.RUDPConfig;

import java.net.DatagramPacket;
import java.net.InetAddress;

public abstract class Message
{
    protected short ver = RUDPConfig.protocal_ver;
    protected short sType = 0;
    protected DatagramPacket dp;
    public int connectId;
    public int clientId;

    public int getSType()
    {
        return this.sType;
    }

    public int getVer()
    {
        return this.ver;
    }

    public DatagramPacket getDatagramPacket()
    {
        return this.dp;
    }

    public void setDstAddress(InetAddress dstIp)
    {
        this.dp.setAddress(dstIp);
    }

    public void setDstPort(int dstPort)
    {
        this.dp.setPort(dstPort);
    }

    public int getConnectId()
    {
        return this.connectId;
    }

    public void setConnectId(int connectId)
    {
        this.connectId = connectId;
    }

    public int getClientId()
    {
        return this.clientId;
    }

    public void setClientId(int clientId)
    {
        this.clientId = clientId;
    }
}

