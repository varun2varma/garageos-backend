package com.garageos.modules.media.service;

import com.google.api.services.drive.model.File;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface GoogleDriveFileService {

    File uploadFile(
            MultipartFile multipartFile,
            String fileName,
            String parentFolderId
    ) throws GeneralSecurityException, IOException;
}