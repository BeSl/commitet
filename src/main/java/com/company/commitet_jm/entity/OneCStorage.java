package com.company.commitet_jm.entity;

import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import java.util.UUID;

@JmixEntity
@Table(name = "ONE_C_STORAGE", indexes = {
    @Index(name = "IDX_ONE_C_STORAGE_PROJECT", columnList = "PROJECT_ID")
})
@Entity
public class OneCStorage {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @InstanceName
    @Column(name = "NAME", length = 350)
    private String name;

    @Column(name = "USER_")
    private String user;

    @Column(name = "PASSWORD")
    private String password;

    @Column(name = "PATH", length = 350)
    private String path;

    @Column(name = "BRANCH")
    private String branch;

    @Column(name = "TYPE_")
    private Integer type;

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @JoinColumn(name = "PROJECT_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Project project;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public TypeStorage getTypeEnum() {
        return type != null ? TypeStorage.fromId(type) : null;
    }

    public void setType(TypeStorage type) {
        this.type = type != null ? type.getId() : null;
    }
}