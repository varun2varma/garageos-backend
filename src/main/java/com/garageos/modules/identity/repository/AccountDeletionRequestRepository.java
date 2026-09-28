package com.garageos.modules.identity.repository;

import com.garageos.core.enums.identity.AccountDeletionRequestStatus;
import com.garageos.modules.identity.entity.AccountDeletionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountDeletionRequestRepository extends JpaRepository<AccountDeletionRequest, Long> {

    boolean existsByUserIdAndStatus(Long userId, AccountDeletionRequestStatus status);

}
