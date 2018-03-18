package com.admxj.rudp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class AckListManage
        implements Runnable
{
    Thread mainThread;
    HashMap<Integer, AckListTask> taskTable;

    public AckListManage()
    {
        this.taskTable = new HashMap();
        this.mainThread = new Thread(this);
        this.mainThread.start();
    }

    synchronized void addAck(ConnectionUDP conn, int sequence)
    {
        if (!this.taskTable.containsKey(Integer.valueOf(conn.connectId)))
        {
            AckListTask at = new AckListTask(conn);
            this.taskTable.put(Integer.valueOf(conn.connectId), at);
        }
        AckListTask at = (AckListTask)this.taskTable.get(Integer.valueOf(conn.connectId));
        at.addAck(sequence);
    }

    synchronized void addLastRead(ConnectionUDP conn)
    {
        if (!this.taskTable.containsKey(Integer.valueOf(conn.connectId)))
        {
            AckListTask at = new AckListTask(conn);
            this.taskTable.put(Integer.valueOf(conn.connectId), at);
        }
    }

    public void run()
    {
        for (;;)
        {
            synchronized (this)
            {
                Iterator<Integer> it = this.taskTable.keySet().iterator();
                continue;
//                int id = ((Integer)it.next()).intValue();
//                AckListTask at = (AckListTask)this.taskTable.get(Integer.valueOf(id));
//                at.run();
//                if (it.hasNext()) {
//                    continue;
//                }
//                this.taskTable.clear();
//                this.taskTable = null;
//                this.taskTable = new HashMap();
            }
//            try
//            {
//                Thread.sleep(RUDPConfig.ackListDelay);
//            }
//            catch (InterruptedException e)
//            {
//                e.printStackTrace();
//            }
        }
    }
}

