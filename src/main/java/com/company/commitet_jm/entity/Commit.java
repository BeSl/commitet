package com.company.commitet_jm.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.Composition;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.data.DbView;
import io.jmix.eclipselink.lazyloading.NotInstantiatedList;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@DbView
@JmixEntity
@Table(name = "COMMIT_", indexes = {
    @Index(name = "IDX_COMMIT__AUTHOR", columnList = "AUTHOR_ID"),
    @Index(name = "IDX_COMMIT__PROJECT", columnList = "PROJECT_ID")
})
@Entity(name = "Commit_")
public class Commit {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @JoinColumn(name = "AUTHOR_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private User author;

    @Column(name = "HASH_COMMIT", columnDefinition = "хэш созданного коммита")
    private String hashCommit;

    @Column(name = "DATE_CREATED", columnDefinition = "дата создания коммита")
    private LocalDateTime dateCreated;

    @Column(name = "URL_BRANCH")
    private String urlBranch;

    @JoinColumn(name = "PROJECT_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @Column(name = "TASK_NUM")
    private String taskNum;

    @Composition
    @OneToMany(mappedBy = "commit")
    private List<FileCommit> files = new NotInstantiatedList<>();

    @Column(name = "FIX_COMMIT")
    private Boolean fixCommit;

    @Column(name = "STATUS")
    private String status;

    @NotNull(message = "обязательно к заполнению")
    @InstanceName
    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "ERROR_INFO")
    private String errorInfo;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public String getHashCommit() {
        return hashCommit;
    }

    public void setHashCommit(String hashCommit) {
        this.hashCommit = hashCommit;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getUrlBranch() {
        return urlBranch;
    }

    public void setUrlBranch(String urlBranch) {
        this.urlBranch = urlBranch;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getTaskNum() {
        return taskNum;
    }

    public void setTaskNum(String taskNum) {
        this.taskNum = taskNum;
    }

    public List<FileCommit> getFiles() {
        return files;
    }

    public void setFiles(List<FileCommit> files) {
        this.files = files;
    }

    public Boolean getFixCommit() {
        return fixCommit;
    }

    public void setFixCommit(Boolean fixCommit) {
        this.fixCommit = fixCommit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }

    public StatusSheduler getStatusEnum() {
        return status != null ? StatusSheduler.fromId(status) : null;
    }

    public void setStatus(StatusSheduler status) {
        this.status = status != null ? status.getId() : null;
    }
}