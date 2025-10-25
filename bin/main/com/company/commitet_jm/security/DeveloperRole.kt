package com.company.commitet_jm.security

import com.company.commitet_jm.entity.Commit
import com.company.commitet_jm.entity.FileCommit
import com.company.commitet_jm.entity.Project
import com.company.commitet_jm.entity.User
import io.jmix.security.model.EntityAttributePolicyAction
import io.jmix.security.model.EntityPolicyAction
import io.jmix.security.role.annotation.*
import io.jmix.securityflowui.role.annotation.MenuPolicy
import io.jmix.securityflowui.role.annotation.ViewPolicy

@ResourceRole(name = "Developer", code = DeveloperRole.CODE)
interface DeveloperRole {

    companion object {
        const val CODE = "developer"
    }

    @EntityAttributePolicyContainer(
        EntityAttributePolicy(
            entityClass = Commit::class,
            attributes = ["*"],
            action = EntityAttributePolicyAction.MODIFY
        )
    )
    @EntityPolicy(entityClass = Commit::class, actions = [EntityPolicyAction.ALL])
    fun commit()

    @EntityAttributePolicyContainer(
        EntityAttributePolicy(
            entityClass = FileCommit::class,
            attributes = ["id"],
            action = EntityAttributePolicyAction.VIEW
        ),
        EntityAttributePolicy(
            entityClass = FileCommit::class,
            attributes = ["name", "data", "commit", "type"],
            action = EntityAttributePolicyAction.MODIFY
        )
    )
    @EntityPolicy(entityClass = FileCommit::class, actions = [EntityPolicyAction.CREATE, EntityPolicyAction.READ])
    fun fileCommit()

    @EntityAttributePolicyContainer(
        EntityAttributePolicy(
            entityClass = Project::class,
            attributes = ["*"],
            action = EntityAttributePolicyAction.VIEW
        )
    )
    @EntityPolicy(entityClass = Project::class, actions = [EntityPolicyAction.READ])
    fun project()

    @EntityAttributePolicyContainer(
        EntityAttributePolicy(
            entityClass = User::class,
            attributes = ["isAdmin", "active"],
            action = EntityAttributePolicyAction.VIEW
        ),
        EntityAttributePolicy(
            entityClass = User::class,
            attributes = ["id", "version", "username", "password", "firstName", "lastName", "email", "gitLogin", "timeZoneId"],
            action = EntityAttributePolicyAction.MODIFY
        )
    )
    @EntityPolicy(entityClass = User::class, actions = [EntityPolicyAction.READ, EntityPolicyAction.UPDATE])
    fun user()

    @MenuPolicy(menuIds = ["Commit_.list", "User.list"])
    @ViewPolicy(viewIds = ["Commit_.list", "Project.list", "Project.listSelect", "FileCommit.detail", "User.list", "User.detail", "changePasswordView", "resetPasswordView"])
    fun screens()

    @SpecificPolicy(resources = ["*"])
    fun specific()

}