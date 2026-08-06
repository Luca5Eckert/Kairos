package com.kairos.module.user.presentation.controller;

import com.kairos.module.user.application.use_case.ChangeMyPasswordUseCase;
import com.kairos.module.user.application.use_case.GetMyProfileUseCase;
import com.kairos.module.user.application.use_case.UpdateMyProfileUseCase;
import com.kairos.module.user.domain.model.Role;
import com.kairos.module.user.domain.model.User;
import com.kairos.share.exception.GlobalHandlerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private GetMyProfileUseCase getMyProfileUseCase;
    @Mock private UpdateMyProfileUseCase updateMyProfileUseCase;
    @Mock private ChangeMyPasswordUseCase changeMyPasswordUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new UserController(getMyProfileUseCase, updateMyProfileUseCase, changeMyPasswordUseCase))
                .setControllerAdvice(new GlobalHandlerException())
                .build();
    }

    @Test
    void getMe_returnsPublicProfileWithoutSecurityMetadata() throws Exception {
        when(getMyProfileUseCase.execute()).thenReturn(user());

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user().getId().toString()))
                .andExpect(jsonPath("$.name").value("Lucas"))
                .andExpect(jsonPath("$.username").value("lucas"))
                .andExpect(jsonPath("$.email").value("lucas@example.com"))
                .andExpect(jsonPath("$.emailConfirmed").value(true))
                .andExpect(jsonPath("$.roles[0]").value("FREE"))
                .andExpect(jsonPath("$.hashPassword").doesNotExist())
                .andExpect(jsonPath("$.confirmationCodeHash").doesNotExist());
    }

    @Test
    void patchMe_updatesOnlySupportedFields() throws Exception {
        when(updateMyProfileUseCase.execute(argThat(command ->
                command.name().equals("New Name") && command.username().equals("NewName"))))
                .thenReturn(user());

        mockMvc.perform(patch("/users/me")
                        .contentType("application/json")
                        .content("""
                                {"name":"New Name","username":"NewName"}
                                """))
                .andExpect(status().isOk());

        verify(updateMyProfileUseCase).execute(argThat(command ->
                command.name().equals("New Name") && command.username().equals("NewName")));
    }

    @Test
    void changePassword_requiresAllCredentialFields() throws Exception {
        mockMvc.perform(post("/users/me/password")
                        .contentType("application/json")
                        .content("""
                                {"currentPassword":"","newPassword":"","newPasswordConfirmation":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(3));

        verifyNoInteractions(changeMyPasswordUseCase);
    }

    @Test
    void changePassword_mapsRequestAndReturnsNoCredentials() throws Exception {
        mockMvc.perform(post("/users/me/password")
                        .contentType("application/json")
                        .content("""
                                {"currentPassword":"OldPassword123!","newPassword":"NewPassword123!","newPasswordConfirmation":"NewPassword123!"}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(changeMyPasswordUseCase).execute(argThat(command ->
                command.currentPassword().equals("OldPassword123!")
                        && command.newPassword().equals("NewPassword123!")
                        && command.newPasswordConfirmation().equals("NewPassword123!")));
    }

    private User user() {
        return new User.Builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .name("Lucas")
                .username("lucas")
                .email("lucas@example.com")
                .hashPassword("hash-not-public")
                .role(Role.FREE)
                .emailConfirmed(true)
                .confirmationCodeHash("code-not-public")
                .build();
    }
}
