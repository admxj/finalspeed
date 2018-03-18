package com.admxj.rudp;

import com.admxj.cap.CapEnv;
import com.admxj.cap.VDatagramSocket;
import com.admxj.rudp.message.MessageType;
import com.admxj.utils.ByteIntConvert;
import com.admxj.utils.MLog;
import com.admxj.utils.MessageCheck;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class Route {
    private DatagramSocket ds;
    public HashMap<Integer, ConnectionUDP> connTable;
    Route route;
    Thread mainThread;
    Thread reveiveThread;
    public static ThreadPoolExecutor es;
    public AckListManage delayAckManage;
    Object syn_ds2Table = new Object();
    Object syn_tunTable = new Object();
    Random ran = new Random();
    public int localclientId;
    LinkedBlockingQueue<DatagramPacket> packetBuffer;
    public static int mode_server = 2;
    public static int mode_client = 1;
    public int mode;
    String pocessName;
    HashSet<Integer> setedTable;
    static int vv;
    HashSet<Integer> closedTable;
    public static int localDownloadSpeed;
    public static int localUploadSpeed;
    ClientManager clientManager;
    HashSet<Integer> pingTable;
    public CapEnv capEnv;
    public ClientControl lastClientControl;
    public boolean useTcpTun;
    public HashMap<Object, Object> contentTable;
    private static List<Trafficlistener> listenerList = new Vector();

    static {
        SynchronousQueue queue = new SynchronousQueue();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(100, 2147483647, 10000L, TimeUnit.MILLISECONDS, queue);
        es = executor;
    }

    public Route(String pocessName, short routePort, int mode2, boolean tcp) throws Exception {
        this.localclientId = Math.abs(this.ran.nextInt());
        this.packetBuffer = new LinkedBlockingQueue();
        this.mode = mode_client;
        this.pocessName = "";
        this.setedTable = new HashSet();
        this.closedTable = new HashSet();
        this.pingTable = new HashSet();
        this.capEnv = null;
        this.useTcpTun = true;
        this.contentTable = new HashMap();
        this.delayAckManage = new AckListManage();
        this.mode = mode2;
        this.useTcpTun = tcp;
        this.pocessName = pocessName;
        if (this.useTcpTun) {
            VDatagramSocket d;
            if (this.mode == 2) {
                d = new VDatagramSocket(routePort);
                d.setClient(false);

                try {
                    this.capEnv = new CapEnv(false);
                    this.capEnv.setListenPort(routePort);
                    this.capEnv.init();
                } catch (Exception var8) {
                    throw var8;
                }

                d.setCapEnv(this.capEnv);
                this.ds = d;
            } else {
                d = new VDatagramSocket();
                d.setClient(true);

                try {
                    this.capEnv = new CapEnv(true);
                    this.capEnv.init();
                } catch (Exception var7) {
                    throw var7;
                }

                d.setCapEnv(this.capEnv);
                this.ds = d;
            }
        } else if (this.mode == 2) {
            MLog.info("Listen udp port: " + CapEnv.toUnsigned(routePort));
            this.ds = new DatagramSocket(CapEnv.toUnsigned(routePort));
        } else {
            this.ds = new DatagramSocket();
        }

        this.connTable = new HashMap();
        this.clientManager = new ClientManager(this);
        this.reveiveThread = new Thread() {
            public void run() {
                while(true) {
                    byte[] b = new byte[1500];
                    DatagramPacket dp = new DatagramPacket(b, b.length);

                    try {
                        Route.this.ds.receive(dp);
                        Route.this.packetBuffer.add(dp);
                    } catch (IOException var6) {
                        var6.printStackTrace();

                        try {
                            Thread.sleep(1L);
                        } catch (InterruptedException var5) {
                            var5.printStackTrace();
                        }
                    }
                }
            }
        };
        this.reveiveThread.start();
        this.mainThread = new Thread() {
            public void run() {
                while(true) {
                    DatagramPacket dp = null;

                    try {
                        dp = (DatagramPacket) Route.this.packetBuffer.take();
                    } catch (InterruptedException var12) {
                        var12.printStackTrace();
                    }

                    if (dp != null) {
                        long t1 = System.currentTimeMillis();
                        byte[] dpData = dp.getData();
                        boolean sTypex = false;
                        if (dp.getData().length < 4) {
                            return;
                        }

                        int sType = MessageCheck.checkSType(dp);
                        if (dp != null) {
                            int connectId = ByteIntConvert.toInt(dpData, 4);
                            int remote_clientId = ByteIntConvert.toInt(dpData, 8);
                            if (!Route.this.closedTable.contains(connectId) || connectId == 0) {
                                if (sType != MessageType.sType_PingMessage && sType != MessageType.sType_PingMessage2) {
                                    if (Route.this.mode == 1 && !Route.this.setedTable.contains(remote_clientId)) {
                                        String key = dp.getAddress().getHostAddress() + ":" + dp.getPort();
                                        int sim_clientId = Math.abs(key.hashCode());
                                        ClientControl clientControl = Route.this.clientManager.getClientControl(sim_clientId, dp.getAddress(), dp.getPort());
                                        if (clientControl.getClientId_real() == -1) {
                                            clientControl.setClientId_real(remote_clientId);
                                        } else if (clientControl.getClientId_real() != remote_clientId) {
                                            clientControl.updateClientId(remote_clientId);
                                        }

                                        Route.this.setedTable.add(remote_clientId);
                                    }

                                    if (Route.this.mode == 2) {
                                        try {
                                            Route.this.getConnection2(dp.getAddress(), dp.getPort(), connectId, remote_clientId);
                                        } catch (Exception var11) {
                                            var11.printStackTrace();
                                        }
                                    }

                                    ConnectionUDP ds3 = (ConnectionUDP) Route.this.connTable.get(connectId);
                                    if (ds3 != null) {
                                        ds3.receiver.onReceivePacket(dp);
                                        if (sType == MessageType.sType_DataMessage) {
                                            TrafficEvent event = new TrafficEvent("", Route.this.ran.nextLong(), dp.getLength(), TrafficEvent.type_downloadTraffic);
                                            Route.fireEvent(event);
                                        }
                                    }
                                } else {
                                    ClientControl clientControlx = null;
                                    if (Route.this.mode == 2) {
                                        clientControlx = Route.this.clientManager.getClientControl(remote_clientId, dp.getAddress(), dp.getPort());
                                    } else if (Route.this.mode == 1) {
                                        String keyx = dp.getAddress().getHostAddress() + ":" + dp.getPort();
                                        int sim_clientIdx = Math.abs(keyx.hashCode());
                                        clientControlx = Route.this.clientManager.getClientControl(sim_clientIdx, dp.getAddress(), dp.getPort());
                                    }

                                    clientControlx.onReceivePacket(dp);
                                }
                            }
                        }
                    }
                }
            }
        };
        this.mainThread.start();
    }

    public static void addTrafficlistener(Trafficlistener listener) {
        listenerList.add(listener);
    }

    static void fireEvent(TrafficEvent event) {
        Iterator var2 = listenerList.iterator();

        while(var2.hasNext()) {
            Trafficlistener listener = (Trafficlistener)var2.next();
            int type = event.getType();
            if (type == TrafficEvent.type_downloadTraffic) {
                listener.trafficDownload(event);
            } else if (type == TrafficEvent.type_uploadTraffic) {
                listener.trafficUpload(event);
            }
        }

    }

    public void sendPacket(DatagramPacket dp) throws IOException {
        this.ds.send(dp);
    }

    public ConnectionProcessor createTunnelProcessor() {
        ConnectionProcessor o = null;

        try {
            Class onwClass = Class.forName(this.pocessName);
            o = (ConnectionProcessor)onwClass.newInstance();
        } catch (ClassNotFoundException var3) {
            var3.printStackTrace();
        } catch (InstantiationException var4) {
            var4.printStackTrace();
        } catch (IllegalAccessException var5) {
            var5.printStackTrace();
        }

        return o;
    }

    void removeConnection(ConnectionUDP conn) {
        Object var2 = this.syn_ds2Table;
        synchronized(this.syn_ds2Table) {
            this.closedTable.add(conn.connectId);
            this.connTable.remove(conn.connectId);
        }
    }

    public ConnectionUDP getConnection2(InetAddress dstIp, int dstPort, int connectId, int clientId) throws Exception {
        ConnectionUDP conn = (ConnectionUDP)this.connTable.get(connectId);
        if (conn == null) {
            ClientControl clientControl = this.clientManager.getClientControl(clientId, dstIp, dstPort);
            conn = new ConnectionUDP(this, dstIp, dstPort, 2, connectId, clientControl);
            Object var7 = this.syn_ds2Table;
            synchronized(this.syn_ds2Table) {
                this.connTable.put(connectId, conn);
            }

            clientControl.addConnection(conn);
        }

        return conn;
    }

    public ConnectionUDP getConnection(String address, int dstPort, String password) throws Exception {
        InetAddress dstIp = InetAddress.getByName(address);
        int connectId = Math.abs(this.ran.nextInt());
        String key = dstIp.getHostAddress() + ":" + dstPort;
        int remote_clientId = Math.abs(key.hashCode());
        ClientControl clientControl = this.clientManager.getClientControl(remote_clientId, dstIp, dstPort);
        clientControl.setPassword(password);
        ConnectionUDP conn = new ConnectionUDP(this, dstIp, dstPort, 1, connectId, clientControl);
        Object var10 = this.syn_ds2Table;
        synchronized(this.syn_ds2Table) {
            this.connTable.put(connectId, conn);
        }

        clientControl.addConnection(conn);
        this.lastClientControl = clientControl;
        return conn;
    }

    public boolean isUseTcpTun() {
        return this.useTcpTun;
    }

    public void setUseTcpTun(boolean useTcpTun) {
        this.useTcpTun = useTcpTun;
    }
}
