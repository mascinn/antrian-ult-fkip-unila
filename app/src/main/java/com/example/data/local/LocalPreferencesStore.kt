package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.WibTimeHelper
import com.example.data.model.RememberedStudentInfo
import com.example.data.model.StaffSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.ultDataStore: DataStore<Preferences> by preferencesDataStore(name = "ult_queue_fkip_prefs")

class LocalPreferencesStore(context: Context) {
    private val dataStore = context.applicationContext.ultDataStore

    private object Keys {
        val ACTIVE_TICKET_ID = stringPreferencesKey("active_ticket_id")
        val ACTIVE_TICKET_DATE = stringPreferencesKey("active_ticket_date")
        val STUDENT_NAME = stringPreferencesKey("student_name")
        val STUDENT_NPM = stringPreferencesKey("student_npm")
        val STUDENT_PRODI = stringPreferencesKey("student_prodi")
        val SIMULATION_OPEN_MODE = booleanPreferencesKey("simulation_open_mode")

        val STAFF_USER_ID = stringPreferencesKey("staff_user_id")
        val STAFF_EMAIL = stringPreferencesKey("staff_email")
        val STAFF_FULL_NAME = stringPreferencesKey("staff_full_name")
        val STAFF_ROLE = stringPreferencesKey("staff_role")
        val STAFF_ACCESS_TOKEN = stringPreferencesKey("staff_access_token")
        val STAFF_REFRESH_TOKEN = stringPreferencesKey("staff_refresh_token")
        val STAFF_COUNTER = stringPreferencesKey("staff_counter")
    }

    val activeTicketIdFlow: Flow<String?> = dataStore.data.map { prefs ->
        val savedDate = prefs[Keys.ACTIVE_TICKET_DATE]
        val today = WibTimeHelper.todayIsoDateWib()
        if (savedDate == today) prefs[Keys.ACTIVE_TICKET_ID] else null
    }

    val simulationOpenModeFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.SIMULATION_OPEN_MODE] ?: false
    }

    val rememberedStudentFlow: Flow<RememberedStudentInfo> = dataStore.data.map { prefs ->
        RememberedStudentInfo(
            name = prefs[Keys.STUDENT_NAME].orEmpty(),
            npm = prefs[Keys.STUDENT_NPM].orEmpty(),
            studyProgram = prefs[Keys.STUDENT_PRODI].orEmpty()
        )
    }

    val staffSessionFlow: Flow<StaffSession?> = dataStore.data.map { prefs ->
        val userId = prefs[Keys.STAFF_USER_ID]
        val accessToken = prefs[Keys.STAFF_ACCESS_TOKEN]
        val refreshToken = prefs[Keys.STAFF_REFRESH_TOKEN]
        if (!userId.isNullOrBlank() && !accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()) {
            StaffSession(
                userId = userId,
                email = prefs[Keys.STAFF_EMAIL].orEmpty(),
                fullName = prefs[Keys.STAFF_FULL_NAME].orEmpty().ifBlank { "Petugas ULT" },
                role = prefs[Keys.STAFF_ROLE].orEmpty().ifBlank { "staff" },
                accessToken = accessToken,
                refreshToken = refreshToken
            )
        } else {
            null
        }
    }

    val selectedCounterFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.STAFF_COUNTER] ?: "Loket 1"
    }

    suspend fun saveActiveTicket(ticketId: String) {
        dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_TICKET_ID] = ticketId
            prefs[Keys.ACTIVE_TICKET_DATE] = WibTimeHelper.todayIsoDateWib()
        }
    }

    suspend fun clearActiveTicket() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.ACTIVE_TICKET_ID)
            prefs.remove(Keys.ACTIVE_TICKET_DATE)
        }
    }

    suspend fun getActiveTicketIdToday(): String? = activeTicketIdFlow.first()

    suspend fun saveStudentInfo(name: String, npm: String, studyProgram: String) {
        dataStore.edit { prefs ->
            prefs[Keys.STUDENT_NAME] = name.trim()
            prefs[Keys.STUDENT_NPM] = npm.trim()
            prefs[Keys.STUDENT_PRODI] = studyProgram.trim()
        }
    }

    suspend fun setSimulationOpenMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SIMULATION_OPEN_MODE] = enabled
        }
    }

    suspend fun saveStaffSession(session: StaffSession) {
        dataStore.edit { prefs ->
            prefs[Keys.STAFF_USER_ID] = session.userId
            prefs[Keys.STAFF_EMAIL] = session.email
            prefs[Keys.STAFF_FULL_NAME] = session.fullName
            prefs[Keys.STAFF_ROLE] = session.role
            prefs[Keys.STAFF_ACCESS_TOKEN] = session.accessToken
            prefs[Keys.STAFF_REFRESH_TOKEN] = session.refreshToken
        }
    }

    suspend fun clearStaffSession() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.STAFF_USER_ID)
            prefs.remove(Keys.STAFF_EMAIL)
            prefs.remove(Keys.STAFF_FULL_NAME)
            prefs.remove(Keys.STAFF_ROLE)
            prefs.remove(Keys.STAFF_ACCESS_TOKEN)
            prefs.remove(Keys.STAFF_REFRESH_TOKEN)
        }
    }

    suspend fun saveSelectedCounter(counterName: String) {
        dataStore.edit { prefs ->
            prefs[Keys.STAFF_COUNTER] = counterName
        }
    }
}
