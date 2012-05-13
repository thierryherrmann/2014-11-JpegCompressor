package com.thn.jpegcompressor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codecimpl.JPEGImageEncoder;

public class JPEGCompressor implements FileOperator
{
    private static final Pattern INCLUDE_JPG_PATTERN = Pattern.compile("(.*)(\\.jpg)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXCLUDE_JPG_PATTERN = Pattern.compile("(.*_s)(\\.jpg)$", Pattern.CASE_INSENSITIVE);
    private static final long MIN_SIZE_IN_KB = 530;

    private CompressRetryPolicy mRetryPolicy;
    private MyLogger mLogger;
    private final File mTempDirectory;

    public JPEGCompressor(MyLogger aLogger, File aTempDirectory)
    {
        mLogger = aLogger;
        mTempDirectory = aTempDirectory;
    }

    @Override
    public boolean accept(File aFile)
    {

        if (aFile.isDirectory())
        {
            return false;
        }
        // only jpg files and don't process the same file twice
        if (!fileNameAccept(aFile.getAbsolutePath()))
        {
            return false;
        }
        return aFile.length() > MIN_SIZE_IN_KB * 1024;
    }

    public void compressJPEG(File aInFile, File aOutFile, float aQuality) throws IOException
    {
        javax.media.jai.PlanarImage image;
        // use a stream instead of a file. When using a stream, we can close the stream later when
        // finished, so this will
        // release the file handle and we'll be able to delete the file. With a file, the handle is
        // kept (JAI bug ?)
        FileSeekableStream fss = new FileSeekableStream(aInFile);
        image = javax.media.jai.JAI.create("stream", fss);
        JPEGEncodeParam encodeParam = new JPEGEncodeParam();
        mLogger.log("Quality for: " + aInFile + " (" + aInFile.length() + "): " + aQuality);
        encodeParam.setQuality(aQuality);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(aOutFile));
        JPEGImageEncoder encoder = new JPEGImageEncoder(out, encodeParam);
        encoder.setParam(encodeParam);
        encoder.encode(image);
        fss.close();
        out.close();
    }

    private static void copyFile(File aSourceFile, File aDestFile) throws IOException
    {
        // Create channel on the source
        FileChannel srcChannel = new FileInputStream(aSourceFile).getChannel();

        // Create channel on the destination
        FileChannel dstChannel = new FileOutputStream(aDestFile).getChannel();

        // Copy file contents from source to destination
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());

        // Close the channels
        srcChannel.close();
        dstChannel.close();
    }

    @Override
    public void execute(File aFile) throws IOException
    {
        mLogger.log(aFile.getAbsolutePath() + " (" + aFile.length() + ")");
        File outFile = new File(getNewFileName(aFile));
        // copy file to transform to temp directory
        File tempOrigFile = new File(mTempDirectory, aFile.getName());
        File tempDestFile = new File(mTempDirectory, outFile.getName());
        copyFile(aFile, tempOrigFile);
        
        mRetryPolicy = new AdaptivePolicy(mLogger);
        float quality;
        do
        {
            quality = mRetryPolicy.computeQuality(tempOrigFile);
            compressJPEG(tempOrigFile, tempDestFile, quality);
        }
        while (mRetryPolicy.mustRetry(tempOrigFile, tempDestFile, quality));

        // copy EXIF data
        ExifUtil.copyExifData(tempOrigFile, tempDestFile);

        copyFile(tempDestFile, outFile);
        // delete original files
        boolean success = aFile.delete();
        if (!success)
        {
            throw new IllegalStateException("could not delete file " + aFile.getAbsolutePath());
        }
        success = tempOrigFile.delete();
        if (!success)
        {
            throw new IllegalStateException("could not delete file " + tempOrigFile.getAbsolutePath());
        }
        success = tempDestFile.delete();
        if (!success)
        {
            throw new IllegalStateException("could not delete file " + tempDestFile.getAbsolutePath());
        }
    }

    private static boolean fileNameAccept(String aAbsolutePath)
    {
        Matcher m = INCLUDE_JPG_PATTERN.matcher(aAbsolutePath);
        if (!m.matches())
        {
            return false;
        }
        m = EXCLUDE_JPG_PATTERN.matcher(aAbsolutePath);
        if (m.matches())
        {
            return false;
        }
        return true;
    }

    private static String getNewFileName(File aFile)
    {
        String absolutePath = aFile.getAbsolutePath();

        Matcher m = INCLUDE_JPG_PATTERN.matcher(absolutePath);
        if (!m.matches())
        {
            throw new IllegalStateException(aFile + " should not have been selected by the file filter");
        }

        return m.group(1) + "_s" + m.group(2);
    }
}
