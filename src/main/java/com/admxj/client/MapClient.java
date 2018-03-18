package com.admxj.client;


import com.admxj.rudp.*;
import com.admxj.utils.NetStatus;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Random;

public class MapClient implements Trafficlistener {
    ConnectionProcessor imTunnelProcessor;
    Route route_udp;
    Route route_tcp;
    short routePort = 45;
    ClientUII ui;
    String serverAddress = "";
    InetAddress address = null;
    int serverPort = 130;
    NetStatus netStatus;
    long lastTrafficTime;
    int downloadSum = 0;
    int uploadSum = 0;
    Thread clientUISpeedUpdateThread;
    int connNum = 0;
    HashSet<ClientProcessorInterface> processTable = new HashSet();
    Object syn_process = new Object();
    static MapClient mapClient;
    PortMapManager portMapManager;
    public String mapdstAddress;
    public int mapdstPort;
    static int monPort = 25874;
    String systemName = System.getProperty("os.name").toLowerCase();
    boolean useTcp = true;
    long clientId;
    Random ran = new Random();

    MapClient(ClientUI ui) throws Exception {
        this.ui = ui;
        mapClient = this;

        try {
            final ServerSocket socket = new ServerSocket(monPort);
            (new Thread() {
                public void run() {
                    try {
                        socket.accept();
                    } catch (IOException var2) {
                        var2.printStackTrace();
                        System.exit(0);
                    }

                }
            }).start();
        } catch (Exception var5) {
            ;
        }

        try {
            this.route_tcp = new Route((String)null, this.routePort, Route.mode_client, true);
        } catch (Exception var4) {
            throw var4;
        }

        try {
            this.route_udp = new Route((String)null, this.routePort, Route.mode_client, false);
        } catch (Exception var3) {
            throw var3;
        }

        this.netStatus = new NetStatus();
        this.portMapManager = new PortMapManager(this);
        this.clientUISpeedUpdateThread = new Thread() {
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(500L);
                    } catch (InterruptedException var2) {
                        var2.printStackTrace();
                    }

                    MapClient.this.updateUISpeed();
                }
            }
        };
        this.clientUISpeedUpdateThread.start();
        Route.addTrafficlistener(this);
    }

    public static MapClient get() {
        return mapClient;
    }

    private void updateUISpeed() {
        if (this.ui != null) {
            this.ui.updateUISpeed(this.connNum, this.netStatus.getDownSpeed(), this.netStatus.getUpSpeed());
        }

    }

    public void setMapServer(String serverAddress, int serverPort, int remotePort, String passwordMd5, String password_proxy_Md5, boolean direct_cn, boolean tcp, String password) {
        if (this.serverAddress == null || !this.serverAddress.equals(serverAddress) || this.serverPort != serverPort) {
            if (this.route_tcp.lastClientControl != null) {
                this.route_tcp.lastClientControl.close();
            }

            if (this.route_udp.lastClientControl != null) {
                this.route_udp.lastClientControl.close();
            }

            this.cleanRule();
            if (serverAddress != null && !serverAddress.equals("")) {
                this.setFireWallRule(serverAddress, serverPort);
            }
        }

        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.address = null;
        this.useTcp = tcp;
        this.resetConnection();
    }

    void setFireWallRule(String serverAddress, int serverPort) {
        try {
            String ip = InetAddress.getByName(serverAddress).getHostAddress();
            String cmd_add1;
            if (this.systemName.contains("mac os")) {
                if (this.ui.isOsx_fw_pf()) {
                    cmd_add1 = "./pf.conf";
                    File f = new File(cmd_add1);
                    File d = f.getParentFile();
                    if (!d.exists()) {
                        d.mkdirs();
                    }

                    if (f.exists()) {
                        f.delete();
                    }

                    String content = "block drop quick proto tcp from any to " + ip + " port = " + serverPort + "\n";
                    this.saveFile(content.getBytes(), cmd_add1);
                    String cmd1 = "pfctl -d";
                    runCommand(cmd1);
                    String cmd2 = "pfctl -Rf " + f.getAbsolutePath();
                    runCommand(cmd2);
                    String cmd3 = "pfctl -e";
                    runCommand(cmd3);
                } else if (this.ui.isOsx_fw_ipfw()) {
                    cmd_add1 = "sudo ipfw add 5050 deny tcp from any to " + ip + " " + serverAddress + " out";
                    runCommand(cmd_add1);
                }
            } else if (this.systemName.contains("linux")) {
                cmd_add1 = "iptables -t filter -A OUTPUT -d " + ip + " -p tcp --dport " + serverPort + " -j DROP -m comment --comment tcptun_fs ";
                runCommand(cmd_add1);
            } else if (this.systemName.contains("windows")) {
                try {
                    Process p2;
                    if (!this.systemName.contains("xp") && !this.systemName.contains("2003")) {
                        cmd_add1 = "netsh advfirewall firewall add rule name=tcptun_fs protocol=TCP dir=out remoteport=" + serverPort + " remoteip=" + ip + " action=block ";
                        p2 = Runtime.getRuntime().exec(cmd_add1, (String[])null);
                        p2.waitFor();
                        String cmd_add2 = "netsh advfirewall firewall add rule name=tcptun_fs protocol=TCP dir=in remoteport=" + serverPort + " remoteip=" + ip + " action=block ";
                        Process p3 = Runtime.getRuntime().exec(cmd_add2, (String[])null);
                        p3.waitFor();
                    } else {
                        cmd_add1 = "ipseccmd -w REG -p \"tcptun_fs\" -r \"Block TCP/" + serverPort + "\" -f 0/255.255.255.255=" + ip + "/255.255.255.255:" + serverPort + ":tcp -n BLOCK -x ";
                        p2 = Runtime.getRuntime().exec(cmd_add1, (String[])null);
                        p2.waitFor();
                    }
                } catch (Exception var11) {
                    var11.printStackTrace();
                }
            }
        } catch (Exception var12) {
            var12.printStackTrace();
        }

    }

    void saveFile(byte[] data, String path) throws Exception {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(path);
            fos.write(data);
        } catch (Exception var8) {
            throw var8;
        } finally {
            if (fos != null) {
                fos.close();
            }

        }

    }

    void cleanRule() {
        if (this.systemName.contains("mac os")) {
            this.cleanTcpTunRule_osx();
        } else if (this.systemName.contains("linux")) {
            this.cleanTcpTunRule_linux();
        } else {
            try {
                String cmd_delete;
                Process p1;
                if (!this.systemName.contains("xp") && !this.systemName.contains("2003")) {
                    cmd_delete = "netsh advfirewall firewall delete rule name=tcptun_fs ";
                    p1 = Runtime.getRuntime().exec(cmd_delete, (String[])null);
                    p1.waitFor();
                } else {
                    cmd_delete = "ipseccmd -p \"tcptun_fs\" -w reg -y";
                    p1 = Runtime.getRuntime().exec(cmd_delete, (String[])null);
                    p1.waitFor();
                }
            } catch (Exception var3) {
                var3.printStackTrace();
            }
        }

    }

    void cleanTcpTunRule_osx() {
        String cmd2 = "sudo ipfw delete 5050";
        runCommand(cmd2);
    }

    void cleanTcpTunRule_linux() {
        while(true) {
            int row = this.getRow_linux();
            if (row <= 0) {
                return;
            }

            String cmd = "iptables -D OUTPUT " + row;
            runCommand(cmd);
        }
    }

    int getRow_linux() {
        int row_delect = -1;
        String cme_list_rule = "iptables -L -n --line-number";
        Thread errorReadThread = null;

        try {
            final Process p = Runtime.getRuntime().exec(cme_list_rule, (String[])null);
            errorReadThread = new Thread() {
                public void run() {
                    InputStream is = p.getErrorStream();
                    BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));

                    String line;
                    try {
                        do {
                            line = localBufferedReader.readLine();
                        } while(line != null);
                    } catch (IOException var5) {
                        var5.printStackTrace();
                    }

                }
            };
            errorReadThread.start();
            InputStream is = p.getInputStream();
            BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));

            label44:
            while(true) {
                try {
                    String line;
                    int index;
                    do {
                        do {
                            line = localBufferedReader.readLine();
                            if (line == null) {
                                break label44;
                            }
                        } while(!line.contains("tcptun_fs"));

                        index = line.indexOf("   ");
                    } while(index <= 0);

                    String n = line.substring(0, index);

                    try {
                        if (row_delect < 0) {
                            row_delect = Integer.parseInt(n);
                        }
                    } catch (Exception var11) {
                        ;
                    }
                } catch (IOException var12) {
                    var12.printStackTrace();
                    break;
                }
            }

            errorReadThread.join();
            p.waitFor();
        } catch (Exception var13) {
            var13.printStackTrace();
        }

        return row_delect;
    }

    void resetConnection() {
        synchronized(this.syn_process){}
    }

    public void onProcessClose(ClientProcessorInterface process) {
        Object var2 = this.syn_process;
        synchronized(this.syn_process) {
            this.processTable.remove(process);
        }
    }

    public synchronized void closeAndTryConnect_Login(boolean testSpeed) {
        this.close();
        boolean loginOK = this.ui.login();
        if (loginOK) {
            this.ui.updateNode(testSpeed);
        }

    }

    public synchronized void closeAndTryConnect() {
        this.close();
    }

    public void close() {
    }

    public void trafficDownload(TrafficEvent event) {
        this.netStatus.addDownload(event.getTraffic());
        this.lastTrafficTime = System.currentTimeMillis();
        this.downloadSum += event.getTraffic();
    }

    public void trafficUpload(TrafficEvent event) {
        this.netStatus.addUpload(event.getTraffic());
        this.lastTrafficTime = System.currentTimeMillis();
        this.uploadSum += event.getTraffic();
    }

    static void runCommand(String command) {
        Thread standReadThread = null;
        Thread errorReadThread = null;

        try {
            final Process p = Runtime.getRuntime().exec(command, (String[])null);
            standReadThread = new Thread() {
                public void run() {
                    InputStream is = p.getInputStream();
                    BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));

                    String line;
                    try {
                        do {
                            line = localBufferedReader.readLine();
                        } while(line != null);
                    } catch (IOException var5) {
                        var5.printStackTrace();
                    }

                }
            };
            standReadThread.start();
            errorReadThread = new Thread() {
                public void run() {
                    InputStream is = p.getErrorStream();
                    BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));

                    String line;
                    try {
                        do {
                            line = localBufferedReader.readLine();
                        } while(line != null);
                    } catch (IOException var5) {
                        var5.printStackTrace();
                    }

                }
            };
            errorReadThread.start();
            standReadThread.join();
            errorReadThread.join();
            p.waitFor();
        } catch (Exception var4) {
            var4.printStackTrace();
        }

    }

    public boolean isUseTcp() {
        return this.useTcp;
    }

    public void setUseTcp(boolean useTcp) {
        this.useTcp = useTcp;
    }

    public ClientUII getUi() {
        return this.ui;
    }

    public void setUi(ClientUII ui) {
        this.ui = ui;
    }
}