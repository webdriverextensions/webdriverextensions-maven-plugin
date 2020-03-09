package com.github.webdriverextensions.newversion;

import com.github.webdriverextensions.LoggedTemporaryFolder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class FileExtractorImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new LoggedTemporaryFolder();

    private Path toDirectory;

    @Before
    public void setUp() throws Exception {
        toDirectory = temporaryFolder.newFolder("to-directory").toPath();
    }

    @Test
    public void extractFile_should_extract_bz2_file_containing_a_single_file() throws Exception {
        // Given
        Path singleFileZip = Paths.get("src/test/resources/file-extractor-test-data/single-file.bz2");
        FileExtractorImpl fileExtractor = new FileExtractorImpl(null);

        // When
        fileExtractor.extractFile(singleFileZip, toDirectory);

        // Then
        assertThat(toDirectory.toFile().listFiles().length, is(1));
        assertThat(toDirectory.resolve("single-file").toFile().exists(), is(true));
    }


    @Test
    public void extractFile_should_extract_gz_file_containing_a_single_file() throws Exception {
        // Given
        Path singleFileZip = Paths.get("src/test/resources/file-extractor-test-data/single-file.gz");
        FileExtractorImpl fileExtractor = new FileExtractorImpl(null);

        // When
        fileExtractor.extractFile(singleFileZip, toDirectory);

        // Then
        assertThat(toDirectory.toFile().listFiles().length, is(1));
        assertThat(toDirectory.resolve("single-file").toFile().exists(), is(true));
    }


    @Test
    public void extractFile_should_extract_tar_file_containing_a_single_file() throws Exception {
        // Given
        Path singleFileZip = Paths.get("src/test/resources/file-extractor-test-data/single-file.tar");
        FileExtractorImpl fileExtractor = new FileExtractorImpl(null);

        // When
        fileExtractor.extractFile(singleFileZip, toDirectory);

        // Then
        assertThat(toDirectory.toFile().listFiles().length, is(1));
        assertThat(toDirectory.resolve("a-file.txt").toFile().exists(), is(true));
    }

    @Test
    public void extractFile_should_extract_tar_file_containing_directory_structure() throws Exception {
        // Given
        Path singleFileZip = Paths.get("src/test/resources/file-extractor-test-data/directories-and-files.tar");
        FileExtractorImpl fileExtractor = new FileExtractorImpl(null);

        // When
        fileExtractor.extractFile(singleFileZip, toDirectory);

        // Then
        assertThat(toDirectory.toFile().listFiles().length, is(2));
        assertThat(toDirectory.resolve(Paths.get("a-directory")).toFile().exists(), is(true));
        assertThat(toDirectory.resolve(Paths.get("a-file.txt")).toFile().exists(), is(true));

        assertThat(toDirectory.resolve(Paths.get("a-directory")).toFile().listFiles().length, is(3));
        assertThat(toDirectory.resolve(Paths.get("a-directory", "another-directory")).toFile().exists(), is(true));
        assertThat(toDirectory.resolve(Paths.get("a-directory", "a-file-in-directory.txt")).toFile().exists(), is(true));
        assertThat(toDirectory.resolve(Paths.get("a-directory", "another-file-in-directory.txt")).toFile().exists(), is(true));

        assertThat(toDirectory.resolve(Paths.get("a-directory", "another-directory")).toFile().listFiles().length, is(1));
        assertThat(toDirectory.resolve(Paths.get("a-directory", "another-directory", "a-file-in-another-directory.txt")).toFile().exists(), is(true));
    }

    @Test
    public void extractFile_should_extract_tar_file_with_extract_pattern() throws Exception {
        // Given
        Path singleFileZip = Paths.get("src/test/resources/file-extractor-test-data/directories-and-files.tar");
        FileExtractorImpl fileExtractor = new FileExtractorImpl(".*a-file-in-directory.txt$");

        // When
        fileExtractor.extractFile(singleFileZip, toDirectory);

        // Then
        assertThat(toDirectory.toFile().listFiles().length, is(1));
        assertThat(toDirectory.resolve("a-file-in-directory.txt").toFile().exists(), is(true));
    }


    @Test
    public void extractFile_should_extract_tar_bz2_file_containing_a_single_file() throws Exception {
        // Given
        Path singleFileZip = Paths.get("src/test/resources/file-extractor-test-data/single-file.tar.bz2");
        FileExtractorImpl fileExtractor = new FileExtractorImpl(null);

        // When
        fileExtractor.extractFile(singleFileZip, toDirectory);

        // Then
        assertThat(toDirectory.toFile().listFiles().length, is(1));
        assertThat(toDirectory.resolve("a-file.txt").toFile().exists(), is(true));
    }

    @Test
    public void extractFile_should_extract_tar_bz2_file_containing_directory_structure() throws Exception {
        // Given
        Path singleFileZip = Paths.get("src/test/resources/file-extractor-test-data/directories-and-files.tar.bz2");
        FileExtractorImpl fileExtractor = new FileExtractorImpl(null);

        // When
        fileExtractor.extractFile(singleFileZip, toDirectory);

        // Then
        assertThat(toDirectory.toFile().listFiles().length, is(2));
        assertThat(toDirectory.resolve(Paths.get("a-directory")).toFile().exists(), is(true));
        assertThat(toDirectory.resolve(Paths.get("a-file.txt")).toFile().exists(), is(true));

        assertThat(toDirectory.resolve(Paths.get("a-directory")).toFile().listFiles().length, is(3));
        assertThat(toDirectory.resolve(Paths.get("a-directory", "another-directory")).toFile().exists(), is(true));
        assertThat(toDirectory.resolve(Paths.get("a-directory", "a-file-in-directory.txt")).toFile().exists(), is(true));
        assertThat(toDirectory.resolve(Paths.get("a-directory", "another-file-in-directory.txt")).toFile().exists(), is(true));

        assertThat(toDirectory.resolve(Paths.get("a-directory", "another-directory")).toFile().listFiles().length, is(1));
        assertThat(toDirectory.resolve(Paths.get("a-directory", "another-directory", "a-file-in-another-directory.txt")).toFile().exists(), is(true));
    }

    @Test
    public void extractFile_should_extract_tar_bz2_file_with_extract_pattern() throws Exception {
        // Given
        Path singleFileZip = Paths.get("src/test/resources/file-extractor-test-data/directories-and-files.tar.bz2");
        FileExtractorImpl fileExtractor = new FileExtractorImpl(".*a-file-in-directory.txt$");

        // When
        fileExtractor.extractFile(singleFileZip, toDirectory);

        // Then
        assertThat(toDirectory.toFile().listFiles().length, is(1));
        assertThat(toDirectory.resolve("a-file-in-directory.txt").toFile().exists(), is(true));
    }


    @Test
    public void extractFile_should_extract_tar_gz_file_containing_a_single_file() throws Exception {
        // Given
        Path singleFileZip = Paths.get("src/test/resources/file-extractor-test-data/single-file.tar.gz");
        FileExtractorImpl fileExtractor = new FileExtractorImpl(null);

        // When
        fileExtractor.extractFile(singleFileZip, toDirectory);

        // Then
        assertThat(toDirectory.toFile().listFiles().length, is(1));
        assertThat(toDirectory.resolve("a-file.txt").toFile().exists(), is(true));
    }

    @Test
    public void extractFile_should_extract_tar_gz_file_containing_directory_structure() throws Exception {
        // Given
        Path singleFileZip = Paths.get("src/test/resources/file-extractor-test-data/directories-and-files.tar.gz");
        FileExtractorImpl fileExtractor = new FileExtractorImpl(null);

        // When
        fileExtractor.extractFile(singleFileZip, toDirectory);

        // Then
        assertThat(toDirectory.toFile().listFiles().length, is(2));
        assertThat(toDirectory.resolve(Paths.get("a-directory")).toFile().exists(), is(true));
        assertThat(toDirectory.resolve(Paths.get("a-file.txt")).toFile().exists(), is(true));

        assertThat(toDirectory.resolve(Paths.get("a-directory")).toFile().listFiles().length, is(3));
        assertThat(toDirectory.resolve(Paths.get("a-directory", "another-directory")).toFile().exists(), is(true));
        assertThat(toDirectory.resolve(Paths.get("a-directory", "a-file-in-directory.txt")).toFile().exists(), is(true));
        assertThat(toDirectory.resolve(Paths.get("a-directory", "another-file-in-directory.txt")).toFile().exists(), is(true));

        assertThat(toDirectory.resolve(Paths.get("a-directory", "another-directory")).toFile().listFiles().length, is(1));
        assertThat(toDirectory.resolve(Paths.get("a-directory", "another-directory", "a-file-in-another-directory.txt")).toFile().exists(), is(true));
    }

    @Test
    public void extractFile_should_extract_tar_gz_file_with_extract_pattern() throws Exception {
        // Given
        Path singleFileZip = Paths.get("src/test/resources/file-extractor-test-data/directories-and-files.tar.gz");
        FileExtractorImpl fileExtractor = new FileExtractorImpl(".*a-file-in-directory.txt$");

        // When
        fileExtractor.extractFile(singleFileZip, toDirectory);

        // Then
        assertThat(toDirectory.toFile().listFiles().length, is(1));
        assertThat(toDirectory.resolve("a-file-in-directory.txt").toFile().exists(), is(true));
    }


    @Test
    public void extractFile_should_extract_zip_file_containing_a_single_file() throws Exception {
        // Given
        Path singleFileZip = Paths.get("src/test/resources/file-extractor-test-data/single-file.zip");
        FileExtractorImpl fileExtractor = new FileExtractorImpl(null);

        // When
        fileExtractor.extractFile(singleFileZip, toDirectory);

        // Then
        assertThat(toDirectory.toFile().listFiles().length, is(1));
        assertThat(toDirectory.resolve("a-file.txt").toFile().exists(), is(true));
    }

    @Test
    public void extractFile_should_extract_zip_file_containing_directory_structure() throws Exception {
        // Given
        Path singleFileZip = Paths.get("src/test/resources/file-extractor-test-data/directories-and-files.zip");
        FileExtractorImpl fileExtractor = new FileExtractorImpl(null);

        // When
        fileExtractor.extractFile(singleFileZip, toDirectory);

        // Then
        assertThat(toDirectory.toFile().listFiles().length, is(2));
        assertThat(toDirectory.resolve(Paths.get("a-directory")).toFile().exists(), is(true));
        assertThat(toDirectory.resolve(Paths.get("a-file.txt")).toFile().exists(), is(true));

        assertThat(toDirectory.resolve(Paths.get("a-directory")).toFile().listFiles().length, is(3));
        assertThat(toDirectory.resolve(Paths.get("a-directory", "another-directory")).toFile().exists(), is(true));
        assertThat(toDirectory.resolve(Paths.get("a-directory", "a-file-in-directory.txt")).toFile().exists(), is(true));
        assertThat(toDirectory.resolve(Paths.get("a-directory", "another-file-in-directory.txt")).toFile().exists(), is(true));

        assertThat(toDirectory.resolve(Paths.get("a-directory", "another-directory")).toFile().listFiles().length, is(1));
        assertThat(toDirectory.resolve(Paths.get("a-directory", "another-directory", "a-file-in-another-directory.txt")).toFile().exists(), is(true));
    }

    @Test
    public void extractFile_should_extract_zip_file_with_extract_pattern() throws Exception {
        // Given
        Path singleFileZip = Paths.get("src/test/resources/file-extractor-test-data/directories-and-files.zip");
        FileExtractorImpl fileExtractor = new FileExtractorImpl(".*a-file-in-directory.txt$");

        // When
        fileExtractor.extractFile(singleFileZip, toDirectory);

        // Then
        assertThat(toDirectory.toFile().listFiles().length, is(1));
        assertThat(toDirectory.resolve("a-file-in-directory.txt").toFile().exists(), is(true));
    }

    @Test
    public void test_unZipOldMS() throws Exception {
        Path aNewZip = Paths.get("src/test/resources/file-extractor-test-data/edge80.zip");
        FileExtractorImpl fileExtractor = new FileExtractorImpl(null);
        fileExtractor.extractFile(aNewZip,toDirectory);
        assert(true);
    }

    @Test
    public void test_unZipNewMS() throws Exception {
        Path aNewZip = Paths.get("src/test/resources/file-extractor-test-data/edge82.zip");
        FileExtractorImpl fileExtractor = new FileExtractorImpl(null);
        fileExtractor.extractFile(aNewZip,toDirectory);
        assert(true);
    }
}
