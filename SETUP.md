# Panduan Setup AktivitasKu

## Prasyarat

| Tool | Versi minimum |
|------|--------------|
| Android Studio | Hedgehog (2023.1.1) atau lebih baru |
| JDK | 17 |
| Android SDK | API 26 (Android 8.0) — minSdk |
| Target SDK | API 35 (Android 15) |
| Gradle | 8.9 (otomatis via wrapper) |

---

## Langkah Setup

### 1. Clone / Buka Project
```
File → Open → pilih folder AktivitasKu/
```

### 2. Buat local.properties
Salin template dan isi path SDK kamu:
```bash
cp local.properties.template local.properties
```
Isi dengan path SDK Android kamu, contoh:
```
sdk.dir=/Users/username/Library/Android/sdk
```

### 3. Sync Gradle
Klik **"Sync Now"** di banner yang muncul, atau:
```
File → Sync Project with Gradle Files
```
Proses ini membutuhkan koneksi internet pertama kali (~2–5 menit).

### 4. Jalankan di HP / Emulator
- Colok HP Android (API 26+) dengan USB Debugging aktif, atau
- Buat AVD (Android Virtual Device) via **Device Manager**
- Tekan tombol **Run ▶** atau `Shift+F10`

---

## Struktur Izin yang Diminta Saat Pertama Buka

Saat pertama kali dibuka, app akan meminta izin secara bertahap:

1. **Notifikasi** — untuk mengirim alarm (Android 13+)
2. **Alarm Tepat Waktu** — membuka Settings untuk izin `SCHEDULE_EXACT_ALARM` (Android 12+)
3. **Mikrofon** — untuk input suara (bisa dilewati)

---

## Menjalankan Tests

### Unit tests (cepat, tidak butuh HP):
```bash
./gradlew test
```
atau klik kanan folder `test/` → **Run Tests**

### Instrumented tests (butuh HP/emulator):
```bash
./gradlew connectedAndroidTest
```

---

## Struktur Project Lengkap

```
AktivitasKu/
├── app/
│   ├── build.gradle.kts           ← dependencies semua library
│   ├── proguard-rules.pro         ← aturan obfuskasi release
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/aktivitasku/
│       │   │   ├── AktivitasKuApp.kt      ← @HiltAndroidApp + WorkManager
│       │   │   ├── MainActivity.kt        ← Compose entry, dark mode reaktif
│       │   │   ├── SplashActivity.kt      ← Splash + permission + onboarding flow
│       │   │   │
│       │   │   ├── data/
│       │   │   │   ├── local/dao/         ← ActivityDao (semua queries Room)
│       │   │   │   ├── local/database/    ← AppDatabase + Converters
│       │   │   │   ├── local/entity/      ← ActivityEntity + toEntity/toDomain
│       │   │   │   └── repository/        ← ActivityRepository
│       │   │   │
│       │   │   ├── di/
│       │   │   │   ├── AppModule.kt       ← provides Database + DAO
│       │   │   │   └── DataStoreModule.kt ← provides DataStore
│       │   │   │
│       │   │   ├── domain/model/
│       │   │   │   └── Activity.kt        ← model + enum: Category, Priority, Repeat
│       │   │   │
│       │   │   ├── presentation/
│       │   │   │   ├── theme/             ← Color.kt, Type.kt, Theme.kt (Blue+White+Teal)
│       │   │   │   ├── navigation/        ← NavGraph + bottom nav (3 tab)
│       │   │   │   ├── onboarding/        ← 4-halaman pager pertama kali buka
│       │   │   │   ├── home/              ← HomeScreen + ViewModel
│       │   │   │   ├── add/               ← AddActivityScreen + ViewModel (voice+text)
│       │   │   │   ├── detail/            ← DetailScreen + ViewModel
│       │   │   │   ├── statistics/        ← StatisticsScreen + ViewModel
│       │   │   │   ├── settings/          ← SettingsScreen + ViewModel (DataStore)
│       │   │   │   ├── components/        ← ActivityCard, CategoryChip, PriorityDot
│       │   │   │   └── widget/            ← Glance widget (live Room data)
│       │   │   │
│       │   │   ├── service/
│       │   │   │   ├── AlarmScheduler.kt      ← setExactAndAllowWhileIdle
│       │   │   │   ├── AlarmReceiver.kt       ← notifikasi + DND check + sound/vibrate
│       │   │   │   ├── AlarmActionReceiver.kt ← snooze (5 mnt) + selesai dari notif
│       │   │   │   ├── BootReceiver.kt        ← reschedule alarm setelah reboot
│       │   │   │   └── WidgetRefreshWorker.kt ← WorkManager, refresh widget tiap 30 mnt
│       │   │   │
│       │   │   └── util/
│       │   │       ├── VoiceParser.kt         ← NLP Bahasa Indonesia (100% offline)
│       │   │       ├── BackupManager.kt       ← export/import JSON
│       │   │       ├── PermissionHelper.kt    ← Composable permission request dialogs
│       │   │       └── DateTimeUtils.kt       ← format helpers Indonesia
│       │   │
│       │   └── res/
│       │       ├── drawable/              ← ic_notification, ic_splash_logo, launcher icons
│       │       ├── layout/                ← widget_loading, widget_preview
│       │       ├── mipmap-anydpi-v26/     ← adaptive icon XMLs
│       │       ├── values/                ← colors.xml, strings.xml, themes.xml
│       │       └── xml/                   ← widget_info, backup_rules, data_extraction_rules
│       │
│       ├── test/                          ← 10 unit test files (JVM only)
│       └── androidTest/                   ← 4 instrumented test files (butuh HP)
│
├── gradle/
│   ├── libs.versions.toml             ← semua versi + library catalog
│   └── wrapper/gradle-wrapper.properties
├── build.gradle.kts                   ← root plugins
├── settings.gradle.kts
├── gradle.properties                  ← JVM heap, parallel build, AndroidX
├── .gitignore
└── local.properties.template
```

---

## Fitur Lengkap

| Fitur | Detail |
|-------|--------|
| **Input teks** | Form lengkap: judul, deskripsi, tanggal, waktu, kategori, prioritas, pengulangan, reminder |
| **Input suara** | `SpeechRecognizer` Android (Bahasa Indonesia, offline) + NLP parser ekstrak waktu/judul/kategori |
| **Alarm presisi** | `AlarmManager.setExactAndAllowWhileIdle()` — aktif walau layar mati/hemat baterai |
| **DND (Jangan Ganggu)** | Window jam bisa diatur, support overnight (mis. 22:00–07:00) |
| **Notifikasi** | Suara + getar, aksi **Tunda 5 menit** dan **Selesai ✓** langsung dari notifikasi |
| **Reschedule on Boot** | `BootReceiver` menjadwalkan ulang semua alarm setelah HP restart |
| **Kategori** | Kerja, Pribadi, Kesehatan, Belajar, Lainnya — masing-masing berwarna |
| **Prioritas** | Rendah / Sedang / Tinggi |
| **Pengulangan** | Harian / Mingguan / Bulanan / Kustom (pilih hari) |
| **Swipe to delete** | Geser kartu ke kiri untuk hapus (dengan konfirmasi) |
| **Pencarian** | Real-time search judul + deskripsi |
| **Filter tanggal** | Strip 7 hari, tap untuk lihat kegiatan per hari |
| **Statistik** | Grafik bar mingguan, streak aktif & terpanjang, breakdown kategori dengan progress bar |
| **Home screen widget** | Glance API — data live dari Room, refresh tiap 30 menit via WorkManager |
| **Backup/Restore** | Export/import file JSON ke storage HP |
| **Dark mode** | Penuh — reaktif dari toggle di Settings, disimpan di DataStore |
| **Onboarding** | 4 halaman pager animasi, hanya muncul sekali |

---

## Tema Warna

| Peran | Nama | Hex |
|-------|------|-----|
| Primary | Biru | `#1565C0` |
| Accent | Teal | `#00C9A7` |
| Background (light) | Putih dingin | `#F5F9FF` |
| Background (dark) | Navy gelap | `#0A0F1E` |
| Error/High priority | Merah | `#EF5350` |
| Warning/Medium | Oranye | `#FFA726` |

---

## Cara Kerja Voice Input

```
Ucapan user
    ↓
SpeechRecognizer (Android bawaan, Bahasa Indonesia)
    ↓
Teks mentah → VoiceParser.parse()
    ↓
Ekstrak: tanggal (hari ini/besok/nama hari/tanggal X)
         waktu  (jam N, pagi/siang/sore/malam/subuh)
         judul  (sisa kalimat setelah hapus marker waktu)
         kategori (deteksi keyword: meeting→WORK, dokter→HEALTH, dll)
    ↓
Form auto-terisi → user review & edit → Simpan
```

Semua proses **100% offline**, tidak ada API pihak ketiga.

---

## Tips Build Release

```bash
# Generate keystore (sekali saja)
keytool -genkey -v -keystore aktivitasku.keystore \
  -alias aktivitasku -keyalg RSA -keysize 2048 -validity 10000

# Build release APK
./gradlew assembleRelease

# Build release AAB (untuk Play Store)
./gradlew bundleRelease
```

Pastikan file `aktivitasku.keystore` **tidak** di-commit ke Git (sudah ada di `.gitignore`).
