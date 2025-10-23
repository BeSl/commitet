package com.company.commitet_jm.sheduledJob

import com.company.commitet_jm.service.git.GitService
import io.jmix.core.DataManager
import io.jmix.core.FileStorageLocator
import io.jmix.flowui.backgroundtask.BackgroundTask
import io.jmix.flowui.backgroundtask.TaskLifeCycle
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit



class GitCloneTask(
    private val dataManager: DataManager,
    private val fileStorageLocator: FileStorageLocator,
    private val gitService: GitService
    ) : BackgroundTask<Int, Void?>(
    10,
    TimeUnit.MINUTES
) {

    companion object {
        private  val log = LoggerFactory.getLogger(GitCloneTask::class.java)
    }
    var urlRepo: String = ""
    var localPath: String = ""
    var defaultBranch: String = ""

    @Throws(Exception::class)
    override fun run(taskLifeCycle: TaskLifeCycle<Int>): Void? {
        val result = gitService.cloneRepo("$urlRepo.git", localPath, defaultBranch)

        if (!result.first) {
            log.error("Ошибка клонирования: ${result.second}")
        } else {
            log.info("репозиторий склонирован")
        }

        return null
    }

}