package com.h2004c.download;

import androidx.annotation.StringRes;
import androidx.constraintlayout.helper.widget.Flow;

import io.reactivex.Flowable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Streaming;

public interface ApiService {

    String sUrl = "https://alissl.ucdl.pp.uc.cn/";

    /**
     * 如果不添加Streaming, io流会等到全部读到内存的时候才会回调onNext方法
     * 添加了Streaming,拿到一部分流,就会调用onNext
     * @return
     */
    @Streaming
    @GET("fs08/2017/05/02/7/106_64d3e3f76babc7bce131650c1c21350d.apk")
    Flowable<ResponseBody> download();
}
