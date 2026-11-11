package com.hyundai.pleos.drivecheck

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.hyundai.pleos.drivecheck.domain.model.LocationStatus
import com.hyundai.pleos.drivecheck.domain.model.RecordSubView
import com.hyundai.pleos.drivecheck.domain.model.SpeakerRole
import com.hyundai.pleos.drivecheck.domain.reducer.IncidentAction
import com.hyundai.pleos.drivecheck.ui.screens.DriveCheckMainScreen
import com.hyundai.pleos.drivecheck.ui.theme.DriveCheckTheme
import com.hyundai.pleos.drivecheck.ui.viewmodel.DriveCheckViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DriveCheckViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDemoIntent(intent)

        setContent {
            DriveCheckTheme {
                DriveCheckMainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDemoIntent(intent)
    }

    private fun handleDemoIntent(intent: Intent?) {
        val step = intent?.getStringExtra("demo_step") ?: return
        when (step) {
            "home" -> viewModel.dispatch(IncidentAction.ResetToDemoStart)
            "listen" -> viewModel.dispatch(IncidentAction.StartVoiceListening())
            "child_report" -> viewModel.dispatch(
                IncidentAction.PassengerReportedUnresponsive(
                    statement = "운전자가 반응하지 않아요.",
                    speakerRole = SpeakerRole.CHILD_PASSENGER_ESTIMATED
                )
            )
            "check" -> viewModel.dispatch(IncidentAction.TriggerAmbiguousHelpRequest())
            "crash" -> viewModel.dispatch(IncidentAction.TriggerCrashSignalSimulation())
            "no_reply_1" -> viewModel.dispatch(IncidentAction.FirstCheckNoReply())
            "no_reply_2" -> viewModel.dispatch(IncidentAction.SecondCheckNoReply())
            "dial_119" -> viewModel.dispatch(IncidentAction.Request119Call(isDirectFromIdle = true))
            "connected_location" -> {
                viewModel.updateLocationSimulation(LocationStatus.AVAILABLE)
                viewModel.dispatch(IncidentAction.CallConnectedConfirmed())
            }
            "stale_location" -> {
                viewModel.updateLocationSimulation(LocationStatus.STALE, observedAt = "14:30")
                viewModel.dispatch(IncidentAction.CallConnectedConfirmed())
            }
            "report" -> viewModel.dispatch(IncidentAction.OpenRecordView(RecordSubView.VEHICLE_REPORT_TABLE))
            "export_config" -> viewModel.dispatch(IncidentAction.OpenRecordView(RecordSubView.EXPORT_PDF_CONFIG))
            "export_pdf" -> viewModel.exportAndPreparePdf(this, launchShareChooser = false)
        }
    }
}
