package com.example.a14gallery_photoandalbumgallery.image;


import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.a14gallery_photoandalbumgallery.BuildConfig;
import com.example.a14gallery_photoandalbumgallery.GIF.AnimatedGIFWriter;
import com.example.a14gallery_photoandalbumgallery.MoveImageToAlbum.ChooseAlbumActivity;
import com.example.a14gallery_photoandalbumgallery.R;
import com.example.a14gallery_photoandalbumgallery.album.AlbumGallery;
import com.example.a14gallery_photoandalbumgallery.database.AppDatabase;
import com.example.a14gallery_photoandalbumgallery.database.albumFavorite.AlbumFavoriteData;
import com.example.a14gallery_photoandalbumgallery.databinding.FragmentImageBinding;
import com.example.a14gallery_photoandalbumgallery.fullscreenImage.FullscreenImageActivity;
import com.example.a14gallery_photoandalbumgallery.setting.SettingActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;


public class ImageFragment extends Fragment implements MenuProvider {
    FragmentImageBinding binding;

    List<Image> images;
    public int typeView = 4;

    public static ImageFragmentAdapter imageFragmentAdapter;
    RecyclerView.LayoutManager layoutManager;
    GridLayoutManager gridLayoutManager;
    boolean upToDown = true;
    boolean sortByDate = true;

    private Uri imageUri;
    ActivityResultLauncher<String> requestPermissionLauncher;
    private ArrayList<RecyclerData> viewList = null;
    BiConsumer<Integer, View> onItemClick;
    BiConsumer<Integer, View> onItemLongClick;

    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<Intent> activityMoveLauncher;

    String nameGIF = "animation";
    int delay = 500;


    public ImageFragment() {

    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final FragmentActivity activity = requireActivity();
        binding = FragmentImageBinding.inflate(inflater, container, false);
        images = ImageGallery.getInstance().getListOfImages(getContext());
        toViewList();
        layoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);

        binding.imageFragmentRecycleView.setHasFixedSize(true);
        binding.imageFragmentRecycleView.setNestedScrollingEnabled(true);
        binding.imageFragmentRecycleView.setLayoutManager(layoutManager);
        setRecyclerViewLayoutManager(4);

        onItemClick = (position, view1) -> {
            if (imageFragmentAdapter.getState() == ImageFragmentAdapter.State.MultipleSelect) {
                if (!viewList.get(position).imageData.isChecked()) {
                    viewList.get(position).imageData.setChecked(true);
                    images.get(viewList.get(position).index).setChecked(true);
                } else {
                    viewList.get(position).imageData.setChecked(false);
                    images.get(viewList.get(position).index).setChecked(false);
                }
                imageFragmentAdapter.notifyItemChanged(position);
            } else {
                Intent intent = new Intent(getContext(), FullscreenImageActivity.class);
                intent.putExtra("position", images.indexOf(viewList.get(position).imageData));
                intent.putExtra("path", viewList.get(position).imageData.getPath());
                Log.e("imagePath", viewList.get(position).imageData.getPath());
                requireContext().startActivity(intent);
            }
        };

        onItemLongClick = (position, view1) -> {
            imageFragmentAdapter.setState(ImageFragmentAdapter.State.MultipleSelect);
            viewList.get(position).imageData.setChecked(true);
            images.get(viewList.get(position).index).setChecked(true);
            imageFragmentAdapter.notifyItemRangeChanged(0, imageFragmentAdapter.getItemCount());
            activity.invalidateOptionsMenu();
        };

        imageFragmentAdapter = new ImageFragmentAdapter(viewList, onItemClick, onItemLongClick);
        imageFragmentAdapter.setState(ImageFragmentAdapter.State.Normal);
        binding.imageFragmentRecycleView.setAdapter(imageFragmentAdapter);

        // Menu
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        // permission camera
        requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        // Permission is granted. Continue the action or workflow in your
                        // app.
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Images.Media.TITLE, "New Picture");
                        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
                        imageUri = requireContext().getContentResolver().insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                        activityResultLauncher.launch(intent);
                    } else {
                        Toast.makeText(getContext(), "There is no app that support this action", Toast.LENGTH_SHORT).show();
                    }
                });
        //

        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    getActivity();
                    if ( result.getResultCode() == Activity.RESULT_OK) {
                        Log.e("ACTION_IMAGE_CAPTURE", "success");
                    }
                    else{
                        Log.e("ACTION_IMAGE_CAPTURE", "fail");
                        requireContext().getContentResolver().delete(imageUri, null,null);
                    }
                });
        activityMoveLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.e("result code image frag", Integer.toString(result.getResultCode()));
                    if (result.getResultCode() == 123) {
                        Intent data = result.getData();
                        String dest = data.getStringExtra("DEST");
                        Log.e("ImageFragment", dest);
                        moveToAlbum(dest);
                    }
                    imageFragmentAdapter.setState(ImageFragmentAdapter.State.Normal);
                    imageFragmentAdapter.notifyItemRangeChanged(0, imageFragmentAdapter.getItemCount());
                });
        return binding.getRoot();
    }


    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menu.clear();
        menuInflater.inflate(R.menu.top_bar_menu_image, menu);
        if (imageFragmentAdapter.getState() == ImageFragmentAdapter.State.MultipleSelect) {
            menu.getItem(0).setVisible(true);
            menu.getItem(1).setVisible(true);
            menu.getItem(6).setVisible(true);
            menu.getItem(2).setVisible(true);
            menu.getItem(3).setVisible(true);
            menu.getItem(7).setVisible(true);
            menu.getItem(4).setVisible(true);
            menu.getItem(5).setVisible(true);
        } else {
            menu.getItem(0).setVisible(false);
            menu.getItem(1).setVisible(true);
            menu.getItem(6).setVisible(true);
            menu.getItem(7).setVisible(true);
            menu.getItem(2).setVisible(false);
            menu.getItem(3).setVisible(false);
            menu.getItem(4).setVisible(false);
            menu.getItem(5).setVisible(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ImageGallery.getInstance().update(getActivity());
        images = ImageGallery.listOfImages(requireContext());
        toViewList();
        if (this.images.size() > 0) {
            binding.textView.setVisibility(View.GONE);
            binding.imageFragmentRecycleView.setVisibility(View.VISIBLE);
        } else {
            binding.textView.setVisibility(View.VISIBLE);
            binding.imageFragmentRecycleView.setVisibility(View.GONE);
        }
        imageFragmentAdapter.setData(viewList);
    }


    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        Activity activity = requireActivity();
        if (menuItem.getItemId() == R.id.img_camera) {
            // Click camera
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            return true;
        }
        if (menuItem.getItemId() == R.id.img_choose) {
            images.forEach(imageData -> imageData.setChecked(true));
            imageFragmentAdapter.setState(ImageFragmentAdapter.State.MultipleSelect);
            imageFragmentAdapter.notifyItemRangeChanged(0, imageFragmentAdapter.getItemCount());
            activity.invalidateOptionsMenu();
            return true;
        }
        if (menuItem.getItemId() == R.id.clear_choose) {
            imageFragmentAdapter.setState(ImageFragmentAdapter.State.Normal);
            imageFragmentAdapter.notifyItemRangeChanged(0, imageFragmentAdapter.getItemCount());
            activity.invalidateOptionsMenu();
            return true;
        }
        if (menuItem.getItemId() == R.id.img_grid_col_2) {
            setRecyclerViewLayoutManager(2);
            return true;
        }
        if (menuItem.getItemId() == R.id.img_grid_col_3) {
            setRecyclerViewLayoutManager(3);
            return true;
        }
        if (menuItem.getItemId() == R.id.img_grid_col_4) {
            setRecyclerViewLayoutManager(4);
            return true;
        }
        if (menuItem.getItemId() == R.id.img_grid_col_5) {
            setRecyclerViewLayoutManager(5);
            return true;
        }
        if (menuItem.getItemId() == R.id.img_view_mode_normal) {
            // Click { sort UP-TO-DOWN
            if (!upToDown) {
                upToDown = true;
                if (sortByDate) {
                    toViewList();
                } else {
                    toViewListMonth();
                }
                imageFragmentAdapter.setData(viewList);
                binding.imageFragmentRecycleView.setAdapter(imageFragmentAdapter);
            }
            return true;
        }
        if (menuItem.getItemId() == R.id.img_view_mode_convert) {
            // Click { sort DOWN-TO-UP
            if (upToDown) {
                upToDown = false;
                setDownToUp();
                imageFragmentAdapter.setData(viewList);
                binding.imageFragmentRecycleView.setAdapter(imageFragmentAdapter);
            }
            return true;
        }
        if (menuItem.getItemId() == R.id.img_view_mode_day) {
            // Click Sort by day
            if (!sortByDate) {
                toViewList();
                imageFragmentAdapter.setData(viewList);
                binding.imageFragmentRecycleView.setAdapter(imageFragmentAdapter);
                sortByDate = true;
                upToDown = true;
            }
            return true;
        }
        if (menuItem.getItemId() == R.id.img_view_mode_month) {
            // Click Sort by month
            if (sortByDate) {
                toViewListMonth();
                imageFragmentAdapter.setData(viewList);
                binding.imageFragmentRecycleView.setAdapter(imageFragmentAdapter);
                upToDown = true;
                sortByDate = false;
            }
            return true;
        }
        if (menuItem.getItemId() == R.id.img_setting) {   // Click Setting
            Intent intent = new Intent(getContext(), SettingActivity.class);
            intent.putExtra("Fragment", 1);
            requireActivity().startActivity(intent);
            return true;
        }
        if (menuItem.getItemId() == R.id.delete_images) {
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
                    Glide.with(getContext()).asGif().load(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                            + "://" + getContext().getResources().getResourcePackageName(R.drawable.instruction_manage)
                            + '/' + getContext().getResources().getResourceTypeName(R.drawable.instruction_manage)
                            + '/' + getContext().getResources().getResourceEntryName(R.drawable.instruction_manage))).into(img);
                    img.setImageResource(R.drawable.instruction_manage);
                    alert.setView(img);
                    alert.show();
                    return false;
                }
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            ArrayList<Image> selectedImages = images.stream()
                    .filter(Image::isChecked)
                    .collect(Collectors.toCollection(ArrayList::new));
            builder.setTitle("Xác nhận");
            builder.setMessage("Xóa "+selectedImages.size()+" ảnh đã chọn?");

            builder.setPositiveButton("Xóa", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing but close the dialog
                    moveToAlbum(Environment.getExternalStorageDirectory().getAbsolutePath() + AlbumGallery.rootFolder + AlbumGallery.recycleBinFolderName);
                    toViewList();
                    imageFragmentAdapter.setState(ImageFragmentAdapter.State.Normal);
                    imageFragmentAdapter.notifyItemRangeChanged(0, imageFragmentAdapter.getItemCount());
                    imageFragmentAdapter.setData(viewList);
                    binding.imageFragmentRecycleView.setAdapter(imageFragmentAdapter);
                    onResume();
                    activity.invalidateOptionsMenu();
                    dialog.dismiss();
                    Snackbar.make(requireView(), "Xóa ảnh thành công", Snackbar.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Hủy", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {

                    // Do nothing
                    dialog.dismiss();
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
            return true;
        }
        if (menuItem.getItemId() == R.id.move_images) {
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
                    Glide.with(getContext()).asGif().load(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                            + "://" + getContext().getResources().getResourcePackageName(R.drawable.instruction_manage)
                            + '/' + getContext().getResources().getResourceTypeName(R.drawable.instruction_manage)
                            + '/' + getContext().getResources().getResourceEntryName(R.drawable.instruction_manage))).into(img);
                    img.setImageResource(R.drawable.instruction_manage);
                    alert.setView(img);
                    alert.show();
                    return false;
                }
            }
            //Show album to choose
            Intent intent = new Intent(getActivity(), ChooseAlbumActivity.class);
            activityMoveLauncher.launch(intent);
            toViewList();
            imageFragmentAdapter.setData(viewList);
            binding.imageFragmentRecycleView.setAdapter(imageFragmentAdapter);
            activity.invalidateOptionsMenu();
        }

        if (menuItem.getItemId() == R.id.slideShow || menuItem.getItemId() == R.id.slideShow_) {
            if (imageFragmentAdapter.getState() == ImageFragmentAdapter.State.MultipleSelect) {
                List<Image> selectedImages = images.stream()
                        .filter(Image::isChecked)
                        .collect(Collectors.toCollection(ArrayList::new));
                if (selectedImages.isEmpty()) {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Lỗi")
                            .setMessage("Vui lòng chọn hình ảnh để trình chiếu!")
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    SlideshowDialog ssDialog = new SlideshowDialog(selectedImages);
                    ssDialog.show(getParentFragmentManager(), "SlideShowDialog");
                }
            } else {
                SlideshowDialog ssDialog = new SlideshowDialog(images);
                ssDialog.show(getParentFragmentManager(), "SlideShowDialog");
            }
        }

        if (menuItem.getItemId() == R.id.create_GIF) {
            inputGIF();
        }
        if (menuItem.getItemId() == R.id.create_PDF) {
            if (imageFragmentAdapter.getState() == ImageFragmentAdapter.State.MultipleSelect) {
                ArrayList<Image> selectedImages = images.stream()
                        .filter(Image::isChecked)
                        .collect(Collectors.toCollection(ArrayList::new));
                createPDF(getContext(), selectedImages);
                imageFragmentAdapter.setState(ImageFragmentAdapter.State.Normal);
                imageFragmentAdapter.notifyItemRangeChanged(0, imageFragmentAdapter.getItemCount());
                imageFragmentAdapter.setData(viewList);
                binding.imageFragmentRecycleView.setAdapter(imageFragmentAdapter);
                onResume();
                activity.invalidateOptionsMenu();
            } else {
                createPDF(getContext(), images);
            }
        }
        return false;
    }

    public class LoadAsyncTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            ImageGallery.getInstance().update(getActivity());
            images = ImageGallery.listOfImages(requireContext());
            if (sortByDate) {
                toViewList();
            } else {
                toViewListMonth();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            imageFragmentAdapter.setData(viewList);
        }
    }

    private void toViewListMonth() {
        int beg = 3;
        if (images.size() > 0) {
            viewList = new ArrayList<>();
            String label = images.get(0).getDateTaken();
            label = label.substring(beg);
            label += '.';
            for (int i = 0; i < images.size(); i++) {
                String labelCur = images.get(i).getDateTaken();
                labelCur = labelCur.substring(beg);
                if (!labelCur.equals(label)) {
                    label = labelCur;
                    viewList.add(new RecyclerData(RecyclerData.Type.Label, label, images.get(i), i));
                }
                viewList.add(new RecyclerData(RecyclerData.Type.Image, "", images.get(i), i));
            }
        }
        for (int i = 0; i < viewList.size(); i++) {
            if (viewList.get(i).type == RecyclerData.Type.Label) {
                switch (viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6)) {
                    case "January": {
                        viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 1");
                        break;
                    }
                    case "February": {
                        viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 2");
                        break;
                    }
                    case "March": {
                        viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 3");
                        break;
                    }
                    case "April": {
                        viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 4");
                        break;
                    }
                    case "May": {
                        viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 5");
                        break;
                    }
                    case "June": {
                        viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 6");
                        break;
                    }
                    case "July": {
                        viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 7");
                        break;
                    }
                    case "August": {
                        viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 8");
                        break;
                    }
                    case "September": {
                        viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 9");
                        break;
                    }
                    case "October": {
                        viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 10");
                        break;
                    }
                    case "November": {
                        viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 11");
                        break;
                    }
                    case "December": {
                        viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 12");
                        break;
                    }
                    default:
                        break;
                }
            }
        }
    }

    private void toViewList() {
        if (images.size() > 0) {
            viewList = new ArrayList<>();
            String label = images.get(0).getDateTaken();
            label += '.';
            for (int i = 0; i < images.size(); i++) {
                String labelCur = images.get(i).getDateTaken();
                if (!labelCur.equals(label)) {
                    label = labelCur;
                    viewList.add(new RecyclerData(RecyclerData.Type.Label, label, images.get(i), i));
                }
                viewList.add(new RecyclerData(RecyclerData.Type.Image, "", images.get(i), i));
            }
            int beg = 3;
            for (int i = 0; i < viewList.size(); i++) {
                if (viewList.get(i).type == RecyclerData.Type.Label) {
                    switch (viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6)) {
                        case "January": {
                            viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 1");
                            break;
                        }
                        case "February": {
                            viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 2");
                            break;
                        }
                        case "March": {
                            viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 3");
                            break;
                        }
                        case "April": {
                            viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 4");
                            break;
                        }
                        case "May": {
                            viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 5");
                            break;
                        }
                        case "June": {
                            viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 6");
                            break;
                        }
                        case "July": {
                            viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 7");
                            break;
                        }
                        case "August": {
                            viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 8");
                            break;
                        }
                        case "September": {
                            viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 9");
                            break;
                        }
                        case "October": {
                            viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 10");
                            break;
                        }
                        case "November": {
                            viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 11");
                            break;
                        }
                        case "December": {
                            viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 12");
                            break;
                        }
                        default:
                            break;
                    }

                }
            }
        }
    }

    private void setDownToUp() {
        if (images.size() > 0) {
            if (!upToDown && !sortByDate) {
                int beg = 3;
                viewList = new ArrayList<>();
                String label = images.get(0).getDateTaken();
                label = label.substring(beg);
                label += '.';
                for (int i = images.size() - 1; i >= 0; i--) {
                    String labelCur = images.get(i).getDateTaken();
                    labelCur = labelCur.substring(beg);
                    if (!labelCur.equals(label)) {
                        label = labelCur;
                        viewList.add(new RecyclerData(RecyclerData.Type.Label, label, images.get(i), i));
                    }
                    viewList.add(new RecyclerData(RecyclerData.Type.Image, "", images.get(i), i));
                }
                for (int i = 0; i < viewList.size(); i++) {
                    if (viewList.get(i).type == RecyclerData.Type.Label) {
                        switch (viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6)) {
                            case "January": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 1");
                                break;
                            }
                            case "February": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 2");
                                break;
                            }
                            case "March": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 3");
                                break;
                            }
                            case "April": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 4");
                                break;
                            }
                            case "May": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 5");
                                break;
                            }
                            case "June": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 6");
                                break;
                            }
                            case "July": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 7");
                                break;
                            }
                            case "August": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 8");
                                break;
                            }
                            case "September": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 9");
                                break;
                            }
                            case "October": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 10");
                                break;
                            }
                            case "November": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 11");
                                break;
                            }
                            case "December": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(0, viewList.get(i).labelData.length() - 6), "Tháng 12");
                                break;
                            }
                            default:
                                break;
                        }
                    }
                }
            } else {
                viewList = new ArrayList<>();
                String label = images.get(0).getDateTaken();
                label += '.';
                for (int i = images.size() - 1; i >= 0; i--) {
                    String labelCur = images.get(i).getDateTaken();
                    if (!labelCur.equals(label)) {
                        label = labelCur;
                        viewList.add(new RecyclerData(RecyclerData.Type.Label, label, images.get(i), i));
                    }
                    viewList.add(new RecyclerData(RecyclerData.Type.Image, "", images.get(i), i));
                }
                int beg = 3;
                for (int i = 0; i < viewList.size(); i++) {
                    if (viewList.get(i).type == RecyclerData.Type.Label) {
                        switch (viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6)) {
                            case "January": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 1");
                                break;
                            }
                            case "February": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 2");
                                break;
                            }
                            case "March": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 3");
                                break;
                            }
                            case "April": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 4");
                                break;
                            }
                            case "May": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 5");
                                break;
                            }
                            case "June": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 6");
                                break;
                            }
                            case "July": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 7");
                                break;
                            }
                            case "August": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 8");
                                break;
                            }
                            case "September": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 9");
                                break;
                            }
                            case "October": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 10");
                                break;
                            }
                            case "November": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 11");
                                break;
                            }
                            case "December": {
                                viewList.get(i).labelData = viewList.get(i).labelData.replace(viewList.get(i).labelData.substring(beg, viewList.get(i).labelData.length() - 6), "Tháng 12");
                                break;
                            }
                            default:
                                break;
                        }
                    }
                }
            }
        }
    }

    public void setRecyclerViewLayoutManager(int newTypeView) {
        typeView = newTypeView;
        gridLayoutManager = new GridLayoutManager(getContext(), typeView);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return viewList.get(position).type == RecyclerData.Type.Label ? typeView : 1;
            }
        });
        binding.imageFragmentRecycleView.setLayoutManager(gridLayoutManager);
    }

    public boolean isFavorite(String imagePath) {
        AlbumFavoriteData img = AppDatabase.getInstance(getContext()).albumFavoriteDataDAO().getFavImgByPath(imagePath);
        return img != null;
    }

    private void moveToAlbum(String dest) {
        ArrayList<Image> selectedImages = images.stream()
                .filter(Image::isChecked)
                .collect(Collectors.toCollection(ArrayList::new));
        for (Image image : selectedImages) {
            Log.e("src", image.getPath());
            Path result = null;
            String src = image.getPath();
            String[] name = src.split("/");
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    result = Files.move(Paths.get(src), Paths.get(dest + "/" + name[name.length - 1]), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                Toast.makeText(requireActivity().getApplicationContext(), "Di chuyển ảnh không thành công: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            if (result != null) {
                if (isFavorite(src)) {
                    AlbumFavoriteData old = AppDatabase.getInstance(getContext()).albumFavoriteDataDAO().getFavImgByPath(src);
                    AppDatabase.getInstance(getContext()).albumFavoriteDataDAO().delete(old);
                    String[] name2 = dest.split("/");
                    Log.e("hello",dest+"-----"+name[name.length-1]+"--"+name2[name2.length-1]);
                    if (!Objects.equals(name2[name2.length - 1], AlbumGallery.recycleBinFolderName)) {
                        AlbumFavoriteData newImg = new AlbumFavoriteData(dest + name[name.length - 1]);
                        AppDatabase.getInstance(getContext()).albumFavoriteDataDAO().insert(newImg);
                    }
                }
                //Toast.makeText(getActivity().getApplicationContext(), "Đã di chuyển ảnh thành công", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireActivity().getApplicationContext(), "Di chuyển ảnh không thành công", Toast.LENGTH_SHORT).show();
            }
        }
        String[] name = dest.split("/");
        if (Objects.equals(name[name.length - 1], AlbumGallery.recycleBinFolderName)) {
            Snackbar.make(requireView(), "Xóa ảnh thành công", Snackbar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(requireView(), "Di chuyển ảnh thành công", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void createGIF(String dest, int delay) {
        ArrayList<Image> selectedImages = images.stream()
                .filter(Image::isChecked)
                .collect(Collectors.toCollection(ArrayList::new));
        AnimatedGIFWriter writer = new AnimatedGIFWriter(true);
        writer.setDelay(delay);
        OutputStream os;
        try {
            os = new FileOutputStream(dest);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Snackbar.make(requireView(), "Tạo ảnh GIF không thành công", Snackbar.LENGTH_SHORT).show();
            return;
        }
        // Use -1 for both logical screen width and height to use the first frame dimension
        try {
            writer.prepareForWrite(os, -1, -1);
        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(requireView(), "Tạo ảnh GIF không thành công", Snackbar.LENGTH_SHORT).show();
            return;
        }
        for (Image image : selectedImages) {
            Bitmap bitmap = BitmapFactory.decodeFile(image.getPath()); // Grab the Bitmap whatever way you can
            try {
                writer.writeFrame(os, bitmap);
            } catch (Exception e) {
                e.printStackTrace();
                Snackbar.make(requireView(), "Tạo ảnh GIF không thành công", Snackbar.LENGTH_SHORT).show();
                return;
            }
        }
        try {
            writer.finishWrite(os);
        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(requireView(), "Tạo ảnh GIF không thành công", Snackbar.LENGTH_SHORT).show();
            return;
        }
        Snackbar.make(requireView(), "Tạo ảnh GIF thành công", Snackbar.LENGTH_SHORT).show();
    }

    @SuppressLint("SetTextI18n")
    public void inputGIF() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setTitle("Tạo ảnh GIF");
        LinearLayout layout = new LinearLayout(getContext());
        final TextView textView1 = new TextView(getContext());
        final EditText input1 = new EditText(getContext()); // Set an EditText view to get user input
        final TextView textView2 = new TextView(getContext());
        final EditText input2 = new EditText(getContext());
        textView1.setText("Nhập tên ảnh (không cần .gif)");
        textView2.setText("Nhập thời gian delay giữa các frame (ms)");
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(textView1);
        layout.addView(input1);
        layout.addView(textView2);
        layout.addView(input2);
        layout.setPadding(50, 50, 50, 0);
        alert.setView(layout);
        String dest = Environment.getExternalStorageDirectory().getAbsolutePath() + AlbumGallery.rootFolder + "GIF/";
        File file = new File(dest);
        if (!file.exists()) {
            boolean success = file.mkdirs();
            if (success) {
                Log.e("RES", "Success");
            } else {
                Log.e("RES", "Failed");
            }
        }
        alert.setPositiveButton("Ok", (dialog, whichButton) -> {
            nameGIF = input1.getText().toString();
            if (nameGIF.isEmpty()) {
                Snackbar.make(requireView(), "Tạo ảnh GIF không thành công", Snackbar.LENGTH_SHORT).show();
                toViewList();
                imageFragmentAdapter.setState(ImageFragmentAdapter.State.Normal);
                imageFragmentAdapter.notifyItemRangeChanged(0, imageFragmentAdapter.getItemCount());
                requireActivity().invalidateOptionsMenu();
                return;
            }
            try {
                delay = Integer.parseInt(input2.getText().toString());
            } catch (Exception e) {
                Snackbar.make(requireView(), "Tạo ảnh GIF không thành công", Snackbar.LENGTH_SHORT).show();
                toViewList();
                imageFragmentAdapter.setState(ImageFragmentAdapter.State.Normal);
                imageFragmentAdapter.notifyItemRangeChanged(0, imageFragmentAdapter.getItemCount());
                requireActivity().invalidateOptionsMenu();
                return;
            }
            File anh = new File(dest + nameGIF + ".gif");
            if (anh.exists()) {
                AlertDialog.Builder confirm = new AlertDialog.Builder(getContext());
                confirm.setTitle("Đợi một chút");
                confirm.setCancelable(true);
                confirm.setMessage("File " + nameGIF + ".gif đã tồn tại. Bạn có muốn ghi đè không?")
                        .setPositiveButton("Có", (dialog1, id) -> {
                            try {
                                createGIF(dest + nameGIF + ".gif", delay);
                                toViewList();
                                imageFragmentAdapter.setState(ImageFragmentAdapter.State.Normal);
                                imageFragmentAdapter.notifyItemRangeChanged(0, imageFragmentAdapter.getItemCount());
                                requireActivity().invalidateOptionsMenu();
                                onResume();
                            } catch (Exception e) {
                                //Exception
                            }
                        })
                        .setNegativeButton("Không", (dialog12, id) -> {
                            dialog12.cancel();
                            toViewList();
                            imageFragmentAdapter.setState(ImageFragmentAdapter.State.Normal);
                            imageFragmentAdapter.notifyItemRangeChanged(0, imageFragmentAdapter.getItemCount());
                            requireActivity().invalidateOptionsMenu();
                        })
                        .show();
            } else {
                createGIF(dest + nameGIF + ".gif", delay);
                toViewList();
                imageFragmentAdapter.setState(ImageFragmentAdapter.State.Normal);
                imageFragmentAdapter.notifyItemRangeChanged(0, imageFragmentAdapter.getItemCount());
                requireActivity().invalidateOptionsMenu();
                onResume();
            }
        });
        alert.setNegativeButton("Hủy", (dialog, whichButton) -> {
            toViewList();
            imageFragmentAdapter.setState(ImageFragmentAdapter.State.Normal);
            imageFragmentAdapter.notifyItemRangeChanged(0, imageFragmentAdapter.getItemCount());
            requireActivity().invalidateOptionsMenu();
        });
        alert.show();

    }

    private String getPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(requireContext(), contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

    public static void createPDF(Context context, List<Image> images) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle("Tạo PDF");
        alert.setMessage("Tên file PDF");
        final EditText input = new EditText(context);
        alert.setView(input);
        alert.setPositiveButton("Export", (dialog, whichButton) -> {
            String fileName = input.getText().toString();
            new CreatePdfTask(context, images, fileName).execute();
        });
        alert.setNegativeButton("Hủy", (dialog, whichButton) -> {});
        alert.show();
    }

    public static class CreatePdfTask extends AsyncTask<Void, Integer, Void> {
        Context context;
        List<Image> imageList;
        String nameFile;
        ProgressDialog progressDialog;
        String directoryPath = android.os.Environment.getExternalStorageDirectory().toString();
        String dest = directoryPath + AlbumGallery.pdfFolder;

        public CreatePdfTask(Context context2, List<Image> arrayList, String name) {
            context = context2;
            imageList = arrayList;
            nameFile = name;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("Vui lòng chờ đợi một chút...");
            progressDialog.setMessage("Đang tạo file " + nameFile + ".pdf...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(100);
            progressDialog.setCancelable(true);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... Void) {
            try {
                Document document = new Document();

                File file = new File(dest);
                if (!file.exists())
                    file.mkdirs();
                PdfWriter.getInstance(document, new FileOutputStream(dest + nameFile + ".pdf"));
                document.open();
                for (int i = 0; i < imageList.size(); i++) {
                    Bitmap bmp = BitmapFactory.decodeFile(imageList.get(i).getPath());
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    com.itextpdf.text.Image image = com.itextpdf.text.Image.getInstance(stream.toByteArray());
                    float scaler = ((document.getPageSize().getWidth() - document.leftMargin()
                            - document.rightMargin() - 0) / image.getWidth()) * 100; // 0 means you have no indentation. If you have any, change it.
                    image.scalePercent(scaler);
                    image.setAlignment(com.itextpdf.text.Image.ALIGN_CENTER | com.itextpdf.text.Image.ALIGN_TOP);
                    document.add(image);
                    publishProgress(i);
                }
                document.close();
            } catch (DocumentException | IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            this.progressDialog.setProgress(((values[0] + 1) * 100) / imageList.size());
            String sb = "Processing (" + (values[0] + 1) + "/" + this.imageList.size() + ")";
            progressDialog.setTitle(sb);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
            Toast.makeText(context, "Tạo thành công:" + dest + nameFile + ".pdf", Toast.LENGTH_SHORT).show();
        }
    }
}
