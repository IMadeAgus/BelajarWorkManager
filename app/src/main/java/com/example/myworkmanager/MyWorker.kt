package com.example.myworkmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.loopj.android.http.AsyncHttpResponseHandler
import com.loopj.android.http.SyncHttpClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import cz.msebera.android.httpclient.Header
import java.text.DecimalFormat

class MyWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    companion object {
        // TAG digunakan untuk menandai log
        private val TAG = MyWorker::class.java.simpleName

        // Konstanta yang digunakan untuk menyimpan key dari extra data dan informasi notifikasi
        const val EXTRA_CITY = "city"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "channel_01"
        const val CHANNEL_NAME = "dicoding channel"
    }

    // Variabel untuk menyimpan status hasil kerja worker
    private var resultStatus: Result? = null

    // Method yang dijalankan oleh worker ketika di-execute
    override fun doWork(): Result {
        // Mendapatkan nama kota dari input data (dari Main Activity)
        val dataCity = inputData.getString(EXTRA_CITY)
        // Memanggil method untuk mendapatkan cuaca saat ini berdasarkan nama kota
        return getCurrentWeather(dataCity)
    }
    //Metode doWork adalah metode yang akan dipanggil ketika WorkManager berjalan. Kode di dalamnya akan dijalankan di background thread secara otomatis. Metode ini juga mengembalikan nilai berupa Result yang berfungsi untuk mengetahui status WorkManager yang berjalan. Ada beberapa status yang bisa dikembalikan yaitu:
    //
    //Result.success(), result yang menandakan berhasil.
    //Result.failure(), result yang menandakan gagal.
    //Result.retry(), result yang menandakan untuk mengulang task lagi.



    // Method untuk mengambil data cuaca saat ini
    private fun getCurrentWeather(city: String?): Result {
        // Menampilkan log bahwa proses pengambilan cuaca dimulai
        Log.d(TAG, "getCurrentWeather: Mulai.....")
        // Memulai looper di thread agar bisa menjalankan operasi UI seperti notifikasi
        Looper.prepare()
        // Membuat HTTP client untuk request data cuaca
        val client = SyncHttpClient()
        // Menyusun URL untuk mengakses API OpenWeatherMap
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=${BuildConfig.APP_ID}"
        // Menampilkan URL yang akan diakses
        Log.d(TAG, "getCurrentWeather: $url")
        // Melakukan request POST ke API
        client.post(url, object : AsyncHttpResponseHandler() {
            // Callback ketika request berhasil
            override fun onSuccess(statusCode: Int, headers: Array<Header?>?, responseBody: ByteArray) {
                // Mengubah response body menjadi string
                val result = String(responseBody)
                // Menampilkan hasil response dalam log
                Log.d(TAG, result)
                try {
                    // Membuat instance Moshi untuk parsing JSON response ke data class
                    val moshi = Moshi.Builder()
                        .addLast(KotlinJsonAdapterFactory())
                        .build()
                    // Membuat adapter untuk memetakan JSON ke class `Response`
                    val jsonAdapter = moshi.adapter(Response::class.java)
                    // Parsing JSON response
                    val response = jsonAdapter.fromJson(result)

                    // Jika response tidak null, maka lanjutkan pengolahan data cuaca
                    response?.let {
                        // Mengambil informasi cuaca utama dan deskripsi
                        val currentWeather = it.weatherList[0].main
                        val description = it.weatherList[0].description
                        // Mengambil suhu dalam Kelvin
                        val tempInKelvin = it.main.temperature

                        // Mengubah suhu dari Kelvin ke Celsius
                        val tempInCelsius = tempInKelvin - 273
                        // Memformat suhu menjadi dua angka desimal
                        val temperature: String = DecimalFormat("##.##").format(tempInCelsius)
                        // Menyusun judul dan pesan notifikasi
                        val title = "Current Weather in $city"
                        val message = "$currentWeather, $description with $temperature celsius"
                        // Menampilkan notifikasi
                        showNotification(title, message)
                    }
                    // Menampilkan log bahwa proses pengambilan cuaca berhasil
                    Log.d(TAG, "onSuccess: Selesai.....")
                    // Mengatur hasil worker menjadi sukses
                    resultStatus = Result.success()
                } catch (e: Exception) {
                    // Menampilkan notifikasi jika terjadi error
                    showNotification("Get Current Weather Not Success", e.message)
                    // Menampilkan log bahwa proses gagal
                    Log.d(TAG, "onSuccess: Gagal.....")
                    // Mengatur hasil worker menjadi gagal
                    resultStatus = Result.failure()
                }
            }

            // Callback ketika request gagal
            override fun onFailure(statusCode: Int, headers: Array<Header?>?, responseBody: ByteArray?, error: Throwable) {
                // Menampilkan log bahwa proses gagal
                Log.d(TAG, "onFailure: Gagal.....")
                // Menampilkan notifikasi jika request gagal
                showNotification("Get Current Weather Failed", error.message)
                // Mengatur hasil worker menjadi gagal
                resultStatus = Result.failure()
            }
        })
        // Mengembalikan hasil worker
        return resultStatus as Result
    }

    // Method untuk menampilkan notifikasi
    private fun showNotification(title: String, description: String?) {
        // Mendapatkan NotificationManager dari sistem
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Membangun notifikasi dengan judul dan pesan yang diberikan
        val notification: NotificationCompat.Builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        // Jika versi Android adalah Oreo atau lebih baru, buat NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notification.setChannelId(CHANNEL_ID)
            notificationManager.createNotificationChannel(channel)
        }
        // Menampilkan notifikasi dengan ID tertentu
        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }
}
