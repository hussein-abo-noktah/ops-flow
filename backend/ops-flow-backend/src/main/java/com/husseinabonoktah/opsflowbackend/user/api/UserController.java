package com.husseinabonoktah.opsflowbackend.user.api;

import com.husseinabonoktah.opsflowbackend.user.api.request.CreateUserRequest;
import com.husseinabonoktah.opsflowbackend.user.api.request.UpdateUserRequest;
import com.husseinabonoktah.opsflowbackend.user.api.response.UserResponse;
import com.husseinabonoktah.opsflowbackend.user.application.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/users/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @GetMapping
    public PagedModel<UserResponse> getAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return new PagedModel<>(service.getAll(page, size));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return service.update(id, request);
    }
}
