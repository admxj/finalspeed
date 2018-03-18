package com.admxj.rudp.message;

import com.admxj.utils.ByteIntConvert;
import com.admxj.utils.ByteShortConvert;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class DataMessage
        extends Message
{
    short sType = MessageType.sType_DataMessage;
    int sequence = 0;
    int length = 0;
    byte[] data;
    byte[] dpData;
    int timeId;
    InetAddress dstAddress;
    int dstPort;
    int offset;
    int firstSendTimeId;
    long firstSendTime;

    public DataMessage(int sequence, byte[] dataq, int offset, short length, int connectId, int clientId)
    {
        this.sequence = sequence;
        this.offset = offset;
        this.length = length;
        this.data = new byte[this.length];
        this.clientId = clientId;
        this.connectId = connectId;
        System.arraycopy(dataq, offset, this.data, 0, length);
        this.length = this.data.length;
    }

    public void create(int timeId)
    {
        this.timeId = timeId;
        this.dpData = new byte[this.length + 16 + 8];
        ByteShortConvert.toByteArray(this.ver, this.dpData, 0);
        ByteShortConvert.toByteArray(this.sType, this.dpData, 2);

        ByteIntConvert.toByteArray(this.connectId, this.dpData, 4);
        ByteIntConvert.toByteArray(this.clientId, this.dpData, 8);

        ByteIntConvert.toByteArray(this.sequence, this.dpData, 12);
        ByteShortConvert.toByteArray((short)this.length, this.dpData, 16);
        ByteIntConvert.toByteArray(this.timeId, this.dpData, 18);
        System.arraycopy(this.data, 0, this.dpData, 22, this.length);
        this.dp = new DatagramPacket(this.dpData, this.dpData.length);
        this.dp.setAddress(this.dstAddress);
        this.dp.setPort(this.dstPort);
    }

    public DataMessage(DatagramPacket dp)
    {
        this.dp = dp;
        this.dpData = dp.getData();
        this.ver = ByteShortConvert.toShort(this.dpData, 0);
        this.sType = ByteShortConvert.toShort(this.dpData, 2);

        this.connectId = ByteIntConvert.toInt(this.dpData, 4);
        this.clientId = ByteIntConvert.toInt(this.dpData, 8);

        this.sequence = ByteIntConvert.toInt(this.dpData, 12);
        this.length = ByteShortConvert.toShort(this.dpData, 16);
        this.timeId = ByteIntConvert.toInt(this.dpData, 18);
        this.data = new byte[this.length];
        System.arraycopy(this.dpData, 22, this.data, 0, this.length);
    }

    public int getSequence()
    {
        return this.sequence;
    }

    public byte[] getData()
    {
        return this.data;
    }

    public int getLength()
    {
        return this.length;
    }

    public int getTimeId()
    {
        return this.timeId;
    }

    public void setTimeId(int timeId)
    {
        this.timeId = timeId;
    }

    public InetAddress getDstAddress()
    {
        return this.dstAddress;
    }

    public void setDstAddress(InetAddress dstAddress)
    {
        this.dstAddress = dstAddress;
    }

    public int getDstPort()
    {
        return this.dstPort;
    }

    public void setDstPort(int dstPort)
    {
        this.dstPort = dstPort;
    }

    public int getFirstSendTimeId()
    {
        return this.firstSendTimeId;
    }

    public void setFirstSendTimeId(int firstSendTimeId)
    {
        this.firstSendTimeId = firstSendTimeId;
    }

    public long getFirstSendTime()
    {
        return this.firstSendTime;
    }

    public void setFirstSendTime(long firstSendTime)
    {
        this.firstSendTime = firstSendTime;
    }
}
