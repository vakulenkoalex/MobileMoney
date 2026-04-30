package com.mobilemoney.data.repository

import android.content.Context
import android.net.Uri
import com.mobilemoney.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRepository(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())

    suspend fun export(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            AppDatabase.closeDatabase()
            delay(500)

            val dbFile = getDatabaseFile()
            if (!dbFile.exists()) {
                return@withContext Result.failure(Exception("База данных не найдена"))
            }

            val size = dbFile.length()
            if (size == 0L) {
                return@withContext Result.failure(Exception("База данных пустая"))
            }

            context.contentResolver.openOutputStream(uri)?.use { output ->
                FileInputStream(dbFile).use { input ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Не удалось сохранить файл"))

            Result.success("Экспорт завершён. Размер: ${size / 1024} KB")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun import(sourceUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            AppDatabase.closeDatabase()

            val tempFile = File(context.cacheDir, "temp_restore.db")
            tempFile.delete()

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Не удалось открыть файл"))

            val dbFile = getDatabaseFile()
            dbFile.parentFile?.mkdirs()

            tempFile.copyTo(dbFile, overwrite = true)
            tempFile.delete()

            deleteDatabaseShmFiles(dbFile)

            Result.success("Импорт завершён. Перезапустите приложение.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateFileName(): String {
        return "mobile_money_${dateFormat.format(Date())}.db"
    }

    private fun getDatabaseFile(): File {
        return File(AppDatabase.getDatabasePath(context))
    }

    private fun deleteDatabaseShmFiles(dbFile: File) {
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        File(dbFile.path + "-journal").delete()
    }
}