package com.thn.jpegcompressor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codecimpl.JPEGImageEncoder;

public class MainText
{
    private static final Logger LOGGER = Logger.getLogger(MainText.class);
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
        if (aArgs.length != 1)
        {
            System.err.println("Usage: " + MainText.class.getName() + " <root directory>");
            System.exit(-1);
        }
        File file = new File(aArgs[0]);
        
        JPEGCompressor compressor = new JPEGCompressor(new File("c:/windows/temp"));
        Consumer<File> fileAction = (File f) -> compressor.execute(f);
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
                                                                       new SimpleThreadFactory());
        FileVisitor<Path> fileVisitor = new JPEGFileVisitor(executor, fileAction);
        try {
            Files.walkFileTree(file.toPath() , fileVisitor);
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("could not process all files", e);
        }
    }
}
