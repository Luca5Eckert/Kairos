package com.kairos.auth.presentation.controller;

import com.kairos.module.auth.application.use_case.ConfirmEmailUseCase;
import com.kairos.module.auth.application.use_case.LoginUseCase;
import com.kairos.module.auth.application.use_case.RegisterUseCase;
import com.kairos.module.auth.domain.model.AuthenticatedSession;
import com.kairos.module.auth.presentation.controller.AuthController;
import com.kairos.share.exception.GlobalHandlerException;
import com.kairos.module.user.domain.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private LoginUseCase loginUseCase;

    @Mock
    private RegisterUseCase registerUseCase;

    @Mock
    private ConfirmEmailUseCase confirmEmailUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new AuthController(loginUseCase, registerUseCase, confirmEmailUseCase))
                .setControllerAdvice(new GlobalHandlerException())
                .build();
    }

    @Test
    void register_validPayload_returnsCreatedAndMapsCommand() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Lucas",
                                  "username": "lucas",
                                  "email": "lucas@example.com",
                                  "password": "RawPassword123!"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(registerUseCase).execute(argThat(command ->
                command.name().equals("Lucas")
                        && command.username().equals("lucas")
                        && command.email().equals("lucas@example.com")
                        && command.password().equals("RawPassword123!")
        ));
    }

    @Test
    void register_missingRequiredFields_returnsValidationError() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "",
                                  "username": "",
                                  "email": "",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation error"))
                .andExpect(jsonPath("$.description").value("One or more fields are invalid"))
                .andExpect(jsonPath("$.path").value("/auth/register"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(4));

        verifyNoInteractions(registerUseCase);
    }

    @Test
    void login_validPayload_returnsTokenAndRoles() throws Exception {
        when(loginUseCase.execute(argThat(command ->
                command.identifier().equals("lucas@example.com")
                        && command.password().equals("RawPassword123!")
        ))).thenReturn(new AuthenticatedSession("access-token", List.of(Role.FREE, Role.ADMIN)));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "identifier": "lucas@example.com",
                                  "password": "RawPassword123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.roles[0]").value("FREE"))
                .andExpect(jsonPath("$.roles[1]").value("ADMIN"));
    }

    @Test
    void login_missingRequiredFields_returnsValidationError() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "identifier": "",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation error"))
                .andExpect(jsonPath("$.path").value("/auth/login"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));

        verifyNoInteractions(loginUseCase);
    }

    @Test
    void confirmEmail_validPayload_returnsTokenAndRoles() throws Exception {
        when(confirmEmailUseCase.execute(argThat(command ->
                command.code().equals("123456")
                        && command.email().equals("lucas@example.com")
        ))).thenReturn(new AuthenticatedSession("confirmed-token", List.of(Role.FREE)));

        mockMvc.perform(post("/auth/confirm-email")
                        .contentType("application/json")
                        .content("""
                                {
                                  "code": "123456",
                                  "email": "lucas@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("confirmed-token"))
                .andExpect(jsonPath("$.roles[0]").value("FREE"));
    }

    @Test
    void confirmEmail_missingRequiredFields_returnsValidationError() throws Exception {
        mockMvc.perform(post("/auth/confirm-email")
                        .contentType("application/json")
                        .content("""
                                {
                                  "code": "",
                                  "email": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation error"))
                .andExpect(jsonPath("$.path").value("/auth/confirm-email"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));

        verifyNoInteractions(confirmEmailUseCase);
    }
}
