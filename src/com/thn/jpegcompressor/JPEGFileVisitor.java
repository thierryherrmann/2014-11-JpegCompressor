package com.thn.jpegcompressor;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class JPEGFileVisitor implements FileVisitor<Path> {
    public static final Pattern INCLUDE_JPG_PATTERN = Pattern.compile("(.*)(\\.jpg)$", Pattern.CASE_INSENSITIVE);
    private static final Logger LOGGER = Logger.getLogger(JPEGFileVisitor.class);
    private static final Pattern EXCLUDE_JPG_PATTERN = Pattern.compile("(.*_s)(\\.jpg)$", Pattern.CASE_INSENSITIVE);
    private static final long MIN_SIZE_IN_KB = 530;

    private final ExecutorService executor;
    private Consumer<File> fileAction;

    public JPEGFileVisitor(ExecutorService executor, Consumer<File> fileAction) {
        this.executor = executor;
        this.fileAction = fileAction;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            throws IOException {
        LOGGER.info(dir);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs)
            throws IOException {
        File file = filePath.toFile();
        if (accept(file)) {
            executor.execute(() -> fileAction.accept(file));
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc)
            throws IOException {
        LOGGER.error("Could not visit file " + file, exc);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc)
            throws IOException {
        return FileVisitResult.CONTINUE;
    }
    
    private static boolean accept(File aFile)
    {
        if (aFile.isDirectory())
        {
            return false;
        }
        String absPath = aFile.getAbsolutePath();
        // only jpg files and don't process the same file twice
        Matcher m = INCLUDE_JPG_PATTERN.matcher(absPath);
        if (!m.matches())
        {
            return false;
        }
        m = EXCLUDE_JPG_PATTERN.matcher(absPath);
        if (m.matches())
        {
            return false;
        }
        return aFile.length() > MIN_SIZE_IN_KB * 1024;
    }
}
