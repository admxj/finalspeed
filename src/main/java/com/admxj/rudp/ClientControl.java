package com.admxj.rudp;

import com.admxj.rudp.message.MessageType;
import com.admxj.rudp.message.PingMessage;
import com.admxj.rudp.message.PingMessage2;
import com.admxj.utils.ByteIntConvert;
import com.admxj.utils.MLog;
import com.admxj.utils.MessageCheck;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;


public class ClientControl
{
    Object synlock = new Object();
    private HashMap<Integer, SendRecord> sendRecordTable = new HashMap();
    HashMap<Integer, SendRecord> sendRecordTable_remote = new HashMap();
    long startSendTime = 0L;
    int maxSpeed = 1048576;
    int initSpeed = this.maxSpeed;
    int currentSpeed = this.initSpeed;
    int lastTime = -1;
    Object syn_timeid = new Object();
    long sended = 0L;
    long markTime = 0L;
    long lastReceivePingTime = System.currentTimeMillis();
    Random ran = new Random();
    HashMap<Integer, Long> pingTable = new HashMap();
    public int pingDelay = 250;
    int clientId_real = -1;
    int maxAcked = 0;
    public HashMap<Integer, ConnectionUDP> connTable = new HashMap();
    Object syn_connTable = new Object();
    Object syn_tunTable = new Object();
    boolean closed = false;
    public ResendManage resendMange = new ResendManage();
    int clientId;
    Thread sendThread;
    long lastSendPingTime;
    long needSleep_All;
    long trueSleep_All;
    long lastLockTime;
    Route route;
    InetAddress dstIp;
    int dstPort;
    String password;

    ClientControl(Route route, int clientId, InetAddress dstIp, int dstPort)
    {
        this.clientId = clientId;
        this.route = route;
        this.dstIp = dstIp;
        this.dstPort = dstPort;
    }

    public void onReceivePacket(DatagramPacket dp)
    {
        byte[] dpData = dp.getData();
        int sType = 0;
        sType = MessageCheck.checkSType(dp);
        int remote_clientId = ByteIntConvert.toInt(dpData, 8);
        if (sType == MessageType.sType_PingMessage)
        {
            PingMessage pm = new PingMessage(dp);
            sendPingMessage2(pm.getPingId(), dp.getAddress(), dp.getPort());
            this.currentSpeed = (pm.getDownloadSpeed() * 1024);
        }
        else if (sType == MessageType.sType_PingMessage2)
        {
            PingMessage2 pm = new PingMessage2(dp);
            this.lastReceivePingTime = System.currentTimeMillis();
            Long t = (Long)this.pingTable.get(Integer.valueOf(pm.getPingId()));
            if (t != null)
            {
                this.pingDelay = ((int)(System.currentTimeMillis() - t.longValue()));
                String protocal = "";
                if (this.route.isUseTcpTun()) {
                    protocal = "tcp";
                } else {
                    protocal = "udp";
                }
                MLog.println("delay_" + protocal + " " + this.pingDelay + "ms " + dp.getAddress().getHostAddress() + ":" + dp.getPort());
            }
        }
    }

    public void sendPacket(DatagramPacket dp)
            throws IOException
    {
        this.route.sendPacket(dp);
    }

    void addConnection(ConnectionUDP conn)
    {
        synchronized (this.syn_connTable)
        {
            this.connTable.put(Integer.valueOf(conn.connectId), conn);
        }
    }

    void removeConnection(ConnectionUDP conn)
    {
        synchronized (this.syn_connTable)
        {
            this.connTable.remove(Integer.valueOf(conn.connectId));
        }
    }

    public void close()
    {
        this.closed = true;
        this.route.clientManager.removeClient(this.clientId);
        synchronized (this.syn_connTable)
        {
            Iterator<Integer> it = getConnTableIterator();
            while (it.hasNext())
            {
                final ConnectionUDP conn = (ConnectionUDP)this.connTable.get(it.next());
                if (conn != null) {
                    Route.es.execute(new Runnable()
                    {
                        public void run()
                        {
                            conn.stopnow = true;
                            conn.destroy(true);
                        }
                    });
                }
            }
        }
    }

    Iterator<Integer> getConnTableIterator()
    {
        Iterator<Integer> it = null;
        synchronized (this.syn_connTable)
        {
            it = new CopiedIterator(this.connTable.keySet().iterator());
        }
        return it;
    }

    public void updateClientId(int newClientId)
    {
        this.clientId_real = newClientId;
        this.sendRecordTable.clear();
        this.sendRecordTable_remote.clear();
    }

    public void onSendDataPacket(ConnectionUDP conn) {}

    public void sendPingMessage()
    {
        int pingid = Math.abs(this.ran.nextInt());
        long pingTime = System.currentTimeMillis();
        this.pingTable.put(Integer.valueOf(pingid), Long.valueOf(pingTime));
        this.lastSendPingTime = System.currentTimeMillis();
        PingMessage lm = new PingMessage(0, this.route.localclientId, pingid, Route.localDownloadSpeed, Route.localUploadSpeed);
        lm.setDstAddress(this.dstIp);
        lm.setDstPort(this.dstPort);
        try
        {
            sendPacket(lm.getDatagramPacket());
        }
        catch (IOException localIOException) {}
    }

    public void sendPingMessage2(int pingId, InetAddress dstIp, int dstPort)
    {
        PingMessage2 lm = new PingMessage2(0, this.route.localclientId, pingId);
        lm.setDstAddress(dstIp);
        lm.setDstPort(dstPort);
        try
        {
            sendPacket(lm.getDatagramPacket());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void onReceivePing(PingMessage pm)
    {
        if (this.route.mode == 2) {
            this.currentSpeed = (pm.getDownloadSpeed() * 1024);
        }
    }

    SendRecord getSendRecord(int timeId)
    {
        SendRecord record = null;
        synchronized (this.syn_timeid)
        {
            record = (SendRecord)this.sendRecordTable.get(Integer.valueOf(timeId));
            if (record == null)
            {
                record = new SendRecord();
                record.setTimeId(timeId);
                this.sendRecordTable.put(Integer.valueOf(timeId), record);
            }
        }
        return record;
    }

    public int getCurrentTimeId()
    {
        long current = System.currentTimeMillis();
        if (this.startSendTime == 0L) {
            this.startSendTime = current;
        }
        int timeId = (int)((current - this.startSendTime) / 1000L);
        return timeId;
    }

    public int getTimeId(long time)
    {
        int timeId = (int)((time - this.startSendTime) / 1000L);
        return timeId;
    }

    public synchronized void sendSleep(long startTime, int length)
    {
        if (this.route.mode == 1) {
            this.currentSpeed = Route.localUploadSpeed;
        }
        if (this.sended == 0L) {
            this.markTime = startTime;
        }
        this.sended += length;
        if (this.sended > 10240L)
        {
            long needTime = (long) (1.0E9F * (float)this.sended / this.currentSpeed);
            long usedTime = System.nanoTime() - this.markTime;
            if (usedTime < needTime)
            {
                long sleepTime = needTime - usedTime;
                this.needSleep_All += sleepTime;

                long moreTime = this.trueSleep_All - this.needSleep_All;
                if ((moreTime > 0L) &&
                        (sleepTime <= moreTime))
                {
                    sleepTime = 0L;
                    this.trueSleep_All -= sleepTime;
                }
                long s = needTime / 1000000L;
                int n = (int)(needTime % 1000000L);
                long t1 = System.nanoTime();
                if (sleepTime > 0L)
                {
                    try
                    {
                        Thread.sleep(s, n);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                    this.trueSleep_All += System.nanoTime() - t1;
                }
            }
            this.sended = 0L;
        }
    }

    public Object getSynlock()
    {
        return this.synlock;
    }

    public void setSynlock(Object synlock)
    {
        this.synlock = synlock;
    }

    public void setClientId(int clientId)
    {
        this.clientId = clientId;
    }

    public int getClientId_real()
    {
        return this.clientId_real;
    }

    public void setClientId_real(int clientId_real)
    {
        this.clientId_real = clientId_real;
        this.lastReceivePingTime = System.currentTimeMillis();
    }

    public long getLastSendPingTime()
    {
        return this.lastSendPingTime;
    }

    public void setLastSendPingTime(long lastSendPingTime)
    {
        this.lastSendPingTime = lastSendPingTime;
    }

    public long getLastReceivePingTime()
    {
        return this.lastReceivePingTime;
    }

    public void setLastReceivePingTime(long lastReceivePingTime)
    {
        this.lastReceivePingTime = lastReceivePingTime;
    }

    public String getPassword()
    {
        return this.password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }
}
