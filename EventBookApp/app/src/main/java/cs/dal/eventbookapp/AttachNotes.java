package cs.dal.eventbookapp;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import com.example.eventbookapp.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.drive.CreateFileActivityOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.metadata.CustomPropertyKey;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import DataProvider.ServiceDataProvider;

/**
 * Class Attach Notes - Code behind for Attach Notes page layout
 */
public class AttachNotes extends AppCompatActivity{


    CoordinatorLayout mainCoordinatorLayout ;
    public static  DriveClient driveClient;
    public static DriveResourceClient driveResourceClient;
    static final int CONTENT_CREATOR = 100;
    static final int AUTHORIZATION = 200;
    static final int CAPTURE_IMAGE = 300;
    static final int FILE_EXPLORER =400;
    static final String fileTitlePrefix = "EventBook";
    static final String CustomPropertyName = "EVENTID";
    public static String currentEventId ;
    Animation animationMove;
    Animation animationRotate ;
    Button savebutton ;
    TextInputEditText editText;
    TextInputLayout txtLayout;

    /**
     * Method overloaded oncreate - called on create of the activity
     * @param savedInstanceState - Previous saved instance passed by the Android System
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attach_notes);
        mainCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        if(null==currentEventId)
        currentEventId = getIntent().getStringExtra("contextEvent");

        //Connect To Google Drive and API
        AuthorizeGoogleSignIn();
        animationMove = AnimationUtils.loadAnimation(this,R.anim.buttonmoveright);
        animationRotate = AnimationUtils.loadAnimation(this,R.anim.buttonrotate);
        FloatingActionButton addImageFab = (FloatingActionButton) findViewById(R.id.AddImage);
        addImageFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StartCameraAndGetImage();
            }
        });
         findViewById(R.id.txtAttach).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StartCameraAndGetImage();
            }
        });

        FloatingActionButton addFiles = (FloatingActionButton) findViewById(R.id.AddFiles);
        addFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ExploreAndPickFiles();
            }
        });

        findViewById(R.id.txtInputFile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ExploreAndPickFiles();
            }
        });

        findViewById(R.id.btnPinned).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setAnimation(animationRotate);
                Intent fileAttachmentIntent = new Intent(AttachNotes.this,AttachedFiles.class);
                startActivity(fileAttachmentIntent);
            }
        });


        savebutton = (Button) findViewById(R.id.save);
        editText = (TextInputEditText) findViewById(R.id.txtInput);
        txtLayout  = (TextInputLayout) findViewById(R.id.txtInputLay);
        savebutton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                savebutton.setEnabled(false);
                String content = editText.getText().toString();
                SaveAsTextFile(content);
            }
        });
    }

    /**
     * Method On Activity result - Callback to retrieve the result from the previously called activity
     * @param requestCode - Request code to uniquely idenfity the requested callback from previous activity
     * @param resultCode - Status of the previous activity
     * @param data - Data result from the previous called activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode== CAPTURE_IMAGE)
        {
            if(Activity.RESULT_OK == resultCode)
            {
                Bitmap capturedBitMap =(Bitmap) data.getExtras().get("data");
                UploadImageToGoogleDrive(capturedBitMap);
            }

        }else if(requestCode == AUTHORIZATION)
        {
            if(RESULT_OK==resultCode)
            {
               ConnectToGoogleDrive();
            }

        }else if(requestCode == CONTENT_CREATOR)
        {
            Snackbar.make(mainCoordinatorLayout,"Image Upload Successfully",Snackbar.LENGTH_SHORT).show();
        }
        else if(requestCode==FILE_EXPLORER)
        {
            if(RESULT_OK==resultCode)
            {
                UploadFileToGoogleDrive(data.getData());
            }
        }
    }

    /**
     * Method StartCameraAndGetImage to start ACTION Image Capture Intent Message Activty
     */
    public void StartCameraAndGetImage()
    {
        try{
            Intent callCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(callCamera,CAPTURE_IMAGE);
        }
        catch(Exception ex)
        {
            Snackbar.make(mainCoordinatorLayout,"Camera permission denied.",Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * Method ExploreAndPickFiles to start ACTION GET CONTENT Activty
     */
    public void ExploreAndPickFiles()
    {
        try{
            Intent fileExplorerIntent = new Intent(Intent.ACTION_GET_CONTENT);
            fileExplorerIntent.setType("*/*");
            fileExplorerIntent.putExtra("CONTENT_TYPE", "text/plain");
            startActivityForResult(fileExplorerIntent,FILE_EXPLORER);

        }catch (Exception ex)
        {
            Snackbar.make(mainCoordinatorLayout,"No Suitable File Explorer found.",Snackbar.LENGTH_SHORT).show();
        }

    }

    /**
     * Method UploadFileToGoogleDrive - To Upload the supplied file uri to the cloud
     * @param newFileToUpload - Uri of the file to upload
     */
    private void UploadFileToGoogleDrive(Uri newFileToUpload)
    {
        final Uri fileUri = newFileToUpload;
        driveResourceClient
                .createContents()
                .continueWithTask(
                        new Continuation<DriveContents, Task<Void>>() {
                            @Override
                            public Task<Void> then(@NonNull Task<DriveContents> task) throws Exception {
                                return createFileIntentSender(task.getResult(), fileUri);
                            }
                        });
    }

    /**
     * Method CreateFieIntentSender - Called as a background activity to create files
     * @param driveContents  - Google Drive File Contents
     * @param fileUri -  File UI to Create
     * @return -  Create File Task
     */
    private Task<Void> createFileIntentSender(DriveContents driveContents, Uri fileUri) {

        File file = new File( fileUri.getPath());
        // Get an output stream for the contents and  Write the bitmap data from it.
        OutputStream outputStream = driveContents.getOutputStream();

        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);

            if (inputStream != null) {
                byte[] data = new byte[1024];
                while (inputStream.read(data) != -1) {
                    outputStream.write(data);
                }
                inputStream.close();
            }
        } catch (Exception e) {
        }

        CustomPropertyKey eventID =
                new CustomPropertyKey(CustomPropertyName, CustomPropertyKey.PUBLIC);
        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder().setMimeType("text/plain")
                .setTitle( fileTitlePrefix+ getCurrentTimeStamp()+".txt").setCustomProperty(eventID,currentEventId)
                .build();
        CreateFileActivityOptions createFileActivityOptions =
                new CreateFileActivityOptions.Builder()
                        .setInitialMetadata(metadataChangeSet)
                        .setInitialDriveContents(driveContents)
                        .build();

        return driveClient
                .newCreateFileActivityIntentSender(createFileActivityOptions)
                .continueWith(
                        new Continuation<IntentSender, Void>() {
                            @Override
                            public Void then(@NonNull Task<IntentSender> task) throws Exception {
                                startIntentSenderForResult(task.getResult(), CONTENT_CREATOR, null, 0, 0, 0);
                                return null;
                            }
                        });
    }

    /**
     * Method Upload Image to Google Drive using streamed bitmap to drive resource client
     * @param bitmapToUpload - Input bitmap image to upload
     */
    private void UploadImageToGoogleDrive(Bitmap bitmapToUpload)
    {
        final Bitmap image = bitmapToUpload;
        driveResourceClient
                .createContents()
                .continueWithTask(
                        new Continuation<DriveContents, Task<Void>>() {
                            @Override
                            public Task<Void> then(@NonNull Task<DriveContents> task) throws Exception {
                                return createImageIntentSender(task.getResult(), image);
                            }
                        });
    }

    /**
     * Method createImageIntentSender - called by background thread to create image resource in the Google Drive
     * @param driveContents  - Drive Content of the google Drive account
     * @param image  - Bitma image to upload
     * @return- Image to upload
     */
    private Task<Void> createImageIntentSender(DriveContents driveContents, Bitmap image) {
        // Get an output stream for the contents and  Write the bitmap data from it.
        OutputStream outputStream = driveContents.getOutputStream();
        ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);

        try {
            outputStream.write(bitmapStream.toByteArray());
        } catch (IOException e) {
        }

        CustomPropertyKey eventID =
                new CustomPropertyKey(CustomPropertyName, CustomPropertyKey.PUBLIC);

        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder().setMimeType("image/jpeg")
                        .setTitle( fileTitlePrefix+ getCurrentTimeStamp()+".png").setCustomProperty(eventID,currentEventId)
                        .build();
        CreateFileActivityOptions createFileActivityOptions =
                new CreateFileActivityOptions.Builder()
                        .setInitialMetadata(metadataChangeSet)
                        .setInitialDriveContents(driveContents)
                        .build();

        return driveClient
                .newCreateFileActivityIntentSender(createFileActivityOptions)
                .continueWith(
                        new Continuation<IntentSender, Void>() {
                            @Override
                            public Void then(@NonNull Task<IntentSender> task) throws Exception {
                                startIntentSenderForResult(task.getResult(), CONTENT_CREATOR, null, 0, 0, 0);
                                return null;
                            }
                        });
    }

    /**
     * Method GetCurrentTimeStamp - Get the current datetime in filename format
     * @return - String timestamp
     */
    private String getCurrentTimeStamp()
    {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    }

    /**
     * Method AUthorizeGoogleSignIn - Called after to check whether the user google sigin is required
     */
    private void AuthorizeGoogleSignIn()
    {
        if(ServiceDataProvider.getGlobalInstance().requiresGoogleAuthorization()) {
            GoogleSignInClient client = ServiceDataProvider.getGlobalInstance().getGoogleSignInClient(AttachNotes.this);
            startActivityForResult(client.getSignInIntent(), AUTHORIZATION);
        }else{
            ConnectToGoogleDrive();
        }

    }

    /**
     * Method ConnectToGoogleDrive - Called to connect to the Google drive instance
     */
    private void ConnectToGoogleDrive()
    {

        if(GoogleSignIn.getLastSignedInAccount(this)!=null) {
            driveClient = Drive.getDriveClient(this,GoogleSignIn.getLastSignedInAccount(this));
            driveResourceClient = Drive.getDriveResourceClient(this,GoogleSignIn.getLastSignedInAccount(this));
            Snackbar.make(mainCoordinatorLayout,"You are now connected to Google Drive",Snackbar.LENGTH_LONG).show();
        }else
        {
            ServiceDataProvider.getGlobalInstance().setGoogleSignInClient(null);
            AuthorizeGoogleSignIn();
        }

    }

    /**
     * Method SaveAsTextFile - Called to Convert  the input string from text field to Text File
     * @param textToWrite - Input text string to save as a content of the text file
     */
    private void SaveAsTextFile(final String textToWrite)
    {
        AsyncTask<Void,Void,Void> textSaveTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                final Task<DriveFolder> rootFolder = driveResourceClient.getRootFolder();
                final Task<DriveContents> contentsTask = driveResourceClient.createContents();

                Tasks.whenAll(rootFolder,contentsTask).continueWith(new Continuation<Void,Task<DriveFile>>() {
                    @Override
                    public Task<DriveFile> then(@NonNull Task<Void> task) throws Exception {

                        DriveFolder parentFolder = rootFolder.getResult();
                        DriveContents contents = contentsTask.getResult();
                        OutputStream outStream = contents.getOutputStream();
                        try{
                            OutputStreamWriter writer = new OutputStreamWriter(outStream);
                            writer.write(textToWrite);
                            writer.close();
                        }catch (Exception io) {}

                        CustomPropertyKey eventID =
                                new CustomPropertyKey(CustomPropertyName, CustomPropertyKey.PUBLIC);

                        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder().setMimeType("text/plain")
                                .setTitle( fileTitlePrefix+ getCurrentTimeStamp()+".txt").setCustomProperty(eventID,currentEventId)
                                .build();

                        return driveResourceClient.createFile(parentFolder,metadataChangeSet,contents).addOnCompleteListener(new OnCompleteListener<DriveFile>() {
                            @Override
                            public void onComplete(@NonNull Task<DriveFile> task) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Snackbar.make(mainCoordinatorLayout,"New File Created on Drive",Snackbar.LENGTH_SHORT).show();
                                        editText.setText("");
                                        savebutton.setEnabled(true);
                                        savebutton.requestFocus();
                                    }
                                });
                            }
                        });
                    }
                });

                return null;
            }
        };

        runAsyncTask(textSaveTask);

    }

    /**
     * Method runAsynctask -  Called to run the supplied async task in the task executor
     * @param task - Task that needs to be executed
     * @return
     */
    private AsyncTask<Void, Void, Void> runAsyncTask(AsyncTask<Void, Void, Void> task) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            return task.execute();
        }
    }


}
