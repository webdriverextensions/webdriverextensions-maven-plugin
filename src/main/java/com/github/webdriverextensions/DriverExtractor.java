package com.github.webdriverextensions;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class DriverExtractor {
    private final InstallDriversMojo mojo;

    public DriverExtractor(InstallDriversMojo mojo) {
        this.mojo = mojo;
    }

    public Path extractDriver(Driver driver, Path fileToExtract) throws MojoExecutionException {

        mojo.getLog().info("  Extracting " + fileToExtract + " to temp folder");
        String fileExtension = FilenameUtils.getExtension(fileToExtract.toString());
        try {
            switch (fileExtension) {
                case "bz2":
                    String extractedFilename = FilenameUtils.getBaseName(fileToExtract.toString());
                    Path extractedFile = Paths.get(mojo.tempDirectory.getPath(), extractedFilename);
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
                    Path extractToDirectory = Paths.get(mojo.tempDirectory.getPath(), FilenameUtils.getBaseName(fileToExtract.toString()));
                    if (!extractToDirectory.toFile().mkdirs()) {
                        throw new RuntimeException("Failed create directory " + extractToDirectory + " for extracted files");
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
                                            throw new RuntimeException("Failed create extracted directory " + directory);
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
                            }
                        }
                    }
                default:
                    throw new UnsupportedOperationException("Unsupported extraction type, file extension: " + fileExtension);
            }
        } catch (Exception e) {
            throw new InstallDriversMojoExecutionException("Failed to extract driver from " + Utils.quote(fileToExtract) + " cause of " + e.getMessage(), e, mojo, driver);
        }
    }
}
