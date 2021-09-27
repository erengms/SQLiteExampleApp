package com.example.sqliteexampleapp;

import static android.graphics.ImageDecoder.decodeBitmap;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.example.sqliteexampleapp.databinding.ActivityArtBinding;
import com.google.android.material.snackbar.Snackbar;

public class ArtActivity extends AppCompatActivity {

    private ActivityArtBinding binding;

    //bunları onCreate altında register etmem gerekiyor.
    private ActivityResultLauncher<Intent> activityResultLauncher; // Galeriden seçince napacaz
    private ActivityResultLauncher<String> permissionLauncher; // İzin verilince napacaz

    private Bitmap selectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //view binding
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        registerLauncher();

        binding.imageViewSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // galeriye erişeceğiz, izinleri kontrol edelim.
                if (ContextCompat.checkSelfPermission(ArtActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    //izin verilmediyse

                    if (ActivityCompat.shouldShowRequestPermissionRationale(ArtActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)){
                        Snackbar.make(view, "Galeriye gitmek için izninizi istiyoruz", Snackbar.LENGTH_INDEFINITE)
                                .setAction("İzin Ver", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        // request permission
                                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);

                                    }
                                }).show();
                    } else {
                        //request permission
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }

                }
                else {
                    //izin verildiyse
                    //gallery

                    Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intentToGallery);
                }

            }
        });
    }

    public void registerLauncher(){

        //galeriye gitmek için intent kullandık ya, sonucu StartActivityForResult()
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                //RESULT_OK kullanıcı galeriden bişey seçti.
                if (result.getResultCode() == RESULT_OK){
                    Intent intentFromResult = result.getData();
                        if (intentFromResult != null){
                            Uri imageData =  intentFromResult.getData(); //galeriden seçilen resmin uri'sini verir.
                           // binding.imageViewSelect.setImageURI(imageData);

                            try {

                                if (Build.VERSION.SDK_INT >= 28){
                                    ImageDecoder.Source source = ImageDecoder.createSource(ArtActivity.this.getContentResolver(), imageData);
                                    selectedImage = ImageDecoder.decodeBitmap(source);
                                    binding.imageViewSelect.setImageBitmap(selectedImage);
                                } else {
                                    selectedImage = MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(), imageData);
                                    binding.imageViewSelect.setImageBitmap(selectedImage);
                                }

                            } catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                }
            }
        });

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result){
                    //permission granted
                    Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intentToGallery);
                } else {
                    //permission denied
                    Toast.makeText(ArtActivity.this, "Permission denied!", Toast.LENGTH_LONG).show();
                }
            }
        });


    }


}