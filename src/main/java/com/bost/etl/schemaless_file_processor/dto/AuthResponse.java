package com.bost.etl.schemaless_file_processor.dto;

import com.bost.etl.schemaless_file_processor.security.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private UUID userId;
    private String username;
    private String email;
    private Set<Role> roles;
}
