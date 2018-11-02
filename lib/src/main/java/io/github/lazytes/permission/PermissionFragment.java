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
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.Fragment;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.github.lazytes.permission.PermissionHelper.TRIGGER;

/**
 * Author: ly(emiya.angra@gmail.com)
 * <p>
 * Date:  2018/10/25
 * <p>
 * Time:  16:12
 */
public class PermissionFragment extends Fragment {

    private static final int REQUEST_PERMISSION = 0x01;

    private Map<String, PublishSubject<Permission>> mSubjects = new HashMap<>();

    private Subject<Object> mCreateSubject = PublishSubject.create();

    public PermissionFragment() {
    }

    static PermissionFragment newInstance() {
        return new PermissionFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mCreateSubject.onNext(TRIGGER);
        mCreateSubject.onComplete();
    }

    void requestPermissions(@NonNull String[] permissions) {
        Log.i(PermissionHelper.TAG, "requestPermissions: " + TextUtils.join(",", permissions));
        requestPermissions(permissions, REQUEST_PERMISSION);
    }

    Observable<Object> prepare() {
        if (isAdded()) {
            return Observable.just(TRIGGER);
        }
        return mCreateSubject;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_PERMISSION || permissions.length == 0 || grantResults.length == 0) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            Log.i(PermissionHelper.TAG, "onRequestPermissionsResult: requestCode = " + requestCode +
                    ", permissions.length = " + permissions.length +
                    ", grantResults.length = " + grantResults.length);
        } else {
            boolean[] showRationale = new boolean[permissions.length];

            for (int i = 0; i < permissions.length; i++) {
                showRationale[i] = shouldShowRequestPermissionRationale(permissions[i]);
            }

            onRequestPermissionsResult(permissions, grantResults, showRationale);
        }
    }

    private void onRequestPermissionsResult(@NonNull String[] permissions, int[] grantResults, boolean[] showRationale) {
        for (int i = 0; i < permissions.length; i++) {
            Log.i(PermissionHelper.TAG, "onRequestPermissionsResult : permission = " + permissions[i]);
            PublishSubject<Permission> subject = mSubjects.get(permissions[i]);
            if (subject == null) {
                Log.e(PermissionHelper.TAG, "Subject for permission[" + permissions[i] + "] is null");
                return;
            }
            mSubjects.remove(permissions[i]);
            boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            subject.onNext(new Permission(permissions[i], granted, showRationale[i]));
            subject.onComplete();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    boolean isGranted(@NonNull String permission) {
        final Context context = getActivity();
        checkNotNull(context, "cannot find activity for PermissionFragment");

        return PermissionChecker.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @TargetApi(Build.VERSION_CODES.M)
    boolean isRevoked(@NonNull String permission) {
        final Context context = getActivity();
        checkNotNull(context, "cannot find activity for PermissionFragment");

        return context.getPackageManager().isPermissionRevokedByPolicy(permission, context.getPackageName());
    }

    public boolean containsByPermission(@NonNull String permission) {
        return mSubjects.containsKey(permission);
    }

    public PublishSubject<Permission> getSubjectByPermission(@NonNull String permission) {
        return mSubjects.get(permission);
    }

    public void setSubjectForPermission(@NonNull PublishSubject<Permission> subject, @NonNull String permission) {
        mSubjects.put(permission, subject);
    }
}
