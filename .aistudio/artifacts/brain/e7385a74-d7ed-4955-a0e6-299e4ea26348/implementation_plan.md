# ULT Queue FKIP Unila — Rencana Implementasi Android

Aplikasi antrean pelayanan administrasi berbasis Android native (Kotlin & Jetpack Compose) untuk **Unit Layanan Terpadu (ULT) FKIP Universitas Lampung**, terhubung secara real-time dengan backend **Supabase** untuk mahasiswa tanpa login dan petugas loket terautentikasi.

---

## Ringkasan Keputusan & Konfirmasi Pengguna

> [!IMPORTANT]
> Seluruh preferensi utama dari sesi klarifikasi telah diterapkan dan menjadi acuan utama desain serta alur aplikasi. Mohon tinjau rencana ini sebelum proses pembangunan kode dimulai.

- **Jam Operasional & Uji Coba (Terkonfirmasi)**: Mengikuti jadwal resmi ULT **Senin–Jumat, 08:00–15:00 WIB** dengan banner informasi tutup otomatis di luar jam kerja, disertai tombol **Mode Simulasi Buka** yang jelas agar alur pengambilan antrean tetap dapat diuji kapan saja.
- **Pilihan Program Studi (Terkonfirmasi)**: Menggunakan **dropdown daftar resmi Program Studi FKIP Unila** (mencakup S1 seluruh jurusan IP, PMIPA, PIPS, PBS, S2 Magister, S3 Ilmu Pendidikan, dan PPG) dilengkapi kolom pencarian cepat.
- **Identitas Visual & Warna (Terkonfirmasi)**: Mengusung tema **Ungu sebagai warna dasar dan Putih bersih** — kontras tajam, modern, akademis, dan nyaman dibaca di ruang tunggu pelayanan.
- **Akses Portal Petugas (Terkonfirmasi)**: Ditempatkan sebagai **tombol Petugas di pojok kanan atas layar Beranda Mahasiswa**, menjaga layar utama tetap fokus bagi mahasiswa namun cepat diakses staf ULT.
- **Keamanan Kredensial Supabase (Catatan Penting)**: Aplikasi Android **hanya** menggunakan `SUPABASE_URL` dan `SUPABASE_PUBLISHABLE_KEY` melalui konfigurasi **Secrets AI Studio** (`BuildConfig`). Kunci `SUPABASE_SECRET_KEY` tidak akan pernah dimasukkan ke dalam APK; sangat disarankan untuk merotasi kunci rahasia tersebut di dashboard Supabase jika sempat dibagikan.

---

## 1. Gambaran Umum & Konsep Inti

- **Apa yang Dibangun**: Sistem antrean digital real-time yang memisahkan pengalaman **Mahasiswa** (tanpa akun, cukup isi Nama, NPM, dan Prodi) dan **Petugas ULT** (login Supabase Auth dengan peran `staff`/`admin`) dalam satu aplikasi Android.
- **Target Pengguna**:
  - **Mahasiswa FKIP Unila** yang mengurus berkas KKN (`A-xxx`), Wisuda (`B-xxx`), Surat Administrasi (`C-xxx`), atau Layanan Administrasi Lainnya (`D-xxx`).
  - **Petugas Loket ULT** (Loket 1, Loket 2, Loket 3) yang memanggil, melayani, menyelesaikan, atau melewati antrean tidak hadir.
- **Nilai Utama**: Menghilangkan antrean fisik yang menumpuk, memastikan penomoran harian anti-duplikat melalui RPC atomik di PostgreSQL, serta memberi notifikasi panggilan instan di ponsel mahasiswa berupa dialog mencolok, bel ding-dong, getaran haptic, dan suara panggilan otomatis (**Text-to-Speech Bahasa Indonesia**).

---

## 2. Pengalaman Pengguna & Desain Visual

### Alur Pengguna Utama

1. **Splash & Pemuatan Awal**:
   - Menampilkan identitas visual ULT FKIP Unila berlatar putih bersih dan aksen ungu selama inisialisasi konfigurasi dan pemeriksaan tiket aktif hari ini di penyimpanan lokal.
   - Jika mahasiswa sudah memiliki tiket aktif hari ini (`waiting`, `called`, atau `in_service`), tersedia kartu pintasan langsung di Beranda untuk kembali ke layar pemantauan tiket.
2. **Beranda Mahasiswa (Papan Pantau & Layanan)**:
   - **Header Ungu-Putih**: Judul **ULT FKIP Unila**, indikator status operasional real-time (WIB), dan **tombol ikon Petugas** di pojok kanan atas menuju layar login staf.
   - **Banner Jam Operasional**: Di luar Senin–Jumat 08:00–15:00 WIB, menampilkan kartu status *Layanan Sedang Tutup* beserta tombol aktifkan **Mode Simulasi Buka** (dengan lencana indikator jelas saat mode simulasi aktif).
   - **Papan Antrean Live (Ringkas)**: Menampilkan nomor yang sedang dipanggil/dilayani di Loket 1, Loket 2, dan Loket 3 secara real-time.
   - **Daftar 4 Kartu Layanan (`A`, `B`, `C`, `D`)**: Menampilkan kode prefix, nama layanan, deskripsi singkat, sisa kuota harian (misal `12 / 50`), serta jumlah antrean menunggu. Mengetuk layanan membuka formulir ambil tiket.
3. **Formulir Ambil Tiket Antrean**:
   - Menampilkan ringkasan layanan terpilih beserta sisa kuota.
   - Input **Nama Lengkap**, **NPM** (validasi numerik), dan **Program Studi FKIP Unila** menggunakan *searchable bottom sheet / dropdown* daftar resmi Prodi FKIP Unila.
   - Saat dikirim, memanggil RPC atomik `take_queue_ticket`. Jika NPM sudah memiliki tiket aktif hari ini, aplikasi menawarkan pembukaan langsung tiket aktif tersebut.
4. **Layar Tiket & Pemantauan Real-Time**:
   - **Kartu Boarding-Pass Antrean**: Nomor tiket berukuran ekstra besar (`A-001`) dengan tipografi monospaced tegas, badge status 4 tahap (`Menunggu`, `Dipanggil`, `Sedang Dilayani`, `Selesai` / `Dilewati`), nama mahasiswa, NPM, Prodi, estimasi sisa antrean di depan, dan loket tujuan.
   - **Peringatan Panggilan Instan (`called`)**: Begitu petugas memanggil tiket, aplikasi langsung menampilkan dialog visual berkontras tinggi *"Giliran Anda! Menuju Loket X"*, memutar bel pengumuman singkat, menggetarkan perangkat, dan membacakan pengumuman suara via Android TTS Bahasa Indonesia (*"Nomor antrean A kosong kosong satu, atas nama ..., silakan menuju Loket satu"*). Tersedia tombol tes suara/putarkan ulang pengumuman.
   - **Selesai / Dilewati**: Menampilkan ringkasan sesi dan tombol kembali ke Beranda untuk mengambil layanan pada hari berikutnya.
5. **Login & Dashboard Petugas ULT**:
   - **Login Petugas**: Form Email & Password terhubung ke Supabase Auth, dilanjutkan validasi peran `staff` atau `admin` pada tabel `profiles`.
   - **Dashboard Manajemen Antrean**:
     - **Pemilih Loket Tugas**: Segmented chips untuk memilih `Loket 1`, `Loket 2`, atau `Loket 3`.
     - **Ringkasan Harian**: Jumlah menunggu, sedang dilayani, dan selesai hari ini.
     - **Kartu Antrean Aktif di Loket Ini**: Menampilkan tiket yang sedang dipanggil (`called`) atau dilayani (`in_service`) oleh loket terpilih, dilengkapi tombol aksi langsung:
       - **Panggil Ulang** (menghitung frekuensi panggilan 1x–3x untuk panduan no-show),
       - **Mulai Layani** (`called` → `in_service`),
       - **Selesai** (`in_service` → `completed`),
       - **Lewati / Tidak Hadir** (`called` → `skipped`, disarankan setelah 3x panggilan).
     - **Filter Kategori & Daftar Antrean Menunggu**: Tab/chip filter (`Semua`, `A - KKN`, `B - Wisuda`, `C - Surat`, `D - Lainnya`), tombol utama **Panggil Antrean Berikutnya**, serta daftar kartu antrean yang terus diperbarui secara real-time.

### Identitas Visual & Tema (Ungu & Putih)

- **Arah Estetika**: *Crisp Institutional Minimalism* — bersih, modern, berwibawa, dan mudah dipindai dalam satu pandangan di area layanan kampus.
- **Palet Warna & Token Material 3**:
  - **Primary (Ungu Utama)**: Deep Royal Purple (`#5B21B6`) untuk tombol utama, header beraksen, dan nomor antrean fokus.
  - **Primary Container / Soft Tint**: Soft Lavender (`#F3EEFF`) dengan garis tepi tipis (`#DDD6FE`) untuk kartu sorotan dan chip aktif.
  - **Background & Surface**: Putih murni (`#FFFFFF`) untuk kartu utama dan Putih Gading/Salju (`#FAF8FF`) untuk latar belakang layar agar kedalaman kartu terasa tanpa warna abu-abu kusam.
  - **On-Surface Ink**: Charcoal Ungu Gelap (`#1A102B`) untuk teks utama dan `#5C5270` untuk teks sekunder agar rasio kontras tetap tinggi.
  - **Aksen Status Semantik Terkendali**: Tetap menjaga dominasi ungu-putih sambil membedakan status operasional secara cepat (`Menunggu`: Ungu lembut, `Dipanggil`: Ungu pekat + indikator berdenyut, `Dilayani`: Indigo/Ungu cerah, `Selesai`: Hijau zamrud lembut, `Tutup/Dilewati`: Merah marun halus).
- **Tipografi & Hierarki**:
  - Mengunduh dan membundel font lokal Google Fonts via `font-util`:
    - **Plus Jakarta Sans** untuk judul antarmuka dan teks tubuh yang bersih dan modern.
    - **JetBrains Mono** untuk nomor tiket (`A-001`), NPM, serta jam WIB agar angka sejajar presisi dan sangat mudah dibaca dari jauh.
- **Ikon Aplikasi (Adaptive Launcher Icon)**: Ikon kustom adaptif bertema **Ungu & Putih** yang merepresentasikan tiket antrean ULT FKIP (menggantikan ikon default Android).
- **Interaksi & Gerak**: Transisi `AnimatedVisibility` dan animasi spring ringan saat status tiket berubah, denyut visual halus ketika status `called`, pull-to-refresh pada daftar antrean, serta penanganan penuh `enableEdgeToEdge()` dan `WindowInsets` (aman dari notch & bilah navigasi sistem).

---

## 3. Keputusan Produk & Pertimbangan Teknis

- **Native Android (Kotlin & Jetpack Compose) vs Flutter**:
  - *Pilihan*: Dibangun 100% native Kotlin & Jetpack Compose sesuai lingkungan proyek Android yang aktif, sambil mempertahankan 100% kompatibilitas skema database, fungsi RPC, aturan RLS, dan alur pengguna dari dokumen arsitektur awal.
  - *Alasan*: Integrasi langsung dengan `android.speech.tts.TextToSpeech`, `VibrationEffect`, `AudioTrack` (bel ding-dong sintetis tanpa ketergantungan file eksternal), `DataStore Preferences`, dan Material 3 adaptif.
- **Klien Supabase Ringan Berbasis Retrofit + OkHttp WebSocket**:
  - *Pilihan*: Menggunakan stack networking bawaan proyek (`Retrofit`, `OkHttp`, `Moshi`) untuk Supabase REST/RPC/Auth dan klien **OkHttp WebSocket** (protokol Phoenix Channel `postgres_changes`) untuk Supabase Realtime, dilengkapi sinkronisasi cadangan otomatis jika koneksi seluler sempat terputus.
  - *Alasan*: Mendukung penuh format kunci baru Supabase `sb_publishable_...` (dikirim melalui header `apikey` tanpa memasukkannya ke header `Authorization: Bearer`, sementara header `Authorization` dipakai khusus untuk JWT sesi Petugas sesuai aturan RLS), sekaligus menjaga ukuran APK ringkas dan bebas konflik dependensi Kotlin.
- **Persistensi Tiket Aktif Mahasiswa**:
  - *Pilihan*: `Jetpack DataStore Preferences` untuk menyimpan ID tiket aktif, tanggal tiket, dan data diri terakhir mahasiswa (Nama, NPM, Prodi) secara aman di perangkat, sementara seluruh sumber kebenaran antrean tetap berada di PostgreSQL Supabase.
  - *Alasan*: Membuka kembali aplikasi otomatis menampilkan tiket yang sedang berjalan, dan otomatis membersihkan referensi lokal saat berganti hari (`CURRENT_DATE` WIB).
- **Keamanan & Batas Akses**:
  - Kredensial Supabase dibaca lewat `BuildConfig` yang diisi melalui panel **Secrets AI Studio** (`.env.example` hanya berisi placeholder untuk `SUPABASE_URL` dan `SUPABASE_PUBLISHABLE_KEY`).
  - Input mahasiswa (Nama, NPM, Prodi) divalidasi di klien dan diproses aman melalui RPC PostgreSQL `take_queue_ticket` berparameter; pembaruan status antrean hanya mengirim permintaan dengan JWT sesi petugas terautentikasi yang diverifikasi oleh kebijakan RLS di server Supabase.

---

## 4. Arsitektur Teknis & Strategi Data *(Referensi Teknis)*

### Diagram Arsitektur & Aliran Data

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                     UI Layer (Jetpack Compose M3)                       │
│  ┌──────────────┐   ┌──────────────────┐   ┌─────────────────────────┐  │
│  │ Splash &     │──▶│ Student Home     │──▶│ Take Queue Form         │  │
│  │ Auto-Restore │   │ (Live Board +    │   │ (Searchable Prodi FKIP) │  │
│  └──────────────┘   │  Staff Icon Btn) │   └───────────┬─────────────┘  │
│                     └────────┬─────────┘               ▼                │
│                              │             ┌─────────────────────────┐  │
│                              │             │ Ticket Monitoring       │  │
│                              ▼             │ (Live Status + Alert    │  │
│                     ┌──────────────────┐   │  Dialog + Chime + TTS)  │  │
│                     │ Staff Login      │   └───────────┬─────────────┘  │
│                     └────────┬─────────┘               │                │
│                              ▼                         │                │
│                     ┌──────────────────┐               │                │
│                     │ Staff Dashboard  │               │                │
│                     │ (Loket 1/2/3 +   │               │                │
│                     │  Filter + Calls) │               │                │
│                     └────────┬─────────┘               │                │
└──────────────────────────────┼─────────────────────────┼────────────────┘
                               ▼                         ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                 ViewModel & Service Layer (MVVM / Flow)                 │
│  ┌─────────────────────┐  ┌────────────────────┐  ┌──────────────────┐  │
│  │ StudentQueueVM      │  │ StaffDashboardVM   │  │ AlertAudioEngine │  │
│  │ (WIB Clock + Sim    │  │ (Auth Session +    │  │ (Chime + Android │  │
│  │  Toggle + Quota)    │  │  Call Transitions) │  │  TTS + Haptics)  │  │
│  └──────────┬──────────┘  └─────────┬──────────┘  └──────────────────┘  │
└─────────────┼───────────────────────┼───────────────────────────────────┘
              ▼                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    Repository & Data Persistence                        │
│  ┌──────────────────────────────┐   ┌────────────────────────────────┐  │
│  │ SupabaseQueueRepository      │   │ LocalPreferencesStore          │  │
│  │ • REST & RPC (Retrofit)      │   │ (DataStore: Active Ticket ID,  │  │
│  │ • Staff Auth (GoTrue JWT)    │   │  Student Profile, Staff Token, │  │
│  │ • Realtime WS (OkHttp Phx)   │   │  Simulation Mode Flag)         │  │
│  └──────────────┬───────────────┘   └────────────────────────────────┘  │
└─────────────────┼───────────────────────────────────────────────────────┘
                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                 Supabase Backend (PostgreSQL + RLS)                     │
│  • Tables: services, counters, profiles, queues (Realtime Enabled)      │
│  • RPC: public.take_queue_ticket(...) [Atomic Daily Sequence & Quota]   │
│  • Auth: Email/Password Staff Accounts + RLS UPDATE Policy              │
└─────────────────────────────────────────────────────────────────────────┘
```

### Model Data & State Utama

- **Service (`services`)**: `id`, `code` (`A`/`B`/`C`/`D`), `name`, `description`, `quota_per_day`, `is_active`, ditambah agregasi jumlah antrean hari ini (`todayCount`, `waitingCount`).
- **Counter (`counters`)**: `id`, `name` (`Loket 1`, `Loket 2`, `Loket 3`), `is_active`.
- **QueueTicket (`queues`)**: `id`, `service_id`, `queue_number`, `sequence_number`, `student_name`, `student_npm`, `student_study_program`, `status` (`waiting` | `called` | `in_service` | `completed` | `skipped`), `counter_name`, `called_by`, `queue_date`, `called_at`, `served_at`, `completed_at`, `created_at`.
- **StaffSession & Profile (`profiles`)**: `userId`, `accessToken`, `refreshToken`, `email`, `fullName`, `role` (`staff` | `admin`).
- **OperationalStatus**: Jam WIB real-time (`Asia/Jakarta`), status buka/tutup otomatis, dan flag `isSimulationOpen` yang bisa diaktifkan pengguna untuk pengujian.

### Pemetaan Interaksi Komponen & Mutasi State

- **Pemilihan Layanan & Validasi Form**: Memeriksa jam operasional (atau Mode Simulasi Buka), kuota tersisa, kelengkapan Nama, format NPM, dan pilihan Prodi dari modal pencarian. Mengubah state ke *Loading*, memanggil `POST /rest/v1/rpc/take_queue_ticket`, menyimpan tiket ke `DataStore`, lalu menavigasi ke layar pemantauan tiket. Jika RPC mengembalikan `existing_ticket_id`, aplikasi memuat tiket aktif tersebut dan menampilkan tombol langsung membukanya.
- **Pembaruan Real-Time & Pemicu Panggilan**: Koneksi WebSocket berlangganan `postgres_changes` tabel `queues` untuk tanggal hari ini (WIB). Setiap event `INSERT`/`UPDATE` langsung menyegarkan daftar antrean di Beranda, Tiket Mahasiswa, dan Dashboard Petugas. Ketika tiket milik mahasiswa berubah ke `called` (atau timestamp `called_at` diperbarui karena dipanggil ulang), `AlertAudioEngine` memutar bel ding-dong, memicu pola getar, membacakan TTS Bahasa Indonesia, dan menampilkan dialog panggilan penuh.
- **Kontrol Loket Petugas**: Memilih loket menyimpan loket aktif pada sesi petugas. Menekan **Panggil Antrean Berikutnya** memilih antrean `waiting` terlama sesuai filter kategori dan mengirim `PATCH /rest/v1/queues?id=eq.<id>` dengan JWT petugas (`status = "called"`, `counter_name`, `called_by`, `called_at`). Aksi **Panggil Ulang** memperbarui `called_at` agar perangkat mahasiswa berbunyi kembali serta menaikkan penghitung panggilan lokal (1x–3x). Aksi **Mulai Layani**, **Selesai**, dan **Lewati** memperbarui status beserta timestamp `served_at` / `completed_at` secara atomik dengan penanganan error dan pemulihan sesi otomatis.
- **Izin Android & Kepatuhan Kebijakan**: Hanya mendeklarasikan izin minimal yang benar-benar digunakan (`INTERNET`, `ACCESS_NETWORK_STATE`, `VIBRATE`) tanpa izin sensitif/berlebih.
