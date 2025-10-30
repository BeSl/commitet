package com.company.commitet_jm.view.login;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.login.AbstractLogin.LoginEvent;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import io.jmix.core.CoreProperties;
import io.jmix.core.MessageTools;
import io.jmix.flowui.component.loginform.JmixLoginForm;
import io.jmix.flowui.kit.component.ComponentUtils;
import io.jmix.flowui.kit.component.loginform.JmixLoginI18n;
import io.jmix.flowui.view.*;
import io.jmix.securityflowui.authentication.AuthDetails;
import io.jmix.securityflowui.authentication.LoginViewSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

@Route(value = "login")
@ViewController("LoginView")
@ViewDescriptor("login-view.xml")
public class LoginView extends StandardView implements LocaleChangeObserver {

    private static final Logger log = LoggerFactory.getLogger(LoginView.class);

    @Autowired
    private CoreProperties coreProperties;

    @Autowired
    private LoginViewSupport loginViewSupport;

    @Autowired
    private MessageTools messageTools;

    @ViewComponent
    private JmixLoginForm login;

    @ViewComponent
    private MessageBundle messageBundle;

    //    @Value("${ui.login.defaultUsername:}")
    private String defaultUsername = "";

    //    @Value("${ui.login.defaultPassword:}")
    private String defaultPassword = "";

    @Subscribe
    public void onInit(InitEvent event) {
        initLocales();
        initDefaultCredentials();
    }

    private void initLocales() {
        Map<Locale, String> locales = new HashMap<>();
        for (Locale locale : coreProperties.getAvailableLocales()) {
            locales.put(locale, messageTools.getLocaleDisplayName(locale));
        }

        ComponentUtils.setItemsMap(login, locales);

        login.setSelectedLocale(VaadinSession.getCurrent().getLocale());
    }

    private void initDefaultCredentials() {
        if (defaultUsername != null && !defaultUsername.isBlank()) {
            login.setUsername(defaultUsername);
        }

        if (defaultPassword != null && !defaultPassword.isBlank()) {
            login.setPassword(defaultPassword);
        }
    }

    @Subscribe("login")
    public void onLogin(LoginEvent event) {
        try {
            loginViewSupport.authenticate(
                    AuthDetails.of(event.getUsername(), event.getPassword())
                            .withLocale(login.getSelectedLocale())
                            .withRememberMe(login.isRememberMe())
            );
        } catch (Exception e) {
            log.warn("Login failed for user '{}': {}", event.getUsername(), e.toString());
            event.getSource().setError(true);
        }
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        UI.getCurrent().getPage().setTitle(messageBundle.getMessage("LoginView.title"));

        JmixLoginI18n loginI18n = JmixLoginI18n.createDefault();

        JmixLoginI18n.JmixForm form = new JmixLoginI18n.JmixForm();
        form.setTitle(messageBundle.getMessage("loginForm.headerTitle"));
        form.setUsername(messageBundle.getMessage("loginForm.username"));
        form.setPassword(messageBundle.getMessage("loginForm.password"));
        form.setSubmit(messageBundle.getMessage("loginForm.submit"));
        form.setForgotPassword(messageBundle.getMessage("loginForm.forgotPassword"));
        form.setRememberMe(messageBundle.getMessage("loginForm.rememberMe"));
        loginI18n.setForm(form);

        LoginI18n.ErrorMessage errorMessage = new LoginI18n.ErrorMessage();
        errorMessage.setTitle(messageBundle.getMessage("loginForm.errorTitle"));
        errorMessage.setMessage(messageBundle.getMessage("loginForm.badCredentials"));
        errorMessage.setUsername(messageBundle.getMessage("loginForm.errorUsername"));
        errorMessage.setPassword(messageBundle.getMessage("loginForm.errorPassword"));
        loginI18n.setErrorMessage(errorMessage);

        login.setI18n(loginI18n);
    }
}