package com.shiptrack.shiptrack_pro.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    String uploadFile(MultipartFile file);
}