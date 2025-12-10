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
    private val gitService: GitService,
    var urlRepo: String = "",
    var localPath: String = "",
    var defaultBranch: String = ""
    ) : BackgroundTask<Int, Void>(
    10,
    TimeUnit.MINUTES
) {

    companion object {
        private  val log = LoggerFactory.getLogger(GitCloneTask::class.java)
    }
    @Throws(Exception::class)
    override fun run(taskLifeCycle: TaskLifeCycle<Int>): Void {
        // Клонирование репозитория

        var fullUrlRepo = urlRepo
        if (!fullUrlRepo.endsWith(".git")) {
            fullUrlRepo += ".git"
        }

        log.info("Клонирование репозитория $fullUrlRepo в $localPath")

        val result = gitService.cloneRepo(fullUrlRepo, localPath, defaultBranch)

        if (!result.first) {
            log.error("Ошибка клонирования: ${result.second}")

            throw RuntimeException(result.second)
        } else {
            log.info("репозиторий склонирован")
        }

        // Проверка на отмену задачи
        if (taskLifeCycle.isCancelled) {
            return null!!
        }

        return null!!
    }



}