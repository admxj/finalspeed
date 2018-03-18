package com.admxj.utils;

class SpeedUnit
{
    int downSum;
    int upSum;

    void addUp(int n)
    {
        this.upSum += n;
    }

    void addDown(int n)
    {
        this.downSum += n;
    }
}
