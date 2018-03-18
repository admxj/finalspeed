package com.admxj.rudp.message;

import com.admxj.utils.ByteIntConvert;
import com.admxj.utils.ByteShortConvert;

import java.net.DatagramPacket;

public class PingMessage2
        extends Message
{
    public short sType = MessageType.sType_PingMessage2;
    byte[] dpData = new byte[16];
    int pingId;

    public PingMessage2(int connectId, int clientId, int pingId)
    {
        ByteShortConvert.toByteArray(this.ver, this.dpData, 0);
        ByteShortConvert.toByteArray(this.sType, this.dpData, 2);
        ByteIntConvert.toByteArray(connectId, this.dpData, 4);
        ByteIntConvert.toByteArray(clientId, this.dpData, 8);
        ByteIntConvert.toByteArray(pingId, this.dpData, 12);
        this.dp = new DatagramPacket(this.dpData, this.dpData.length);
    }

    public PingMessage2(DatagramPacket dp)
    {
        this.dp = dp;
        this.dpData = dp.getData();
        this.ver = ByteShortConvert.toShort(this.dpData, 0);
        this.sType = ByteShortConvert.toShort(this.dpData, 2);
        this.connectId = ByteIntConvert.toInt(this.dpData, 4);
        this.clientId = ByteIntConvert.toInt(this.dpData, 8);
        this.pingId = ByteIntConvert.toInt(this.dpData, 12);
    }

    public int getPingId()
    {
        return this.pingId;
    }

    public void setPingId(int pingId)
    {
        this.pingId = pingId;
    }
}
