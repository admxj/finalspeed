package com.admxj.rudp;

public class RUDPConfig
{
    public static short protocal_ver = 0;
    public static int packageSize = 1000;
    public static boolean twice_udp = false;
    public static boolean twice_tcp = false;
    public static int maxWin = 1024;
    public static int ackListDelay = 5;
    public static int ackListSum = 300;
    public static boolean double_send_start = true;
    public static int reSendDelay_min = 100;
    public static float reSendDelay = 0.37F;
    public static int reSendTryTimes = 10;
}

