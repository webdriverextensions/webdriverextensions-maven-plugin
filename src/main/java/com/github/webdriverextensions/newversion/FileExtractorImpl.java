package com.github.webdriverextensions.newversion;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.regex.Pattern;

public class FileExtractorImpl implements FileExtractor {

    private final Pattern extractPattern;

    private final PathMatcher TAR_BZ2 = FileSystems.getDefault().getPathMatcher("glob:**.tar.bz2");
    private final PathMatcher TAR_GZ = FileSystems.getDefault().getPathMatcher("glob:**.tar.gz");
    private final PathMatcher BZ2 = FileSystems.getDefault().getPathMatcher("glob:**.bz2");
    private final PathMatcher GZ = FileSystems.getDefault().getPathMatcher("glob:**.gz");
    private final PathMatcher TAR = FileSystems.getDefault().getPathMatcher("glob:**.tar");
    private final PathMatcher ZIP = FileSystems.getDefault().getPathMatcher("glob:**.zip");

    public FileExtractorImpl(String extractPattern) {
        this.extractPattern = extractPattern == null ? null : Pattern.compile(extractPattern);
    }

    @Override
    public boolean isExtractable(Path file) {
        return TAR_BZ2.matches(file) ||
               TAR_GZ.matches(file) ||
               BZ2.matches(file) ||
               GZ.matches(file) ||
               TAR.matches(file) ||
               ZIP.matches(file);
    }

    @Override
    public void extractFile(Path file, Path toDirectory) {
        try {
            if (TAR_BZ2.matches(file)) {
                extractTarBz2File(file, toDirectory);
            } else if (TAR_GZ.matches(file)) {
                extractTarGzFile(file, toDirectory);
            } else if (BZ2.matches(file)) {
                extractBz2File(file, toDirectory);
            } else if (GZ.matches(file)) {
                extractGzFile(file, toDirectory);
            } else if (TAR.matches(file)) {
                extractTarFile(file, toDirectory);
            } else if (ZIP.matches(file)) {
                extractZipFile(file, toDirectory);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void extractBz2File(Path file, Path toDirectory) throws IOException {
        String extractedFilename = FilenameUtils.getBaseName(file.toString());
        Path fileToExtract = toDirectory.resolve(extractedFilename);
        try (FileInputStream fin = new FileInputStream(file.toFile())) {
            try (BufferedInputStream bin = new BufferedInputStream(fin)) {
                try (BZip2CompressorInputStream bzip2Archive = new BZip2CompressorInputStream(bin)) {
                    Files.copy(bzip2Archive, fileToExtract);
                }
            }
        }
    }

    private void extractGzFile(Path file, Path toDirectory) throws IOException {
        String extractedFilename = FilenameUtils.getBaseName(file.toString());
        Path fileToExtract = toDirectory.resolve(extractedFilename);
        try (FileInputStream fin = new FileInputStream(file.toFile())) {
            try (BufferedInputStream bin = new BufferedInputStream(fin)) {
                try (GzipCompressorInputStream gzipArchive = new GzipCompressorInputStream(bin)) {
                    Files.copy(gzipArchive, fileToExtract);
                }
            }
        }
    }

    private void extractTarFile(Path file, Path toDirectory) throws IOException {
        Files.createDirectories(toDirectory);
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            try (BufferedInputStream bis = new BufferedInputStream(fis)) {
                try (TarArchiveInputStream tarArchive = new TarArchiveInputStream(bis)) {
                    extractTar(toDirectory, tarArchive);
                }
            }
        }
    }

    private void extractTarBz2File(Path file, Path toDirectory) throws IOException {
        Files.createDirectories(toDirectory);
        try (FileInputStream fin = new FileInputStream(file.toFile())) {
            try (BufferedInputStream bin = new BufferedInputStream(fin)) {
                try (BZip2CompressorInputStream bzip2Archive = new BZip2CompressorInputStream(bin)) {
                    try (TarArchiveInputStream tarArchive = new TarArchiveInputStream(bzip2Archive)) {
                        extractTar(toDirectory, tarArchive);
                    }
                }
            }
        }
    }

    private void extractTar(Path toDirectory, TarArchiveInputStream tarArchive) throws IOException {
        for (TarArchiveEntry tarEntry = tarArchive.getNextTarEntry(); tarEntry != null; tarEntry = tarArchive.getNextTarEntry()) {
            if (tarEntry.isDirectory()) {
                if (extractPattern != null) {
                    continue;
                }
                Files.createDirectories(toDirectory.resolve(tarEntry.getName()));
            } else {
                if (tarEntry.isSymbolicLink()) {
                    continue;
                }
                extractPattern(toDirectory, tarArchive, tarEntry);
            }
        }
    }

    private void extractPattern(Path toDirectory, TarArchiveInputStream tarArchive, TarArchiveEntry tarEntry) throws IOException {
        if (extractPattern != null) {
            if (!extractPattern.matcher(tarEntry.getName()).matches()) {
                return;
            }
            Path filename = Paths.get(tarEntry.getName()).getFileName();
            Path fileToExtract = toDirectory.resolve(filename);
            Files.copy(tarArchive, fileToExtract);
        } else {
            Path fileToExtract = toDirectory.resolve(tarEntry.getName());
            Files.copy(tarArchive, fileToExtract);
        }
    }

    private void extractTarGzFile(Path file, Path toDirectory) throws IOException {
        Files.createDirectories(toDirectory);
        try (FileInputStream fin = new FileInputStream(file.toFile())) {
            try (BufferedInputStream bin = new BufferedInputStream(fin)) {
                try (GzipCompressorInputStream gzipArchive = new GzipCompressorInputStream(bin)) {
                    try (TarArchiveInputStream tarArchive = new TarArchiveInputStream(gzipArchive)) {
                        extractTar(toDirectory, tarArchive);
                    }
                }
            }
        }
    }

    private void extractZipFile(Path file, Path toDirectory) throws IOException {
        Files.createDirectories(toDirectory);
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            try (BufferedInputStream bis = new BufferedInputStream(fis)) {
                try (ZipArchiveInputStream zipArchive = new ZipArchiveInputStream(bis)) {
                    for (ZipArchiveEntry zipEntry = zipArchive.getNextZipEntry(); zipEntry != null; zipEntry = zipArchive.getNextZipEntry()) {

                        if (zipEntry.isDirectory()) {
                            if (extractPattern != null) {
                                continue;
                            }
                            Files.createDirectories(toDirectory.resolve(zipEntry.getName()));
                        } else {
                            if (zipEntry.isUnixSymlink()) {
                                continue;
                            }
                            if (extractPattern != null) {
                                if (!extractPattern.matcher(zipEntry.getName()).matches()) {
                                    continue;
                                }
                                Path filename = Paths.get(zipEntry.getName()).getFileName();
                                Path fileToExtract = toDirectory.resolve(filename);
                                Files.copy(zipArchive, fileToExtract);
                            } else {
                                Path fileToExtract = toDirectory.resolve(zipEntry.getName());
                                Files.copy(zipArchive, fileToExtract);
                            }
                        }
                    }
                }
            }
        }
    }
}
