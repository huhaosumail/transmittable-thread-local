package com.alibaba.demo;


import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.TtlRunnable;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TTlWithNoStandardDemo {
    private static TransmittableThreadLocal t1  = new TransmittableThreadLocal();

    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1,1,3000, TimeUnit.MILLISECONDS,new ArrayBlockingQueue(100));


    /**
     * 第41行和第43行的区别就是没有修饰任务
     * 是不是侧面说明 由于TransmittableThreadLocal继承InheritableThreadLocal  异步线程也会同步上下文
     * 导致存在内存泄漏的风险 相当于异步线程已经被污染了 存在上下文
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args)  throws InterruptedException {
        try{
            t1.set("val1");
            Runnable runnable1 = () -> System.out.println("异步线程1: " +Thread.currentThread().getName()+":"+ t1.get());
            threadPoolExecutor.submit(TtlRunnable.get(runnable1));

        }finally {
            //删除上下文
            t1.remove();
            Thread.sleep(2000);
            //主线程获取 为空符合预期
            System.out.println("主线程试试t1: " +Thread.currentThread().getName()+":"+ t1.get());
            //第六步 异步任务重新获取
            Runnable runnable2 = () -> System.out.println("异步线程2: " +Thread.currentThread().getName()+":"+ t1.get());
            Runnable runnable3 = () -> System.out.println("异步线程3: " +Thread.currentThread().getName()+":"+ t1.get());

            //这里执行异步任务 获取为空 符合预期 但是这里符合预期是因为父线程为空  已经被remove
            //所以包装后 导致父线程传递到子线程 子线程也会为空 但是不代表子线程真的为空  子线程执行完任务后
            //会重新覆盖原来的子线程的值  但是这里看不出来 只是认为没有值了
            threadPoolExecutor.submit(TtlRunnable.get(runnable2));
            //这里执行异步任务 获取有值  明明在上面主线程remove了  是不是子线程也同步了上下文 那是不是就存在内存泄漏呢？
            threadPoolExecutor.submit(runnable3);
        }


    }
}

