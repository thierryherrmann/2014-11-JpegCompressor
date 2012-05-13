package com.thn.jpegcompressor;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

public interface FileOperator extends FileFilter
{
    void execute(File aFile) throws IOException;
}
