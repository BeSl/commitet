package com.company.commitet_jm.entity;

import io.jmix.core.DeletePolicy;
import io.jmix.core.FileRef;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import java.util.UUID;

@JmixEntity
@Table(name = "FILE_COMMIT", indexes = {
    @Index(name = "IDX_FILE_COMMIT_COMMIT", columnList = "COMMIT_ID")
})
@Entity
public class FileCommit {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @InstanceName
    @Column(name = "NAME")
    private String name;

    @Column(name = "DATA_", length = 4096)
    private FileRef data;

    @Column(name = "TYPE_")
    private String type;

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @JoinColumn(name = "COMMIT_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Commit commit;

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

    public FileRef getData() {
        return data;
    }

    public void setData(FileRef data) {
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Commit getCommit() {
        return commit;
    }

    public void setCommit(Commit commit) {
        this.commit = commit;
    }

    public TypesFiles getTypeEnum() {
        return type != null ? TypesFiles.fromId(type) : null;
    }

    public void setType(TypesFiles type) {
        this.type = type != null ? type.getId() : null;
    }
}