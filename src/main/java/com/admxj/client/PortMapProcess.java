package com.admxj.client;

import com.alibaba.fastjson.JSONObject;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;


import com.admxj.rudp.ClientProcessorInterface;
import com.admxj.rudp.ConnectionUDP;
import com.admxj.rudp.Constant;
import com.admxj.rudp.Route;
import com.admxj.rudp.UDPInputStream;
import com.admxj.rudp.UDPOutputStream;

public class PortMapProcess implements ClientProcessorInterface {
    Random ran = new Random();
    UDPInputStream tis;
    UDPOutputStream tos;
    String serverAddress = "";
    int serverPort;
    ConnectionUDP conn;
    MapClient mapClient;
    public Socket srcSocket;
    public Socket dstSocket;
    String password_proxy_md5;
    DataInputStream srcIs = null;
    DataOutputStream srcOs = null;
    boolean closed = false;
    boolean success = false;

    public PortMapProcess(MapClient mapClient, Route route, Socket srcSocket, String serverAddress2, int serverPort2, String password_proxy_md5, String dstAddress, int dstPort) {
        this.mapClient = mapClient;
        this.serverAddress = serverAddress2;
        this.serverPort = serverPort2;
        this.srcSocket = srcSocket;
        this.password_proxy_md5 = password_proxy_md5;

        try {
            this.srcIs = new DataInputStream(srcSocket.getInputStream());
            this.srcOs = new DataOutputStream(srcSocket.getOutputStream());
            this.conn = route.getConnection(this.serverAddress, this.serverPort, (String)null);
            this.tis = this.conn.uis;
            this.tos = this.conn.uos;
            JSONObject requestJson = new JSONObject();
            requestJson.put("dst_address", dstAddress);
            requestJson.put("dst_port", dstPort);
            requestJson.put("password_proxy_md5", password_proxy_md5);
            byte[] requestData = requestJson.toJSONString().getBytes("utf-8");
            this.tos.write(requestData, 0, requestData.length);
            final Pipe p1 = new Pipe();
            final Pipe p2 = new Pipe();
            byte[] responeData = this.tis.read2();
            String hs = new String(responeData, "utf-8");
            JSONObject responeJSon = JSONObject.parseObject(hs);
            int code = responeJSon.getIntValue("code");
            String message = responeJSon.getString("message");
            String uimessage = "";
            if (code == Constant.code_success) {
                Route.es.execute(new Runnable() {
                    public void run() {
                        try {
                            p2.pipe(PortMapProcess.this.tis, PortMapProcess.this.srcOs, 1073741824, (ConnectionUDP)null);
                        } catch (Exception var5) {
                            var5.printStackTrace();
                        } finally {
                            PortMapProcess.this.close();
                        }

                    }
                });
                Route.es.execute(new Runnable() {
                    public void run() {
                        try {
                            p1.pipe(PortMapProcess.this.srcIs, PortMapProcess.this.tos, 204800, p2);
                        } catch (Exception var5) {
                            ;
                        } finally {
                            PortMapProcess.this.close();
                        }

                    }
                });
                this.success = true;
                uimessage = "连接服务器成功";
            } else {
                this.close();
                uimessage = "连接服务器失败," + message;
            }

            if (ClientUI.ui != null) {
                ClientUI.ui.setMessage(uimessage);
            }
        } catch (Exception var19) {
            var19.printStackTrace();
        }

    }

    void close() {
        if (!this.closed) {
            this.closed = true;
            if (this.srcIs != null) {
                try {
                    this.srcIs.close();
                } catch (IOException var4) {
                    var4.printStackTrace();
                }
            }

            if (this.srcOs != null) {
                try {
                    this.srcOs.close();
                } catch (IOException var3) {
                    var3.printStackTrace();
                }
            }

            if (this.tos != null) {
                this.tos.closeStream_Local();
            }

            if (this.tis != null) {
                this.tis.closeStream_Local();
            }

            if (this.conn != null) {
                this.conn.close_local();
            }

            if (this.srcSocket != null) {
                try {
                    this.srcSocket.close();
                } catch (IOException var2) {
                    var2.printStackTrace();
                }
            }

            this.mapClient.onProcessClose(this);
        }

    }

    public void onMapClientClose() {
        try {
            this.srcSocket.close();
        } catch (IOException var2) {
            var2.printStackTrace();
        }

    }
}
