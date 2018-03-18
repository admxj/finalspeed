package com.admxj.cap;

import com.admxj.rudp.Route;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

public class VDatagramSocket
        extends DatagramSocket
{
    boolean useTcpTun = true;
    boolean client = true;
    LinkedBlockingQueue<TunData> packetList = new LinkedBlockingQueue();
    CapEnv capEnv;
    int localPort;
    Object syn_tun = new Object();
    boolean tunConnecting = false;

    public VDatagramSocket()
            throws SocketException
    {}

    public VDatagramSocket(int port)
            throws SocketException
    {
        this.localPort = port;
    }

    public int getLocalPort()
    {
        return this.localPort;
    }

    public void send(DatagramPacket p)
            throws IOException
    {
        TCPTun tun = null;
        if (this.client)
        {
            tun = this.capEnv.tcpManager.getDefaultTcpTun();
            if (tun != null)
            {
                if ((!tun.remoteAddress.getHostAddress().equals(p.getAddress().getHostAddress())) ||
                        (CapEnv.toUnsigned(tun.remotePort) != p.getPort()))
                {
                    this.capEnv.tcpManager.removeTun(tun);
                    this.capEnv.tcpManager.setDefaultTcpTun(null);
                }
            }
            else
            {
                tryConnectTun_Client(p.getAddress(), (short)p.getPort());
                tun = this.capEnv.tcpManager.getDefaultTcpTun();
            }
        }
        else
        {
            tun = this.capEnv.tcpManager.getTcpConnection_Server(p.getAddress().getHostAddress(), (short)p.getPort());
        }
        if (tun != null)
        {
            if (tun.preDataReady) {
                tun.sendData(p.getData());
            } else {
                throw new IOException("���������������!");
            }
        }
        else {
            throw new IOException("���������������!  thread " + Route.es.getActiveCount() + " " + p.getAddress() + ":" + p.getPort());
        }
    }

    void tryConnectTun_Client(InetAddress dstAddress, short dstPort)
    {
        synchronized (this.syn_tun)
        {
            if (this.capEnv.tcpManager.getDefaultTcpTun() == null) {
                if (this.tunConnecting)
                {
                    try
                    {
                        this.syn_tun.wait();
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    this.tunConnecting = true;
                    try
                    {
                        this.capEnv.createTcpTun_Client(dstAddress.getHostAddress(), dstPort);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    this.tunConnecting = false;
                }
            }
        }
    }

    public synchronized void receive(DatagramPacket p)
            throws IOException
    {
        TunData td = null;
        try
        {
            td = (TunData)this.packetList.take();
            p.setData(td.data);
            p.setLength(td.data.length);
            p.setAddress(td.tun.remoteAddress);
            p.setPort(CapEnv.toUnsigned(td.tun.remotePort));
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    void onReceinveFromTun(TunData td)
    {
        this.packetList.add(td);
    }

    public boolean isClient()
    {
        return this.client;
    }

    public void setClient(boolean client)
    {
        this.client = client;
    }

    public CapEnv getCapEnv()
    {
        return this.capEnv;
    }

    public void setCapEnv(CapEnv capEnv)
    {
        this.capEnv = capEnv;
        capEnv.vDatagramSocket = this;
    }
}

