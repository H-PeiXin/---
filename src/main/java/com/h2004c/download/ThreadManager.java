package com.h2004c.download;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 单例: app运行的过程中,当前类的对象只能有一个
 * 1.私有化构造
 */
public class ThreadManager {
    //单例:饿汉,懒汉
    //饿汉

    private static final ThreadManager sManager = new ThreadManager();
    private final ThreadPoolExecutor mExecutor;

    private ThreadManager(){
        //线程池
        /**
         * int corePoolSize,核心线程的数量
         * int maximumPoolSize,最大线程数量
         * long keepAliveTime,非核心线程存活的时间
         * TimeUnit unit,存活的时间单位
         * BlockingQueue<Runnable> workQueue 排队策略
         */
        mExecutor = new ThreadPoolExecutor(3,
                10, 1,
                TimeUnit.MINUTES, new LinkedBlockingDeque<Runnable>());

    }

    public static ThreadManager getInstance(){
        return sManager;
    }


    //执行任务
    public void execute(Runnable runnable){
        if (runnable != null){
            mExecutor.execute(runnable);
        }
    }

    //移除任务
    public void remove(Runnable runnable){
        if (runnable != null){
            mExecutor.remove(runnable);
        }
    }

}
