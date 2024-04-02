package com.xncoder.advanceprotection.FaceDetection;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Trace;
import android.util.Size;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.xncoder.advanceprotection.R;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class CameraActivity extends AppCompatActivity
        implements ImageReader.OnImageAvailableListener,
        Camera.PreviewCallback {

    private boolean useCamera2API;
    private boolean addPending = false;
    private boolean isProcessingFrame = false;
    private boolean computingDetection = false;
    protected int previewHeight = 0, previewWidth = 0, yRowStride;
    private static final boolean TF_OD_API_IS_QUANTIZED = false, MAINTAIN_ASPECT = false, SAVE_PREVIEW_BITMAP = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 360);
    OverlayView trackingOverlay;
    private Matrix cropToFrameTransform, frameToCropTransform;
    private Bitmap croppedBitmap;
    private Bitmap faceBmp;
    private Bitmap rgbFrameBitmap;
    private Bitmap portraitBmp;
    private FaceDetector faceDetector;
    private Integer sensorOrientation;
    private static final int TF_OD_API_INPUT_SIZE = 112;
    private SimilarityClassifier detector;
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt", TF_OD_API_MODEL_FILE = "mobile_face_net.tflite", KEY_USE_FACING = "use_facing";
    private MultiBoxTracker tracker;
    private Integer useFacing;
    private int[] rgbBytes;
    private final byte[][] yuvBytes = new byte[3][];
    private Runnable imageConverter, postInferenceCallback;
    private long timestamp = 0;
    private SaveFaces saveFaces;
    private static final DetectorMode MODE = DetectorMode.TF_OD_API;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_face_detection);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton addFaces = findViewById(R.id.add_faces);
        addFaces.setOnClickListener(view -> {
            addPending = true;
        });

        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();

        faceDetector = FaceDetection.getClient(options);

        Intent intent = getIntent();

        useFacing = intent.getIntExtra(KEY_USE_FACING, CameraCharacteristics.LENS_FACING_FRONT);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        saveFaces = new SaveFaces(this);

        setFragment();
    }

    protected void setFragment() {
        String cameraId = chooseCamera();
        Fragment fragment;
        if (useCamera2API) {
            CameraConnectionFragment camera2Fragment =
                    CameraConnectionFragment.newInstance(
                            (size, rotation) -> {
                                previewHeight = size.getHeight();
                                previewWidth = size.getWidth();
                                CameraActivity.this.onPreviewSizeChosen(size, rotation);
                            },
                            this,
                            getLayoutId(),
                            getDesiredPreviewFrameSize());

            camera2Fragment.setCamera(cameraId);
            fragment = camera2Fragment;
        } else {
            int facing = (useFacing == CameraCharacteristics.LENS_FACING_BACK) ?
                    Camera.CameraInfo.CAMERA_FACING_BACK :
                    Camera.CameraInfo.CAMERA_FACING_FRONT;
            fragment = new LegacyCameraConnectionFragment(this,
                    getLayoutId(),
                    getDesiredPreviewFrameSize(), facing);
        }
        getFragmentManager().beginTransaction().replace(R.id.preview_camera, fragment).commit();
    }

    private boolean isHardwareLevelSupported(CameraCharacteristics characteristics) {
        int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            return false;
        }
        // deviceLevel is not LEGACY, can use numerical sort
        return android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL <= deviceLevel;
    }

    private String chooseCamera() {
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                final StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (useFacing != null &&
                        facing != null &&
                        !facing.equals(useFacing)
                ) {
                    continue;
                }
                useCamera2API = (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                        || isHardwareLevelSupported(
                        characteristics);
                return cameraId;
            }
        } catch (CameraAccessException ignored) {
        }

        return null;
    }

    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
        BorderedText borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        try {
            detector = TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED,
                    this);
        } catch (final IOException e) {
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();

        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);


        int targetW, targetH;
        if (sensorOrientation == 90 || sensorOrientation == 270) {
            targetH = previewWidth;
            targetW = previewHeight;
        }
        else {
            targetW = previewWidth;
            targetH = previewHeight;
        }
        int cropW = (int) (targetW / 2.0);
        int cropH = (int) (targetH / 2.0);

        croppedBitmap = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888);

        portraitBmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        faceBmp = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropW, cropH,
                        sensorOrientation, MAINTAIN_ASPECT);


        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                canvas -> {
                    tracker.draw(canvas);
                    if (isDebug()) {
                        tracker.drawDebug(canvas);
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    public boolean isDebug() {
        return false;
    }

    protected int getLayoutId() {
        return R.layout.fragment_tracking;
    }

    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    private void showAddFaceDialog(SimilarityClassifier.Recognition rec) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.image_popup, null);

        ImageView face = popupView.findViewById(R.id.dlg_image);
        EditText faceN = popupView.findViewById(R.id.face_input);
        Button okBtn = popupView.findViewById(R.id.button_ok);
        Button cancelBtn = popupView.findViewById(R.id.button_cancel);

        face.setImageBitmap(rec.getCrop());
        faceN.setHint("Enter the name");

        PopupWindow popupWindow = new PopupWindow(popupView, (int) (getResources().getDisplayMetrics().widthPixels * 0.7),
                (int) (getResources().getDisplayMetrics().heightPixels * 0.46));

        okBtn.setOnClickListener(view -> {
//            saveFaces.insertImage(rec.getCrop(), faceN.getText().toString());
            detector.register(faceN.getText().toString(), rec);
            popupWindow.dismiss();
        });

        cancelBtn.setOnClickListener(view -> popupWindow.dismiss());

        popupWindow.setOutsideTouchable(false);
        popupWindow.setFocusable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
    }

    private void retrieve() {

        saveFaces.getAllImages().forEach(bitmapStringPair -> {

            float confidence;

            final List<SimilarityClassifier.Recognition> resultsAux = detector.recognizeImage(faceBmp, true);

            if (!resultsAux.isEmpty()) {
                SimilarityClassifier.Recognition result = resultsAux.get(0);
                float conf = result.getDistance();
                if (conf < 1.0f) {
                    confidence = conf;
                } else {
                    confidence = -1f;
                }
            } else {
                confidence = -1f;
            }

            InputImage image = InputImage.fromBitmap(bitmapStringPair.first, 0);
            faceDetector
                    .process(image)
                    .addOnSuccessListener(faces -> {
                        if (faces.isEmpty()) {
                            return;
                        }
                        for(Face face : faces) {
                            final SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
                                    "0", bitmapStringPair.second, confidence, new RectF(face.getBoundingBox()));
                            detector.register(bitmapStringPair.second, result);
                        }

                    });


        });
    }

    private void updateResults(long currTimestamp, final List<SimilarityClassifier.Recognition> mappedRecognitions) {

        tracker.trackResults(mappedRecognitions, currTimestamp);
        trackingOverlay.postInvalidate();
        computingDetection = false;

        if (!mappedRecognitions.isEmpty()) {
            SimilarityClassifier.Recognition rec = mappedRecognitions.get(0);

            if (rec.getExtra() != null) {
                showAddFaceDialog(rec);
            }

        }
    }

    @Override
    public synchronized void onStart() {
        super.onStart();
    }
    @Override
    public synchronized void onResume() {
        super.onResume();
    }
    @Override
    public synchronized void onPause() {
        super.onPause();
    }
    @Override
    public synchronized void onStop() {
        super.onStop();
    }
    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
    }

    private enum DetectorMode {
        TF_OD_API;
    }

    private Matrix createTransform(final int srcWidth,
            final int srcHeight, final int dstWidth,
            final int dstHeight, final int applyRotation) {

        Matrix matrix = new Matrix();
        if (applyRotation != 0) {
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);
            matrix.postRotate(applyRotation);
        }

        if (applyRotation != 0) {
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;

    }

    protected Integer getCameraFacing() {
        return useFacing;
    }

    private void onFacesDetected(long currTimestamp, List<Face> faces, boolean add) {

        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);

        Objects.requireNonNull(MODE);

        final List<SimilarityClassifier.Recognition> mappedRecognitions =
                new LinkedList<>();

        int sourceW = rgbFrameBitmap.getWidth();
        int sourceH = rgbFrameBitmap.getHeight();
        int targetW = portraitBmp.getWidth();
        int targetH = portraitBmp.getHeight();
        Matrix transform = createTransform(
                sourceW,
                sourceH,
                targetW,
                targetH,
                sensorOrientation);
        final Canvas cv = new Canvas(portraitBmp);

        cv.drawBitmap(rgbFrameBitmap, transform, null);

        final Canvas cvFace = new Canvas(faceBmp);

        for (Face face : faces) {

            final RectF boundingBox = new RectF(face.getBoundingBox());

            cropToFrameTransform.mapRect(boundingBox);

            RectF faceBB = new RectF(boundingBox);
            transform.mapRect(faceBB);

            float sx = ((float) TF_OD_API_INPUT_SIZE) / faceBB.width();
            float sy = ((float) TF_OD_API_INPUT_SIZE) / faceBB.height();
            Matrix matrix = new Matrix();
            matrix.postTranslate(-faceBB.left, -faceBB.top);
            matrix.postScale(sx, sy);

            cvFace.drawBitmap(portraitBmp, matrix, null);

            String label = "";
            float confidence = -1f;
            int color = Color.rgb(140, 87, 255);
            Object extra = null;
            Bitmap crop = null;

            if (add) {
                crop = Bitmap.createBitmap(portraitBmp,
                        (int) faceBB.left,
                        (int) faceBB.top,
                        (int) faceBB.width(),
                        (int) faceBB.height());
            }

            final List<SimilarityClassifier.Recognition> resultsAux = detector.recognizeImage(faceBmp, add);

            if (!resultsAux.isEmpty()) {
                SimilarityClassifier.Recognition result = resultsAux.get(0);
                extra = result.getExtra();
                float conf = result.getDistance();
                if (conf < 1.0f) {

                    confidence = conf;
                    label = result.getTitle();
                    if (result.getId().equals("0")) {
                        color = Color.GREEN;
                    }
                    else {
                        color = Color.RED;
                    }
                }
            }

            if (getCameraFacing() == CameraCharacteristics.LENS_FACING_FRONT) {
                Matrix flip = new Matrix();
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    flip.postScale(1, -1, previewWidth / 2.0f, previewHeight / 2.0f);
                }
                else {
                    flip.postScale(-1, 1, previewWidth / 2.0f, previewHeight / 2.0f);
                }
                flip.mapRect(boundingBox);

            }

            final SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
                    "0", label, confidence, boundingBox);

            result.setColor(color);
            result.setLocation(boundingBox);
            result.setExtra(extra);
            result.setCrop(crop);
            mappedRecognitions.add(result);

        }
        updateResults(currTimestamp, mappedRecognitions);
    }

    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        InputImage image = InputImage.fromBitmap(croppedBitmap, 0);
        faceDetector
                .process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.isEmpty()) {
                        updateResults(currTimestamp, new LinkedList<>());
                        return;
                    }
                        onFacesDetected(currTimestamp, faces, addPending);
                        addPending = false;
                });
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (isProcessingFrame) {
            return;
        }

        try {
            if (rgbBytes == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                int rotation = 90;
                if (useFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                    rotation = 270;
                }
                onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), rotation);
            }
        } catch (final Exception e) {
            return;
        }

        isProcessingFrame = true;
        yuvBytes[0] = bytes;
        yRowStride = previewWidth;

        imageConverter =
                () -> ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);

        postInferenceCallback =
                () -> {
                    camera.addCallbackBuffer(bytes);
                    isProcessingFrame = false;
                };
        processImage();
    }

    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        try {
            final Image image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (isProcessingFrame) {
                image.close();
                return;
            }
            isProcessingFrame = true;
            Trace.beginSection("imageAvailable");
            final Image.Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter =
                    () -> ImageUtils.convertYUV420ToARGB8888(
                            yuvBytes[0],
                            yuvBytes[1],
                            yuvBytes[2],
                            previewWidth,
                            previewHeight,
                            yRowStride,
                            uvRowStride,
                            uvPixelStride,
                            rgbBytes);

            postInferenceCallback =
                    () -> {
                        image.close();
                        isProcessingFrame = false;
                    };

            processImage();
        } catch (final Exception e) {
            Trace.endSection();
            return;
        }
        Trace.endSection();
    }

}