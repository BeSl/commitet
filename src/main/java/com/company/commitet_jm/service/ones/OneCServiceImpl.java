package com.company.commitet_jm.service.ones;

import com.company.commitet_jm.component.ShellExecutor;
import com.company.commitet_jm.entity.AppSettings;
import io.jmix.core.DataManager;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@Service
public class OneCServiceImpl implements OneCService {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(OneCServiceImpl.class);

    private final DataManager dataManager;
    private final ShellExecutor shellExecutor;

    private String v8unpackPath = "";

    @Autowired
    public OneCServiceImpl(DataManager dataManager, ShellExecutor shellExecutor) {
        this.dataManager = dataManager;
        this.shellExecutor = shellExecutor;
    }

    @Override
    public void uploadExtFiles(File inputFile, String outDir, String pathInstall, String version) {
        String res = shellExecutor.executeCommand(java.util.Arrays.asList(
                pathPlatform(pathInstall, version),
                "DESIGNER",
                "/DumpExternalDataProcessorOrReportToFiles",
                "\"" + outDir + "\"",
                "\"" + inputFile.getPath() + "\""
        ));
        log.debug("Строка запуска " + res);
    }

    @Override
    public void unpackExtFiles(File inputFile, String outDir) {
        if (v8unpackPath.isEmpty()) {
            Optional<AppSettings> unpackPath = dataManager.load(AppSettings.class)
                    .query("select apps from AppSettings apps where apps.name = :pName")
                    .parameter("pName", "v8unpack")
                    .optional();

            if (unpackPath.isPresent()) {
                v8unpackPath = unpackPath.get().getValue();
            }
        }

        String res = shellExecutor.executeCommand(java.util.Arrays.asList(
                v8unpackPath,
                "-U",
                inputFile.getPath(),
                outDir
        ));

        log.info("unpack rename files");

        filterAndRenameFiles(
                new File(outDir),
                Set.of("form.data", "module.data", "Form.bin"),
                originalName -> {
                    switch (originalName) {
                        case "form.data":
                            return "form";
                        case "module.data":
                            return "Module.bsl";
                        default:
                            return originalName;
                    }
                }
        );
        log.debug("Unpack command " + res);
    }

    @Override
    public String pathPlatform(String basePath, String version) {
        return basePath + "\\" + version + "\\bin\\1cv8.exe";
    }

    /**
     * Оставляет в директории только указанные файлы, переименовывает их и удаляет остальные
     *
     * @param directory  Целевая директория
     * @param keepFiles  Список имён файлов для сохранения (регистрозависимый)
     * @param renameRule Функция для генерации нового имени файла на основе старого
     */
    private void filterAndRenameFiles(
            File directory,
            Set<String> keepFiles,
            Function<String, String> renameRule
    ) {
        if (!directory.isDirectory()) return;

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && keepFiles.contains(file.getName())) {
                    String newName = renameRule.apply(file.getName());
                    File newFile = new File(directory, newName);
                    if (!newName.equals(file.getName())) {
                        if (newFile.exists()) newFile.delete();
                        file.renameTo(newFile);
                    }
                } else {
                    if (!file.isDirectory()) {
                        file.delete();
                    }
                }
            }
        }
    }
}