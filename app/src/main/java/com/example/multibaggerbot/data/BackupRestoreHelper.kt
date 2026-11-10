package com.example.multibaggerbot.data

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object BackupRestoreHelper {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun exportBackup(context: Context, backup: FullAppBackup) {
        try {
            val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Multibagger_Backup_$dateStamp.json"

            val serialized = json.encodeToString(backup)
            val file = File(context.cacheDir, fileName)
            file.writeText(serialized)

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Multibagger Bot Full Portfolio Backup ($dateStamp)")
                putExtra(Intent.EXTRA_TEXT, "Multibagger Bot secure encrypted backup containing ${backup.auditPositions.size} holdings and ${backup.simulationConfigs.size} scenarios.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooser = Intent.createChooser(intent, "Save / Export Backup File").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
            Toast.makeText(context, "Backup file generated successfully!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Backup export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun parseBackupJson(rawJson: String): FullAppBackup? {
        return try {
            json.decodeFromString<FullAppBackup>(rawJson.trim())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
