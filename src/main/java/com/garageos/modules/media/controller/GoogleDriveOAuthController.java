package com.garageos.modules.media.controller;

import com.garageos.modules.media.service.GoogleDriveClientService;
import com.garageos.modules.media.service.GoogleDriveFolderService;
import com.garageos.modules.media.service.GoogleDriveOAuthService;
import com.google.api.services.drive.Drive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/media/google")
public class GoogleDriveOAuthController {

    private final GoogleDriveOAuthService oauthService;
    private final GoogleDriveClientService driveClientService;
    private final GoogleDriveFolderService folderService;

    public GoogleDriveOAuthController(
            GoogleDriveOAuthService oauthService,
            GoogleDriveClientService driveClientService,
            GoogleDriveFolderService folderService) {

        this.oauthService = oauthService;
        this.driveClientService = driveClientService;
        this.folderService = folderService;
    }

    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize()
            throws GeneralSecurityException, IOException {

        String authorizationUrl =
                oauthService.getAuthorizationUrl();

        return ResponseEntity
                .status(302)
                .header("Location", authorizationUrl)
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(
            @RequestParam("code") String code)
            throws GeneralSecurityException, IOException {

        oauthService.exchangeCode(code);

        return ResponseEntity.ok(
                "Google Drive authorization successful."
        );
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testDriveAccess()
            throws GeneralSecurityException, IOException {

        Drive drive =
                driveClientService.getDriveClient();

        var about =
                drive.about()
                        .get()
                        .setFields(
                                "user(displayName,emailAddress)"
                        )
                        .execute();

        return ResponseEntity.ok(
                Map.of(
                        "status", "SUCCESS",
                        "message",
                        "Google Drive connection successful.",
                        "user",
                        about.getUser()
                )
        );
    }

    @GetMapping("/test/create-folder")
    public ResponseEntity<String> createTestFolder()
            throws GeneralSecurityException, IOException {

        var folder =
                folderService.getOrCreateFolder(
                        "GarageST-Test",
                        "1nxCjGoe_NHyCy0MXUpp7G2Z_tHy8XRWq"
                );

        return ResponseEntity.ok(
                "Folder ready. ID: " + folder.getId()
        );
    }

    @GetMapping("/test/create-job-card-structure")
    public ResponseEntity<?> createJobCardStructure(
            @RequestParam String garageCode,
            @RequestParam String jobCardNumber) {

        try {
            var garageFolder =
                    folderService.getOrCreateGarageFolder(
                            garageCode
                    );

            var jobCardFolder =
                    folderService.getOrCreateJobCardFolder(
                            garageCode,
                            jobCardNumber
                    );

            var beforeService =
                    folderService.getOrCreateStageFolder(
                            garageCode,
                            jobCardNumber,
                            "BEFORE_SERVICE"
                    );

            var duringRepair =
                    folderService.getOrCreateStageFolder(
                            garageCode,
                            jobCardNumber,
                            "DURING_REPAIR"
                    );

            var afterRepair =
                    folderService.getOrCreateStageFolder(
                            garageCode,
                            jobCardNumber,
                            "AFTER_REPAIR"
                    );

            return ResponseEntity.ok(
                    Map.of(
                            "garageFolderId",
                            garageFolder.getId(),
                            "jobCardFolderId",
                            jobCardFolder.getId(),
                            "beforeServiceFolderId",
                            beforeService.getId(),
                            "duringRepairFolderId",
                            duringRepair.getId(),
                            "afterRepairFolderId",
                            afterRepair.getId()
                    )
            );

        } catch (Exception ex) {

            ex.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .body(
                            Map.of(
                                    "error",
                                    ex.getClass().getName(),
                                    "message",
                                    ex.getMessage() == null
                                            ? "No exception message"
                                            : ex.getMessage()
                            )
                    );
        }
    }
}