package com.admxj.client;

import com.admxj.rudp.ConnectionUDP;
import com.admxj.rudp.UDPInputStream;
import com.admxj.rudp.UDPOutputStream;

import java.io.InputStream;
import java.io.OutputStream;


public class Pipe {
    int lastTime = -1;
    boolean readed = false;
    public Pipe p2;
    byte[] pv;
    int pvl;

    public Pipe() {
    }

    public void pipe(InputStream is, UDPOutputStream tos, int initSpeed, Pipe p2) throws Exception {
        byte[] buf = new byte[102400];

        int len;
        for(boolean sendeda = false; (len = is.read(buf)) > 0; tos.write(buf, 0, len)) {
            this.readed = true;
            if (!sendeda) {
                sendeda = true;
            }
        }

    }

    void sendSleep(long startTime, int speed, int length) {
        long needTime = (long)(1000.0F * (float)length / (float)speed);
        long usedTime = System.currentTimeMillis() - startTime;
        if (usedTime < needTime) {
            try {
                Thread.sleep(needTime - usedTime);
            } catch (InterruptedException var10) {
                var10.printStackTrace();
            }
        }

    }

    public void pipe(UDPInputStream tis, OutputStream os, int maxSpeed, ConnectionUDP conn) throws Exception {
        byte[] buf = new byte[1000];
        boolean sended = false;
        boolean sendedb = false;
        int var9 = 0;

        int len;
        while((len = tis.read(buf, 0, buf.length)) > 0) {
            if (!sendedb) {
                this.pv = buf;
                this.pvl = len;
                sendedb = true;
            }

            ++var9;
            long needTime = (long)(1000 * len / maxSpeed);
            long startTime = System.currentTimeMillis();
            os.write(buf, 0, len);
            if (!sended) {
                sended = true;
            }

            long usedTime = System.currentTimeMillis() - startTime;
            if (usedTime < needTime) {
                try {
                    Thread.sleep(needTime - usedTime);
                } catch (InterruptedException var17) {
                    var17.printStackTrace();
                }
            }
        }

    }
}
