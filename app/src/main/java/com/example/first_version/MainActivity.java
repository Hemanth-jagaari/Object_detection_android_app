package com.example.first_version;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;
    private ImageView imgview;
    private TextView textview;
    private Canvas canvas;
    private Paint paint;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imgview=findViewById(R.id.imageView);
        textview=findViewById(R.id.textview);
        Button enableCamera = findViewById(R.id.enableCamera);
        enableCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasCameraPermission()) {
                    enableCamera();
                } else {
                    requestPermission();
                }
            }
        });
        AssetManager assetManager = getAssets();
        paint = new Paint();
        paint.setAlpha(0xA0); // the transparency
        paint.setColor(Color.RED); // color is red
        paint.setStyle(Paint.Style.STROKE); // stroke or fill or ...
        paint.setStrokeWidth(5);


        ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder().setMaxResults(4).build();
        try {
            InputStream istr = assetManager.open("photo_second.jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(istr);

            istr.close();


             bitmap=bitmap.copy(Bitmap.Config.ARGB_8888,true);

            ObjectDetector objectDetector = ObjectDetector.createFromFileAndOptions(this, "lite-model_metadata_2.tflite", options);




            List<Detection> results = objectDetector.detect(TensorImage.fromBitmap(bitmap));
            for(Detection res:results){
                textview.append(" "+res.getCategories().get(0).getLabel()+" ");
                canvas = new Canvas(bitmap);
                canvas.drawRect(res.getBoundingBox(), paint);


            }
            imgview.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestPermission() {
        ActivityCompat.requestPermissions(
                this,
                CAMERA_PERMISSION,
                CAMERA_REQUEST_CODE
        );
    }

    private void enableCamera() {
        Intent intent = new Intent(this, MainActivity2.class);
        startActivity(intent);
    }
}