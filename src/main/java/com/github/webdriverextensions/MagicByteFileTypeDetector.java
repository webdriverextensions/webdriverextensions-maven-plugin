package com.github.webdriverextensions;

import org.apache.tika.Tika;

public class MagicByteFileTypeDetector {
    public String detectFileType(String path){
        return new Tika().detect(path);
    }
}
