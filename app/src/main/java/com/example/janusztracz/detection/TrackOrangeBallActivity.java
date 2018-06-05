package com.example.janusztracz.detection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TrackOrangeBallActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "ORAGNE_BALL";
    private CameraBridgeViewBase mOpenCvCameraView;
    private Scalar min_values = new Scalar(13, 138, 168);
    private Scalar max_values = new Scalar(255, 255, 255);
    public Rect rectangle = new Rect(new Point(0, 480), new Point(1280, 240));
    public Mat mMatRoi_rgb = new Mat();
    public Mat mRgba = new Mat();
    public Mat mMatRoi;


    boolean drawContours = true;
    boolean drawCoordinates = true;

    BluetoothAdapter mBluetoothAdapter;

    BluetoothDevice mDevice;
    TrackOrangeBallActivity.ConnectThread connectThread;


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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.track_orange_ball_activity);
        Log.i(TAG, "called onCreate");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.track_orange_ball_activity_camera);
        mOpenCvCameraView.setMaxFrameSize(1280, 720);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        // CZESC ODPOWIEDZIALNA ZA POLACZENIE BT
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                Log.i(TAG, "device" + device.getAddress());
                Log.i(TAG, "device" + device.getName());
                if (device.getAddress().equals("20:17:02:03:18:20")) {
                    mDevice = device;
                    Log.i(TAG, "DeviceFound");
                    break;
                }
            }
            connectThread = new TrackOrangeBallActivity.ConnectThread(mDevice);
            connectThread.start();


        }
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

    public void onDestroy() {
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        connectThread.cancel();
        super.onDestroy();


    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        // Odnośnik dla ROI - do kopiowania
        Mat submat = mRgba.submat(rectangle);
        // Utworzenie ROI do analizy
        mMatRoi = new Mat(mRgba, rectangle);
        //ROI rgb do wklejenia na matryce wyjsciową
        mMatRoi_rgb = mMatRoi.clone();
        //Konwersja do HSV
        Imgproc.cvtColor(mMatRoi, mMatRoi, Imgproc.COLOR_RGB2HSV);
        //Progowanie
        Core.inRange(mMatRoi, min_values, max_values, mMatRoi);
        //Instancja punktu
        Point point = new Point();
        //Funkcja zwracająca współrzędne do "point"
        getSingleObjectCoordinates(mMatRoi, point, drawContours, drawCoordinates);
        mMatRoi_rgb.copyTo(submat);
        //Jeśli jest połączenie bluetooth, wyślij dane
        if (connectThread != null && connectThread.isConnected()) {
            int pozycjaX = (int) point.x;
            connectThread.writeData(Integer.toString(pozycjaX));

        }
        return mRgba;
    }

    private Point getSingleObjectCoordinates(Mat matrixRoi, Point outputCoordinates, boolean drawContours, boolean drawCoordinates) {
        // Deklaracje listy konturu i hierearhii
        //TODO: Sprawdzić czy wymagane do poprawnego działania
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();

        // Znajdz kontury matrycy wejściowej
        Imgproc.findContours(matrixRoi, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        // Minimalna wartość pola
        double largestArea = 20;

        // Wartość startowa
        int largestAreaIndex = -1;

        // Sprawdz wszystkie kontury i wybierz największy
        if (contours.size() > 0) {
            for (int i = 0; i < contours.size(); i++) {
                double area = Imgproc.contourArea(contours.get(i), false);
                if (area > largestArea) {
                    largestArea = area;
                    largestAreaIndex = i;
                }
            }

            // Szukanie środka ciężkości
            List<Moments> moments = new ArrayList<Moments>(2);
            if (largestAreaIndex != -1) {
                moments.add(0, Imgproc.moments(contours.get(largestAreaIndex)));
                outputCoordinates.x = moments.get(0).get_m10() / moments.get(0).get_m00();
                outputCoordinates.y = moments.get(0).get_m01() / moments.get(0).get_m00();
            }

            // Narysuj kontury na matrycy ROI RGB
            if (drawContours) {
                Imgproc.drawContours(mMatRoi_rgb, contours, largestAreaIndex, new Scalar(255, 0, 0), 4);
                Imgproc.drawMarker(mMatRoi_rgb, outputCoordinates, new Scalar(0, 255, 0));
            }

            // Napisz PUNKT, wspolrzedne i wielkość pola dla debugowania
            if (drawCoordinates) {
                Imgproc.putText(mMatRoi_rgb, "Punkt1", outputCoordinates, 2, 1, new Scalar(0, 255, 0));
                Imgproc.putText(mMatRoi_rgb, Double.toString(outputCoordinates.x), new Point(outputCoordinates.x, outputCoordinates.y + 100), 2, 1, new Scalar(0, 255, 0));
                Imgproc.putText(mMatRoi_rgb, Double.toString(largestArea), new Point(outputCoordinates.x + 200, outputCoordinates.y), 2, 1, new Scalar(0, 255, 0));
            }
        }

        //Rysowanie kwadratu ROI
        Imgproc.rectangle(mMatRoi_rgb, new Point(rectangle.x, rectangle.y), new Point(rectangle.x + 1280, rectangle.y - 240), new Scalar(255, 0, 0), 2);

        return outputCoordinates;
    }

    // FIltrowanie obrazu - nieużywane
    private Mat morphOps(Mat treshold) {

        Mat erode = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(20, 20));
        Mat dilate = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(20, 20));
        Imgproc.erode(treshold, treshold, erode);
        Imgproc.dilate(treshold, treshold, dilate);
        return treshold;
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private OutputStream outStream;

        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createInsecureRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.i(TAG, "run: CONNECTED");
            } catch (IOException connectException) {
                Log.i(TAG, "run:  NOT CONNECTED");
            }
        }


        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
                if (outStream != null)
                    outStream.close();
                finish();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }

        //Sending Message
        public void writeData(String data) {
            String info = data;
            try {
                outStream = mmSocket.getOutputStream();
                outStream.write(info.getBytes());
                outStream.write(10);
                Log.i(TAG, "writeData: MSG SENT");
            } catch (IOException e) {
                e.printStackTrace();
                Log.i(TAG, "run: CANT SEND MSG");
            }
        }

        public boolean isConnected() {
            return mmSocket.isConnected();
        }
    }
}
