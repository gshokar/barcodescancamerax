package ca.aatl.mobi.android.barcodescancamreax;

import android.content.Context;
import android.graphics.Matrix;
import android.hardware.display.DisplayManager;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;

import android.util.Size;
import android.view.View;
import android.view.ViewGroup;

import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class AutoFitPreviewBuilder {

    private static final String TAG = AutoFitPreviewBuilder.class.getSimpleName();

    public static Integer getDisplaySurfaceRotation(Display display) {
        Integer degrees = null;

        if(display != null) {
            switch (display.getRotation()){
                case Surface.ROTATION_0:
                    degrees = 0;
                    break;
                case Surface.ROTATION_90:
                    degrees = 90;
                    break;
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;
            }
        }
        return degrees;
    }

    public static Preview build(PreviewConfig config, TextureView view){
        return new AutoFitPreviewBuilder(config, view).preview;
    }

    private PreviewConfig config;
    private TextureView view;
    private Preview preview;

    private AutoFitPreviewBuilder(PreviewConfig config, TextureView view) {
        this.config = config;
        this.view = view;
        init();
    }

    /** Internal variable used to keep track of the use case's output rotation */
    private int bufferRotation = 0;

    /** Internal variable used to keep track of the viewRef's rotation */
    private Integer viewFinderRotation = null;

    /** Internal variable used to keep track of the use-case's output dimension */
    private Size bufferDimens = new Size(0, 0);

    /** Internal variable used to keep track of the viewRef's dimension */
    private Size viewFinderDimens = new Size(0, 0);

    /** Internal variable used to keep track of the viewRef's display */
    private int viewFinderDisplay = -1;

    /** Internal reference of the [DisplayManager] */
    private DisplayManager displayManager;

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {

        }

        @Override
        public void onDisplayRemoved(int displayId) {

        }

        @Override
        public void onDisplayChanged(int displayId) {

            if(view == null) return;

            if (displayId == viewFinderDisplay) {
                Display display = displayManager.getDisplay(displayId);
                Integer rotation = getDisplaySurfaceRotation(display);
                updateTransform(view, rotation, bufferDimens, viewFinderDimens);
            }
        }
    };

    private void updateTransform(TextureView view, Integer rotation, Size newBufferDimens, Size newViewFinderDimens
    ) {

        if(view == null) return;

        if (rotation == viewFinderRotation &&
                Objects.equals(newBufferDimens, bufferDimens) &&
                Objects.equals(newViewFinderDimens, viewFinderDimens)) {
            // Nothing has changed, no need to transform output again
            return;
        }

        if (rotation == null) {
            // Invalid rotation - wait for valid inputs before setting matrix
            return;
        } else {
            // Update internal field with new inputs
            viewFinderRotation = rotation;
        }

        if (newBufferDimens.getWidth() == 0 || newBufferDimens.getHeight() == 0) {
            // Invalid buffer dimens - wait for valid inputs before setting matrix
            return;
        } else {
            // Update internal field with new inputs
            bufferDimens = newBufferDimens;
        }

        if (newViewFinderDimens.getWidth() == 0 || newViewFinderDimens.getHeight() == 0) {
            // Invalid view finder dimens - wait for valid inputs before setting matrix
            return;
        } else {
            // Update internal field with new inputs
            viewFinderDimens = newViewFinderDimens;
        }

        Matrix matrix = new Matrix();

        Log.d(TAG, "Applying output transformation.\n" +
                "View finder size: $viewFinderDimens.\n" +
                "Preview output size: $bufferDimens\n" +
                "View finder rotation: $viewFinderRotation\n" +
                "Preview output rotation: $bufferRotation");

        // Compute the center of the view finder
        float centerX = viewFinderDimens.getWidth() / 2f;
        float centerY = viewFinderDimens.getHeight() / 2f;

        // Correct preview output to account for display rotation
        matrix.postRotate(new Float(-viewFinderRotation), centerX, centerY);

        // Buffers are rotated relative to the device's 'natural' orientation: swap width and height
        float bufferRatio = bufferDimens.getHeight() / (float)bufferDimens.getWidth();

        int scaledWidth;
        int scaledHeight;
        // Match longest sides together -- i.e. apply center-crop transformation
        if (viewFinderDimens.getWidth() > viewFinderDimens.getHeight()) {
            scaledHeight = viewFinderDimens.getWidth();
            scaledWidth = Math.round(viewFinderDimens.getWidth() * bufferRatio);
        } else {
            scaledHeight = viewFinderDimens.getHeight();
            scaledWidth = Math.round(viewFinderDimens.getHeight() * bufferRatio);
        }

        // Compute the relative scale value
        float xScale = scaledWidth / (float) viewFinderDimens.getWidth();
        float yScale = scaledHeight / (float) viewFinderDimens.getHeight();

        // Scale input buffers to fill the view finder
        matrix.preScale(xScale, yScale, centerX, centerY);

        // Finally, apply transformations to our View
        view.setTransform(matrix);
    }

    private void init() {

        if(view == null) {
            throw new IllegalArgumentException("Invalid reference to view finder used");
        }
        // Initialize the display and rotation from texture view information
        viewFinderDisplay = view.getDisplay().getDisplayId();
        viewFinderRotation = (getDisplaySurfaceRotation(view.getDisplay()) == null) ? getDisplaySurfaceRotation(view.getDisplay()) : 0;

        // Initialize public use-case with the given config
        preview = new Preview(config);

        // Every time the view finder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener(previewOutput -> {

            if(view == null){
                return;
            }

            Log.d(TAG, "Preview output changed. " +
                    "Size: " + previewOutput.getTextureSize().toString() +
                    " Rotation: " + previewOutput.getRotationDegrees());

            // To update the SurfaceTexture, we have to remove it and re-add it
            ViewGroup parent = (ViewGroup)view.getParent();
            parent.removeView(view);
            parent.addView(view, 0);

            // Update internal texture
            view.setSurfaceTexture(previewOutput.getSurfaceTexture());

            // Apply relevant transformations
            bufferRotation = previewOutput.getRotationDegrees();
            Integer rotation = getDisplaySurfaceRotation(view.getDisplay());
            updateTransform(view, rotation, previewOutput.getTextureSize(), viewFinderDimens);
        });

        // Every time the provided texture view changes, recompute layout
        view.addOnLayoutChangeListener(
                (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    TextureView viewTexture = (TextureView)view;

                    Size newViewFinderDimens = new Size(right - left, bottom - top);

                    Log.d(TAG, "View finder layout changed. Size: " + newViewFinderDimens.toString());

                    Integer rotation = getDisplaySurfaceRotation(viewTexture.getDisplay());

                    updateTransform(viewTexture, rotation, bufferDimens, newViewFinderDimens);

                });

        // Every time the orientation of device changes, recompute layout
        // NOTE: This is unnecessary if we listen to display orientation changes in the camera
        //  fragment and call [Preview.setTargetRotation()] (like we do in this sample), which will
        //  trigger [Preview.OnPreviewOutputUpdateListener] with a new
        //  [PreviewOutput.rotationDegrees]. CameraX Preview use case will not rotate the frames for
        //  us, it will just tell us about the buffer rotation with respect to sensor orientation.
        //  In this sample, we ignore the buffer rotation and instead look at the view finder's
        //  rotation every time [updateTransform] is called, which gets triggered by
        //  [CameraFragment] display listener -- but the approach taken in this sample is not the
        //  only valid one.
        displayManager = (DisplayManager) view.getContext().getSystemService(Context.DISPLAY_SERVICE);
        displayManager.registerDisplayListener(displayListener, null);

        // Remove the display listeners when the view is detached to avoid holding a reference to
        //  it outside of the Fragment that owns the view.
        // NOTE: Even though using a weak reference should take care of this, we still try to avoid
        //  unnecessary calls to the listener this way.
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            @Override
            public void onViewAttachedToWindow(View view) {
                displayManager.registerDisplayListener(displayListener, null);
            }

            @Override
            public void onViewDetachedFromWindow(View view) {
                displayManager.unregisterDisplayListener(displayListener);
            }
        });
    }
}
