package com.hermann.bussenliste;

import android.Manifest;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
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

    private DataSource dataSource;

    private static final String TAG = "ImportDataActivity";

    private String[] FilePathStrings;
    private String[] FileNameStrings;
    private File[] listFile;
    private File file;

    private ArrayList<String> pathHistory;
    private String lastDirectory;
    private int count = 0;

    private Button buttonUpDirectory, buttonSDCard;
    private ProgressBar progressBar;
    private ListView listViewInternalStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_data);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        listViewInternalStorage = (ListView) findViewById(R.id.lvInternalStorage);
        buttonUpDirectory = (Button) findViewById(R.id.btnUpDirectory);
        buttonSDCard = (Button) findViewById(R.id.btnViewSDCard);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        checkFilePermissions();

        dataSource = new DataSource(this);

        listViewInternalStorage.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                lastDirectory = pathHistory.get(count);
                if (lastDirectory.equals(adapterView.getItemAtPosition(i))) {
                    Log.d(TAG, "listViewInternalStorage: Selected a file for upload: " + lastDirectory);
                    //Execute method for reading the excel data.
                    AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
                    builder.setView(R.layout.import_files_dialog);
                    builder.show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
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

    private void readExcelData(String filePath) {
        //Declare input file

        File inputFile = new File(filePath);

        try {
            InputStream inputStream = new FileInputStream(inputFile);
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheetPlayers = workbook.getSheetAt(0);
            XSSFSheet sheetFines = workbook.getSheetAt(1);

            FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();

            StringBuilder stringBuilderPlayers = createStringBuilder(sheetPlayers, formulaEvaluator);
            StringBuilder stringBuilderFines = createStringBuilder(sheetFines, formulaEvaluator);

            parseStringBuilderPlayers(stringBuilderPlayers);
            parseStringBuilderFines(stringBuilderFines);

        } catch (FileNotFoundException e) {
            Log.e(TAG, "readExcelData: FileNotFoundException. " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "readExcelData: Error reading inputstream. " + e.getMessage());
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private StringBuilder createStringBuilder(XSSFSheet sheet, FormulaEvaluator formulaEvaluator) {
        int rowsCount = sheet.getPhysicalNumberOfRows();
        StringBuilder sb = new StringBuilder();

        //outter loop, loops through rows
        for (int r = 1; r < rowsCount; r++) {
            Row row = sheet.getRow(r);
            int cellsCount = row.getPhysicalNumberOfCells();
            //inner loop, loops through columns
            for (int c = 0; c < cellsCount; c++) {
                //handles if there are too many columns on the excel sheet.
                if (c > 3) {
                    Toast.makeText(this, R.string.error_incorrect_excel_format, Toast.LENGTH_SHORT).show();
                    break;
                } else {
                    String value = getCellAsString(row, c, formulaEvaluator);
                    String cellInfo = "r:" + r + "; c:" + c + "; v:" + value;
                    Log.d("READ_EXCEL", "readExcelData: Data from row: " + cellInfo);
                    sb.append(value + ", ");
                }
            }
            sb.append(":");
        }
        Log.d(TAG, "readExcelData: STRINGBUILDER: " + sb.toString());

        return sb;
    }

    /**
     * Method for parsing imported data
     */


    public void parseStringBuilderPlayers(StringBuilder mStringBuilder) {
        Log.d(TAG, "parseStringBuilderFines: Started parsing.");

        // splits the sb into rows.
        String[] rows = mStringBuilder.toString().split(":");

        //Add to the ArrayList<XYValue> row by row
        for (int i = 0; i < rows.length; i++) {
            //Split the columns of the rows
            String[] columns = rows[i].split(",");

            //use try catch to make sure there are no "" that try to parse into doubles.
            try {
                String name = columns[0];

                //add the the uploadData ArrayList
                dataSource.open();
                dataSource.createPlayer(name);
                dataSource.close();

            } catch (NumberFormatException e) {
                Log.e(TAG, "parseStringBuilderFines: NumberFormatException: " + e.getMessage());
            }
        }
    }


    public void parseStringBuilderFines(StringBuilder mStringBuilder) {
        Log.d(TAG, "parseStringBuilderFines: Started parsing.");

        // splits the sb into rows.
        String[] rows = mStringBuilder.toString().split(":");

        //Add to the ArrayList<XYValue> row by row
        for (int i = 0; i < rows.length; i++) {
            //Split the columns of the rows
            String[] columns = rows[i].split(",");

            //use try catch to make sure there are no "" that try to parse into doubles.
            try {
                String description = columns[0];
                int amount = (int) Double.parseDouble(columns[1].trim());

                //add the the uploadData ArrayList
                dataSource.open();
                dataSource.createFine(description, amount);
                dataSource.close();

            } catch (NumberFormatException e) {
                Log.e(TAG, "parseStringBuilderFines: NumberFormatException: " + e.getMessage());
            }
        }
    }


    /**
     * Returns the cell as a string from the excel file
     *
     * @param row
     * @param c
     * @param formulaEvaluator
     * @return
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

            listFile = file.listFiles();

            // Create a String array for FilePathStrings
            FilePathStrings = new String[listFile.length];

            // Create a String array for FileNameStrings
            FileNameStrings = new String[listFile.length];

            for (int i = 0; i < listFile.length; i++) {
                // Get the path of the image file
                FilePathStrings[i] = listFile[i].getAbsolutePath();
                // Get the name image file
                FileNameStrings[i] = listFile[i].getName();
            }

            for (int i = 0; i < listFile.length; i++) {
                Log.d("Files", "FileName:" + listFile[i].getName());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, FilePathStrings);
            listViewInternalStorage.setAdapter(adapter);

        } catch (NullPointerException e) {
            Log.e(TAG, "checkInternalStorage: NULLPOINTEREXCEPTION " + e.getMessage());
        }
    }


    private void checkFilePermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.READ_EXTERNAL_STORAGE");
            permissionCheck += this.checkSelfPermission("Manifest.permission.WRITE_EXTERNAL_STORAGE");
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1001); //Any number
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }
}