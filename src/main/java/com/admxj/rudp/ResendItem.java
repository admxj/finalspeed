package com.admxj.rudp;

public class ResendItem
{
    int count;
    ConnectionUDP conn;
    int sequence;
    long resendTime;

    ResendItem(ConnectionUDP conn, int sequence)
    {
        this.conn = conn;
        this.sequence = sequence;
    }

    void addCount()
    {
        this.count += 1;
    }

    public int getCount()
    {
        return this.count;
    }

    public void setCount(int count)
    {
        this.count = count;
    }

    public ConnectionUDP getConn()
    {
        return this.conn;
    }

    public void setConn(ConnectionUDP conn)
    {
        this.conn = conn;
    }

    public int getSequence()
    {
        return this.sequence;
    }

    public void setSequence(int sequence)
    {
        this.sequence = sequence;
    }

    public long getResendTime()
    {
        return this.resendTime;
    }

    public void setResendTime(long resendTime)
    {
        this.resendTime = resendTime;
    }
}

