package com.kieshalab.keishamoji.stickers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kieshalab.keishamoji.KeyboardService;
import com.kieshalab.keishamoji.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mist on 12.12.16.
 */

public class StickerAdapter extends RecyclerView.Adapter<StickerHolder> {
    private final String TAG = "StickerAdapter";
    private List<StickerData> stickerDataList = new ArrayList<StickerData>();
    private KeyboardService keyboardService;

    public StickerAdapter(KeyboardService kis, List<StickerData> sdl) {
        this.keyboardService = kis;
        this.stickerDataList = sdl;
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
