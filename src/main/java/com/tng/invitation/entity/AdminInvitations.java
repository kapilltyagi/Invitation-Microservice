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
@Table("public.invitations")
public class AdminInvitations {
    @Id
    @Column("invitation_id")
    private UUID invitationId;

    @Column("invitations_type")
    private String invitationsType;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("contact_id")
    private UUID contactId;

    @Column("company_name")
    private String companyName;

    @Column("company_id")
    private UUID companyId;

    @Column("country_abbr")
    private String countryAbbr;

    @Column("email")
    private String email;

    @Column("invited_to")
    private String invitedTo;

    @Column("invitation_status")
    private String invitationStatus;

    @Column("invitation_sent_at")
    private Timestamp invitationSentAt;

    @Column("invitation_accepted_at")
    private Timestamp invitationAcceptedAt;

    @Column("created_at")
    private Timestamp createdAt;

    @Column("state_province_abbr")
    private String stateProvinceAbbr;
}

