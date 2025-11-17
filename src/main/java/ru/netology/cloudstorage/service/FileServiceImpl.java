package ru.netology.cloudstorage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudstorage.exception.BadRequestException;
import ru.netology.cloudstorage.exception.InternalServerErrorException;
import ru.netology.cloudstorage.model.dto.FileListItem;
import ru.netology.cloudstorage.model.entity.FileMetadata;
import ru.netology.cloudstorage.repository.FileMetadataRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileServiceImpl implements FileService {
    private final FileMetadataRepository metadataRepository;
    private final Path fileStorageLocation;

    public FileServiceImpl(FileMetadataRepository metadataRepository,
                           @Value("${app.file.upload-dir:./uploads}") String uploadDir) {
        this.metadataRepository = metadataRepository;
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new InternalServerErrorException("Error creating storage directory.", ex);
        }
    }

    @Transactional
    @Override
    public void uploadFile(String username, String filename, MultipartFile file) {
        if (file.isEmpty() || filename == null || filename.isEmpty()) {
            throw new BadRequestException("Error input data: File or filename is missing.");
        }

        if (metadataRepository.findByOwnerUsernameAndFilename(username, filename).isPresent()) {
            throw new BadRequestException("File with this name already exists.");
        }

        try {
            String storedFilename = username + "_" + UUID.randomUUID().toString();
            Path targetLocation = this.fileStorageLocation.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            FileMetadata metadata = new FileMetadata();
            metadata.setFilename(filename);
            metadata.setOwnerUsername(username);
            metadata.setSize(file.getSize());
            metadata.setStoragePath(targetLocation.toString());
            metadata.setMimeType(file.getContentType());

            metadataRepository.save(metadata);

        } catch (IOException ex) {
            throw new InternalServerErrorException("Error uploading file: Failed to store file.", ex);
        }
    }

    @Transactional
    @Override
    public void deleteFile(String username, String filename) {
        FileMetadata metadata = metadataRepository.findByOwnerUsernameAndFilename(username, filename)
                .orElseThrow(() -> new BadRequestException("Error input data: File not found or access denied."));

        Path filePath = Paths.get(metadata.getStoragePath());
        try {
            Files.delete(filePath);
        } catch (IOException ex) {
            throw new InternalServerErrorException("Error deleting file: Failed to delete physical file.", ex);
        }

        metadataRepository.delete(metadata);
    }

    @Override
    public Resource downloadFile(String username, String filename) {
        FileMetadata metadata = metadataRepository.findByOwnerUsernameAndFilename(username, filename)
                .orElseThrow(() -> new BadRequestException("Error input data: File not found or access denied."));

        try {
            Path filePath = Paths.get(metadata.getStoragePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new InternalServerErrorException("Error downloading file: File not readable on server.");
            }
        } catch (MalformedURLException ex) {
            throw new InternalServerErrorException("Error downloading file: Invalid file path.", ex);
        }
    }
    @Override
    public List<FileListItem> getFilesList(String username, int limit) {
        if (limit <= 0) {
            throw new BadRequestException("Error input data: limit must be > 0.");
        }

        return metadataRepository.findByOwnerUsername(username, PageRequest.of(0, limit))
                .stream()
                .map(m -> new FileListItem(m.getFilename(), m.getSize()))
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void renameFile(String username, String filename, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new BadRequestException("Error input data: new filename is missing.");
        }

        FileMetadata metadata = metadataRepository.findByOwnerUsernameAndFilename(username, filename)
                .orElseThrow(() -> new BadRequestException("Error input data: File not found or access denied."));

        if (metadataRepository.findByOwnerUsernameAndFilename(username, newName).isPresent()) {
            throw new BadRequestException("File with this name already exists.");
        }

        metadata.setFilename(newName);
        metadataRepository.save(metadata);
    }

}
