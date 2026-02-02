package com.example.screentimemanager;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


public class appLocker extends Worker {
    public appLocker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork(){
        try {
            for (int i = 0; i <= 100; i++) {
                if (isStopped()) {
                    return Result.failure();
                }
                Log.d("appLocker", "Processing " + i);

                Thread.sleep(1000);
            }
            return Result.success();
        } catch (InterruptedException e) {
            Log.e("appLocker", "Worker interrupted", e);
            return Result.retry();
        } catch (Exception e) {
            Log.e("appLocker", "Error in background loop", e);
            return Result.failure();
        }
    }
}
