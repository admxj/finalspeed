package com.admxj.rudp;

import java.net.ConnectException;
import java.net.InetAddress;

public class UDPOutputStream
{
    public ConnectionUDP conn;
    InetAddress dstIp;
    int dstPort;
    Sender sender;
    boolean streamClosed = false;

    UDPOutputStream(ConnectionUDP conn)
    {
        this.conn = conn;
        this.dstIp = conn.dstIp;
        this.dstPort = conn.dstPort;
        this.sender = conn.sender;
    }

    public void write(byte[] data, int offset, int length)
            throws ConnectException, InterruptedException
    {
        this.sender.sendData(data, offset, length);
    }

    public void closeStream_Local()
    {
        this.sender.closeStream_Local();
    }
}
