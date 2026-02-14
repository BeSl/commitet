package com.company.commitet_jm.service.ones

import java.io.File

interface OneCService {
    fun uploadExtFiles(inputFile: File, outDir: String, pathInstall: String, version: String)
    fun unpackExtFiles(inputFile: File, outDir: String, pathInstall: String, version: String)
    fun pathPlatform(basePath: String?, version: String?): String
}