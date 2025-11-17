package ru.netology.cloudstorage.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudstorage.model.dto.FileListItem;

import java.util.List;

public interface FileService {
    void uploadFile(String username, String filename, MultipartFile file);
    void deleteFile(String username, String filename);
    Resource downloadFile(String username, String filename);
    List<FileListItem> getFilesList(String username, int limit);
    void renameFile(String username, String filename, String newName);
}
