package com.github.kiraruto.sistemaBancario.controller;

import com.github.kiraruto.sistemaBancario.dto.UserDTO;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.service.UserService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.github.kiraruto.sistemaBancario.utils.VerifyUUID.validateUUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUser() {
        List<User> all = userService.getAllUser();
        return ResponseEntity.ok(all);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable("id") String id) {
        User user = userService.getUserById(validateUUID(id));
        return ResponseEntity.ok(user);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<User> createUserAccount(@RequestBody @Valid UserDTO userDTO) {
        User user = userService.createUserAccount(userDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping("/{id}/admin")
    @Transactional
    public ResponseEntity<User> userToAdmin(@PathVariable("id") String id) {
        userService.userToAdmin(validateUUID(id));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/gerente")
    public ResponseEntity<User> userToGerente(@PathVariable("id") String id) {
        userService.userToGerente(validateUUID(id));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/cliente")
    public ResponseEntity<User> userToCliente(@PathVariable("id") String id) {
        userService.userToCliente(validateUUID(id));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/disableUser")
    public ResponseEntity<Void> disableUser(@PathVariable("id") String id) {
        userService.disableUser(validateUUID(id));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activateUser")
    public ResponseEntity<Void> activateUser(@PathVariable("id") String id) {
        userService.activateUser(validateUUID(id));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<User> updateUser(@PathVariable("id") String id, @RequestBody UserDTO userDTO) {
        User updatedUser = userService.updateUser(validateUUID(id), userDTO);
        return ResponseEntity.ok(updatedUser);
    }
}
