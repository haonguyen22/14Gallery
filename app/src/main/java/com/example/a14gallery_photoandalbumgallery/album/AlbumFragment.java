package com.example.a14gallery_photoandalbumgallery.album;

import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a14gallery_photoandalbumgallery.BuildConfig;
import com.example.a14gallery_photoandalbumgallery.Image;
import com.example.a14gallery_photoandalbumgallery.R;
import com.example.a14gallery_photoandalbumgallery.databinding.FragmentAlbumBinding;
import com.example.a14gallery_photoandalbumgallery.detailAlbum.DetailAlbumActivity;
import com.example.a14gallery_photoandalbumgallery.password.CreatePasswordActivity;
import com.example.a14gallery_photoandalbumgallery.password.InputPasswordActivity;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AlbumFragment extends Fragment implements MenuProvider {
    private static final int APP_STORAGE_ACCESS_REQUEST_CODE = 501;
    FragmentAlbumBinding binding;
    List<Album> albums;
    String rootFolder = "/14Gallery/";
    String favoriteAlbumFolderName = "FavoriteAlbum";
    String privateAlbumFolderName = "PrivateAlbum";
    String recycleBinFolderName = "RecycleBin";
    AlbumFragmentAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAlbumBinding.inflate(inflater, container, false);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        binding.albumFragmentRecycleView.setHasFixedSize(true);
        binding.albumFragmentRecycleView.setLayoutManager(layoutManager);
        binding.albumFragmentRecycleView.setNestedScrollingEnabled(false);

        AlbumGallery.getInstance().load(getContext());
        albums = AlbumGallery.getInstance().albums;
        adapter = new AlbumFragmentAdapter(getContext(), albums);
        binding.albumFragmentRecycleView.setAdapter(adapter);


        //Album Favorite, RecycleBin
        Album Favorite = new Album();
        Album RecycleBin = new Album();

        // Menu
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        ImageView favoriteAlbum = (ImageView) binding.getRoot().findViewById(R.id.favoriteAlbum);
        ImageView privateAlbum = (ImageView) binding.getRoot().findViewById(R.id.privateAlbum);
        ImageView recycleBin = (ImageView) binding.getRoot().findViewById(R.id.recycleBin);

        //Tạo album Ưa thích nếu chưa tạo
        File favoriteAlbumFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + rootFolder + favoriteAlbumFolderName);
        if (!favoriteAlbumFolder.exists()) {
            favoriteAlbumFolder.mkdirs();
        }

        //Tạo album Riêng tư nếu chưa tạo
        File privateAlbumFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + rootFolder + privateAlbumFolderName);
        if (!privateAlbumFolder.exists()) {
            privateAlbumFolder.mkdirs();
        }

        //Tạo Thùng rác nếu chưa tạo
        File recycleBinFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + rootFolder + recycleBinFolderName);
        if (!recycleBinFolder.exists()) {
            recycleBinFolder.mkdirs();
        }

        favoriteAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "FavoriteAlbum clicked!", Toast.LENGTH_SHORT).show();
                File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + rootFolder + favoriteAlbumFolderName);
                File[] content = folder.listFiles();
                String folderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + rootFolder + favoriteAlbumFolderName + '/';
                folderPath = folderPath + "%";
                Favorite.setName("Ưa thích");
                String[] projection = new String[]{
                        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                        MediaStore.Images.Media.DATA,
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DATE_TAKEN
                };
                String[] selectionArgs = new String[]{folderPath};
                String selection = MediaStore.Images.Media.DATA + " like ? ";
                Uri images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                Cursor cursor = getActivity().getContentResolver().query(images, projection, selection, selectionArgs, null);
                if (cursor != null && cursor.getCount() > 0) {
                    if (cursor.moveToFirst()) {
                        String bucketName;
                        String data;
                        String imageId;
                        long dateTaken;
                        int bucketNameColumn = cursor.getColumnIndex(
                                MediaStore.Images.Media.BUCKET_DISPLAY_NAME);

                        int imageUriColumn = cursor.getColumnIndex(
                                MediaStore.Images.Media.DATA);

                        int imageIdColumn = cursor.getColumnIndex(
                                MediaStore.Images.Media._ID);

                        int dateTakenColumn = cursor.getColumnIndex(
                                MediaStore.Images.Media.DATE_TAKEN);
                        do {
                            Calendar myCal = Calendar.getInstance();
                            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.UK);
                            // Get the field values
                            bucketName = cursor.getString(bucketNameColumn);
                            data = cursor.getString(imageUriColumn);
                            imageId = cursor.getString(imageIdColumn);
                            dateTaken = cursor.getLong(dateTakenColumn);
                            myCal.setTimeInMillis(dateTaken);
                            String dateText = formatter.format(myCal.getTime());

                            Image image = new Image();
                            image.setAlbumName(bucketName);
                            image.setPath(data);
                            image.setId(Integer.parseInt(imageId));
                            image.setDateTaken(dateText);

                            Favorite.getAlbumImages().add(image);

                        } while (cursor.moveToNext());
                    }

                    cursor.close();
                }
                Intent intent = new Intent(getActivity(), DetailAlbumActivity.class);
                Gson gson = new Gson();
                String imagesObj = gson.toJson(Favorite);
                intent.putExtra("ALBUM", imagesObj);
                getActivity().startActivity(intent);
            }

        });
        privateAlbum.setOnClickListener(new View.OnClickListener()

            {
                @Override
                public void onClick (View view){
                Toast.makeText(getActivity(), "PrivateAlbum clicked!", Toast.LENGTH_SHORT).show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // loading is given
                        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("PREFS", 0);
                        if (sharedPreferences.getString("password", "0").equals("0")) {
                            // Intent to navigate to Create Password Screen
                            Intent intent = new Intent(getActivity().getApplicationContext(), CreatePasswordActivity.class);
                            startActivity(intent);
                            //getActivity().finish();
                        } else {
                            //Intent to navigate to Input Password Screen
                            Intent intent = new Intent(getActivity().getApplicationContext(), InputPasswordActivity.class);
                            getActivity().startActivity(intent);
                            //getActivity().finish();
                        }
                    }
                }, 2000);
            }
            });
        recycleBin.setOnClickListener(new View.OnClickListener()

            {
                @Override
                public void onClick (View view){
                Toast.makeText(getActivity(), "RecycleBin clicked!", Toast.LENGTH_SHORT).show();
                    File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + rootFolder + recycleBinFolderName);
                    File[] content = folder.listFiles();
                    String folderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + rootFolder + recycleBinFolderName + '/';
                    folderPath = folderPath + "%";
                    RecycleBin.setName("Thùng rác");
                    String[] projection = new String[]{
                            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                            MediaStore.Images.Media.DATA,
                            MediaStore.Images.Media._ID,
                            MediaStore.Images.Media.DATE_TAKEN
                    };
                    String[] selectionArgs = new String[]{folderPath};
                    String selection = MediaStore.Images.Media.DATA + " like ? ";
                    Uri images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    Cursor cursor = getActivity().getContentResolver().query(images, projection, selection, selectionArgs, null);
                    if (cursor != null && cursor.getCount() > 0) {
                        if (cursor.moveToFirst()) {
                            String bucketName;
                            String data;
                            String imageId;
                            long dateTaken;
                            int bucketNameColumn = cursor.getColumnIndex(
                                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME);

                            int imageUriColumn = cursor.getColumnIndex(
                                    MediaStore.Images.Media.DATA);

                            int imageIdColumn = cursor.getColumnIndex(
                                    MediaStore.Images.Media._ID);

                            int dateTakenColumn = cursor.getColumnIndex(
                                    MediaStore.Images.Media.DATE_TAKEN);
                            do {
                                Calendar myCal = Calendar.getInstance();
                                SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.UK);
                                // Get the field values
                                bucketName = cursor.getString(bucketNameColumn);
                                data = cursor.getString(imageUriColumn);
                                imageId = cursor.getString(imageIdColumn);
                                dateTaken = cursor.getLong(dateTakenColumn);
                                myCal.setTimeInMillis(dateTaken);
                                String dateText = formatter.format(myCal.getTime());

                                Image image = new Image();
                                image.setAlbumName(bucketName);
                                image.setPath(data);
                                image.setId(Integer.parseInt(imageId));
                                image.setDateTaken(dateText);

                                RecycleBin.getAlbumImages().add(image);

                            } while (cursor.moveToNext());
                        }

                        cursor.close();
                    }
                    Intent intent = new Intent(getActivity(), DetailAlbumActivity.class);
                    Gson gson = new Gson();
                    String imagesObj = gson.toJson(RecycleBin);
                    intent.putExtra("ALBUM", imagesObj);
                    getActivity().startActivity(intent);
            }
            });
        return binding.getRoot();
        }

        @Override
        public void onCreateMenu (@NonNull Menu menu, @NonNull MenuInflater menuInflater){
            menu.clear();
            if (!menu.hasVisibleItems()) {
                menuInflater.inflate(R.menu.top_bar_menu_album, menu);
            }
        }

        @Override
        public boolean onMenuItemSelected (@NonNull MenuItem menuItem){
            if (menuItem.getItemId() == R.id.alb_add) {
                //Kiểm tra và thiết lập quyền quản lý bộ nhớ
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (!Environment.isExternalStorageManager()) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                        alert.setTitle("PERMISSION NEEDED");
                        alert.setMessage("This app need mange your storage to be able to create album folder");
                        alert.setPositiveButton("ALLOW", (dialog, whichButton) -> { // Set an EditText view to get user input
                            Intent intent = new Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                            startActivityForResult(intent, APP_STORAGE_ACCESS_REQUEST_CODE);
                        });
                        alert.setNegativeButton("DENY", (dialog, whichButton) -> {/* Canceled.*/});
                        alert.show();
                    } else {
                        //Tạo Dialog tạo Album mới
                        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                        alert.setTitle("Tạo album mới");
                        alert.setMessage("Tên album");
                        final EditText input = new EditText(getContext()); // Set an EditText view to get user input
                        alert.setView(input);
                        alert.setPositiveButton("Ok", (dialog, whichButton) -> {
                            String value = input.getText().toString();
                            // Do something with value!
                            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + rootFolder + value);

                            Log.e("DIR", Environment.getExternalStorageDirectory().toString());
                            if (!file.exists()) {
                                boolean success = file.mkdirs();
                                if (success) {
                                    Log.e("RES", "Success");
                                } else {
                                    Log.e("RES", "Failed");
                                }
                                Toast.makeText(getActivity(), "Successful", Toast.LENGTH_SHORT).show();
                                AlbumGallery.getInstance().update(getContext());
                                albums = AlbumGallery.getInstance().albums;
                                adapter = new AlbumFragmentAdapter(getContext(), albums);
                                binding.albumFragmentRecycleView.setAdapter(adapter);
                            } else {
                                Toast.makeText(getActivity(), "Folder Already Exists", Toast.LENGTH_SHORT).show();
                            }
                        });
                        alert.setNegativeButton("Cancel", (dialog, whichButton) -> {/* Canceled.*/});
                        alert.show();
                    }
                }
                return true;
            }
            if (menuItem.getItemId() == R.id.alb_camera) {  // Click camera
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                startActivity(intent);
                return true;
            }
            if (menuItem.getItemId() == R.id.alb_choose) {
                // Click choose(Lựa chọn)
                return true;
            }
            if (menuItem.getItemId() == R.id.alb_grid_col_2) {
                // Click grid_col_2
                return true;
            }
            if (menuItem.getItemId() == R.id.alb_grid_col_3) {
                // Click grid_col_3
                return true;
            }
            if (menuItem.getItemId() == R.id.alb_grid_col_4) {
                // Click grid_col_4
                return true;
            }
            if (menuItem.getItemId() == R.id.alb_grid_col_5) {
                // Click grid_col_5
                return true;
            }
            if (menuItem.getItemId() == R.id.alb_view_mode_normal) {
                // Click Lên rồi xuống
                return true;
            }
            if (menuItem.getItemId() == R.id.alb_view_mode_convert) {
                // Click Đảo ngược
                return true;
            }
            if (menuItem.getItemId() == R.id.alb_view_mode_day) {
                // Click Xếp theo ngày
                return true;
            }
            if (menuItem.getItemId() == R.id.alb_view_mode_month) {
                // Click Xếp theo tháng
                return true;
            }
            if (menuItem.getItemId() == R.id.alb_setting) {
                // Click Setting
                return true;
            }
            return false;
        }

    }