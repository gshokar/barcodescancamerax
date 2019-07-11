package ca.aatl.mobi.android.barcodescancamreax;

import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;

public interface BarcodeAnalyzerListener {
    void onBarcodeAnalyzed(FirebaseVisionBarcode barcode);
}
