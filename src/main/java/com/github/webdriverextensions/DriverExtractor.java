package com.github.webdriverextensions;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

public class DriverExtractor {
    private final String tempDirectory;
    private final Log log;

    public DriverExtractor(String tempDirectory, Log log) {
        this.tempDirectory = tempDirectory;
        this.log = log;
    }

    public Path extractDriver(Driver driver, Path path) throws MojoExecutionException {

        String filename = path.toString();
        String filextension = FilenameUtils.getExtension(filename);

        String extractedFilename = FilenameUtils.getName(path.toString()).replaceFirst("\\." + filextension + "$", "");
        Path extractPath = Paths.get(path.getParent().toString(), extractedFilename);

        log.debug("handling type:" + filextension + "(" + filename + ")");

        try {
            switch (filextension) {
                case "bz2":
                    try (FileInputStream fin = new FileInputStream(path.toFile())) {
                        try (BufferedInputStream bin = new BufferedInputStream(fin)) {
                            try (BZip2CompressorInputStream input = new BZip2CompressorInputStream(bin)) {
                                FileUtils.copyInputStreamToFile(input, extractPath.toFile());
                                return extractDriver(driver, extractPath);
                            }
                        }
                    }
                case "tar":
                case "zip":
                    try (FileInputStream fin = new FileInputStream(path.toFile())) {
                        try (BufferedInputStream bin = new BufferedInputStream(fin)) {
                            try (ArchiveInputStream aiStream = new ArchiveStreamFactory().createArchiveInputStream(filextension, bin)) {

                                Path extractToDirectory = Paths.get(tempDirectory, driver.getId());
                                if (extractToDirectory.toFile().exists()) {
                                    FileUtils.deleteDirectory(extractToDirectory.toFile());
                                }
                                extractToDirectory.toFile().mkdirs();

                                Pattern pattern = null;
                                if (null != driver.getFileMatchInside()) {
                                    pattern = Pattern.compile(driver.getFileMatchInside());
                                }

                                ArchiveEntry entry;
                                while ((entry = aiStream.getNextEntry()) != null) {
                                    String name = entry.getName();
                                    if (pattern != null && entry.isDirectory()) {
                                        // ignore
                                    } else if (entry.isDirectory()) {
                                        File directory = new File(extractToDirectory.toFile(), name);
                                        if (!directory.mkdirs()) {
                                            throw new MojoExecutionException("failed to create " + directory);
                                        }
                                    } else {
                                        File file = null;
                                        if (entry instanceof TarArchiveEntry) {
                                            TarArchiveEntry archiveEntry = (TarArchiveEntry) entry;
                                            if (archiveEntry.isFile()) {
                                                file = new File(extractToDirectory.toFile(), name);
                                            }
                                        } else if (entry instanceof ZipArchiveEntry) {
                                            ZipArchiveEntry archiveEntry = (ZipArchiveEntry) entry;
                                            if (!archiveEntry.isUnixSymlink()) {
                                                file = new File(extractToDirectory.toFile(), name);
                                            }
                                        }

                                        if (pattern != null) {
                                            if (pattern.matcher(name).matches()) {
                                                file = new File(extractToDirectory.toFile(), driver.getId());
                                            } else {
                                                file = null;
                                            }
                                        }

                                        if (file != null) {
                                            try (OutputStream out = new FileOutputStream(file)) {
                                                IOUtils.copy(aiStream, out);
                                            }
                                        }
                                    }
                                }
                                return extractToDirectory;
                            } catch (ArchiveException e) {
                                throw new MojoExecutionException(e.getMessage(), e);
                            }
                        }
                    }
                default:
                    throw new MojoExecutionException("unhandled type:" + filextension);
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
