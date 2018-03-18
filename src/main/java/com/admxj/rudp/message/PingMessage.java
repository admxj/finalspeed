package com.admxj.rudp.message;

import com.admxj.utils.ByteIntConvert;
import com.admxj.utils.ByteShortConvert;

import java.net.DatagramPacket;

public class PingMessage
        extends Message
{
    public short sType = MessageType.sType_PingMessage;
    byte[] dpData = new byte[20];
    int pingId;
    int downloadSpeed;
    int uploadSpeed;

    public PingMessage(int connectId, int clientId, int pingId, int downloadSpeed, int uploadSpeed)
    {
        ByteShortConvert.toByteArray(this.ver, this.dpData, 0);
        ByteShortConvert.toByteArray(this.sType, this.dpData, 2);
        ByteIntConvert.toByteArray(connectId, this.dpData, 4);
        ByteIntConvert.toByteArray(clientId, this.dpData, 8);
        ByteIntConvert.toByteArray(pingId, this.dpData, 12);
        ByteShortConvert.toByteArray((short)(downloadSpeed / 1024), this.dpData, 16);
        ByteShortConvert.toByteArray((short)(uploadSpeed / 1024), this.dpData, 18);
        this.dp = new DatagramPacket(this.dpData, this.dpData.length);
    }

    public PingMessage(DatagramPacket dp)
    {
        this.dp = dp;
        this.dpData = dp.getData();
        this.ver = ByteShortConvert.toShort(this.dpData, 0);
        this.sType = ByteShortConvert.toShort(this.dpData, 2);
        this.connectId = ByteIntConvert.toInt(this.dpData, 4);
        this.clientId = ByteIntConvert.toInt(this.dpData, 8);
        this.pingId = ByteIntConvert.toInt(this.dpData, 12);
        this.downloadSpeed = ByteShortConvert.toShort(this.dpData, 16);
        this.uploadSpeed = ByteShortConvert.toShort(this.dpData, 18);
    }

    public int getPingId()
    {
        return this.pingId;
    }

    public void setPingId(int pingId)
    {
        this.pingId = pingId;
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
}
