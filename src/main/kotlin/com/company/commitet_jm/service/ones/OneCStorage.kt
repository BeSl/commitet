package com.company.commitet_jm.service.ones

import com.company.commitet_jm.component.ShellExecutor
import com.company.commitet_jm.entity.OneCStorage
import com.company.commitet_jm.entity.Platform
import com.company.commitet_jm.service.GitWorker
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths


class OneCStorageService() {

    //runner:OneRunner, executor: ShellExecutor
//    1.Создаём хранилище конфигураций:
//    /ConfigurationRepositoryCreate

//    1cv8.exe designer  /F "D:\1cBase\morpho" /N "Admin" /P "abc" /ConfigurationRepositoryF "D:\Storage\morpho" /ConfigurationRepositoryN "AdminStorage" /ConfigurationRepositoryP "qwerty" /ConfigurationRepositoryCreate

//    2.Добавление ещё одного пользователя
//    /ConfigurationRepositoryAddUser

//    1cv8.exe designer  /F "D:\1cBase\morpho" /N "Admin" /P "abc" /ConfigurationRepositoryF "D:\Storage\morpho" /ConfigurationRepositoryN "AdminStorage" /ConfigurationRepositoryP "qwerty" /ConfigurationRepositoryAddUser -User "DirectorStorage" -Pwd "qwerty" -Rights Administration
    companion object{
        private  val log = LoggerFactory.getLogger(GitWorker::class.java)
        private val oneCRunner = OneRunner
    }
    val executor = ShellExecutor()

    fun createOneCStorage(storageParam: OneCStorage){
        //создать файловую базу,
        //создать хранилище

        //создание макет команды

//        1cv8.exe designer  /F "D:\1cBase\morpho" /N "Admin" /P "abc" /ConfigurationRepositoryF "D:\Storage\morpho" /ConfigurationRepositoryN "AdminStorage" /ConfigurationRepositoryP "qwerty" /ConfigurationRepositoryCreate
//
        log.debug("Начал создавать хранилище ${storageParam.name}")
        log.debug("Создаю временную базу ${storageParam.name}")
        val pathBase = "\"${storageParam!!.project!!.tempBasePath}\\\\${storageParam.name}\\\""
        if (!clearDirectoryNio("${storageParam!!.project!!.tempBasePath}\\${storageParam.name}")){
            throw RuntimeException("Не смог очистить директорию $pathBase")
        }
        var command = listOf(
            pathPlatform(storageParam!!.project!!.platform),
            "CREATEINFOBASE",
            "File="+pathBase
        )
        log.debug("Строка запуска $command")
        var res = executor.executeCommand(command)

        log.debug("Вывод системы $res")
        log.debug("пустая база создана ${storageParam.name}")

        command = listOf(
            pathPlatform(storageParam!!.project!!.platform),
            "DESIGNER",
            "/F",
            pathBase
//            " /LoadConfigFromFiles",
//            " \"${storageParam.project!!.localPath}\\src\\\""
//            , //TODO сделать определение пути для расширений
//            " /UpdateDBCfg",
//            " /DisableStartupMessages"
        )

        log.debug("Строка запуска $command")
        res = executor.executeCommand(command)
        log.debug("Результат запуска 2 $res")
//        LoadConfigFromFiles <каталог загрузки> [­Extension <Имя расширения>] [­AllExtensions] –files “<файлы>” –listFile <файлСписка> ­ Format <режим> [1](https://master1c8.ru/wp-content/uploads/2017/10/%D0%9F%D0%B0%D1%80%D0%B0%D0%BC%D0%B5%D1%82%D1%80%D1%8B%D0%97%D0%B0%D0%BF%D1%83%D1%81%D0%BA%D0%B0.pdf)

    }

    fun pathPlatform(platform: Platform?): String {
        return "${platform!!.pathInstalled}\\${platform!!.version}\\bin\\1cv8.exe"
    }

    fun clearDirectoryNio(directoryPath: String): Boolean {
        val path = Paths.get(directoryPath)

        if (!Files.exists(path) || !Files.isDirectory(path)) {
            log.info("Каталог $directoryPath не существует или это не каталог")
            return false
        }

        return try {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .filter { it != path } // Не удаляем сам корневой каталог
                .forEach { filePath ->
                    Files.deleteIfExists(filePath)
                }
            log.info("Содержимое каталога $directoryPath успешно удалено (NIO)")
            true
        } catch (e: SecurityException) {
            log.error("Ошибка безопасности при удалении содержимого каталога: ${e.message}")
            false
        } catch (e: Exception) {
            log.error("Произошла ошибка при удалении содержимого каталога: ${e.message}")
            false
        }
    }

}