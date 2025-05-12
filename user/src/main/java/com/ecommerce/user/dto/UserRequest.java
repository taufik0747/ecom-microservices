package com.ecommerce.user.dto;

import lombok.Data;

@Data
public class UserRequest {

    private String firstName;
    private String LastName;
    private String email;
    private String phone;
    private AddressDTO address;
}
