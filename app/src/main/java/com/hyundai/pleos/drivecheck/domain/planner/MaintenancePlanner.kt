package com.hyundai.pleos.drivecheck.domain.planner

import com.hyundai.pleos.drivecheck.data.sdk.ReadState
import com.hyundai.pleos.drivecheck.data.sdk.TirePressureRead
import com.hyundai.pleos.drivecheck.data.sdk.WarningLampRead

/**
 * 조회값과 기획 기준을 비교해 정비 시점을 예측합니다.
 * - 관측(조회값)과 분석(예측)을 한 문장에 섞지 않고 분리해 생성합니다.
 * - 15,000km와 보증 D-30은 기획용 기준이며 현대차 공식 주기나 확정된 만료일이 아닙니다.
 */
data class MaintenanceRead(
    val odometerKm: ReadState<Double> = ReadState.NotWired("아직 조회 전 (앞왼쪽 도어 위치 연결 확인 후 순차 조회)"),
    val tirePressure: ReadState<TirePressureRead> = ReadState.NotWired("아직 조회 전 (누적 주행거리 확인 후 조회)"),
    val warningLamps: ReadState<WarningLampRead> = ReadState.NotWired("아직 조회 전 (타이어 공기압 확인 후 조회)"),
    val batterySocPercent: ReadState<Int> = ReadState.NotWired("아직 조회 전 (경고등 확인 후 조회)"),
    val warrantyDaysLeftPlanInput: Int? = null,
)

object MaintenancePlanner {
    /** 기획 기준. 현대차 공식 정비 주기 아님. */
    const val PLAN_INTERVAL_KM = 15_000

    /** 기획 기준. 실제 보증 만료일 아님. */
    const val PLAN_WARRANTY_NOTICE_DAYS = 30

    /**
     * 화면의 '조회값 (관측)' 영역에 표시할 문장만 반환합니다.
     */
    fun describeObservations(read: MaintenanceRead): List<String> = listOf(
        "• 차량 주행 정보 · 누적 주행거리: ${formatOdometer(read.odometerKm)}",
        "• 차량 안전 상태 · 타이어 공기압(getLowTirePressure): ${formatTire(read.tirePressure)}",
        "• 차량 안전 상태 · 고장 경고등(getTelltalesInfo): ${formatWarningLamps(read.warningLamps)}",
        "• EV 및 연료 · 배터리 잔량(getEvBatteryStateOfCharge): ${formatBattery(read.batterySocPercent)}",
    )

    /**
     * 화면의 '분석 (예측 · 차량이 확정한 사실 아님)' 영역에 표시할 문장만 반환합니다.
     */
    fun describeAnalysis(read: MaintenanceRead): List<String> {
        val lines = mutableListOf<String>()

        when (val odo = read.odometerKm) {
            is ReadState.Value -> {
                val into = odo.value % PLAN_INTERVAL_KM
                val left = PLAN_INTERVAL_KM - into
                lines += "• 정비 시점 예측: 기획 기준 ${PLAN_INTERVAL_KM}km 단위로 비교하면 다음 기준점까지 약 ${left.toInt()}km 남음 (차량 확정 사실 아님)"
            }
            is ReadState.Failed -> {
                lines += "• 정비 시점 예측: 누적 주행거리 조회 실패로 분석 보류"
            }
            is ReadState.NotWired -> {
                lines += "• 정비 시점 예측: 누적 주행거리 조회 전으로 분석하지 않음"
            }
        }

        when (val tire = read.tirePressure) {
            is ReadState.Value -> {
                val raw = tire.value.lowPressureValue
                lines += if (raw != null && raw > 0f) {
                    "• 타이어 분석: 저공기압 신호값($raw)이 있어 공기압 점검을 권장함 (고장 단정 아님)"
                } else {
                    "• 타이어 분석: 저공기압 경고 신호 없음 (정밀 수치 getTirePressure는 PRIVATE_CAR_SAFETY 권한 대상이라 호출하지 않음)"
                }
            }
            is ReadState.Failed -> lines += "• 타이어 분석: 조회 실패로 분석 보류"
            is ReadState.NotWired -> lines += "• 타이어 분석: 조회 전으로 분석하지 않음"
        }

        when (val lamp = read.warningLamps) {
            is ReadState.Value -> {
                lines += if (lamp.value.activeLamps.isEmpty()) {
                    "• 경고등 분석: 점등된 고장 경고등이 관측되지 않음"
                } else {
                    "• 경고등 분석: 점등 신호(${lamp.value.activeLamps.size}건)가 있어 점검을 권장하며 고장 원인을 단정하지 않음"
                }
            }
            is ReadState.Failed -> lines += "• 경고등 분석: 조회 실패로 분석 보류"
            is ReadState.NotWired -> lines += "• 경고등 분석: 조회 전으로 분석하지 않음"
        }

        val days = read.warrantyDaysLeftPlanInput
        lines += if (days == null) {
            "• 무상보증 분석: 차량 SDK에 보증 만료일 조회 API가 없어 미연결 상태임 (기획 기준 D-${PLAN_WARRANTY_NOTICE_DAYS}는 확정 만료일이 아님)"
        } else if (days <= PLAN_WARRANTY_NOTICE_DAYS) {
            "• 무상보증 분석(예측): 기획 입력값 D-${days}가 기획 기준 D-${PLAN_WARRANTY_NOTICE_DAYS} 이내이므로 가까운 블루핸즈 표시 대상임"
        } else {
            "• 무상보증 분석(예측): 기획 입력값 D-${days}가 기획 기준 D-${PLAN_WARRANTY_NOTICE_DAYS} 바깥임"
        }

        lines += "• 기준 안내: ${PLAN_INTERVAL_KM}km와 보증 D-${PLAN_WARRANTY_NOTICE_DAYS}는 기획용 비교 기준이며 현대차 공식 주기나 실제 확정 만료일이 아님"
        return lines
    }

    private fun formatOdometer(state: ReadState<Double>): String = when (state) {
        is ReadState.Value -> "${state.value.toInt()} km"
        is ReadState.Failed -> "조회 실패 (${state.message})"
        is ReadState.NotWired -> "미연결 (${state.reason})"
    }

    private fun formatTire(state: ReadState<TirePressureRead>): String = when (state) {
        is ReadState.Value -> "${state.value.wheelLabel} = ${state.value.lowPressureValue}"
        is ReadState.Failed -> "조회 실패 (${state.message})"
        is ReadState.NotWired -> "미연결 (${state.reason})"
    }

    private fun formatWarningLamps(state: ReadState<WarningLampRead>): String = when (state) {
        is ReadState.Value -> if (state.value.activeLamps.isEmpty()) {
            "점등 없음 (총 ${state.value.totalChecked}개 항목 수신)"
        } else {
            "점등 ${state.value.activeLamps.joinToString(", ")}"
        }
        is ReadState.Failed -> "조회 실패 (${state.message})"
        is ReadState.NotWired -> "미연결 (${state.reason})"
    }

    private fun formatBattery(state: ReadState<Int>): String = when (state) {
        is ReadState.Value -> "${state.value}%"
        is ReadState.Failed -> "조회 실패 (${state.message})"
        is ReadState.NotWired -> "미연결 (${state.reason})"
    }
}
