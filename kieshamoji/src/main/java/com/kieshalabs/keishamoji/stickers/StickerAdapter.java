package com.kieshalabs.keishamoji.stickers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.kieshalabs.keishamoji.KeyboardService;
import com.kieshalabs.keishamoji.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mist on 12.12.16.
 */

public class StickerAdapter extends RecyclerView.Adapter<StickerAdapter.StickerHolder> {
    private final String TAG = "StickerAdapter";
    private List<StickerData> stickerDataList;
    private KeyboardService keyboardService;

    public StickerAdapter(KeyboardService kis, List<StickerData> sdl) {
        this.keyboardService = kis;
        this.stickerDataList = sdl;
    }

    public static class StickerHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;

        public StickerHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.imageItem);
        }
    }

    @Override
    public StickerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item, parent, false);
        return new StickerHolder(view);
    }

    @Override
    public void onBindViewHolder(final StickerHolder holder, final int position) {
        final StickerData sticker = stickerDataList.get(position);
        System.out.println("ID:: "+sticker.imageId);
        System.out.println("name:: "+sticker.packName);

        System.out.println();


        try {
            Bitmap ico = BitmapFactory.decodeFile(sticker.iconKey.getPath());
            holder.imageView.setImageBitmap(ico);
        } catch (Exception e) {
            e.printStackTrace();
        }

        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyboardService.inputContent(sticker, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stickerDataList.size();
    }
}
