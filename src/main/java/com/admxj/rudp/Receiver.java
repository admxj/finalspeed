package com.admxj.rudp;


import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import com.admxj.rudp.message.*;
import com.admxj.utils.MessageCheck;

public class Receiver {
    ConnectionUDP conn;
    Sender sender;
    public InetAddress dstIp;
    public int dstPort;
    HashMap<Integer, DataMessage> receiveTable = new HashMap();
    int lastRead = -1;
    int lastReceive = -1;
    Object availOb = new Object();
    boolean isReady = false;
    Object readyOb = new Object();
    byte[] b4 = new byte[4];
    int lastRead1 = 0;
    int maxWinR = 10;
    int lastRead2 = -1;
    UDPInputStream uis;
    float availWin;
    int currentRemoteTimeId;
    int closeOffset;
    boolean streamClose;
    boolean reveivedClose;
    static int m = 0;
    static int x;
    static int x2;
    static int c;
    boolean b;
    boolean b2;
    public int nw;
    long received;

    Receiver(ConnectionUDP conn) {
        this.availWin = (float)RUDPConfig.maxWin;
        this.streamClose = false;
        this.reveivedClose = false;
        this.b = false;
        this.conn = conn;
        this.uis = new UDPInputStream(conn);
        this.sender = conn.sender;
        this.dstIp = conn.dstIp;
        this.dstPort = conn.dstPort;
    }

    public byte[] receive() throws ConnectException {
        DataMessage me = null;
        if (this.conn.isConnected()) {
            me = (DataMessage)this.receiveTable.get(this.lastRead + 1);
            Object var2 = this.availOb;
            synchronized(this.availOb) {
                if (me == null) {
                    try {
                        this.availOb.wait();
                    } catch (InterruptedException var5) {
                        var5.printStackTrace();
                    }

                    me = (DataMessage)this.receiveTable.get(this.lastRead + 1);
                }
            }

            if (!this.streamClose) {
                this.checkCloseOffset_Remote();
                if (me == null) {
                    throw new ConnectException("连接已断开ccccccc");
                } else {
                    this.conn.sender.sendLastReadDelay();
                    ++this.lastRead;
                    var2 = this.availOb;
                    synchronized(this.availOb) {
                        this.receiveTable.remove(me.getSequence());
                    }

                    this.received += (long)me.getData().length;
                    return me.getData();
                }
            } else {
                throw new ConnectException("连接已断开");
            }
        } else {
            throw new ConnectException("连接未建立");
        }
    }

    public void onReceivePacket(DatagramPacket dp) {
        if (dp != null && this.conn.isConnected()) {
            int ver = MessageCheck.checkVer(dp);
            int sType = MessageCheck.checkSType(dp);
            if (ver == RUDPConfig.protocal_ver) {
                this.conn.live();
                if (sType == MessageType.sType_DataMessage) {
                    DataMessage me = new DataMessage(dp);
                    int timeId = me.getTimeId();
                    SendRecord record = (SendRecord)this.conn.clientControl.sendRecordTable_remote.get(timeId);
                    if (record == null) {
                        record = new SendRecord();
                        record.setTimeId(timeId);
                        this.conn.clientControl.sendRecordTable_remote.put(timeId, record);
                    }

                    record.addSended(me.getData().length);
                    if (timeId > this.currentRemoteTimeId) {
                        this.currentRemoteTimeId = timeId;
                    }

                    int sequence = me.getSequence();
                    this.conn.sender.sendAckDelay(me.getSequence());
                    if (sequence > this.lastRead) {
                        Object var8 = this.availOb;
                        synchronized(this.availOb) {
                            this.receiveTable.put(sequence, me);
                            if (this.receiveTable.containsKey(this.lastRead + 1)) {
                                this.availOb.notify();
                            }
                        }
                    }
                } else if (sType == MessageType.sType_AckListMessage) {
                    long t2 = System.currentTimeMillis();
                    AckListMessage alm = new AckListMessage(dp);
                    int lastRead3 = alm.getLastRead();
                    if (lastRead3 > this.lastRead2) {
                        this.lastRead2 = lastRead3;
                    }

                    ArrayList ackList = alm.getAckList();

                    for(int i = 0; i < ackList.size(); ++i) {
                        int sequence = (Integer)ackList.get(i);
                        this.conn.sender.removeSended_Ack(sequence);
                    }

                    SendRecord rc1 = this.conn.clientControl.getSendRecord(alm.getR1());
                    if (rc1 != null && alm.getS1() > rc1.getAckedSize()) {
                        rc1.setAckedSize(alm.getS1());
                    }

                    SendRecord rc2 = this.conn.clientControl.getSendRecord(alm.getR2());
                    if (rc2 != null && alm.getS2() > rc2.getAckedSize()) {
                        rc2.setAckedSize(alm.getS2());
                    }

                    SendRecord rc3 = this.conn.clientControl.getSendRecord(alm.getR3());
                    if (rc3 != null && alm.getS3() > rc3.getAckedSize()) {
                        rc3.setAckedSize(alm.getS3());
                    }

                    if (this.checkWin()) {
                        long t1 = System.currentTimeMillis();
                        this.conn.sender.play();
                        int var10000 = (int)(System.currentTimeMillis() - t1);
                    }
                } else if (sType == MessageType.sType_CloseMessage_Stream) {
                    CloseMessage_Stream cm = new CloseMessage_Stream(dp);
                    this.reveivedClose = true;
                    int n = cm.getCloseOffset();
                    this.closeStream_Remote(n);
                } else if (sType == MessageType.sType_CloseMessage_Conn) {
                    new CloseMessage_Conn(dp);
                    this.conn.close_remote();
                }
            }
        }

    }

    public void destroy() {
        Object var1 = this.availOb;
        synchronized(this.availOb) {
            this.receiveTable.clear();
        }
    }

    boolean checkWin() {
        this.nw = this.conn.sender.sendOffset - this.lastRead2;
        boolean b = false;
        if ((float)this.nw < this.availWin) {
            b = true;
        }

        return b;
    }

    void closeStream_Remote(int closeOffset) {
        this.closeOffset = closeOffset;
        if (!this.streamClose) {
            this.checkCloseOffset_Remote();
        }

    }

    void checkCloseOffset_Remote() {
        if (!this.streamClose && this.reveivedClose && this.lastRead >= this.closeOffset - 1) {
            this.streamClose = true;
            Object var1 = this.availOb;
            synchronized(this.availOb) {
                this.availOb.notifyAll();
            }

            this.conn.sender.closeStream_Remote();
        }

    }

    void closeStream_Local() {
        if (!this.streamClose) {
            ++c;
            this.streamClose = true;
            Object var1 = this.availOb;
            synchronized(this.availOb) {
                this.availOb.notifyAll();
            }

            this.conn.sender.closeStream_Local();
        }

    }

    public int getCurrentTimeId() {
        return this.currentRemoteTimeId;
    }

    public void setCurrentTimeId(int currentTimeId) {
        this.currentRemoteTimeId = currentTimeId;
    }

    public int getCloseOffset() {
        return this.closeOffset;
    }

    public void setCloseOffset(int closeOffset) {
        this.closeOffset = closeOffset;
    }
}
