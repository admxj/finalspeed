package com.admxj.cap;

import com.admxj.rudp.CopiedIterator;
import com.admxj.utils.MLog;

import java.net.Inet4Address;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class TunManager
{
    HashMap<String, TCPTun> connTable = new HashMap();
    static TunManager tunManager;
    TCPTun defaultTcpTun;
    Thread scanThread;
    Object syn_scan;
    CapEnv capEnv;

    TunManager(CapEnv capEnv)
    {
        tunManager = this;

        this.syn_scan = new Object();

        this.scanThread = new Thread()
        {
            public void run()
            {
                for (;;)
                {
                    try
                    {
                        Thread.sleep(1000L);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                    TunManager.this.scan();
                }
            }
        };
        this.scanThread.start();

        this.capEnv = capEnv;
    }

    void scan()
    {
        Iterator<String> it = getConnTableIterator();
        while (it.hasNext())
        {
            String key = (String)it.next();
            TCPTun tun = (TCPTun)this.connTable.get(key);
            if (tun != null) {
                if (tun.preDataReady)
                {
                    long t = System.currentTimeMillis() - tun.lastReceiveDataTime;
                    if (t > 6000L)
                    {
                        this.connTable.remove(key);
                        if (this.capEnv.client)
                        {
                            this.defaultTcpTun = null;
                            MLog.println("tcp������������");
                        }
                    }
                }
                else if (System.currentTimeMillis() - tun.createTime > 5000L)
                {
                    this.connTable.remove(key);
                }
            }
        }
    }

    public void removeTun(TCPTun tun)
    {
        this.connTable.remove(tun.key);
    }

    Iterator<String> getConnTableIterator()
    {
        Iterator<String> it = null;
        synchronized (this.syn_scan)
        {
            it = new CopiedIterator(this.connTable.keySet().iterator());
        }
        return it;
    }

    public static TunManager get()
    {
        return tunManager;
    }

    public TCPTun getTcpConnection_Client(String remoteAddress, short remotePort, short localPort)
    {
        return (TCPTun)this.connTable.get(remoteAddress + ":" + remotePort + ":" + localPort);
    }

    public void addConnection_Client(TCPTun conn)
    {
        synchronized (this.syn_scan)
        {
            String key = conn.remoteAddress.getHostAddress() + ":" + conn.remotePort + ":" + conn.localPort;

            conn.setKey(key);
            this.connTable.put(key, conn);
        }
    }

    public TCPTun getTcpConnection_Server(String remoteAddress, short remotePort)
    {
        return (TCPTun)this.connTable.get(remoteAddress + ":" + remotePort);
    }

    public void addConnection_Server(TCPTun conn)
    {
        synchronized (this.syn_scan)
        {
            String key = conn.remoteAddress.getHostAddress() + ":" + conn.remotePort;

            conn.setKey(key);
            this.connTable.put(key, conn);
        }
    }

    public TCPTun getDefaultTcpTun()
    {
        return this.defaultTcpTun;
    }

    public void setDefaultTcpTun(TCPTun defaultTcpTun)
    {
        this.defaultTcpTun = defaultTcpTun;
    }
}
