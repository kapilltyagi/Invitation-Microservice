package com.tng.invitation.repository;

import com.tng.invitation.entity.AdminInvitations;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface InvitationRepositoryInterface extends ReactiveCrudRepository<AdminInvitations, UUID> {

    int existsByEmail(String email);
}
