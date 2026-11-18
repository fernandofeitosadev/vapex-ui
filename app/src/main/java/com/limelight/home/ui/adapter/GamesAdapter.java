package com.limelight.home.ui.adapter;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.limelight.LaunchGameActivity;
import com.limelight.PcView;
import com.limelight.R;
import com.limelight.home.ui.HomeViewModel;
import com.limelight.home.ui.model.GameModel;
import com.limelight.preferences.AddComputerManually;

import java.util.List;


public class GamesAdapter extends RecyclerView.Adapter<GamesAdapter.ViewHolder> {
    public interface OnGameClickListener {
        void onGameClick(GameModel game);
    }

    private final List<GameModel> games;
    private final OnGameClickListener listener;


    public GamesAdapter(List<GameModel> games, OnGameClickListener listener) {
        this.games = games;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_game, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GameModel game = games.get(position);

        holder.txtTitle.setText(game.getTitle());
        holder.txtSubtitle.setText(game.getSubtitle());

        if (game.getBadge() != null && !game.getBadge().isEmpty()) {
            holder.txtBadge.setText(game.getBadge());
            holder.txtBadge.setVisibility(View.VISIBLE);
        } else {
            holder.txtBadge.setVisibility(View.GONE);
        }

        Glide.with(holder.itemView.getContext())
                .load(game.getImage())
                .placeholder(android.R.drawable.progress_horizontal)
                .error(android.R.drawable.stat_notify_error)
                .into(holder.imgCover);

        holder.itemView.setOnClickListener(v -> {
            listener.onGameClick(game);
        });
    }

    @Override
    public int getItemCount() {
        return games.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ShapeableImageView imgCover;
        TextView txtTitle;
        TextView txtSubtitle;
        TextView txtBadge;
        CircularProgressIndicator progressCard;

        ViewHolder(View itemView) {
            super(itemView);

            imgCover = itemView.findViewById(R.id.imgCover);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtSubtitle = itemView.findViewById(R.id.txtSubtitle);
            txtBadge = itemView.findViewById(R.id.txtBadge);
            progressCard = itemView.findViewById(R.id.progressCard);
        }
    }
}