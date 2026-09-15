package com.garageos.modules.media.repository;

import com.garageos.modules.media.entity.GoogleDriveCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleDriveCredentialRepository extends JpaRepository<GoogleDriveCredential, Long> {

    Optional<GoogleDriveCredential> findByCredentialKey(String credentialKey);

    void deleteByCredentialKey(String credentialKey);
}
