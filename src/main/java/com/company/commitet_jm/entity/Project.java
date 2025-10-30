package com.company.commitet_jm.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.data.DbView;
import io.jmix.eclipselink.lazyloading.NotInstantiatedList;
import jakarta.persistence.*;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@DbView
@JmixEntity
@Table(name = "PROJECT", indexes = {
    @Index(name = "IDX_PROJECT_PLATFORM", columnList = "PLATFORM_ID")
})
@Entity
public class Project {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @Column(name = "ADMIN_GIT_NAME")
    private String adminGitName;

    @Column(name = "ADMIN_GIT_PASSWORD")
    private String adminGitPassword;

    @Column(name = "LOCAL_PATH")
    private String localPath;

    @Column(name = "DEFAULT_BRANCH")
    private String defaultBranch;

    @InstanceName
    @Column(name = "NAME")
    private String name;

    @Column(name = "URL_REPO", length = 500)
    private String urlRepo;

    @Column(name = "VERSION", nullable = false)
    @Version
    private Integer version;

    @JoinColumn(name = "PLATFORM_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private Platform platform;

    @Column(name = "TEMP_BASE_PATH")
    private String tempBasePath;

    @Composition
    @OneToMany(mappedBy = "project")
    private List<OneCStorage> storages = new NotInstantiatedList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAdminGitName() {
        return adminGitName;
    }

    public void setAdminGitName(String adminGitName) {
        this.adminGitName = adminGitName;
    }

    public String getAdminGitPassword() {
        return adminGitPassword;
    }

    public void setAdminGitPassword(String adminGitPassword) {
        this.adminGitPassword = adminGitPassword;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrlRepo() {
        return urlRepo;
    }

    public void setUrlRepo(String urlRepo) {
        this.urlRepo = urlRepo;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public String getTempBasePath() {
        return tempBasePath;
    }

    public void setTempBasePath(String tempBasePath) {
        this.tempBasePath = tempBasePath;
    }

    public List<OneCStorage> getStorages() {
        return storages;
    }

    public void setStorages(List<OneCStorage> storages) {
        this.storages = storages;
    }
}