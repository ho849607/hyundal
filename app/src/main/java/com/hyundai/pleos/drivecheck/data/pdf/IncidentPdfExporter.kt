package com.hyundai.pleos.drivecheck.data.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.hyundai.pleos.drivecheck.domain.model.*
import com.hyundai.pleos.drivecheck.domain.reducer.DriveCheckUiState
import java.io.File
import java.io.FileOutputStream

/**
 * M3: Generates a real PDF file for the incident report and invokes Android's system share sheet.
 * Strictly follows Rule 5 & Rule 6:
 * - Clearly labels simulation/mock sources.
 * - Filters sections according to ReportExportConfig (voice transcript is OFF by default).
 * - Preserves original events and appends user correction memos separately.
 */
object IncidentPdfExporter {

    fun generateIncidentPdf(context: Context, state: DriveCheckUiState, versionNumber: Int): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outFile = File(reportsDir, "${state.incident.incidentId}_report_v$versionNumber.pdf")

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(18, 41, 54)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(65, 95, 112)
            textSize = 11f
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(25, 68, 87)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 45, 56)
            textSize = 10.5f
        }
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(169, 28, 60)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var y = 48f
        canvas.drawText("DriveCheck 사건 대응 보고서 v$versionNumber", 40f, y, titlePaint)
        y += 22f
        canvas.drawText(
            "사건 ID: ${state.incident.incidentId}  |  발생 시각: ${state.incident.startedAtKst}  |  지역: ${state.incident.summaryRegion}  |  구분: 시연 기록 (모의 데이터 포함)",
            40f,
            y,
            subPaint
        )
        y += 18f
        canvas.drawText(
            "※ 주의: 공모전 시연 환경에서 생성된 보고서이며, 실제 119 발신 및 차량 제어 없이 모의 출처가 명시되었습니다.",
            40f,
            y,
            badgePaint
        )
        y += 26f

        val cfg = state.exportConfig

        if (cfg.includeTimeAndLocation) {
            canvas.drawText("1. 사건 시각 및 차량 위치 (출처: ${state.location.originMode.badgeText})", 40f, y, sectionPaint)
            y += 18f
            canvas.drawText(
                "• 위치 상태: ${state.location.status.name}  |  확인 시각: ${state.location.observedAtText}  |  정확도: ±${state.location.accuracyMeters}m",
                48f,
                y,
                bodyPaint
            )
            y += 16f
            canvas.drawText(
                "• 변환 주소: ${state.location.resolvedAddress ?: "주소 미변환"} (${state.location.latitude}, ${state.location.longitude})",
                48f,
                y,
                bodyPaint
            )
            y += 24f
        }

        canvas.drawText("2. 시간순 사건 기록 (원본 보존)", 40f, y, sectionPaint)
        y += 18f

        val filteredEvents = state.events.filter { ev ->
            when (ev.source) {
                EventSource.VEHICLE_SENSOR, EventSource.VEHICLE_STATE -> cfg.includeSensorAndVehicleActions
                EventSource.CALL_GATEWAY, EventSource.GUARDIAN_GATEWAY -> cfg.includeCallAndAlertResults
                EventSource.APP_SYSTEM -> cfg.includeTimeAndLocation || cfg.includeCallAndAlertResults
                EventSource.USER_SPEECH, EventSource.USER_TOUCH, EventSource.SIMULATION_INJECTOR -> true
            }
        }

        filteredEvents.forEach { ev ->
            if (y > 740f) return@forEach
            val simTag = if (ev.isSimulated) "[모의]" else "[실측]"
            canvas.drawText(
                "${ev.observedAt}  |  ${ev.source.korLabel} $simTag  |  ${ev.kind}  →  ${ev.outcome}",
                48f,
                y,
                bodyPaint
            )
            y += 16f
        }
        y += 12f

        if (cfg.includePassengerVoiceTranscript) {
            canvas.drawText("3. 확정 음성 자막 기록 (사용자 선택 포함됨)", 40f, y, sectionPaint)
            y += 18f
            state.utterances.forEach { utt ->
                if (y > 760f) return@forEach
                canvas.drawText(
                    "${utt.observedAt} · ${utt.speakerRole.displayLabel}: “${utt.finalizedText}”",
                    48f,
                    y,
                    bodyPaint
                )
                y += 16f
            }
            y += 12f
        } else {
            canvas.drawText("3. 확정 음성 자막 기록: 기본 해제됨 (미포함)", 40f, y, subPaint)
            y += 20f
        }

        if (state.correctionMemos.isNotEmpty()) {
            canvas.drawText("4. 사후 사용자 정정 메모 (원본 이벤트와 별도 보관)", 40f, y, sectionPaint)
            y += 18f
            state.correctionMemos.forEach { memo ->
                if (y > 790f) return@forEach
                canvas.drawText("${memo.createdAt} · ${memo.memoText}", 48f, y, bodyPaint)
                y += 16f
            }
        }

        pdfDocument.finishPage(page)
        FileOutputStream(outFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return outFile
    }

    fun launchSystemSharePdf(context: Context, pdfFile: File, incidentId: String): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "DriveCheck 사건 대응 보고서 ($incidentId)")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "DriveCheck 사건 대응 보고서 ($incidentId) 파일입니다. [시연 모의 데이터 포함]"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "사건 대응 보고서 PDF 공유").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
