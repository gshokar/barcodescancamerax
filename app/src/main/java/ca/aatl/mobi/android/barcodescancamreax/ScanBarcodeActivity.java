package ca.aatl.mobi.android.barcodescancamreax;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Rational;
import android.util.Size;
import android.view.TextureView;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;

public class ScanBarcodeActivity extends AppCompatActivity implements BarcodeAnalyzerListener{

    public static final String BARCODE_OBJECT = "BARCODE_OBJECT";
    TextureView scanBarcodeView;
    private BarcodeAnalyzer barcodeAnalyzer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_barcode);

        scanBarcodeView = findViewById(R.id.scan_barcode_view);


        scanBarcodeView.post(this::startCamera);
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private void startCamera(){


        DisplayMetrics metrics = new DisplayMetrics();

        scanBarcodeView.getDisplay().getMetrics(metrics);
        Point displaySize = new Point();

        scanBarcodeView.getDisplay().getSize(displaySize);

        Rational screenAspectRatio = new Rational(metrics.widthPixels, metrics.heightPixels);
        Size targetResolution = new Size(1920, 1080);

        PreviewConfig config = new PreviewConfig.Builder()
                .setLensFacing(CameraX.LensFacing.BACK)
                .setTargetRotation(scanBarcodeView.getDisplay().getRotation())
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetResolution(targetResolution)
                .build();
        Preview preview = AutoFitPreviewBuilder.build(config, scanBarcodeView);

        HandlerThread thread = new HandlerThread("DriverLicenseBarcodeAnalyzer");
        thread.start();

        ImageAnalysisConfig analysisConfig = new ImageAnalysisConfig.Builder()
                .setLensFacing(CameraX.LensFacing.BACK)
                .setTargetRotation(scanBarcodeView.getDisplay().getRotation())
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setCallbackHandler(new Handler(thread.getLooper()))
                //              .setTargetAspectRatio(screenAspectRatio)
                .setTargetResolution(targetResolution)
                .build();

        barcodeAnalyzer = new BarcodeAnalyzer(this.getBaseContext());

        barcodeAnalyzer.addBarcodeAnalyzerListener(this);

        ImageAnalysis imageAnalyzer = new ImageAnalysis(analysisConfig);

        imageAnalyzer.setAnalyzer(barcodeAnalyzer);

        CameraX.bindToLifecycle(this, preview, imageAnalyzer);
    }

    @Override
    public void onBarcodeAnalyzed(FirebaseVisionBarcode barcode) {

        barcodeAnalyzer.close();

        Intent data = new Intent();
        data.putExtra(BARCODE_OBJECT, barcode.getDisplayValue());
        setResult(CommonStatusCodes.SUCCESS, data);
        finish();
    }
}
