package com.thn.jpegcompressor;

public class ConsoleMyLogger implements MyLogger
{
    @Override
    public void log(String aString)
    {
        System.out.println(aString);
    }
}
