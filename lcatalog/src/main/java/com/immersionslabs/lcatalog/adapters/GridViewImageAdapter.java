package com.immersionslabs.lcatalog.adapters;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.immersionslabs.lcatalog.FullScreenImageViewActivity;
import com.immersionslabs.lcatalog.GalleryActivity;
import com.immersionslabs.lcatalog.R;

import java.util.ArrayList;

public class GridViewImageAdapter extends RecyclerView.Adapter<GridViewImageAdapter.ViewHolder> {

    public static final String TAG = "GridViewImageAdapter";
    private Activity _activity;
    private ArrayList<String> _filePaths = new ArrayList<>();

    public GridViewImageAdapter(GalleryActivity activity,
                                ArrayList<String> imagePaths) {

        this._activity = activity;
        this._filePaths = imagePaths;
    }

    @Override
    public GridViewImageAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_gallery, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        Glide.with(_activity)
                .load(_filePaths.get(position))
                .placeholder(R.drawable.dummy_icon)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.imageView);
        Log.e(TAG, " images" + holder.imageView);

        holder.gallery_container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(_activity, FullScreenImageViewActivity.class);
                intent.putExtra("position", position);
                _activity.startActivity(intent);

            }
        });
    }

    @Override
    public int getItemCount() {
        return _filePaths.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        RelativeLayout gallery_container;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.gallery_item);
            gallery_container = itemView.findViewById(R.id.gallery_container);
        }
    }
}
