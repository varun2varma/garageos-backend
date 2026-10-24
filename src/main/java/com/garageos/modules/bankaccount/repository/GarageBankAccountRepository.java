package com.garageos.modules.bankaccount.repository;

import com.garageos.modules.bankaccount.entity.GarageBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GarageBankAccountRepository extends JpaRepository<GarageBankAccount, Long> {

    Optional<GarageBankAccount> findByGarageIdAndCurrentTrue(Long garageId);

    List<GarageBankAccount> findByGarageIdOrderByCreatedAtDesc(Long garageId);
}
