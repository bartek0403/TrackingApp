package com.example.janusztracz.detection;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // Natywne bibloteki
    static {
        System.loadLibrary("native-lib");
    }

    private static final String TAG = "MAIN";

    private JavaCameraView mOpenCvCameraView;

    private static final int VIEW_MODE_RGBA = 0;
    private static final int VIEW_MODE_HSV = 1;
    private static final int VIEW_MODE_TRESH = 2;

    private int mViewMode;

    private MenuItem mItemPreviewRGBA;
    private MenuItem mItemPreviewHSV;
    private MenuItem mItemPreviewTreshold;
    private MenuItem mItemTracking;
    private MenuItem mItemSetThreshold;
    private MenuItem mItemOrangeBall;
    private MenuItem mItemBluetooth;

    private Scalar min_values = new Scalar(13, 138, 251);
    private Scalar max_values = new Scalar(255, 255, 255);

    Mat mRgba;
    Mat mHSV;
    Mat mTresh;
    Mat mOut;
    boolean tracking = false;

    int V_MIN, V_MAX, S_MIN, S_MAX, H_MIN, H_MAX;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (JavaCameraView)
                findViewById(R.id.show_camera_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemPreviewRGBA = menu.add("Widok RGBA");
        mItemPreviewHSV = menu.add("Widok HSV");
        mItemPreviewTreshold = menu.add("Progowanie");
        mItemSetThreshold = menu.add("Ustaw prog");
        mItemOrangeBall = menu.add("Sledzenie kulki");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        if (item == mItemPreviewRGBA) {
            mViewMode = VIEW_MODE_RGBA;
        } else if (item == mItemPreviewHSV) {
            mViewMode = VIEW_MODE_HSV;
        } else if (item == mItemPreviewTreshold) {
            mViewMode = VIEW_MODE_TRESH;
        } else if (item == mItemSetThreshold) {
            Intent settings = new Intent(this, SetTresholdActivity.class);
            startActivityForResult(settings, 1);
        } else if (item == mItemTracking) {
            if (tracking) {
                tracking = false;
                Toast.makeText(MainActivity.this, "Sledzenie wylaczone", Toast.LENGTH_SHORT).show();
            } else {
                tracking = true;
                Toast.makeText(MainActivity.this, "Sledzenie wlaczone", Toast.LENGTH_SHORT).show();
            }

        } else if (item == mItemOrangeBall) {
            Intent orangeBall = new Intent(this, TrackOrangeBallActivity.class);
            startActivity(orangeBall);
        } else if (item == mItemBluetooth) {
            Intent bluetooth = new Intent(this, BluetoothActivity.class);
            startActivity(bluetooth);
        }

        return true;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mHSV = new Mat(height, width, CvType.CV_8UC4);
        mTresh = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mHSV.release();
        mTresh.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        final int viewMode = mViewMode;
        switch (viewMode) {
            case VIEW_MODE_RGBA:
                mOut = inputFrame.rgba();
                break;
            case VIEW_MODE_HSV:
                mRgba = inputFrame.rgba();
                Imgproc.cvtColor(mRgba, mOut, Imgproc.COLOR_RGB2HSV);
                break;
            case VIEW_MODE_TRESH:
                mRgba = inputFrame.rgba();
                Imgproc.cvtColor(mRgba, mHSV, Imgproc.COLOR_RGB2HSV);
                Core.inRange(mHSV, min_values, max_values, mOut);
                break;
        }
        if (tracking && viewMode == VIEW_MODE_TRESH) {
            mOut = trackObject(mOut);
            return mRgba;
        }
        return mOut;
    }


    // FIltrowanie obrazu
    private Mat morphOps(Mat treshold) {

        Mat erode = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(20, 20));
        Mat dilate = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(20, 20));
        Imgproc.erode(treshold, treshold, erode);
        return treshold;
    }

    //Sledzenie obiektu
    private Mat trackObject(Mat tresh) {
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        final int viewMode = mViewMode;
        Imgproc.findContours(tresh, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        double firstArea = 0;
        double secondArea = 0;
        int firstAreaIndex = 0;
        int secondAreaIndex = 0;
        if (contours.size() > 0) {

            for (int i = 0; i < contours.size(); i++) {
                double area = Imgproc.contourArea(contours.get(i), false);
                if (area > firstArea) {
                    firstArea = area;
                    firstAreaIndex = i;
                }
                if (area < firstArea && area > secondArea) {
                    secondArea = area;
                    secondAreaIndex = i;
                }
            }
            List<Moments> moments = new ArrayList<Moments>(2);
            moments.add(0, Imgproc.moments(contours.get(firstAreaIndex)));
            moments.add(1, Imgproc.moments(contours.get(secondAreaIndex)));
            Point punkt1 = new Point();
            Point punkt2 = new Point();
            punkt1.x = moments.get(0).get_m10() / moments.get(0).get_m00();
            punkt1.y = moments.get(0).get_m01() / moments.get(0).get_m00();
            punkt2.x = moments.get(1).get_m10() / moments.get(1).get_m00();
            punkt2.y = moments.get(1).get_m01() / moments.get(1).get_m00();

            Imgproc.drawContours(mRgba, contours, firstAreaIndex, new Scalar(255, 0, 0), 4);
            Imgproc.drawMarker(mRgba, punkt1, new Scalar(0, 255, 0));
            Imgproc.putText(mRgba, "Punkt1", punkt1, 2, 1, new Scalar(0, 255, 0));
            Imgproc.drawMarker(mRgba, punkt2, new Scalar(0, 255, 0));


        }
        return mRgba;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                min_values = (Scalar) data.getSerializableExtra(Intent.EXTRA_TEXT);
                max_values = (Scalar) data.getSerializableExtra(Intent.EXTRA_PACKAGE_NAME);
                Log.e(TAG, String.valueOf(max_values));
                Log.e(TAG, String.valueOf(min_values));
            }
        }
    }


}
