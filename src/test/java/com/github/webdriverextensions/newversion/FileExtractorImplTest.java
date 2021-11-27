package com.github.webdriverextensions.newversion;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class FileExtractorImplTest {

    private Path getTestArchive(final String archiveName) {
        return Paths.get("src/test/resources/file-extractor-test-data", archiveName);
    }

    @DisplayName("extractFile should extract archive containing a single file")
    @ParameterizedTest(name = "{index} ==> for file type/name {0}")
    @ValueSource(strings = {"single-file.bz2", "single-file.gz", "single-file.tar", "single-file.tar.bz2", "single-file.zip", "single-file.tar.gz"})
    void extractFile_should_extract_file_containing_a_single_file(final String archiveName, @TempDir final Path toDirectory) throws Exception {
        // Given
        Path singleFileZip = getTestArchive(archiveName);
        FileExtractorImpl fileExtractor = new FileExtractorImpl(null);

        // When
        fileExtractor.extractFile(singleFileZip, toDirectory);

        // Then
        assertThat(toDirectory.toFile().list()).containsExactly("single-file");
        assertThat(toDirectory.resolve("single-file")).isNotEmptyFile();
    }

    @DisplayName("extractFile should extract archive containing directory structure")
    @ParameterizedTest(name = "{index} ==> for file type/name {0}")
    @ValueSource(strings = {"directories-and-files.tar", "directories-and-files.tar.bz2", "directories-and-files.tar.gz", "directories-and-files.zip"})
    void extractFile_should_extract_file_containing_directory_structure(final String archiveName, @TempDir final Path toDirectory) throws Exception {
        // Given
        Path singleFileZip = getTestArchive(archiveName);
        FileExtractorImpl fileExtractor = new FileExtractorImpl(null);

        // When
        fileExtractor.extractFile(singleFileZip, toDirectory);

        // Then
        assertThat(toDirectory.toFile().list()).containsExactlyInAnyOrder("a-file.txt", "a-directory");
        assertThat(toDirectory.resolve("a-file.txt")).isNotEmptyFile();
        assertThat(toDirectory.resolve("a-directory")).isNotEmptyDirectory().satisfies(dir -> {
            assertThat(dir.toFile().list()).containsExactlyInAnyOrder("another-directory", "a-file-in-directory.txt", "another-file-in-directory.txt");
        });
        assertThat(toDirectory.resolve("a-directory").resolve("another-directory")).isNotEmptyDirectory().satisfies(dir -> {
            assertThat(dir.toFile().list()).containsExactly("a-file-in-another-directory.txt");
        });
    }

    @DisplayName("extractFile should extract archive with extract pattern")
    @ParameterizedTest(name = "{index} ==> for file type/name {0}")
    @ValueSource(strings = {"directories-and-files.tar", "directories-and-files.tar.bz2", "directories-and-files.tar.gz", "directories-and-files.zip"})
    void extractFile_should_extract_file_with_extract_pattern(final String archiveName, @TempDir final Path toDirectory) throws Exception {
        // Given
        Path singleFileZip = getTestArchive(archiveName);
        FileExtractorImpl fileExtractor = new FileExtractorImpl(".*a-file-in-directory.txt$");

        // When
        fileExtractor.extractFile(singleFileZip, toDirectory);

        // Then
        assertThat(toDirectory.toFile().list()).containsExactly("a-file-in-directory.txt");
        assertThat(toDirectory.resolve("a-file-in-directory.txt")).isNotEmptyFile();
    }

    @ParameterizedTest(name = "{index} ==> for file type/name {0}")
    @ValueSource(strings = {"path-traversal.tar", "path-traversal.zip"})
    void zipSlipShouldThrowException(final String archiveName, @TempDir final Path toDirectory) throws Exception {
        final FileExtractorImpl fileExtractor = new FileExtractorImpl(null);
        final Path extractDir = toDirectory.resolve("sub1").resolve("sub2");
        Files.createDirectories(extractDir);
        fileExtractor.extractFile(getTestArchive(archiveName), extractDir);
        // temporaryFolder must only contain "sub1" otherwise it would also contain "single-file" and "directories-and-files"
        assertThat(toDirectory.toFile().list()).containsExactly("sub1");
    }

    @ParameterizedTest(name = "{index} ==> for file type/name {0}")
    @ValueSource(strings = {"directory-without-D-attribute.zip", "directory-without-D-attribute.tar"})
    void extractZipWithDirectoryWithoutDAttribShouldSucceed(final String archiveName, @TempDir final Path toDirectory) throws Exception {
        final FileExtractorImpl fileExtractor = new FileExtractorImpl(null);
        assertThatCode(() -> fileExtractor.extractFile(getTestArchive(archiveName), toDirectory))
                .doesNotThrowAnyException();
        assertThat(toDirectory.resolve("directories-and-files").resolve("a-file.txt")).isRegularFile();
    }
}
