package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.UserDTO;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumUserRole;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.utils.UserValidate;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserValidate userValidate;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;

    private final UUID testUserId = UUID.randomUUID();
    private final UserDTO testUserDTO = new UserDTO("test", "Test", "User", "testUser", "test@email.com");

    private User createTestUser(UUID id, EnumUserRole role, boolean isActive) {
        User user = new User();
        user.setId(id);
        user.setUsername("testUser");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@email.com");
        user.setRole(role);
        user.setActive(isActive);
        user.setCheckingAccount(new CheckingAccount());
        user.setSavingsAccount(new SavingsAccount());
        user.setScheduledTransfer(new ArrayList<>());
        return user;
    }

    @Nested
    @DisplayName("UserDetailsService Tests")
    class UserDetailsServiceTests {
        @Test
        @DisplayName("Should load user by username (email)")
        void userDetailsService_ShouldLoadUserByUsername() {
            User testUser = createTestUser(testUserId, EnumUserRole.CLIENTE, true);
            when(userRepository.findByEmail("testUser")).thenReturn(Optional.of(testUser));

            UserDetailsService service = userService.userDetailsService();
            UserDetails userDetails = service.loadUserByUsername("testUser");

            assertThat(userDetails.getUsername()).isEqualTo("testUser");
        }

        @Test
        @DisplayName("Should throw when user not found by email")
        void userDetailsService_ShouldThrowWhenUserNotFound() {
            when(userRepository.findByEmail("nonexistent@email.com")).thenReturn(Optional.empty());

            UserDetailsService service = userService.userDetailsService();
            assertThatThrownBy(() -> service.loadUserByUsername("nonexistent@email.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("Usuario não encontradi");
        }
    }

    @Nested
    @DisplayName("User Activation Tests")
    class UserActivationTests {
        @Test
        @DisplayName("Should activate inactive user")
        void activateUser_ShouldActivateInactiveUser() {
            User inactiveUser = createTestUser(testUserId, EnumUserRole.CLIENTE, false);
            when(userValidate.validateUserFalseToTrue(testUserId)).thenReturn(inactiveUser);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            userService.activateUser(testUserId);

            verify(userRepository).save(argThat(user -> user.isActive()));
        }

        @Test
        @DisplayName("Should throw when activating already active user")
        void activateUser_ShouldThrowWhenAlreadyActive() {
            when(userValidate.validateUserFalseToTrue(testUserId))
                    .thenThrow(new IllegalArgumentException("Usuário já ativo"));

            assertThatThrownBy(() -> userService.activateUser(testUserId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário já ativo");
        }
    }

    @Nested
    @DisplayName("Role Change Tests")
    class RoleChangeTests {
        @Test
        @DisplayName("Should change role to ADMIN")
        void userToAdmin_ShouldChangeRoleToAdmin() {
            User user = createTestUser(testUserId, EnumUserRole.CLIENTE, true);
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            userService.userToAdmin(testUserId);

            verify(userRepository).save(argThat(u ->
                    u.getRole() == EnumUserRole.ADMIN
            ));
        }

        @Test
        @DisplayName("Should change role to GERENTE")
        void userToGerente_ShouldChangeRoleToManager() {
            User user = createTestUser(testUserId, EnumUserRole.CLIENTE, true);
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            userService.userToGerente(testUserId);

            verify(userRepository).save(argThat(u ->
                    u.getRole() == EnumUserRole.GERENTE
            ));
        }

        @Test
        @DisplayName("Should throw when changing role of inactive user")
        void roleChange_ShouldThrowWhenUserInactive() {
            User inactiveUser = createTestUser(testUserId, EnumUserRole.CLIENTE, false);
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(inactiveUser));

            assertThatThrownBy(() -> userService.userToAdmin(testUserId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("O usuário está inativo!");
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {
        @Test
        @DisplayName("Should handle null input in updateUser")
        void updateUser_ShouldThrowWhenNullInput() {
            assertThatThrownBy(() -> userService.updateUser(null, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle empty username in createUser")
        void createUser_ShouldThrowWhenEmptyUsername() {
            UserDTO invalidDTO = new UserDTO("", "Test", "User", "", "test@email.com");
            doThrow(new IllegalArgumentException("Username cannot be empty"))
                    .when(userValidate).validateCreateAccountUser(invalidDTO);

            assertThatThrownBy(() -> userService.createUserAccount(invalidDTO))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("Should find all active users")
    void getAllUser_ShouldReturnOnlyActiveUsers() {
        User activeUser = createTestUser(testUserId, EnumUserRole.CLIENTE, true);
        User inactiveUser = createTestUser(UUID.randomUUID(), EnumUserRole.CLIENTE, false);

        when(userRepository.findAllByIsActiveTrue()).thenReturn(List.of(activeUser));

        List<User> result = userService.getAllUser();

        assertThat(result)
                .hasSize(1)
                .allMatch(User::isActive);
    }

    @Test
    @DisplayName("Should throw when user not found in getUserById")
    void getUserById_ShouldThrowWhenNotFound() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(testUserId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("O Id não existe!");
    }

    @Test
    @DisplayName("Should update user with same email")
    void updateUser_ShouldAllowSameEmail() {
        User existingUser = createTestUser(testUserId, EnumUserRole.CLIENTE, true);
        when(userValidate.validateUserTrueToFalse(testUserId)).thenReturn(existingUser);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO updateDTO = new UserDTO("test", "Test", "User", "testUser", "test@email.com");
        assertDoesNotThrow(() -> userService.updateUser(testUserId, updateDTO));
    }
}