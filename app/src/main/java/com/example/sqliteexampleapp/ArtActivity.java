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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.io.ByteArrayOutputStream;

public class ArtActivity extends AppCompatActivity {

    private ActivityArtBinding binding;

    //bunları onCreate altında register etmem gerekiyor.
    private ActivityResultLauncher<Intent> activityResultLauncher; // Galeriden seçince napacaz
    private ActivityResultLauncher<String> permissionLauncher; // İzin verilince napacaz

    private Bitmap selectedImage;

    private SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //view binding
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        registerLauncher();

        database = getApplicationContext().openOrCreateDatabase("Arts", MODE_PRIVATE, null);

        Intent intent = getIntent();
        String info  = intent.getStringExtra("info");

        if (info.equals("new")){
            //new Art
            binding.editTextName.setText("");
            binding.editTextYear.setText("");
            binding.imageViewSelect.setImageResource(R.drawable.ic_round_image_search_24);

        } else {
            int artId = intent.getIntExtra("artId", 1);
            binding.buttonSave.setVisibility(View.INVISIBLE);

            try {
                Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", new String[] {String.valueOf(artId)} );
                int nameIx = cursor.getColumnIndex("name");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while (cursor.moveToNext()){
                    binding.editTextName.setText(cursor.getString(nameIx));
                    binding.editTextYear.setText(cursor.getString(yearIx));

                    byte[] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    binding.imageViewSelect.setImageBitmap(bitmap);
                }
                cursor.close();

            } catch (Exception e){
                e.printStackTrace();
            }
        }

        //select image
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

        //save button click
        binding.buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String art_name = binding.editTextName.getText().toString();
                String art_year = binding.editTextYear.getText().toString();

                Bitmap smallImage = makeSmallerImage(selectedImage, 300);

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                smallImage.compress(Bitmap.CompressFormat.PNG, 80, outputStream);
                byte[] byteArray = outputStream.toByteArray();

                // db oluştur ve verileri kaydet
                try {
                    database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, name VARCHAR, year VARCHAR, image BLOB)");

                    String sqlString = "INSERT INTO arts (name, year, image) VALUES(?, ?, ?)";
                    SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
                    // soru işaretleri 1, 2, 3, 4, diye artan indexli gidiyor.
                    sqLiteStatement.bindString(1, art_name);
                    sqLiteStatement.bindString(2, art_year);
                    sqLiteStatement.bindBlob(3, byteArray);
                    sqLiteStatement.execute();

                } catch (Exception e){
                    e.printStackTrace();
                }

                Intent intent = new Intent(ArtActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //Önceki açık activityleri sonlandırır.
                startActivity(intent);
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
                                    selectedImage = ImageDecoder.decodeBitmap(source); //Bitmap'e çevirdik
                                } else {
                                    selectedImage = MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(), imageData);
                                }
                                binding.imageViewSelect.setImageBitmap(selectedImage);

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

    //Bitmap file size küçültme
    public Bitmap makeSmallerImage(Bitmap image, int maxSize){
        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float) width / (float) height;

        if (bitmapRatio > 1) {
            //landscape image
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            // portrait image
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image, width, height, true);
    }


}