package com.admxj.cap;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import com.admxj.rudp.SendRecord;
import com.admxj.utils.MLog;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.EthernetPacket.EthernetHeader;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV4Packet.IpV4Header;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.TcpPacket.TcpHeader;
import org.pcap4j.packet.namednumber.TcpPort;
import org.pcap4j.util.MacAddress;

public class TCPTun
{
    HashMap<Integer, TcpPacket> sendedTable_server = new HashMap();
    HashMap<Integer, TcpPacket> sendedTable_history_server = new HashMap();
    int clientSequence = Integer.MIN_VALUE;
    static Random random = new Random();
    PcapHandle sendHandle;
    HashSet<Short> selfAckTable = new HashSet();
    HashMap<Integer, SendRecord> sendrecordTable = new HashMap();
    MacAddress dstMacaAddress;
    int sequenceNum = -1;
    Thread sendThread;
    boolean sended = false;
    Packet basePacket_server;
    short baseIdent = 100;
    IPacket dst_readed_packet;
    IPacket last_send_packet;
    int presend_server;
    ArrayList<IPacket> packetList = new ArrayList();
    HashMap<Integer, IPacket> packetTable_l = new HashMap();
    HashMap<Integer, IPacket> packetTable = new HashMap();
    ArrayList<IPacket> unacked_list = new ArrayList();
    Object syn_packetList = new Object();
    int max_client_ack = Integer.MIN_VALUE;
    int sendIndex = 0;
    long lasSetDelayTime = 0L;
    long lastDelay = 300L;
    Object syn_delay = new Object();
    Thread resendScanThread;
    boolean connectReady = false;
    boolean preDataReady = false;
    CapEnv capEnv;
    public Inet4Address remoteAddress;
    public short remotePort;
    int remoteStartSequence;
    int remoteSequence;
    int remoteIdent;
    int remoteSequence_max;
    Inet4Address localAddress;
    short localPort;
    int localStartSequence = random.nextInt();
    int localSequence;
    int localIdent = random.nextInt(32667);
    Object syn_send_data = new Object();
    long lastSendAckTime;
    long lastReceiveDataTime;
    long createTime = System.currentTimeMillis();
    String key;
    Object syn_ident = new Object();

    TCPTun(CapEnv capEnv, Inet4Address serverAddress, short serverPort, MacAddress srcAddress_mac, MacAddress dstAddrress_mac)
    {
        this.capEnv = capEnv;
        this.sendHandle = capEnv.sendHandle;
        this.remoteAddress = serverAddress;
        this.remotePort = serverPort;
        this.localAddress = capEnv.local_ipv4;
        this.localPort = ((short)(random.nextInt(55535) + 10000));
        Packet syncPacket = null;
        try
        {
            syncPacket = PacketUtils.createSync(srcAddress_mac, dstAddrress_mac, this.localAddress, this.localPort, serverAddress, serverPort, this.localStartSequence, getIdent());
            try
            {
                this.sendHandle.sendPacket(syncPacket);
                this.localSequence = (this.localStartSequence + 1);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            MLog.println("���������������������  ident " + this.localIdent);
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
        }
        MLog.println(syncPacket);
    }

    TCPTun(CapEnv capServerEnv, Inet4Address remoteAddress, short remotePort)
    {
        this.capEnv = capServerEnv;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.sendHandle = this.capEnv.sendHandle;
        this.localPort = capServerEnv.listenPort;
        this.localAddress = this.capEnv.local_ipv4;
    }

    void init_client(Inet4Address clientAddress, int clientPort, Inet4Address serverAddress, int serverPort, int client_start_sequence) {}

    void init_server(Inet4Address clientAddress, int clientPort, Inet4Address serverAddress, int serverPort, int client_start_sequence, int server_start_sequence) {}

    public void process_server(Packet packet, EthernetPacket.EthernetHeader ethernetHeader, IpV4Packet.IpV4Header ipV4Header, TcpPacket tcpPacket, boolean client)
    {
        TcpPacket.TcpHeader tcpHeader = tcpPacket.getHeader();
        if (!this.preDataReady)
        {
            if (!this.connectReady)
            {
                this.dstMacaAddress = ethernetHeader.getSrcAddr();
                if ((tcpHeader.getSyn()) && (!tcpHeader.getAck()))
                {
                    this.remoteStartSequence = tcpHeader.getSequenceNumber();
                    this.remoteSequence = (this.remoteStartSequence + 1);
                    this.remoteSequence_max = this.remoteSequence;
                    MLog.println("��������������������� " + this.remoteAddress.getHostAddress() + ":" + this.remotePort + "->" + this.localAddress.getHostAddress() + ":" + this.localPort + " ident " + ipV4Header.getIdentification());
                    MLog.println(packet);
                    Packet responePacket = PacketUtils.createSyncAck(
                            this.capEnv.local_mac,
                            this.capEnv.gateway_mac,
                            this.localAddress, this.localPort,
                            ipV4Header.getSrcAddr(), ((Short)tcpHeader.getSrcPort().value()).shortValue(),
                            tcpHeader.getSequenceNumber() + 1, this.localStartSequence, (short)0);
                    try
                    {
                        this.sendHandle.sendPacket(responePacket);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    this.localSequence = (this.localStartSequence + 1);
                    MLog.println("��������������������� " + this.capEnv.local_mac + "->" + this.capEnv.gateway_mac + " " + this.localAddress + "->" + " ident " + 0);

                    MLog.println(responePacket);
                }
                if ((!tcpHeader.getSyn()) && (tcpHeader.getAck()))
                {

                    if (tcpPacket.getPayload() == null) {
                        if (tcpHeader.getAcknowledgmentNumber() == this.localSequence)
                        {
                            MLog.println("���������������������  ident " + ipV4Header.getIdentification());
                            MLog.println(packet);
                            new Thread()
                            {
                                public void run() {}
                            };
                            this.connectReady = true;
                        }
                    }
                    this.sendedTable_server.remove(Integer.valueOf(tcpHeader.getAcknowledgmentNumber()));
                    this.selfAckTable.contains(Short.valueOf(ipV4Header.getIdentification()));
                }
            }
            else if (tcpPacket.getPayload() != null)
            {
                this.preDataReady = true;
                onReceiveDataPacket(tcpPacket, tcpHeader, ipV4Header);
                byte[] sim = getSimResponeHead();
                sendData(sim);
            }
        }
        else if (tcpPacket.getPayload() != null)
        {
            onReceiveDataPacket(tcpPacket, tcpHeader, ipV4Header);
            TunData td = new TunData();
            td.tun = this;
            td.data = tcpPacket.getPayload().getRawData();
            this.capEnv.vDatagramSocket.onReceinveFromTun(td);
        }
        if (tcpHeader.getRst()) {
            MLog.println("reset packet " + ipV4Header.getIdentification() + " " + tcpHeader.getSequenceNumber() + " " + this.remoteAddress.getHostAddress() + ":" + this.remotePort + "->" + this.localAddress.getHostAddress() + ":" + this.localPort + " " + " ident " + ipV4Header.getIdentification());
        }
    }

    public void process_client(CapEnv capEnv, Packet packet, EthernetPacket.EthernetHeader ethernetHeader, IpV4Packet.IpV4Header ipV4Header, TcpPacket tcpPacket, boolean client)
    {
        TcpPacket.TcpHeader tcpHeader = tcpPacket.getHeader();
        byte[] payload = null;
        if (tcpPacket.getPayload() != null) {
            payload = tcpPacket.getPayload().getRawData();
        }
        if (!this.preDataReady)
        {
            if (!this.connectReady)
            {
                if ((tcpHeader.getAck()) && (tcpHeader.getSyn()) &&
                        (tcpHeader.getAcknowledgmentNumber() == this.localStartSequence + 1))
                {
                    MLog.println("���������������������  ident " + ipV4Header.getIdentification());
                    MLog.println(packet);
                    this.remoteStartSequence = tcpHeader.getSequenceNumber();
                    this.remoteSequence = (this.remoteStartSequence + 1);
                    this.remoteSequence_max = this.remoteSequence;
                    Packet p3 = PacketUtils.createAck(capEnv.local_mac, capEnv.gateway_mac, capEnv.local_ipv4, this.localPort, this.remoteAddress, this.remotePort, this.remoteSequence, this.localSequence, getIdent());
                    try
                    {
                        this.sendHandle.sendPacket(p3);
                        MLog.println("���������������������  ident " + this.localIdent);
                        MLog.println(p3);
                        this.connectReady = true;

                        byte[] sim = getSimRequestHead(this.remotePort);
                        sendData(sim);
                        MLog.println("������������  ident " + this.localIdent);
                    }
                    catch (PcapNativeException e)
                    {
                        e.printStackTrace();
                    }
                    catch (NotOpenException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            else if (tcpPacket.getPayload() != null)
            {
                this.preDataReady = true;
                onReceiveDataPacket(tcpPacket, tcpHeader, ipV4Header);
                MLog.println("������������  ident " + ipV4Header.getIdentification());
            }
        }
        else if (tcpPacket.getPayload() != null)
        {
            onReceiveDataPacket(tcpPacket, tcpHeader, ipV4Header);
            TunData td = new TunData();
            td.tun = this;
            td.data = tcpPacket.getPayload().getRawData();
            capEnv.vDatagramSocket
                    .onReceinveFromTun(td);
        }
        if (tcpHeader.getRst()) {
            MLog.println("reset packet " + ipV4Header.getIdentification() + " " + tcpHeader.getSequenceNumber() + " " + this.remoteAddress.getHostAddress() + ":" + this.remotePort + "->" + this.localAddress.getHostAddress() + ":" + this.localPort);
        }
    }

    void onReceiveDataPacket(TcpPacket tcpPacket, TcpPacket.TcpHeader tcpHeader, IpV4Packet.IpV4Header ipV4Header)
    {
        if (System.currentTimeMillis() - this.lastSendAckTime > 1000L)
        {
            int rs = tcpHeader.getSequenceNumber() + tcpPacket.getPayload().getRawData().length;
            if (rs > this.remoteSequence_max) {
                this.remoteSequence_max = rs;
            }
            Packet ackPacket = PacketUtils.createAck(
                    this.capEnv.local_mac,
                    this.capEnv.gateway_mac,
                    this.localAddress, this.localPort,
                    ipV4Header.getSrcAddr(), ((Short)tcpHeader.getSrcPort().value()).shortValue(),
                    this.remoteSequence_max, this.localSequence, getIdent());
            try
            {
                this.sendHandle.sendPacket(ackPacket);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            this.lastSendAckTime = System.currentTimeMillis();
            this.lastReceiveDataTime = System.currentTimeMillis();
        }
    }

    void sendData(byte[] data)
    {
        Packet dataPacket = PacketUtils.createDataPacket(this.capEnv.local_mac,
                this.capEnv.gateway_mac,
                this.localAddress, this.localPort,
                this.remoteAddress, this.remotePort,
                this.localSequence, this.remoteSequence_max, data, getIdent());
        synchronized (this.syn_send_data)
        {
            try
            {
                this.sendHandle.sendPacket(dataPacket);
                this.localSequence += data.length;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    short getIdent()
    {
        synchronized (this.syn_ident)
        {
            this.localIdent += 1;
            if (this.localIdent >= 32767) {
                this.localIdent = 0;
            }
        }
        return (short)this.localIdent;
    }

    public static byte[] getSimResponeHead()
    {
        StringBuffer sb = new StringBuffer();

        sb.append("HTTP/1.1 200 OK\r\n");
        sb.append("Server: Apache/2.2.15 (CentOS)\r\n");
        sb.append("Accept-Ranges: bytes\r\n");
        sb.append("Content-Length: " + Math.abs(random.nextInt()) + "\r\n");
        sb.append("Connection: Keep-Alive\r\n");
        sb.append("Content-Type: application/octet-stream\r\n");
        sb.append("\r\n");

        String simRequest = sb.toString();
        byte[] simData = simRequest.getBytes();
        return simData;
    }

    public static byte[] getSimRequestHead(int port)
    {
        StringBuffer sb = new StringBuffer();
        String domainName = getRandomString(5 + random.nextInt(10)) + ".com";
        sb.append("GET /" + getRandomString(8 + random.nextInt(10)) + "." + getRandomString(2 + random.nextInt(5)) + " HTTP/1.1" + "\r\n");
        sb.append("Accept: application/x-ms-application, image/jpeg, application/xaml+xml, image/gif, image/pjpeg, application/x-ms-xbap, */*\r\n");
        sb.append("Accept-Language: zh-CN\r\n");
        sb.append("Accept-Encoding: gzip, deflate\r\n");
        sb.append("User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0\r\n");
        sb.append("Host: " + domainName + "\r\n");
        sb.append("Connection: Keep-Alive\r\n");
        sb.append("\r\n");
        String simRequest = sb.toString();
        byte[] simData = simRequest.getBytes();
        return simData;
    }

    public static String getRandomString(int length)
    {
        String base = "abcdefghkmnopqrstuvwxyz";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++)
        {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    public InetAddress getSourcrAddress()
    {
        return this.localAddress;
    }

    public int getSourcePort()
    {
        return this.localPort;
    }

    public void setSourcePort(short sourcePort)
    {
        this.localPort = sourcePort;
    }

    public boolean isConnectReady()
    {
        return this.connectReady;
    }

    public void setConnectReady(boolean connectReady)
    {
        this.connectReady = connectReady;
    }

    public String getKey()
    {
        return this.key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }
}
