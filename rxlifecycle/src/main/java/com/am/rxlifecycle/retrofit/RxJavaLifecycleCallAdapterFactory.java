package com.am.rxlifecycle.retrofit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.am.rxlifecycle.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Created by dhh on 2017/10/10.
 */

public class RxJavaLifecycleCallAdapterFactory extends CallAdapter.Factory {

    private static CallAdapter.Factory adapterFactory;
    private LifecycleManager lifecycleManager;

    private RxJavaLifecycleCallAdapterFactory(LifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
        try {
            Class.forName("retrofit2.Retrofit");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Must be dependency retrofit2 !");
        }
        if (adapterFactory == null) {
            try {
                Class.forName("retrofit2.adapter.rxjava.RxJavaCallAdapterFactory");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Must be dependency RxJavaCallAdapterFactory !");
            }
            adapterFactory = RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io());
        }
    }

    public static CallAdapter.Factory create() {
        try {
            Class.forName("retrofit2.Retrofit");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Must be dependency retrofit2 !");
        }
        if (adapterFactory == null) {
            try {
                Class.forName("retrofit2.adapter.rxjava.RxJavaCallAdapterFactory");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Must be dependency RxJavaCallAdapterFactory !");
            }
            adapterFactory = RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io());
        }
        return adapterFactory;
    }

    /**
     * 确保你注入的factory内部是RxJavaCallAdapterFactory
     *
     * @param factory
     */
    public static void injectCallAdapterFactory(CallAdapter.Factory factory) {
        RxJavaLifecycleCallAdapterFactory.adapterFactory = factory;
    }

    public static CallAdapter.Factory createWithLifecycleManager(LifecycleManager lifecycleManager) {
        if (lifecycleManager == null) throw new NullPointerException("lifecycleManager == null");
        return new RxJavaLifecycleCallAdapterFactory(lifecycleManager);
    }

    @Nullable
    @Override
    public CallAdapter<?, ?> get(@NonNull Type returnType, @NonNull Annotation[] annotations, @NonNull Retrofit retrofit) {
        return new RxJavaLifecycleCallAdapter<>(adapterFactory.get(returnType, annotations, retrofit), lifecycleManager);
    }

    private static final class RxJavaLifecycleCallAdapter<R> implements CallAdapter<R, Observable<?>> {
        private CallAdapter<R, ?> callAdapter;
        private LifecycleManager lifecycleManager;

        public RxJavaLifecycleCallAdapter(CallAdapter<R, ?> callAdapter, LifecycleManager lifecycleManager) {
            this.callAdapter = callAdapter;
            this.lifecycleManager = lifecycleManager;
        }

        @Override
        public Type responseType() {
            return callAdapter.responseType();
        }

        @Override
        public Observable<?> adapt(@NonNull Call<R> call) {
            Observable<?> observable = (Observable) callAdapter.adapt(call);
            return observable
                    .compose(lifecycleManager.bindOnDestroy())
                    .subscribeOn(Schedulers.io());
        }

    }
}
