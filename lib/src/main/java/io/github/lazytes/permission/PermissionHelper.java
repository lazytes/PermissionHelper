/*
 * Copyright (C) 2018. ly(emiya.angra@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.lazytes.permission;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;

/**
 * Author: ly(emiya.angra@gmail.com)
 * <p>
 * Date:  2018/10/25
 * <p>
 * Time:  15:18
 */
public class PermissionHelper {
    static final String TAG = PermissionHelper.class.getSimpleName();
    static final Object TRIGGER = new Object();

    private Lazy<PermissionFragment> mFragment;

    public PermissionHelper(FragmentActivity activity) {
        mFragment = getLazyFragment(activity.getSupportFragmentManager());
    }

    private Lazy<PermissionFragment> getLazyFragment(final FragmentManager manager) {
        return new Lazy<PermissionFragment>() {
            private PermissionFragment fragment;

            @Override
            public PermissionFragment get() {
                if (fragment == null) {
                    fragment = createOrFindFragment(manager);
                }
                return fragment;
            }
        };
    }

    private PermissionFragment createOrFindFragment(FragmentManager manager) {
        PermissionFragment fragment = (PermissionFragment) manager.findFragmentByTag(TAG);
        if (fragment == null) {
            fragment = PermissionFragment.newInstance();
            manager.beginTransaction().add(fragment, TAG).commitAllowingStateLoss();
        }

        return fragment;
    }

    public Observable<Permission> requestEach(String... permissions) {
        return Observable.just(TRIGGER).compose(transformer(permissions));
    }

    public Single<Permission> request(String permission) {
        return requestEach(permission).singleOrError();
    }

    public <T> ObservableTransformer<T, Permission> transformer(final String... permissions) {
        return new ObservableTransformer<T, Permission>() {
            @Override
            public ObservableSource<Permission> apply(Observable<T> upstream) {
                Log.i(TAG, "transformer in PermissionHelper");
                return requestEach(upstream, permissions);
            }
        };
    }

    private Observable<Permission> requestEach(Observable<?> source, final String... permissions) {
        if (permissions == null || permissions.length == 0) {
            throw new IllegalArgumentException("require at least one permission");
        }
        return source
                .flatMap(new Function<Object, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(Object o) {
                        return mFragment.get().prepare();
                    }
                })
                .flatMap(new Function<Object, ObservableSource<Permission>>() {
                    @Override
                    public ObservableSource<Permission> apply(Object o) {
                        return requestActual(permissions);
                    }
                });
    }

    @TargetApi(Build.VERSION_CODES.M)
    private Observable<Permission> requestActual(final String... permissions) {
        List<Observable<Permission>> list = new ArrayList<>();
        List<String> permissionsForRequest = new ArrayList<>();

        for (String permission : permissions) {

            if (isGranted(permission)) {
                list.add(Observable.just(new Permission(permission, true, false)));
                continue;
            }

            if (isRevoked(permission)) {
                list.add(Observable.just(new Permission(permission, false, false)));
                continue;
            }

            PublishSubject<Permission> subject = mFragment.get().getSubjectByPermission(permission);
            if (subject == null) {
                permissionsForRequest.add(permission);
                subject = PublishSubject.create();
                mFragment.get().setSubjectForPermission(subject, permission);
            }

            list.add(subject);
        }

        if (!permissionsForRequest.isEmpty()) {
            String[] array = permissionsForRequest.toArray(new String[0]);
            requestPermissionsFromFragment(array);
        }

        return Observable.concat(Observable.fromIterable(list));
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermissionsFromFragment(String[] permissions) {
        mFragment.get().requestPermissions(permissions);
    }

    public boolean isMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public boolean isGranted(String permission) {
        return !isMarshmallow() || mFragment.get().isGranted(permission);
    }

    public boolean isRevoked(String permission) {
        return isMarshmallow() && mFragment.get().isRevoked(permission);
    }

    @FunctionalInterface
    public interface Lazy<V> {
        V get();
    }
}
