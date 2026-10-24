package com.garageos.modules.bankaccount.dto.response;

import java.time.LocalDateTime;

/**
 * The only shape a bank account ever leaves the backend in: the number is masked (last four digits), there is no
 * field that could carry the full number or the ciphertext, and it is built only for the garage's own owner or a
 * platform administrator. It is deliberately not part of any shared garage DTO.
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
        LocalDateTime registeredAt) {
}
