package com.admxj.rudp;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

public class ConnectionUDP {
    public InetAddress dstIp;
    public int dstPort;
    public Sender sender;
    public Receiver receiver;
    public UDPOutputStream uos;
    public UDPInputStream uis;
    long connetionId;
    Route route;
    int mode;
    private boolean connected = true;
    long lastLiveTime = System.currentTimeMillis();
    long lastSendLiveTime = 0L;
    static Random ran = new Random();
    int connectId;
    ConnectionProcessor connectionProcessor;
    private LinkedBlockingQueue<DatagramPacket> dpBuffer = new LinkedBlockingQueue();
    public ClientControl clientControl;
    public boolean localClosed = false;
    public boolean remoteClosed = false;
    public boolean destroied = false;
    public boolean stopnow = false;

    public ConnectionUDP(Route ro, InetAddress dstIp, int dstPort, int mode, int connectId, ClientControl clientControl)
            throws Exception
    {
        this.clientControl = clientControl;
        this.route = ro;
        this.dstIp = dstIp;
        this.dstPort = dstPort;
        this.mode = mode;
        if (mode != 1) {}
        this.connectId = connectId;
        try
        {
            this.sender = new Sender(this);
            this.receiver = new Receiver(this);
            this.uos = new UDPOutputStream(this);
            this.uis = new UDPInputStream(this);
            if (mode == 2) {
                ro.createTunnelProcessor().process(this);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            this.connected = false;
            this.route.connTable.remove(Integer.valueOf(connectId));
            e.printStackTrace();
            synchronized (this)
            {
                notifyAll();
            }
            throw e;
        }
        synchronized (this)
        {
            notifyAll();
        }
    }

    public DatagramPacket getPacket(int connectId)
            throws InterruptedException
    {
        DatagramPacket dp = (DatagramPacket)this.dpBuffer.take();
        return dp;
    }

    public String toString()
    {
        return new String(this.dstIp + ":" + this.dstPort);
    }

    public boolean isConnected()
    {
        return this.connected;
    }

    public void close_local()
    {
        if (!this.localClosed)
        {
            this.localClosed = true;
            if (!this.stopnow) {
                this.sender.sendCloseMessage_Conn();
            }
            destroy(false);
        }
    }

    public void close_remote()
    {
        if (!this.remoteClosed)
        {
            this.remoteClosed = true;
            destroy(false);
        }
    }

    public void destroy(boolean force)
    {
        if ((!this.destroied) && (
                ((this.localClosed) && (this.remoteClosed)) || (force)))
        {
            this.destroied = true;
            this.connected = false;
            this.uis.closeStream_Local();
            this.uos.closeStream_Local();
            this.sender.destroy();
            this.receiver.destroy();
            this.route.removeConnection(this);
            this.clientControl.removeConnection(this);
        }
    }

    public void close_timeout() {}

    void live()
    {
        this.lastLiveTime = System.currentTimeMillis();
    }
}
