package com.garageos.modules.identity.repository;

import com.garageos.modules.identity.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    List<PasswordResetToken> findByUserIdAndUsedFalse(Long userId);

    /**
     * Single-use consumption as one atomic statement: of any number of concurrent requests presenting the same
     * authorization, exactly one sees an update count of 1.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PasswordResetToken t set t.used = true where t.id = :id and t.used = false and t.expiresAt > :now")
    int consumeIfValid(@Param("id") Long id, @Param("now") LocalDateTime now);
}
