package com.garageos.modules.identity.repository;

import com.garageos.modules.identity.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserSessionRepository
        extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByRefreshToken(
            String refreshToken);

    List<UserSession> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    void deleteByExpiresAtBefore(
            LocalDateTime dateTime);

    Optional<UserSession> findByRefreshTokenAndRevokedFalse(
            String refreshToken);

    void deleteByRefreshToken(String refreshToken);

    @Modifying
    @Query("""
            update UserSession s
            set s.revoked = true
            where s.user.id = :userId
            and s.revoked = false
            """)
    void revokeAllByUserId(Long userId);

}