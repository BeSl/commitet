package com.company.commitet_jm.app

import com.company.commitet_jm.service.GitWorker
import io.jmix.core.DataManager
import io.jmix.core.FileStorageLocator
import io.jmix.flowui.backgroundtask.BackgroundTask
import io.jmix.flowui.backgroundtask.TaskLifeCycle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit



class GitCloneTask(
    private val dataManager: DataManager,
    private val fileStorageLocator: FileStorageLocator,
    ) : BackgroundTask<Int, Void?>(
    10,
    TimeUnit.MINUTES
) {

    companion object {
        private  val log = LoggerFactory.getLogger(GitWorker::class.java)
    }
    var urlRepo: String = ""
    var localPath: String = ""
    var defaultBranch: String = ""

    @Throws(Exception::class)
    override fun run(taskLifeCycle: TaskLifeCycle<Int>): Void? {
        val gw = GitWorker(dataManager = dataManager, fileStorageLocator = fileStorageLocator)

        val result = gw.cloneRepo("$urlRepo.git", localPath, defaultBranch)

        if (!result.first) {
            log.error("Ошибка клонирования: ${result.second}")
        } else {
            log.info("репозиторий склонирован")
        }

        return null
    }

}