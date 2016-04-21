package com.github.webdriverextensions;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
    private final File tempDirectory;
    private final Log log;

    public DriverExtractor(File tempDirectory, Log log) {
        this.tempDirectory = tempDirectory;
        this.log = log;
    }

    public Path extractDriver(Driver driver, Path fileToExtract) throws MojoExecutionException {

        log.info("  Extracting " + fileToExtract);
        String fileExtension = FilenameUtils.getExtension(fileToExtract.toString());
        try {
            switch (fileExtension) {
                case "bz2":
                    String extractedFilename = FilenameUtils.getBaseName(fileToExtract.toString());
                    Path extractedFile = Paths.get(tempDirectory.getPath(), extractedFilename);
                    try (FileInputStream fin = new FileInputStream(fileToExtract.toFile())) {
                        try (BufferedInputStream bin = new BufferedInputStream(fin)) {
                            try (BZip2CompressorInputStream input = new BZip2CompressorInputStream(bin)) {
                                FileUtils.copyInputStreamToFile(input, extractedFile.toFile());
                            }
                        }
                    }
                    if (!fileToExtract.toString().contains(Paths.get("webdriverextensions-maven-plugin", "cache").toString())) {
                        FileUtils.forceDelete(fileToExtract.toFile());
                    }
                    return extractDriver(driver, extractedFile);
                case "tar":
                case "zip":
                    Path extractToDirectory = Paths.get(tempDirectory.getPath(), FilenameUtils.getBaseName(fileToExtract.toString()));
                    if (!extractToDirectory.toFile().mkdirs()) {
                        throw new MojoExecutionException("Failed create directory " + extractToDirectory + " for extracted files");
                    }

                    Pattern pattern = null;
                    if (null != driver.getFileMatchInside()) {
                        pattern = Pattern.compile(driver.getFileMatchInside());
                    }

                    try (FileInputStream fin = new FileInputStream(fileToExtract.toFile())) {
                        try (BufferedInputStream bin = new BufferedInputStream(fin)) {
                            try (ArchiveInputStream aiStream = new ArchiveStreamFactory().createArchiveInputStream(fileExtension, bin)) {
                                ArchiveEntry entry;
                                while ((entry = aiStream.getNextEntry()) != null) {
                                    String name = entry.getName();
                                    if (pattern != null && entry.isDirectory()) {
                                        // ignore
                                    } else if (entry.isDirectory()) {
                                        File directory = new File(extractToDirectory.toFile(), name);
                                        if (!directory.mkdirs()) {
                                            throw new MojoExecutionException("Failed create extracted directory " + directory);
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
                                                file = new File(extractToDirectory.toFile(), FilenameUtils.getName(name));
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
                                if (!fileToExtract.toString().contains(Paths.get("webdriverextensions-maven-plugin", "cache").toString())) {
                                    FileUtils.forceDelete(fileToExtract.toFile());
                                }
                                return extractToDirectory;
                            } catch (ArchiveException e) {
                                throw new MojoExecutionException(e.getMessage(), e);
                            }
                        }
                    }
                default:
                    throw new MojoExecutionException("Unsupported file type, file extension: " + fileExtension);
            }
        } catch (MojoExecutionException e) {
            log.info("Failed to extract driver: " + e.getMessage() + "\ndriver: " + driver, e);
            throw e;
        } catch (Exception e) {
            log.info("Failed to extract driver: " + e.getMessage() + "\ndriver: " + driver, e);
            throw new MojoExecutionException("Failed to extract driver: " + e.getMessage() + "\ndriver: " + driver, e);
        }
    }
}
