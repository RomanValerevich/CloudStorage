package ru.netology.cloudstorage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import ru.netology.cloudstorage.exception.BadRequestException;
import ru.netology.cloudstorage.model.FileMetadata;
import ru.netology.cloudstorage.repository.FileMetadataRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private FileMetadataRepository metadataRepository;

    private FileServiceImpl fileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Use tempDir as upload directory
        fileService = new FileServiceImpl(metadataRepository, tempDir.toString());
    }

    @Test
    void uploadFile_savesFileAndMetadata() throws IOException {
        String username = "user";
        String filename = "test.txt";
        byte[] content = "hello".getBytes();
        MockMultipartFile multipart = new MockMultipartFile("file", filename, "text/plain", content);

        when(metadataRepository.findByOwnerUsernameAndFilename(username, filename)).thenReturn(Optional.empty());
        ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
        when(metadataRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        fileService.uploadFile(username, filename, multipart);

        FileMetadata saved = captor.getValue();
        assertEquals(filename, saved.getFilename());
        assertEquals(username, saved.getOwnerUsername());
        assertEquals((long) content.length, saved.getSize());
        assertEquals("text/plain", saved.getMimeType());
        assertNotNull(saved.getStoragePath());
        assertTrue(Files.exists(Path.of(saved.getStoragePath())));
    }

    @Test
    void uploadFile_throwsWhenDuplicate() {
        String username = "user";
        String filename = "dup.txt";
        MockMultipartFile multipart = new MockMultipartFile("file", filename, "text/plain", "x".getBytes());

        when(metadataRepository.findByOwnerUsernameAndFilename(username, filename)).thenReturn(Optional.of(new FileMetadata()));

        assertThrows(BadRequestException.class, () -> fileService.uploadFile(username, filename, multipart));
        verify(metadataRepository, never()).save(any());
    }

    @Test
    void uploadFile_throwsOnEmptyInput() {
        String username = "user";
        String filename = "";
        MockMultipartFile multipart = new MockMultipartFile("file", "file", "text/plain", new byte[0]);
        assertThrows(BadRequestException.class, () -> fileService.uploadFile(username, filename, multipart));
    }

    @Test
    void deleteFile_removesPhysicalFileAndMetadata() throws IOException {
        String username = "user";
        String filename = "to-delete.txt";
        Path stored = Files.createTempFile(tempDir, "stored-", ".bin");

        FileMetadata meta = new FileMetadata();
        meta.setOwnerUsername(username);
        meta.setFilename(filename);
        meta.setStoragePath(stored.toString());

        when(metadataRepository.findByOwnerUsernameAndFilename(username, filename)).thenReturn(Optional.of(meta));

        fileService.deleteFile(username, filename);

        assertFalse(Files.exists(stored));
        verify(metadataRepository).delete(meta);
    }

    @Test
    void deleteFile_throwsWhenNotFound() {
        when(metadataRepository.findByOwnerUsernameAndFilename("user", "missing.txt")).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> fileService.deleteFile("user", "missing.txt"));
    }

    @Test
    void downloadFile_returnsResource() throws IOException {
        String username = "user";
        String filename = "download.bin";
        Path stored = Files.createTempFile(tempDir, "stored-", ".bin");
        Files.writeString(stored, "data");

        FileMetadata meta = new FileMetadata();
        meta.setOwnerUsername(username);
        meta.setFilename(filename);
        meta.setStoragePath(stored.toString());

        when(metadataRepository.findByOwnerUsernameAndFilename(username, filename)).thenReturn(Optional.of(meta));

        Resource resource = fileService.downloadFile(username, filename);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    void downloadFile_throwsWhenNotFound() {
        when(metadataRepository.findByOwnerUsernameAndFilename("user", "missing"))
                .thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> fileService.downloadFile("user", "missing"));
    }


    @Test
    void getFilesList_throwsWhenLimitInvalid() {
        assertThrows(BadRequestException.class, () -> fileService.getFilesList("user", 0));
        assertThrows(BadRequestException.class, () -> fileService.getFilesList("user", -1));
    }

    @Test
    void renameFile_updatesName() {
        String username = "user";
        String filename = "old.txt";
        String newName = "new.txt";

        FileMetadata meta = new FileMetadata();
        meta.setOwnerUsername(username);
        meta.setFilename(filename);
        when(metadataRepository.findByOwnerUsernameAndFilename(username, filename)).thenReturn(Optional.of(meta));
        when(metadataRepository.findByOwnerUsernameAndFilename(username, newName)).thenReturn(Optional.empty());

        fileService.renameFile(username, filename, newName);
        assertEquals(newName, meta.getFilename());
        verify(metadataRepository).save(meta);
    }


}
