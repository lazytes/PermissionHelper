package io.github.lazytes.permission.sample;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.jakewharton.rxbinding2.view.RxView;

import androidx.appcompat.app.AppCompatActivity;
import io.github.lazytes.permission.Permission;
import io.github.lazytes.permission.PermissionHelper;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate");

        final PermissionHelper helper = new PermissionHelper(this);

        Disposable disposable = RxView.clicks(findViewById(R.id.button1))
                .compose(helper.transformer(Manifest.permission.READ_EXTERNAL_STORAGE))
                .subscribe(new Consumer<Permission>() {
                    @Override
                    public void accept(Permission permission) {
                        Log.i(TAG, "onNext: " + permission.toString());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        Log.e(TAG, "onError", throwable);
                    }
                });
        disposables.add(disposable);

        Button button2 = findViewById(R.id.button2);

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disposable disposable1 = helper.requestEach(Manifest.permission.RECORD_AUDIO)
                        .subscribe(new Consumer<Permission>() {
                            @Override
                            public void accept(Permission permission) {
                                Log.i(TAG, "onNext: " + permission.toString());
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) {
                                Log.e(TAG, "onError", throwable);
                            }
                        }, new Action() {
                            @Override
                            public void run() {
                                Log.i(TAG, "onComplete ");
                            }
                        });
                disposables.add(disposable1);
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        if (!disposables.isDisposed()) {
            disposables.dispose();
        }
        super.onDestroy();
    }
}
