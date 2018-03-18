package com.admxj.server;

import com.admxj.rudp.ConnectionProcessor;
import com.admxj.rudp.Route;
import com.admxj.utils.MLog;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class FSServer {
    ConnectionProcessor imTunnelProcessor;
    Route route_udp;
    Route route_tcp;
    Route route;
    int routePort = 150;
    static FSServer udpServer;
    String systemName = System.getProperty("os.name").toLowerCase();

    public static void main(String[] args) throws Exception {
        new FSServer();
    }

    static FSServer get() {
        return udpServer;
    }

    public FSServer() throws Exception {
        MLog.info("System Name: " + this.systemName);
        udpServer = this;
        final MapTunnelProcessor mp = new MapTunnelProcessor();
        String port_s = this.readFileData("./cnf/listen_port");
        if (port_s != null && !port_s.trim().equals("")) {
            port_s = port_s.replaceAll("\n", "").replaceAll("\r", "");
            this.routePort = Integer.parseInt(port_s);
        }

        this.route_udp = new Route(mp.getClass().getName(), (short)this.routePort, Route.mode_server, false);
        if (this.systemName.equals("linux")) {
            this.startFirewall_linux();
            this.setFireWall_linux_udp();
        }

        Route.es.execute(new Runnable() {
            public void run() {
                try {
                    FSServer.this.route_tcp = new Route(mp.getClass().getName(), (short) FSServer.this.routePort, Route.mode_server, true);
                    if (FSServer.this.systemName.equals("linux")) {
                        FSServer.this.setFireWall_linux_tcp();
                    }
                } catch (Exception var2) {
                    ;
                }

            }
        });
    }

    void startFirewall_linux() {
        String cmd1 = "service iptables start";
        this.runCommand(cmd1);
    }

    void setFireWall_linux_udp() {
        this.cleanUdpTunRule();
        String cmd2 = "iptables -A INPUT -p udp --dport " + this.routePort + " -j ACCEPT" + " -m comment --comment udptun_fs_server";
        this.runCommand(cmd2);
    }

    void cleanUdpTunRule() {
        while(true) {
            int row = this.getRow("udptun_fs_server");
            if (row <= 0) {
                return;
            }

            String cmd = "iptables -D INPUT " + row;
            this.runCommand(cmd);
        }
    }

    void setFireWall_linux_tcp() {
        this.cleanTcpTunRule();
        String cmd2 = "iptables -A INPUT -p tcp --dport " + this.routePort + " -j DROP" + " -m comment --comment tcptun_fs_server ";
        this.runCommand(cmd2);
    }

    void cleanTcpTunRule() {
        while(true) {
            int row = this.getRow("tcptun_fs_server");
            if (row <= 0) {
                return;
            }

            String cmd = "iptables -D INPUT " + row;
            this.runCommand(cmd);
        }
    }

    int getRow(String name) {
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
                        } while(!line.contains(name));

                        index = line.indexOf("   ");
                    } while(index <= 0);

                    String n = line.substring(0, index);

                    try {
                        if (row_delect < 0) {
                            row_delect = Integer.parseInt(n);
                        }
                    } catch (Exception var12) {
                        ;
                    }
                } catch (IOException var13) {
                    var13.printStackTrace();
                    break;
                }
            }

            errorReadThread.join();
            p.waitFor();
        } catch (Exception var14) {
            var14.printStackTrace();
        }

        return row_delect;
    }

    void runCommand(String command) {
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
        } catch (Exception var5) {
            var5.printStackTrace();
        }

    }

    String readFileData(String path) {
        String content = null;
        FileInputStream fis = null;
        DataInputStream dis = null;

        try {
            File file = new File(path);
            fis = new FileInputStream(file);
            dis = new DataInputStream(fis);
            byte[] data = new byte[(int)file.length()];
            dis.readFully(data);
            content = new String(data, "utf-8");
        } catch (Exception var15) {
            ;
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException var14) {
                    var14.printStackTrace();
                }
            }

        }

        return content;
    }
}
