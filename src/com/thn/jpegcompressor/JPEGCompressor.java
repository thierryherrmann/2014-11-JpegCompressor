package com.thn.jpegcompressor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codecimpl.JPEGImageEncoder;

public class JPEGCompressor
{
    private static final Logger LOGGER = Logger.getLogger(JPEGCompressor.class);

    private final File mTempDirectory;

    public JPEGCompressor(File aTempDirectory)
    {
        mTempDirectory = aTempDirectory;
    }

    public void execute(File aFile)
    {
        try {
            LOGGER.info(aFile.getAbsolutePath() + " (" + aFile.length() + ")");
            File outFile = new File(getNewFileName(aFile));
            // copy file to transform to temp directory
            File tempOrigFile = new File(mTempDirectory, aFile.getName());
            File tempDestFile = new File(mTempDirectory, outFile.getName());
            copyFile(aFile, tempOrigFile);
            
            CompressRetryPolicy retryPolicy = new AdaptivePolicy();
            float quality;
            do
            {
                quality = retryPolicy.computeQuality(tempOrigFile);
                compressJPEG(tempOrigFile, tempDestFile, quality);
            }
            while (retryPolicy.mustRetry(tempOrigFile, tempDestFile, quality));

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
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Could not process file: " + aFile, e);
        }
    }

    private static void copyFile(File aSourceFile, File aDestFile) throws IOException
    {
        Files.copy(aSourceFile.toPath(), aDestFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void compressJPEG(File aInFile, File aOutFile, float aQuality) throws IOException
    {
        javax.media.jai.PlanarImage image;
        // use a stream instead of a file. When using a stream, we can close the stream later when
        // finished, so this will
        // release the file handle and we'll be able to delete the file. With a file, the handle is
        // kept (JAI bug ?)
        FileSeekableStream fss = new FileSeekableStream(aInFile);
        image = javax.media.jai.JAI.create("stream", fss);
        JPEGEncodeParam encodeParam = new JPEGEncodeParam();
        LOGGER.info("Quality for: " + aInFile + " (" + aInFile.length() + "): " + aQuality);
        encodeParam.setQuality(aQuality);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(aOutFile));
        JPEGImageEncoder encoder = new JPEGImageEncoder(out, encodeParam);
        encoder.setParam(encodeParam);
        encoder.encode(image);
        fss.close();
        out.close();
    }

    private static String getNewFileName(File aFile)
    {
        String absolutePath = aFile.getAbsolutePath();

        Matcher m = JPEGFileVisitor.INCLUDE_JPG_PATTERN.matcher(absolutePath);
        if (!m.matches())
        {
            throw new IllegalStateException(aFile + " should not have been selected by the file filter");
        }

        return m.group(1) + "_s" + m.group(2);
    }
}
