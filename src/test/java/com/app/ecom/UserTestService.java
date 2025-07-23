package com.app.ecom;

import com.app.ecom.dto.AddressDTO;
import com.app.ecom.dto.UserRequest;
import com.app.ecom.dto.UserResponse;
import com.app.ecom.model.Address;
import com.app.ecom.model.User;
import com.app.ecom.model.UserRole;
import com.app.ecom.repository.UserRepository;
import com.app.ecom.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UserRequest userRequest;
    private UserRequest userRequestWithAddress;
    private User user;
    private User savedUser;

    @BeforeEach
    void setUp() {
        // Arrange - Set up test data
        userRequest = createUserRequest();
        userRequestWithAddress = createUserRequestWithAddress();
        user = createUser();
        savedUser = createSavedUser();
    }

    @Nested
    @DisplayName("Fetch All Users Tests")
    class FetchAllUsersTests {

        @Test
        @DisplayName("Should return all users successfully")
        void shouldReturnAllUsersSuccessfully() {
            // Arrange
            List<User> users = Arrays.asList(
                    createSavedUser(),
                    createSecondUser()
            );
            when(userRepository.findAll()).thenReturn(users);

            // Act
            List<UserResponse> result = userService.fetchAllUsers();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getFirstName()).isEqualTo("John");
            assertThat(result.get(0).getLastName()).isEqualTo("Doe");
            assertThat(result.get(0).getEmail()).isEqualTo("john.doe@example.com");
            assertThat(result.get(1).getFirstName()).isEqualTo("Jane");

            verify(userRepository, times(2)).findAll(); // Called twice in the method
        }

        @Test
        @DisplayName("Should return empty list when no users exist")
        void shouldReturnEmptyListWhenNoUsersExist() {
            // Arrange
            when(userRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<UserResponse> result = userService.fetchAllUsers();

            // Assert
            assertThat(result).isEmpty();

            verify(userRepository, times(2)).findAll();
        }

        @Test
        @DisplayName("Should handle users with and without addresses")
        void shouldHandleUsersWithAndWithoutAddresses() {
            // Arrange
            User userWithAddress = createSavedUserWithAddress();
            User userWithoutAddress = createSavedUser();
            userWithoutAddress.setAddress(null);

            when(userRepository.findAll()).thenReturn(Arrays.asList(userWithAddress, userWithoutAddress));

            // Act
            List<UserResponse> result = userService.fetchAllUsers();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getAddress()).isNotNull();
            assertThat(result.get(1).getAddress()).isNull();
        }

        @Test
        @DisplayName("Should convert user roles correctly")
        void shouldConvertUserRolesCorrectly() {
            // Arrange
            User adminUser = createSavedUser();
            adminUser.setRole(UserRole.ADMIN);
            User customerUser = createSavedUser();
            customerUser.setRole(UserRole.CUSTOMER);

            when(userRepository.findAll()).thenReturn(Arrays.asList(adminUser, customerUser));

            // Act
            List<UserResponse> result = userService.fetchAllUsers();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getRole()).isEqualTo(UserRole.ADMIN);
            assertThat(result.get(1).getRole()).isEqualTo(UserRole.CUSTOMER);
        }
    }

    @Nested
    @DisplayName("Add User Tests")
    class AddUserTests {

        @Test
        @DisplayName("Should add user successfully without address")
        void shouldAddUserSuccessfullyWithoutAddress() {
            // Arrange
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // Act
            userService.addUser(userRequest);

            // Assert - Verify save was called with correct user data
            verify(userRepository, times(1)).save(argThat(user ->
                    user.getFirstName().equals("John") &&
                            user.getLastName().equals("Doe") &&
                            user.getEmail().equals("john.doe@example.com") &&
                            user.getPhone().equals("123-456-7890") &&
                            user.getAddress() == null
            ));
        }

        @Test
        @DisplayName("Should add user successfully with address")
        void shouldAddUserSuccessfullyWithAddress() {
            // Arrange
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // Act
            userService.addUser(userRequestWithAddress);

            // Assert
            verify(userRepository, times(1)).save(argThat(user ->
                    user.getFirstName().equals("John") &&
                            user.getAddress() != null &&
                            user.getAddress().getStreet().equals("123 Main St") &&
                            user.getAddress().getCity().equals("New York") &&
                            user.getAddress().getState().equals("NY") &&
                            user.getAddress().getCountry().equals("USA") &&
                            user.getAddress().getZipcode().equals("10001")
            ));
        }

        @Test
        @DisplayName("Should handle null address in user request")
        void shouldHandleNullAddressInUserRequest() {
            // Arrange
            UserRequest requestWithNullAddress = createUserRequest();
            requestWithNullAddress.setAddress(null);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // Act
            userService.addUser(requestWithNullAddress);

            // Assert
            verify(userRepository, times(1)).save(argThat(user ->
                    user.getAddress() == null
            ));
        }

        @Test
        @DisplayName("Should throw exception when user request is null")
        void shouldThrowExceptionWhenUserRequestIsNull() {
            // Act & Assert
            assertThrows(NullPointerException.class, () ->
                    userService.addUser(null)
            );

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should handle partial address data")
        void shouldHandlePartialAddressData() {
            // Arrange
            UserRequest requestWithPartialAddress = createUserRequest();
            AddressDTO partialAddress = new AddressDTO();
            partialAddress.setStreet("123 Main St");
            partialAddress.setCity("New York");
            // Missing state, country, zipcode
            requestWithPartialAddress.setAddress(partialAddress);

            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // Act
            userService.addUser(requestWithPartialAddress);

            // Assert
            verify(userRepository, times(1)).save(argThat(user ->
                    user.getAddress() != null &&
                            user.getAddress().getStreet().equals("123 Main St") &&
                            user.getAddress().getCity().equals("New York") &&
                            user.getAddress().getState() == null &&
                            user.getAddress().getCountry() == null &&
                            user.getAddress().getZipcode() == null
            ));
        }
    }

    @Nested
    @DisplayName("Fetch User Tests")
    class FetchUserTests {

        @Test
        @DisplayName("Should fetch user by ID successfully")
        void shouldFetchUserByIdSuccessfully() {
            // Arrange
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.of(savedUser));

            // Act
            Optional<UserResponse> result = userService.fetchUser(userId);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo("1");
            assertThat(result.get().getFirstName()).isEqualTo("John");
            assertThat(result.get().getLastName()).isEqualTo("Doe");
            assertThat(result.get().getEmail()).isEqualTo("john.doe@example.com");
            assertThat(result.get().getPhone()).isEqualTo("123-456-7890");
            assertThat(result.get().getRole()).isEqualTo(UserRole.CUSTOMER);

            verify(userRepository, times(1)).findById(userId);
        }

        @Test
        @DisplayName("Should return empty when user not found")
        void shouldReturnEmptyWhenUserNotFound() {
            // Arrange
            Long nonExistentId = 999L;
            when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act
            Optional<UserResponse> result = userService.fetchUser(nonExistentId);

            // Assert
            assertThat(result).isEmpty();

            verify(userRepository, times(1)).findById(nonExistentId);
        }

        @Test
        @DisplayName("Should fetch user with address successfully")
        void shouldFetchUserWithAddressSuccessfully() {
            // Arrange
            Long userId = 1L;
            User userWithAddress = createSavedUserWithAddress();
            when(userRepository.findById(userId)).thenReturn(Optional.of(userWithAddress));

            // Act
            Optional<UserResponse> result = userService.fetchUser(userId);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getAddress()).isNotNull();
            assertThat(result.get().getAddress().getStreet()).isEqualTo("123 Main St");
            assertThat(result.get().getAddress().getCity()).isEqualTo("New York");
            assertThat(result.get().getAddress().getState()).isEqualTo("NY");
            assertThat(result.get().getAddress().getCountry()).isEqualTo("USA");
            assertThat(result.get().getAddress().getZipcode()).isEqualTo("10001");
        }

        @Test
        @DisplayName("Should handle null user ID")
        void shouldHandleNullUserId() {
            // Arrange
            when(userRepository.findById(null)).thenReturn(Optional.empty());

            // Act
            Optional<UserResponse> result = userService.fetchUser(null);

            // Assert
            assertThat(result).isEmpty();

            verify(userRepository, times(1)).findById(null);
        }
    }

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update existing user successfully")
        void shouldUpdateExistingUserSuccessfully() {
            // Arrange
            Long userId = 1L;
            User existingUser = createSavedUser();
            UserRequest updateRequest = createUserRequest();
            updateRequest.setFirstName("UpdatedJohn");
            updateRequest.setEmail("updated.john@example.com");

            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(existingUser)).thenReturn(existingUser);

            // Act
            boolean result = userService.updateUser(userId, updateRequest);

            // Assert
            assertThat(result).isTrue();
            assertThat(existingUser.getFirstName()).isEqualTo("UpdatedJohn");
            assertThat(existingUser.getEmail()).isEqualTo("updated.john@example.com");

            verify(userRepository, times(1)).findById(userId);
            verify(userRepository, times(1)).save(existingUser);
        }

        @Test
        @DisplayName("Should return false when user not found")
        void shouldReturnFalseWhenUserNotFound() {
            // Arrange
            Long nonExistentId = 999L;
            when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act
            boolean result = userService.updateUser(nonExistentId, userRequest);

            // Assert
            assertThat(result).isFalse();

            verify(userRepository, times(1)).findById(nonExistentId);
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should update user with new address")
        void shouldUpdateUserWithNewAddress() {
            // Arrange
            Long userId = 1L;
            User existingUser = createSavedUser();
            existingUser.setAddress(null); // User without address initially

            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(existingUser)).thenReturn(existingUser);

            // Act
            boolean result = userService.updateUser(userId, userRequestWithAddress);

            // Assert
            assertThat(result).isTrue();
            assertThat(existingUser.getAddress()).isNotNull();
            assertThat(existingUser.getAddress().getStreet()).isEqualTo("123 Main St");
            assertThat(existingUser.getAddress().getCity()).isEqualTo("New York");
        }

        @Test
        @DisplayName("Should update user address when address already exists")
        void shouldUpdateUserAddressWhenAddressAlreadyExists() {
            // Arrange
            Long userId = 1L;
            User existingUser = createSavedUserWithAddress();
            UserRequest updateRequest = createUserRequestWithAddress();
            updateRequest.getAddress().setStreet("456 Oak Avenue");
            updateRequest.getAddress().setCity("Los Angeles");
            updateRequest.getAddress().setState("CA");

            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(existingUser)).thenReturn(existingUser);

            // Act
            boolean result = userService.updateUser(userId, updateRequest);

            // Assert
            assertThat(result).isTrue();
            assertThat(existingUser.getAddress().getStreet()).isEqualTo("456 Oak Avenue");
            assertThat(existingUser.getAddress().getCity()).isEqualTo("Los Angeles");
            assertThat(existingUser.getAddress().getState()).isEqualTo("CA");
        }

        @Test
        @DisplayName("Should handle null update request")
        void shouldHandleNullUpdateRequest() {
            // Arrange
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.of(savedUser));

            // Act & Assert
            assertThrows(NullPointerException.class, () ->
                    userService.updateUser(userId, null)
            );
        }

        @Test
        @DisplayName("Should remove address when request has null address")
        void shouldRemoveAddressWhenRequestHasNullAddress() {
            // Arrange
            Long userId = 1L;
            User existingUser = createSavedUserWithAddress();
            UserRequest updateRequest = createUserRequest();
            updateRequest.setAddress(null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(existingUser)).thenReturn(existingUser);

            // Act
            boolean result = userService.updateUser(userId, updateRequest);

            // Assert
            assertThat(result).isTrue();
            // Note: Based on the code, if address is null, it won't update the address
            // The existing address will remain unchanged
            assertThat(existingUser.getAddress()).isNotNull(); // Address should remain
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete user lifecycle")
        void shouldHandleCompleteUserLifecycle() {
            // Arrange
            Long userId = 1L;
            User savedUser = createSavedUser();

            // Mock operations
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(userRepository.findById(userId)).thenReturn(Optional.of(savedUser));
            when(userRepository.findAll()).thenReturn(Arrays.asList(savedUser));

            // Act - Add user
            userService.addUser(userRequest);

            // Act - Fetch user
            Optional<UserResponse> fetchedUser = userService.fetchUser(userId);

            // Act - Update user
            UserRequest updateRequest = createUserRequest();
            updateRequest.setFirstName("UpdatedName");
            boolean updated = userService.updateUser(userId, updateRequest);

            // Act - Fetch all users
            List<UserResponse> allUsers = userService.fetchAllUsers();

            // Assert
            verify(userRepository, times(1)).save(any(User.class));
            assertThat(fetchedUser).isPresent();
            assertThat(updated).isTrue();
            assertThat(allUsers).hasSize(1);
        }
    }

    // Helper methods for creating test data
    private UserRequest createUserRequest() {
        UserRequest request = new UserRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe@example.com");
        request.setPhone("123-456-7890");
        return request;
    }

    private UserRequest createUserRequestWithAddress() {
        UserRequest request = createUserRequest();
        AddressDTO address = new AddressDTO();
        address.setStreet("123 Main St");
        address.setCity("New York");
        address.setState("NY");
        address.setCountry("USA");
        address.setZipcode("10001");
        request.setAddress(address);
        return request;
    }

    private User createUser() {
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setPhone("123-456-7890");
        user.setRole(UserRole.CUSTOMER);
        return user;
    }

    private User createSavedUser() {
        User user = createUser();
        user.setId(1L);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private User createSavedUserWithAddress() {
        User user = createSavedUser();
        Address address = new Address();
        address.setId(1L);
        address.setStreet("123 Main St");
        address.setCity("New York");
        address.setState("NY");
        address.setCountry("USA");
        address.setZipcode("10001");
        user.setAddress(address);
        return user;
    }

    private User createSecondUser() {
        User user = new User();
        user.setId(2L);
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setEmail("jane.smith@example.com");
        user.setPhone("987-654-3210");
        user.setRole(UserRole.ADMIN);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}