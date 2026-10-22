# Liquid Glass untuk launcher cleanyourtv

Fork ini menambahkan tampilan ala iOS terbaru ke launcher `cleanyourtv`, diuji di STB ZTE B866F V1.1 (IndiHome, Android TV 11).
Semua fitur asli tetap ada; cara build dan pasang sama dengan [README](../README.md).

## Yang ditambahkan

**Home**
- Gaya *liquid glass*: tombol Wi-Fi/Setelan, dock, dan menu dropdown memakai kaca bening dengan kilau di atas dan bingkai terang.
  Android 11 belum punya blur, jadi efeknya ditiru dengan gradasi transparan.
- Baris cuaca di samping jam (Open-Meteo, tanpa API key) plus prakiraan hujan berikutnya: `26° Berawan · Hujan ~15.00`.
- Baris statistik: CPU, RAM, dan kecepatan internet (↓/↑) yang diperbarui tiap 3 detik.
- Titik ping di tombol Wi-Fi: hijau < 80 ms, kuning < 200 ms, merah di atasnya atau putus.
- Tombol Setelan hanya ikon.
- Kartu media: muncul saat ada aplikasi yang memutar media (Spotify, YouTube, ...), berisi sampul, judul, artis,
  progress bar, dan tombol ⏮ ⏯ ⏭. Warna kartu mengikuti sampul album. Saat daftar aplikasi digulir, kartu berubah jadi
  mini player berbentuk kapsul di bawah tombol Wi-Fi.

**Control Center** (tekan tombol Setelan di Home)
- Modul beda ukuran seperti iOS: blok koneksi 2×2 (Wi-Fi, Bluetooth, Suara, Tampilan), Tidur, Timer tidur, Penyimpanan,
  Aplikasi, Sistem, Launcher, Mulai ulang, Matikan.
- **Tidur** membuat STB standby; **Timer tidur** berganti Mati → 30 → 60 → 90 menit.
- **Mulai ulang / Matikan** ditekan dua kali (tekan lagi dalam 4 detik).

## Setelan

Di `launcher/app/src/main/res/values/config.xml`:

| Kunci | Fungsi |
|---|---|
| `weather_location` | `"lintang,bujur"` kota kamu, mis. `-6.2,106.8` (Jakarta). Kosong = baris cuaca mati. |
| `dock_apps` | Urutan aplikasi di dock (nama paket). |
| `show_stats` | Baris CPU / RAM / internet. |
| `settings_key_panel` | Tombol Setelan membuka Control Center. |

## Izin tambahan setelah pasang

Beberapa fitur butuh izin yang hanya bisa diberikan lewat ADB. Jalankan setelah `build-and-install.sh`
(ulangi setiap kali launcher dipasang ulang, karena Android mencabutnya saat reinstall):

```bash
# Tidur dan timer tidur membuat STB benar-benar standby (tanpa ini: hanya screensaver)
adb shell settings put secure enabled_accessibility_services com.gathin.tvhome/.SleepService
adb shell settings put secure accessibility_enabled 1

# Kartu media membaca pemutar yang sedang aktif
adb shell cmd notification allow_listener com.gathin.tvhome/com.gathin.tvhome.MediaListener
```

**Mulai ulang** dan **Matikan** butuh root (Magisk). Saat pertama dipakai, Magisk menampilkan *Permintaan superuser*
di TV: pilih **Izinkan**. Tanpa root, kedua tombol hanya menampilkan pesan "Perlu akses root". Memberi root ke launcher
berarti launcher punya akses penuh ke perangkat; pertimbangkan sebelum mengizinkan.

## Daftar debloat ZTE B866F

[`examples/zte-b866f-v1.1/debloat.txt`](../examples/zte-b866f-v1.1/debloat.txt) berisi paket yang aman di-disable di
STB ZTE B866F (IndiHome). `com.ztestb.updater` dan `com.ztestb.upgrade` ditolak firmware dan tetap aktif.
