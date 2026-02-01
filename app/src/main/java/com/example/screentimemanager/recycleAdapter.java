package com.example.screentimemanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

public class recycleAdapter extends RecyclerView.Adapter<recycleAdapter.MyViewHolder> {
    private ArrayList<AppTime> apps;

    private ArrayList<String> tracked;

    public recycleAdapter(ArrayList<AppTime> apps, Context context){
        this.apps = apps;
        this.tracked = loadArrayList("TrackedApps", context);
    }

    private void saveArrayList(ArrayList<String> list, String key, Context context) {
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(key, json);
        editor.apply();
    }

    public ArrayList<String> loadArrayList(String key, Context context) {
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(key, null);
        java.lang.reflect.Type type = new TypeToken<ArrayList<String>>() {}.getType();
        ArrayList<String> list = gson.fromJson(json, type);
        return (list != null) ? list : new ArrayList<>();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView application;
        private TextView usageTime;
        private Switch lockSwitch;

        public MyViewHolder(final View view){
            super(view);
            application = view.findViewById(R.id.Application);
            usageTime = view.findViewById(R.id.ScreenTime);
            lockSwitch = view.findViewById(R.id.switch1);
        }
    }

    @NonNull
    @Override
    public recycleAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_items, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull recycleAdapter.MyViewHolder holder, int position) {
        AppTime app = apps.get(position);
        holder.application.setText(app.getAppName());
        holder.usageTime.setText(String.format("%d mins", app.getUsage()));

        holder.lockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                tracked.add(app.getPackageName());
            } else {
                tracked.remove(app.getPackageName());
            }
            saveArrayList(tracked, "TrackedApps", buttonView.getContext());
            Log.d("SwitchToggled", String.valueOf(tracked));
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }
}
