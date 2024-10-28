package com.example.myworkmanager

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.WorkManager
import com.example.myworkmanager.databinding.ActivityMainBinding
import android.Manifest
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), OnClickListener {
    // Deklarasi WorkManager untuk mengatur dan menjalankan background tasks
    private lateinit var workManager: WorkManager
    // Binding digunakan untuk menghubungkan komponen UI yang ada di layout
    private lateinit var binding: ActivityMainBinding
    // Variabel untuk request periodic task
    private lateinit var periodicWorkRequest: PeriodicWorkRequest

    // Launcher untuk meminta izin kepada user untuk menampilkan notifikasi
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            // Jika izin diberikan, tampilkan pesan
            if (isGranted) {
                Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show()
            } else {
                // Jika izin ditolak, tampilkan pesan
                Toast.makeText(this, "Notifications permission rejected", Toast.LENGTH_SHORT).show()
            }
        }

    // Method onCreate dipanggil saat activity pertama kali dibuat
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Menggunakan view binding untuk menghubungkan layout dengan activity
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Memeriksa apakah versi Android 33 atau lebih tinggi untuk meminta izin notifikasi
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Inisialisasi WorkManager
        workManager = WorkManager.getInstance(this)

        // Menghubungkan tombol di UI dengan listener untuk menangani klik
        binding.btnOneTimeTask.setOnClickListener(this)
        binding.btnPeriodicTask.setOnClickListener(this)
        binding.btnCancelTask.setOnClickListener(this)
    }

    // Implementasi method onClick dari OnClickListener untuk menangani event klik
    override fun onClick(view: View?) {
        when (view?.id) {
            // Ketika tombol one-time task diklik, jalankan startOneTimeTask
            R.id.btnOneTimeTask -> startOneTimeTask()
            // Ketika tombol periodic task diklik, jalankan startPeriodicTask
            R.id.btnPeriodicTask -> startPeriodicTask()
            // Ketika tombol cancel task diklik, jalankan cancelPeriodicTask
            R.id.btnCancelTask -> cancelPeriodicTask()
        }
    }

    // Method untuk memulai one-time task
    // Dengan menggunakan WorkManager, Anda bisa mengatur kapan task dieksekusi
    private fun startOneTimeTask() {
        // Mengubah textStatus untuk menunjukkan bahwa task sedang berjalan
        binding.textStatus.text = getString(R.string.status)

        //Kirim Data ke Worker
        // Membuat data input untuk dikirimkan ke Worker
        val data = Data.Builder()
            .putString(MyWorker.EXTRA_CITY, binding.editCity.text.toString())
            .build()
        //Fungsi di atas digunakan untuk membuat one-time request. Saat membuat request, Anda bisa menambahkan data untuk dikirimkan dengan membuat object
        // Data yang berisi data key-value, key yang dipakai di sini yaitu MyWorker.EXTRA_CITY. Setelah itu dikirimkan melalui setInputData.

        // Membuat constraints untuk memastikan task hanya berjalan saat ada koneksi internet
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        //setRequiredNetworkType, ketika bernilai CONNECTED berarti dia harus terhubung ke koneksi internet, apa pun jenisnya.
        // Bila kita ingin memasang ketentuan bahwa job hanya akan berjalan ketika perangkat terhubung ke network Wi-fi, maka kita perlu
        // memberikan nilai UNMETERED.

        //setRequiresDeviceIdle, menentukan apakah task akan dijalankan ketika perangkat dalam keadaan sedang digunakan atau tidak.
        // Secara default, parameter ini bernilai false. Bila kita ingin task dijalankan ketika perangkat dalam kondisi tidak digunakan,
        // maka kita beri nilai true.

        //setRequiresCharging, menentukan apakah task akan dijalankan ketika baterai sedang diisi atau tidak. Nilai true akan mengindikasikan bahwa
        // task hanya berjalan ketika baterai sedang diisi. Kondisi ini dapat digunakan bila task yang dijalankan akan memakan waktu yang lama.

        //setRequiresStorageNotLow, menentukan apakah task yang dijalankan membutuhkan ruang storage yang tidak sedikit. Secara default,
        // nilainya bersifat false.
        //Dan ketentuan lainnya yang bisa kita gunakan.




        //WorkRequest
        //Selanjutnya, mari kita bedah kode untuk membuat WorkManager di kelas MainActivity, Ada dua macam request yang bisa dibuat, yaitu:

        //OneTimeWorkRequest untuk menjalankan task sekali saja, untuk membuatnya Anda menggunakan kode berikut:
        // Membuat OneTimeWorkRequest dengan constraints dan data yang sudah dibuat
        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(MyWorker::class.java)
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        // Menjalankan task menggunakan WorkManager
        workManager.enqueue(oneTimeWorkRequest)


        //WorkInfo
        //WorkInfo digunakan untuk mengetahui status task yang dieksekusi, perhatikan kode di dalam MainActivity.
        // Mengamati status dari work request dan memperbarui UI
        workManager.getWorkInfoByIdLiveData(oneTimeWorkRequest.id)
            .observe(this@MainActivity) { workInfo ->
                // Mendapatkan status dari task yang sedang berjalan
                val status = workInfo.state.name
                // Menambahkan status ke textStatus
                binding.textStatus.append("\n" + status)
            }
    //Anda dapat membaca status secara live dengan menggunakan getWorkInfoByIdLiveData. Anda juga bisa memberikan aksi pada state
    // tertentu dengan mengambil data state dan membandingkannya dengan konstanta yang bisa didapat di WorkInfo.State.
    // Misalnya, pada kode di atas kita mengatur tombol Cancel task aktif jika task dalam state ENQUEUED.
    }

    // Method untuk memulai periodic task (task yang berjalan berulang setiap 15 menit)
    private fun startPeriodicTask() {
        // Mengubah textStatus untuk menunjukkan bahwa task sedang berjalan
        binding.textStatus.text = getString(R.string.status)

        //Kirim Data ke Worker
        // Membuat data input untuk dikirimkan ke Worker
        val data = Data.Builder()
            .putString(MyWorker.EXTRA_CITY, binding.editCity.text.toString())
            .build()

        // Membuat constraints untuk memastikan task hanya berjalan saat ada koneksi internet
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        //PeriodicWorkRequest untuk menjalankan task secara periodic, untuk membuatnya Anda menggunakan kode berikut:
        // Membuat PeriodicWorkRequest dengan interval 15 menit
        periodicWorkRequest = PeriodicWorkRequest.Builder(MyWorker::class.java, 15, TimeUnit.MINUTES)
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        // Menjalankan periodic task menggunakan WorkManager
        workManager.enqueue(periodicWorkRequest)

        // Mengamati status dari periodic task dan memperbarui UI
        workManager.getWorkInfoByIdLiveData(periodicWorkRequest.id)
            .observe(this@MainActivity) { workInfo ->
                // Mendapatkan status dari task yang sedang berjalan
                val status = workInfo.state.name
                // Menambahkan status ke textStatus
                binding.textStatus.append("\n" + status)

                // Menonaktifkan tombol cancel jika status task belum dijadwalkan
                binding.btnCancelTask.isEnabled = false
                // Mengaktifkan tombol cancel jika task telah masuk ke status ENQUEUED
                if (workInfo.state == WorkInfo.State.ENQUEUED) {
                    binding.btnCancelTask.isEnabled = true
                }
            }
    }

    // Method untuk membatalkan periodic task
    private fun cancelPeriodicTask() {
        // Membatalkan task berdasarkan ID dari periodicWorkRequest
        workManager.cancelWorkById(periodicWorkRequest.id)
    }
    //Kode di atas digunakan untuk membatalkan task berdasarkan id request. Selain menggunakan id, Anda juga bisa menambahkan tag pada request.
// Kelebihan dari penggunaan tag yaitu Anda bisa membatalkan task lebih dari satu task sekaligus
}
