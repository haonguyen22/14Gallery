package com.example.a14gallery_photoandalbumgallery.album;

import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;

import android.app.AlertDialog;
import android.content.ContentResolver;
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
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.example.a14gallery_photoandalbumgallery.BuildConfig;
import com.example.a14gallery_photoandalbumgallery.CombineAlbum.CombineAlbumActivity;
import com.example.a14gallery_photoandalbumgallery.database.AppDatabase;
import com.example.a14gallery_photoandalbumgallery.database.albumFavorite.AlbumFavoriteData;
import com.example.a14gallery_photoandalbumgallery.image.Image;
import com.example.a14gallery_photoandalbumgallery.R;
import com.example.a14gallery_photoandalbumgallery.databinding.FragmentAlbumBinding;
import com.example.a14gallery_photoandalbumgallery.detailAlbum.DetailAlbumActivity;
import com.example.a14gallery_photoandalbumgallery.image.ImageGallery;
import com.example.a14gallery_photoandalbumgallery.password.CreatePasswordActivity;
import com.example.a14gallery_photoandalbumgallery.password.InputPasswordActivity;
import com.example.a14gallery_photoandalbumgallery.setting.SettingActivity;
import com.google.gson.Gson;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlbumFragment extends Fragment implements MenuProvider {
    private static final int APP_STORAGE_ACCESS_REQUEST_CODE = 501;
    FragmentAlbumBinding binding;
    List<Album> albums;
    AlbumFragmentAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAlbumBinding.inflate(inflater, container, false);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        binding.albumFragmentRecycleView.setHasFixedSize(true);
        binding.albumFragmentRecycleView.setLayoutManager(layoutManager);
        binding.albumFragmentRecycleView.setNestedScrollingEnabled(false);

        AlbumGallery.getInstance().update(getContext());
        albums = AlbumGallery.getInstance().albums;
        adapter = new AlbumFragmentAdapter(getContext(), albums);
        binding.albumFragmentRecycleView.setAdapter(adapter);


        // Menu
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        ImageView favoriteAlbum = binding.getRoot().findViewById(R.id.favoriteAlbum);
        ImageView privateAlbum = binding.getRoot().findViewById(R.id.privateAlbum);
        ImageView recycleBin = binding.getRoot().findViewById(R.id.recycleBin);

        //Tạo album Ưa thích nếu chưa tạo
        File favoriteAlbumFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + AlbumGallery.rootFolder + AlbumGallery.favoriteAlbumFolderName);
        if (!favoriteAlbumFolder.exists()) {
            favoriteAlbumFolder.mkdirs();
        }

        //Tạo album Riêng tư nếu chưa tạo
        File privateAlbumFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + AlbumGallery.rootFolder + AlbumGallery.privateAlbumFolderName);
        if (!privateAlbumFolder.exists()) {
            privateAlbumFolder.mkdirs();
        }

        //Tạo Thùng rác nếu chưa tạo
        File recycleBinFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + AlbumGallery.rootFolder + AlbumGallery.recycleBinFolderName);
        if (!recycleBinFolder.exists()) {
            recycleBinFolder.mkdirs();
        }


        favoriteAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Album Favorite = null;
                try {
                    Favorite = getAlbumFavorite();
                    Intent intent = new Intent(getActivity().getApplicationContext(), DetailAlbumActivity.class);
                    Gson gson = new Gson();
                    String imagesObj = gson.toJson(Favorite);
                    intent.putExtra("ALBUM", imagesObj);
                    intent.putExtra("NAME", AlbumGallery.favoriteAlbumFolderName);
                    startActivity(intent);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

        });

        privateAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                            intent.putExtra("message", "OpenPrivate");
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            getActivity().startActivity(intent);
                            //getActivity().finish();
                        }
                    }
                }, 2000);
            }

        });
        recycleBin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Album RecycleBin = getRecycleBin();
                Intent intent = new Intent(getActivity().getApplicationContext(), DetailAlbumActivity.class);
                Gson gson = new Gson();
                String imagesObj = gson.toJson(RecycleBin);
                intent.putExtra("ALBUM", imagesObj);
                intent.putExtra("NAME", AlbumGallery.recycleBinFolderName);
                getActivity().startActivity(intent);
            }
        });
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        AlbumGallery.getInstance().update(getContext());
        albums = AlbumGallery.getInstance().albums;
        adapter = new AlbumFragmentAdapter(getContext(), albums);
        binding.albumFragmentRecycleView.setAdapter(adapter);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menu.clear();
        if (!menu.hasVisibleItems()) {
            menuInflater.inflate(R.menu.top_bar_menu_album, menu);
        }
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.alb_add) {
            //Kiểm tra và thiết lập quyền quản lý bộ nhớ
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                    alert.setTitle("PERMISSION NEEDED");
                    alert.setMessage("This app need you allow to mange your storage to be able to create, or modify images and albums");
                    alert.setPositiveButton("ALLOW", (dialog, whichButton) -> { // Set an EditText view to get user input
                        Intent intent = new Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                        startActivityForResult(intent, APP_STORAGE_ACCESS_REQUEST_CODE);
                    });
                    alert.setNegativeButton("DENY", (dialog, whichButton) -> {/* Canceled.*/});
                    ImageView img=new ImageView(getContext());
                    Glide.with(getContext()).asGif().load(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                            + "://" + getContext().getResources().getResourcePackageName(R.drawable.instruction_manage)
                            + '/' + getContext().getResources().getResourceTypeName(R.drawable.instruction_manage)
                            + '/' + getContext().getResources().getResourceEntryName(R.drawable.instruction_manage))).into(img);
                    img.setImageResource(R.drawable.instruction_manage);
                    alert.setView(img);
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
                        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + AlbumGallery.rootFolder + value);

                        Log.e("DIR", Environment.getExternalStorageDirectory().toString());
                        if (!file.exists()) {
                            boolean success = file.mkdirs();
                            if (success) {
                                Log.e("RES", "Success");
                            } else {
                                Log.e("RES", "Failed");
                            }
                            Toast.makeText(getActivity(), "Tạo thành công", Toast.LENGTH_SHORT).show();
                            AlbumGallery.getInstance().update(getContext());
                            albums = AlbumGallery.getInstance().albums;
                            adapter = new AlbumFragmentAdapter(getContext(), albums);
                            binding.albumFragmentRecycleView.setAdapter(adapter);
                        } else {
                            Toast.makeText(getActivity(), "Tên album đã tồn tại", Toast.LENGTH_SHORT).show();
                        }
                    });
                    alert.setNegativeButton("Hủy", (dialog, whichButton) -> {/* Canceled.*/});
                    alert.show();
                }
            }
            return true;
        }

        if (menuItem.getItemId() == R.id.alb_setting) {
            Intent intent = new Intent(getContext(), SettingActivity.class);
            intent.putExtra("Fragment", 2);
            requireActivity().startActivity(intent);
            return true;
        }
        if (menuItem.getItemId()==R.id.alb_combine) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                    alert.setTitle("PERMISSION NEEDED");
                    alert.setMessage("This app need you allow to mange your storage to be able to create, or modify images and albums");
                    alert.setPositiveButton("ALLOW", (dialog, whichButton) -> { // Set an EditText view to get user input
                        Intent intent = new Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                        startActivityForResult(intent, 501);
                    });
                    alert.setNegativeButton("DENY", (dialog, whichButton) -> {/* Canceled.*/});
                    ImageView img = new ImageView(getContext());
                    Glide.with(this).asGif().load(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                            + "://" + this.getResources().getResourcePackageName(R.drawable.instruction_manage)
                            + '/' + this.getResources().getResourceTypeName(R.drawable.instruction_manage)
                            + '/' + this.getResources().getResourceEntryName(R.drawable.instruction_manage))).into(img);
                    img.setImageResource(R.drawable.instruction_manage);
                    alert.setView(img);
                    alert.show();
                    return false;
                }
            }
            Intent intent=new Intent(getContext(), CombineAlbumActivity.class);
            startActivity(intent);
            return true;
        }
        return false;
    }

    private Album getAlbumFavorite() throws ParseException {
        Album Favorite = new Album();
        Favorite.setName("Ưa thích");
        List<AlbumFavoriteData>FavList= AppDatabase.getInstance(getContext()).albumFavoriteDataDAO().getAllFavImg();
        for (int i=0;i<FavList.size();i++) {
            Image img=new Image();
            img.setPath(FavList.get(i).imagePath);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                img= ImageGallery.getInstance().getImageByPath(getContext(),FavList.get(i).imagePath);
                SimpleDateFormat formatter = new SimpleDateFormat("dd MMMM, yyyy\nEEEE HH:mm", Locale.UK);
                Date d=formatter.parse(img.getDateTaken());
                SimpleDateFormat Nformatter = new SimpleDateFormat("dd MMMM, yyyy", Locale.UK);
                String datetext=Nformatter.format(d);
                img.setDateTaken(datetext);
                Log.e("date",img.getDateTaken());
            }
            Favorite.getAlbumImages().add(img);
        }
        return Favorite;
    }

    private Album getAlbumPrivate() {
        Album Private = new Album();
        File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + AlbumGallery.rootFolder + AlbumGallery.privateAlbumFolderName);
        File[] content = folder.listFiles();
        String folderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + AlbumGallery.rootFolder + AlbumGallery.privateAlbumFolderName + '/';
        folderPath = folderPath + "%";
        Private.setName("Riêng tư");
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

                    Private.getAlbumImages().add(image);

                } while (cursor.moveToNext());
            }

            cursor.close();
        }
        return Private;
    }

    private Album getRecycleBin() {
        Album RecycleBin = new Album();
        File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + AlbumGallery.rootFolder + AlbumGallery.recycleBinFolderName);
        File[] content = folder.listFiles();
        String folderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + AlbumGallery.rootFolder + AlbumGallery.recycleBinFolderName + '/';
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
        return RecycleBin;
    }

}