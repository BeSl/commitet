package com.company.commitet_jm.security

import com.company.commitet_jm.entity.Commit
import com.company.commitet_jm.entity.FileCommit
import com.company.commitet_jm.entity.Project
import com.company.commitet_jm.entity.User
import io.jmix.security.model.EntityAttributePolicyAction
import io.jmix.security.model.EntityPolicyAction
import io.jmix.security.role.annotation.EntityAttributePolicy
import io.jmix.security.role.annotation.EntityAttributePolicyContainer
import io.jmix.security.role.annotation.EntityPolicy
import io.jmix.security.role.annotation.ResourceRole
import io.jmix.securityflowui.role.annotation.MenuPolicy
import io.jmix.securityflowui.role.annotation.ViewPolicy

@ResourceRole(name = "Developer", code = DeveloperRole.CODE, scope = ["UI"])
interface DeveloperRole {

    companion object {
        const val CODE = "developer"
    }

    @EntityAttributePolicyContainer(
        EntityAttributePolicy(
            entityClass = Commit::class,
            attributes = ["*"],
            action = EntityAttributePolicyAction.VIEW
        )
    )
    @EntityPolicy(entityClass = Commit::class, actions = [EntityPolicyAction.CREATE, EntityPolicyAction.READ])
    fun commit()

    @EntityAttributePolicyContainer(
        EntityAttributePolicy(
            entityClass = FileCommit::class,
            attributes = ["*"],
            action = EntityAttributePolicyAction.VIEW
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
        EntityAttributePolicy(entityClass = User::class, attributes = ["*"], action = EntityAttributePolicyAction.VIEW)
    )
    @EntityPolicy(entityClass = User::class, actions = [EntityPolicyAction.READ])
    fun user()

    @MenuPolicy(menuIds = ["Commit_.detail"])
    @ViewPolicy(viewIds = ["Commit_.detail"])
    fun screens()
}