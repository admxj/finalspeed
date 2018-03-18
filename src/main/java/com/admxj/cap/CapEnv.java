package com.admxj.cap;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import com.admxj.rudp.Route;
import com.admxj.utils.ByteShortConvert;
import com.admxj.utils.MLog;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapStat;
import org.pcap4j.core.Pcaps;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IllegalPacket;
import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.EthernetPacket.EthernetHeader;
import org.pcap4j.packet.IpV4Packet.IpV4Header;
import org.pcap4j.packet.TcpPacket.TcpHeader;
import org.pcap4j.util.MacAddress;

public class CapEnv {
    public MacAddress gateway_mac;
    public MacAddress local_mac;
    Inet4Address local_ipv4;
    public PcapHandle sendHandle;
    VDatagramSocket vDatagramSocket;
    String testIp_tcp = "";
    String testIp_udp = "5.5.5.5";
    String selectedInterfaceName = null;
    String selectedInterfaceDes = "";
    PcapNetworkInterface nif;
    private final int COUNT = -1;
    private final int READ_TIMEOUT = 1;
    private final int SNAPLEN = 10240;
    HashMap<Integer, TCPTun> tunTable = new HashMap();
    LinkedBlockingQueue<Packet> packetList = new LinkedBlockingQueue();
    Random random = new Random();
    boolean client = false;
    short listenPort;
    TunManager tcpManager = null;
    CapEnv capEnv = this;
    Thread versinMonThread;
    boolean detect_by_tcp = true;
    public boolean tcpEnable = true;
    boolean ppp = false;

    public CapEnv(boolean isClient) {
        this.client = isClient;
        this.tcpManager = new TunManager(this);
    }

    public void init() throws Exception {
        this.initInterface();
        Thread thread_process = new Thread() {
            public void run() {
                while(true) {
                    try {
                        Packet packet = (Packet)CapEnv.this.packetList.take();
                        EthernetPacket packet_eth = (EthernetPacket)packet;
                        EthernetHeader head_eth = packet_eth.getHeader();
                        IpV4Packet ipV4Packet = null;
                        if (CapEnv.this.ppp) {
                            ipV4Packet = CapEnv.this.getIpV4Packet_pppoe(packet_eth);
                        } else if (packet_eth.getPayload() instanceof IpV4Packet) {
                            ipV4Packet = (IpV4Packet)packet_eth.getPayload();
                        }

                        if (ipV4Packet != null) {
                            IpV4Header ipV4Header = ipV4Packet.getHeader();
                            if (ipV4Packet.getPayload() instanceof TcpPacket) {
                                TcpPacket tcpPacket = (TcpPacket)ipV4Packet.getPayload();
                                TcpHeader tcpHeader = tcpPacket.getHeader();
                                TCPTun conn;
                                if (CapEnv.this.client) {
                                    conn = CapEnv.this.tcpManager.getTcpConnection_Client(ipV4Header.getSrcAddr().getHostAddress(), (Short)tcpHeader.getSrcPort().value(), (Short)tcpHeader.getDstPort().value());
                                    if (conn != null) {
                                        conn.process_client(CapEnv.this.capEnv, packet, head_eth, ipV4Header, tcpPacket, false);
                                    }
                                } else {
                                    conn = null;
                                    conn = CapEnv.this.tcpManager.getTcpConnection_Server(ipV4Header.getSrcAddr().getHostAddress(), (Short)tcpHeader.getSrcPort().value());
                                    if ((Short)tcpHeader.getDstPort().value() == CapEnv.this.listenPort) {
                                        if (tcpHeader.getSyn() && !tcpHeader.getAck() && conn == null) {
                                            conn = new TCPTun(CapEnv.this.capEnv, ipV4Header.getSrcAddr(), (Short)tcpHeader.getSrcPort().value());
                                            CapEnv.this.tcpManager.addConnection_Server(conn);
                                        }

                                        conn = CapEnv.this.tcpManager.getTcpConnection_Server(ipV4Header.getSrcAddr().getHostAddress(), (Short)tcpHeader.getSrcPort().value());
                                        if (conn != null) {
                                            conn.process_server(packet, head_eth, ipV4Header, tcpPacket, true);
                                        }
                                    }
                                }
                            } else if (packet_eth.getPayload() instanceof IllegalPacket) {
                                MLog.println("IllegalPacket!!!");
                            }
                        }
                    } catch (InterruptedException var9) {
                        var9.printStackTrace();
                    } catch (IllegalRawDataException var10) {
                        var10.printStackTrace();
                    }
                }
            }
        };
        thread_process.start();
        Thread var10000 = new Thread() {
            public void run() {
                long t = System.currentTimeMillis();

                while(true) {
                    if (System.currentTimeMillis() - t > 5000L) {
                        MLog.info("休眠恢复");
                        MLog.println("重新初始化接口");

                        try {
                            CapEnv.this.initInterface();
                        } catch (Exception var4) {
                            var4.printStackTrace();
                        }
                    }

                    t = System.currentTimeMillis();

                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException var5) {
                        var5.printStackTrace();
                    }
                }
            }
        };
    }

    PromiscuousMode getMode(PcapNetworkInterface pi) {
        PromiscuousMode mode = null;
        String string = (pi.getDescription() + ":" + pi.getName()).toLowerCase();
        if (string.contains("wireless")) {
            mode = PromiscuousMode.NONPROMISCUOUS;
        } else {
            mode = PromiscuousMode.PROMISCUOUS;
        }

        return mode;
    }

    void initInterface() throws Exception {
        this.detectInterface();
        List<PcapNetworkInterface> allDevs = Pcaps.findAllDevs();
        MLog.println("网络接口列表10: ");
        Iterator var3 = allDevs.iterator();

        while(var3.hasNext()) {
            PcapNetworkInterface pi = (PcapNetworkInterface)var3.next();
            String desString = "";
            if (pi.getDescription() != null) {
                desString = pi.getDescription();
            }

            MLog.info("  " + desString + "   " + pi.getName());
            if (pi.getName().equals(this.selectedInterfaceName) && desString.equals(this.selectedInterfaceDes)) {
                this.nif = pi;
            }
        }

        if (this.nif != null) {
            String desString = "";
            if (this.nif.getDescription() != null) {
                desString = this.nif.getDescription();
            }

            MLog.info("自动选择网络接口:\n  " + desString + "   " + this.nif.getName());
        } else {
            this.tcpEnable = false;
        }

        if (this.tcpEnable) {
            this.sendHandle = this.nif.openLive(10240, this.getMode(this.nif), 1);
            final PcapHandle handle = this.nif.openLive(10240, this.getMode(this.nif), 1);
            final PacketListener listener = new PacketListener() {
                public void gotPacket(Packet packet) {
                    try {
                        if (packet instanceof EthernetPacket) {
                            CapEnv.this.packetList.add(packet);
                        }
                    } catch (Exception var3) {
                        var3.printStackTrace();
                    }

                }
            };
            Thread thread = new Thread() {
                public void run() {
                    try {
                        handle.loop(-1, listener);
                        PcapStat ps = handle.getStats();
                        handle.close();
                    } catch (Exception var2) {
                        var2.printStackTrace();
                    }

                }
            };
            thread.start();
        }

    }

    void detectInterface() {
        List<PcapNetworkInterface> allDevs = null;
        HashMap handleTable = new HashMap();

        try {
            allDevs = Pcaps.findAllDevs();
        } catch (PcapNativeException var11) {
            var11.printStackTrace();
            return;
        }

        Iterator var4 = allDevs.iterator();

        PcapHandle handle;
        while(var4.hasNext()) {
            final PcapNetworkInterface pi = (PcapNetworkInterface)var4.next();

            try {
                handle = pi.openLive(10240, this.getMode(pi), 1);
                handleTable.put(pi, handle);
                final PacketListener listener = new PacketListener() {
                    public void gotPacket(Packet packet) {
                        try {
                            if (packet instanceof EthernetPacket) {
                                EthernetPacket packet_eth = (EthernetPacket)packet;
                                EthernetHeader head_eth = packet_eth.getHeader();
                                if ((Short)head_eth.getType().value() == -30620) {
                                    CapEnv.this.ppp = true;
                                    PacketUtils.ppp = CapEnv.this.ppp;
                                }

                                IpV4Packet ipV4Packet = null;
                                IpV4Header ipV4Header = null;
                                if (CapEnv.this.ppp) {
                                    ipV4Packet = CapEnv.this.getIpV4Packet_pppoe(packet_eth);
                                } else if (packet_eth.getPayload() instanceof IpV4Packet) {
                                    ipV4Packet = (IpV4Packet)packet_eth.getPayload();
                                }

                                if (ipV4Packet != null) {
                                    ipV4Header = ipV4Packet.getHeader();
                                    if (ipV4Header.getSrcAddr().getHostAddress().equals(CapEnv.this.testIp_tcp)) {
                                        CapEnv.this.local_mac = head_eth.getDstAddr();
                                        CapEnv.this.gateway_mac = head_eth.getSrcAddr();
                                        CapEnv.this.local_ipv4 = ipV4Header.getDstAddr();
                                        CapEnv.this.selectedInterfaceName = pi.getName();
                                        if (pi.getDescription() != null) {
                                            CapEnv.this.selectedInterfaceDes = pi.getDescription();
                                        }
                                    }

                                    if (ipV4Header.getDstAddr().getHostAddress().equals(CapEnv.this.testIp_tcp)) {
                                        CapEnv.this.local_mac = head_eth.getSrcAddr();
                                        CapEnv.this.gateway_mac = head_eth.getDstAddr();
                                        CapEnv.this.local_ipv4 = ipV4Header.getSrcAddr();
                                        CapEnv.this.selectedInterfaceName = pi.getName();
                                        if (pi.getDescription() != null) {
                                            CapEnv.this.selectedInterfaceDes = pi.getDescription();
                                        }
                                    }

                                    if (ipV4Header.getDstAddr().getHostAddress().equals(CapEnv.this.testIp_udp)) {
                                        CapEnv.this.local_mac = head_eth.getSrcAddr();
                                        CapEnv.this.gateway_mac = head_eth.getDstAddr();
                                        CapEnv.this.local_ipv4 = ipV4Header.getSrcAddr();
                                        CapEnv.this.selectedInterfaceName = pi.getName();
                                        if (pi.getDescription() != null) {
                                            CapEnv.this.selectedInterfaceDes = pi.getDescription();
                                        }
                                    }
                                }
                            }
                        } catch (Exception var6) {
                            var6.printStackTrace();
                        }

                    }
                };
                PcapHandle finalHandle = handle;
                Thread thread = new Thread() {
                    public void run() {
                        try {
                            finalHandle.loop(-1, listener);
                            PcapStat ps = finalHandle.getStats();
                            finalHandle.close();
                        } catch (Exception var2) {
                            ;
                        }

                    }
                };
                thread.start();
            } catch (PcapNativeException var10) {
                ;
            }
        }

        try {
            this.detectMac_tcp();
        } catch (UnknownHostException var9) {
            var9.printStackTrace();
        }

        Iterator it = handleTable.keySet().iterator();

        while(it.hasNext()) {
            PcapNetworkInterface pi = (PcapNetworkInterface)it.next();
            handle = (PcapHandle)handleTable.get(pi);

            try {
                handle.breakLoop();
            } catch (NotOpenException var8) {
                var8.printStackTrace();
            }
        }

    }

    IpV4Packet getIpV4Packet_pppoe(EthernetPacket packet_eth) throws IllegalRawDataException {
        IpV4Packet ipV4Packet = null;
        byte[] pppData = packet_eth.getPayload().getRawData();
        if (pppData.length > 8 && pppData[8] == 69) {
            byte[] b2 = new byte[2];
            System.arraycopy(pppData, 4, b2, 0, 2);
            short len = ByteShortConvert.toShort(b2, 0);
            int ipLength = toUnsigned(len) - 2;
            byte[] ipData = new byte[ipLength];
            PacketUtils.pppHead_static[2] = pppData[2];
            PacketUtils.pppHead_static[3] = pppData[3];
            if (ipLength == pppData.length - 8) {
                System.arraycopy(pppData, 8, ipData, 0, ipLength);
                ipV4Packet = IpV4Packet.newPacket(ipData, 0, ipData.length);
            } else {
                MLog.println("长度不符!");
            }
        }

        return ipV4Packet;
    }

    public static String printHexString(byte[] b) {
        StringBuffer sb = new StringBuffer();

        for(int i = 0; i < b.length; ++i) {
            String hex = Integer.toHexString(b[i] & 255);
            hex = hex.replaceAll(":", " ");
            if (hex.length() == 1) {
                hex = '0' + hex;
            }

            sb.append(hex + " ");
        }

        return sb.toString();
    }

    public void createTcpTun_Client(String dstAddress, short dstPort) throws Exception {
        Inet4Address serverAddress = (Inet4Address)Inet4Address.getByName(dstAddress);
        TCPTun conn = new TCPTun(this, serverAddress, dstPort, this.local_mac, this.gateway_mac);
        this.tcpManager.addConnection_Client(conn);
        boolean success = false;

        for(int i = 0; i < 6; ++i) {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException var8) {
                var8.printStackTrace();
            }

            if (conn.preDataReady) {
                success = true;
                break;
            }
        }

        if (success) {
            this.tcpManager.setDefaultTcpTun(conn);
        } else {
            this.tcpManager.removeTun(conn);
            this.tcpManager.setDefaultTcpTun((TCPTun)null);
            throw new Exception("创建隧道失败!");
        }
    }

    private void detectMac_tcp() throws UnknownHostException {
        InetAddress address = InetAddress.getByName("www.bing.com");
        boolean por = true;
        this.testIp_tcp = address.getHostAddress();

        for(int i = 0; i < 5; ++i) {
            try {
                Route.es.execute(new Runnable() {
                    public void run() {
                        try {
                            Socket socket = new Socket(CapEnv.this.testIp_tcp, 80);
                            socket.close();
                        } catch (UnknownHostException var2) {
                            var2.printStackTrace();
                        } catch (IOException var3) {
                            var3.printStackTrace();
                        }

                    }
                });
                Thread.sleep(500L);
                if (this.local_mac != null) {
                    break;
                }
            } catch (Exception var7) {
                var7.printStackTrace();

                try {
                    Thread.sleep(1L);
                } catch (InterruptedException var6) {
                    var6.printStackTrace();
                }
            }
        }

    }

    private void detectMac_udp() {
        for(int i = 0; i < 10; ++i) {
            try {
                DatagramSocket ds = new DatagramSocket();
                DatagramPacket dp = new DatagramPacket(new byte[1000], 1000);
                dp.setAddress(InetAddress.getByName(this.testIp_udp));
                dp.setPort(5555);
                ds.send(dp);
                ds.close();
                Thread.sleep(500L);
                if (this.local_mac != null) {
                    break;
                }
            } catch (Exception var5) {
                var5.printStackTrace();

                try {
                    Thread.sleep(1L);
                } catch (InterruptedException var4) {
                    var4.printStackTrace();
                }
            }
        }

    }

    public short getListenPort() {
        return this.listenPort;
    }

    public void setListenPort(short listenPort) {
        this.listenPort = listenPort;
        if (!this.client) {
            MLog.info("Listen tcp port: " + toUnsigned(listenPort));
        }

    }

    public static int toUnsigned(short s) {
        return s & '\uffff';
    }
}
