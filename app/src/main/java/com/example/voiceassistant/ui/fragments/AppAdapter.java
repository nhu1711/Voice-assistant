package com.example.voiceassistant.ui.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voiceassistant.R;

import java.util.List;
import java.util.Set;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

    private final List<AppInfo> appList;
    private final Set<String> selectedPackages;

    public AppAdapter(List<AppInfo> appList, Set<String> selectedPackages) {
        this.appList = appList;
        this.selectedPackages = selectedPackages;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_selection, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo app = appList.get(position);
        holder.ivIcon.setImageDrawable(app.icon);
        holder.tvName.setText(app.name);
        
        holder.cbSelected.setOnCheckedChangeListener(null);
        holder.cbSelected.setChecked(selectedPackages.contains(app.packageName));
        
        holder.cbSelected.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedPackages.add(app.packageName);
            } else {
                selectedPackages.remove(app.packageName);
            }
        });
        
        holder.itemView.setOnClickListener(v -> holder.cbSelected.setChecked(!holder.cbSelected.isChecked()));
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public static class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        CheckBox cbSelected;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_app_icon);
            tvName = itemView.findViewById(R.id.tv_app_name);
            cbSelected = itemView.findViewById(R.id.cb_app_selected);
        }
    }
}
