package com.company.commitet_jm.service.ones

//ПРИМЕР ПУТИ после имени диска 2 СЛЭША
//        val pathSource = """"C:\\develop\test\repo\src\""""

import com.company.commitet_jm.component.ShellExecutor
import com.company.commitet_jm.entity.OneCStorage
import com.company.commitet_jm.entity.Platform
import com.company.commitet_jm.service.GitWorker
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths

@Component
class OneCStorageService() {

    companion object{
        private val log = LoggerFactory.getLogger(GitWorker::class.java)
        private val executor = ShellExecutor
    }
//    @Autowired
//    private val executor: ShellExecutor
    val executor = ShellExecutor()

    fun createOneCStorage(storageParam: OneCStorage){
        val pathBase = "\"${storageParam!!.project!!.tempBasePath}\\\\${storageParam.name}\\\""

        log.debug("Начал создавать хранилище ${storageParam.name}")
        log.debug("Создаю временную базу ${storageParam.name}")
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


        val pathSource = "\"${storageParam.project!!.localPath}\\src\""
        command = listOf(
            pathPlatform(storageParam!!.project!!.platform),
            "DESIGNER",
            "/F",
            pathBase,
            "/LoadConfigFromFiles",
            pathSource,
            " /UpdateDBCfg"
        )

        log.debug("Строка запуска $command")
        res = executor.executeCommand(command)
        log.debug("Результат запуска 2 $res")
//        LoadConfigFromFiles <каталог загрузки> [­Extension <Имя расширения>] [­AllExtensions] –files “<файлы>” –listFile <файлСписка> ­ Format
    //        <режим> [1](https://master1c8.ru/wp-content/uploads/2017/10/%D0%9F%D0%B0%D1%80%D0%B0%D0%BC%D0%B5%D1%82%D1%80%D1%8B%D0%97%D0%B0%D0%BF%D1%83%D1%81%D0%BA%D0%B0.pdf)
        command = listOf(
            pathPlatform(storageParam!!.project!!.platform),
            "DESIGNER",
            "/F",
            pathBase,
            "/ConfigurationRepositoryF",
            storageParam!!.path.toString(),
            "/ConfigurationRepositoryN", storageParam!!.user.toString(),
            "/ConfigurationRepositoryP", storageParam!!.password.toString(),
            "/ConfigurationRepositoryCreate"
        )

        log.debug("Строка запуска $command")
        res = executor.executeCommand(command)
        log.debug("Результат запуска 2 $res")

    }

    fun pathPlatform(platform: Platform?): String {
        return "${platform!!.pathInstalled}\\${platform!!.version}\\bin\\1cv8.exe"
    }
// создать нового пользователя
//    /ConfigurationRepositoryAddUser

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