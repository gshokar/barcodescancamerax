package ca.aatl.mobi.android.barcodescancamreax;

import android.content.Context;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BarcodeAnalyzer implements ImageAnalysis.Analyzer, OnSuccessListener<List<FirebaseVisionBarcode>> {

    private static final String TAG = BarcodeAnalyzer.class.getSimpleName();
    private FirebaseVisionBarcodeDetector detector;
    private List<BarcodeAnalyzerListener> barcodeAnalyzerListeners = new ArrayList<>();
    private FirebaseVisionBarcodeDetectorOptions options;
    private boolean isDetected = false;

    public BarcodeAnalyzer(Context context) {

        options = new FirebaseVisionBarcodeDetectorOptions
                .Builder()
                .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_PDF417) // other is 39
                .build();
        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);

    }

    public void addBarcodeAnalyzerListener(BarcodeAnalyzerListener listener){
        barcodeAnalyzerListeners.add(listener);
    }

    @Override
    public void analyze(ImageProxy image, int rotationDegrees) {

        if (isDetected == false) {
            FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromMediaImage(image.getImage(), getRotation(rotationDegrees));

            detector.detectInImage(firebaseVisionImage)
                    .addOnFailureListener(ex -> {
                        Log.d(TAG, ex.getMessage());
                    })
                    .addOnSuccessListener(this);
        }
    }

    private int getRotation(int rotationDegrees) {
        int rotation = FirebaseVisionImageMetadata.ROTATION_0;

        switch (rotationDegrees){
            case 0:
                rotation = FirebaseVisionImageMetadata.ROTATION_0;
                break;
            case 90:
                rotation = FirebaseVisionImageMetadata.ROTATION_90;
                break;
            case 180:
                rotation = FirebaseVisionImageMetadata.ROTATION_180;
                break;
            case 270:
                rotation = FirebaseVisionImageMetadata.ROTATION_270;
                break;
        }

        return rotation;
    }

    @Override
    public void onSuccess(List<FirebaseVisionBarcode> barcodes) {
        Log.d(TAG, "Barcodes detected: " + barcodes.size());

        if(barcodes.size() > 0) {
            isDetected = true;
            barcodeAnalyzerListeners.forEach(
                    barcodeAnalyzerListener ->
                            barcodeAnalyzerListener.onBarcodeAnalyzed(barcodes.get(0))
            );
        }
    }

    public void close(){
        if( detector != null){
            try {
                detector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
