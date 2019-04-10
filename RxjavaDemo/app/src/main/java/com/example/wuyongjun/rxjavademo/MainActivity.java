package com.example.wuyongjun.rxjavademo;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import org.reactivestreams.Subscription;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.FlowableSubscriber;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    int i = 1;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        final Observable<Object> trueObserverable = Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> observableEmitter) throws Exception {
                Log.d(TAG, "emitter: trueObserverable");
                observableEmitter.onNext(new Object());
                observableEmitter.onComplete();
            }
        });
        final Observable<Object> falseObserverable = Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> observableEmitter) throws Exception {
                Log.d(TAG, "emitter: falseObserverable");
                observableEmitter.onNext(new Object());
                observableEmitter.onComplete();
            }
        });
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> observableEmitter) throws Exception {
                //执行耗时操作
                Log.d(TAG, "emmitter: 开始执行耗时操作");
                observableEmitter.onNext("开始执行耗时操作");
                Thread.sleep(2000);
                Log.d(TAG, "emmitter: 执行完了耗时操作");
                observableEmitter.onNext("执行完了耗时操作");
                //当调用onComplete方法时，onNext方法就不能在执行，用于事件结束时执行
                observableEmitter.onComplete();

            }
        })
                .subscribeOn(Schedulers.io())//这里指定了上面subscribe方法执行的线程
                .map(new Function<String, Boolean>() {//map常用于数据转化，可以将A对象转化为B对象
                    @Override
                    public Boolean apply(String s) throws Exception {
                        Log.d(TAG, "数据类型转化");
                        return TextUtils.equals(s, "开始执行耗时操作");
                    }
                })//将String转化成boolean
                .flatMap(new Function<Boolean, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(Boolean aBoolean) throws Exception {
                        Log.d(TAG, "将数据转化成Observable");
                        return aBoolean ? trueObserverable : falseObserverable;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())//下面的方法切换到android的ui线程执行
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onSubscribe(Disposable disposable) {
                        //观察者把被观察者订阅的时候执行,获取disposabale,当需要中断被观察者时执行
                        // 一般用作activity销毁的时候执行取消订阅
//                if (!disposable.isDisposed()) {
//                    disposable.dispose();
//                }
                        mCompositeDisposable.add(disposable);
                        Log.d(TAG, "onSubscribe: 事件被订阅了");

                    }

                    @Override
                    public void onNext(Object o) {
                        //observableEmitter.onNext()时执行
                        //一般用作处理ui显示等操作，上面的observeOn方法指定了onNext执行的线程
                        Log.d(TAG, "onNext: " + String.format("调用第%d次onNext", i));
                        i++;
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        //之被观察者执行中抛出异常时会执行该方法
                        Log.d(TAG, "onError: " + throwable.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        //多次调用只会执行一次
                        i = 1;
                        Log.d(TAG, "onComplete: ");
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!mCompositeDisposable.isDisposed()) {
            mCompositeDisposable.dispose();
        }
    }

    @SuppressLint("CheckResult")
    public void commonClick(View v) {
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                //这里一般处理一些耗时操作
                //处理完后调用
                emitter.onNext("处理完毕");
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable disposable) {
                        //被订阅时执行，可以做一些progressbar显示
                    }

                    @Override
                    public void onNext(String s) {
                        //emitter.onNext()时执行
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        //发生异常时回调
                    }

                    @Override
                    public void onComplete() {
                        //emitter.onComplete()时执行,多次调用onComplete只会执行一次
                    }
                });

    }

    @SuppressLint("CheckResult")
    public void backpressClick(View v) {
        /*Observable.interval(1,TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        try {
                            Thread.sleep(800);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "accept: "+aLong);
                    }
                });*/

        Flowable.interval(1, TimeUnit.MILLISECONDS)
                .onBackpressureLatest()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        try {
                            Thread.sleep(800);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "accept: " + aLong);
                    }
                });

    }

    @SuppressLint("CheckResult")
    public void flowableClick(View v) {
        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> emitter) throws Exception {
                Log.d(TAG, "first requestCount: " + emitter.requested());
                for (int j = 0; j < 100; j++) {
                    Log.d(TAG, "emitter: " + j + " ,requestCount: " + emitter.requested());
                    emitter.onNext(j);
                    Thread.sleep(1000);
                }
            }
        }, BackpressureStrategy.BUFFER)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new FlowableSubscriber<Integer>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(10);
//                        s.cancel();
                    }

                    @Override
                    public void onNext(Integer integer) {
                        Log.d(TAG, "onNext: " + integer);
                        /*try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }*/
                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    @SuppressLint("CheckResult")
    public void zipClick(View v) {
        Observable observable1 = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                Log.d(TAG, "emitter: " + 1);
                emitter.onNext(1);
                Thread.sleep(1000);

                Log.d(TAG, "emitter: " + 2);
                emitter.onNext(2);
                Thread.sleep(1000);

                Log.d(TAG, "emitter: " + 3);
                emitter.onNext(3);
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io());
        Observable observable2 = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                Log.d(TAG, "emitter: A");
                emitter.onNext("A");
                Thread.sleep(1000);

                Log.d(TAG, "emitter: B");
                emitter.onNext("B");
                Thread.sleep(1000);

                Log.d(TAG, "emitter: C");
                emitter.onNext("C");
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io());
        Observable.zip(observable1, observable2, new BiFunction<Integer, String, String>() {
            @Override
            public String apply(Integer o, String o2) throws Exception {
                Log.d(TAG, "apply: " + o + o2);
                return o + o2;
            }
        })
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String o) throws Exception {
                        Log.d(TAG, "accept: " + o);
                    }
                });

    }

    /**
     * 轮询
     *
     * @param v
     */
    @SuppressLint("CheckResult")
    public void intervalClick(View v) {
        final Observable<Integer> observable = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                Log.d(TAG, "emitter: " + i);
                emitter.onNext(i);
                i++;
                Thread.sleep(1000);
            }
        });
        Observable.interval(1, TimeUnit.SECONDS)//轮训周期
                .take(5)//指定被观察者发射的次数
                .flatMap(new Function<Long, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Long aLong) throws Exception {
                        Log.d(TAG, "flatMap apply: " + aLong);
                        return observable;
                    }
                })//指定轮序处理事件
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Log.d(TAG, "subscribe accept: " + integer);
                    }
                });
    }

    /**
     * 搜索场景使用,点击防抖也可以
     *
     * @param v
     */
    @SuppressLint("CheckResult")
    public void debounceClick(View v) {
        PublishSubject<String> publishSubject = PublishSubject.create();
        Disposable disposable = publishSubject.debounce(200, TimeUnit.MILLISECONDS)//过滤实践
                .filter(new Predicate<String>() {
                    @Override
                    public boolean test(String s) throws Exception {
                        return !TextUtils.isEmpty(s);
                    }
                })
                .switchMap(new Function<String, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(String s) throws Exception {
                        return go2search(s);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String o) throws Exception {
                        //用于对象显示
                        Log.d(TAG, "accept: " + o);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.d(TAG, "error: " + throwable.getMessage());
                    }
                });
        for (int j = 0; j < 30; j++) {
            Log.d(TAG, "onNext: " + j);
            publishSubject.onNext(j + "");
            try {
                Thread.sleep(190);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 搜索获取对象
     *
     * @param s
     * @return
     */
    private ObservableSource<String> go2search(final String s) {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                Log.d(TAG, "go2search: " + s);
                emitter.onNext(s);
            }
        }).subscribeOn(Schedulers.io());
//    }

//        public void onClick() {
//
//            List<File> folders = new ArrayList<>();
//            new Thread() {
//                @Override
//                public void run() {
//                    super.run();
//                    for (File folder : folders) {
//                        File[] files = folder.listFiles();
//                        for (File file : files) {
//                            if (file.getName().endsWith(".png")) {
//                                final Bitmap bitmap = getBitmapFromFile(file);
//                                MainActivity.this.runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        imageCollectorView.addImage(bitmap);
//                                    }
//                                });
//                            }
//                        }
//                    }
//                }
//            }.start();
//
//            Observable.fromIterable(folders)
//                    .flatMap(new Function<File, ObservableSource<File>>() {
//                        @Override
//                        public ObservableSource<File> apply(File folder) throws Exception {
//                            return Observable.fromArray(folder.listFiles());
//                        }
//                    })
//                    .filter(new Predicate<File>() {
//                        @Override
//                        public boolean test(File file) throws Exception {
//                            return file.getName().endsWith(".png");
//                        }
//                    })
//                    .map(new Function<File, Bitmap>() {
//                        @Override
//                        public Bitmap apply(File file) throws Exception {
//                            return getBitmapFromFile(file);
//                        }
//                    })
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(new Observer<Bitmap>() {
//                        @Override
//                        public void onSubscribe(Disposable d) {
//
//                        }
//
//                        @Override
//                        public void onNext(Bitmap bitmap) {
//                            imageCollectorView.addImage(bitmap);
//                        }
//
//                        @Override
//                        public void onError(Throwable e) {
//
//                        }
//
//                        @Override
//                        public void onComplete() {
//
//                        }
//                    });
//        }
    }
}
