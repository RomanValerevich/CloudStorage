package ru.netology.cloudstorage.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.cloudstorage.model.entity.FileMetadata;


import java.util.List;
import java.util.Optional;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    Optional<FileMetadata> findByOwnerUsernameAndFilename(String username, String filename);
    List<FileMetadata> findByOwnerUsername(String ownerUsername, Pageable pageable);
}
