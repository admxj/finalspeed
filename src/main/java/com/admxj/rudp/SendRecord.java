package com.admxj.rudp;

public class SendRecord
{
    int sendSize;
    int sendSize_First;
    int sendCount;
    int ackedSize;
    int timeId;
    int speed;
    boolean speedRecored = false;
    int resended;

    float getDropRate()
    {
        int droped = getSendSize() - getAckedSize();
        if (droped < 0) {
            droped = 0;
        }
        float dropRate = 0.0F;
        if (getSendSize() > 0) {
            dropRate = droped / getSendSize();
        }
        return dropRate;
    }

    float getResendRate()
    {
        float resendRate = 0.0F;
        if (getSendSize_First() > 0) {
            resendRate = getResended() / getSendSize_First();
        }
        return resendRate;
    }

    void addResended(int size)
    {
        this.resended += size;
    }

    void addSended(int size)
    {
        this.sendCount += 1;
        this.sendSize += size;
    }

    void addSended_First(int size)
    {
        this.sendSize_First += size;
    }

    public int getSendSize()
    {
        return this.sendSize;
    }

    public int getSendCount()
    {
        return this.sendCount;
    }

    public int getAckedSize()
    {
        return this.ackedSize;
    }

    public void setAckedSize(int ackedSize)
    {
        if (ackedSize > this.ackedSize) {
            this.ackedSize = ackedSize;
        }
    }

    public int getTimeId()
    {
        return this.timeId;
    }

    public void setTimeId(int timeId)
    {
        this.timeId = timeId;
    }

    public int getSpeed()
    {
        return this.speed;
    }

    public void setSpeed(int speed)
    {
        this.speed = speed;
        this.speedRecored = true;
    }

    public boolean isSpeedRecored()
    {
        return this.speedRecored;
    }

    public int getResended()
    {
        return this.resended;
    }

    public void setResended(int resended)
    {
        this.resended = resended;
    }

    public int getSendSize_First()
    {
        return this.sendSize_First;
    }

    public void setSendSize_First(int sendSize_First)
    {
        this.sendSize_First = sendSize_First;
    }
}
