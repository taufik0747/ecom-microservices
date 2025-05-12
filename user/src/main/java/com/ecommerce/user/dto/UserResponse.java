package com.ecommerce.user.dto;


import com.ecommerce.user.models.UserRole;
import lombok.Data;

@Data
public class UserResponse {
    private String id;
    private String firstName;
    private String LastName;
    private String email;
    private String phone;
    private UserRole role;
    private AddressDTO address;
}
