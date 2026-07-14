package com.kairos.share.presentation;

import com.kairos.auth.application.command.ConfirmEmailCommand;
import com.kairos.auth.application.command.LoginCommand;
import com.kairos.auth.application.command.RegisterCommand;
import com.kairos.auth.presentation.dto.login.LoginResponse;
import com.kairos.context_engine.application.command.UploadSourceCommand;
import com.kairos.context_engine.application.query.SearchSourceQuery;
import com.kairos.user.domain.model.Role;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiContractFactoryTest {

    @Test
    void authCommandFactories_preserveInputValues() {
        RegisterCommand register = RegisterCommand.of("Lucas", "lucas", "lucas@example.com", "RawPassword123!");
        LoginCommand login = LoginCommand.of("lucas@example.com", "RawPassword123!");
        ConfirmEmailCommand confirmEmail = ConfirmEmailCommand.of("123456", "lucas@example.com");

        assertThat(register.name()).isEqualTo("Lucas");
        assertThat(register.username()).isEqualTo("lucas");
        assertThat(register.email()).isEqualTo("lucas@example.com");
        assertThat(register.password()).isEqualTo("RawPassword123!");
        assertThat(login.identifier()).isEqualTo("lucas@example.com");
        assertThat(login.password()).isEqualTo("RawPassword123!");
        assertThat(confirmEmail.code()).isEqualTo("123456");
        assertThat(confirmEmail.email()).isEqualTo("lucas@example.com");
    }

    @Test
    void sourceCommandAndQueryFactories_preserveInputValues() {
        UploadSourceCommand upload = UploadSourceCommand.of("RAG notes", "Knowledge graphs improve retrieval.");
        SearchSourceQuery search = SearchSourceQuery.of("How does graph retrieval work?");

        assertThat(upload.title()).isEqualTo("RAG notes");
        assertThat(upload.content()).isEqualTo("Knowledge graphs improve retrieval.");
        assertThat(search.searchTerm()).isEqualTo("How does graph retrieval work?");
    }

    @Test
    void loginResponseFactory_convertsRolesToPublicNames() {
        LoginResponse response = LoginResponse.of("access-token", List.of(Role.FREE, Role.ADMIN));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.roles()).containsExactly("FREE", "ADMIN");
    }
}
