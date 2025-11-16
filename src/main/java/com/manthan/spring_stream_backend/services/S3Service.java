package com.manthan.spring_stream_backend.services;

import org.springframework.web.multipart.MultipartFile;

public interface S3Service {
    String uploadFile(MultipartFile file);
    void deleteFile(String fileUrl);
    long getFileSize(String key);
    byte[] getPartialFile(String key, long start, long end);
}