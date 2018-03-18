package com.admxj.rudp;

public class ConnInfo
{
    String requestHost = null;
    String requestPath = null;
    boolean http = false;
    StreamPipe.HttpHost host = null;

    public String getRequestHost()
    {
        return this.requestHost;
    }

    public void setRequestHost(String requestHost)
    {
        this.requestHost = requestHost;
    }

    public String getRequestPath()
    {
        return this.requestPath;
    }

    public void setRequestPath(String requestPath)
    {
        this.requestPath = requestPath;
    }

    public boolean isHttp()
    {
        return this.http;
    }

    public void setHttp(boolean http)
    {
        this.http = http;
    }

    public StreamPipe.HttpHost getHost()
    {
        return this.host;
    }

    public void setHost(StreamPipe.HttpHost host)
    {
        this.host = host;
    }
}
