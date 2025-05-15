package com.company.commitet_jm.service.ones

import com.company.commitet_jm.component.ShellExecutor
import com.company.commitet_jm.entity.OneCStorage
import com.company.commitet_jm.service.GitWorker
import com.company.commitet_jm.service.ones.OneRunner.Companion
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service


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
        val pathBase = "/F\"${storageParam!!.project!!.tempBasePath}\\${storageParam.name} \""
        var command = listOf(
            "${storageParam.project!!.platform!!.pathInstalled}\\${storageParam.project!!.platform!!.version}\\bin\\1cv8.exe",
            "CREATEINFOBASE",
            pathBase
        )
        log.debug("Строка запуска $command")
        var res = executor.executeCommand(command)

        log.debug("Вывод системы $res")
        log.debug("пустая база создана ${storageParam.name}")

        command = listOf(
            "${storageParam.project!!.platform!!.pathInstalled}\\${storageParam.project!!.platform!!.version}\\bin\\1cv8.exe",
            "DESIGNER",
            "/F\"${storageParam!!.project!!.tempBasePath}\\${storageParam.name} \"",

            )

        log.debug("Строка запуска $command")
        res = executor.executeCommand(command)

    }




}