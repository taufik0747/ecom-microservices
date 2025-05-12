package com.app.ecom.dto;

import com.app.ecom.model.Address;
import com.app.ecom.model.UserRole;
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
