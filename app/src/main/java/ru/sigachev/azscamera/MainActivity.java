package ru.sigachev.azscamera;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ImageView imageViewResult;
    private TextView textViewResult;
    private CameraView cameraView;
    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.9f;
    private static final int INPUT_SIZE = 300;
    private static final String INPUT_NAME = "image_tensor";
    private static final String[] OUTPUT_NAME = {"detection_boxes", "detection_scores",
            "detection_classes", "num_detections" };

    private static final String MODEL_FILE = "file:///android_asset/frozen_inference_graph.pb";
    private static final String LABEL_FILE = "file:///android_asset/labels.pbtxt";
    private Button btnDetectObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraView = (CameraView) findViewById(R.id.camera);
        imageViewResult = (ImageView) findViewById(R.id.resIMG);
        textViewResult = (TextView) findViewById(R.id.info);
        btnDetectObject = (Button) findViewById(R.id.click);

        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {

                Bitmap bitmap = cameraKitImage.getBitmap();

                bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);



                final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
                Rect sq;
                final Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.0f);
                Canvas c = new Canvas(bitmap);
                for (Classifier.Recognition r : results
                     ) {
                    if(r.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API)
                    {
                        c.drawRect(results.get(0).getLocation(),paint);
                        imageViewResult.setImageBitmap(bitmap);
                        textViewResult.setText(results.get(0).getConfidence().toString());
                    }
                }




            }



            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }

        });

        btnDetectObject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.captureImage();
            }
        });
        initTensorFlowAndLoadModel();
    }
        @Override
        protected void onResume() {
            super.onResume();
            cameraView.start();
        }

        @Override
        protected void onPause() {
            cameraView.stop();
            super.onPause();
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            executor.execute(new Runnable() {
                                 @Override
                                 public void run() {
                                     classifier.close();
                                 }
                             }
            );


        }
    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowObjectDetectionAPIModel.create(getAssets(),MODEL_FILE, LABEL_FILE,INPUT_SIZE);
                  //  makeButtonVisible();
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }
}
