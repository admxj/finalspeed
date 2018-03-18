package com.admxj.rudp;

import com.admxj.rudp.message.AckListMessage;
import com.admxj.rudp.message.CloseMessage_Conn;
import com.admxj.rudp.message.CloseMessage_Stream;
import com.admxj.rudp.message.DataMessage;

import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class Sender {
    DataMessage me2 = null;
    int interval;
    public int sum = 0;
    int sleepTime = 100;
    ConnectionUDP conn;
    Receiver receiver = null;
    boolean bussy = false;
    Object bussyOb = new Object();
    boolean isHave = false;
    public HashMap<Integer, DataMessage> sendTable = new HashMap();
    boolean isReady = false;
    Object readyOb = new Object();
    Object winOb = new Object();
    public InetAddress dstIp;
    public int dstPort;
    public int sequence = 0;
    int sendOffset = -1;
    boolean pause = false;
    int unAckMin = 0;
    int unAckMax = -1;
    int sendSum = 0;
    int reSendSum = 0;
    UDPOutputStream uos;
    int sw = 0;
    static Random ran = new Random();
    long lastSendTime = -1L;
    boolean closed = false;
    boolean streamClosed = false;
    static int s = 0;
    Object syn_send_table = new Object();
    HashMap<Integer, DataMessage> unAckTable = new HashMap();

    Sender(ConnectionUDP conn)
    {
        this.conn = conn;
        this.uos = new UDPOutputStream(conn);
        this.receiver = conn.receiver;
        this.dstIp = conn.dstIp;
        this.dstPort = conn.dstPort;
    }

    void sendData(byte[] data, int offset, int length)
            throws ConnectException, InterruptedException
    {
        int packetLength = RUDPConfig.packageSize;
        int sum = length / packetLength;
        if (length % packetLength != 0) {
            sum++;
        }
        if (sum == 0) {
            sum = 1;
        }
        int len = packetLength;
        if (length <= len)
        {
            this.sw += 1;
            sendNata(data, 0, length);
            this.sw -= 1;
        }
        else
        {
            for (int i = 0; i < sum; i++)
            {
                byte[] b = new byte[len];
                System.arraycopy(data, offset, b, 0, len);
                sendNata(b, 0, b.length);
                offset += packetLength;
                if (offset + len > length) {
                    len = length - (sum - 1) * packetLength;
                }
            }
        }
    }

    void sendNata(byte[] data, int offset, int length)
            throws ConnectException, InterruptedException
    {
        if (!this.closed)
        {
            if (!this.streamClosed)
            {
                DataMessage me = new DataMessage(this.sequence, data, 0, (short)length, this.conn.connectId, this.conn.route.localclientId);
                me.setDstAddress(this.dstIp);
                me.setDstPort(this.dstPort);
                synchronized (this.syn_send_table)
                {
                    this.sendTable.put(Integer.valueOf(me.getSequence()), me);
                }
                synchronized (this.winOb)
                {
                    if (!this.conn.receiver.checkWin()) {
                        try
                        {
                            this.winOb.wait();
                        }
                        catch (InterruptedException e)
                        {
                            throw e;
                        }
                    }
                }
                boolean twice = false;
                if (RUDPConfig.twice_tcp) {
                    twice = true;
                }
                if ((RUDPConfig.double_send_start) &&
                        (me.getSequence() <= 5)) {
                    twice = true;
                }
                sendDataMessage(me, false, twice, true);
                this.lastSendTime = System.currentTimeMillis();
                this.sendOffset += 1;
                s += me.getData().length;
                this.conn.clientControl.resendMange.addTask(this.conn, this.sequence);
                this.sequence += 1;
            }
            else
            {
                throw new ConnectException("RDP���������������sendData");
            }
        }
        else {
            throw new ConnectException("RDP������������������");
        }
    }

    public void closeStream_Local()
    {
        if (!this.streamClosed)
        {
            this.streamClosed = true;
            this.conn.receiver.closeStream_Local();
            if (!this.conn.stopnow) {
                sendCloseMessage_Stream();
            }
        }
    }

    public void closeStream_Remote()
    {
        if (!this.streamClosed) {
            this.streamClosed = true;
        }
    }

    void sendDataMessage(DataMessage me, boolean resend, boolean twice, boolean block)
    {
        synchronized (this.conn.clientControl.getSynlock())
        {
            long startTime = System.nanoTime();
            long t1 = System.currentTimeMillis();
            this.conn.clientControl.onSendDataPacket(this.conn);

            int timeId = this.conn.clientControl.getCurrentTimeId();

            me.create(timeId);

            SendRecord record_current = this.conn.clientControl.getSendRecord(timeId);
            if (!resend)
            {
                me.setFirstSendTimeId(timeId);
                me.setFirstSendTime(System.currentTimeMillis());
                record_current.addSended_First(me.getData().length);
                record_current.addSended(me.getData().length);
            }
            else
            {
                SendRecord record = this.conn.clientControl.getSendRecord(me.getFirstSendTimeId());
                record.addResended(me.getData().length);
                record_current.addSended(me.getData().length);
            }
            try
            {
                this.sendSum += 1;
                this.sum += 1;
                this.unAckMax += 1;

                long t = System.currentTimeMillis();
                send(me.getDatagramPacket());
                if (twice) {
                    send(me.getDatagramPacket());
                }
                if (block) {
                    this.conn.clientControl.sendSleep(startTime, me.getData().length);
                }
                TrafficEvent event = new TrafficEvent("", ran.nextLong(), me.getData().length, TrafficEvent.type_uploadTraffic);
                Route.fireEvent(event);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    void sendAckDelay(int ackSequence)
    {
        this.conn.route.delayAckManage.addAck(this.conn, ackSequence);
    }

    void sendLastReadDelay()
    {
        this.conn.route.delayAckManage.addLastRead(this.conn);
    }

    DataMessage getDataMessage(int sequence)
    {
        return (DataMessage)this.sendTable.get(Integer.valueOf(sequence));
    }

    public void reSend(int sequence, int count)
    {
        if (this.sendTable.containsKey(Integer.valueOf(sequence)))
        {
            DataMessage dm = (DataMessage)this.sendTable.get(Integer.valueOf(sequence));
            if (dm != null) {
                sendDataMessage(dm, true, false, true);
            }
        }
    }

    public void destroy()
    {
        synchronized (this.syn_send_table)
        {
            this.sendTable.clear();
        }
    }

    void removeSended_Ack(int sequence)
    {
        synchronized (this.syn_send_table)
        {
            DataMessage localDataMessage = (DataMessage)this.sendTable.remove(Integer.valueOf(sequence));
        }
    }

    void play()
    {
        synchronized (this.winOb)
        {
            this.winOb.notifyAll();
        }
    }

    void close()
    {
        synchronized (this.winOb)
        {
            this.winOb.notifyAll();
        }
        if (!this.closed) {
            this.closed = true;
        }
    }

    void sendCloseMessage_Stream()
    {
        CloseMessage_Stream cm = new CloseMessage_Stream(this.conn.connectId, this.conn.route.localclientId, this.sequence);
        cm.setDstAddress(this.dstIp);
        cm.setDstPort(this.dstPort);
        try
        {
            send(cm.getDatagramPacket());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        try
        {
            send(cm.getDatagramPacket());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    void sendCloseMessage_Conn()
    {
        CloseMessage_Conn cm = new CloseMessage_Conn(this.conn.connectId, this.conn.route.localclientId);
        cm.setDstAddress(this.dstIp);
        cm.setDstPort(this.dstPort);
        try
        {
            send(cm.getDatagramPacket());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        try
        {
            send(cm.getDatagramPacket());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    void sendALMessage(ArrayList ackList)
    {
        int currentTimeId = this.conn.receiver.getCurrentTimeId();
        AckListMessage alm = new AckListMessage(this.conn.connetionId, ackList, this.conn.receiver.lastRead,
                this.conn.clientControl.sendRecordTable_remote, currentTimeId,
                this.conn.connectId, this.conn.route.localclientId);
        alm.setDstAddress(this.dstIp);
        alm.setDstPort(this.dstPort);
        try
        {
            send(alm.getDatagramPacket());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    void send(DatagramPacket dp)
            throws IOException
    {
        sendPacket(dp, Integer.valueOf(this.conn.connectId));
    }

    public void sendPacket(DatagramPacket dp, Integer di)
            throws IOException
    {
        this.conn.clientControl.sendPacket(dp);
    }
}
