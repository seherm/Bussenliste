package com.hermann.bussenliste.unitconverter.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hermann.bussenliste.repository.DataSourceFine;
import com.hermann.bussenliste.repository.DataSourcePlayer;
import com.hermann.bussenliste.unitconverter.adapters.FinesAdapter;
import com.hermann.bussenliste.R;
import com.hermann.bussenliste.domain.Fine;
import com.hermann.bussenliste.domain.Player;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class PlayerDetailsActivity extends AppCompatActivity {

    private static final int TAKE_IMAGE_REQUEST = 1;
    private static final int PICK_IMAGE_REQUEST = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 3;

    private static final String PRODUCTION_SERVER_ADDRESS = "https://bussenliste.000webhostapp.com/";

    private ArrayList<Fine> selectedItems;
    private Player selectedPlayer;
    private FinesAdapter finesAdapter;
    private DataSourcePlayer dataSourcePlayer;
    private DataSourceFine dataSourceFine;

    private TextView finesSumTextView;
    private TextView playerNameTextView;
    private ImageView playerPhotoImageView;
    private ListView finesListView;

    private String mCurrentPhotoPath;

    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_details);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFineSelectionDialog();
            }
        });

        playerPhotoImageView = findViewById(R.id.player_image);
        finesSumTextView = findViewById(R.id.fines_sum);
        playerNameTextView = findViewById(R.id.player_name);
        finesListView = findViewById(R.id.finesListView);

        dataSourcePlayer = new DataSourcePlayer(this);
        dataSourceFine = new DataSourceFine(this);

        String selectedPlayerName = getIntent().getStringExtra("SelectedPlayerName");
        selectedPlayer = dataSourcePlayer.getPlayer(selectedPlayerName);
        playerNameTextView.setText(selectedPlayer.getName());

        updateUI();

        finesAdapter = new FinesAdapter(this, selectedPlayer.getFines());
        finesListView.setAdapter(finesAdapter);
        finesListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        finesListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {
                int checkedCount = finesListView.getCheckedItemCount();
                actionMode.setTitle(Integer.toString(checkedCount));
                finesAdapter.toggleSelection(i);
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                actionMode.getMenuInflater().inflate(R.menu.delete_action_mode, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.delete_mode:
                        SparseBooleanArray selected = finesAdapter.getSelectedIds();
                        for (int i = (selected.size() - 1); i >= 0; i--) {
                            if (selected.valueAt(i)) {
                                Fine selectedItem = finesAdapter.getItem(selected.keyAt(i));
                                finesAdapter.remove(selectedItem);
                                selectedPlayer.getFines().remove(selectedItem);
                            }
                        }
                        String fineAmount = getString(R.string.fineAmount, selectedPlayer.getTotalSumOfFines());
                        finesSumTextView.setText(fineAmount);
                        try {
                            dataSourcePlayer.updatePlayer(selectedPlayer);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(getApplicationContext(), R.string.deleted_fines, Toast.LENGTH_LONG).show();
                        actionMode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_delete_player:
                deletePlayer();
                return true;
            case R.id.action_change_photo:
                showAddPhotoDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAKE_IMAGE_REQUEST: {
                if (resultCode == RESULT_OK) {
                    handleCameraPhoto();
                }
                break;
            } // ACTION_TAKE_PHOTO

            case PICK_IMAGE_REQUEST: {
                if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                    Uri uri = data.getData();

                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        int scaleFactor = 15;
                        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth()/scaleFactor, bitmap.getHeight()/scaleFactor,false);
                        setPlayerPhoto(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                } // ACTION_PICK_PHOTO
            } // switch
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showAddPhotoDialog();
                } else {
                    Toast.makeText(this, R.string.error_camera_permission, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void showAddPhotoDialog() {

        try {
            PackageManager packageManager = getPackageManager();
            int hasPerm = packageManager.checkPermission(Manifest.permission.CAMERA, getPackageName());
            if (hasPerm == PackageManager.PERMISSION_GRANTED) {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.change_photo);
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        dialog.dismiss();
                    }
                });
                if (!selectedPlayer.hasPhoto()) {
                    builder.setItems(R.array.select_photo_items_array, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {
                            switch (item) {
                                case 0:
                                    dialog.dismiss();
                                    dispatchTakePictureIntent();
                                    break;
                                case 1:
                                    dialog.dismiss();
                                    dispatchPickPictureIntent();
                            }
                        }
                    });

                    builder.show();
                } else {
                    builder.setItems(R.array.select_photo_items_array_with_delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {
                            switch (item) {
                                case 0:
                                    dialog.dismiss();
                                    dispatchTakePictureIntent();
                                    break;
                                case 1:
                                    dialog.dismiss();
                                    dispatchPickPictureIntent();
                                    break;
                                case 2:
                                    dialog.dismiss();
                                    deletePicture();
                            }
                        }
                    });
                    builder.show();
                }
            } else {
                requestPermissions();
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_camera_permission, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }


    private void showFineSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        selectedItems = new ArrayList<>();
        final List<Fine> allFines = dataSourceFine.getAllFines();
        String[] namesStringArray = new String[allFines.size()];
        for (int i = 0; i < allFines.size(); i++) {
            namesStringArray[i] = allFines.get(i).getDescription() + " (" + allFines.get(i).getAmount() + ".-)";
        }
        builder.setTitle(R.string.add_fair)

                .setMultiChoiceItems(namesStringArray, null,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    // If the user checked the item, add it to the selected items
                                    selectedItems.add(allFines.get(which));
                                } else if (selectedItems.contains(allFines.get(which))) {
                                    // Else, if the item is already in the array, remove it
                                    selectedItems.remove(allFines.get(which));
                                }
                            }
                        });

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                for (Fine fine : selectedItems) {
                    selectedPlayer.addFine(fine);
                    finesAdapter.notifyDataSetChanged();
                }
                try {
                    dataSourcePlayer.updatePlayer(selectedPlayer);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                updateUI();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()

        {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateUI() {
        String fineAmount = getString(R.string.fineAmount, selectedPlayer.getTotalSumOfFines());
        finesSumTextView.setText(fineAmount);
        Bitmap playerPhoto = selectedPlayer.getPhoto();
        if (playerPhoto != null) {
            playerPhotoImageView.setImageBitmap(playerPhoto);
        }else{
            playerPhotoImageView.setImageResource(R.drawable.player);
        }
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(PlayerDetailsActivity.this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(PlayerDetailsActivity.this,
                    Manifest.permission.CAMERA)) {
            }
            ActivityCompat.requestPermissions(PlayerDetailsActivity.this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, storageDir);
        return image;
    }

    private File setUpPhotoFile() throws IOException {

        File f = createImageFile();
        mCurrentPhotoPath = f.getAbsolutePath();

        return f;
    }

    private void setPic() {
        /* There isn't enough memory to open up more than a couple camera photos */
        /* So pre-scale the target bitmap into which the file is decoded */

        // Get the dimensions of the View
        int targetW = playerPhotoImageView.getWidth();
        int targetH = playerPhotoImageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        //int photoW = bmOptions.outWidth;
        //int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        //int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        int scaleFactor = 15;

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        /* Decode the JPEG file into a Bitmap */
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

        /* Associate the Bitmap to the Selected Player */
        setPlayerPhoto(bitmap);
    }


    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void dispatchTakePictureIntent() {
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePhotoIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = setUpPhotoFile();
            } catch (IOException ex) {
                ex.printStackTrace();
                photoFile = null;
                mCurrentPhotoPath = null;
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.hermann.bussenliste.fileprovider",
                        photoFile);
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePhotoIntent, TAKE_IMAGE_REQUEST);
            }
        }
    }

    private void dispatchPickPictureIntent() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhotoIntent, PICK_IMAGE_REQUEST);
    }

    private void deletePicture() {
        selectedPlayer.setPhoto(null);
        updateUI();
        try {
            dataSourcePlayer.updatePlayer(selectedPlayer);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setPlayerPhoto(Bitmap bitmap) {
        selectedPlayer.setPhoto(bitmap);
        updateUI();
        try {
            dataSourcePlayer.updatePlayer(selectedPlayer);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    private void handleCameraPhoto() {

        if (mCurrentPhotoPath != null) {
            setPic();
            galleryAddPic();
            mCurrentPhotoPath = null;
        }

    }

    private void deletePlayer(){
        dataSourcePlayer.deletePlayer(selectedPlayer.getId());
        //Delete player in remote MySQL DB
        deletePlayerOnServer(selectedPlayer);
    }

    private void deletePlayerOnServer(Player player) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        Gson gson = new GsonBuilder().create();
        params.put("playersJSON", gson.toJson(player.getName()));
        client.post(PRODUCTION_SERVER_ADDRESS + "deleteplayer.php", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Toast.makeText(getApplicationContext(), R.string.deleted_players, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                if (statusCode == 404) {
                    Toast.makeText(getApplicationContext(), R.string.requested_resource_not_found, Toast.LENGTH_LONG).show();
                } else if (statusCode == 500) {
                    Toast.makeText(getApplicationContext(), R.string.something_went_wrong_at_server_end, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.unexpected_error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
