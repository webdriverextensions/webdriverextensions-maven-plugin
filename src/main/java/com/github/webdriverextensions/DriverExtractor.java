package com.github.webdriverextensions;

import static com.github.webdriverextensions.Utils.quote;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;

class DriverExtractor {
    private final InstallDriversMojo mojo;

    DriverExtractor(InstallDriversMojo mojo) {
        this.mojo = mojo;
    }

    Path extractDriver(Driver driver, Path fileToExtract) throws MojoExecutionException {
        String fileExtension = FilenameUtils.getExtension(fileToExtract.toString());
        try {
            switch (fileExtension) {
                case "bz2":
                    mojo.getLog().info("  Extracting " + quote(fileToExtract) + " to temp folder");
                    String extractedFilename = FilenameUtils.getBaseName(fileToExtract.toString());
                    Path extractedFile = Paths.get(mojo.tempDirectory.getPath(), extractedFilename);
                    try (FileInputStream fin = new FileInputStream(fileToExtract.toFile())) {
                        try (BufferedInputStream bin = new BufferedInputStream(fin)) {
                            try (BZip2CompressorInputStream input = new BZip2CompressorInputStream(bin)) {
                                FileUtils.copyInputStreamToFile(input, extractedFile.toFile());
                            }
                        }
                    }
                    decideToDeleteFile(fileToExtract);
                    return extractDriver(driver, extractedFile);
                case "gz":
                    mojo.getLog().info("  Extracting " + quote(fileToExtract) + " to temp folder");
                    String extractedFromGzFilename = FilenameUtils.getBaseName(fileToExtract.toString());
                    Path extractedFromGzFile = Paths.get(mojo.tempDirectory.getPath(), extractedFromGzFilename);

                    try (FileInputStream fin = new FileInputStream(fileToExtract.toFile())) {
                        try (BufferedInputStream bin = new BufferedInputStream(fin)) {
                            try (GzipCompressorInputStream input = new GzipCompressorInputStream(bin)) {

                                File file = new File(extractedFromGzFile.toFile(), extractedFromGzFilename);
                                if (!extractedFromGzFile.toFile().mkdirs()) {
                                    throw new RuntimeException("Failed create directory " + quote(extractedFromGzFile) + " for extracted files");
                                }

                                try (FileOutputStream out = new FileOutputStream(file)) {
                                    IOUtils.copy(input, out);
                                }
                            }
                        }
                    }
                    decideToDeleteFile(fileToExtract);
                    return extractedFromGzFile;
                case "tar":
                case "zip":
                    mojo.getLog().info("  Extracting " + quote(fileToExtract) + " to temp folder");
                    Path extractToDirectory = Paths.get(mojo.tempDirectory.getPath(), FilenameUtils.getBaseName(fileToExtract.toString()));
                    if (!extractToDirectory.toFile().mkdirs()) {
                        throw new RuntimeException("Failed create directory " + quote(extractToDirectory) + " for extracted files");
                    }

                    Pattern pattern = null;
                    if (null != driver.getFileMatchInside()) {
                        pattern = Pattern.compile(driver.getFileMatchInside());
                    }

                    try (FileInputStream fin = new FileInputStream(fileToExtract.toFile())) {
                        try (BufferedInputStream bin = new BufferedInputStream(fin)) {
                            try (ArchiveInputStream aiStream = new ArchiveStreamFactory().createArchiveInputStream(
                                    fileExtension,
                                    bin)) {
                                ArchiveEntry entry;
                                while ((entry = aiStream.getNextEntry()) != null) {
                                    String name = entry.getName();
                                    if (pattern != null && entry.isDirectory()) {
                                        // ignore
                                    } else if (entry.isDirectory()) {
                                        File directory = new File(extractToDirectory.toFile(), name);
                                        if (!directory.mkdirs()) {
                                            throw new RuntimeException("Failed create extracted directory " + quote(
                                                    directory));
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
                                                file = new File(extractToDirectory.toFile(),
                                                        FilenameUtils.getName(name));
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
                            }
                        }
                    }
                    decideToDeleteFile(fileToExtract);
                    return extractToDirectory;
                case "exe":
                case "":
                    if (mojo.keepDownloadedWebdrivers) {
                        mojo.getLog().info("  Copying " + quote(fileToExtract) + " to temp folder");
                        Path copyToDirectory = Paths.get(mojo.tempDirectory.getPath(), FilenameUtils.getName(fileToExtract.toString()));
                        FileUtils.copyFile(fileToExtract.toFile(), copyToDirectory.toFile());
                        decideToDeleteFile(fileToExtract);
                        return mojo.tempDirectory.toPath();
                    } else {
                        return fileToExtract;
                    }
                default:
                    throw new UnsupportedOperationException("Unsupported extraction type, file extension: " + fileExtension);
            }
        } catch (Exception e) {
            throw new InstallDriversMojoExecutionException("Failed to extract driver from " + quote(fileToExtract) + " cause of " + e
                    .getMessage(), e, mojo, driver);
        }
    }

    private void decideToDeleteFile(Path fileToExtract) throws IOException {
        Path cache = Paths.get("webdriverextensions-maven-plugin", "cache");
        boolean isNotInCache = !fileToExtract.toString().contains(cache.toString());
        if (isNotInCache && !mojo.keepDownloadedWebdrivers) {
            FileUtils.forceDelete(fileToExtract.toFile());
        }
    }
}
