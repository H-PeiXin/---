package com.h2004c.download;

import android.util.Log;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;


/**
 * 多线程下载
 * 多线程断点续传: 一旦下载中断,下次下载的时候需要接着下
 */
public class MultiDownload {

    //线程数量
    public static final int THREAD_COUNT =3;
    private static String TAG = "MultiDownload";
    //多线程下载
    public static final int TYPE_MULTI = 0;
    //断点续传
    public static final int TYPE_MULTI_CONTINUE = 1;

    //多线程下载,HttpUrlConnection
    //1.拿到任务量
    //2.尽可能平分任务,但是如果无法平分,规定让某个(最后一个线程)线程将多余的任务承担下来
    //3.每个线程拿到自己的任务之后分别取做下载就可以了(RandomAccessFile)
    public static void download(final String url, final String savePath, final int type){
        ThreadManager.getInstance()
                .execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            URL url1 = new URL(url);
                            HttpURLConnection conn = (HttpURLConnection) url1.openConnection();
                            if (conn.getResponseCode() == 200) {
                                //1517570
                                ////1.拿到任务量
                                int contentLength = conn.getContentLength();
                                //平均任务量
                                // 11/ 3 = 3
                                int avarage = contentLength / THREAD_COUNT;
                                //2.尽可能平分任务,但是如果无法平分,规定让某个(最后一个线程)线程将多余的任务承担下来
                                //假设任务量 11个字节 , 0 到 10
                                //线程0: 0,1,2  --- 3个字节
                                //线程1: 3,4,5 --- 3个字节
                                //线程2: 6,7,8,9,10 -- 5个字节

                                //需要开启三个线程去下载任务
                                for (int i = 0; i < THREAD_COUNT; i++) {

                                    //任务起始位置
                                    int start = i * avarage;
                                    //非最后一个线程的任务结束位置
                                    int end = (i+1)*avarage-1;
                                    //最后一个线程
                                    if (i == THREAD_COUNT -1){
                                        end = contentLength -1;
                                    }

                                    //3.每个线程拿到自己的任务之后分别取做下载就可以了()
                                    if (type == TYPE_MULTI){
                                        multiDown(i,start,end,url,savePath);
                                    }else {
                                        multiContinueDown(i,start,end,url,savePath);
                                    }

                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    //开启各个线程去分别下载任务
    private static void multiDown(final int threadId, final int start,
                                  final int end, final String url, final String savePath) {
        ThreadManager.getInstance()
                .execute(new Runnable() {
                    @Override
                    public void run() {
                        //每个线程都去取自己的任务去下载
                        try {
                            URL url1 = new URL(url);
                            HttpURLConnection conn = (HttpURLConnection) url1.openConnection();
                            //从服务器上拿到我们想要的部分资源
                            conn.setRequestProperty("Range","bytes="+start+"-"+end);
                            //200: 拿到全部资源的请求吗
                            //206: 拿到部分资源成功的请求码
                            if (conn.getResponseCode() == HttpURLConnection.HTTP_PARTIAL) {
                                InputStream inputStream = conn.getInputStream();

                                //读写,读写也得注意
                                //随机访问流,
                                //特征:
                                //1.可以读也可以写,输出流的作用
                                //2.可以指定读写的位置
                                RandomAccessFile raf = new RandomAccessFile(savePath,"rw");
                                //指定位置去写
                                raf.seek(start);

                                byte[] bytes = new byte[1024];
                                int len = 0;
                                int count = 0;
                                while ((len = inputStream.read(bytes)) != -1){
                                    raf.write(bytes,0,len);
                                    count += len;
                                    Log.d(TAG, "threadId:"+threadId+"总任务量: "+(end - start+1)+",count:"+count);
                                }

                                raf.close();
                                inputStream.close();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

    }


    //开启各个线程去分别下载任务,断点续传
    //1.每次读写都需要记录每个线程下载的位置,
    //2.每次开始下载的时候都需要判断究竟需不需要断点续传
    private static void multiContinueDown(final int threadId, final int start,
                                  final int end, final String url, final String savePath) {
        ThreadManager.getInstance()
                .execute(new Runnable() {
                    @Override
                    public void run() {
                        //每个线程都去取自己的任务去下载
                        try {
                            URL url1 = new URL(url);
                            HttpURLConnection conn = (HttpURLConnection) url1.openConnection();

                            //上次下载的位置  0 -  100 ,lastIndex  60
                            int lastIndex = (int) SpUtil.getParam("thread:" + threadId,
                                    0);
                            if (lastIndex == 0){
                                lastIndex = start;
                            }
                            //从服务器上拿到我们想要的部分资源
                            conn.setRequestProperty("Range","bytes="+lastIndex+"-"+end);
                            //200: 拿到全部资源的请求吗
                            //206: 拿到部分资源成功的请求码
                            if (conn.getResponseCode() == HttpURLConnection.HTTP_PARTIAL) {
                                InputStream inputStream = conn.getInputStream();

                                //读写,读写也得注意
                                //随机访问流,
                                //特征:
                                //1.可以读也可以写,输出流的作用
                                //2.可以指定读写的位置
                                RandomAccessFile raf = new RandomAccessFile(savePath,"rw");
                                //指定位置去写
                                raf.seek(lastIndex);

                                byte[] bytes = new byte[1024];
                                int len = 0;
                                //下载进度
                                int count = 0;
                                while ((len = inputStream.read(bytes)) != -1){
                                    raf.write(bytes,0,len);
                                    count += len;
                                    Log.d(TAG, "threadId:"+threadId+"总任务量: "+(end - lastIndex+1)+",count:"+count);

                                    // threadId:2总任务量: 505858,count:85115
                                    //threadId:2总任务量: 421767,count:344064
                                    //threadId:1总任务量: 505856,count:131708
                                    //threadId:1总任务量: 375172,count:375172
                                    //每次读写都需要记录每个线程下载的位置,
                                    SpUtil.setParam("thread:"+threadId,(lastIndex+count));
                                }


                                //如果下载完成了,将下载的位置记录为0
                                SpUtil.setParam("thread:"+threadId,0);

                                raf.close();
                                inputStream.close();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

    }
}
