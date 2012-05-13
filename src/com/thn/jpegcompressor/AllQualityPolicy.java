package com.thn.jpegcompressor;

import java.io.File;

public class AllQualityPolicy implements CompressRetryPolicy
{
    private int mQualityPercent = 100;
    private MyLogger mLogger;

    public AllQualityPolicy(MyLogger aLogger)
    {
        mLogger = aLogger;
    }

    @Override
    public float computeQuality(File aInputFile)
    {
        mQualityPercent -= 5;
        return mQualityPercent / 100f;
    }

    @Override
    public boolean mustRetry(File aLastInputFile, File aLastOutputFile, float aLastQuality)
    {
        String sep = System.getProperty("file.separator");
        String absolutePath = aLastInputFile.getAbsolutePath();
        long outputLength = aLastOutputFile.length();
        boolean debug = false;
        if (debug)
        {
            String filename = absolutePath.substring(absolutePath.indexOf(sep));
            long inputLength = aLastInputFile.length();
            mLogger.log(filename + " (" + inputLength + " -> " + outputLength + ": "
                    + (outputLength * 100 / inputLength) + "%), Q: " + aLastQuality);
        }
        mLogger.log(aLastQuality + "\t" + outputLength);
        return mQualityPercent > 10;
    }
}
