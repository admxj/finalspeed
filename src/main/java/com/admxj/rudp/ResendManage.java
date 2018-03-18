package com.admxj.rudp;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

public class ResendManage
        implements Runnable
{
    boolean haveTask = false;
    Object signalOb = new Object();
    Thread mainThread;
    long vTime = 0L;
    long lastReSendTime;
    LinkedBlockingQueue<ResendItem> taskList = new LinkedBlockingQueue();

    public ResendManage()
    {
        Route.es.execute(this);
    }

    public void addTask(ConnectionUDP conn, int sequence)
    {
        ResendItem ri = new ResendItem(conn, sequence);
        ri.setResendTime(getNewResendTime(conn));
        this.taskList.add(ri);
    }

    long getNewResendTime(ConnectionUDP conn)
    {
        int delayAdd = conn.clientControl.pingDelay + (int)(conn.clientControl.pingDelay * RUDPConfig.reSendDelay);
        if (delayAdd < RUDPConfig.reSendDelay_min) {
            delayAdd = RUDPConfig.reSendDelay_min;
        }
        long time = System.currentTimeMillis() + delayAdd;
        return time;
    }

    public void run()
    {
        for (;;)
        {
            try
            {
                final ResendItem ri = (ResendItem)this.taskList.take();
                if (ri.conn.isConnected())
                {
                    long sleepTime = ri.getResendTime() - System.currentTimeMillis();
                    if (sleepTime > 0L) {
                        Thread.sleep(sleepTime);
                    }
                    ri.addCount();
                    if (ri.conn.sender.getDataMessage(ri.sequence) != null) {
                        if (!ri.conn.stopnow) {
                            Route.es.execute(new Runnable()
                            {
                                public void run()
                                {
                                    ri.conn.sender.reSend(ri.sequence, ri.getCount());
                                }
                            });
                        }
                    }
                    if (ri.getCount() < RUDPConfig.reSendTryTimes)
                    {
                        ri.setResendTime(getNewResendTime(ri.conn));
                        this.taskList.add(ri);
                    }
                }
                if (!ri.conn.clientControl.closed) {}
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }
}
