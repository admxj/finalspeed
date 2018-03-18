package com.admxj.client;

import com.admxj.rudp.Route;
import com.admxj.utils.MLog;
import com.admxj.utils.Tools;
import com.alibaba.fastjson.JSONObject;
import net.miginfocom.swing.MigLayout;
import org.pcap4j.core.Pcaps;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public class ClientUI implements ClientUII, WindowListener {
    JFrame mainFrame;
    JComponent mainPanel;
    JTextField text_serverAddress;
    MapClient mapClient;
    JLabel uploadSpeedField;
    JLabel downloadSpeedField;
    JLabel stateText;
    ClientConfig config = null;
    String configFilePath = "client_config.json";
    String logoImg = "img/offline.png";
    String offlineImg = "img/offline.png";
    String name = "FinalSpeed";
    private TrayIcon trayIcon;
    private SystemTray tray;
    int serverVersion = -1;
    int localVersion = 1;
    boolean checkingUpdate = false;
    String domain = "";
    String homeUrl;
    public static ClientUI ui;
    JTextField text_ds;
    JTextField text_us;
    boolean ky = true;
    String errorMsg = "保存失败请检查输入信息!";
    JButton button_site;
    MapRuleListModel model;
    public MapRuleListTable tcpMapRuleListTable;
    boolean capSuccess = false;
    Exception capException = null;
    boolean b1 = false;
    boolean success_firewall_windows = true;
    boolean success_firewall_osx = true;
    String systemName = null;
    public boolean osx_fw_pf = false;
    public boolean osx_fw_ipfw = false;
    JRadioButton r_tcp;
    JRadioButton r_udp;
    String updateUrl;

    public ClientUI() {
        this.domain = "d1sm.net";
        this.homeUrl = "http://www.d1sm.net/?client_sf";
        this.updateUrl = "http://fs.d1sm.net/finalspeed/update.properties";
        this.systemName = System.getProperty("os.name").toLowerCase();
        MLog.info("System: " + this.systemName + " " + System.getProperty("os.version"));
        ui = this;
        this.mainFrame = new JFrame();
        this.mainFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(this.logoImg));
        this.initUI();
        this.checkQuanxian();
        this.loadConfig();
        this.mainFrame.setTitle("FinalSpeed 1.0");
        this.mainFrame.addWindowListener(this);
        this.mainPanel = (JPanel)this.mainFrame.getContentPane();
        this.mainPanel.setLayout(new MigLayout("align center , insets 10 10 10 10"));
        this.mainPanel.setBorder((Border)null);
        this.mainFrame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent evt) {
                ClientUI.this.text_ds.requestFocus();
            }
        });
        JPanel centerPanel = new JPanel();
        this.mainPanel.add(centerPanel, "wrap");
        centerPanel.setLayout(new MigLayout("insets 0 0 0 0"));
        JPanel loginPanel = new JPanel();
        centerPanel.add(loginPanel, "");
        loginPanel.setLayout(new MigLayout("insets 0 0 0 0"));
        JLabel label_msg = new JLabel();
        label_msg.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new MigLayout("insets 10 0 10 0"));
        centerPanel.add(rightPanel, "width :: ,top");
        JPanel mapPanel = new JPanel();
        mapPanel.setLayout(new MigLayout("insets 0 0 0 0"));
        mapPanel.setBorder(BorderFactory.createTitledBorder("加速列表"));
        rightPanel.add(mapPanel);
        this.model = new MapRuleListModel();
        this.tcpMapRuleListTable = new MapRuleListTable(this, this.model);
        JScrollPane tablePanel = new JScrollPane();
        tablePanel.setViewportView(this.tcpMapRuleListTable);
        mapPanel.add(tablePanel, "height 50:160:1024 ,growy,width :250:,wrap");
        tablePanel.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                ClientUI.this.tcpMapRuleListTable.clearSelection();
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }
        });
        JPanel p9 = new JPanel();
        p9.setLayout(new MigLayout("insets 1 0 3 0 "));
        mapPanel.add(p9, "align center,wrap");
        JButton button_add = this.createButton("添加");
        p9.add(button_add);
        button_add.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new AddMapFrame(ClientUI.ui, ClientUI.this.mainFrame, (MapRule)null, false);
            }
        });
        JButton button_edit = this.createButton("修改");
        p9.add(button_edit);
        button_edit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int index = ClientUI.this.tcpMapRuleListTable.getSelectedRow();
                if (index > -1) {
                    MapRule mapRule = ClientUI.this.model.getMapRuleAt(index);
                    new AddMapFrame(ClientUI.ui, ClientUI.this.mainFrame, mapRule, true);
                }

            }
        });
        JButton button_remove = this.createButton("删除");
        p9.add(button_remove);
        button_remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int index = ClientUI.this.tcpMapRuleListTable.getSelectedRow();
                if (index > -1) {
                    MapRule mapRule = ClientUI.this.model.getMapRuleAt(index);
                    ClientUI.this.mapClient.portMapManager.removeMapRule(mapRule.getName());
                    ClientUI.this.loadMapRule();
                }

            }
        });
        JPanel pa = new JPanel();
        pa.setBorder(BorderFactory.createTitledBorder("服务器"));
        pa.setLayout(new MigLayout("insets 0 0 0 0"));
        loginPanel.add(pa, "growx,wrap");
        JPanel p1 = new JPanel();
        p1.setLayout(new MigLayout("insets 0 0 0 0"));
        pa.add(p1, "wrap");
        p1.add(new JLabel("地址:"), "width 50::");
        this.text_serverAddress = new JTextField();
        this.text_serverAddress.setToolTipText("主机:端口号");
        p1.add(this.text_serverAddress, "width 130::");
        TextComponentPopupMenu.installToComponent(this.text_serverAddress);
        JPanel panelr = new JPanel();
        pa.add(panelr, "wrap");
        panelr.setLayout(new MigLayout("insets 0 0 0 0"));
        panelr.add(new JLabel("传输协议:"));
        this.r_tcp = new JRadioButton("TCP");
        this.r_tcp.setFocusPainted(false);
        panelr.add(this.r_tcp);
        this.r_udp = new JRadioButton("UDP");
        this.r_udp.setFocusPainted(false);
        panelr.add(this.r_udp);
        ButtonGroup bg = new ButtonGroup();
        bg.add(this.r_tcp);
        bg.add(this.r_udp);
        if (this.config.getProtocal().equals("udp")) {
            this.r_udp.setSelected(true);
        } else {
            this.r_tcp.setSelected(true);
        }

        JPanel sp = new JPanel();
        sp.setBorder(BorderFactory.createTitledBorder("物理带宽"));
        sp.setLayout(new MigLayout("insets 5 5 5 5"));
        JPanel pa1 = new JPanel();
        sp.add(pa1, "wrap");
        pa1.setLayout(new MigLayout("insets 0 0 0 0"));
        loginPanel.add(sp, "wrap");
        pa1.add(new JLabel("下载:"), "width ::");
        this.text_ds = new JTextField("0");
        pa1.add(this.text_ds, "width 80::");
        this.text_ds.setHorizontalAlignment(4);
        this.text_ds.setEditable(false);
        JButton button_set_speed = this.createButton("设置带宽");
        pa1.add(button_set_speed);
        button_set_speed.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new SpeedSetFrame(ClientUI.ui, ClientUI.this.mainFrame);
            }
        });
        JPanel pa2 = new JPanel();
        sp.add(pa2, "wrap");
        pa2.setLayout(new MigLayout("insets 0 0 0 0"));
        loginPanel.add(sp, "wrap");
        pa2.add(new JLabel("上传:"), "width ::");
        this.text_us = new JTextField("0");
        pa2.add(this.text_us, "width 80::");
        this.text_us.setHorizontalAlignment(4);
        this.text_us.setEditable(false);
        JPanel p4 = new JPanel();
        p4.setLayout(new MigLayout("insets 5 0 0 0 "));
        loginPanel.add(p4, "align center,wrap");
        JButton button_save = this.createButton("确定");
        p4.add(button_save);
        this.button_site = this.createButton("网站");
        p4.add(this.button_site);
        this.button_site.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ClientUI.this.openUrl(ClientUI.this.homeUrl);
            }
        });
        JButton button_exit = this.createButton("退出");
        p4.add(button_exit);
        button_exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        button_save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ClientUI.this.config.getDownloadSpeed() == 0 || ClientUI.this.config.getUploadSpeed() == 0) {
                    new SpeedSetFrame(ClientUI.ui, ClientUI.this.mainFrame);
                }

                ClientUI.this.setMessage("");
                ClientUI.this.saveConfig();
            }
        });
        this.stateText = new JLabel("");
        this.mainPanel.add(this.stateText, "align right ,wrap");
        this.downloadSpeedField = new JLabel();
        this.downloadSpeedField.setHorizontalAlignment(4);
        this.mainPanel.add(this.downloadSpeedField, "align right ");
        this.updateUISpeed(0, 0, 0);
        this.setMessage(" ");
        String server_addressTxt = this.config.getServerAddress();
        if (this.config.getServerAddress() != null && !this.config.getServerAddress().equals("") && this.config.getServerPort() != 150 && this.config.getServerPort() != 0) {
            server_addressTxt = server_addressTxt + ":" + this.config.getServerPort();
        }

        this.text_serverAddress.setText(server_addressTxt);
        if (this.config.getRemoteAddress() != null && !this.config.getRemoteAddress().equals("") && this.config.getRemotePort() > 0) {
            (new StringBuilder(String.valueOf(this.config.getRemoteAddress()))).append(":").append(this.config.getRemotePort()).toString();
        }

        boolean width = true;
        if (this.systemName.contains("os x")) {
            width = true;
        }

        this.mainFrame.pack();
        this.mainFrame.setLocationRelativeTo((Component)null);
        PopupMenu trayMenu = new PopupMenu();
        this.tray = SystemTray.getSystemTray();
        this.trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage(this.offlineImg), this.name, trayMenu);
        this.trayIcon.setImageAutoSize(true);
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ClientUI.this.mainFrame.toFront();
                ClientUI.this.mainFrame.setVisible(true);
            }
        };
        this.trayIcon.addActionListener(listener);
        this.trayIcon.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent arg0) {
            }

            public void mouseEntered(MouseEvent arg0) {
            }

            public void mouseExited(MouseEvent arg0) {
            }

            public void mousePressed(MouseEvent arg0) {
            }

            public void mouseReleased(MouseEvent arg0) {
            }
        });

        try {
            this.tray.add(this.trayIcon);
        } catch (AWTException var36) {
            var36.printStackTrace();
        }

        try {
            MenuItem item3 = new MenuItem("Exit");
            ActionListener al = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ClientUI.this.exit();
                }
            };
            item3.addActionListener(al);
            trayMenu.add(item3);
        } catch (Exception var35) {
            var35.printStackTrace();
        }

        this.checkFireWallOn();
        if (!this.success_firewall_windows) {
            this.mainFrame.setVisible(true);
            JOptionPane.showMessageDialog(this.mainFrame, "启动windows防火墙失败,请先运行防火墙服务.");
            System.exit(0);
        }

        if (!this.success_firewall_osx) {
            this.mainFrame.setVisible(true);
            JOptionPane.showMessageDialog(this.mainFrame, "启动ipfw/pf防火墙失败,请先安装.");
            System.exit(0);
        }

        Thread thread = new Thread() {
            public void run() {
                try {
                    Pcaps.findAllDevs();
                    ClientUI.this.b1 = true;
                } catch (Exception var2) {
                    var2.printStackTrace();
                }

            }
        };
        thread.start();

        try {
            thread.join();
        } catch (InterruptedException var34) {
            var34.printStackTrace();
        }

        if (!this.b1) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        ClientUI.this.mainFrame.setVisible(true);
                        String msg = "启动失败,请先安装libpcap";
                        if (ClientUI.this.systemName.contains("windows")) {
                            msg = "启动失败,请先安装winpcap";
                        }

                        JOptionPane.showMessageDialog(ClientUI.this.mainFrame, msg);
                        if (ClientUI.this.systemName.contains("windows")) {
                            try {
                                Process var2 = Runtime.getRuntime().exec("winpcap_install.exe", (String[])null);
                            } catch (IOException var3) {
                                var3.printStackTrace();
                            }
                        }

                        System.exit(0);
                    }
                });
            } catch (InvocationTargetException var32) {
                var32.printStackTrace();
            } catch (InterruptedException var33) {
                var33.printStackTrace();
            }
        }

        try {
            this.mapClient = new MapClient(this);
        } catch (Exception var31) {
            var31.printStackTrace();
            this.capException = var31;
        }

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    if (!ClientUI.this.mapClient.route_tcp.capEnv.tcpEnable) {
                        ClientUI.this.mainFrame.setVisible(true);
                        ClientUI.this.r_tcp.setEnabled(false);
                        ClientUI.this.r_udp.setSelected(true);
                    }

                }
            });
        } catch (InvocationTargetException var29) {
            var29.printStackTrace();
        } catch (InterruptedException var30) {
            var30.printStackTrace();
        }

        this.mapClient.setUi(this);
        this.mapClient.setMapServer(this.config.getServerAddress(), this.config.getServerPort(), this.config.getRemotePort(), (String)null, (String)null, this.config.isDirect_cn(), this.config.getProtocal().equals("tcp"), (String)null);
        Route.es.execute(new Runnable() {
            public void run() {
                ClientUI.this.checkUpdate();
            }
        });
        this.setSpeed(this.config.getDownloadSpeed(), this.config.getUploadSpeed());
        this.mainFrame.setVisible(true);
        this.loadMapRule();
        if (this.config.getDownloadSpeed() == 0 || this.config.getUploadSpeed() == 0) {
            new SpeedSetFrame(ui, this.mainFrame);
        }

    }

    void checkFireWallOn() {
        String runFirewall;
        if (this.systemName.contains("os x")) {
            runFirewall = "ipfw";

            Process p;
            try {
                p = Runtime.getRuntime().exec(runFirewall, (String[])null);
                this.osx_fw_ipfw = true;
            } catch (IOException e) {
                //e.printStackTrace();
            }

            runFirewall = "pfctl";

            try {
                p = Runtime.getRuntime().exec(runFirewall, (String[])null);
                this.osx_fw_pf = true;
            } catch (IOException var8) {
                var8.printStackTrace();
            }

            this.success_firewall_osx = this.osx_fw_ipfw | this.osx_fw_pf;
        } else if (this.systemName.contains("linux")) {
            runFirewall = "service iptables start";
        } else if (this.systemName.contains("windows")) {
            runFirewall = "netsh advfirewall set allprofiles state on";
            Thread standReadThread = null;
            Thread errorReadThread = null;

            try {
                final Process p = Runtime.getRuntime().exec(runFirewall, (String[])null);
                standReadThread = new Thread() {
                    public void run() {
                        InputStream is = p.getInputStream();
                        BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));

                        while(true) {
                            try {
                                String line = localBufferedReader.readLine();
                                if (line == null) {
                                    break;
                                }

                                if (line.contains("Windows")) {
                                    ClientUI.this.success_firewall_windows = false;
                                }
                            } catch (IOException var5) {
                                var5.printStackTrace();
                                ClientUI.this.exit();
                                break;
                            }
                        }

                    }
                };
                standReadThread.start();
                errorReadThread = new Thread() {
                    public void run() {
                        InputStream is = p.getErrorStream();
                        BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));

                        while(true) {
                            try {
                                String line = localBufferedReader.readLine();
                                if (line == null) {
                                    break;
                                }

                                System.out.println("error" + line);
                            } catch (IOException var5) {
                                var5.printStackTrace();
                                ClientUI.this.exit();
                                break;
                            }
                        }

                    }
                };
                errorReadThread.start();
            } catch (IOException var7) {
                var7.printStackTrace();
                this.success_firewall_windows = false;
            }

            if (standReadThread != null) {
                try {
                    standReadThread.join();
                } catch (InterruptedException var6) {
                    var6.printStackTrace();
                }
            }

            if (errorReadThread != null) {
                try {
                    errorReadThread.join();
                } catch (InterruptedException var5) {
                    var5.printStackTrace();
                }
            }
        }

    }

    void checkQuanxian() {
        if (this.systemName.contains("windows")) {
            boolean b = false;
            File file = new File(System.getenv("WINDIR") + "\\test.file");

            try {
                file.createNewFile();
            } catch (IOException var4) {
                var4.printStackTrace();
            }

            b = file.exists();
            file.delete();
            if (!b) {
                JOptionPane.showMessageDialog((Component)null, "请以管理员身份运行! ");
                System.exit(0);
            }
        }

    }

    void loadMapRule() {
        this.tcpMapRuleListTable.setMapRuleList(this.mapClient.portMapManager.getMapList());
    }

    void select(String name) {
        int index = this.model.getMapRuleIndex(name);
        if (index > -1) {
            this.tcpMapRuleListTable.getSelectionModel().setSelectionInterval(index, index);
        }

    }

    void setSpeed(int downloadSpeed, int uploadSpeed) {
        this.config.setDownloadSpeed(downloadSpeed);
        this.config.setUploadSpeed(uploadSpeed);
        int s1 = (int)((float)downloadSpeed * 1.1F);
        this.text_ds.setText(" " + Tools.getSizeStringKB((long)s1) + "/s ");
        int s2 = (int)((float)uploadSpeed * 1.1F);
        this.text_us.setText(" " + Tools.getSizeStringKB((long)s2) + "/s ");
        Route.localDownloadSpeed = downloadSpeed;
        Route.localUploadSpeed = this.config.uploadSpeed;
        this.saveConfig();
    }

    void exit() {
        this.mainFrame.setVisible(false);
        System.exit(0);
    }

    void openUrl(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (IOException var3) {
            var3.printStackTrace();
        } catch (URISyntaxException var4) {
            var4.printStackTrace();
        }

    }

    public void setMessage(String message) {
        this.stateText.setText("状态: " + message);
    }

    ClientConfig loadConfig() {
        ClientConfig cfg = new ClientConfig();
        if (!(new File(this.configFilePath)).exists()) {
            JSONObject json = new JSONObject();

            try {
                this.saveFile(json.toJSONString().getBytes(), this.configFilePath);
            } catch (Exception var5) {
                var5.printStackTrace();
            }
        }

        try {
            String content = readFileUtf8(this.configFilePath);
            JSONObject json = JSONObject.parseObject(content);
            cfg.setServerAddress(json.getString("server_address"));
            cfg.setServerPort(json.getIntValue("server_port"));
            cfg.setRemotePort(json.getIntValue("remote_port"));
            cfg.setRemoteAddress(json.getString("remote_address"));
            if (json.containsKey("direct_cn")) {
                cfg.setDirect_cn(json.getBooleanValue("direct_cn"));
            }

            cfg.setDownloadSpeed(json.getIntValue("download_speed"));
            cfg.setUploadSpeed(json.getIntValue("upload_speed"));
            if (json.containsKey("socks5_port")) {
                cfg.setSocks5Port(json.getIntValue("socks5_port"));
            }

            if (json.containsKey("protocal")) {
                cfg.setProtocal(json.getString("protocal"));
            }

            this.config = cfg;
        } catch (Exception var4) {
            var4.printStackTrace();
        }

        return cfg;
    }

    void saveConfig() {
        Thread thread = new Thread() {
            public void run() {
                boolean success = false;

                try {
                    int serverPort = 150;
                    String addressTxt = ClientUI.this.text_serverAddress.getText();
                    addressTxt = addressTxt.trim().replaceAll(" ", "");
                    ClientUI.this.text_serverAddress.setText(addressTxt);
                    String serverAddress = addressTxt;
                    int index;
                    String ports;
                    if (addressTxt.startsWith("[")) {
                        index = addressTxt.lastIndexOf("]:");
                        if (index > 0) {
                            serverAddress = addressTxt.substring(0, index + 1);
                            ports = addressTxt.substring(index + 2);
                            serverPort = Integer.parseInt(ports);
                        }
                    } else {
                        index = addressTxt.lastIndexOf(":");
                        if (index > 0) {
                            serverAddress = addressTxt.substring(0, index);
                            ports = addressTxt.substring(index + 1);
                            serverPort = Integer.parseInt(ports);
                        }
                    }

                    String protocal = "tcp";
                    if (ClientUI.this.r_udp.isSelected()) {
                        protocal = "udp";
                    }

                    JSONObject json = new JSONObject();
                    json.put("server_address", serverAddress);
                    json.put("server_port", serverPort);
                    json.put("download_speed", ClientUI.this.config.getDownloadSpeed());
                    json.put("upload_speed", ClientUI.this.config.getUploadSpeed());
                    json.put("socks5_port", ClientUI.this.config.getSocks5Port());
                    json.put("protocal", protocal);
                    ClientUI.this.saveFile(json.toJSONString().getBytes("utf-8"), ClientUI.this.configFilePath);
                    ClientUI.this.config.setServerAddress(serverAddress);
                    ClientUI.this.config.setServerPort(serverPort);
                    ClientUI.this.config.setProtocal(protocal);
                    success = true;
                    String realAddress = serverAddress;
                    if (serverAddress != null) {
                        realAddress = serverAddress.replace("[", "");
                        realAddress = realAddress.replace("]", "");
                    }

                    boolean tcp = protocal.equals("tcp");
                    ClientUI.this.mapClient.setMapServer(realAddress, serverPort, 0, (String)null, (String)null, ClientUI.this.config.isDirect_cn(), tcp, (String)null);
                    ClientUI.this.mapClient.closeAndTryConnect();
                } catch (Exception var12) {
                    var12.printStackTrace();
                } finally {
                    if (!success) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                JOptionPane.showMessageDialog(ClientUI.this.mainFrame, ClientUI.this.errorMsg, "错误", 0);
                            }
                        });
                    }

                }

            }
        };
        thread.start();

        try {
            thread.join();
        } catch (InterruptedException var3) {
            var3.printStackTrace();
        }

    }

    public static String readFileUtf8(String path) throws Exception {
        String str = null;
        FileInputStream fis = null;
        DataInputStream dis = null;

        try {
            File file = new File(path);
            int length = (int)file.length();
            byte[] data = new byte[length];
            fis = new FileInputStream(file);
            dis = new DataInputStream(fis);
            dis.readFully(data);
            str = new String(data, "utf-8");
        } catch (Exception var17) {
            var17.printStackTrace();
            throw var17;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException var16) {
                    var16.printStackTrace();
                }
            }

            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException var15) {
                    var15.printStackTrace();
                }
            }

        }

        return str;
    }

    void saveFile(byte[] data, String path) throws Exception {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(path);
            fos.write(data);
        } catch (Exception var8) {
            if (this.systemName.contains("windows")) {
                JOptionPane.showMessageDialog((Component)null, "请以管理员身份运行222! " + path);
                System.exit(0);
            }

            throw var8;
        } finally {
            if (fos != null) {
                fos.close();
            }

        }

    }

    public void updateUISpeed(int conn, int downloadSpeed, int uploadSpeed) {
        String string = " 下载:" + Tools.getSizeStringKB((long)downloadSpeed) + "/s" + " 上传:" + Tools.getSizeStringKB((long)uploadSpeed) + "/s";
        if (this.downloadSpeedField != null) {
            this.downloadSpeedField.setText(string);
        }

    }

    JButton createButton(String name) {
        JButton button = new JButton(name);
        button.setMargin(new Insets(0, 5, 0, 5));
        button.setFocusPainted(false);
        return button;
    }

    boolean haveNewVersion() {
        return this.serverVersion > this.localVersion;
    }

    public void checkUpdate() {
        int option;
        for(option = 0; option < 3; ++option) {
            this.checkingUpdate = true;

            try {
                Properties propServer = new Properties();
                HttpURLConnection uc = Tools.getConnection(this.updateUrl);
                uc.setUseCaches(false);
                InputStream in = uc.getInputStream();
                propServer.load(in);
                this.serverVersion = Integer.parseInt(propServer.getProperty("version"));
                break;
            } catch (Exception var10) {
                var10.printStackTrace();

                try {
                    Thread.sleep(3000L);
                } catch (InterruptedException var9) {
                    var9.printStackTrace();
                }
            } finally {
                this.checkingUpdate = false;
            }
        }

        if (this.haveNewVersion()) {
            option = JOptionPane.showConfirmDialog(this.mainFrame, "发现新版本,立即更新吗?", "提醒", 2);
            if (option == 0) {
                this.openUrl(this.homeUrl);
            }
        }

    }

    void initUI() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Font font = new Font("宋体", 0, 12);
                UIManager.put("ToolTip.font", font);
                UIManager.put("Table.font", font);
                UIManager.put("TableHeader.font", font);
                UIManager.put("TextField.font", font);
                UIManager.put("ComboBox.font", font);
                UIManager.put("TextField.font", font);
                UIManager.put("PasswordField.font", font);
                UIManager.put("TextArea.font,font", font);
                UIManager.put("TextPane.font", font);
                UIManager.put("EditorPane.font", font);
                UIManager.put("FormattedTextField.font", font);
                UIManager.put("Button.font", font);
                UIManager.put("CheckBox.font", font);
                UIManager.put("RadioButton.font", font);
                UIManager.put("ToggleButton.font", font);
                UIManager.put("ProgressBar.font", font);
                UIManager.put("DesktopIcon.font", font);
                UIManager.put("TitledBorder.font", font);
                UIManager.put("Label.font", font);
                UIManager.put("List.font", font);
                UIManager.put("TabbedPane.font", font);
                UIManager.put("MenuBar.font", font);
                UIManager.put("Menu.font", font);
                UIManager.put("MenuItem.font", font);
                UIManager.put("PopupMenu.font", font);
                UIManager.put("CheckBoxMenuItem.font", font);
                UIManager.put("RadioButtonMenuItem.font", font);
                UIManager.put("Spinner.font", font);
                UIManager.put("Tree.font", font);
                UIManager.put("ToolBar.font", font);
                UIManager.put("OptionPane.messageFont", font);
                UIManager.put("OptionPane.buttonFont", font);
                ToolTipManager.sharedInstance().setInitialDelay(130);
            }
        });
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public boolean login() {
        return false;
    }

    public boolean updateNode(boolean testSpeed) {
        return true;
    }

    public boolean isOsx_fw_pf() {
        return this.osx_fw_pf;
    }

    public void setOsx_fw_pf(boolean osx_fw_pf) {
        this.osx_fw_pf = osx_fw_pf;
    }

    public boolean isOsx_fw_ipfw() {
        return this.osx_fw_ipfw;
    }

    public void setOsx_fw_ipfw(boolean osx_fw_ipfw) {
        this.osx_fw_ipfw = osx_fw_ipfw;
    }
}
