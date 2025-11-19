package ru.netology.cloudstorage.controller;


import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudstorage.dto.FileListItem;
import ru.netology.cloudstorage.dto.LoginRequest;
import ru.netology.cloudstorage.dto.LoginResponse;
import ru.netology.cloudstorage.dto.RenameRequest;
import ru.netology.cloudstorage.dto.RegisterRequest;
import ru.netology.cloudstorage.service.AuthService;
import ru.netology.cloudstorage.service.FileService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@RestController
@RequestMapping
public class CloudController {
    private final AuthService authService;
    private final FileService fileService;

    public CloudController(AuthService authService, FileService fileService) {
        this.authService = authService;
        this.fileService = fileService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        log.info("Login request for {}", request.getLogin());
        String token = authService.login(request.getLogin(), request.getPassword());
        return ResponseEntity.ok(new LoginResponse(token));
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        log.info("Register request for {}", request.getUsername());
        authService.register(request.getUsername(), request.getPassword(), request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("auth-token") String authToken) {
        log.info("Logout request received");
        authService.logout(authToken);
        return ResponseEntity.ok().build();
    }

    // POST /file (Upload)
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam String filename,
            @RequestPart("file") MultipartFile file
    ) {
        String username = authService.validateTokenAndGetUsername(authToken);
        log.info("Upload file '{}' by '{}'", filename, username);
        fileService.uploadFile(username, filename, file);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/file")
    public ResponseEntity<Void> deleteFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam String filename
    ) {
        String username = authService.validateTokenAndGetUsername(authToken);
        log.info("Delete file '{}' by '{}'", filename, username);
        fileService.deleteFile(username, filename);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> downloadFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam String filename
    ) {
        String username = authService.validateTokenAndGetUsername(authToken);
        log.info("Download file '{}' by '{}'", filename, username);
        Resource fileResource = fileService.downloadFile(username, filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileResource);
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileListItem>> getFilesList(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("limit") int limit
    ) {
        String username = authService.validateTokenAndGetUsername(authToken);
        log.info("Get files list with limit {} by '{}'", limit, username);
        List<FileListItem> files = fileService.getFilesList(username, limit)
                .stream()
                .map(m -> new FileListItem(m.getFilename(), m.getSize()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(files);
    }

    // PUT /file (Rename)
    @PutMapping("/file")
    public ResponseEntity<Void> renameFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam String filename,
            @RequestBody RenameRequest request
    ) {
        String username = authService.validateTokenAndGetUsername(authToken);
        log.info("Rename file '{}' -> '{}' by '{}'", filename, request.getName(), username);
        fileService.renameFile(username, filename, request.getName());
        return ResponseEntity.ok().build();
    }
}
