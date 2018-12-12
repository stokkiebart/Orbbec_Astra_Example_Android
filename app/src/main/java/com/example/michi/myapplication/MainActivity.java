package com.example.michi.myapplication;

import android.content.Intent;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.orbbec.astra.Astra;
import com.orbbec.astra.BodyStream;
import com.orbbec.astra.DepthStream;
import com.orbbec.astra.ImageStream;
import com.orbbec.astra.ImageStreamMode;
import com.orbbec.astra.PointFrame;
import com.orbbec.astra.PointStream;
import com.orbbec.astra.ReaderFrame;
import com.orbbec.astra.StreamReader;
import com.orbbec.astra.StreamSet;
import com.orbbec.astra.Vector3D;
import com.orbbec.astra.android.AstraAndroidContext;
import com.orbbec.astra.android.AstraDeviceManager;
import com.orbbec.astra.android.AstraDeviceMonitorActivity;
import com.orbbec.astra.android.unity3d.AstraUnityPlayerActivity;

import java.nio.ByteBuffer;
import java.sql.Time;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private Executor ex;
    private final String device_URI = "D16111910441";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());

        // Executor class
        ex = new Executor(){
            @Override
            public void execute(@NonNull Runnable r) {
                new Thread (r).start();
            }
        };
        // Execute the Runnable object
        ex.execute(new UpdateRunnable());
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    /**
     * "Main" Runnable code (to be run inside of Astra initialized Thread)
     */
    private class UpdateRunnable implements Runnable {
        boolean frameFinished = false;
        ArrayList<Vector3D> vector3DList = new ArrayList<>();

        @Override
        public void run() {
            // Astra.initialize
            final AstraAndroidContext aac = new AstraAndroidContext(getApplicationContext());


            try {
                aac.initialize();
                aac.openAllDevices();

                TimeUnit.SECONDS.sleep(5);
                Log.e("STREAM", "open stream");
                StreamSet streamSet = StreamSet.open();
                StreamReader reader = streamSet.createReader();

                reader.addFrameListener(new StreamReader.FrameListener() {

                    public void onFrameReady(StreamReader reader, ReaderFrame frame) {
                        PointFrame df = PointFrame.get(frame);
                        ByteBuffer buffer = df.getByteBuffer();
                        if (df.isValid()) {
                            Log.e("ASTRA", "frame is valid");

                            while (buffer.hasRemaining()) {
                                vector3DList.add(new Vector3D(
                                        buffer.getFloat(),
                                        buffer.getFloat(),
                                        buffer.getFloat() * -1));
                            }
                            frameFinished = true;
                        }
                    }
                });

                PointStream pointStream = PointStream.get(reader);
                pointStream.start();

                while (!frameFinished) {
                    Astra.update();
                    TimeUnit.MILLISECONDS.sleep(100);
                }

            } catch (Throwable e) {
                Log.e("ASTRA", e.toString());
            } finally {
                aac.terminate();
            }

            Log.e("DATAAAA", "x: " + this.vector3DList.get(200).getX());
            Log.e("DATAAAA", "y: " + this.vector3DList.get(200).getY());
            Log.e("DATAAAA", "z: " + this.vector3DList.get(200).getZ());
            Log.e("DATAAAA", "size of list: " + this.vector3DList.size());
        }
    }
}