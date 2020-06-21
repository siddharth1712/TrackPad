package com.example.trackpad;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    TextView T;
    EditText E1, E2;
    float x = 0f;
    float y = 0f;
    int flag=0;
    private static final int SELECT_DEVICE_REQUEST_CODE = 42;
    private CompanionDeviceManager deviceManager;
    private AssociationRequest pairingRequest;
    private BluetoothDeviceFilter deviceFilter;
    BluetoothAdapter bluetoothAdapter;
    int width = Resources.getSystem().getDisplayMetrics().widthPixels;
    int height = Resources.getSystem().getDisplayMetrics().heightPixels;
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        E1 = (EditText) findViewById(R.id.editText);
        E2 = (EditText) findViewById(R.id.editText2);
        T = (TextView) findViewById(R.id.textview);
        Log.e(width+"x"+height,"Hi");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!(bluetoothAdapter.isEnabled())) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        deviceManager = getSystemService(CompanionDeviceManager.class);
        deviceFilter = new BluetoothDeviceFilter.Builder().build();
        pairingRequest = new AssociationRequest.Builder()
                .addDeviceFilter(deviceFilter)
                .setSingleDevice(false)
                .build();
        deviceManager.associate(pairingRequest,
                new CompanionDeviceManager.Callback() {
                    @Override
                    public void onDeviceFound(IntentSender chooserLauncher) {
                        try {
                            startIntentSenderForResult(chooserLauncher,
                                    SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(CharSequence error) {

                    }
                },
                null);
            T.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    final float x = event.getX();
                    final float y = event.getY();
                    float lastXAxis = x;
                    float lastYAxis = y;
                    float x2 = (float) Math.round(lastXAxis * 100) / 100;
                    float y2 = (float) Math.round(lastYAxis * 100) / 100;
                    if(flag==1)
                        write(dataf(x2,y2));
                    E1.setText(Float.toString(lastXAxis));
                    E2.setText(Float.toString(lastYAxis));
                    return true;
                }
            });

    }
    public byte[] dataf(float x,float y)
    {
        int f1=0;
        int f2=0;
        if(x<0){
            x=-x;
            f1=1;
        }
        if(y<0){
            y=-y;
            f2=1;
        }
        int dX= (int) ((x-(int)x)*100);
        int dY=(int)((y-(int)y)*100);
        int mX=(int)x;
        int mY=(int)y;
        int f2x=mX/100;
        int f2y=mY/100;
        int l2x=mX-f2x*100;
        int l2y=mY-f2y*100;
        byte[] position=new byte[6];
        if(f1==1){
            dX=-dX;
            f2x=-f2x;
            l2x=-l2x;
        }
        if(f2==1){
            dY=-dY;
            f2y=-f2y;
            l2y=-l2y;
        }
        position[0]=(byte)f2x;
        position[1]=(byte)l2x;
        position[2]=(byte)dX;
        position[3]=(byte)f2y;
        position[4]=(byte)l2y;
        position[5]=(byte)dY;
        return(position);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_DEVICE_REQUEST_CODE &&
                resultCode == Activity.RESULT_OK) {
            // User has chosen to pair with the Bluetooth device.
            final BluetoothDevice deviceToPair =
                    data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
            deviceToPair.createBond();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ConnectThread(deviceToPair);

                }
            },20000);
        }
    }
    String TAG = "MainActivity";
    BluetoothSocket mmSocket;
    public void ConnectThread(BluetoothDevice device) {
        final BluetoothDevice mmDevice;
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        mmDevice = device;
        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            UUID mUUID = new UUID(0x1112, 0x0000);
            tmp = device.createRfcommSocketToServiceRecord(mUUID);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        mmSocket = tmp;
        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();
            System.out.println("Hello");
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            Log.e("", "Couldn't establish Bluetooth connection!");
            return;
        }
        ConnectedThread(mmSocket);
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            mmSocket.close();
            mmInStream.close();
            mmOutStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }

    InputStream mmInStream =null;
    OutputStream mmOutStream=null;
    String TAG2 = "MY_APP_DEBUG_TAG";
    public void ConnectedThread(BluetoothSocket socket){

        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        // Get the input and output streams; using temp objects because
        // member streams are final.
        try {
            tmpIn = socket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG2, "Error occurred when creating input stream", e);
        }
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG2, "Error occurred when creating output stream", e);
        }
        mmOutStream = tmpOut;
        mmInStream = tmpIn;
        flag=1;
    }
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
            Log.e(TAG2, "Error occurred when sending data", e);
            Bundle bundle = new Bundle();
            bundle.putString("toast",
                    "Couldn't send data to the other device");
        }
    }
}
