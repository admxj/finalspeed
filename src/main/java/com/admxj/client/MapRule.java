package com.admxj.client;

import java.io.Serializable;
import java.net.ServerSocket;

public class MapRule implements Serializable {
    int listen_port;
    int dst_port;
    String name;
    boolean using = false;
    ServerSocket serverSocket;

    public MapRule() {
    }

    public int getListen_port() {
        return this.listen_port;
    }

    public void setListen_port(int listen_port) {
        this.listen_port = listen_port;
    }

    public int getDst_port() {
        return this.dst_port;
    }

    public void setDst_port(int dst_port) {
        this.dst_port = dst_port;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
