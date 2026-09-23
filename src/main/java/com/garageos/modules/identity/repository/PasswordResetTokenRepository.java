package com.garageos.modules.identity.repository;

import com.garageos.modules.identity.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    List<PasswordResetToken> findByUserIdAndUsedFalse(Long userId);
}
