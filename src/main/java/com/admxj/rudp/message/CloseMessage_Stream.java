package com.admxj.rudp.message;


import com.admxj.utils.ByteIntConvert;
import com.admxj.utils.ByteShortConvert;

import java.net.DatagramPacket;


public class CloseMessage_Stream
        extends Message
{
    public short sType = MessageType.sType_CloseMessage_Stream;
    byte[] data;
    byte[] dpData;
    int closeOffset;

    public CloseMessage_Stream(int connectId, int clientId, int closeOffset)
    {
        byte[] dpData = new byte[16];
        this.clientId = clientId;
        this.connectId = connectId;
        ByteShortConvert.toByteArray(this.ver, dpData, 0);
        ByteShortConvert.toByteArray(this.sType, dpData, 2);
        ByteIntConvert.toByteArray(connectId, dpData, 4);
        ByteIntConvert.toByteArray(clientId, dpData, 8);
        ByteIntConvert.toByteArray(closeOffset, dpData, 12);
        this.dp = new DatagramPacket(dpData, dpData.length);
    }

    public CloseMessage_Stream(DatagramPacket dp)
    {
        this.dp = dp;
        this.dpData = dp.getData();
        this.ver = ByteShortConvert.toShort(this.dpData, 0);
        this.sType = ByteShortConvert.toShort(this.dpData, 2);

        this.connectId = ByteIntConvert.toInt(this.dpData, 4);
        this.clientId = ByteIntConvert.toInt(this.dpData, 8);
        this.closeOffset = ByteIntConvert.toInt(this.dpData, 12);
    }

    public int getCloseOffset()
    {
        return this.closeOffset;
    }
}
