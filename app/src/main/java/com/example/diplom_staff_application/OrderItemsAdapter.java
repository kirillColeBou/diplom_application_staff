package com.example.diplom_staff_application;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class OrderItemsAdapter extends RecyclerView.Adapter<OrderItemsAdapter.OrderItemViewHolder> {
    private final Context context;
    private List<CartItem> orderItems;

    public OrderItemsAdapter(List<CartItem> orderItems, Context context) {
        this.context = context;
        this.orderItems = orderItems != null ? new ArrayList<>(orderItems) : new ArrayList<>();
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.cart_item, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        CartItem item = orderItems.get(position);
        if (item.getProduct() == null) {
            Log.e("OrderItemsAdapter", "Product is null for item at position " + position);
            return;
        }
        holder.productName.setText(item.getProduct().getName());
        holder.productPrice.setText(String.format("%d ₽", (int)(item.getProduct().getPrice() * item.getCount())));
        holder.productSize.setText(String.format("Размер: %s", item.getSize()));
        loadProductImage(item.getProduct().getId(), holder.productImage);
    }

    private void loadProductImage(int productId, ImageView imageView) {
        String cacheKey = "product_" + productId + "_0";
        ImageCacheManager cacheManager = ImageCacheManager.getInstance(context);
        Bitmap cachedBitmap = cacheManager.getBitmapFromMemoryCache(cacheKey);
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap);
            return;
        }
        cachedBitmap = cacheManager.getBitmapFromDiskCache(cacheKey);
        if (cachedBitmap != null) {
            cacheManager.addBitmapToMemoryCache(cacheKey, cachedBitmap);
            imageView.setImageBitmap(cachedBitmap);
            return;
        }
        ImageContext.loadImagesForProduct(context, productId, new ImageContext.ImagesCallback() {
            @Override
            public void onSuccess(List<String> images) {
                if (images != null && !images.isEmpty() && !images.get(0).isEmpty()) {
                    try {
                        String base64Image = images.get(0).split(",")[1];
                        byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        imageView.setImageBitmap(decodedByte);
                        cacheManager.addBitmapToMemoryCache(cacheKey, decodedByte);
                        cacheManager.addBitmapToDiskCache(cacheKey, decodedByte);
                    } catch (Exception e) {
                        imageView.setImageResource(R.drawable.nike_air_force);
                    }
                } else {
                    imageView.setImageResource(R.drawable.nike_air_force);
                }
            }

            @Override
            public void onError(String error) {
                imageView.setImageResource(R.drawable.nike_air_force);
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }

    public void updateItems(List<CartItem> newItems) {
        this.orderItems.clear();
        if (newItems != null) {
            this.orderItems.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    static class OrderItemViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productSize, productPrice;

        OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            productSize = itemView.findViewById(R.id.product_size);
            productPrice = itemView.findViewById(R.id.product_price);
        }
    }
}