package com.hermann.bussenliste.unitconverter.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.hermann.bussenliste.repository.DataSourceFine;
import com.hermann.bussenliste.repository.DataSourcePlayer;
import com.hermann.bussenliste.R;
import com.hermann.bussenliste.domain.Player;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ImportDataActivity extends AppCompatActivity {

    private static final String TAG = "ImportDataActivity";
    private DataSourcePlayer dataSourcePlayer;
    private DataSourceFine dataSourceFine;
    private File file;
    private ArrayList<String> pathHistory;
    private String lastDirectory;
    private int count = 0;
    private ProgressDialog progressDialog;
    private ListView listViewInternalStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_data);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dataSourcePlayer = new DataSourcePlayer(this);
        dataSourceFine = new DataSourceFine(this);

        listViewInternalStorage = (ListView) findViewById(R.id.lvInternalStorage);
        Button buttonUpDirectory = (Button) findViewById(R.id.btnUpDirectory);
        Button buttonSDCard = (Button) findViewById(R.id.btnViewSDCard);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.importing_files));
        progressDialog.setMessage(getString(R.string.please_wait));
        progressDialog.setCancelable(false);

        checkFilePermissions();

        listViewInternalStorage.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                lastDirectory = pathHistory.get(count);
                if (lastDirectory.equals(adapterView.getItemAtPosition(i))) {
                    Log.d(TAG, "listViewInternalStorage: Selected a file for import: " + lastDirectory);
                    progressDialog.show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //Execute method for reading the excel data.
                            readExcelData(lastDirectory);
                        }
                    }).start();
                } else {
                    count++;
                    pathHistory.add(count, (String) adapterView.getItemAtPosition(i));
                    checkInternalStorage();
                    Log.d(TAG, "listViewInternalStorage: " + pathHistory.get(count));
                }
            }
        });

        //Goes up one directory level
        buttonUpDirectory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (count == 0) {
                    Log.d(TAG, "buttonUpDirectory: You have reached the highest level directory.");
                } else {
                    pathHistory.remove(count);
                    count--;
                    checkInternalStorage();
                    Log.d(TAG, "buttonUpDirectory: " + pathHistory.get(count));
                }
            }
        });

        //Opens the SDCard or phone memory
        buttonSDCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                count = 0;
                pathHistory = new ArrayList<>();
                pathHistory.add(count, System.getenv("EXTERNAL_STORAGE"));
                Log.d(TAG, "buttonSDCard: " + pathHistory.get(count));
                checkInternalStorage();
            }
        });


    }

    /**
     * reads the excel file columns then rows. Stores data as player or fine object
     */
    private void readExcelData(String filePath) {
        //Declare input file
        File inputFile = new File(filePath);

        try {
            InputStream inputStream = new FileInputStream(inputFile);
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            int numberOfSheets = workbook.getNumberOfSheets();

            //Creates for every Sheet a StringBuilder object
            for (int i = 0; i < numberOfSheets; i++) {
                XSSFSheet sheet = workbook.getSheetAt(i);
                int rowsCount = sheet.getPhysicalNumberOfRows();
                FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
                StringBuilder stringBuilder = new StringBuilder();

                //outer loop, loops through rows
                for (int r = 1; r < rowsCount; r++) {
                    Row row = sheet.getRow(r);
                    int cellsCount = row.getPhysicalNumberOfCells();
                    //inner loop, loops through columns
                    for (int c = 0; c < cellsCount; c++) {
                        //handles if there are too many columns on the excel sheet.
                        if (i == 0 && c > 2 || i == 1 && c > 3) {
                            Toast.makeText(this, R.string.error_incorrect_excel_format, Toast.LENGTH_SHORT).show();
                            break;
                        } else {
                            String value = getCellAsString(row, c, formulaEvaluator);
                            String cellInfo = "r:" + r + "; c:" + c + "; v:" + value;
                            Log.d(TAG, "readExcelData: Data from row: " + cellInfo);
                            stringBuilder.append(value).append(", ");
                        }
                    }
                    stringBuilder.append(":");
                }
                Log.d(TAG, "readExcelData: StringBuilder: " + stringBuilder.toString());
                parseStringBuilder(stringBuilder, i);
            }

        } catch (FileNotFoundException e) {
            Log.e(TAG, "readExcelData: FileNotFoundException. " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "readExcelData: Error reading InputStream. " + e.getMessage());
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                progressDialog.dismiss();
                goToMainPage();
            }
        });
    }

    /**
     * Method for parsing imported data
     */
    private void parseStringBuilder(StringBuilder stringBuilder, int sheetIndex) {
        Log.d(TAG, "parseStringBuilder: Started parsing.");

        // splits the sb into rows.
        String[] rows = stringBuilder.toString().split(":");

        //Create the players row by row
        for (String row : rows) {
            //Split the columns of the rows
            String[] columns = row.split(",");

            //use try catch to make sure there are no "" that try to parse into doubles.
            try {

                if (sheetIndex == 0) {
                    String name = columns[0];
                    Player player = new Player(name);
                    dataSourcePlayer.createPlayer(player);
                } else {
                    String description = columns[0];
                    int amount = (int) Double.parseDouble(columns[1].trim());
                    dataSourceFine.createFine(description, amount);
                }


            } catch (NumberFormatException e) {
                Log.e(TAG, "parseStringBuilder: NumberFormatException: " + e.getMessage());
            }
        }
    }

    /**
     * Returns the cell as a string from the excel file
     */
    private String getCellAsString(Row row, int c, FormulaEvaluator formulaEvaluator) {
        String value = "";
        try {
            Cell cell = row.getCell(c);
            CellValue cellValue = formulaEvaluator.evaluate(cell);
            switch (cellValue.getCellType()) {
                case Cell.CELL_TYPE_BOOLEAN:
                    value = "" + cellValue.getBooleanValue();
                    break;
                case Cell.CELL_TYPE_NUMERIC:
                    double numericValue = cellValue.getNumberValue();
                    if (HSSFDateUtil.isCellDateFormatted(cell)) {
                        double date = cellValue.getNumberValue();
                        SimpleDateFormat formatter =
                                new SimpleDateFormat("dd/MM/yy", Locale.GERMAN);
                        value = formatter.format(HSSFDateUtil.getJavaDate(date));
                    } else {
                        value = "" + numericValue;
                    }
                    break;
                case Cell.CELL_TYPE_STRING:
                    value = "" + cellValue.getStringValue();
                    break;
                default:
            }
        } catch (NullPointerException e) {

            Log.e(TAG, "getCellAsString: NullPointerException: " + e.getMessage());
        }
        return value;
    }


    private void checkInternalStorage() {
        Log.d(TAG, "checkInternalStorage: Started.");
        try {
            if (!Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED)) {
                Toast.makeText(this, "No SD card found.", Toast.LENGTH_SHORT).show();
            } else {
                // Locate the image folder in your SD Car;d
                file = new File(pathHistory.get(count));
                Log.d(TAG, "checkInternalStorage: directory path: " + pathHistory.get(count));
            }

            File[] listFile = file.listFiles();

            // Create a String array for FilePathStrings
            String[] filePathStrings = new String[listFile.length];

            // Create a String array for FileNameStrings
            String[] fileNameStrings = new String[listFile.length];

            for (int i = 0; i < listFile.length; i++) {
                // Get the path of the image file
                filePathStrings[i] = listFile[i].getAbsolutePath();
                // Get the name image file
                fileNameStrings[i] = listFile[i].getName();
            }

            for (File aListFile : listFile) {
                Log.d("Files", "FileName:" + aListFile.getName());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filePathStrings);
            listViewInternalStorage.setAdapter(adapter);

        } catch (NullPointerException e) {
            Log.e(TAG, "checkInternalStorage: NullPointerException " + e.getMessage());
        }
    }

    private void goToMainPage(){
        startActivity(new Intent(this, MainActivity.class));
    }

    private void checkFilePermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.READ_EXTERNAL_STORAGE");
            permissionCheck += this.checkSelfPermission("Manifest.permission.WRITE_EXTERNAL_STORAGE");
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1001); //Any number
            }
        } else {
            Log.d(TAG, "checkFilePermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }
}