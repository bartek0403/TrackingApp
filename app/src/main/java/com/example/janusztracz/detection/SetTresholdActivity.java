package com.example.janusztracz.detection;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.Serializable;

public class SetTresholdActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private CameraBridgeViewBase mOpenCvCameraView;
    int minH_value = 0;
    int minS_value = 0;
    int minV_value = 0;
    int maxH_value = 0;
    int maxS_value = 0;
    int maxV_value = 0;

    Mat mRgba = new Mat();
    Mat mHSV = new Mat();
    Mat mOut = new Mat();

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
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

    public SetTresholdActivity() {
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.setting_activity);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.show_camera_activity_java_setting_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        final TextView show_minH = (TextView) findViewById(R.id.show_minH_value);
        final SeekBar minH = (SeekBar) findViewById(R.id.seekbar_min_H);
        minH.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                minH_value = progress;
                show_minH.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView show_minS = (TextView) findViewById(R.id.show_minS_value);
        final SeekBar minS = (SeekBar) findViewById(R.id.seekbar_min_S);
        minS.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                minS_value = progress;
                show_minS.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView show_minV = (TextView) findViewById(R.id.show_minV_value);
        final SeekBar minV = (SeekBar) findViewById(R.id.seekbar_min_V);
        minV.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                minV_value = progress;
                show_minV.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView show_maxH = (TextView) findViewById(R.id.show_maxH_value);
        final SeekBar maxH = (SeekBar) findViewById(R.id.seekbar_max_H);
        maxH.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                maxH_value = progress;
                show_maxH.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView show_maxS = (TextView) findViewById(R.id.show_maxS_value);
        final SeekBar maxS = (SeekBar) findViewById(R.id.seekbar_max_S);
        maxS.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                maxS_value = progress;
                show_maxS.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView show_maxV = (TextView) findViewById(R.id.show_maxV_value);
        final SeekBar maxV = (SeekBar) findViewById(R.id.seekbar_max_V);
        maxV.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                maxV_value = progress;
                show_maxV.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
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
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onBackPressed() {
        Scalar min_values = new Scalar(minH_value, minS_value, minV_value);
        Scalar max_values = new Scalar(maxH_value, maxS_value, maxV_value);
        Intent sendData = new Intent();
        sendData.putExtra(Intent.EXTRA_TEXT, (Serializable) min_values);
        sendData.putExtra(Intent.EXTRA_PACKAGE_NAME, (Serializable) max_values);
        if (getParent() == null) {
            setResult(Activity.RESULT_OK, sendData);
        } else {
            getParent().setResult(Activity.RESULT_OK, sendData);
        }
        finish();
        super.onBackPressed();

    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        Imgproc.cvtColor(mRgba, mHSV, Imgproc.COLOR_RGB2HSV);
        Core.inRange(mHSV, new Scalar(minH_value, minS_value, minV_value), new Scalar(maxH_value, maxS_value, maxV_value), mOut);
        return mOut;
    }
}
