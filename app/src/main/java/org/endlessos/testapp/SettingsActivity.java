package org.endlessos.testapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class SettingsActivity extends Activity {
    // TODO: Restart the Kolibri service if it is running?
    // TODO: Display helpful information after choosing a collection, such as its name from manifest.json

    private static final String TAG = Constants.TAG;

    private static final int CHOOSE_FILE_RESULT_CODE = 8778;

    Button chooseContentButton;
    Button importContentButton;
    ProgressBar importContentProgress;
    TextView outputTextView;

    private Uri selectedContentUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getActionBar().setTitle("Endless Key Settings");

        chooseContentButton = findViewById(R.id.chooseContentButton);
        chooseContentButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        startChooseContent();
                    }
                }
        );

        importContentButton = findViewById(R.id.importContentButton);
        importContentButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        startImportContent();
                    }
                }
        );

        importContentProgress = findViewById(R.id.importContentProgress);

        outputTextView = findViewById(R.id.outputTextView);
    }

    private void startChooseContent() {
        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
        chooseFile.setType("application/zip");
        startActivityForResult(
                Intent.createChooser(chooseFile, "Choose a file"),
                CHOOSE_FILE_RESULT_CODE
        );
    }

    private void startImportContent() {
        chooseContentButton.setEnabled(false);
        importContentButton.setEnabled(false);
        importContentProgress.setIndeterminate(true);

        try {
            outputTextView.setText("Importing...");
            copyKolibriContent(
                    selectedContentUri,
                    KolibriUtils.getKolibriHome(getBaseContext())
            );
            setSelectedContentUri(null);
        } catch (IOException e) {
            outputTextView.setText("Import error:\n"+e.getStackTrace().toString());
            return;
        } finally {
            chooseContentButton.setEnabled(true);
            importContentProgress.setIndeterminate(false);
        }

        outputTextView.setText("Finished importing");
    }

    private void setSelectedContentUri(Uri value) {
        selectedContentUri = value;
        if (value != null) {
            importContentButton.setEnabled(true);
            outputTextView.setText("Ready to import '" + selectedContentUri.toString() + "'");
        } else {
            importContentButton.setEnabled(false);
            outputTextView.setText("");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != CHOOSE_FILE_RESULT_CODE) {
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        setSelectedContentUri(data.getData());
    }

    protected void copyKolibriContent(Uri sourceContent, File outputDirectory) throws IOException {
        // TODO: This needs to happen asynchronously.

        InputStream fileInput;

        fileInput = getContentResolver().openInputStream(sourceContent);
        ZipInputStream zipInput = new ZipInputStream(new BufferedInputStream(fileInput));
        ZipEntry zipEntry = null;
        byte[] buffer = new byte[1024];

        while (true) {
            String fileName;
            File outputFile;

            zipEntry = zipInput.getNextEntry();

            if (zipEntry == null) {
                break;
            }

            fileName = zipEntry.getName();

            if (!fileName.startsWith("content/")) {
                Log.d(TAG, "Content file has wrong parent directory: " + fileName);
                break;
            }

            if (fileName.equals("content/manifest.json")) {
                Long timeSeconds = System.currentTimeMillis() / 1000;
                fileName = "content/manifest."+timeSeconds+".json";
            }

            outputFile = new File(outputDirectory, fileName);

            if (zipEntry.isDirectory()) {
                try {
                    outputFile.mkdirs();
                } catch (SecurityException error) {
                    error.printStackTrace();
                }
            } else {
                int count;
                FileOutputStream fileOutput;

                fileOutput = new FileOutputStream(outputFile);

                while (true) {
                    count = zipInput.read(buffer);

                    if (count == -1) {
                        break;
                    }

                    fileOutput.write(buffer, 0, count);
                }

                fileOutput.close();
                zipInput.closeEntry();
            }
        }
    }
}