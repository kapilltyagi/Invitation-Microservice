package com.tng.invitation.repository;

import com.tng.invitation.entity.AdminInvitations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static org.springframework.data.relational.core.query.Criteria.where;


@RequiredArgsConstructor
@Component
@Slf4j
public class InvitationRepository {

    @Autowired
    private final DatabaseClient databaseClient;

    public String findByEmail(String email) {
        //AdminInvitations result;
        try {
        Mono<String> result = this.databaseClient.select()
                .from(AdminInvitations.class)
                .matching(where("email").like(email))
                .fetch()
                .one().map(x -> x.getEmail());

            //System.out.println("result==" + result.toFuture().get());
            return result.toFuture().get();
        }catch (Exception e){}

        return null;
    }

}
