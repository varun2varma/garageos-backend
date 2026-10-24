package com.garageos.modules.identity.service.impl;

import com.garageos.core.enums.otp.OtpPurpose;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.identity.service.EmailVerificationService;
import com.garageos.modules.otp.service.OtpService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final UserRepository userRepository;
    private final OtpService otpService;

    public EmailVerificationServiceImpl(UserRepository userRepository, OtpService otpService) {
        this.userRepository = userRepository;
        this.otpService = otpService;
    }

    @Override
    @Transactional
    public void sendOwn(GarageUserPrincipal principal) {

        User user = load(principal);

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BusinessException("There is no e-mail address on your account to verify.");
        }

        // Always the account's own stored address, never client input.
        otpService.sendEmail(OtpPurpose.EMAIL_VERIFICATION, user.getEmail(), user.getId());
    }

    @Override
    @Transactional
    public void verifyOwn(GarageUserPrincipal principal, String otp) {

        User user = load(principal);

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BusinessException("There is no e-mail address on your account to verify.");
        }

        otpService.verifyAndConsume(OtpPurpose.EMAIL_VERIFICATION, user.getEmail(), otp);

        user.markEmailVerified(LocalDateTime.now());
        userRepository.save(user);
    }

    private User load(GarageUserPrincipal principal) {
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}
