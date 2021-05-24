package com.example.first_version;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;

import com.example.first_version.databinding.ActivityMain2Binding;
import com.example.first_version.databinding.ActivityMainBinding;
import com.google.common.util.concurrent.ListenableFuture;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import android.graphics.Matrix;

public class MainActivity2 extends AppCompatActivity {
    private PreviewView previewview;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ObjectDetector.ObjectDetectorOptions options;
    private ObjectDetector objectDetector;
    private ActivityMain2Binding binding;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= DataBindingUtil.setContentView(this,R.layout.activity_main2);

        previewview = findViewById(R.id.previewview);
        Log.d("Test:","previewFIND");
        options = ObjectDetector.ObjectDetectorOptions.builder().setMaxResults(4).build();
        try {
            objectDetector = ObjectDetector.createFromFileAndOptions(this, "lite-model_metadata_2.tflite", options);
            Log.d("Test:","ObjectDetector Initialized");
            cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            Log.d("Test:","cameraprovider");

            cameraProviderFuture.addListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                        //bindImageAnalysis(cameraProvider);
                        bindPreview(cameraProvider);
                        Log.d("Test:","bindimageAnalysis");
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, ContextCompat.getMainExecutor(this));


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void bindPreview(ProcessCameraProvider cameraProvider){
        Preview preview=new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(binding.previewview.getSurfaceProvider());

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                @SuppressLint("UnsafeOptInUsageError") Image mediaImage = image.getImage();
                int rotation=image.getImageInfo().getRotationDegrees();

                Bitmap bitmap=toBitmap(mediaImage);
                bitmap=rotateBitmap(bitmap,rotation);



                List<Detection> results = objectDetector.detect(TensorImage.fromBitmap(bitmap));
                for(Detection res:results){
                    if(binding.parent.getChildCount()>1) binding.parent.removeViewAt(1);
                    RectF rect = res.getBoundingBox();
                    String label=res.getCategories().get(0).getLabel();

                    Log.d("Label:",label);

                    Draw element=new Draw(MainActivity2.this,rect,TextUtils.isEmpty(label)?"undefined":label);

                    binding.parent.addView(element);
                }
                Log.d("Test:","nextDetection");



                image.close();
            }
        });
        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector,
                imageAnalysis, preview);
        Log.d("Test:","binde to life cycle");
    }
   /* private void bindImageAnalysis(@org.jetbrains.annotations.NotNull ProcessCameraProvider cameraProvider) {
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                @SuppressLint("UnsafeOptInUsageError") Image mediaImage = image.getImage();
                int rotation=image.getImageInfo().getRotationDegrees();
                Bitmap bitmap=toBitmap(mediaImage);
                bitmap=rotateBitmap(bitmap,rotation);

                //imag=InputImage.fromMediaImage(mediaImage,image.getImageInfo().getRotationDegrees());


                List<Detection> results = objectDetector.detect(TensorImage.fromBitmap(bitmap));
                for(Detection res:results){
                    RectF rect = res.getBoundingBox();
                    String label=res.getCategories().get(0).getLabel();
                    Log.d("Label:",label);
                    Draw element=new Draw(this,rect,label);

                    binding.parent.addView(element);
                }
                Log.d("Test:","nextDetection");



                image.close();
            }
        });
        Preview preview = new Preview.Builder().build();
        Log.d("Test:","preview Use case");
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewview.getSurfaceProvider());
        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector,
                imageAnalysis, preview);
        Log.d("Test:","binde to life cycle");
    }*/
    private static Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }
    private Bitmap rotateBitmap(Bitmap bitmap,int rotation){
        if(rotation==0)return bitmap;
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);

    }
    public Bitmap ByteArrayToBitmap(byte[] byteArray)
    {
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(byteArray);
        Bitmap bitmap = BitmapFactory.decodeStream(arrayInputStream);
        return bitmap;
    }
}