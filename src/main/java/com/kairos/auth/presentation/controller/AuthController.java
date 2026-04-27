package com.kairos.auth.presentation.controller;

import com.kairos.auth.application.command.LoginCommand;
import com.kairos.auth.application.use_case.ConfirmEmailUseCase;
import com.kairos.auth.application.use_case.LoginUseCase;
import com.kairos.auth.application.use_case.RegisterUseCase;

import com.kairos.auth.presentation.dto.login.LoginRequest;
import com.kairos.auth.presentation.dto.login.LoginResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;
    private final ConfirmEmailUseCase confirmEmailUseCase;

    public AuthController(LoginUseCase loginUseCase, RegisterUseCase registerUseCase, ConfirmEmailUseCase confirmEmailUseCase) {
        this.loginUseCase = loginUseCase;
        this.registerUseCase = registerUseCase;
        this.confirmEmailUseCase = confirmEmailUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(LoginRequest request) {
        var command = LoginCommand.create(request.identifier(), request.password());

        var session = loginUseCase.execute(command);
        var response = LoginResponse.create(session.accessToken(), session.roles());

        return ResponseEntity.ok(response);
    }

}
