package com.garageos.modules.bankaccount.dto.response;

import java.time.LocalDateTime;

/**
 * The only shape a bank account ever leaves the backend in. The number is masked (last four digits) for everyone except
 * the platform administrator route ({@code currentForPlatform}, SUPER_ADMIN only, audited), which also fills
 * {@code accountNumber}; for the garage's own owner it is always null. The ciphertext is never part of it. It is
 * deliberately not part of any shared garage DTO.
 */
public record BankAccountView(
        Long id,
        String accountHolderName,
        String bankName,
        String ifsc,
        String accountType,
        String accountNumberMasked,
        String verificationStatus,
        String verificationProvider,
        String verificationReference,
        LocalDateTime verifiedAt,
        String failureReason,
        LocalDateTime registeredAt,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        String accountNumber) {
}
