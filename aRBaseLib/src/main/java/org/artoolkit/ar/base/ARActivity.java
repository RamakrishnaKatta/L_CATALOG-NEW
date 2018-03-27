package org.artoolkit.ar.base;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.artoolkit.ar.base.camera.CameraEventListener;
import org.artoolkit.ar.base.camera.CaptureCameraPreview;
import org.artoolkit.ar.base.rendering.ARRenderer;
import org.artoolkit.ar.base.rendering.gles20.ARRendererGLES20;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

//import android.os.AsyncTask;
//import android.os.AsyncTask.Status;

/**
 * An activity which can be subclassed to create an AR application. ARActivity handles almost all of
 * the required operations to create a simple augmented reality application.
 * <p/>
 * ARActivity automatically creates a camera preview surface and an OpenGL surface view, and
 * arranges these correctly in the user interface.The subclass simply needs to provide a FrameLayout
 * object which will be populated with these UI components, using {@link #supplyFrameLayout() supplyFrameLayout}.
 * <p/>
 * To create a custom AR experience, the subclass should also provide a custom renderer using
 * {@link #supplyRenderer() Renderer}. This allows the subclass to handle OpenGL drawing calls on its own.
 */

@TargetApi(Build.VERSION_CODES.KITKAT)
public abstract class ARActivity extends Activity implements CameraEventListener, View.OnClickListener {

    /**
     * Android logging tag for this class.
     */
    protected final static String TAG = "ARActivity";
    /**
     * Renderer to use. This is provided by the subclass using {@link #supplyRenderer() Renderer()}.
     */
    protected ARRenderer renderer;

    /**
     * Layout that will be filled with the camera preview and GL views. This is provided by the subclass using {@link #supplyFrameLayout() supplyFrameLayout()}.
     */
    protected FrameLayout mainFrameLayout;
//    LinearLayout mFlashButtonArea;
    /**
     * Camera preview which will provide video frames.
     */
    private CaptureCameraPreview preview;
    /**
     * GL surface to render the virtual objects
     */
    private GLSurfaceView mOpenGlSurfaceViewInstance;
    private boolean firstUpdate = false;
    private Context mContext;
    private ImageButton mSettingButton;
    private ImageButton mFlashButton;
    private ImageButton mCaptureButton;
    private ImageButton mScreenshotButton;
    private ImageButton mHdrButton, mAutoSceneButton, mWhiteBalanceButton, mContinousPictureButton, mAutoFocusButton, mSteadyShotButton;
    private LinearLayout mHdrButtonArea, mAutoSceneButtonArea, mFlashArea, mWhiteBalanceButtonArea, mContinousPictureButtonArea, mAutoFocusButtonArea, mSteadyShotButtonArea;

    private boolean flashmode = false;
    private boolean cameraoptions_visibility = false;
    private boolean hdr_toggle = false;
    private boolean steady_toggle = false;
    private boolean autoscene_toggle = false;
    private boolean autofocus_toggle = false;
    private boolean continouspicture_toggle = false;
    private boolean whitebalance_toggle = false;
    private ProgressDialog progressDialog;


    @SuppressWarnings("unused")
    public Context getAppContext() {
        return mContext;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();


        // This needs to be done just only the very first time the application is run,
        // or whenever a new preference is added (e.g. after an application upgrade).
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Correctly configures the activity window for running AR in a layer
        // on top of the camera preview. This includes entering 
        // fullscreen landscape mode and enabling transparency. 
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        progressDialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.setTitle("Configuring your Camera");
        progressDialog.setMessage("It Takes few minutes, Please be patient.");
        progressDialog.show();

        AndroidUtils.reportDisplayInformation(this);
    }

    /**
     * Allows subclasses to supply a custom {@link Renderer}.
     *
     * @return The {@link Renderer} to use.
     */
    protected abstract ARRenderer supplyRenderer();

    /**
     * Allows subclasses to supply a {@link FrameLayout} which will be populated
     * with a camera preview and GL surface view.
     *
     * @return The {@link FrameLayout} to use.
     */
    protected abstract FrameLayout supplyFrameLayout();

    @Override
    protected void onStart() {

        super.onStart();

        Log.i(TAG, "onStart(): Activity starting.");
        progressDialog.setMessage("We made sure its worth waiting !!");

        if (!ARToolKit.getInstance().initialiseNative(this.getCacheDir().getAbsolutePath())) { // Use cache directory for Data files.
            notifyFinish("The native library is not loaded. The application cannot continue.");
            return;
        }

        mainFrameLayout = supplyFrameLayout();
        if (mainFrameLayout == null) {
            Log.e(TAG, "onStart(): Error: supplyFrameLayout did not return a layout.");
            return;
        }

        renderer = supplyRenderer();
        if (renderer == null) {
            Log.e(TAG, "onStart(): Error: supplyRenderer did not return a renderer.");
            // No renderer supplied, use default, which does nothing
            renderer = new ARRenderer();
        }
    }

    @Override
    public void onResume() {
        //Log.i(TAG, "onResume()");
        super.onResume();

        // Create the camera preview
        preview = new CaptureCameraPreview(this, this);

        Log.e(TAG, "onResume(): CaptureCameraPreview created");

        // Create the GL view
        mOpenGlSurfaceViewInstance = new GLSurfaceView(this);

        // Check if the system supports OpenGL ES 2.0.
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo;
        configurationInfo = activityManager.getDeviceConfigurationInfo();
        boolean supportsEs2;
        assert configurationInfo != null;
        supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (supportsEs2) {
            Log.e(TAG, "onResume(): OpenGL ES 2.x is supported");

            if (renderer instanceof ARRendererGLES20) {
                // Request an OpenGL ES 2.0 compatible context.
                mOpenGlSurfaceViewInstance.setEGLContextClientVersion(2);
            } else {
                Log.e(TAG, "onResume(): OpenGL ES 2.x is supported but only a OpenGL 1.x renderer is available." +
                        " \n Use ARRendererGLES20 for ES 2.x support. \n Continuing with OpenGL 1.x.");
                mOpenGlSurfaceViewInstance.setEGLContextClientVersion(1);
            }
        } else {
            Log.e(TAG, "onResume(): Only OpenGL ES 1.x is supported");
            if (renderer instanceof ARRendererGLES20)
                throw new RuntimeException("Only OpenGL 1.x available but a OpenGL 2.x renderer was provided.");
            // This is where you could create an OpenGL ES 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2.
            mOpenGlSurfaceViewInstance.setEGLContextClientVersion(1);
        }

        mOpenGlSurfaceViewInstance.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mOpenGlSurfaceViewInstance.getHolder().setFormat(PixelFormat.TRANSLUCENT); // Needs to be a translucent surface so the camera preview shows through.
        mOpenGlSurfaceViewInstance.setRenderer(renderer);
        mOpenGlSurfaceViewInstance.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); // Only render when we have a frame (must call requestRender()).
        mOpenGlSurfaceViewInstance.setZOrderMediaOverlay(true); // Request that GL view's SurfaceView be on top of other SurfaceViews (including CameraPreview's SurfaceView).

        Log.e(TAG, "onResume(): GLSurfaceView created");

        // Add the views to the interface
        mainFrameLayout.addView(preview, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mainFrameLayout.addView(mOpenGlSurfaceViewInstance, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        Log.e(TAG, "onResume(): Views added to main layout.");

        if (mOpenGlSurfaceViewInstance != null) {
            mOpenGlSurfaceViewInstance.onResume();
        }

        //Load settings button
        View settingsButtonLayout = this.getLayoutInflater().inflate(R.layout.setting_button_layout, mainFrameLayout, false);
        mSettingButton = settingsButtonLayout.findViewById(R.id.button_settings);
        mainFrameLayout.addView(settingsButtonLayout);
        mSettingButton.setOnClickListener(this);

        //Load Options buttons
        View OptionsButtonLayout = this.getLayoutInflater().inflate(R.layout.options_buttons_layout, mainFrameLayout, false);

        mFlashButton = OptionsButtonLayout.findViewById(R.id.button_flash);
        mCaptureButton = OptionsButtonLayout.findViewById(R.id.button_capture);
        mScreenshotButton = OptionsButtonLayout.findViewById(R.id.button_screenshot);
        mainFrameLayout.addView(OptionsButtonLayout);

        mAutoFocusButton = settingsButtonLayout.findViewById(R.id.button_autofocus);
        mAutoSceneButton = settingsButtonLayout.findViewById(R.id.button_auto);
        mContinousPictureButton = settingsButtonLayout.findViewById(R.id.button_continous_scene_focus);
        mHdrButton = settingsButtonLayout.findViewById(R.id.button_hdr);
        mWhiteBalanceButton = settingsButtonLayout.findViewById(R.id.button_whitebalance);
        mSteadyShotButton = settingsButtonLayout.findViewById(R.id.button_steady_on);
        mFlashButton = settingsButtonLayout.findViewById(R.id.button_flash);

        mAutoFocusButtonArea = settingsButtonLayout.findViewById(R.id.button_autofocus_area);
        mAutoSceneButtonArea = settingsButtonLayout.findViewById(R.id.button_autoscene_area);
        mContinousPictureButtonArea = settingsButtonLayout.findViewById(R.id.button_continous_scene_focus_area);
        mHdrButtonArea = settingsButtonLayout.findViewById(R.id.button_hdr_area);
        mWhiteBalanceButtonArea = settingsButtonLayout.findViewById(R.id.button_whitebalance_area);
        mSteadyShotButtonArea = settingsButtonLayout.findViewById(R.id.button_steady_on_area);
        mFlashArea = settingsButtonLayout.findViewById(R.id.flash_area);

        mFlashButton.setOnClickListener(this);
        mCaptureButton.setOnClickListener(this);
        mScreenshotButton.setOnClickListener(this);

        mHdrButton.setOnClickListener(this);
        mAutoSceneButton.setOnClickListener(this);
        mWhiteBalanceButton.setOnClickListener(this);
        mContinousPictureButton.setOnClickListener(this);
        mAutoFocusButton.setOnClickListener(this);
        mSteadyShotButton.setOnClickListener(this);

        mHdrButtonArea.setOnClickListener(this);
        mAutoSceneButtonArea.setOnClickListener(this);
        mWhiteBalanceButtonArea.setOnClickListener(this);
        mContinousPictureButtonArea.setOnClickListener(this);
        mAutoFocusButtonArea.setOnClickListener(this);
        mSteadyShotButtonArea.setOnClickListener(this);
        mFlashArea.setOnClickListener(this);

        mHdrButtonArea.setVisibility(View.GONE);
        mAutoSceneButtonArea.setVisibility(View.GONE);
        mWhiteBalanceButtonArea.setVisibility(View.GONE);
        mContinousPictureButtonArea.setVisibility(View.GONE);
        mAutoFocusButtonArea.setVisibility(View.GONE);
        mSteadyShotButtonArea.setVisibility(View.GONE);
        mFlashArea.setVisibility(View.GONE);

//        mFlashButtonArea = OptionsButtonLayout.findViewById(R.id.button_flash_area);
//        if (!getBaseContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
//            mFlashButtonArea.setVisibility(View.GONE);
//        }
    }

    @Override
    protected void onPause() {
        //Log.i(TAG, "onPause()");
        super.onPause();

        if (mOpenGlSurfaceViewInstance != null) {
            mOpenGlSurfaceViewInstance.onPause();
        }

        // System hardware must be released in onPause(), so it's available to
        // any incoming activity. Removing the CameraPreview will do this for the
        // camera. Also do it for the GLSurfaceView, since it serves no purpose
        // with the camera preview gone.
        mainFrameLayout.removeView(mOpenGlSurfaceViewInstance);
        mainFrameLayout.removeView(preview);
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop(): Activity stopping.");

        super.onStop();
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mSettingButton)) {

            // Toast.makeText(this, "Options are used for development purpose. \n Your current options are \n Resolution : 1280x720 \n Aspect Ratio : 16:9 ", Toast.LENGTH_SHORT).show();

//            v.getContext().startActivity(new Intent(v.getContext(), CameraPreferencesActivity.class));
            if (cameraoptions_visibility) {
                mHdrButtonArea.setVisibility(View.GONE);
                mAutoSceneButtonArea.setVisibility(View.GONE);
                mWhiteBalanceButtonArea.setVisibility(View.GONE);
                mContinousPictureButtonArea.setVisibility(View.GONE);
                mAutoFocusButtonArea.setVisibility(View.GONE);
                mSteadyShotButtonArea.setVisibility(View.GONE);
                mFlashArea.setVisibility(View.GONE);
                cameraoptions_visibility = false;
            } else {
                mHdrButtonArea.setVisibility(View.VISIBLE);
                mAutoSceneButtonArea.setVisibility(View.VISIBLE);
                mWhiteBalanceButtonArea.setVisibility(View.VISIBLE);
                mContinousPictureButtonArea.setVisibility(View.VISIBLE);
                mAutoFocusButtonArea.setVisibility(View.VISIBLE);
                mSteadyShotButtonArea.setVisibility(View.VISIBLE);
                mFlashArea.setVisibility(View.VISIBLE);
                cameraoptions_visibility = true;
            }


        }
        if (v.equals(mFlashButton)) {
            CameraFlash();
        }

        if (v.equals(mCaptureButton)) {
            CameraImage();
        }

        if (v.equals(mScreenshotButton)) {
            renderer.printOptionEnable = true;
        }
        if (v.equals(mAutoFocusButton)) {
            if (autofocus_toggle) {
                EnvconstantsAR.AUTOFOCUS = false;
                autofocus_toggle = false;
                mAutoFocusButton.setImageResource(R.mipmap.ic_auto_focus);
            } else {
                EnvconstantsAR.AUTOFOCUS = true;
                autofocus_toggle = true;
                mAutoFocusButton.setImageResource(R.mipmap.ic_autofocus_off);

            }
        }

        if (v.equals(mAutoSceneButton)) {

            if (autoscene_toggle) {
                EnvconstantsAR.AUTOSCENE = false;
                autoscene_toggle = false;
                mAutoSceneButton.setImageResource(R.mipmap.ic_autooff);
            } else {
                EnvconstantsAR.AUTOSCENE = true;
                autoscene_toggle = true;
                mAutoSceneButton.setImageResource(R.mipmap.ic_autoscene);
            }

        }
        if (v.equals(mSteadyShotButton)) {
            if (steady_toggle) {
                EnvconstantsAR.STEADYSHOT = false;
                steady_toggle = false;
                mSteadyShotButton.setImageResource(R.mipmap.ic_steadyoff);
            } else {
                EnvconstantsAR.STEADYSHOT = true;
                steady_toggle = true;
                mSteadyShotButton.setImageResource(R.mipmap.ic_steadyon);
            }

        }
        if (v.equals(mContinousPictureButton)) {
            if (continouspicture_toggle) {
                EnvconstantsAR.CONTINOUSPICTURE = false;
                continouspicture_toggle = false;
                mContinousPictureButton.setImageResource(R.mipmap.ic_focus_continous_picture);
            } else {
                EnvconstantsAR.CONTINOUSPICTURE = true;
                continouspicture_toggle = true;
                mContinousPictureButton.setImageResource(R.mipmap.ic_continouspicture_off);
            }

        }
        if (v.equals(mHdrButton)) {
            if (hdr_toggle) {
                EnvconstantsAR.HDR = false;
                hdr_toggle = false;
                mHdrButton.setImageResource(R.mipmap.ic_hdron);
            } else {
                EnvconstantsAR.HDR = true;
                hdr_toggle = true;
                mHdrButton.setImageResource(R.mipmap.ic_hdroff);
            }


        }
        if (v.equals(mWhiteBalanceButton)) {
            if (whitebalance_toggle) {
                EnvconstantsAR.WHITEBALANCE = false;
                whitebalance_toggle = false;
                mWhiteBalanceButton.setImageResource(R.mipmap.ic_autowhitebalance);
            } else {
                EnvconstantsAR.WHITEBALANCE = true;
                whitebalance_toggle = true;
                mWhiteBalanceButton.setImageResource(R.mipmap.ic_autobalanceoff);
            }
        }
    }

    private void CameraFlash() {

        if (preview.camera != null) {
            try {
                Camera.Parameters param = preview.camera.getParameters();
                param.setFlashMode(!flashmode ? Camera.Parameters.FLASH_MODE_TORCH
                        : Camera.Parameters.FLASH_MODE_OFF);
                preview.camera.setParameters(param);
                flashmode = !flashmode;
                Toast toast = Toast.makeText(this, "Flash Activated", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                toast.show();

            } catch (Exception ignored) {
            }
        }
    }

    private void CameraImage() {

        preview.camera.takePicture(null, null, new Camera.PictureCallback() {

            private File imageFile;

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                try {

                    String cPath = Environment.getExternalStorageDirectory() + "/L_CATALOG/Screenshots";

                    // convert byte array into bitmap
                    Bitmap loadedImage = null;
                    Bitmap rotatedBitmap = null;
                    loadedImage = BitmapFactory.decodeByteArray(data, 0, data.length);

                    // rotate Image
                    Matrix rotateMatrix = new Matrix();
                    rotateMatrix.postRotate(preview.rotation);
                    rotatedBitmap = Bitmap.createBitmap(loadedImage, 0, 0, loadedImage.getWidth(), loadedImage.getHeight(), rotateMatrix, false);

                    File folder;
                    if (Environment.getExternalStorageState().contains(Environment.MEDIA_MOUNTED)) {
                        folder = new File(cPath);
                    } else {
                        folder = new File(cPath);
                    }
                    boolean success = true;
                    if (!folder.exists()) {
                        success = folder.mkdirs();
                    }
                    if (success) {
                        Date now = new Date();
                        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);
                        String ImageFileName = folder.getAbsolutePath() + File.separator + now + ".jpg";
                        Log.i(TAG, "ScreenShotFileName : " + ImageFileName);
                        imageFile = new File(ImageFileName);
                        imageFile.createNewFile();

                        Toast toast;
                        toast = Toast.makeText(mContext, "Image Saved to your gallery", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                        toast.show();
                    } else {

                        Toast toast;
                        toast = Toast.makeText(mContext, "Image Not Captured and Saved", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                        toast.show();

                        return;
                    }
                    ByteArrayOutputStream ostream = new ByteArrayOutputStream();

                    // save image into gallery
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);

                    FileOutputStream fout = new FileOutputStream(imageFile);
                    fout.write(ostream.toByteArray());
                    fout.close();
                    ContentValues values = new ContentValues();

                    values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.MediaColumns.DATA, imageFile.getAbsolutePath());

                    mContext.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                camera.startPreview();
            }
        });

    }

    /**
     * Returns the camera preview that is providing the video frames.
     *
     * @return The camera preview that is providing the video frames.
     */
    @SuppressWarnings("unused")
    public CaptureCameraPreview getCameraPreview() {
        return preview;
    }

    /**
     * Returns the GL surface view.
     *
     * @return The GL surface view.
     */
    @SuppressWarnings("unused")
    public GLSurfaceView getGLView() {
        return mOpenGlSurfaceViewInstance;
    }

    @Override
    public void cameraPreviewStarted(int width, int height, int rate, int cameraIndex, boolean cameraIsFrontFacing) {

        if (ARToolKit.getInstance().initialiseAR(width, height, "/storage/emulated/0/L_CATALOG/cache/Data/camera_para.dat", cameraIndex, cameraIsFrontFacing)) {
            // Expects Data to be already in the cache dir. This can be done with the AssetUnpacker.

            progressDialog.setMessage("Another couple of Minutes");
            startTimer(130000);

            Log.e(TAG, "getGLView(): Camera initialised");
        } else {
            // Error
            Log.e(TAG, "getGLView(): Error initialising camera. Cannot continue.");
            finish();
        }

        Log.e(TAG, "Camera settings: " + width + "x" + height + "@" + rate + "fps");

        firstUpdate = true;
    }

    public void startTimer(final long finish) {
        new CountDownTimer(finish, 1000) {

            public void onTick(long millisUntilFinished) {
                long remainedSecs = millisUntilFinished / 1000;
                progressDialog.setMessage("Estimated Time Left :    " + (remainedSecs / 60) + " Minutes  : " + (remainedSecs % 60) + " Seconds");
            }

            public void onFinish() {
                progressDialog.setMessage("DONE, Opening Camera");
                cancel();
            }
        }.start();
    }

    @Override
    public void cameraPreviewFrame(byte[] frame) {

        if (firstUpdate) {
            // ARToolKit has been initialised. The renderer can now add markers, etc...
            if (renderer.configureARScene()) {

                Log.e(TAG, "cameraPreviewFrame(): Scene configured successfully");

                new android.os.Handler().postDelayed(new Runnable() {
                    public void run() {

                        progressDialog.dismiss();
                    }
                }, 130000);

            } else {
                // Error
                Log.e(TAG, "cameraPreviewFrame(): Error configuring scene. Cannot continue.");
                finish();
            }
            firstUpdate = false;
        }

        if (ARToolKit.getInstance().convertAndDetect(frame)) {

            // Update the renderer as the frame has changed
            if (mOpenGlSurfaceViewInstance != null)
                mOpenGlSurfaceViewInstance.requestRender();

            onFrameProcessed();
        }
    }

    public void onFrameProcessed() {
    }

    @Override
    public void cameraPreviewStopped() {
        ARToolKit.getInstance().cleanup();
    }

    @SuppressWarnings("unused")
    protected void showInfo() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setMessage("ARToolKit Version: " + NativeInterface.arwGetARToolKitVersion());

        dialogBuilder.setCancelable(false);
        dialogBuilder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        AlertDialog alert = dialogBuilder.create();
        alert.setTitle("ARToolKit");
        alert.show();
    }

    public void notifyFinish(String errorMessage) {
        new AlertDialog.Builder(this)
                .setMessage(errorMessage)
                .setTitle("Error")
                .setCancelable(true)
                .setNeutralButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                finish();
                            }
                        })
                .show();
    }
}
