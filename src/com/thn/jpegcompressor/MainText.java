package com.thn.jpegcompressor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codecimpl.JPEGImageEncoder;

public class MainText
{
    public static void compressJPEG(String inFile, String outFile) throws Exception
    {
        javax.media.jai.PlanarImage image;
        image = javax.media.jai.JAI.create("fileload", inFile);
        JPEGEncodeParam encodeParam = new JPEGEncodeParam();
        encodeParam.setQuality(0.15f);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
        JPEGImageEncoder encoder = new JPEGImageEncoder(out, encodeParam);
        encoder.setParam(encodeParam);
        encoder.encode(image);
    }

    public static void main(String[] aArgs)
    {
        try
        {
            if (aArgs.length != 1)
            {
                System.err.println("Usage: " + MainText.class.getName() + " <root directory>");
                System.exit(-1);
            }
            JPEGCompressor compressor = new JPEGCompressor(new ConsoleMyLogger(), new File("c:/windows/temp"));
            DirectoryWalker.execute(new File(aArgs[0]), compressor);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
