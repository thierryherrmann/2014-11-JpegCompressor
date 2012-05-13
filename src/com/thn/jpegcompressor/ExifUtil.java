package com.thn.jpegcompressor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.cmc.sanselan.ImageReadException;
import org.cmc.sanselan.ImageWriteException;
import org.cmc.sanselan.Sanselan;
import org.cmc.sanselan.common.IImageMetadata;
import org.cmc.sanselan.formats.jpeg.JpegImageMetadata;
import org.cmc.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.cmc.sanselan.formats.tiff.TiffImageMetadata;
import org.cmc.sanselan.formats.tiff.write.TiffOutputSet;

public class ExifUtil
{
    public static void copyExifData(File aExifFile, File aJpgWithoutExif)
    {
        File tempFile = null;
        IImageMetadata metadata = null;
        JpegImageMetadata jpegMetadata = null;
        TiffImageMetadata exif = null;
        OutputStream os = null;
        TiffOutputSet outputSet = new TiffOutputSet();

        // establish metadata
        try
        {
            metadata = Sanselan.getMetadata(aExifFile);
        }
        catch (ImageReadException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        // establish jpegMedatadata
        if (metadata != null)
        {
            jpegMetadata = (JpegImageMetadata) metadata;
        }

        // establish exif
        if (jpegMetadata != null)
        {
            exif = jpegMetadata.getExif();
        }

        // establish outputSet
        if (exif == null)
        {
            return;
        }
        
        try
        {
            outputSet = exif.getOutputSet();
        }
        catch (ImageWriteException e)
        {
            e.printStackTrace();
        }

        // create stream using temp file for dst
        try
        {
            tempFile = File.createTempFile("temp-" + System.currentTimeMillis(), ".jpg");
            os = new FileOutputStream(tempFile);
            os = new BufferedOutputStream(os);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        // write/update EXIF metadata to output stream
        try
        {
            new ExifRewriter().updateExifMetadataLossless(aJpgWithoutExif, os, outputSet);
        }
        catch (ImageReadException e)
        {
            e.printStackTrace();
        }
        catch (ImageWriteException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (os != null)
            {
                try
                {
                    os.close();
                }
                catch (IOException e)
                {
                }
            }
        }

        forceRenameFile(tempFile, aJpgWithoutExif);
    }

    private static void forceRenameFile(File aSource, File aDestination)
    {
        if (aDestination.exists())
        {
            boolean success = aDestination.delete();
            if (!success)
            {
                throw new IllegalStateException("Could not delete file: " + aDestination);
            }
        }
        boolean success = aSource.renameTo(aDestination);
        if (!success)
        {
            throw new IllegalStateException("Could not rename file: " + aSource + " -> "
                    + aDestination);
        }
    }
}