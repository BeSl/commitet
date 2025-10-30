package com.company.commitet_jm.entity;

import io.jmix.core.HasTimeZone;
import io.jmix.core.annotation.Secret;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.SystemLevel;
import io.jmix.core.metamodel.annotation.DependsOnProperties;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.security.authentication.JmixUserDetails;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@JmixEntity
@Entity
@Table(name = "USER_", indexes = {
    @Index(name = "IDX_USER__ON_USERNAME", columnList = "USERNAME", unique = true)
})
public class User implements JmixUserDetails, HasTimeZone {

    @Id
    @Column(name = "ID", nullable = false)
    @JmixGeneratedValue
    private UUID id;

    @Column(name = "IS_ADMIN")
    private Boolean isAdmin;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version;

    @Column(name = "USERNAME", nullable = false)
    private String username;

    @Secret
    @SystemLevel
    @Column(name = "PASSWORD")
    private String password;

    @Column(name = "FIRST_NAME")
    private String firstName;

    @Column(name = "LAST_NAME")
    private String lastName;

    @Email
    @Column(name = "EMAIL")
    private String email;

    @Column(name = "GIT_LOGIN")
    private String gitLogin;

    @Column(name = "ACTIVE")
    private Boolean active = true;

    @Column(name = "TIME_ZONE_ID")
    private String timeZoneId;

    @Column(name = "LLM_AVAILABLE")
    private Boolean llmAvailable;

    @Transient
    private Collection<GrantedAuthority> userAuthorities;

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return userAuthorities != null ? userAuthorities : new ArrayList<>();
    }

    public void setAuthorities(Collection<GrantedAuthority> authorities) {
        this.userAuthorities = authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active == Boolean.TRUE;
    }

    @InstanceName
    @DependsOnProperties({"firstName", "lastName", "username"})
    public String getDisplayName() {
        String firstName = this.firstName != null ? this.firstName : "";
        String lastName = this.lastName != null ? this.lastName : "";
        String username = this.username != null ? this.username : "";
        return (firstName + " " + lastName + " [" + username + "]").trim();
    }

    @Override
    public String getTimeZoneId() {
        return timeZoneId;
    }

    public void setTimeZoneId(String timeZoneId) {
        this.timeZoneId = timeZoneId;
    }

    @Override
    public boolean isAutoTimeZone() {
        return true;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGitLogin() {
        return gitLogin;
    }

    public void setGitLogin(String gitLogin) {
        this.gitLogin = gitLogin;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getLlmAvailable() {
        return llmAvailable;
    }

    public void setLlmAvailable(Boolean llmAvailable) {
        this.llmAvailable = llmAvailable;
    }
}