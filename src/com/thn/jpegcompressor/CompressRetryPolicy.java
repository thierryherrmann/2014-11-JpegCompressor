package com.thn.jpegcompressor;

import java.io.File;

public interface CompressRetryPolicy
{
    boolean mustRetry(File aLastInputFile, File aLastOutputFile, float aLastQuality);
    
    float computeQuality(File aInputFile);
}
