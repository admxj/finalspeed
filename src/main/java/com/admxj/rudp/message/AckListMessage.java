package com.admxj.rudp.message;

import com.admxj.rudp.SendRecord;
import com.admxj.utils.ByteIntConvert;
import com.admxj.utils.ByteShortConvert;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.HashMap;

public class AckListMessage extends Message {

    ArrayList<Integer> ackList;
    byte[] dpData = null;
    int lastRead;
    int r1;
    int r2;
    int r3;
    int s1;
    int s2;
    int s3;

    public AckListMessage(long connId, ArrayList ackList, int lastRead, HashMap<Integer, SendRecord> sendRecordTable, int timeId, int connectId, int clientId)
    {
        this.clientId = clientId;
        this.connectId = connectId;
        this.ackList = ackList;
        this.lastRead = lastRead;
        int len1 = 18 + 4 * ackList.size();
        this.dpData = new byte[len1 + 24 + 9];
        this.sType = MessageType.sType_AckListMessage;
        ByteShortConvert.toByteArray(this.ver, this.dpData, 0);
        ByteShortConvert.toByteArray(this.sType, this.dpData, 2);
        ByteIntConvert.toByteArray(connectId, this.dpData, 4);
        ByteIntConvert.toByteArray(clientId, this.dpData, 8);

        ByteIntConvert.toByteArray(lastRead, this.dpData, 12);

        ByteShortConvert.toByteArray((short)ackList.size(), this.dpData, 16);
        for (int i = 0; i < ackList.size(); i++)
        {
            int sequence = ((Integer)ackList.get(i)).intValue();
            ByteIntConvert.toByteArray(sequence, this.dpData, 10 + 4 * i + 8);
        }
        int u1 = timeId - 2;
        ByteIntConvert.toByteArray(u1, this.dpData, len1 + 8);
        SendRecord r1 = (SendRecord)sendRecordTable.get(Integer.valueOf(u1));
        int s1 = 0;
        if (r1 != null) {
            s1 = r1.getSendSize();
        }
        ByteIntConvert.toByteArray(s1, this.dpData, len1 + 4 + 8);

        int u2 = timeId - 1;
        ByteIntConvert.toByteArray(u2, this.dpData, len1 + 8 + 8);
        SendRecord r2 = (SendRecord)sendRecordTable.get(Integer.valueOf(u2));
        int s2 = 0;
        if (r2 != null) {
            s2 = r2.getSendSize();
        }
        ByteIntConvert.toByteArray(s2, this.dpData, len1 + 12 + 8);

        int u3 = timeId;
        ByteIntConvert.toByteArray(u3, this.dpData, len1 + 16 + 8);
        SendRecord r3 = (SendRecord)sendRecordTable.get(Integer.valueOf(u3));
        int s3 = 0;
        if (r3 != null) {
            s3 = r3.getSendSize();
        }
        ByteIntConvert.toByteArray(s3, this.dpData, len1 + 20 + 8);

        this.dp = new DatagramPacket(this.dpData, this.dpData.length);
    }

    public ArrayList getAckList()
    {
        return this.ackList;
    }

    public AckListMessage(DatagramPacket dp)
    {
        this.dp = dp;
        this.dpData = dp.getData();
        this.ver = ByteShortConvert.toShort(this.dpData, 0);
        this.sType = ByteShortConvert.toShort(this.dpData, 2);
        this.connectId = ByteIntConvert.toInt(this.dpData, 4);
        this.clientId = ByteIntConvert.toInt(this.dpData, 8);

        this.lastRead = ByteIntConvert.toInt(this.dpData, 12);
        int sum = ByteShortConvert.toShort(this.dpData, 16);
        this.ackList = new ArrayList();
        int t = 0;
        for (int i = 0; i < sum; i++)
        {
            t = 10 + 4 * i;
            int sequence = ByteIntConvert.toInt(this.dpData, t + 8);
            this.ackList.add(Integer.valueOf(sequence));
        }
        t = 10 + 4 * sum - 4;
        this.r1 = ByteIntConvert.toInt(this.dpData, t + 4 + 8);
        this.s1 = ByteIntConvert.toInt(this.dpData, t + 8 + 8);

        this.r2 = ByteIntConvert.toInt(this.dpData, t + 12 + 8);
        this.s2 = ByteIntConvert.toInt(this.dpData, t + 16 + 8);

        this.r3 = ByteIntConvert.toInt(this.dpData, t + 20 + 8);
        this.s3 = ByteIntConvert.toInt(this.dpData, t + 24 + 8);
    }

    public int getLastRead()
    {
        return this.lastRead;
    }

    public int getR1()
    {
        return this.r1;
    }

    public int getR3()
    {
        return this.r3;
    }

    public int getS1()
    {
        return this.s1;
    }

    public int getS2()
    {
        return this.s2;
    }

    public int getS3()
    {
        return this.s3;
    }

    public int getR2()
    {
        return this.r2;
    }
}