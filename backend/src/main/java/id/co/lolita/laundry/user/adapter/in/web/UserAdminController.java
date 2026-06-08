package id.co.lolita.laundry.user.adapter.in.web;

import id.co.lolita.laundry.user.adapter.in.web.dto.CreateUserRequest;
import id.co.lolita.laundry.user.adapter.in.web.dto.SetActiveRequest;
import id.co.lolita.laundry.user.adapter.in.web.dto.UpdateUserRequest;
import id.co.lolita.laundry.user.adapter.in.web.dto.UserResponse;
import id.co.lolita.laundry.user.domain.port.in.ManageUserUseCase;
import id.co.lolita.laundry.user.domain.port.in.ManageUserUseCase.CreateUserCommand;
import id.co.lolita.laundry.user.domain.port.in.ManageUserUseCase.UpdateUserCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Owner-only user administration (Phase 5). STAFF and DRIVER have no access — managing who
 * can log in is an OWNER privilege. The Auth0 account is created separately in the Auth0
 * dashboard; this screen registers/maintains the matching local {@code users} row.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
class UserAdminController {

    private final ManageUserUseCase users;

    @GetMapping
    List<UserResponse> list() {
        return users.list().stream().map(UserResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(users.create(
                new CreateUserCommand(request.auth0Sub(), request.fullName(), request.role())));
    }

    @PutMapping("/{id}")
    UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(users.update(
                new UpdateUserCommand(id, request.fullName(), request.role())));
    }

    @PatchMapping("/{id}/status")
    UserResponse setActive(@PathVariable Long id, @Valid @RequestBody SetActiveRequest request) {
        return UserResponse.from(users.setActive(id, request.active()));
    }
}