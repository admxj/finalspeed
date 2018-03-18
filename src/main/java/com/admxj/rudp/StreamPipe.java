package com.admxj.rudp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;

public class StreamPipe {
    DataInputStream is;
    DataOutputStream os;
    List<PipeListener> listenerList;
    boolean closed = false;
    int maxLen = 2000;
    long lastResetTime;
    int maxSpeed = 104857600;
    int port = 0;
    int limiteConnectTime;
    String userId = "";
    byte[] preReadData;
    int preReadDataLength;
    Socket socketA;
    Socket socketB;
    boolean writing = false;
    int BUF_SIZE;
    ArrayList<byte[]> dataList = new ArrayList();
    Semaphore semp_have_data = new Semaphore(0);
    int cachedSize = 0;
    int supserSocketId = -1;
    static int type_request = 1;
    static int type_respone = 2;
    int type = 0;
    ConnInfo connInfo;

    public StreamPipe(ConnInfo connInfo, DataInputStream is, DataOutputStream os, int BUF_SIZE, int maxSpeed)
    {
        this(connInfo, is, os, BUF_SIZE, maxSpeed, null, 0);
    }

    public StreamPipe(ConnInfo ci, final DataInputStream is, final DataOutputStream os, int BUF_SIZE1, int maxSpeed, final byte[] preReadData, final int preReadDataLength)
    {
        this.connInfo = ci;
        this.listenerList = new Vector();
        this.maxSpeed = maxSpeed;
        this.preReadData = preReadData;
        this.BUF_SIZE = BUF_SIZE1;

        Runnable thread = new Runnable()
        {
            int count = 0;

            public void run()
            {
                byte[] data = new byte[StreamPipe.this.BUF_SIZE];
                int len = 0;
                try
                {
                    if (preReadData != null) {
                        try
                        {
                            os.write(preReadData, 0, preReadDataLength);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                            return;
                        }
                    }
                    boolean parsed = false;
                    try
                    {
                        while ((len = is.read(data)) > 0) {
                            try
                            {
                                os.write(data, 0, len);
                            }
                            catch (IOException e)
                            {
                                break;
                            }
                        }
                    }
                    catch (IOException localIOException1) {}
                }
                finally
                {
                    StreamPipe.this.close();
                }
            }
        };
        Route.es.execute(thread);
    }

    void close()
    {
        if (!this.closed)
        {
            this.closed = true;
            try
            {
                Thread.sleep(500L);
            }
            catch (InterruptedException localInterruptedException) {}
            if (this.socketA != null) {
                Route.es.execute(new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            StreamPipe.this.socketA.close();
                        }
                        catch (IOException localIOException) {}
                    }
                });
            }
            if (this.socketB != null) {
                Route.es.execute(new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            StreamPipe.this.socketB.close();
                        }
                        catch (IOException localIOException) {}
                    }
                });
            }
            fireClose();
        }
    }

    class HttpHost
    {
        String address;
        int port = 80;

        HttpHost() {}

        public String getAddress()
        {
            return this.address;
        }

        public void setAddress(String address)
        {
            this.address = address;
        }

        public int getPort()
        {
            return this.port;
        }

        public void setPort(int port)
        {
            this.port = port;
        }
    }

    HttpHost readHost(String data)
    {
        HttpHost hh = new HttpHost();
        String host = null;
        data = data.replaceAll("\r", "");
        data = data.replaceAll(" ", "");
        String[] ls = data.split("\n");
        String[] arrayOfString1;
        int j = (arrayOfString1 = ls).length;
        for (int i = 0; i < j; i++)
        {
            String l = arrayOfString1[i];
            if (l.startsWith("Host:"))
            {
                String s1 = l.substring(5);
                int index2 = s1.indexOf(":");
                if (index2 > -1)
                {
                    int port = Integer.parseInt(s1.substring(index2 + 1));
                    hh.setPort(port);
                    s1 = s1.substring(0, index2);
                }
                host = s1;
                hh.setAddress(host);
            }
        }
        return hh;
    }

    public void addListener(PipeListener listener)
    {
        this.listenerList.add(listener);
    }

    void fireClose()
    {
        for (PipeListener listener : this.listenerList) {
            listener.pipeClose();
        }
    }

    public int getPort()
    {
        return this.port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public int getLimiteConnectTime()
    {
        return this.limiteConnectTime;
    }

    public void setLimiteConnectTime(int limiteConnectTime)
    {
        this.limiteConnectTime = limiteConnectTime;
    }

    public String getUserId()
    {
        return this.userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public Socket getSocketA()
    {
        return this.socketA;
    }

    public void setSocketA(Socket socketA)
    {
        this.socketA = socketA;
    }

    public Socket getSocketB()
    {
        return this.socketB;
    }

    public void setSocketB(Socket socketB)
    {
        this.socketB = socketB;
    }

    public int getSupserSocketId()
    {
        return this.supserSocketId;
    }

    public void setSupserSocketId(int supserSocketId)
    {
        this.supserSocketId = supserSocketId;
    }

    public int getType()
    {
        return this.type;
    }

    public void setType(int type)
    {
        this.type = type;
    }

    public ConnInfo getConnInfo()
    {
        return this.connInfo;
    }

    public void setConnInfo(ConnInfo connInfo)
    {
        this.connInfo = connInfo;
    }
}
