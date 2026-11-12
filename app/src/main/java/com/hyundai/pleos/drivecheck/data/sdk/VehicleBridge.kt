package com.hyundai.pleos.drivecheck.data.sdk

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.area.WheelArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.WheelValue
import ai.pleos.playground.vehicle.constant.telltale.TelltaleState
import android.content.Context

/**
 * 공식 샘플과 같은 순서로 붙입니다:
 * initialize → capability 확인 → read → 끝나면 release.
 *
 * - 도어 위치 getCurrentDoorPosition은 SDK 연결 확인용입니다. 성공을 정비 완료로 쓰지 않습니다.
 * - 카탈로그에 실제로 있는 typed API만 순서대로 호출합니다:
 *   1. 차량 주행 정보: 누적 주행거리 (Odometer.checkCurrentDistanceDrivenCapabilities → getCurrentDistanceDriven)
 *   2. 차량 안전 상태: 타이어 공기압 (Tire.checkLowTirePressureCapabilities → getLowTirePressure, CAR_SAFETY 범위)
 *   3. 차량 안전 상태: 경고등 (Safety.getTelltalesInfo, CAR_SAFETY 범위)
 *   4. EV 및 연료: 배터리 잔량 (EvBattery.checkEvBatteryStateOfChargeCapabilities → getEvBatteryStateOfCharge)
 * - 블루핸즈 길안내(NaviHelper), 비상등, 문 잠금, 창문, 공조는 이번 단계에서 호출하지 않습니다.
 */
class VehicleBridge(context: Context) {
    private val vehicle = Vehicle(context.applicationContext)
    private var ready = false

    val isReady: Boolean
        get() = ready

    fun open(): Result<Unit> = runCatching {
        if (!ready) {
            vehicle.initialize()
            ready = true
        }
    }

    /**
     * 공식 예제: 앞왼쪽 도어 위치 조회 (연결 확인 전용).
     * 실패하면 메서드를 늘리지 않고 오류를 그대로 돌려줍니다.
     */
    fun readFrontLeftDoor(onPosition: (Int?) -> Unit, onFailed: (Exception) -> Unit) {
        if (!ready) {
            onFailed(IllegalStateException("Vehicle.initialize()가 먼저입니다."))
            return
        }
        try {
            val area = DoorArea(DoorValue.ROW_1_LEFT)
            vehicle.getDoor().checkDoorPositionCapabilities(
                onComplete = { config, _ ->
                    if (!config.accessType.canRead()) {
                        onFailed(IllegalStateException("도어 위치 읽기 capability 미지원 (${config.accessType})"))
                        return@checkDoorPositionCapabilities
                    }
                    vehicle.getDoor().getCurrentDoorPosition(
                        area = area,
                        onComplete = onPosition,
                        onFailed = onFailed,
                    )
                },
                onFailed = onFailed,
            )
        } catch (e: Exception) {
            onFailed(e)
        }
    }

    /**
     * 1. 차량 주행 정보: 누적 주행거리 (CAR_DRIVE)
     * checkCurrentDistanceDrivenCapabilities → getCurrentDistanceDriven
     */
    fun readOdometerKm(onResult: (ReadState<Double>) -> Unit) {
        if (!ready) {
            onResult(ReadState.Failed("Vehicle.initialize()가 먼저입니다."))
            return
        }
        try {
            val odometer = vehicle.getOdometer()
            odometer.checkCurrentDistanceDrivenCapabilities(
                onComplete = { config ->
                    if (!config.accessType.canRead()) {
                        onResult(ReadState.Failed("누적 주행거리 capability 미지원 (${config.accessType})"))
                        return@checkCurrentDistanceDrivenCapabilities
                    }
                    odometer.getCurrentDistanceDriven(
                        onComplete = { km ->
                            if (km == null) {
                                onResult(ReadState.Failed("누적 주행거리 응답값이 null입니다."))
                            } else {
                                onResult(ReadState.Value(km.toDouble()))
                            }
                        },
                        onFailed = { err -> onResult(ReadState.Failed(describeError(err))) },
                    )
                },
                onFailed = { err -> onResult(ReadState.Failed(describeError(err))) },
            )
        } catch (e: Exception) {
            onResult(ReadState.Failed(describeError(e)))
        }
    }

    /**
     * 2. 차량 안전 상태: 타이어 공기압 (CAR_SAFETY)
     * 참고: Tire.getTirePressure는 PRIVATE_CAR_SAFETY 권한을 요구하므로 호출하지 않고,
     * CAR_SAFETY 권한 범위인 checkLowTirePressureCapabilities → getLowTirePressure만 호출합니다.
     */
    fun readLowTirePressure(onResult: (ReadState<TirePressureRead>) -> Unit) {
        if (!ready) {
            onResult(ReadState.Failed("Vehicle.initialize()가 먼저입니다."))
            return
        }
        try {
            val tire = vehicle.getTire()
            tire.checkLowTirePressureCapabilities(
                onComplete = { config, areas ->
                    if (!config.accessType.canRead()) {
                        onResult(ReadState.Failed("타이어 공기압 저하 조회 capability 미지원 (${config.accessType})"))
                        return@checkLowTirePressureCapabilities
                    }
                    val targetArea = areas.firstOrNull() ?: WheelArea(WheelValue.LEFT_FRONT)
                    tire.getLowTirePressure(
                        area = targetArea,
                        onComplete = { rawValue ->
                            onResult(
                                ReadState.Value(
                                    TirePressureRead(
                                        wheelLabel = targetArea.values.joinToString(",") { it.name },
                                        lowPressureValue = rawValue,
                                    )
                                )
                            )
                        },
                        onFailed = { err -> onResult(ReadState.Failed(describeError(err))) },
                    )
                },
                onFailed = { err -> onResult(ReadState.Failed(describeError(err))) },
            )
        } catch (e: Exception) {
            onResult(ReadState.Failed(describeError(e)))
        }
    }

    /**
     * 3. 차량 안전 상태: 고장 경고등 (CAR_SAFETY)
     * 참고: Safety.checkTelltalesCapabilities는 PRIVATE_CAR_SAFETY 권한을 요구하므로 호출하지 않고,
     * CAR_SAFETY 권한 범위인 Safety.getTelltalesInfo만 호출합니다.
     */
    fun readWarningLamps(onResult: (ReadState<WarningLampRead>) -> Unit) {
        if (!ready) {
            onResult(ReadState.Failed("Vehicle.initialize()가 먼저입니다."))
            return
        }
        try {
            vehicle.getSafety().getTelltalesInfo(
                onComplete = { telltales ->
                    val list = telltales?.telltales.orEmpty()
                    val activeNames = list
                        .filter { it.state == TelltaleState.ON || it.state == TelltaleState.BLINK }
                        .map { "${it.type.name}(${it.state.name})" }
                    onResult(
                        ReadState.Value(
                            WarningLampRead(
                                totalChecked = list.size,
                                activeLamps = activeNames,
                            )
                        )
                    )
                },
                onFailed = { err -> onResult(ReadState.Failed(describeError(err))) },
            )
        } catch (e: Exception) {
            onResult(ReadState.Failed(describeError(e)))
        }
    }

    /**
     * 4. EV 및 연료: 배터리 잔량 (CAR_ENERGY)
     * checkEvBatteryStateOfChargeCapabilities → getEvBatteryStateOfCharge
     */
    fun readBatterySocPercent(onResult: (ReadState<Int>) -> Unit) {
        if (!ready) {
            onResult(ReadState.Failed("Vehicle.initialize()가 먼저입니다."))
            return
        }
        try {
            val evBattery = vehicle.getEvBattery()
            evBattery.checkEvBatteryStateOfChargeCapabilities(
                onComplete = { config ->
                    if (!config.accessType.canRead()) {
                        onResult(ReadState.Failed("배터리 잔량 capability 미지원 (${config.accessType})"))
                        return@checkEvBatteryStateOfChargeCapabilities
                    }
                    evBattery.getEvBatteryStateOfCharge(
                        onComplete = { soc ->
                            if (soc == null) {
                                onResult(ReadState.Failed("배터리 잔량 응답값이 null입니다."))
                            } else {
                                onResult(ReadState.Value(soc))
                            }
                        },
                        onFailed = { err -> onResult(ReadState.Failed(describeError(err))) },
                    )
                },
                onFailed = { err -> onResult(ReadState.Failed(describeError(err))) },
            )
        } catch (e: Exception) {
            onResult(ReadState.Failed(describeError(e)))
        }
    }

    fun close() {
        if (!ready) return
        runCatching { vehicle.release() }
        ready = false
    }

    private fun AccessType?.canRead(): Boolean =
        this == AccessType.READ || this == AccessType.READ_WRITE

    companion object {
        fun describeError(error: Throwable): String =
            "${error::class.java.simpleName}: ${error.message ?: "메시지 없음"}"
    }
}

data class TirePressureRead(
    val wheelLabel: String,
    val lowPressureValue: Float?,
)

data class WarningLampRead(
    val totalChecked: Int,
    val activeLamps: List<String>,
)

sealed interface ReadState<out T> {
    data class NotWired(val reason: String) : ReadState<Nothing>
    data class Value<T>(val value: T) : ReadState<T>
    data class Failed(val message: String) : ReadState<Nothing>
}
