package com.company.commitet_jm.view.onecstorage;

import com.company.commitet_jm.component.CommandFunction;
import com.company.commitet_jm.entity.OneCStorage;
import com.company.commitet_jm.service.ones.OneCStorageService;
import com.company.commitet_jm.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Arrays;

@Route(value = "one-c-storages/:id", layout = MainView.class)
@ViewController(id = "OneCStorage.detail")
@ViewDescriptor(path = "one-c-storage-detail-view.xml")
@EditedEntityContainer("oneCStorageDc")
public class OneCStorageDetailView extends StandardDetailView<OneCStorage> {

    @ViewComponent
    private VerticalLayout notCommandBox;
    
    @ViewComponent
    private VerticalLayout createStorageBox;
    
    @ViewComponent
    private VerticalLayout addUserStorageBox;
    
    @ViewComponent
    private VerticalLayout copyUserStorageBox;
    
    @ViewComponent
    private VerticalLayout historyStorageBox;
    
    @ViewComponent
    private VerticalLayout cmd_param;
    
    @ViewComponent
    private Button executeCommandButton;

    @ViewComponent
    private ComboBox<String> userRightsField;

    @ViewComponent
    private ComboBox<String> reportFormatField;

    private CommandFunction selectedCommand = null;

    @Autowired
    private OneCStorageService storage;

    @Subscribe
    private void onInit(InitEvent event) {
        // Инициализация полей
        userRightsField.setItems("READ_ONLY", "FULL_ACCESS", "VERSION_MANAGEMENT");
        reportFormatField.setItems("TXT", "MXL");
    }

    @Subscribe(id = "createStorageButtonClick", subject = "clickListener")
    private void onCreateStorageButtonClickClick(ClickEvent<JmixButton> event) {
        changeVisibleLayout(createStorageBox, () -> {
            storage.createOneCStorage(getEditedEntity());
        });
    }

    @Subscribe(id = "historyStorageButton", subject = "clickListener")
    private void historyStorageButtonClick(ClickEvent<JmixButton> event) {
        changeVisibleLayout(historyStorageBox, this::historyStorage);
    }

    @Subscribe(id = "addStorageUseButton", subject = "clickListener")
    private void addStorageUseButtonClickClick(ClickEvent<JmixButton> event) {
        changeVisibleLayout(addUserStorageBox, this::addUserStorage);
    }

    @Subscribe(id = "copyUsersStorageButton", subject = "clickListener")
    private void copyUsersStorageButtonClick(ClickEvent<JmixButton> event) {
        changeVisibleLayout(copyUserStorageBox, this::copyUsersStorage);
    }

    public void changeVisibleLayout(VerticalLayout visCompoment, CommandFunction cmd) {
        cmd_param.getChildren().forEach(component -> {
            if (component != visCompoment) {
                component.setVisible(false);
            }
        });
        visCompoment.setVisible(true);
        selectedCommand = cmd;
        executeCommandButton.setVisible(true);
    }

    @Subscribe(id = "executeCommandButton", subject = "clickListener")
    private void onExecuteCommandButtonClick(ClickEvent<JmixButton> event) {
        try {
            if (selectedCommand != null) {
                selectedCommand.invoke();
            }
        } catch (ValidationException e) {
            // showNotification(e.getMessage(), NotificationVariant.LUMO_ERROR);
        } catch (Exception e) {
            // showNotification("Ошибка выполнения: " + e.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    public void historyStorage() {
        // Implementation goes here
    }
    
    public void addUserStorage() {
        // Implementation goes here
    }
    
    public void copyUsersStorage() {
        // Implementation goes here
    }

    // Вспомогательные классы
    public static class HistoryOptions {
        private Integer startVersion;
        private Integer endVersion;
        private LocalDate startDate;
        private LocalDate endDate;
        private String format;

        public HistoryOptions() {
            this.format = "TXT";
        }

        public HistoryOptions(Integer startVersion, Integer endVersion, LocalDate startDate, LocalDate endDate, String format) {
            this.startVersion = startVersion;
            this.endVersion = endVersion;
            this.startDate = startDate;
            this.endDate = endDate;
            this.format = format != null ? format : "TXT";
        }

        // Getters and setters
        public Integer getStartVersion() {
            return startVersion;
        }

        public void setStartVersion(Integer startVersion) {
            this.startVersion = startVersion;
        }

        public Integer getEndVersion() {
            return endVersion;
        }

        public void setEndVersion(Integer endVersion) {
            this.endVersion = endVersion;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }

    public enum UserRights {
        READ_ONLY,
        FULL_ACCESS,
        VERSION_MANAGEMENT
    }
}