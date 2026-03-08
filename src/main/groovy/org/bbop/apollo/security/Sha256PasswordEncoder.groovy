package org.bbop.apollo.security

import org.springframework.security.crypto.password.PasswordEncoder

import java.security.MessageDigest

class Sha256PasswordEncoder implements PasswordEncoder {

    @Override
    String encode(CharSequence rawPassword) {
        MessageDigest md = MessageDigest.getInstance("SHA-256")
        byte[] hash = md.digest(rawPassword.toString().getBytes("UTF-8"))
        return hash.encodeHex().toString()
    }

    @Override
    boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encode(rawPassword) == encodedPassword
    }
}
