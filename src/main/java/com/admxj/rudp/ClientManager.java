package com.admxj.rudp;

import com.admxj.utils.MLog;

import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;


public class ClientManager
{
    HashMap<Integer, ClientControl> clientTable = new HashMap();
    Thread mainThread;
    Route route;
    int receivePingTimeout = 8000;
    int sendPingInterval = 1000;
    Object syn_clientTable = new Object();

    ClientManager(Route route)
    {
        this.route = route;
        this.mainThread = new Thread()
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
                    ClientManager.this.scanClientControl();
                }
            }
        };
        this.mainThread.start();
    }

    void scanClientControl()
    {
        Iterator<Integer> it = getClientTableIterator();
        long current = System.currentTimeMillis();
        while (it.hasNext())
        {
            ClientControl cc = (ClientControl)this.clientTable.get(it.next());
            if (cc != null) {
                if (current - cc.getLastReceivePingTime() < this.receivePingTimeout)
                {
                    if (current - cc.getLastSendPingTime() > this.sendPingInterval) {
                        cc.sendPingMessage();
                    }
                }
                else
                {
                    MLog.println("������������client " + cc.dstIp.getHostAddress() + ":" + cc.dstPort + " " + new Date());
                    synchronized (this.syn_clientTable)
                    {
                        cc.close();
                    }
                }
            }
        }
    }

    void removeClient(int clientId)
    {
        this.clientTable.remove(Integer.valueOf(clientId));
    }

    Iterator<Integer> getClientTableIterator()
    {
        Iterator<Integer> it = null;
        synchronized (this.syn_clientTable)
        {
            it = new CopiedIterator(this.clientTable.keySet().iterator());
        }
        return it;
    }

    ClientControl getClientControl(int clientId, InetAddress dstIp, int dstPort)
    {
        ClientControl c = (ClientControl)this.clientTable.get(Integer.valueOf(clientId));
        if (c == null)
        {
            c = new ClientControl(this.route, clientId, dstIp, dstPort);
            synchronized (this.syn_clientTable)
            {
                this.clientTable.put(Integer.valueOf(clientId), c);
            }
        }
        return c;
    }
}
