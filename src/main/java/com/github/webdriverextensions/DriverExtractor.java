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

    public Path extractDriver(Driver driver, Path path) throws MojoExecutionException {

        String filextension = FilenameUtils.getExtension(path.getFileName().toString());

        try {
            switch (filextension) {
                case "bz2":
                    try (FileInputStream fin = new FileInputStream(path.toFile())) {
                        try (BufferedInputStream bin = new BufferedInputStream(fin)) {
                            try (BZip2CompressorInputStream input = new BZip2CompressorInputStream(bin)) {
                                String extractedFilename = FilenameUtils.getBaseName(path.toString());
                                Path extractedPath = Paths.get(tempDirectory.getAbsolutePath(), extractedFilename);
                                log.info("  Extracting " + path);
                                FileUtils.copyInputStreamToFile(input, extractedPath.toFile());
                                return extractDriver(driver, extractedPath);
                            }
                        }
                    }
                case "tar":
                case "zip":
                    try (FileInputStream fin = new FileInputStream(path.toFile())) {
                        try (BufferedInputStream bin = new BufferedInputStream(fin)) {
                            try (ArchiveInputStream aiStream = new ArchiveStreamFactory().createArchiveInputStream(filextension, bin)) {

                                Path extractToDirectory = Paths.get(tempDirectory.getPath());
                                log.info("  Extracting " + path);
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
                                throw new MojoExecutionException(e.getMessage() + "\ndriver:\n" + driver, e);
                            }
                        }
                    }
                default:
                    throw new MojoExecutionException("Unsupported file type, file extension: " + filextension + "\ndriver:\n" + driver);
            }
        } catch (MojoExecutionException e) {
            log.info("Failed to extract driver: " + e.getMessage() + "\ndriver:\n" + driver, e);
            throw e;
        } catch (Exception e) {
            log.info("Failed to extract driver: " + e.getMessage() + "\ndriver:\n" + driver, e);
            throw new MojoExecutionException("Failed to extract driver: " + e.getMessage() + "\ndriver:\n" + driver, e);
        }
    }
}
