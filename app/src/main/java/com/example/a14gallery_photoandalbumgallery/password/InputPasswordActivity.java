package com.example.a14gallery_photoandalbumgallery.password;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.andrognito.patternlockview.PatternLockView;
import com.andrognito.patternlockview.listener.PatternLockViewListener;
import com.andrognito.patternlockview.utils.PatternLockUtils;
import com.example.a14gallery_photoandalbumgallery.Image;
import com.example.a14gallery_photoandalbumgallery.MainActivity;
import com.example.a14gallery_photoandalbumgallery.R;
import com.example.a14gallery_photoandalbumgallery.album.Album;
import com.example.a14gallery_photoandalbumgallery.detailAlbum.DetailAlbumActivity;
import com.google.gson.Gson;

import java.io.File;
import java.io.*;
import java.net.StandardSocketOptions;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class InputPasswordActivity extends AppCompatActivity {
    String rootFolder="/14Gallery/";
    String privateAlbumFolderName="PrivateAlbum";

    PatternLockView mPatternLockView;
    String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_password);

        // shared preference when user comes second time to the app
        SharedPreferences sharedPreferences = getSharedPreferences("PREFS", 0);
        password = sharedPreferences.getString("password", "0");

        Bundle bundle = getIntent().getExtras();
        String message = bundle.getString("message");
        Log.e("MESSAGE",message);

        mPatternLockView = (PatternLockView) findViewById(R.id.pattern_lock_view);
        mPatternLockView.addPatternLockListener(new PatternLockViewListener() {
            @Override
            public void onStarted() {

            }

            @Override
            public void onProgress(List<PatternLockView.Dot> progressPattern) {

            }

            @Override
            public void onComplete(List<PatternLockView.Dot> pattern) {
                // if drawn pattern is equal to created pattern you will navigate to home screen
                if (password.equals(PatternLockUtils.patternToString(mPatternLockView, pattern))) {
                    if (Objects.equals(message, "OpenPrivate")) {
                        Album Private = new Album();
                        File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + rootFolder + privateAlbumFolderName);
                        File[] content = folder.listFiles();
                        String folderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + rootFolder + privateAlbumFolderName + '/';
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
                        Cursor cursor = getContentResolver().query(images, projection, selection, selectionArgs, null);
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

                        Intent intent = new Intent(getApplicationContext(), DetailAlbumActivity.class);
                        Gson gson = new Gson();
                        String imagesObj = gson.toJson(Private);
                        intent.putExtra("ALBUM", imagesObj);
                        startActivity(intent);
                    }
                    if (Objects.equals(message, "AddPrivate")) {
                        String imagePath = bundle.getString("imagePath");
                        String name[]=imagePath.split("/");
                        Log.e("PATH",imagePath);
                        Log.e("PATH",Environment.getExternalStorageDirectory().getAbsolutePath()+rootFolder+privateAlbumFolderName+"/"+name[name.length-1]);
                        String imageDestPath=Environment.getExternalStorageDirectory().getAbsolutePath()+rootFolder+privateAlbumFolderName+"/"+name[name.length-1];
                        File dest=new File(imageDestPath);
                        File src=new File(imagePath);
                        moveFile(imagePath, imageDestPath);
                    }
                    finish();
                } else {
                    // other wise you will get error wrong password
                    Toast.makeText(InputPasswordActivity.this, "Wrong Password", Toast.LENGTH_SHORT).show();
                    mPatternLockView.clearPattern();
                }
            }

            @Override
            public void onCleared() {

            }

        });
    }
    private void moveFile(String src, String dest) {
        Path result = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                result = Files.move(Paths.get(src), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Di chuyển ảnh không thành công: "+ e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        if(result != null) {
            Toast.makeText(getApplicationContext(), "Đã di chuyển ảnh vào Riêng tư", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getApplicationContext(), "Di chuyển ảnh không thành công", Toast.LENGTH_SHORT).show();
        }
    }
}