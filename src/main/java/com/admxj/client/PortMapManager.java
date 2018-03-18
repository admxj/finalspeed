package com.admxj.client;

import com.admxj.rudp.Route;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class PortMapManager {
    MapClient mapClient;
    ArrayList<MapRule> mapList = new ArrayList();
    HashMap<Integer, MapRule> mapRuleTable = new HashMap();
    String configFilePath = "port_map.json";

    PortMapManager(MapClient mapClient) {
        this.mapClient = mapClient;
        this.loadMapRule();
    }

    void addMapRule(MapRule mapRule) throws Exception {
        if (this.getMapRule(mapRule.name) != null) {
            throw new Exception("映射 " + mapRule.name + " 已存在,请修改名称!");
        } else {
            ServerSocket serverSocket = null;

            try {
                serverSocket = new ServerSocket(mapRule.getListen_port());
                this.listen(serverSocket);
                this.mapList.add(mapRule);
                this.mapRuleTable.put(mapRule.listen_port, mapRule);
                this.saveMapRule();
            } catch (IOException var4) {
                throw new Exception("端口 " + mapRule.getListen_port() + " 已经被占用!");
            }
        }
    }

    void removeMapRule(String name) {
        MapRule mapRule = this.getMapRule(name);
        if (mapRule != null) {
            this.mapList.remove(mapRule);
            this.mapRuleTable.remove(mapRule.listen_port);
            if (mapRule.serverSocket != null) {
                try {
                    mapRule.serverSocket.close();
                } catch (IOException var5) {
                    var5.printStackTrace();
                }
            }

            try {
                this.saveMapRule();
            } catch (Exception var4) {
                var4.printStackTrace();
            }
        }

    }

    void updateMapRule(MapRule mapRule_origin, MapRule mapRule_new) throws Exception {
        if (this.getMapRule(mapRule_new.name) != null && !mapRule_origin.name.equals(mapRule_new.name)) {
            throw new Exception("映射 " + mapRule_new.name + " 已存在,请修改名称!");
        } else {
            ServerSocket serverSocket = null;
            if (mapRule_origin.listen_port != mapRule_new.listen_port) {
                try {
                    serverSocket = new ServerSocket(mapRule_new.getListen_port());
                    this.listen(serverSocket);
                    mapRule_origin.using = false;
                    if (mapRule_origin.serverSocket != null) {
                        mapRule_origin.serverSocket.close();
                    }

                    mapRule_origin.serverSocket = serverSocket;
                    this.mapRuleTable.remove(mapRule_origin.listen_port);
                    this.mapRuleTable.put(mapRule_new.listen_port, mapRule_new);
                } catch (IOException var5) {
                    throw new Exception("端口 " + mapRule_new.getListen_port() + " 已经被占用!");
                }
            }

            mapRule_origin.name = mapRule_new.name;
            mapRule_origin.listen_port = mapRule_new.listen_port;
            mapRule_origin.dst_port = mapRule_new.dst_port;
            this.saveMapRule();
        }
    }

    void saveMapRule() throws Exception {
        JSONObject json = new JSONObject();
        JSONArray json_map_list = new JSONArray();
        json.put("map_list", json_map_list);
        this.mapList.size();
        Iterator var4 = this.mapList.iterator();

        while(var4.hasNext()) {
            MapRule r = (MapRule)var4.next();
            JSONObject json_rule = new JSONObject();
            json_rule.put("name", r.name);
            json_rule.put("listen_port", r.listen_port);
            json_rule.put("dst_port", r.dst_port);
            json_map_list.add(json_rule);
        }

        try {
            this.saveFile(json.toJSONString().getBytes("utf-8"), this.configFilePath);
        } catch (UnsupportedEncodingException var6) {
            var6.printStackTrace();
        } catch (Exception var7) {
            var7.printStackTrace();
            throw new Exception("保存失败!");
        }

    }

    void loadMapRule() {
        JSONObject json = null;

        try {
            String content = readFileUtf8(this.configFilePath);
            json = JSONObject.parseObject(content);
        } catch (Exception var10) {
            ;
        }

        if (json != null && json.containsKey("map_list")) {
            JSONArray json_map_list = json.getJSONArray("map_list");

            for(int i = 0; i < json_map_list.size(); ++i) {
                JSONObject json_rule = (JSONObject)json_map_list.get(i);
                MapRule mapRule = new MapRule();
                mapRule.name = json_rule.getString("name");
                mapRule.listen_port = json_rule.getIntValue("listen_port");
                mapRule.dst_port = json_rule.getIntValue("dst_port");
                this.mapList.add(mapRule);

                try {
                    ServerSocket serverSocket = new ServerSocket(mapRule.getListen_port());
                    this.listen(serverSocket);
                    mapRule.serverSocket = serverSocket;
                } catch (IOException var9) {
                    mapRule.using = true;
                    var9.printStackTrace();
                }

                this.mapRuleTable.put(mapRule.listen_port, mapRule);
            }
        }

    }

    MapRule getMapRule(String name) {
        MapRule rule = null;
        Iterator var4 = this.mapList.iterator();

        while(var4.hasNext()) {
            MapRule r = (MapRule)var4.next();
            if (r.getName().equals(name)) {
                rule = r;
                break;
            }
        }

        return rule;
    }

    public ArrayList<MapRule> getMapList() {
        return this.mapList;
    }

    public void setMapList(ArrayList<MapRule> mapList) {
        this.mapList = mapList;
    }

    void listen(final ServerSocket serverSocket) {
        Route.es.execute(new Runnable() {
            public void run() {
                while(true) {
                    try {
                        final Socket socket = serverSocket.accept();
                        Route.es.execute(new Runnable() {
                            public void run() {
                                int listenPort = serverSocket.getLocalPort();
                                MapRule mapRule = (MapRule) PortMapManager.this.mapRuleTable.get(listenPort);
                                if (mapRule != null) {
                                    Route route = null;
                                    if (PortMapManager.this.mapClient.isUseTcp()) {
                                        route = PortMapManager.this.mapClient.route_tcp;
                                    } else {
                                        route = PortMapManager.this.mapClient.route_udp;
                                    }

                                    new PortMapProcess(PortMapManager.this.mapClient, route, socket, PortMapManager.this.mapClient.serverAddress, PortMapManager.this.mapClient.serverPort, (String)null, (String)null, mapRule.dst_port);
                                }

                            }
                        });
                    } catch (IOException var2) {
                        var2.printStackTrace();
                        return;
                    }
                }
            }
        });
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
}
