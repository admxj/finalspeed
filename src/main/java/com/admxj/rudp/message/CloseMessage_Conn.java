package com.admxj.rudp.message;

import com.admxj.utils.ByteIntConvert;
import com.admxj.utils.ByteShortConvert;

import java.net.DatagramPacket;

public class CloseMessage_Conn
        extends Message
{
    public short sType = MessageType.sType_CloseMessage_Conn;
    byte[] data;
    byte[] dpData;

    public CloseMessage_Conn(int connectId, int clientId)
    {
        byte[] dpData = new byte[12];
        this.clientId = clientId;
        this.connectId = connectId;
        ByteShortConvert.toByteArray(this.ver, dpData, 0);
        ByteShortConvert.toByteArray(this.sType, dpData, 2);
        ByteIntConvert.toByteArray(connectId, dpData, 4);
        ByteIntConvert.toByteArray(clientId, dpData, 8);
        this.dp = new DatagramPacket(dpData, dpData.length);
    }

    public CloseMessage_Conn(DatagramPacket dp)
    {
        this.dp = dp;
        this.dpData = dp.getData();
        this.ver = ByteShortConvert.toShort(this.dpData, 0);
        this.sType = ByteShortConvert.toShort(this.dpData, 2);
        this.connectId = ByteIntConvert.toInt(this.dpData, 4);
        this.clientId = ByteIntConvert.toInt(this.dpData, 8);
    }
}
