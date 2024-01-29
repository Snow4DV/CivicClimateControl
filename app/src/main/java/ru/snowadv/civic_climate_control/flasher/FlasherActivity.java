package ru.snowadv.civic_climate_control.flasher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;


import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;

import javax.xml.transform.Result;

import ArduinoUploader.ArduinoSketchUploader;
import ArduinoUploader.Config.Arduino;
import ArduinoUploader.Config.McuIdentifier;
import ArduinoUploader.Config.Protocol;
import ArduinoUploader.IArduinoUploaderLogger;
import CSharpStyle.IProgress;
import ru.snowadv.civic_climate_control.R;
import ru.snowadv.civic_climate_control.databinding.ActivityFlasherBinding;

public class FlasherActivity extends AppCompatActivity {

    public static final String TAG = FlasherActivity.class.getSimpleName();
    private UsbDevice currentDevice;
    private ActivityFlasherBinding binding;
    private String deviceKeyName;


    private final BroadcastReceiver mUsbNotifyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbSerialManager.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                case UsbSerialManager.ACTION_NO_USB: // NO USB CONNECTED
                case UsbSerialManager.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                case UsbSerialManager.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                case UsbSerialManager.ACTION_USB_DEVICE_NOT_WORKING:
                    Toast.makeText(context, R.string.adapter_not_connected, Toast.LENGTH_SHORT).show();
                    finish();
                    break;
            }
        }
    };

    @NonNull
    private View initViewBinding() {
        binding = ActivityFlasherBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }
    private void setUsbFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbSerialManager.ACTION_USB_PERMISSION_REQUEST);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    }


    private void setAdapterTypesDropdownListAdapter() {
        ArrayAdapter<Boards> adapter = new ArrayAdapter<>(this, R.layout.list_item, Boards.values());
        binding.adapterTypeTextView.setText(Boards.ARDUINO_UNO.toString());
        binding.adapterTypeTextView.setAdapter(adapter);
    }



    private void initFields() {
        binding.deviceName.setText(String.format("%s\nVID:%s\nPID:%s\nKey:%s", currentDevice.getProductName(),
                currentDevice.getVendorId(), currentDevice.getProductId(), currentDevice.getDeviceName()));
        deviceKeyName = currentDevice.getDeviceName();
        binding.flashLog.setMovementMethod(new ScrollingMovementMethod());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentDevice = getIntent().getParcelableExtra("adapter");
        setUsbFilter();
        View rootView = initViewBinding();
        setContentView(rootView);
        binding.buttonFlash.setOnClickListener(view -> {
            new Thread(new UploadRunnable()).start();
        });
        initFields();
        setAdapterTypesDropdownListAdapter();
    }



    @Override
    public void onPause() {
        super.onPause();
        //unregisterReceiver(mUsbNotifyReceiver);
    }

    public void uploadHex() {
        logUI("Starting the upload");
        Boards board;
        try {
            board = Boards.valueOf(binding.adapterTypeTextView.getText().toString());
        } catch (IllegalArgumentException exception) {
            logUI("Choose the board first");
            return;
        }
        logUI("Selected board: " + board);
        Arduino arduinoBoard = new Arduino(board.name, board.chipType, board.uploadBaudrate, board.uploadProtocol);
        Protocol protocol = Protocol.valueOf(arduinoBoard.getProtocol().name());
        McuIdentifier mcu = McuIdentifier.valueOf(arduinoBoard.getMcu().name());
        String preOpenRst = arduinoBoard.getPreOpenResetBehavior();
        String preOpenStr = preOpenRst;
        if (preOpenRst == null) preOpenStr = "";
        else if (preOpenStr.equalsIgnoreCase("none")) preOpenStr = "";

        String postOpenRst = arduinoBoard.getPostOpenResetBehavior();
        String postOpenStr = postOpenRst;
        if (postOpenRst == null) postOpenStr = "";
        else if (postOpenStr.equalsIgnoreCase("none")) postOpenStr = "";

        String closeRst = arduinoBoard.getCloseResetBehavior();
        String closeStr = closeRst;
        if (closeRst == null) closeStr = "";
        else if (closeStr.equalsIgnoreCase("none")) closeStr = "";

        Arduino customArduino = new Arduino("Custom", mcu, arduinoBoard.getBaudRate(), protocol);
        if (!TextUtils.isEmpty(preOpenStr))
            customArduino.setPreOpenResetBehavior(preOpenStr);
        if (!TextUtils.isEmpty(postOpenStr))
            customArduino.setPostOpenResetBehavior(postOpenStr);
        if (!TextUtils.isEmpty(closeStr))
            customArduino.setCloseResetBehavior(closeStr);
        if (protocol == Protocol.Avr109) customArduino.setSleepAfterOpen(0);
        else customArduino.setSleepAfterOpen(250);
        IArduinoUploaderLogger logger = new IArduinoUploaderLogger() {
            @Override
            public void Error(String message, Exception exception) {
                Log.e(TAG, "Error:" + message);
                logUI("Error:" + message);
            }

            @Override
            public void Warn(String message) {
                Log.w(TAG, "Warn:" + message);
                logUI("Warn:" + message);
            }

            @Override
            public void Info(String message) {
                Log.i(TAG, "Info:" + message);
                logUI("Info:" + message);
            }

            @Override
            public void Debug(String message) {
                Log.d(TAG, "Debug:" + message);
                logUI("Debug:" + message);
            }

            @Override
            public void Trace(String message) {
                Log.d(TAG, "Trace:" + message);
                logUI("Trace:" + message);
            }
        };

        IProgress<Double> progress = value -> {
            String result = String.format("Upload progress: %1$,3.2f%%", value * 100);
            Log.d(TAG, result);
            logUI(result);

        };

        try {
            final BufferedInputStream in = new BufferedInputStream(new URL(getFirmwareDownloadLink(board)).openStream());
            logUI(String.format("Downloading firmware {%s}", getFirmwareDownloadLink(board)));
            Reader reader = new InputStreamReader(in);
            Collection<String> hexFileContents = new LineReader(reader).readLines();
            double size = String.join("", hexFileContents).getBytes().length / 1024.0;
            logUI(String.format("Finished downloading firmware {%f Kb}", size));
            ArduinoSketchUploader<SerialPortStreamImpl> uploader = new ArduinoSketchUploader<SerialPortStreamImpl>(
                    this, SerialPortStreamImpl.class, null, logger, progress);
            uploader.UploadSketch(hexFileContents, customArduino, deviceKeyName);
        } catch(FileNotFoundException ex) {
            ex.printStackTrace();
            logUI("ERROR: " + getString(R.string.file_not_found_exception));
        } catch(UnknownHostException exception) {
            exception.printStackTrace();
            logUI("ERROR: " + getString(R.string.no_internet_exception));
        } catch (Exception ex) {
            ex.printStackTrace();
            logUI("ERROR: " + ex);
        }

    }

    private void logUI(String text) {
        runOnUiThread(() -> binding.flashLog.append(text + "\n"));
    }

    private class UploadRunnable implements Runnable {
        @Override
        public void run() {
            uploadHex();
        }
    }


    private String getFirmwareDownloadLink(Boards board) {
        boolean isMaster = binding.masterCheckbox.isChecked();
        boolean isPreRelease = binding.preReleaseCheckbox.isChecked();
        String tag = (isPreRelease) ? getLatestPreReleaseTag("Snow4DV", "civic-adapter-platformio")
                : "latest";
        return String.format(isPreRelease ? "https://github.com/Snow4DV/civic-adapter-platformio/releases/download/%s/%s-%s.hex" : "https://github.com/Snow4DV/civic-adapter-platformio/releases/" +
                "%s/download/%s-%s.hex", tag, isMaster ? "MASTER" : "SLAVE", board.toString());
    }

    public static String getLatestPreReleaseTag(String owner, String repo)  {
        try {
            String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/releases";

            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                Gson gson = new Gson();
                GitHubRelease[] releases = gson.fromJson(response.toString(), GitHubRelease[].class);

                for (GitHubRelease release : releases) {
                    if (release.prerelease) {
                        return release.tag_name;
                    }
                }

                System.out.println("No pre-release tag found.");
                return null;
            } else {
                System.out.println("Failed to fetch GitHub releases. Status code: " + responseCode);
                return null;
            }
        } catch(IOException exception) {
            return null;
        }
    }


    class GitHubRelease {
        String tag_name;
        boolean prerelease;
    }


}