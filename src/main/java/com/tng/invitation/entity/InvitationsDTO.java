package com.tng.invitation.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvitationsDTO {
    private String firstName;
    private String lastName;
    private String companyName;
    private String email;
    private String countryAbbr;
    private String stateProvinceAbbr;
    private String invitedTo;
    private String result;
    private String message;
}

