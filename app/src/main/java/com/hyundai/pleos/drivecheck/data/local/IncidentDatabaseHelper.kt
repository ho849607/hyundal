package com.hyundai.pleos.drivecheck.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.hyundai.pleos.drivecheck.domain.model.*
import com.hyundai.pleos.drivecheck.domain.reducer.defaultSeededDemoEvents
import com.hyundai.pleos.drivecheck.domain.reducer.defaultSeededUtterances

/**
 * M3: Local SQLite persistence for Incident, IncidentEvent, UtteranceRecord, CorrectionMemo, and ReportExportVersion.
 * Preserves immutable original events and keeps correction memos and utterance records in separate tables (Rule 5 & Rule 6).
 */
class IncidentDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE incidents (
                incident_id TEXT PRIMARY KEY,
                started_at_kst TEXT NOT NULL,
                summary_region TEXT NOT NULL,
                is_simulation INTEGER NOT NULL,
                phase TEXT NOT NULL,
                driver_consciousness INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE incident_events (
                seq_id INTEGER PRIMARY KEY AUTOINCREMENT,
                event_id TEXT NOT NULL,
                incident_id TEXT NOT NULL,
                source TEXT NOT NULL,
                kind TEXT NOT NULL,
                observed_at TEXT NOT NULL,
                received_at TEXT NOT NULL,
                outcome TEXT NOT NULL,
                is_simulated INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE utterance_records (
                seq_id INTEGER PRIMARY KEY AUTOINCREMENT,
                utterance_id TEXT NOT NULL,
                incident_id TEXT NOT NULL,
                observed_at TEXT NOT NULL,
                speaker_role TEXT NOT NULL,
                finalized_text TEXT NOT NULL,
                is_simulated INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE correction_memos (
                seq_id INTEGER PRIMARY KEY AUTOINCREMENT,
                memo_id TEXT NOT NULL,
                incident_id TEXT NOT NULL,
                created_at TEXT NOT NULL,
                memo_text TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE report_exports (
                version_number INTEGER PRIMARY KEY,
                incident_id TEXT NOT NULL,
                file_path TEXT NOT NULL,
                created_at TEXT NOT NULL,
                share_status TEXT NOT NULL
            )
            """.trimIndent()
        )

        seedDefaultIncidentIfEmpty(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS report_exports")
        db.execSQL("DROP TABLE IF EXISTS correction_memos")
        db.execSQL("DROP TABLE IF EXISTS utterance_records")
        db.execSQL("DROP TABLE IF EXISTS incident_events")
        db.execSQL("DROP TABLE IF EXISTS incidents")
        onCreate(db)
    }

    private fun seedDefaultIncidentIfEmpty(db: SQLiteDatabase) {
        val defaultIncident = Incident()
        val cv = ContentValues().apply {
            put("incident_id", defaultIncident.incidentId)
            put("started_at_kst", defaultIncident.startedAtKst)
            put("summary_region", defaultIncident.summaryRegion)
            put("is_simulation", if (defaultIncident.isSimulationRecord) 1 else 0)
            put("phase", defaultIncident.phase.name)
            putNull("driver_consciousness")
        }
        db.insertWithOnConflict("incidents", null, cv, SQLiteDatabase.CONFLICT_REPLACE)

        defaultSeededDemoEvents().forEach { ev ->
            insertEventInternal(db, ev)
        }
        defaultSeededUtterances().forEach { utt ->
            insertUtteranceInternal(db, utt)
        }
    }

    private fun insertEventInternal(db: SQLiteDatabase, event: IncidentEvent) {
        val cv = ContentValues().apply {
            put("event_id", event.eventId)
            put("incident_id", event.incidentId)
            put("source", event.source.name)
            put("kind", event.kind)
            put("observed_at", event.observedAt)
            put("received_at", event.receivedAt)
            put("outcome", event.outcome)
            put("is_simulated", if (event.isSimulated) 1 else 0)
        }
        db.insert("incident_events", null, cv)
    }

    private fun insertUtteranceInternal(db: SQLiteDatabase, utt: UtteranceRecord) {
        val cv = ContentValues().apply {
            put("utterance_id", utt.utteranceId)
            put("incident_id", utt.incidentId)
            put("observed_at", utt.observedAt)
            put("speaker_role", utt.speakerRole.name)
            put("finalized_text", utt.finalizedText)
            put("is_simulated", if (utt.isSimulated) 1 else 0)
        }
        db.insert("utterance_records", null, cv)
    }

    fun syncStateToDatabase(
        incident: Incident,
        events: List<IncidentEvent>,
        utterances: List<UtteranceRecord>,
        memos: List<CorrectionMemo>,
        exports: List<ReportExportVersion>
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put("incident_id", incident.incidentId)
                put("started_at_kst", incident.startedAtKst)
                put("summary_region", incident.summaryRegion)
                put("is_simulation", if (incident.isSimulationRecord) 1 else 0)
                put("phase", incident.phase.name)
                when (incident.driverConsciousnessConfirmed) {
                    null -> putNull("driver_consciousness")
                    true -> put("driver_consciousness", 1)
                    false -> put("driver_consciousness", 0)
                }
            }
            db.insertWithOnConflict("incidents", null, cv, SQLiteDatabase.CONFLICT_REPLACE)

            db.delete("incident_events", "incident_id = ?", arrayOf(incident.incidentId))
            events.forEach { insertEventInternal(db, it) }

            db.delete("utterance_records", "incident_id = ?", arrayOf(incident.incidentId))
            utterances.forEach { insertUtteranceInternal(db, it) }

            db.delete("correction_memos", "incident_id = ?", arrayOf(incident.incidentId))
            memos.forEach { memo ->
                val mcv = ContentValues().apply {
                    put("memo_id", memo.memoId)
                    put("incident_id", memo.incidentId)
                    put("created_at", memo.createdAt)
                    put("memo_text", memo.memoText)
                }
                db.insert("correction_memos", null, mcv)
            }

            db.delete("report_exports", "incident_id = ?", arrayOf(incident.incidentId))
            exports.forEach { exp ->
                val ecv = ContentValues().apply {
                    put("version_number", exp.versionNumber)
                    put("incident_id", exp.incidentId)
                    put("file_path", exp.filePath)
                    put("created_at", exp.createdAt)
                    put("share_status", exp.shareStatus)
                }
                db.insertWithOnConflict("report_exports", null, ecv, SQLiteDatabase.CONFLICT_REPLACE)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun loadEvents(incidentId: String): List<IncidentEvent> {
        val list = mutableListOf<IncidentEvent>()
        val db = readableDatabase
        db.query(
            "incident_events",
            null,
            "incident_id = ?",
            arrayOf(incidentId),
            null,
            null,
            "seq_id ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val sourceName = cursor.getString(cursor.getColumnIndexOrThrow("source"))
                val source = runCatching { EventSource.valueOf(sourceName) }.getOrDefault(EventSource.APP_SYSTEM)
                list.add(
                    IncidentEvent(
                        eventId = cursor.getString(cursor.getColumnIndexOrThrow("event_id")),
                        incidentId = cursor.getString(cursor.getColumnIndexOrThrow("incident_id")),
                        source = source,
                        kind = cursor.getString(cursor.getColumnIndexOrThrow("kind")),
                        observedAt = cursor.getString(cursor.getColumnIndexOrThrow("observed_at")),
                        receivedAt = cursor.getString(cursor.getColumnIndexOrThrow("received_at")),
                        outcome = cursor.getString(cursor.getColumnIndexOrThrow("outcome")),
                        isSimulated = cursor.getInt(cursor.getColumnIndexOrThrow("is_simulated")) == 1
                    )
                )
            }
        }
        return if (list.isEmpty()) defaultSeededDemoEvents() else list
    }

    fun loadUtterances(incidentId: String): List<UtteranceRecord> {
        val list = mutableListOf<UtteranceRecord>()
        val db = readableDatabase
        db.query(
            "utterance_records",
            null,
            "incident_id = ?",
            arrayOf(incidentId),
            null,
            null,
            "seq_id ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val roleName = cursor.getString(cursor.getColumnIndexOrThrow("speaker_role"))
                val role = runCatching { SpeakerRole.valueOf(roleName) }.getOrDefault(SpeakerRole.UNKNOWN_SPEAKER)
                list.add(
                    UtteranceRecord(
                        utteranceId = cursor.getString(cursor.getColumnIndexOrThrow("utterance_id")),
                        incidentId = cursor.getString(cursor.getColumnIndexOrThrow("incident_id")),
                        observedAt = cursor.getString(cursor.getColumnIndexOrThrow("observed_at")),
                        speakerRole = role,
                        finalizedText = cursor.getString(cursor.getColumnIndexOrThrow("finalized_text")),
                        isSimulated = cursor.getInt(cursor.getColumnIndexOrThrow("is_simulated")) == 1
                    )
                )
            }
        }
        return if (list.isEmpty()) defaultSeededUtterances() else list
    }

    fun loadCorrectionMemos(incidentId: String): List<CorrectionMemo> {
        val list = mutableListOf<CorrectionMemo>()
        val db = readableDatabase
        db.query(
            "correction_memos",
            null,
            "incident_id = ?",
            arrayOf(incidentId),
            null,
            null,
            "seq_id ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    CorrectionMemo(
                        memoId = cursor.getString(cursor.getColumnIndexOrThrow("memo_id")),
                        incidentId = cursor.getString(cursor.getColumnIndexOrThrow("incident_id")),
                        createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at")),
                        memoText = cursor.getString(cursor.getColumnIndexOrThrow("memo_text"))
                    )
                )
            }
        }
        return list
    }

    fun loadExports(incidentId: String): List<ReportExportVersion> {
        val list = mutableListOf<ReportExportVersion>()
        val db = readableDatabase
        db.query(
            "report_exports",
            null,
            "incident_id = ?",
            arrayOf(incidentId),
            null,
            null,
            "version_number ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    ReportExportVersion(
                        versionNumber = cursor.getInt(cursor.getColumnIndexOrThrow("version_number")),
                        incidentId = cursor.getString(cursor.getColumnIndexOrThrow("incident_id")),
                        filePath = cursor.getString(cursor.getColumnIndexOrThrow("file_path")),
                        createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at")),
                        shareStatus = cursor.getString(cursor.getColumnIndexOrThrow("share_status"))
                    )
                )
            }
        }
        return list
    }

    companion object {
        private const val DATABASE_NAME = "drivecheck_incidents.db"
        private const val DATABASE_VERSION = 1
    }
}
