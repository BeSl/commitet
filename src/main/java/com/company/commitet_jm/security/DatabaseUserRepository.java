package com.company.commitet_jm.security;

import com.company.commitet_jm.entity.User;
import io.jmix.securitydata.user.AbstractDatabaseUserRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component("UserRepository")
public class DatabaseUserRepository extends AbstractDatabaseUserRepository<User> {

    @Override
    public Class<User> getUserClass() {
        return User.class;
    }

    @Override
    public void initSystemUser(User systemUser) {
        var authorities = grantedAuthoritiesBuilder
                .addResourceRole(FullAccessRole.CODE)
                .build();
        systemUser.setAuthorities(authorities);
    }

    @Override
    public void initAnonymousUser(User anonymousUser) {
        // No initialization needed for anonymous user
    }
}