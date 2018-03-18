package com.admxj.utils;

import java.util.Vector;

public class NetStatus
{
    public long uploadSum;
    public long downloadSum;
    Thread mainThread;
    int averageTime;
    Vector<SpeedUnit> speedList;
    SpeedUnit currentUnit;
    public int upSpeed = 0;
    public int downSpeed = 0;

    public NetStatus()
    {
        this(2);
    }

    public NetStatus(int averageTime)
    {
        this.averageTime = averageTime;
        this.speedList = new Vector();
        for (int i = 0; i < averageTime; i++)
        {
            SpeedUnit unit = new SpeedUnit();
            if (i == 0) {
                this.currentUnit = unit;
            }
            this.speedList.add(unit);
        }
        this.mainThread = new Thread()
        {
            public void run()
            {
                long lastTime = System.currentTimeMillis();
                for (;;)
                {
                    if (Math.abs(System.currentTimeMillis() - lastTime) > 1000L)
                    {
                        lastTime = System.currentTimeMillis();
                        NetStatus.this.calcuSpeed();
                    }
                    try
                    {
                        Thread.sleep(100L);
                    }
                    catch (InterruptedException localInterruptedException) {}
                }
            }
        };
        this.mainThread.start();
    }

    public void stop()
    {
        this.mainThread.interrupt();
    }

    public int getUpSpeed()
    {
        return this.upSpeed;
    }

    public void setUpSpeed(int upSpeed)
    {
        this.upSpeed = upSpeed;
    }

    public int getDownSpeed()
    {
        return this.downSpeed;
    }

    public void setDownSpeed(int downSpeed)
    {
        this.downSpeed = downSpeed;
    }

    void calcuSpeed()
    {
        int ds = 0;int us = 0;
        for (SpeedUnit unit : this.speedList)
        {
            ds += unit.downSum;
            us += unit.upSum;
        }
        this.upSpeed = ((int)(us / this.speedList.size()));
        this.downSpeed = ((int)ds / this.speedList.size());

        this.speedList.remove(0);
        SpeedUnit unit = new SpeedUnit();
        this.currentUnit = unit;
        this.speedList.add(unit);
    }

    public void addDownload(int sum)
    {
        this.downloadSum += sum;
        this.currentUnit.addDown(sum);
    }

    public void addUpload(int sum)
    {
        this.uploadSum += sum;
        this.currentUnit.addUp(sum);
    }

    public void sendAvail() {}

    public void receiveAvail() {}

    public void setUpLimite(int speed) {}

    public void setDownLimite(int speed) {}
}
