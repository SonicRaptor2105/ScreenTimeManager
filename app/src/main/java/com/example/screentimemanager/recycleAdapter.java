package com.example.screentimemanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class recycleAdapter extends RecyclerView.Adapter<recycleAdapter.MyViewHolder> {
    private ArrayList<com.example.screentimemanager.AppTime> apps;

    public recycleAdapter(ArrayList<com.example.screentimemanager.AppTime> apps){
        this.apps = apps;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView application;
        private TextView usageTime;

        public MyViewHolder(final View view){
            super(view);
            application = view.findViewById(R.id.Application);
            usageTime = view.findViewById(R.id.ScreenTime);
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
        String name = apps.get(position).getApp();
        long usage = apps.get(position).getUsage();

        holder.application.setText(name);
        holder.usageTime.setText(String.format("%d mins", usage));
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }
}
