package com.admxj.rudp;

public abstract interface Trafficlistener
{
    public abstract void trafficDownload(TrafficEvent paramTrafficEvent);

    public abstract void trafficUpload(TrafficEvent paramTrafficEvent);
}
