package com.thn.jpegcompressor;

import java.io.File;

import org.apache.log4j.Logger;

public class AdaptivePolicy implements CompressRetryPolicy
{
    private static final Logger LOGGER = Logger.getLogger(AdaptivePolicy.class);
    enum State { ITERATING, COMPUTE_LOW_QUALITY, FINAL }
    
    private static final int TARGET_SIZE_IN_K = 500;
    private static final int TARGET_TOLERANCE = 40;

    private int mQualityPercent = 80;
    private State mState = State.ITERATING;
    private long mSizeForHighQuality;
    private long mSizeForLowQuality;

    @Override
    public float computeQuality(File aInputFile)
    {
        if (State.ITERATING == mState)
        {
            mQualityPercent -= 5;
            return mQualityPercent / 100f;
        }
        if (State.COMPUTE_LOW_QUALITY == mState)
        {
            mQualityPercent = 10;
            return mQualityPercent / 100f;
        }
        if (State.FINAL == mState)
        {
            /*
            75 -> mSizeForHighQuality
            10 -> mSizeForLowQuality
            ?  -> 500000
            500     
            75 = mSizeForHighQuality * a + b     
            10 = mSizeForLowQuality  * a + 75 - mSizeForHighQuality * a         
            65 = (mSizeForHighQuality - mSizeForLowQuality) a
            a = 65 / (mSizeForHighQuality - mSizeForLowQuality)
            b = 75 - mSizeForHighQuality  * a
            
            ? = 500000 * a + b
            
             */
            
            int correction = 75;
            float b = 75 - mSizeForHighQuality  * ((float) 65) / (mSizeForHighQuality - mSizeForLowQuality);
            mQualityPercent = Math.round((TARGET_SIZE_IN_K + correction) * 1000 * 65 / (mSizeForHighQuality - mSizeForLowQuality) + b);
            return mQualityPercent / 100f;
        }
        throw new IllegalStateException("bad state: " + mState);
    }

    @Override
    public boolean mustRetry(File aLastInputFile, File aLastOutputFile, float aLastQuality)
    {
        long outputLengthInK = aLastOutputFile.length() / 1000;
        if (State.ITERATING == mState)
        {
            if (outputLengthInK < TARGET_SIZE_IN_K + TARGET_TOLERANCE)
            {
                LOGGER.info("final length: " + aLastOutputFile.length());
                return false;
            }
            if (mQualityPercent == 75)
            {
                mSizeForHighQuality = aLastOutputFile.length();
                mState = State.COMPUTE_LOW_QUALITY;
            }
            return true;
        }
        if (State.COMPUTE_LOW_QUALITY == mState)
        {
            mSizeForLowQuality = aLastOutputFile.length();
            mState = State.FINAL;
            return true;
        }
        if (State.FINAL == mState)
        {
            LOGGER.info("final length for " + aLastOutputFile + ": " + aLastOutputFile.length());
            return false;
        }
        throw new IllegalStateException("bad state: " + mState);
    }
}
