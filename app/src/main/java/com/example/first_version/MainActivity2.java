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
import android.speech.tts.TextToSpeech;
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
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import android.graphics.Matrix;
import android.widget.Toast;

import static androidx.core.math.MathUtils.clamp;

public class MainActivity2 extends AppCompatActivity {
    private PreviewView previewview;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ObjectDetector.ObjectDetectorOptions options;
    private ObjectDetector objectDetector;
    private ActivityMain2Binding binding;
    private TextToSpeech textToSpeech;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= DataBindingUtil.setContentView(this,R.layout.activity_main2);

        previewview = findViewById(R.id.previewview);
        Log.d("Test:","previewFIND");
        options = ObjectDetector.ObjectDetectorOptions.builder().setMaxResults(2).build();
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int ttsLang = textToSpeech.setLanguage(Locale.US);

                    if (ttsLang == TextToSpeech.LANG_MISSING_DATA
                            || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "The Language is not supported!");
                    } else {
                        Log.i("TTS", "Language Supported.");
                    }
                    Log.i("TTS", "Initialization success.");
                } else {
                    Toast.makeText(getApplicationContext(), "TTS Initialization failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });
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

                Bitmap bitmap=yuv420ToBitmap(mediaImage);
                bitmap=rotateBitmap(bitmap,rotation);



                List<Detection> results = objectDetector.detect(TensorImage.fromBitmap(bitmap));
                for(Detection res:results){
                    Log.d("Score ", String.valueOf(res.getCategories().get(0).getScore()));
                    if(binding.parent.getChildCount()>1) binding.parent.removeViewAt(1);
                    RectF rect = res.getBoundingBox();
                    String label=res.getCategories().get(0).getLabel();

                    Log.d("Label:",label);
                    int speechStatus = textToSpeech.speak(label, TextToSpeech.QUEUE_FLUSH, null);

                    if (speechStatus == TextToSpeech.ERROR) {
                        Log.e("TTS", "Error in converting Text to Speech!");
                    }

                    Draw element=new Draw(MainActivity2.this,rect,TextUtils.isEmpty(label)?"undefined":label);

                    binding.parent.addView(element);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.d("Test:","nextDetection");



                image.close();
            }
        });
        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector,
                imageAnalysis, preview);
        Log.d("Test:","binde to life cycle");
    }

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
    public Bitmap yuv420ToBitmap(Image image) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        // sRGB array needed by Bitmap static factory method I use below.
        int[] argbArray = new int[imageWidth * imageHeight];
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        yBuffer.position(0);

        // This is specific to YUV420SP format where U & V planes are interleaved
        // so you can access them directly from one ByteBuffer. The data is saved as
        // UVUVUVUVU... for NV12 format and VUVUVUVUV... for NV21 format.
        //
        // The alternative way to handle this would be refer U & V as separate
        // `ByteBuffer`s and then use PixelStride and RowStride to find the right
        // index of the U or V value per pixel.
        ByteBuffer uvBuffer = image.getPlanes()[1].getBuffer();
        uvBuffer.position(0);
        int r, g, b;
        int yValue, uValue, vValue;

        for (int y = 0; y < imageHeight - 2; y++) {
            for (int x = 0; x < imageWidth - 2; x++) {
                int yIndex = y * imageWidth + x;
                // Y plane should have positive values belonging to [0...255]
                yValue = (yBuffer.get(yIndex) & 0xff);

                int uvx = x / 2;
                int uvy = y / 2;
                // Remember UV values are common for four pixel values.
                // So the actual formula if U & V were in separate plane would be:
                // `pos (for u or v) = (y / 2) * (width / 2) + (x / 2)`
                // But since they are in single plane interleaved the position becomes:
                // `u = 2 * pos`
                // `v = 2 * pos + 1`, if the image is in NV12 format, else reverse.
                int uIndex = uvy * imageWidth + 2 * uvx;
                // ^ Note that here `uvy = y / 2` and `uvx = x / 2`
                int vIndex = uIndex + 1;

                uValue = (uvBuffer.get(uIndex) & 0xff) - 128;
                vValue = (uvBuffer.get(vIndex) & 0xff) - 128;
                r = (int) (yValue + 1.370705f * vValue);
                g = (int) (yValue - (0.698001f * vValue) - (0.337633f * uValue));
                b = (int) (yValue + 1.732446f * uValue);
                r = clamp(r, 0, 255);
                g = clamp(g, 0, 255);
                b = clamp(b, 0, 255);
                // Use 255 for alpha value, no transparency. ARGB values are
                // positioned in each byte of a single 4 byte integer
                // [AAAAAAAARRRRRRRRGGGGGGGGBBBBBBBB]
                argbArray[yIndex] = (255 << 24) | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
            }
        }

        return Bitmap.createBitmap(argbArray, imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
    }
}