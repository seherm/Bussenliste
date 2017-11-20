package com.hermann.bussenliste;

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
import android.text.InputType;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PlayerDetailsActivity extends AppCompatActivity {

    private ArrayList<Fine> selectedItems;
    private Player selectedPlayer;
    private FinesAdapter finesAdapter;
    private DataSourcePlayer dataSourcePlayer;
    private DataSourceFine dataSourceFine;
    private TextView totalSumOfFines;
    private ImageView mImageView;

    private static final int TAKE_IMAGE_REQUEST = 1;
    private static final int PICK_IMAGE_REQUEST = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 3;

    String mCurrentPhotoPath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_details);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFineSelectionDialog();
            }
        });

        selectedPlayer = (Player) getIntent().getSerializableExtra("SelectedPlayer");
        TextView playerName = (TextView) findViewById(R.id.player_name);
        playerName.setText(selectedPlayer.getName());

        updateTotalSumOfFinesView();

        dataSourcePlayer = new DataSourcePlayer(this);
        dataSourceFine = new DataSourceFine(this);

        mImageView = findViewById(R.id.player_image);

        final ListView finesListView = (ListView) findViewById(R.id.finesListView);
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
                        totalSumOfFines.setText(fineAmount);
                        try {
                            dataSourcePlayer.updatePlayer(selectedPlayer.getId(), selectedPlayer.getFines());
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
            case R.id.action_create_fine:
                showCreateNewFineDialog();
                return true;
            case R.id.action_add_photo:
                showAddPhotoDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showCreateNewFineDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText descriptionBox = new EditText(this);
        descriptionBox.setHint(R.string.fineDescription);
        layout.addView(descriptionBox);
        final EditText amountBox = new EditText(this);
        amountBox.setHint(R.string.fineAmountText);
        amountBox.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        layout.addView(amountBox);
        builder.setTitle(R.string.action_create_fine);
        builder.setView(layout);
        builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String fineDescription = String.valueOf(descriptionBox.getText());
                String fineAmount = String.valueOf(amountBox.getText());
                dataSourceFine.createFine(fineDescription, Integer.parseInt(fineAmount));
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.show();
    }

    private void showAddPhotoDialog() {

        checkPermissions();

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
                                    takePhoto();
                                    break;
                                case 1:
                                    pickPhoto();
                                    dialog.dismiss();
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
                                    takePhoto();
                                    break;
                                case 1:
                                    dialog.dismiss();
                                    pickPhoto();
                                    break;
                                case 2:
                                    dialog.dismiss();
                                    deletePhoto();
                            }
                        }
                    });
                    builder.show();
                }
            } else {
                Toast.makeText(this, R.string.error_camera_permission, Toast.LENGTH_SHORT).show();
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
                    dataSourcePlayer.updatePlayer(selectedPlayer.getId(), selectedPlayer.getFines());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                updateTotalSumOfFinesView();
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

    private void updateTotalSumOfFinesView() {
        if (totalSumOfFines == null) {
            totalSumOfFines = (TextView) findViewById(R.id.total_sum_of_fines);
        }
        String fineAmount = getString(R.string.fineAmount, selectedPlayer.getTotalSumOfFines());
        totalSumOfFines.setText(fineAmount);
    }

    private void checkPermissions() {
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

    private void pickPhoto() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhotoIntent, PICK_IMAGE_REQUEST);
    }

    private void deletePhoto() {
        selectedPlayer.setPhoto(null);
    }

    private void takePhoto() {
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePhotoIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
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

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        selectedPlayer.setPhoto(bitmap);
        mImageView.setImageBitmap(bitmap);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri uri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                // Log.d(TAG, String.valueOf(bitmap));
                mImageView.setImageBitmap(bitmap);
                selectedPlayer.setPhoto(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if(requestCode == TAKE_IMAGE_REQUEST && resultCode == RESULT_OK){
            handleCameraPhoto();
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void handleCameraPhoto() {

        if (mCurrentPhotoPath != null) {
            setPic();
            galleryAddPic();
            mCurrentPhotoPath = null;
        }

    }
}
