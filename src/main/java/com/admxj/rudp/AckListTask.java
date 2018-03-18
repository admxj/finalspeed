package com.admxj.rudp;

import com.admxj.rudp.message.AckListMessage;

import java.util.ArrayList;
import java.util.HashSet;

public class AckListTask
{
    ConnectionUDP conn;
    AckListMessage alm;
    int lastRead = 0;
    ArrayList<Integer> ackList;
    HashSet set;

    AckListTask(ConnectionUDP conn)
    {
        this.conn = conn;
        this.ackList = new ArrayList();
        this.set = new HashSet();
    }

    synchronized void addAck(int sequence)
    {
        if (!this.set.contains(Integer.valueOf(sequence)))
        {
            this.ackList.add(Integer.valueOf(sequence));
            this.set.add(Integer.valueOf(sequence));
        }
    }

    synchronized void run()
    {
        int offset = 0;
        int packetLength = RUDPConfig.ackListSum;
        int length = this.ackList.size();

        int sum = length / packetLength;
        if (length % packetLength != 0) {
            sum++;
        }
        if (sum == 0) {
            sum = 1;
        }
        int len = packetLength;
        if (length <= len)
        {
            this.conn.sender.sendALMessage(this.ackList);
            this.conn.sender.sendALMessage(this.ackList);
        }
        else
        {
            for (int i = 0; i < sum; i++)
            {
                ArrayList<Integer> nl = copy(offset, len, this.ackList);
                this.conn.sender.sendALMessage(nl);
                this.conn.sender.sendALMessage(nl);

                offset += packetLength;
                if (offset + len > length) {
                    len = length - (sum - 1) * packetLength;
                }
            }
        }
    }

    ArrayList<Integer> copy(int offset, int length, ArrayList<Integer> ackList)
    {
        ArrayList<Integer> nl = new ArrayList();
        for (int i = 0; i < length; i++) {
            nl.add((Integer)ackList.get(offset + i));
        }
        return nl;
    }
}
