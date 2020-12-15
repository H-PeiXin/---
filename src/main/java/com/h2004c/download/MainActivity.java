package com.h2004c.download;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.ResourceSubscriber;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

/**
 * 单线程下载:
 * ok,Retrofit,HttpUrlConnection
 * 1.获取io流(输入流)
 * 2.写io流
 * <p>
 * 安装: 适配高版本(6.0,7.0,8.0以上)
 * <p>
 * 多线程下载
 * <p>
 * 多线程断点续传
 * <p>
 * 12.4
 * 1.服务中下载,进度传,activity
 * 2.安装 :适配(6.0以下,7.0, 8.0及其以上),代码是固定的
 * 7.0: 清单文件需要添加内容提供者(authority),需要在InstallUtil中7.0的安装方法中配置
 * 8.0:未知来源权限,使用的地方onActivityResult()
 * 3.多线程下载
 * <p>
 * 4.多线程断点续传
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private Button mBtnOk;
    private Button mBtnRetrofit;
    private Button mBtnHttp;
    private ProgressBar mPb;
    private TextView mTv;

    //5-6M
    private String mUrl = "https://github.com/iceCola7/WanAndroid/raw/master/app/release/WanAndroid-release.apk";
    //1.6M,计算器
    private String mUrl2 = "https://alissl.ucdl.pp.uc.cn/fs08/2017/05/02/7/106_64d3e3f76babc7bce131650c1c21350d.apk";

    String savaFile = "/storage/emulated/0/a.apk";
    private Button mBtnService;
    private Button mBtnStop;
    private Button mBtnMulti;
    private Button mBtnMutltiContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        //sp 分为存 和 取
        //存
        /*SharedPreferences sp = getSharedPreferences("xx", Context.MODE_PRIVATE);
        sp.edit().putFloat("age",1.0f).commit();

        //取
        float age = sp.getFloat("age", 0);*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void initView() {
        mBtnOk = (Button) findViewById(R.id.btn_ok);
        mBtnRetrofit = (Button) findViewById(R.id.btn_Retrofit);
        mBtnHttp = (Button) findViewById(R.id.btn_http);
        mPb = (ProgressBar) findViewById(R.id.pb);
        mTv = (TextView) findViewById(R.id.tv);

        mBtnOk.setOnClickListener(this);
        mBtnRetrofit.setOnClickListener(this);
        mBtnHttp.setOnClickListener(this);
        mBtnService = (Button) findViewById(R.id.btn_service);
        mBtnService.setOnClickListener(this);
        mBtnStop = (Button) findViewById(R.id.btn_stop);
        mBtnStop.setOnClickListener(this);
        mBtnMulti = (Button) findViewById(R.id.btn_multi);
        mBtnMulti.setOnClickListener(this);
        mBtnMutltiContinue = (Button) findViewById(R.id.btn_mutlti_continue);
        mBtnMutltiContinue.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_ok:
                //不能直接下载,应该处理sd权限
                // okDownload();
                initPers();
                break;
            case R.id.btn_Retrofit:
                retrofitDown();
                break;
            case R.id.btn_http:
                http();
                break;
            case R.id.btn_service:
                //启动服务
                Intent intent = new Intent(this, MyService.class);
                intent.putExtra("url", mUrl2);
                startService(intent);

                break;
            case R.id.btn_stop:
                //停止服务
                stopService(new Intent(this, MyService.class));
                break;
            case R.id.btn_multi:
                MultiDownload.download(mUrl2,"/storage/emulated/0/ww.apk",MultiDownload.TYPE_MULTI);
                break;
            case R.id.btn_mutlti_continue:
                MultiDownload.download(mUrl2,"/storage/emulated/0/ww.apk",MultiDownload.TYPE_MULTI_CONTINUE);
                break;
        }
    }

    private void http() {
        ThreadManager.getInstance()
                .execute(new Runnable() {
                    @Override
                    public void run() {
                        //子线程
                        Log.d(TAG, "run: " + Thread.currentThread().getName());
                        try {
                            URL url = new URL(mUrl2);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            if (conn.getResponseCode() == 200) {
                                InputStream inputStream = conn.getInputStream();

                                //读写io流
                                saveFile(inputStream, conn.getContentLength());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

    }

    private void retrofitDown() {
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

    private void initPers() {
        //危险权限处理分为几步
        //1.清单文件配置权限
        //2.检测权限,有,该干干嘛,没有,第三步
        int result = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        //有权限
        //    public static final int PERMISSION_GRANTED = 0;
        //没有权限
        //    public static final int PERMISSION_DENIED = -1;
        if (result == PackageManager.PERMISSION_GRANTED) {
            //有权限
            okDownload();
        } else {
            //没有权限
            //3.申请权限
            String[] pers = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            ActivityCompat.requestPermissions(this, pers, 100);
        }
        //4.处理申请结果,onRequestPermissionsResult()

    }

    ////4.处理申请结果,
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //有权限
                okDownload();
            } else {
                //没有权限
                showToast("用户拒绝了");
            }
        }
    }

    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void okDownload() {
         /*1.获取io流(输入流)
         2.写io流*/
        //client.newCall().enqueue()
        Request request = new Request.Builder()
                .get()//默认get,可以不指定
                .url(mUrl2)
                .build();
        new OkHttpClient()
                .newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        //response.body().string()
                        //1.获取io流(输入流)
                        InputStream inputStream = response.body().byteStream();
                        //文件总大小
                        long max = response.body().contentLength();
                        //2.写io流,FileOutputStream
                        saveFile(inputStream, max);
                    }
                });
    }

    private void saveFile(InputStream inputStream, final long max) {
        ///2.写io流,FileOutputStream 往sd卡写
        //危险权限(9组): 联系人,电话,sd,日历,位置,体感传感器,麦克风,相机,短信
        //危险权限处理分为几步
        //1.清单文件配置权限
        //2.检测权限,有,该干干嘛,没有,第三步
        //3.申请权限
        //4.处理申请结果
        //java.io.FileNotFoundException: /storage/emulated/0/a.apk:
        // open failed: EACCES (Permission denied)
        File file = new File(savaFile);
        //file.exists() 判断文件是否存在,true,代表存在
        try {
            //如果文件不存在,创建

            if (!file.exists()) {
                file.createNewFile();
            }
            Log.d(TAG, "saveFile: " + file.exists());

            FileOutputStream fos = new FileOutputStream(savaFile);
            byte[] bytes = new byte[1024];
            int len;

            //当前读写了多少
            int count = 0;
            //将内容读到字节数组中
            while ((len = inputStream.read(bytes)) != -1) {
                //写,把字节数组中的内容写到文件中
                //从0开始,写len
                fos.write(bytes, 0, len);
                //count = count+len;
                count += len;
                Log.d(TAG, "saveFile: " + Thread.currentThread().getName() + "," + count + ",max:" + max);

                //更新进度条
                //子线程里能刷洗的ui:Progressbar,SurfaceView
                final int finalCount = count;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //long :8个字节,int: 4字节
                        //long类型强转为int类型有可能经度丢失
                        mPb.setMax((int) max);
                        mPb.setProgress(finalCount);

                        mTv.setText(finalCount + "/" + max);

                        //一旦max == finalCount相等了,下载完了,需要在主线程里面调用
                        if (max == finalCount) {
                            InstallUtil.installApk(MainActivity.this, savaFile);
                        }
                    }
                });
            }

            //走到这里,说明读写完了
            //关流
            inputStream.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //线程模型,如果不做切换,默认Posting,发事件什么线程,收也是什么线程
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void recevie(EventMsg msg) {
        //runOnUiThread();
        mPb.setMax(msg.max);
        mPb.setProgress(msg.progress);

        // 1/ 2 -->0
        float pro = msg.progress * 1.0f / msg.max * 100;
        mTv.setText(msg.progress + "/" + msg.max + "," + pro + " %");

        if (msg.max == msg.progress) {
            InstallUtil.installApk(this, savaFile);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == InstallUtil.UNKNOWN_CODE) {
            //等给了未知来源应用安装的权限之后,再调用一次安装方法
            InstallUtil.installApk(this, savaFile);//再次执行安装流程，包含权限判等
        }
    }
}
