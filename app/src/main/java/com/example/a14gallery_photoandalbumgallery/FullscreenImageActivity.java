package com.example.a14gallery_photoandalbumgallery;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.a14gallery_photoandalbumgallery.databinding.ActivityFullscreenImageBinding;

import java.io.File;

public class FullscreenImageActivity extends AppCompatActivity implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {
    ActivityFullscreenImageBinding binding;
    String imagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFullscreenImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = getIntent();
        imagePath = intent.getExtras().getString("path");

        // Set on click for buttons
        binding.btnShare.setOnClickListener(this);
        binding.btnEdit.setOnClickListener(this);
        binding.btnDelete.setOnClickListener(this);
        binding.btnHide.setOnClickListener(this);
        binding.btnMore.setOnClickListener(this);

        Glide.with(this)
                .load(imagePath)
                .fitCenter()
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(binding.imageView);
    }

    @Override
    public void onClick(View view) {
        // Share button
        if (view.getId() == R.id.btnShare) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(imagePath));
            shareIntent.setType("image/jpeg");
            startActivity(Intent.createChooser(shareIntent, null));
        }

        //  Edit button
        if (view.getId() == R.id.btnEdit) {
            Toast.makeText(this, "Button clicked", Toast.LENGTH_SHORT).show();
        }

        //  Hide button
        if (view.getId() == R.id.btnHide) {
            Toast.makeText(this, "Button clicked", Toast.LENGTH_SHORT).show();
        }

        //  Delete button
        if (view.getId() == R.id.btnDelete) {
            Toast.makeText(this, "Button clicked", Toast.LENGTH_SHORT).show();
            File file = new File(imagePath);
            if (file.delete()) {
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                ImageGallery.getInstance().update(this);
            } else {
                Toast.makeText(this, "Delete unsuccessfully", Toast.LENGTH_SHORT).show();
            }
            finish();
        }

        //  More button
        if (view.getId() == R.id.btnMore) {
            Toast.makeText(this, "Button clicked", Toast.LENGTH_SHORT).show();
            // Initialize the popup menu
            PopupMenu popupMenu = new PopupMenu(FullscreenImageActivity.this, binding.btnMore);

            // Inflate the popup menu
            popupMenu.getMenuInflater().inflate(R.menu.menu_fullscreen, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(this);
            // Show the popup menu
            popupMenu.show();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        // Rename button
        if (menuItem.getItemId() == R.id.btnRename) {
            return true;
        }

        // Add to album button
        if (menuItem.getItemId() == R.id.btnAddToAlbum) {
            return true;
        }

        // Details button
        if (menuItem.getItemId() == R.id.btnDetails) {
            return true;
        }
        return false;
    }
}