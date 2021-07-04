package com.microservice.authentication.common.service;

import com.microservice.authentication.common.model.UserType;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import lombok.AllArgsConstructor;
import org.jboss.aerogear.security.otp.Totp;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

@AllArgsConstructor
public class CustomAuthenticationProvider extends DaoAuthenticationProvider {

    private final AuthenticationCommonRepository userRepository;

    @Override
    public Authentication authenticate(Authentication auth)
        throws AuthenticationException {
        String verificationCode = ((CustomWebAuthenticationDetails) auth.getDetails()).getVerificationCode();
        com.microservice.authentication.common.model.Authentication user = userRepository.findByEmail(auth.getName());
        if ((user == null)) {
            throw new BadCredentialsException("Invalid username or password");
        }
        if (user.getUserType() == UserType.NONE) {
            Totp totp = new Totp(user.getSecret());
            if (!isValidLong(verificationCode) || !totp.verify(verificationCode)) {
                throw new BadCredentialsException("Invalid verification code");
            }
        }

        Authentication result = super.authenticate(auth);
        return new UsernamePasswordAuthenticationToken(
            user, result.getCredentials(), result.getAuthorities());
    }

    private boolean isValidLong(String code) {
        try {
            Long.parseLong(code);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
