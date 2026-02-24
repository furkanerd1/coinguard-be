package com.coinguard.common.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String storeFile(MultipartFile file, String subDirectory);

    void deleteFile(String fileUrl);
}
