package com.github.kiraruto.sistemaBancario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kiraruto.sistemaBancario.dto.UserDTO;
import com.github.kiraruto.sistemaBancario.global.exception.GlobalExceptionHandler;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumUserRole;
import com.github.kiraruto.sistemaBancario.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User user;
    private UserDTO userDTO;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();

        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setRole(EnumUserRole.CLIENTE);
        user.setActive(true);

        userDTO = new UserDTO(
                "testuser",
                "Test",
                "User",
                "password",
                "test@example.com"
        );
    }

    @Test
    void getAllUsers_ShouldReturnListOfUsers() throws Exception {
        when(userService.getAllUser()).thenReturn(Arrays.asList(user));

        mockMvc.perform(get("/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("testuser"))
                .andExpect(jsonPath("$[0].email").value("test@example.com"));

        verify(userService, times(1)).getAllUser();
    }

    @Test
    void getUserById_ShouldReturnUser() throws Exception {
        when(userService.getUserById(userId)).thenReturn(user);

        mockMvc.perform(get("/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService, times(1)).getUserById(userId);
    }

    @Test
    void createUserAccount_ShouldCreateUserAndReturnCreatedStatus() throws Exception {
        when(userService.createUserAccount(any(UserDTO.class))).thenReturn(user);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService, times(1)).createUserAccount(any(UserDTO.class));
    }

    @Test
    void userToAdmin_ShouldUpdateUserRoleToAdmin() throws Exception {
        doNothing().when(userService).userToAdmin(userId);

        mockMvc.perform(put("/users/" + userId + "/admin")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).userToAdmin(userId);
    }

    @Test
    void userToGerente_ShouldUpdateUserRoleToGerente() throws Exception {
        doNothing().when(userService).userToGerente(userId);

        mockMvc.perform(put("/users/" + userId + "/gerente")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).userToGerente(userId);
    }

    @Test
    void userToCliente_ShouldUpdateUserRoleToCliente() throws Exception {
        doNothing().when(userService).userToCliente(userId);

        mockMvc.perform(put("/users/" + userId + "/cliente")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).userToCliente(userId);
    }

    @Test
    void disableUser_ShouldSetUserAsInactive() throws Exception {
        doNothing().when(userService).disableUser(userId);

        mockMvc.perform(put("/users/" + userId + "/disableUser")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).disableUser(userId);
    }

    @Test
    void activateUser_ShouldSetUserAsActive() throws Exception {
        doNothing().when(userService).activateUser(userId);

        mockMvc.perform(put("/users/" + userId + "/activateUser")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).activateUser(userId);
    }

    @Test
    void updateUser_ShouldUpdateUserDetails() throws Exception {
        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setUsername("updateduser");
        updatedUser.setFirstName("gustavo");
        updatedUser.setLastName("henrique");
        updatedUser.setPassword("156516");
        updatedUser.setEmail("updated@example.com");
        updatedUser.setRole(EnumUserRole.CLIENTE);
        updatedUser.setActive(true);
        updatedUser.setScheduledTransfer(null);
        updatedUser.setSavingsAccount(null);
        updatedUser.setCheckingAccount(null);

        when(userService.updateUser(eq(userId), any(UserDTO.class))).thenReturn(updatedUser);

        mockMvc.perform(put("/users/" + userId + "/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updateduser"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));

        verify(userService, times(1)).updateUser(eq(userId), any(UserDTO.class));
    }

    @Test
    void getUserById_WithInvalidId_ShouldReturnNotFound() throws Exception {
        UUID invalidId = UUID.randomUUID();
        when(userService.getUserById(invalidId))
                .thenThrow(new EntityNotFoundException("O Id não existe!"));

        mockMvc.perform(get("/users/" + invalidId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).getUserById(invalidId);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    void createUserAccount_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        UserDTO invalidUserDTO = new UserDTO(
                "", // username vazio
                "", // firstName vazio
                "", // lastName vazio
                "short", // senha curta
                "invalid-email" // email inválido
        );

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUserDTO)))
                .andExpect(status().isUnprocessableEntity());
    }
}