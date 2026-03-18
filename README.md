# AktivitasKu 📱

Aplikasi Android offline untuk menyimpan dan mengelola kegiatan dengan alarm & notifikasi presisi.
Tema: **Biru · Putih · Teal (`#00C9A7`)**

---

## Fitur Utama

| Fitur | Keterangan |
|-------|------------|
| Input Suara | `SpeechRecognizer` bawaan Android (Bahasa Indonesia, offline) |
| Input Teks | Form lengkap dengan validasi |
| Alarm Presisi | `AlarmManager.setExactAndAllowWhileIdle()` — aktif saat layar mati |
| Notifikasi | Tindakan Tunda (5 menit) & Selesai langsung dari notifikasi |
| Jadwal Berulang | Harian / Mingguan / Bulanan / Kustom hari |
| Kategori & Prioritas | 5 kategori berwarna, 3 level prioritas |
| Statistik | Grafik mingguan, streak, breakdown kategori |
| Home Widget | Widget layar utama via Glance API |
| Backup/Restore | Export/import JSON lokal |
| Dark Mode | Penuh — semua warna otomatis beradaptasi |
| Reschedule on Boot | Alarm dijadwalkan ulang setelah restart HP |

---

## Tech Stack

```
Kotlin 2.0         - Bahasa utama
Jetpack Compose    - UI framework
Room Database      - SQLite wrapper offline
AlarmManager       - Alarm presisi
WorkManager        - Background tasks + reschedule
Hilt               - Dependency injection
Navigation Compose - Navigasi antar layar
Glance API         - Home screen widget
SpeechRecognizer   - Voice input bawaan Android
```

---

## Struktur Project

```
app/src/main/java/com/aktivitasku/
│
├── AktivitasKuApp.kt          ← @HiltAndroidApp
├── MainActivity.kt            ← Entry point Compose
├── SplashActivity.kt          ← Splash + permission check
│
├── data/
│   ├── local/
│   │   ├── dao/ActivityDao.kt
│   │   ├── database/AppDatabase.kt + Converters
│   │   └── entity/ActivityEntity.kt
│   └── repository/ActivityRepository.kt
│
├── di/
│   └── AppModule.kt           ← Hilt providers
│
├── domain/model/
│   └── Activity.kt            ← Domain model + enums
│
├── presentation/
│   ├── theme/
│   │   ├── Color.kt           ← Full palette: Blue, White, Teal
│   │   ├── Type.kt            ← Typography
│   │   └── Theme.kt           ← Light & Dark color scheme
│   ├── navigation/NavGraph.kt ← Bottom nav + NavHost
│   ├── home/                  ← HomeScreen + ViewModel
│   ├── add/                   ← AddActivityScreen + ViewModel
│   ├── detail/                ← DetailScreen + ViewModel
│   ├── statistics/            ← StatisticsScreen + ViewModel
│   ├── components/            ← ActivityCard, CategoryChip, dll
│   └── widget/                ← Glance home screen widget
│
├── service/
│   ├── AlarmScheduler.kt      ← Schedule / cancel alarms
│   ├── AlarmReceiver.kt       ← Tampilkan notifikasi saat alarm
│   ├── AlarmActionReceiver.kt ← Handle Tunda / Selesai
│   └── BootReceiver.kt        ← Reschedule setelah reboot
│
└── util/
    ├── VoiceParser.kt         ← NLP parser Bahasa Indonesia
    ├── BackupManager.kt       ← Export/import JSON
    ├── PermissionHelper.kt    ← Permission request UI
    └── DateTimeUtils.kt       ← Format helper
```

---

## Setup

### 1. Buka di Android Studio
```
File → Open → pilih folder AktivitasKu/
```

### 2. Sync Gradle
```
Tunggu Gradle sync selesai (butuh koneksi internet pertama kali)
```

### 3. Tambahkan ikon (wajib untuk build sukses)
Buat file-file berikut di `res/drawable/`:
- `ic_notification.xml` — ikon notifikasi (24dp, putih, vector)
- `ic_splash_logo.xml` — ikon splash screen

Contoh `ic_notification.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
  <path android:fillColor="#FFFFFF"
    android:pathData="M12,2a7,7 0,0 0-7,7v4l-2,2v1h18v-1l-2-2V9A7,7 0,0 0,12,2zm0,20a2,2 0,0 0,2-2H10a2,2 0,0 0,2,2z"/>
</vector>
```

### 4. Run
Colok HP Android (API 26+) dan tekan Run.

---

## Izin yang Diperlukan

| Izin | Tujuan |
|------|--------|
| `SCHEDULE_EXACT_ALARM` | Alarm tepat waktu (Android 12+) |
| `POST_NOTIFICATIONS` | Notifikasi (Android 13+) |
| `RECORD_AUDIO` | Input suara |
| `RECEIVE_BOOT_COMPLETED` | Reschedule alarm setelah reboot |
| `VIBRATE` | Getar saat alarm |

---

## Warna Tema

| Peran | Warna | Hex |
|-------|-------|-----|
| Primary | Biru | `#1565C0` |
| Accent | Teal | `#00C9A7` |
| Background | Putih dingin | `#F5F9FF` |
| Dark bg | Navy gelap | `#0A0F1E` |

---

## Yang Perlu Ditambahkan (Tahap Selanjutnya)

- [ ] DatePickerDialog di `AddActivityScreen`
- [ ] TimePickerDialog di `AddActivityScreen`
- [ ] Ikon launcher di `res/mipmap/`
- [ ] Unit tests untuk `VoiceParser`
- [ ] Widget data binding (connect real Room data ke widget)
- [ ] Export ke Google Calendar (opsional)
