package com.admxj.server;

import com.admxj.client.Pipe;
import com.admxj.rudp.*;
import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


public class MapTunnelProcessor
        implements ConnectionProcessor
{
    Socket dstSocket = null;
    boolean closed = false;
    MapTunnelProcessor pc;
    ConnectionUDP conn;
    UDPInputStream tis;
    UDPOutputStream tos;
    InputStream sis;
    OutputStream sos;

    public void process(ConnectionUDP conn)
    {
        this.conn = conn;
        this.pc = this;
        Route.es.execute(new Runnable()
        {
            public void run()
            {
                MapTunnelProcessor.this.process();
            }
        });
    }

    void process()
    {
        this.tis = this.conn.uis;
        this.tos = this.conn.uos;
        try
        {
            byte[] headData = this.tis.read2();
            String hs = new String(headData, "utf-8");
            JSONObject requestJSon = JSONObject.parseObject(hs);
            int dstPort = requestJSon.getIntValue("dst_port");
            String message = "";
            JSONObject responeJSon = new JSONObject();
            int code = Constant.code_failed;
            code = Constant.code_success;
            responeJSon.put("code", Integer.valueOf(code));
            responeJSon.put("message", message);
            byte[] responeData = responeJSon.toJSONString().getBytes("utf-8");
            this.tos.write(responeData, 0, responeData.length);
            if (code != Constant.code_success)
            {
                close();
                return;
            }
            this.dstSocket = new Socket("127.0.0.1", dstPort);
            this.dstSocket.setTcpNoDelay(true);
            this.sis = this.dstSocket.getInputStream();
            this.sos = this.dstSocket.getOutputStream();

            final Pipe p1 = new Pipe();
            final Pipe p2 = new Pipe();

            Route.es.execute(new Runnable()
            {
                /* Error */
                public void run()
                {
                    // Byte code:
                    //   0: aload_0
                    //   1: getfield 17	net/fs/server/MapTunnelProcessor$2:val$p1	Lnet/fs/client/Pipe;
                    //   4: aload_0
                    //   5: getfield 15	net/fs/server/MapTunnelProcessor$2:this$0	Lnet/fs/server/MapTunnelProcessor;
                    //   8: getfield 29	net/fs/server/MapTunnelProcessor:sis	Ljava/io/InputStream;
                    //   11: aload_0
                    //   12: getfield 15	net/fs/server/MapTunnelProcessor$2:this$0	Lnet/fs/server/MapTunnelProcessor;
                    //   15: getfield 35	net/fs/server/MapTunnelProcessor:tos	Lnet/fs/rudp/UDPOutputStream;
                    //   18: ldc 39
                    //   20: aload_0
                    //   21: getfield 19	net/fs/server/MapTunnelProcessor$2:val$p2	Lnet/fs/client/Pipe;
                    //   24: invokevirtual 40	net/fs/client/Pipe:pipe	(Ljava/io/InputStream;Lnet/fs/rudp/UDPOutputStream;ILnet/fs/client/Pipe;)V
                    //   27: goto +35 -> 62
                    //   30: astore_1
                    //   31: aload_0
                    //   32: getfield 15	net/fs/server/MapTunnelProcessor$2:this$0	Lnet/fs/server/MapTunnelProcessor;
                    //   35: invokevirtual 46	net/fs/server/MapTunnelProcessor:close	()V
                    //   38: goto +31 -> 69
                    //   41: astore_1
                    //   42: aload_0
                    //   43: getfield 15	net/fs/server/MapTunnelProcessor$2:this$0	Lnet/fs/server/MapTunnelProcessor;
                    //   46: invokevirtual 46	net/fs/server/MapTunnelProcessor:close	()V
                    //   49: goto +20 -> 69
                    //   52: astore_2
                    //   53: aload_0
                    //   54: getfield 15	net/fs/server/MapTunnelProcessor$2:this$0	Lnet/fs/server/MapTunnelProcessor;
                    //   57: invokevirtual 46	net/fs/server/MapTunnelProcessor:close	()V
                    //   60: aload_2
                    //   61: athrow
                    //   62: aload_0
                    //   63: getfield 15	net/fs/server/MapTunnelProcessor$2:this$0	Lnet/fs/server/MapTunnelProcessor;
                    //   66: invokevirtual 46	net/fs/server/MapTunnelProcessor:close	()V
                    //   69: return
                    // Line number table:
                    //   Java source line #87	-> byte code offset #0
                    //   Java source line #88	-> byte code offset #27
                    //   Java source line #93	-> byte code offset #31
                    //   Java source line #90	-> byte code offset #41
                    //   Java source line #93	-> byte code offset #42
                    //   Java source line #92	-> byte code offset #52
                    //   Java source line #93	-> byte code offset #53
                    //   Java source line #94	-> byte code offset #60
                    //   Java source line #93	-> byte code offset #62
                    //   Java source line #95	-> byte code offset #69
                    // Local variable table:
                    //   start	length	slot	name	signature
                    //   0	70	0	this	2
                    //   30	1	1	localIOException	IOException
                    //   41	1	1	localException	Exception
                    //   52	9	2	localObject	Object
                    // Exception table:
                    //   from	to	target	type
                    //   0	27	30	java/io/IOException
                    //   0	27	41	java/lang/Exception
                    //   0	31	52	finally
                    //   41	42	52	finally
                }
            });
            Route.es.execute(new Runnable()
            {
                /* Error */
                public void run()
                {
                    // Byte code:
                    //   0: aload_0
                    //   1: getfield 16	net/fs/server/MapTunnelProcessor$3:val$p2	Lnet/fs/client/Pipe;
                    //   4: aload_0
                    //   5: getfield 14	net/fs/server/MapTunnelProcessor$3:this$0	Lnet/fs/server/MapTunnelProcessor;
                    //   8: getfield 26	net/fs/server/MapTunnelProcessor:tis	Lnet/fs/rudp/UDPInputStream;
                    //   11: aload_0
                    //   12: getfield 14	net/fs/server/MapTunnelProcessor$3:this$0	Lnet/fs/server/MapTunnelProcessor;
                    //   15: getfield 32	net/fs/server/MapTunnelProcessor:sos	Ljava/io/OutputStream;
                    //   18: ldc 36
                    //   20: aload_0
                    //   21: getfield 14	net/fs/server/MapTunnelProcessor$3:this$0	Lnet/fs/server/MapTunnelProcessor;
                    //   24: getfield 37	net/fs/server/MapTunnelProcessor:conn	Lnet/fs/rudp/ConnectionUDP;
                    //   27: invokevirtual 41	net/fs/client/Pipe:pipe	(Lnet/fs/rudp/UDPInputStream;Ljava/io/OutputStream;ILnet/fs/rudp/ConnectionUDP;)V
                    //   30: goto +35 -> 65
                    //   33: astore_1
                    //   34: aload_0
                    //   35: getfield 14	net/fs/server/MapTunnelProcessor$3:this$0	Lnet/fs/server/MapTunnelProcessor;
                    //   38: invokevirtual 47	net/fs/server/MapTunnelProcessor:close	()V
                    //   41: goto +31 -> 72
                    //   44: astore_1
                    //   45: aload_0
                    //   46: getfield 14	net/fs/server/MapTunnelProcessor$3:this$0	Lnet/fs/server/MapTunnelProcessor;
                    //   49: invokevirtual 47	net/fs/server/MapTunnelProcessor:close	()V
                    //   52: goto +20 -> 72
                    //   55: astore_2
                    //   56: aload_0
                    //   57: getfield 14	net/fs/server/MapTunnelProcessor$3:this$0	Lnet/fs/server/MapTunnelProcessor;
                    //   60: invokevirtual 47	net/fs/server/MapTunnelProcessor:close	()V
                    //   63: aload_2
                    //   64: athrow
                    //   65: aload_0
                    //   66: getfield 14	net/fs/server/MapTunnelProcessor$3:this$0	Lnet/fs/server/MapTunnelProcessor;
                    //   69: invokevirtual 47	net/fs/server/MapTunnelProcessor:close	()V
                    //   72: return
                    // Line number table:
                    //   Java source line #102	-> byte code offset #0
                    //   Java source line #103	-> byte code offset #30
                    //   Java source line #108	-> byte code offset #34
                    //   Java source line #105	-> byte code offset #44
                    //   Java source line #108	-> byte code offset #45
                    //   Java source line #107	-> byte code offset #55
                    //   Java source line #108	-> byte code offset #56
                    //   Java source line #109	-> byte code offset #63
                    //   Java source line #108	-> byte code offset #65
                    //   Java source line #110	-> byte code offset #72
                    // Local variable table:
                    //   start	length	slot	name	signature
                    //   0	73	0	this	3
                    //   33	1	1	localIOException	IOException
                    //   44	1	1	localException	Exception
                    //   55	9	2	localObject	Object
                    // Exception table:
                    //   from	to	target	type
                    //   0	30	33	java/io/IOException
                    //   0	30	44	java/lang/Exception
                    //   0	34	55	finally
                    //   44	45	55	finally
                }
            });
        }
        catch (Exception e2)
        {
            close();
        }
    }

    void close()
    {
        if (!this.closed)
        {
            this.closed = true;
            if (this.sis != null) {
                try
                {
                    this.sis.close();
                }
                catch (IOException localIOException) {}
            }
            if (this.sos != null) {
                try
                {
                    this.sos.close();
                }
                catch (IOException localIOException1) {}
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
            if (this.dstSocket != null) {
                try
                {
                    this.dstSocket.close();
                }
                catch (IOException localIOException2) {}
            }
        }
    }
}
