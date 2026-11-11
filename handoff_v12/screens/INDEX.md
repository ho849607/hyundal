# DriveCheck 화면 PNG 색인

디자인 기준 v12. 화면 예시의 연락·차량·기관 데이터는 모의 값이다. P0 목록 중 휴대폰 화면은 내보내기와 공유 UI 참고용이며 독립적인 휴대폰 앱과 동기화는 P1이다.

## P0 구현 우선순위

| 순서 | 참고 화면 |
| --- | --- |
| 01 도움 요청 진입 | [01_vehicle_16x9/manual_home.png](01_vehicle_16x9/manual_home.png) |
| 02 음성 입력 | [01_vehicle_16x9/listen_0.png](01_vehicle_16x9/listen_0.png) |
| 03 확정 발화와 자막 | [01_vehicle_16x9/listen_3.png](01_vehicle_16x9/listen_3.png) |
| 04 상태 확인 | [01_vehicle_16x9/flow_check.png](01_vehicle_16x9/flow_check.png) |
| 05 첫 미응답 | [01_vehicle_16x9/driver_no_reply.png](01_vehicle_16x9/driver_no_reply.png) |
| 06 재확인 뒤 미확인 | [01_vehicle_16x9/driver_no_reply_repeated.png](01_vehicle_16x9/driver_no_reply_repeated.png) |
| 07 동승자 제보 및 119 | [01_vehicle_16x9/driver_unresponsive.png](01_vehicle_16x9/driver_unresponsive.png) |
| 08 119 발신 경로 | [01_vehicle_16x9/flow_direct.png](01_vehicle_16x9/flow_direct.png) |
| 09 위치 표시 | [01_vehicle_16x9/flow_location.png](01_vehicle_16x9/flow_location.png) |
| 10 실패 및 대체 경로 | [01_vehicle_16x9/flow_failed.png](01_vehicle_16x9/flow_failed.png) |
| 11 연결 미확인 | [01_vehicle_16x9/flow_unknown.png](01_vehicle_16x9/flow_unknown.png) |
| 12 위치 갱신 미확인 | [01_vehicle_16x9/flow_stale.png](01_vehicle_16x9/flow_stale.png) |
| 13 사건 기록 | [01_vehicle_16x9/report_vehicle.png](01_vehicle_16x9/report_vehicle.png) |
| 14 보고서 공유 화면 참고 | [02_phone_9x16/mobile_exported.png](02_phone_9x16/mobile_exported.png) |
| 예외 음성 재시도 | [01_vehicle_16x9/recognition_retry.png](01_vehicle_16x9/recognition_retry.png) |
| 예외 좁은 화면 | [03_vehicle_narrow/narrow.png](03_vehicle_narrow/narrow.png) |

## 전체 파일

| 장면 | PNG | 해상도 |
| --- | --- | --- |
| 03A 도움을 요청했을 때 | [01_vehicle_16x9/manual_home.png](01_vehicle_16x9/manual_home.png) | 1920×1080 |
| 03B 동승자의 말만으로 상황이 모호할 때 | [01_vehicle_16x9/flow_child.png](01_vehicle_16x9/flow_child.png) | 1920×1080 |
| 03C 정차 후 필요한 연락을 고를 때 | [01_vehicle_16x9/suggest.png](01_vehicle_16x9/suggest.png) | 1920×1080 |
| 04A 차량의 충돌 신호가 들어왔을 때 | [01_vehicle_16x9/flow_detect.png](01_vehicle_16x9/flow_detect.png) | 1920×1080 |
| 04B 위험 정도가 확실하지 않을 때 | [01_vehicle_16x9/flow_check.png](01_vehicle_16x9/flow_check.png) | 1920×1080 |
| 04C 괜찮다고 답하고 지속 위험이 없을 때 | [01_vehicle_16x9/safe_answer.png](01_vehicle_16x9/safe_answer.png) | 1920×1080 |
| 04D 첫 상태 확인에 답이 없을 때 | [01_vehicle_16x9/driver_no_reply.png](01_vehicle_16x9/driver_no_reply.png) | 1920×1080 |
| 05A 한 번 더 확인해도 응답이 없을 때 | [01_vehicle_16x9/driver_no_reply_repeated.png](01_vehicle_16x9/driver_no_reply_repeated.png) | 1920×1080 |
| 05B 운전자가 운전을 계속하기 어렵다고 답할 때 | [01_vehicle_16x9/guide_illness.png](01_vehicle_16x9/guide_illness.png) | 1920×1080 |
| 05C 동승자가 실제 반응 없음이나 의식불명을 제보할 때 | [01_vehicle_16x9/driver_unresponsive.png](01_vehicle_16x9/driver_unresponsive.png) | 1920×1080 |
| 05D 사용자가 직접 ‘119 전화’를 요청했을 때 | [01_vehicle_16x9/flow_direct.png](01_vehicle_16x9/flow_direct.png) | 1920×1080 |
| 06B 동승자 제보 뒤 119를 선택했을 때 | [01_vehicle_16x9/flow_dial.png](01_vehicle_16x9/flow_dial.png) | 1920×1080 |
| 06C 119 실제 연결이 확인됐을 때 | [01_vehicle_16x9/flow_location.png](01_vehicle_16x9/flow_location.png) | 1920×1080 |
| 06D 발신 경로가 실패를 반환했을 때 | [01_vehicle_16x9/flow_failed.png](01_vehicle_16x9/flow_failed.png) | 1920×1080 |
| 07B 연결 여부를 확인할 수 없을 때 | [01_vehicle_16x9/flow_unknown.png](01_vehicle_16x9/flow_unknown.png) | 1920×1080 |
| 07C 위치가 오래됐거나 갱신이 안 될 때 | [01_vehicle_16x9/flow_stale.png](01_vehicle_16x9/flow_stale.png) | 1920×1080 |
| 07D 통화 중 최신 주소와 승인 경로가 있을 때 | [01_vehicle_16x9/flow_read.png](01_vehicle_16x9/flow_read.png) | 1920×1080 |
| 08B 요청한 주소를 통화 상대에게 말할 때 | [01_vehicle_16x9/flow_call_speaking.png](01_vehicle_16x9/flow_call_speaking.png) | 1920×1080 |
| 08C ‘직접 말하기’를 눌렀을 때 | [01_vehicle_16x9/flow_call_return.png](01_vehicle_16x9/flow_call_return.png) | 1920×1080 |
| 09A 통화 중 ‘대신 답하기’를 선택했을 때 | [01_vehicle_16x9/assist_start.png](01_vehicle_16x9/assist_start.png) | 1920×1080 |
| 09B 상황요원의 질문을 자막으로 받았을 때 | [01_vehicle_16x9/assist_question.png](01_vehicle_16x9/assist_question.png) | 1920×1080 |
| 09C 탑승자가 답변 버튼을 눌렀을 때 | [01_vehicle_16x9/assist_answer.png](01_vehicle_16x9/assist_answer.png) | 1920×1080 |
| 09D 자동 답변을 켰지만 탑승자 답이 없을 때 | [01_vehicle_16x9/assist_no_reply.png](01_vehicle_16x9/assist_no_reply.png) | 1920×1080 |
| 10A 보호자와 통화 중 대화 보조를 켰을 때 | [01_vehicle_16x9/assist_family.png](01_vehicle_16x9/assist_family.png) | 1920×1080 |
| 10B 휴대폰에서 답을 고를 때 | [02_phone_9x16/mobile_call_reply.png](02_phone_9x16/mobile_call_reply.png) | 1080×1920 |
| 11A 제조사 긴급 정차 작동 신호가 왔을 때 | [01_vehicle_16x9/vehicle_auto_started.png](01_vehicle_16x9/vehicle_auto_started.png) | 1920×1080 |
| 11B 감속 상태가 확인됐을 때 | [01_vehicle_16x9/vehicle_decelerating.png](01_vehicle_16x9/vehicle_decelerating.png) | 1920×1080 |
| 11C 제조사가 정차 완료를 회신했을 때 | [01_vehicle_16x9/vehicle_stop_confirmed.png](01_vehicle_16x9/vehicle_stop_confirmed.png) | 1920×1080 |
| 11D 비상등 자동 작동까지 확인됐을 때 | [01_vehicle_16x9/vehicle_auto_hazard.png](01_vehicle_16x9/vehicle_auto_hazard.png) | 1920×1080 |
| 12A 차량이 과열을 경고했을 때 | [01_vehicle_16x9/heat_only.png](01_vehicle_16x9/heat_only.png) | 1920×1080 |
| 12B 주행 중 불이나 연기 신고가 들어왔을 때 | [01_vehicle_16x9/fire_moving.png](01_vehicle_16x9/fire_moving.png) | 1920×1080 |
| 12C 불이나 연기 신고 뒤 정차가 확인됐을 때 | [01_vehicle_16x9/fire_stopped.png](01_vehicle_16x9/fire_stopped.png) | 1920×1080 |
| 12D 화재 의심 중 차량 정차 여부가 불명확할 때 | [01_vehicle_16x9/fire_unknown.png](01_vehicle_16x9/fire_unknown.png) | 1920×1080 |
| 13B 충돌로 다친 사람이 있다고 알려왔을 때 | [01_vehicle_16x9/guide_crash.png](01_vehicle_16x9/guide_crash.png) | 1920×1080 |
| 13C 연기 신고 뒤 안전한 정차가 확인됐을 때 | [01_vehicle_16x9/guide_smoke.png](01_vehicle_16x9/guide_smoke.png) | 1920×1080 |
| 13D 안전하게 정차했고 차량 고장을 알렸을 때 | [01_vehicle_16x9/guide_breakdown.png](01_vehicle_16x9/guide_breakdown.png) | 1920×1080 |
| 14A 등록·동의 조건으로 보호자 알림을 준비할 때 | [01_vehicle_16x9/flow_guardian.png](01_vehicle_16x9/flow_guardian.png) | 1920×1080 |
| 14B 구급대가 의료 정보 확인을 요청할 때 | [01_vehicle_16x9/flow_medical.png](01_vehicle_16x9/flow_medical.png) | 1920×1080 |
| 14C 교통 통제 등 별도 경찰 신고가 필요할 때 | [01_vehicle_16x9/flow_police.png](01_vehicle_16x9/flow_police.png) | 1920×1080 |
| 14D 보호자 알림 발송 전에 취소했을 때 | [01_vehicle_16x9/flow_cancelled.png](01_vehicle_16x9/flow_cancelled.png) | 1920×1080 |
| 15A 정차 뒤 다른 연락 메뉴를 열었을 때 | [01_vehicle_16x9/other_contacts.png](01_vehicle_16x9/other_contacts.png) | 1920×1080 |
| 15B 등록된 견인 연락을 요청했을 때 | [01_vehicle_16x9/route_tow.png](01_vehicle_16x9/route_tow.png) | 1920×1080 |
| 15C 등록된 보험사 연락을 요청했을 때 | [01_vehicle_16x9/route_insurance.png](01_vehicle_16x9/route_insurance.png) | 1920×1080 |
| 15D 선택한 연락처가 등록되지 않았을 때 | [01_vehicle_16x9/contact_unconfigured.png](01_vehicle_16x9/contact_unconfigured.png) | 1920×1080 |
| 15E 해당 차량이 정차 지원을 제공하지 않을 때 | [01_vehicle_16x9/vehicle_unsupported.png](01_vehicle_16x9/vehicle_unsupported.png) | 1920×1080 |
| 15F 차량 조치 결과를 확인할 수 없을 때 | [01_vehicle_16x9/car_unavailable.png](01_vehicle_16x9/car_unavailable.png) | 1920×1080 |
| 16A 음성 명령을 이해하지 못했을 때 | [01_vehicle_16x9/recognition_retry.png](01_vehicle_16x9/recognition_retry.png) | 1920×1080 |
| 16C 보호자 알림 전송이 실패했을 때 | [01_vehicle_16x9/flow_offline.png](01_vehicle_16x9/flow_offline.png) | 1920×1080 |
| 16E 좁은 창으로 전환될 때 | [03_vehicle_narrow/narrow.png](03_vehicle_narrow/narrow.png) | 1080×1440 |
| 17A 마이크가 열리고 발화 전일 때 | [01_vehicle_16x9/listen_0.png](01_vehicle_16x9/listen_0.png) | 1920×1080 |
| 17B 첫 구절이 인식됐을 때 | [01_vehicle_16x9/listen_1.png](01_vehicle_16x9/listen_1.png) | 1920×1080 |
| 17C 중간 자막이 이어질 때 | [01_vehicle_16x9/listen_2.png](01_vehicle_16x9/listen_2.png) | 1920×1080 |
| 17D 문장이 확정됐을 때 | [01_vehicle_16x9/listen_3.png](01_vehicle_16x9/listen_3.png) | 1920×1080 |
| 18A 누가 말했는지 구분할 수 없을 때 | [01_vehicle_16x9/listen_unknown.png](01_vehicle_16x9/listen_unknown.png) | 1920×1080 |
| 18B 동승자의 구체적인 상태 진술을 들었을 때 | [01_vehicle_16x9/flow_offer.png](01_vehicle_16x9/flow_offer.png) | 1920×1080 |
| 18C 보호자가 제보를 열었을 때 | [02_phone_9x16/guardian_faint_report.png](02_phone_9x16/guardian_faint_report.png) | 1080×1920 |
| 19A 연결·위치를 휴대폰에서 볼 때 | [02_phone_9x16/mobile_flow_location.png](02_phone_9x16/mobile_flow_location.png) | 1080×1920 |
| 19B 의료 카드가 필요할 때 | [02_phone_9x16/mobile_flow_medical.png](02_phone_9x16/mobile_flow_medical.png) | 1080×1920 |
| 19C 사건 기록이 저장됐을 때 | [02_phone_9x16/mobile_flow_done.png](02_phone_9x16/mobile_flow_done.png) | 1080×1920 |
| 19D 보험사가 수신을 확인했을 때 | [02_phone_9x16/mobile_insurer_received.png](02_phone_9x16/mobile_insurer_received.png) | 1080×1920 |
| 20A 긴급 대응을 마치고 기록을 저장할 때 | [01_vehicle_16x9/flow_done.png](01_vehicle_16x9/flow_done.png) | 1920×1080 |
| 20B 저장한 차량 사건을 열었을 때 | [01_vehicle_16x9/report_vehicle.png](01_vehicle_16x9/report_vehicle.png) | 1920×1080 |
| 20C 별도 음성 자막을 확인할 때 | [01_vehicle_16x9/record_voice.png](01_vehicle_16x9/record_voice.png) | 1920×1080 |
| 21A 보호자가 알림을 받았을 때 | [02_phone_9x16/guardian.png](02_phone_9x16/guardian.png) | 1080×1920 |
| 21B 같은 사건을 휴대폰으로 열 때 | [02_phone_9x16/mobile_case.png](02_phone_9x16/mobile_case.png) | 1080×1920 |
| 21C 제출할 항목을 고를 때 | [02_phone_9x16/mobile_export.png](02_phone_9x16/mobile_export.png) | 1080×1920 |
| 21D 보고서 파일이 준비됐을 때 | [02_phone_9x16/mobile_exported.png](02_phone_9x16/mobile_exported.png) | 1080×1920 |
| 22A 휴대폰 연동이 끊겼을 때 | [02_phone_9x16/mobile_pairing.png](02_phone_9x16/mobile_pairing.png) | 1080×1920 |
| 22B 확정 자막을 정정할 때 | [02_phone_9x16/mobile_correction.png](02_phone_9x16/mobile_correction.png) | 1080×1920 |
| 22C 파일 공유 뒤 접수 대기 중일 때 | [02_phone_9x16/mobile_shared.png](02_phone_9x16/mobile_shared.png) | 1080×1920 |
| 23A 승인된 수신처와 항목을 선택했을 때 | [02_phone_9x16/mobile_medical_share.png](02_phone_9x16/mobile_medical_share.png) | 1080×1920 |
| 24A 연락처를 등록·수정할 때 | [02_phone_9x16/mobile_contacts.png](02_phone_9x16/mobile_contacts.png) | 1080×1920 |
| 24B 의료 정보를 입력할 때 | [02_phone_9x16/mobile_medical.png](02_phone_9x16/mobile_medical.png) | 1080×1920 |
| 24C 병원·연락처를 입력할 때 | [02_phone_9x16/mobile_hospital.png](02_phone_9x16/mobile_hospital.png) | 1080×1920 |
| 24D 의료 카드 저장을 확인할 때 | [02_phone_9x16/mobile_card.png](02_phone_9x16/mobile_card.png) | 1080×1920 |
| 25A 보호자가 등록에 동의할 때 | [02_phone_9x16/opt_in.png](02_phone_9x16/opt_in.png) | 1080×1920 |
| 25B 등록 문장을 녹음할 때 | [02_phone_9x16/enroll_sample.png](02_phone_9x16/enroll_sample.png) | 1080×1920 |
| 25C 목소리 등록을 완료했을 때 | [02_phone_9x16/enrolled.png](02_phone_9x16/enrolled.png) | 1080×1920 |
| 29B 등록 차량을 열었을 때 | [02_phone_9x16/mobile_vehicle.png](02_phone_9x16/mobile_vehicle.png) | 1080×1920 |
| 그래픽 자산 address | [04_icons_and_mascot/address.png](04_icons_and_mascot/address.png) | 439×427 |
| 그래픽 자산 clock | [04_icons_and_mascot/clock.png](04_icons_and_mascot/clock.png) | 395×401 |
| 그래픽 자산 guardian | [04_icons_and_mascot/guardian.png](04_icons_and_mascot/guardian.png) | 529×445 |
| 그래픽 자산 heat | [04_icons_and_mascot/heat.png](04_icons_and_mascot/heat.png) | 361×314 |
| 그래픽 자산 insurance | [04_icons_and_mascot/insurance.png](04_icons_and_mascot/insurance.png) | 446×481 |
| 그래픽 자산 ioniq5 | [04_icons_and_mascot/ioniq5.png](04_icons_and_mascot/ioniq5.png) | 1752×897 |
| 그래픽 자산 ioniq5_top_view | [04_icons_and_mascot/ioniq5_top_view.png](04_icons_and_mascot/ioniq5_top_view.png) | 1200×750 |
| 그래픽 자산 medical | [04_icons_and_mascot/medical.png](04_icons_and_mascot/medical.png) | 305×289 |
| 그래픽 자산 mic | [04_icons_and_mascot/mic.png](04_icons_and_mascot/mic.png) | 212×327 |
| 그래픽 자산 mobile | [04_icons_and_mascot/mobile.png](04_icons_and_mascot/mobile.png) | 211×342 |
| 그래픽 자산 mochi | [04_icons_and_mascot/mochi.png](04_icons_and_mascot/mochi.png) | 1021×863 |
| 그래픽 자산 phone | [04_icons_and_mascot/phone.png](04_icons_and_mascot/phone.png) | 288×298 |
| 그래픽 자산 pin | [04_icons_and_mascot/pin.png](04_icons_and_mascot/pin.png) | 242×308 |
| 그래픽 자산 police | [04_icons_and_mascot/police.png](04_icons_and_mascot/police.png) | 429×469 |
| 그래픽 자산 receipt | [04_icons_and_mascot/receipt.png](04_icons_and_mascot/receipt.png) | 341×424 |
| 그래픽 자산 record | [04_icons_and_mascot/record.png](04_icons_and_mascot/record.png) | 269×311 |
| 그래픽 자산 share | [04_icons_and_mascot/share.png](04_icons_and_mascot/share.png) | 321×269 |
| 그래픽 자산 speaker | [04_icons_and_mascot/speaker.png](04_icons_and_mascot/speaker.png) | 434×366 |
| 그래픽 자산 stop | [04_icons_and_mascot/stop.png](04_icons_and_mascot/stop.png) | 202×347 |
| 그래픽 자산 tow | [04_icons_and_mascot/tow.png](04_icons_and_mascot/tow.png) | 544×431 |
