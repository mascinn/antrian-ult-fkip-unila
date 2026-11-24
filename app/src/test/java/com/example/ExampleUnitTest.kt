package com.example

import com.example.core.FkipStudyPrograms
import com.example.data.model.CallTicketPatch
import com.example.data.model.QueueStatus
import com.example.data.model.TakeTicketRpcRequest
import com.example.data.model.TakeTicketRpcResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  private val moshi: Moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  @Test
  fun moshi_serializes_rpc_request_and_parses_response() {
    val reqAdapter = moshi.adapter(TakeTicketRpcRequest::class.java)
    val reqJson = reqAdapter.toJson(
      TakeTicketRpcRequest(
        serviceId = "svc-1",
        studentName = "Siti Rahmawati",
        studentNpm = "2113021045",
        studyProgram = "S1 Pendidikan Matematika"
      )
    )
    assertTrue(reqJson.contains("\"p_service_id\":\"svc-1\""))
    assertTrue(reqJson.contains("\"p_student_npm\":\"2113021045\""))

    val respJson = """
      {
        "success": true,
        "message": "Nomor antrean berhasil diambil",
        "data": {
          "id": "q-1",
          "service_id": "svc-1",
          "queue_number": "A-001",
          "sequence_number": 1,
          "student_name": "Siti Rahmawati",
          "student_npm": "2113021045",
          "student_study_program": "S1 Pendidikan Matematika",
          "status": "waiting",
          "queue_date": "2026-11-24"
        }
      }
    """.trimIndent()
    val parsed = moshi.adapter(TakeTicketRpcResponse::class.java).fromJson(respJson)
    assertNotNull(parsed)
    assertTrue(parsed!!.success)
    assertEquals("A-001", parsed.data?.queueNumber)
    assertEquals(QueueStatus.WAITING, parsed.data?.queueStatus)
  }

  @Test
  fun moshi_serializes_staff_call_patch() {
    val patchJson = moshi.adapter(CallTicketPatch::class.java).toJson(
      CallTicketPatch(
        counterName = "Loket 1",
        calledBy = "staff-uuid",
        calledAt = "2026-11-24T08:00:00.000Z"
      )
    )
    assertTrue(patchJson.contains("\"status\":\"called\""))
    assertTrue(patchJson.contains("\"counter_name\":\"Loket 1\""))
  }

  @Test
  fun fkip_study_programs_search_filters_accurately() {
    val matches = FkipStudyPrograms.filter("PGSD")
    assertTrue(matches.any { it.name.contains("PGSD") })
  }
}
