package com.thn.jpegcompressor;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

public class DirectoryWalker
{
    public static void execute(File aDirectory, FileOperator aFileOperator) throws IOException
    {
        if (! aDirectory.exists() || ! aDirectory.isDirectory())
        {
            throw new IllegalArgumentException(aDirectory + " directory could not be found");
        }
        // select files to process in aDirectory
        File[] files = aDirectory.listFiles(aFileOperator);
        // filter out any remaining directory and execute file operator
        for (File file : files)
        {
            if (! file.isDirectory())
            {
                try
                {
                    aFileOperator.execute(file);
                }
                catch (Exception e)
                {
                    System.err.println("Error when processing file: " + file);
                    e.printStackTrace();
                }
            }
        }
        
        // execute the walker on subdirectories
        File[] directories = aDirectory.listFiles(new DirectoryFilter());
        for (File directory : directories)
        {
            execute(directory, aFileOperator);
        }
    }
    
    
    private static class DirectoryFilter implements FileFilter
    {
        @Override
        public boolean accept(File aPathname)
        {
            return aPathname.isDirectory();
        }
    }
}
