package com.company.commitet_jm.entity

import io.jmix.core.DeletePolicy
import io.jmix.core.entity.annotation.JmixGeneratedValue
import io.jmix.core.entity.annotation.OnDeleteInverse
import io.jmix.core.metamodel.annotation.JmixEntity
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@JmixEntity
@Table(name = "USER_EXTERNAL_ID", indexes = [
    Index(name = "IDX_USER_EXTERNAL_ID_VALUE", columnList = "EXTERNAL_ID", unique = true),
    Index(name = "IDX_USER_EXTERNAL_ID_USER", columnList = "USER_ID")
])
@Entity
open class UserExternalId {
    @Id
    @Column(name = "ID", nullable = false)
    @JmixGeneratedValue
    var id: UUID? = null

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @JoinColumn(name = "USER_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    var user: User? = null

    @Column(name = "EXTERNAL_ID", nullable = false, unique = true)
    var externalId: String? = null

    @Column(name = "SOURCE")
    var source: String? = null

    @Column(name = "DESCRIPTION")
    var description: String? = null

    @Column(name = "DATE_CREATED")
    var dateCreated: LocalDateTime? = null
}
