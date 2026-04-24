package com.kairos.auth.domain.port;

public interface CodeConfirmationPort {

    String generateCode();

    boolean validateCode(String code, String email);

}
