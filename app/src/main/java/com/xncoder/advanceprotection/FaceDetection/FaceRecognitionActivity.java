package com.xncoder.advanceprotection.FaceDetection;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.face.Face;
import com.xncoder.advanceprotection.R;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FaceRecognitionActivity extends AppCompatActivity implements FaceRecognitionProcessor.FaceRecognitionCallback {

    private Interpreter faceNetInterpreter;
    private FaceRecognitionProcessor faceRecognitionProcessor;
    private Face face;
    private Bitmap faceBitmap;
    private float[] faceVector;
    protected PreviewView previewView;
    protected GraphicOverlay graphicOverlay;
    private ExtendedFloatingActionButton addFaceButton;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Executor executor = Executors.newSingleThreadExecutor();
    private VisionBaseProcessor processor;
    private ImageAnalysis imageAnalysis;
    private SaveFaces saveFaces;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_helper_new);

        previewView = findViewById(R.id.camera_source_preview);
        graphicOverlay = findViewById(R.id.graphic_overlay);
        addFaceButton = findViewById(R.id.button_add_face);

        cameraProviderFuture = ProcessCameraProvider.getInstance(getApplicationContext());

        processor = setProcessor();

        saveFaces = new SaveFaces(this);

        initSource();

        addFaceButton.setVisibility(View.VISIBLE);
    }

    protected VisionBaseProcessor setProcessor() {
        try {
            faceNetInterpreter = new Interpreter(FileUtil.loadMappedFile(this, "mobile_face_net.tflite"), new Interpreter.Options());
        } catch (IOException e) {
            e.printStackTrace();
        }

        faceRecognitionProcessor = new FaceRecognitionProcessor(
                faceNetInterpreter,
                graphicOverlay,
                this
        );
        faceRecognitionProcessor.activity = this;
        return faceRecognitionProcessor;
    }

    private void initSource() {

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
            }
        }, ContextCompat.getMainExecutor(getApplicationContext()));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        int lensFacing = getLensFacing();
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build();

        saveFaces.getAllData().forEach((name, vectorFace) -> {
            faceRecognitionProcessor.registerFace(name, convertFloatArray(vectorFace));
        });
        setFaceDetector(lensFacing);
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
    }

    public float[] convertFloatArray(Float[] floatArray) {
        float[] newArray = new float[floatArray.length];
        for (int i = 0; i < floatArray.length; i++) {
            newArray[i] = floatArray[i];
        }
        return newArray;
    }

    protected int getLensFacing() {
        return CameraSelector.LENS_FACING_FRONT;
    }

    private void setFaceDetector(int lensFacing) {
        previewView.getPreviewStreamState().observe(this, new Observer<PreviewView.StreamState>() {
            @Override
            public void onChanged(PreviewView.StreamState streamState) {
                if (streamState != PreviewView.StreamState.STREAMING) {
                    return;
                }

                View preview = previewView.getChildAt(0);
                float width = preview.getWidth() * preview.getScaleX();
                float height = preview.getHeight() * preview.getScaleY();
                float rotation = preview.getDisplay().getRotation();
                if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
                    float temp = width;
                    width = height;
                    height = temp;
                }

                imageAnalysis.setAnalyzer(
                        executor,
                        createFaceDetector((int) width, (int) height, lensFacing)
                );
                previewView.getPreviewStreamState().removeObserver(this);
            }
        });
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private ImageAnalysis.Analyzer createFaceDetector(int width, int height, int lensFacing) {
        graphicOverlay.setPreviewProperties(width, height, lensFacing);
        return imageProxy -> {
            if (imageProxy.getImage() == null) {
                imageProxy.close();
                return;
            }
            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
            // converting from YUV format
            processor.detectInImage(imageProxy, toBitmap(imageProxy.getImage()), rotationDegrees);
            // after done, release the ImageProxy object
            imageProxy.close();
        };
    }

    private Bitmap toBitmap(Image image) {
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

    @Override
    public void onFaceDetected(Face face, Bitmap faceBitmap, float[] faceVector) {
        this.face = face;
        this.faceBitmap = faceBitmap;
        this.faceVector = faceVector;
    }

    @Override
    public void onFaceRecognised(Face face, float probability, String name) {
        Toast.makeText(this, "Name : " + name, Toast.LENGTH_SHORT).show();
    }

    public void onAddFaceClicked(View view) {

        if (face == null || faceBitmap == null) {
            return;
        }

        Bitmap tempBitmap = faceBitmap;
        float[] tempVector = faceVector;

        Intent returnIntent = new Intent();
        returnIntent.putExtra("Bitmap", tempBitmap);
        returnIntent.putExtra("Vector", tempVector);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (processor != null) {
            processor.stop();
        }
    }
}