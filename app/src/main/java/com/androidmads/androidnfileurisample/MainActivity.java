package com.androidmads.androidnfileurisample;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    ImageView imgPreview;
    TextView imgPath;
    Button btnPickCamera;
    Button btnPickGallery;
    Button btnOpenFile;

    Uri outputFileUri;
    private static final int PICK_FROM_CAMERA = 1;
    private static final int PICK_FROM_GALLERY = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initOperations();
    }

    private void initViews() {
        imgPreview = findViewById(R.id.imgPreview);
        imgPath = findViewById(R.id.imgPath);
        btnPickCamera = findViewById(R.id.btnCapture);
        btnPickGallery = findViewById(R.id.btnGallery);
        btnOpenFile = findViewById(R.id.btnOpenImg);
    }

    private void initOperations() {
        btnPickGallery.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onClick(View view) {
                // Checking Permission for Android M and above
                if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PICK_FROM_GALLERY);
                    return;
                }
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                // Start the Intent
                startActivityForResult(galleryIntent, PICK_FROM_GALLERY);
            }
        });

        btnPickCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Checking Permission for Android M and above
                if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, PICK_FROM_CAMERA);
                    return;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    ContentValues values = new ContentValues(1);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
                    outputFileUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    captureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                    startActivityForResult(captureIntent, PICK_FROM_CAMERA);
                } else {
                    Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    File file = new File(Environment.getExternalStorageDirectory(), "MyPhoto.jpg");
                    outputFileUri = Uri.fromFile(file);
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                    startActivityForResult(captureIntent, PICK_FROM_CAMERA);
                }
            }
        });

        btnOpenFile.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onClick(View v) {
                // Checking Permission for Android M and above
                if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PICK_FROM_GALLERY);
                    return;
                }
                File file = new File(imgPath.getText().toString());
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Uri apkURI = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".provider", file);
                    intent.setDataAndType(apkURI, "image/jpg");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else {
                    intent.setDataAndType(Uri.fromFile(file), "image/jpg");
                }
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap bitmap;
        switch (requestCode) {
            case PICK_FROM_CAMERA:
                if (resultCode == Activity.RESULT_OK) {

                    Uri selectedImage = outputFileUri;
                    ContentResolver cr = getContentResolver();
                    getContentResolver().notifyChange(selectedImage, null);
                    try {
                        bitmap = android.provider.MediaStore.Images.Media.getBitmap(cr, selectedImage);
                        int nh = (int) ( bitmap.getHeight() * (512.0 / bitmap.getWidth()) );
                        bitmap = Bitmap.createScaledBitmap(bitmap, 512, nh, true);
                        imgPreview.setImageBitmap(bitmap);
                        imgPath.setText(outputFileUri.getPath());
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT)
                                .show();
                    }
                }
                break;
            case PICK_FROM_GALLERY:
                if (resultCode == Activity.RESULT_OK) {
                    //pick image from gallery
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    // Get the cursor
                    assert selectedImage != null;
                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn,
                            null, null, null);
                    // Move to first row
                    assert cursor != null;
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String imgDecodableString = cursor.getString(columnIndex);
                    cursor.close();
                    bitmap = BitmapFactory.decodeFile(imgDecodableString);
                    imgPreview.setImageBitmap(bitmap);
                    imgPath.setText(imgDecodableString);
                }
                break;
        }
    }
}
