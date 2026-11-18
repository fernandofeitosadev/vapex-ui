package com.limelight.home.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.limelight.R;
import com.limelight.home.ui.model.BannerModel;

import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.ViewHolder> {

    private final List<BannerModel> banners;

    public BannerAdapter(List<BannerModel> banners) {
        this.banners = banners;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_banner, parent, false);
        
        // Ajusta a largura para 95% da tela
        int screenWidth = parent.getContext().getResources().getDisplayMetrics().widthPixels;
        int itemWidth = (int) (screenWidth * 0.95);
        
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = itemWidth;
        view.setLayoutParams(params);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BannerModel banner = banners.get(position);
        holder.txtTitle.setText(banner.getTitle());
        holder.txtDescription.setText(banner.getDescription());
        holder.txtBadge.setText(banner.getBadge());

        if (banner.getImageRes() != 0) {
            holder.bannerImage.setImageResource(banner.getImageRes());
        } else if (banner.getImageUrl() != null) {
            Glide.with(holder.itemView.getContext())
                    .load(banner.getImageUrl())
                    .into(holder.bannerImage);
        }
    }

    @Override
    public int getItemCount() {
        return banners.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView bannerImage;
        TextView txtTitle, txtDescription, txtBadge;

        ViewHolder(View itemView) {
            super(itemView);
            bannerImage = itemView.findViewById(R.id.bannerImage);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            txtBadge = itemView.findViewById(R.id.txtBadge);
        }
    }
}