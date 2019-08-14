package com.kotlinify.cropiwa.sample;

import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.View;

import com.kotlinify.cropiwa.image.CropIwaResultReceiver;
import com.kotlinify.cropiwa.sample.adapter.CropGalleryAdapter;
import com.kotlinify.cropiwa.sample.data.CropGallery;
import com.kotlinify.cropiwa.sample.fragment.ChooseImageForCropFragment;
import com.kotlinify.cropiwa.sample.fragment.ConfirmDeletePhotoFragment;

public class CropGalleryActivity extends AppCompatActivity implements CropIwaResultReceiver.Listener,
        CropGalleryAdapter.Listener, ConfirmDeletePhotoFragment.Listener {

    private static final String TAG_CHOOSE_IMAGE_FRAGMENT = "choose_image";
    private static final String TAG_CONFIRM_DELETE_IMAGE = "confirm_delete";

    private CropIwaResultReceiver cropResultReceiver;
    private CropGalleryAdapter cropGalleryAdapter;

    private View container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_gallery);

        container = findViewById(R.id.container);

        cropGalleryAdapter = new CropGalleryAdapter();
        cropGalleryAdapter.setListener(this);
        cropGalleryAdapter.addImages(CropGallery.getCroppedImageUris());
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(cropGalleryAdapter);

        cropResultReceiver = new CropIwaResultReceiver();
        cropResultReceiver.setListener(this);
        cropResultReceiver.register(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_crop_gallery);
        setSupportActionBar(toolbar);
    }

    @Override
    public void onCropSuccess(Uri croppedUri) {
        cropGalleryAdapter.addImage(croppedUri);
    }

    @Override
    public void onNewCropButtonClicked() {
        ChooseImageForCropFragment fragment = new ChooseImageForCropFragment();
        fragment.show(getSupportFragmentManager(), TAG_CHOOSE_IMAGE_FRAGMENT);
    }

    @Override
    public void onLongPressOnImage(Uri image) {
        ConfirmDeletePhotoFragment fragment = new ConfirmDeletePhotoFragment();
        fragment.setListener(this, image);
        fragment.show(getSupportFragmentManager(), TAG_CONFIRM_DELETE_IMAGE);
    }

    @Override
    public void onDeleteConfirmed(Uri image) {
        cropGalleryAdapter.removeImage(image);
        CropGallery.removeFromGallery(image);
    }

    @Override
    public void onCropFailed(Throwable e) {
        Snackbar.make(container,
                getString(R.string.msg_crop_failed, e.getMessage()),
                Snackbar.LENGTH_LONG)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cropResultReceiver.unregister(this);
    }
}
