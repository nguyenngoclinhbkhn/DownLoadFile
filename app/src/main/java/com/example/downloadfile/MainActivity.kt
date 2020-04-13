package com.example.downloadfile

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.downloadfile.client.Client
import com.example.downloadfile.model.Photo
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var imageFileFolder: File
    private lateinit var imageFileName: File
    private var progressDialog: ProgressDialog? = null
    private val listPhoto = ArrayList<Photo>()
    private var listFake = ArrayList<Photo>()
    private val numberSize = 1000
    private lateinit var handler: Handler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createFake()
        requestPermission(
            arrayOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ), 111
        )
        progressDownload.max = 4999

        handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                progressDownload.setProgress(msg.arg1 * 100 / 5000)
                txtPercent.text = "${msg.arg1 * 100 / 5000} %"
            }
        }
        btnDownVer1.setOnClickListener(this)
        btnDownVer2.setOnClickListener(this)
        btnDownVer3.setOnClickListener(this)
    }


    private fun requestPermission(permission: Array<String>, requestCode: Int) {
        if (requestCode == 111 && ActivityCompat.checkSelfPermission(
                this,
                permission[0]
            ) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                permission[1]
            ) == PackageManager.PERMISSION_GRANTED
        ) { //TODO
            imageFileFolder = File(Environment.getExternalStorageDirectory(), "RxKotlin")
            imageFileFolder.mkdir()
            Toast.makeText(this, "Let download image", Toast.LENGTH_SHORT).show()
        } else { // ==> gui yeu cau de nguoi dung cho phep
            ActivityCompat.requestPermissions(
                this@MainActivity, arrayOf(permission[0], permission[1]), requestCode
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 111 && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults.size > 0 && grantResults[1] == PackageManager.PERMISSION_GRANTED
        ) { //nguoi dung dong y
            imageFileFolder = File(Environment.getExternalStorageDirectory(), "Test")
            imageFileFolder.mkdir()
            Toast.makeText(this, "Let download image", Toast.LENGTH_SHORT).show()
        } else { // nguoi dung ko dong y
            finish()
        }
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnDownVer1 -> {
                showDialog()
                dowVer1()

            }
            R.id.btnDownVer2 -> {
                showDialog()
                val call = Client.createAPI().getListPhotoVer2()
                call.enqueue(object : Callback<List<Photo>> {
                    override fun onFailure(call: Call<List<Photo>>, t: Throwable) {
                        cancelDialog()
                    }

                    override fun onResponse(
                        call: Call<List<Photo>>,
                        response: Response<List<Photo>>
                    ) {
                        if (response != null) {
                            dowVer2()
                        }
                    }
                })


            }
            R.id.btnDownVer3 -> {
//                val it = getFake()
//                val distance = it.size / 3
//                val listTop: ArrayList<List<Photo>> = arrayListOf()
//                var start = 0
//                var end = distance;
//                var count = 0;
//                listTop.clear()
//                while (true) {
//                    if (end >= it.size) break
//                    val list = arrayListOf<Photo>()
//                    for (i in start until end) {
//                        list.add(it[i])
//                    }
//                    listTop.add(list)
//                    start += distance
//                    end += distance
//
//                    count++
//                }
//                val list = arrayListOf<Photo>()
//                for (i in (start) until it.size) {
//                    list.add(it[i])
//                }
//                listTop.add(list)
//                Log.e("TAG", "count $count")
//                Log.e("TAG", "size listtop ${listTop.size}")
//
//                for (test in 0 until listTop.size) {
//                    Log.e("TAG", "Size ${listTop[test].size}  ${listTop[test]}")
////                    createObservableVer3(test).subscribe(getObserverVer3())
//                }
//                for (test in 0 until listTop.size) {
//                    Log.e("TAG", "size ver 2 ${listTop[test].size}")
//                }

                downVer3()
            }
        }
    }

    private fun dowVer1() {
        val intThreadCount = Runtime.getRuntime().availableProcessors() - 1
        val executorService = Executors.newFixedThreadPool(intThreadCount)
        val schedulers = Schedulers.from(executorService)
        val singlePhoto = Client.createAPI().getListPhoto(); // lấy list photo từ json thật
        singlePhoto.subscribeOn(Schedulers.io())
            .flatMap {
                Observable.create<Photo> { emitter ->
                    listFake.forEach { it ->
                        // fake list lấy từ json vì url ảnh của json ko dow ảnh được
                        val bitmap = getBitmapFromURL(it.url)
                        savePhoto(bitmap)
                        emitter.onNext(it)
                    }
                    emitter.onComplete()
                }
                    .subscribeOn(schedulers)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Photo> {
                override fun onComplete() {
                    Log.e("OnComplete", "OnComplete ${listPhoto.size}")
                    cancelDialog()
                }

                override fun onSubscribe(d: Disposable) {
                }

                override fun onNext(t: Photo) {
                    Log.e("TAG", "onNext $t")
                    listPhoto.add(t)
                    progressDownload.progress = listPhoto.size * 100 / numberSize
                    txtPercent.text = " ${listPhoto.size * 100 / numberSize}  %"
                }

                override fun onError(e: Throwable) {
                }
            })
    }

    private fun dowVer2() {
        var count = 0
        val executors =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1)
        listFake.forEach { it ->
            val thread = Thread(Runnable {
                savePhoto(getBitmapFromURL(it.url))
                count++
                val message = Message();
                message.arg1 = count;
                handler.sendMessage(message);

            })
            executors.execute(thread)
        }

        while (!executors.isTerminated) {

        }
        cancelDialog()

    }

    private fun downVer3() {
        val count = Runtime.getRuntime().availableProcessors() - 1;
        val single = Client.createAPI().getListPhoto();
        progressDownload.max = 101
        single.subscribeOn(Schedulers.io())
            .map { it ->
                getFake()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ it ->
                val distance = it.size / count
                val listTop: ArrayList<List<Photo>> = arrayListOf()
                var start = 0
                var end = distance;
                listTop.clear()
                while (true) {
                    if (end >= it.size) break
                    val list = arrayListOf<Photo>()
                    for (i in start until end) {
                        list.add(it[i])
                    }
                    listTop.add(list)
                    start += distance
                    end += distance
                }
                val list = arrayListOf<Photo>()
                for (i in (start) until it.size) {
                    list.add(it[i])
                }
                listTop.add(list)
               for (test in 0 until listTop.size) {
                   Log.e("TAG", "Size ${listTop[test].size}  ${listTop[test]}")
                    createObservableVer3(listTop[test]).subscribe(getObserverVer3())
               }
            },
                { error ->
                    Log.e("TAG", "Error ${error.toString()}")
                })

    }

    private fun createObservableVer3(list: List<Photo>): Observable<Photo> {
        return object : Observable<Photo>() {
            override fun subscribeActual(observer: Observer<in Photo>?) {
                for (test in list) {
                    savePhoto(getBitmapFromURL(test.url))
                    observer?.onNext(test)
                }
                observer?.onComplete()
            }

        }.subscribeOn(Schedulers.from(Executors.newFixedThreadPool(1)))
            .observeOn(AndroidSchedulers.mainThread())
    }

    private var countVer3 = 0;
    private fun getObserverVer3(): Observer<Photo> {
        return object : Observer<Photo> {
            override fun onComplete() {
                Log.e("TAG", "complete")

            }

            override fun onSubscribe(d: Disposable) {
            }

            override fun onNext(t: Photo) {
                countVer3++;
                progressDownload.progress = countVer3 * 100 / numberSize
                txtPercent.text = " ${countVer3 * 100 / numberSize}  %"
                Log.e("TAG", "countVer3 $countVer3")
            }

            override fun onError(e: Throwable) {
            }

        }
    }

    private fun showDialog() {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this)
        }
        progressDialog?.show()
    }

    private fun cancelDialog() {
        if (progressDialog != null) {
            progressDialog?.cancel()
        }
    }


    fun getBitmapFromURL(src: String?): Bitmap? {
        val url = URL(src);
        val bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
        return bmp
    }

    fun savePhoto(bmp: Bitmap?) {
        var out: FileOutputStream? = null
        imageFileName = File(imageFileFolder, "${System.currentTimeMillis()}.jpg")
        try {
            out = FileOutputStream(imageFileName)
            bmp?.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
            out = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createFake() {
        for (i in 0..numberSize) {
            listFake.add(Photo(1, 1, "sdf", "http://placehold.jp/150x150.png", "sdf"))
        }
    }

    private fun getFake(): List<Photo> {
        var listFake = ArrayList<Photo>()
        for (i in 0..numberSize) {
            listFake.add(Photo(1, 1, "sdf", "http://placehold.jp/150x150.png", "sdf"))
        }

        return listFake
    }


}
