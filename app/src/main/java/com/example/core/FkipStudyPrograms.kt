package com.example.core

data class StudyProgramOption(
    val name: String,
    val level: String,
    val department: String
)

object FkipStudyPrograms {
    val all: List<StudyProgramOption> = listOf(
        // S1 — Jurusan Ilmu Pendidikan (IP)
        StudyProgramOption("S1 Pendidikan Guru Sekolah Dasar (PGSD)", "S1", "Jurusan Ilmu Pendidikan"),
        StudyProgramOption("S1 Pendidikan Guru PAUD (PG-PAUD)", "S1", "Jurusan Ilmu Pendidikan"),
        StudyProgramOption("S1 Bimbingan dan Konseling (BK)", "S1", "Jurusan Ilmu Pendidikan"),
        StudyProgramOption("S1 Pendidikan Jasmani (Penjas / PJKR)", "S1", "Jurusan Ilmu Pendidikan"),

        // S1 — Jurusan PMIPA
        StudyProgramOption("S1 Pendidikan Matematika", "S1", "Jurusan PMIPA"),
        StudyProgramOption("S1 Pendidikan Fisika", "S1", "Jurusan PMIPA"),
        StudyProgramOption("S1 Pendidikan Kimia", "S1", "Jurusan PMIPA"),
        StudyProgramOption("S1 Pendidikan Biologi", "S1", "Jurusan PMIPA"),
        StudyProgramOption("S1 Pendidikan Teknologi Informasi (PTI)", "S1", "Jurusan PMIPA"),

        // S1 — Jurusan PIPS
        StudyProgramOption("S1 Pendidikan Ekonomi", "S1", "Jurusan PIPS"),
        StudyProgramOption("S1 Pendidikan Sejarah", "S1", "Jurusan PIPS"),
        StudyProgramOption("S1 Pendidikan Geografi", "S1", "Jurusan PIPS"),
        StudyProgramOption("S1 Pendidikan Pancasila dan Kewarganegaraan (PPKn)", "S1", "Jurusan PIPS"),

        // S1 — Jurusan Pendidikan Bahasa dan Seni (PBS)
        StudyProgramOption("S1 Pendidikan Bahasa dan Sastra Indonesia", "S1", "Jurusan Bahasa & Seni"),
        StudyProgramOption("S1 Pendidikan Bahasa Inggris", "S1", "Jurusan Bahasa & Seni"),
        StudyProgramOption("S1 Pendidikan Bahasa Lampung", "S1", "Jurusan Bahasa & Seni"),
        StudyProgramOption("S1 Pendidikan Bahasa Prancis", "S1", "Jurusan Bahasa & Seni"),
        StudyProgramOption("S1 Pendidikan Tari", "S1", "Jurusan Bahasa & Seni"),
        StudyProgramOption("S1 Pendidikan Musik", "S1", "Jurusan Bahasa & Seni"),

        // S2 — Program Magister
        StudyProgramOption("S2 Magister Administrasi Pendidikan", "S2", "Pascasarjana FKIP"),
        StudyProgramOption("S2 Magister Teknologi Pendidikan", "S2", "Pascasarjana FKIP"),
        StudyProgramOption("S2 Magister Pendidikan Matematika", "S2", "Pascasarjana FKIP"),
        StudyProgramOption("S2 Magister Pendidikan Fisika", "S2", "Pascasarjana FKIP"),
        StudyProgramOption("S2 Magister Pendidikan IPA", "S2", "Pascasarjana FKIP"),
        StudyProgramOption("S2 Magister Pendidikan IPS", "S2", "Pascasarjana FKIP"),
        StudyProgramOption("S2 Magister Pendidikan Bahasa dan Sastra Indonesia", "S2", "Pascasarjana FKIP"),
        StudyProgramOption("S2 Magister Pendidikan Bahasa Inggris", "S2", "Pascasarjana FKIP"),
        StudyProgramOption("S2 Magister Pendidikan Bahasa dan Kebudayaan Lampung", "S2", "Pascasarjana FKIP"),
        StudyProgramOption("S2 Magister Keguruan Guru Sekolah Dasar (PGSD)", "S2", "Pascasarjana FKIP"),

        // S3 & Profesi
        StudyProgramOption("S3 Doktor Ilmu Pendidikan", "S3", "Pascasarjana FKIP"),
        StudyProgramOption("Pendidikan Profesi Guru (PPG)", "Profesi", "Program Profesi FKIP")
    )

    fun filter(query: String): List<StudyProgramOption> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return all
        return all.filter {
            it.name.contains(trimmed, ignoreCase = true) ||
                it.department.contains(trimmed, ignoreCase = true) ||
                it.level.contains(trimmed, ignoreCase = true)
        }
    }
}
