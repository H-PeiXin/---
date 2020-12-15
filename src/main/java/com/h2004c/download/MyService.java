package com.h2004c.download;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.ResourceSubscriber;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;


/**
 * service生命周期
 * 绑定服务bindService()
 * onCreate --- > onBind()   --  >  onUnBind()  --> onDestroy(unbindService();)
 *
 *
 * 启动服务startService
 * onCreate --  > onStartCommand  -- > onDestroy()(stopService())
 *
 * onCreate方法只有第一次启动/绑定服务的时候才会调用,只要服务没有销毁,就不会再走了
 */
public class MyService extends Service {

    private String TAG = "MyService";
    String savaFile = "/storage/emulated/0/cc.apk";
    public MyService() {
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String url = intent.getStringExtra("url");
        Log.d(TAG, "onStartCommand: "+url);
        download(url);
        return super.onStartCommand(intent, flags, startId);

    }

    private void download(String url) {
        new Retrofit.Builder()
                .baseUrl(ApiService.sUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(ApiService.class)
                .download()
                .subscribeOn(Schedulers.io())
                //.observeOn()//观察者运行的线程,如果不指定,直接使用被观察者的线程
                .subscribeWith(new ResourceSubscriber<ResponseBody>() {
                    @Override
                    public void onNext(ResponseBody responseBody) {
                        saveFile(responseBody.byteStream(),
                                responseBody.contentLength());
                    }

                    @Override
                    public void onError(Throwable t) {
                        Log.d(TAG, "onError: " + t.toString());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void saveFile(InputStream inputStream, final long max) {

        File file = new File(savaFile);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(savaFile);
            byte[] bytes = new byte[1024];
            int len;

            int count = 0;
            while ((len = inputStream.read(bytes)) != -1) {
                fos.write(bytes, 0, len);
                count += len;
                Log.d(TAG, "saveFile: " + Thread.currentThread().getName() + "," + count + ",max:" + max);

                //子线程
                EventBus.getDefault().post(new EventMsg((int) max,count));
            }
            inputStream.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
    }
}
