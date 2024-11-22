package com.am.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.am.rxlifecycle.ActivityEvent;
import com.am.rxlifecycle.LifecycleManager;
import com.am.rxlifecycle.LifecycleTransformer;
import com.am.rxlifecycle.RxLifecycle;
import com.am.rxlifecycle.retrofit.HttpHelper;
import com.jakewharton.rxbinding.view.RxView;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

public class RxJava1Activity extends AppCompatActivity {

    private LifecycleManager mLifecycleManager;
    private MyTextView myTextView;
    private Subscription mSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        startActivity(new Intent(this,RxJava2Activity.class));
//        finish();
        RxLifecycle.injectRxLifecycle(this);
        initView();
        mLifecycleManager = RxLifecycle.with(this);
        Observable.just(1)
                .compose(bindToLifecycle())
                .subscribe();
        Observable.just("34")
                .compose(this.<String>bindToLifecycle())
                .subscribe();

        mSubscription = Observable.just(1).subscribe();


        //RxLifecycle-Retrofit 模块代码demo

        //初始化HttpHelper
        HttpHelper.getInstance().setBaseUrl("https://github.com/dhhAndroid/");
        HttpHelper.getInstance().setClient(new OkHttpClient());
        HttpHelper.getInstance().setConverterFactory(GsonConverterFactory.create());
        final Api api = HttpHelper.getInstance().createWithLifecycleManager(Api.class, RxLifecycle.with(this));
        final Button button = (Button) findViewById(R.id.button);
        RxView.clicks(button)
                .flatMap(new Func1<Void, Observable<Long>>() {
                    @Override
                    public Observable<Long> call(Void aVoid) {
                        return Observable.interval(0, 1, TimeUnit.SECONDS, AndroidSchedulers.mainThread());
                    }
                })
                .take(6)
                .map(new Func1<Long, Long>() {
                    @Override
                    public Long call(Long aLong) {
                        return 5 - aLong;
                    }
                })
                .takeFirst(new Func1<Long, Boolean>() {
                    @Override
                    public Boolean call(Long aLong) {
                        button.setText(aLong + "秒后开始网络请求");
                        return aLong == 0;
                    }
                })
                .flatMap(new Func1<Long, Observable<ResponseBody>>() {
                    @Override
                    public Observable<ResponseBody> call(Long aLong) {
                        return api.RxJava1get("https://github.com/dhhAndroid/RxLifecycle");
                    }
                })
                .map(new Func1<ResponseBody, String>() {
                    @Override
                    public String call(ResponseBody body) {
                        try {
                            return body.string();
                        } catch (IOException e) {
                            return "解析错误 !";
                        }
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Toast.makeText(RxJava1Activity.this, "网络请求取消/完成了 !", Toast.LENGTH_SHORT).show();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        Log.d("RxJava1Activity", s);
                        button.setText("网络请求完成!");
                        Toast.makeText(RxJava1Activity.this, s, Toast.LENGTH_SHORT).show();
                    }
                });
        api.RxJava1get("https://github.com/dhhAndroid/RxLifecycle")
                .compose(RxLifecycle.with(this).<ResponseBody>bindOnDestroy())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody body) {

                    }
                });
    }

    private void unSubscribeTest() {
        Observable.just(1, 23, 434, 5454, 343, 346, 56, 67, 4, -1)
                //取前五个就注销
                .take(5)
                //直到条件满足,注销
                .takeUntil(new Func1<Integer, Boolean>() {
                    @Override
                    public Boolean call(Integer integer) {
                        return integer > 66666;
                    }
                })
                //直到另外一个Observable发送数据就注销,本库主要用的这个操作符
                .takeUntil(Observable.just(1))
                .first(new Func1<Integer, Boolean>() {
                    @Override
                    public Boolean call(Integer integer) {
                        return integer == 111;
                    }
                })
                .map(new Func1<Integer, Integer>() {
                    @Override
                    public Integer call(Integer integer) {
                        if (integer < 0) {
                            //抛异常注销,这种用法在我另外一个库RxProgressManager使用到
                            throw new RuntimeException("数据不能小于0");
                        }
                        return integer;
                    }
                })
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer integer) {
                        if (integer == 666) {
                            //当满足条件注销
                            unsubscribe();
                        }
                    }
                });
    }

    private <T> LifecycleTransformer<T> bindToLifecycle() {
        return RxLifecycle.with(this).bindToLifecycle();
    }

    private <T> LifecycleTransformer<T> bindOnDestroy() {
        return RxLifecycle.with(this).bindOnDestroy();
    }

    private <T> LifecycleTransformer<T> bindUntilEvent(ActivityEvent event) {
        return RxLifecycle.with(this).bindUntilEvent(event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Observable.just(1)
                .compose(bindToLifecycle())
                .subscribe();
        myTextView.RxLifeCycleSetText("dhhAndroid");

    }

    @Override
    protected void onResume() {
        super.onResume();
        Observable.just(1)
                .compose(RxLifecycle.with(this).<Integer>bindToLifecycle())
                .subscribe();
//        RxWebSocketUtil.getInstance().setShowLog(BuildConfig.DEBUG);
//        RxWebSocketUtil.getInstance().getWebSocketString("ws://127.0.0.1:8089")
//                .compose(RxLifecycle.with(this).<String>bindToLifecycle())
//                .subscribe(new Action1<String>() {
//                    @Override
//                    public void call(String s) {
//
//                    }
//                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Observable.just("dhhAndroid")
                .compose(RxLifecycle.with(this).<String>bindOnDestroy())
                .subscribe();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Observable.just("dhhAndroid")
                .compose(RxLifecycle.with(this).<String>bindOnDestroy())
                .subscribe();
        Observable.timer(10, TimeUnit.SECONDS)
                .compose(mLifecycleManager.<Long>bindToLifecycle())
                .subscribe();
        test();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSubscription != null && !mSubscription.isUnsubscribed()) {
            mSubscription.unsubscribe();
        }
    }

    private void test() {
        Observable.timer(10, TimeUnit.SECONDS)
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        Log.d("RxJava1Activity", "注册");
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Log.d("RxJava1Activity", "注销");
                    }
                })
                .compose(RxLifecycle.with(this).<Long>bindOnDestroy())
                .subscribe();
        Observable.timer(10, TimeUnit.SECONDS)
                .compose(RxLifecycle.with(this).<Long>bindToLifecycle())
                .subscribe();
        Observable.timer(10, TimeUnit.SECONDS)
                //当activity onstop 注销
                .compose(RxLifecycle.with(this).<Long>bindUntilEvent(ActivityEvent.onStop))
                .subscribe();
    }

    private void initView() {
        myTextView = (MyTextView) findViewById(R.id.myTextView);
    }
}
