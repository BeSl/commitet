package com.company.commitet_jm.security;

import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.security.role.annotation.SpecificPolicy;
import io.jmix.securityflowui.role.UiMinimalPolicies;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

@ResourceRole(name = "UI: minimal access", code = UiMinimalRole.CODE, scope = {"UI"})
public interface UiMinimalRole extends UiMinimalPolicies {

    String CODE = "ui-minimal";

    @ViewPolicy(viewIds = {"MainView", "Commit_.detail"})
    void main();

    @ViewPolicy(viewIds = {"LoginView"})
    @SpecificPolicy(resources = {"ui.loginToUi"})
    void login();

    @MenuPolicy(menuIds = {"Commit_.detail"})
    void screens();
}