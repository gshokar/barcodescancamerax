package ca.aatl.mobi.android.barcodescancamreax;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;

public class MainActivity extends AppCompatActivity {

    public static final int RC_BARCODE_CAPTURE = 1001;
    public static final int PERMISSIONS_REQUEST_CODE = 1002;
    private static final String[] PERMISSIONS_REQUIRED = {Manifest.permission.CAMERA};
    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView tvBarcodeContents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvBarcodeContents = findViewById(R.id.barcode_contents);

        AppCompatButton scanBarcode = findViewById(R.id.scan_barcode);

        scanBarcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                scanBarcodes();


            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case RC_BARCODE_CAPTURE:
                if (resultCode == CommonStatusCodes.SUCCESS) {
                    if (data != null) {
                        String barcode = data.getStringExtra(ScanBarcodeActivity.BARCODE_OBJECT);


                        Log.d(TAG, "Barcode read: " + barcode);

                        tvBarcodeContents.setText(barcode);
                    } else {
                        Log.d(TAG, "No barcode captured, intent data is null");
                        tvBarcodeContents.setText("No barcode captured, intent data is null");
                    }
                } else {

                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Take the user to the success fragment when permission is granted
                Toast.makeText(this, "Permission request granted", Toast.LENGTH_LONG).show();

                scanBarcodes();
            } else {
                Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void scanBarcodes() {
        if (!hasPermission()) {
            // Request camera-related permissions
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE);
        } else {
            Intent intent = new Intent(this, ScanBarcodeActivity.class);
            this.startActivityForResult(intent, MainActivity.RC_BARCODE_CAPTURE);

        }
    }

    private boolean hasPermission() {

        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
}
