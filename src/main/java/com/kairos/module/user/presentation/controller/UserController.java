package com.kairos.module.user.presentation.controller;

import com.kairos.module.user.application.command.ChangePasswordCommand;
import com.kairos.module.user.application.command.UpdateProfileCommand;
import com.kairos.module.user.application.use_case.ChangeMyPasswordUseCase;
import com.kairos.module.user.application.use_case.GetMyProfileUseCase;
import com.kairos.module.user.application.use_case.UpdateMyProfileUseCase;
import com.kairos.module.user.presentation.dto.ChangePasswordRequest;
import com.kairos.module.user.presentation.dto.UpdateProfileRequest;
import com.kairos.module.user.presentation.dto.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final GetMyProfileUseCase getMyProfileUseCase;
    private final UpdateMyProfileUseCase updateMyProfileUseCase;
    private final ChangeMyPasswordUseCase changeMyPasswordUseCase;

    public UserController(GetMyProfileUseCase getMyProfileUseCase,
                          UpdateMyProfileUseCase updateMyProfileUseCase,
                          ChangeMyPasswordUseCase changeMyPasswordUseCase) {
        this.getMyProfileUseCase = getMyProfileUseCase;
        this.updateMyProfileUseCase = updateMyProfileUseCase;
        this.changeMyPasswordUseCase = changeMyPasswordUseCase;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMe() {
        return ResponseEntity.ok(UserProfileResponse.from(getMyProfileUseCase.execute()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMe(@RequestBody @Valid UpdateProfileRequest request) {
        var command = new UpdateProfileCommand(request.name(), request.username());
        return ResponseEntity.ok(UserProfileResponse.from(updateMyProfileUseCase.execute(command)));
    }

    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        changeMyPasswordUseCase.execute(new ChangePasswordCommand(
                request.currentPassword(),
                request.newPassword(),
                request.newPasswordConfirmation()
        ));
        return ResponseEntity.noContent().build();
    }
}
