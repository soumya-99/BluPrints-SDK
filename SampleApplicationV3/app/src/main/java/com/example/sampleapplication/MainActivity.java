package com.example.sampleapplication;

import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_ADMIN;
import static android.Manifest.permission.BLUETOOTH_ADVERTISE;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import BpPrinter.mylibrary.BluetoothConnectivity;
import BpPrinter.mylibrary.BpPrinter;
import BpPrinter.mylibrary.CardReader;
import BpPrinter.mylibrary.CardScanner;
import BpPrinter.mylibrary.Scrybe;
import BpPrinter.mylibrary.UsbConnectivity;


@RequiresApi(api = Build.VERSION_CODES.S)
public class MainActivity extends AppCompatActivity implements CardScanner, Scrybe {

    BluetoothConnectivity BpScrybeDevice;
    UsbConnectivity m_BpUsbDevice;
    final static int CONN_TYPE_BT = 1;
    final static int CONN_TYPE_USB = 2;
    int m_conn_type = CONN_TYPE_BT;
    CardReader BpcardReader = null;
    public BpPrinter BPprinter = null;
    ArrayList<String> printerList;
    Button btn_text = null;
    Button btn_img = null;
    Button btn_QrCode = null;
    Button btn_barCode = null;
    int glbPrinterWidth;
    Button btn_Print = null;
    Spinner spinner;
    int numChars;
    private static final String[] INITIAL_PERMS = {
            BLUETOOTH_SCAN,
            BLUETOOTH,
            BLUETOOTH_CONNECT,
            BLUETOOTH_ADVERTISE,
            BLUETOOTH_ADMIN
    };
    private static final int INITIAL_REQUEST = 1337;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, INITIAL_PERMS, INITIAL_REQUEST);
        spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.printer_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (position == 1) {
                    glbPrinterWidth = 48;
                    onSetPrinterType(view);
                } else {
                    glbPrinterWidth = 32;
                    onSetPrinterType(view);
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

        BpScrybeDevice = new BluetoothConnectivity(this);
        m_BpUsbDevice = new UsbConnectivity(this, this);

        Button btn_Conn = (Button) findViewById(R.id.conn_btn);
        btn_text = (Button) findViewById(R.id.text_btn);
        btn_img = (Button) findViewById(R.id.img_btn);
        btn_QrCode = (Button) findViewById(R.id.qr_btn);
        btn_barCode = (Button) findViewById(R.id.barCode_btn);
        btn_Print = (Button) findViewById(R.id.Print_btn);
        RadioGroup radioGroup = findViewById(R.id.idRadioGroup);
        registerForContextMenu(btn_Conn);

        btn_barCode.setEnabled(false);
        btn_img.setEnabled(false);
        btn_Print.setEnabled(false);
        btn_QrCode.setEnabled(false);
        btn_text.setEnabled(false);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.USBBtn:
                        m_conn_type = CONN_TYPE_USB;
                        break;
                    case R.id.BluetoothBtn:
                        m_conn_type = CONN_TYPE_BT;
                        break;
                }
            }
        });
    }

    public void onSetPrinterType(View v) {
        if (glbPrinterWidth == 32) {
            glbPrinterWidth = 32;
        } else {
            glbPrinterWidth = 48;
            showAlert("48 Characters / Line or 3 Inch (80mm) Printer Selected!");
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Select Printer to connect");

        for (int i = 0; i < printerList.size(); i++) {
            menu.add(0, v.getId(), 0, printerList.get(i));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);
        String printerName = item.getTitle().toString();
        try {
            BpScrybeDevice.connectToPrinter(printerName);
            BpcardReader = BpScrybeDevice.getCardReader(this);
            BPprinter = BpScrybeDevice.getAemPrinter();
            Toast.makeText(MainActivity.this, "Connected with " + printerName, Toast.LENGTH_SHORT).show();
            btn_barCode.setEnabled(true);
            btn_img.setEnabled(true);
            btn_Print.setEnabled(true);
            btn_QrCode.setEnabled(true);
            btn_text.setEnabled(true);
        } catch (IOException e) {
            if (e.getMessage().contains("Service discovery failed")) {
                Toast.makeText(MainActivity.this, "Not Connected\n" + printerName + " is unreachable or off otherwise it is connected with other device", Toast.LENGTH_SHORT).show();
            } else if (e.getMessage().contains("Device or resource busy")) {
                Toast.makeText(MainActivity.this, "the device is already connected", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Unable to connect", Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }


    @Override
    public void onScanMSR(String s, CardReader.CARD_TRACK card_track) {

    }

    @Override
    public void onScanDLCard(String s) {

    }

    @Override
    public void onScanRCCard(String s) {

    }

    @Override
    public void onScanRFD(String s) {

    }

    @Override
    public void onScanPacket(String s) {

    }

    @Override
    public void onScanFwUpdateRespPacket(byte[] bytes) {

    }

    @Override
    public void onDiscoveryComplete(ArrayList<String> arrayList) {

    }

    @Override
    public void onUsbConnected() {

    }

    public void onPairedPrinters(View view) {

        if (m_conn_type == CONN_TYPE_BT) {
            String p = (String) BpScrybeDevice.pairPrinter("BTprinter0314");
            printerList = (ArrayList<String>) BpScrybeDevice.getPairedPrinters();

            if (printerList.size() > 0) {
                openContextMenu(view);

            } else
                showAlert("No Paired Printers found");
        } else {
            if (m_BpUsbDevice != null) {
                if (m_BpUsbDevice.connectToPrinter() == true) {
                    BPprinter = m_BpUsbDevice.getUsbPrinter();
                    if (BPprinter != null) {
                        Toast.makeText(this, "USB Printer Connected", Toast.LENGTH_SHORT).show();
                        btn_barCode.setEnabled(true);
                        btn_img.setEnabled(true);
                        btn_Print.setEnabled(true);
                        btn_QrCode.setEnabled(true);
                        btn_text.setEnabled(true);
                    } else
                        Toast.makeText(this, "USB Printer Connect Fail", Toast.LENGTH_SHORT).show();

                } else
                    Toast.makeText(this, "USB Printer Not Found", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(this, "USB Host not Initialized", Toast.LENGTH_SHORT).show();
        }
    }

    public void showAlert(String alertMsg) {
        AlertDialog.Builder alertBox = new AlertDialog.Builder(MainActivity.this);

        alertBox.setMessage(alertMsg).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                return;
            }
        });

        AlertDialog alert = alertBox.create();
        alert.show();
    }

    public void onTestPrinter(View view) throws IOException {
        BPprinter.Initialize_Printer();
        String data1 = "CENTER ALIGNMENT\n";
        BPprinter.POS_Set_Text_alingment((byte) 0x01);
        BPprinter.print(data1);
        String data2 = "LEFT ALIGNMENT\n";
        BPprinter.POS_Set_Text_alingment((byte) 0x02);
        BPprinter.print(data2);
        String data3 = "RIGHT ALIGNMENT\n";
        BPprinter.POS_Set_Text_alingment((byte) 0x00);
        BPprinter.print(data3);
        String data4 = "TAHOMA\n";
        BPprinter.POS_Set_Char_Mode((byte) 0x01);
        BPprinter.print(data4);
        String data5 = "CALIBRI\n";
        BPprinter.POS_Set_Char_Mode((byte) 0x02);
        BPprinter.print(data5);
        String data6 = "VERDANA\n";
        BPprinter.POS_Set_Char_Mode((byte) 0x03);
        BPprinter.print(data6);
        String data7 = "NORMAL\n";
        BPprinter.POS_Set_Char_Mode((byte) 0x00);
        BPprinter.print(data7);
        String data8 = "DOUBLE HEIGHT\n";
        BPprinter.POS_Set_Char_Mode((byte) 0x10);
        BPprinter.print(data8);
        BPprinter.POS_Set_Char_Mode((byte) 0x00);
        String data9 = "DOUBLE WIDTH\n";
        BPprinter.POS_Set_Char_Mode((byte) 0x20);
        BPprinter.print(data9);
        BPprinter.POS_Set_Char_Mode((byte) 0x00);
        String data10 = "UNDERLINE TEXT\n";
        BPprinter.POS_Set_Char_Mode((byte) 0x80);
        BPprinter.print(data10);
        BPprinter.POS_Set_Char_Mode((byte) 0x00);
        String data11 = "NEGATIVE TEXT";
        BPprinter.POS_text_Character_Spacing((byte) 0x00);
        BPprinter.POS_text_Reverse_Printing((byte) 0x01);
        BPprinter.print(data11);
        BPprinter.setCarriageReturn();
        BPprinter.setCarriageReturn();
        BPprinter.POS_text_Reverse_Printing((byte) 0x00);
        BPprinter.POS_Set_Char_Mode((byte) 0x00);
        BPprinter.Initialize_Printer();
        BPprinter.setCarriageReturn();
        BPprinter.setCarriageReturn();
        BPprinter.POS_Set_Text_alingment((byte) 0x01);
        numChars = glbPrinterWidth;
        onPrintBillBluetooth(numChars);
    }

    public void onPrintBillBluetooth(int numChars) throws IOException {

        if (BPprinter == null) {
            Toast.makeText(MainActivity.this, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        BPprinter.POS_Set_Text_alingment((byte) 0x01);
        BPprinter.POS_Set_Text_alingment((byte) 0x01);
        BPprinter.Initialize_Printer();
        String data = "TWO INCH PRINTER: TEST PRINT\n ";
        BPprinter.POS_Set_Text_alingment((byte) 0x01);
        String d = " _______________________________\n";
        try {
            if (numChars == 32) {
                BPprinter.print(data);
                BPprinter.print(d);
                BPprinter.POS_Set_Text_alingment((byte) 0x01);
                data = "CODE|DESC|RATE(Rs)|QTY |AMT(Rs)\n";
                BPprinter.print(data);
                BPprinter.print(d);
                data = "13|ColgateGel |35.00|02|70.00\n" +
                        "29|Pears Soap |25.00|01|25.00\n" +
                        "88|Lux Shower |46.00|01|46.00\n" +
                        "15|Dabur Honey|65.00|01|65.00\n" +
                        "52|Dairy Milk |20.00|10|200.00\n" +
                        "128|Maggie TS |36.00|04|144.00\n" +
                        "_______________________________\n";

                BPprinter.print(data);
                BPprinter.POS_Set_Text_alingment((byte) 0x01);
                data = "  TOTAL AMOUNT (Rs.)   550.00\n";
                BPprinter.print(data);
                BPprinter.print(d);
                data = "Thank you! \n";
                BPprinter.setCarriageReturn();
                BPprinter.setCarriageReturn();
                BPprinter.setCarriageReturn();
                BPprinter.setCarriageReturn();

            } else {
                BPprinter.Initialize_Printer();
                data = "         THREE INCH PRINTER: TEST PRINT\n";
                BPprinter.print(data);
                d = "________________________________________________\n";
                BPprinter.print(d);
                data = "CODE|   DESCRIPTION   |RATE(Rs)|QTY |AMOUNT(Rs)\n";
                BPprinter.print(data);
                BPprinter.print(d);
                data = " 13 |Colgate Total Gel | 35.00  | 02 |  70.00\n" +
                        " 29 |Pears Soap 250g   | 25.00  | 01 |  25.00\n" +
                        " 88 |Lux Shower Gel 500| 46.00  | 01 |  46.00\n" +
                        " 15 |Dabur Honey 250g  | 65.00  | 01 |  65.00\n" +
                        " 52 |Cadbury Dairy Milk| 20.00  | 10 | 200.00\n" +
                        "128 |Maggie Totamto Sou| 36.00  | 04 | 144.00\n" +
                        "______________________________________________\n";

                BPprinter.print(data);
                data = "          TOTAL AMOUNT (Rs.)   550.00\n";
                BPprinter.print(data);
                BPprinter.print(d);
                data = "        Thank you! \n";
                BPprinter.print(data);
                BPprinter.setCarriageReturn();
                BPprinter.setCarriageReturn();
                BPprinter.setCarriageReturn();
                BPprinter.setCarriageReturn();
            }
            if (m_conn_type == CONN_TYPE_BT){
                BpScrybeDevice.disConnectPrinter();
            }
            else{
                m_BpUsbDevice.disConnectPrinter();
            }
            btn_barCode.setEnabled(false);
            btn_img.setEnabled(false);
            btn_Print.setEnabled(false);
            btn_QrCode.setEnabled(false);
            btn_text.setEnabled(false);





        } catch (IOException e) {
            if (e.getMessage().contains("socket closed"))
                Toast.makeText(MainActivity.this, "Printer not connected", Toast.LENGTH_SHORT).show();

        }

    }

    public void onPrintBarcode(View view) {
        BarcodeBT();
    }

    public void BarcodeBT() {
        if (BPprinter == null) {
            showAlert("Printer not connected");
            return;
        }
        String text = "*BLUPRINTS*";
        if (text.isEmpty()) {
            showAlert("Write Text TO Generate Barcode");
        } else {
            try {
                BPprinter.printBarcode(text, BpPrinter.BARCODE_TYPE.CODE39, BpPrinter.BARCODE_HEIGHT.HT_SMALL, BpPrinter.CHAR_POSITION.POS_BELOW);
                BPprinter.setLineFeed(2);
                String text1 = "12345678901";
                BPprinter.printBarcode(text1, BpPrinter.BARCODE_TYPE.UPCE, BpPrinter.BARCODE_HEIGHT.HT_SMALL, BpPrinter.CHAR_POSITION.POS_ABOVE);
                BPprinter.setLineFeed(2);
                String text2 = "0123456";
                BPprinter.printBarcode(text2, BpPrinter.BARCODE_TYPE.EAN8, BpPrinter.BARCODE_HEIGHT.HT_MEDIUM, BpPrinter.CHAR_POSITION.POS_NONE);
                BPprinter.setLineFeed(2);
                String text3 = "123456789123";
                BPprinter.printBarcode(text3, BpPrinter.BARCODE_TYPE.EAN13, BpPrinter.BARCODE_HEIGHT.HT_LARGE, BpPrinter.CHAR_POSITION.POS_BELOW);
                BPprinter.setLineFeed(2);
                BPprinter.printBarcode(text1, BpPrinter.BARCODE_TYPE.UPCA, BpPrinter.BARCODE_HEIGHT.HT_LARGE, BpPrinter.CHAR_POSITION.POS_BOTH);
                BPprinter.setLineFeed(2);
                BPprinter.setCarriageReturn();
                BPprinter.setCarriageReturn();
                BPprinter.setCarriageReturn();
                BPprinter.AutoCut();
                if (m_conn_type == CONN_TYPE_BT){
                    BpScrybeDevice.disConnectPrinter();
                }
                else{
                    m_BpUsbDevice.disConnectPrinter();
                }

                btn_barCode.setEnabled(false);
                btn_img.setEnabled(false);
                btn_Print.setEnabled(false);
                btn_QrCode.setEnabled(false);
                btn_text.setEnabled(false);

            } catch (IOException e) {
                showAlert("Printer not connected");
            }
        }
    }

    public void onPrintImage(View view) throws IOException, InterruptedException {
        onPrintingImage();
        //showAlert("Image Printed");

    }

    private void onPrintingImage() throws IOException, InterruptedException {
        InputStream is = getAssets().open("bluprintlogo1.jpg");
        Bitmap inputBitmap = BitmapFactory.decodeStream(is);
        if(glbPrinterWidth == 32){
            BPprinter.POS_Set_Text_alingment((byte) 0x01);
            BPprinter.printImage(inputBitmap, 0);
            BPprinter.printImage(inputBitmap, 0);
            BPprinter.printImage(inputBitmap, 0);
            BPprinter.printImage(inputBitmap, 0);
            BPprinter.printImage(inputBitmap, 0);
            BPprinter.setCarriageReturn();
            BPprinter.setCarriageReturn();
            BPprinter.setCarriageReturn();
            BPprinter.setCarriageReturn();
            BPprinter.Initialize_Printer();

        }
        else{
            BPprinter.POS_Set_Text_alingment((byte) 0x01);
            BPprinter.printImage(inputBitmap, 1);
            BPprinter.printImage(inputBitmap, 1);
            BPprinter.printImage(inputBitmap, 1);
            BPprinter.printImage(inputBitmap, 1);
            BPprinter.printImage(inputBitmap, 1);
            BPprinter.setCarriageReturn();
            BPprinter.setCarriageReturn();
            BPprinter.setCarriageReturn();
            BPprinter.setCarriageReturn();
            BPprinter.Initialize_Printer();
        }

        if (m_conn_type == CONN_TYPE_BT){
            BpScrybeDevice.disConnectPrinter();
        }
        else{
            m_BpUsbDevice.disConnectPrinter();
        }

        btn_barCode.setEnabled(false);
        btn_img.setEnabled(false);
        btn_Print.setEnabled(false);
        btn_QrCode.setEnabled(false);
        btn_text.setEnabled(false);



    }


/*
    private void onPrintImage() {
        try {
            InputStream is = getAssets().open("bluprintlogo1.jpg");
            Bitmap inputBitmap = BitmapFactory.decodeStream(is);
            if (glbPrinterWidth == 32) {
                BPprinter.POS_Set_Text_alingment((byte) 0x01);
                BPprinter.printImage(inputBitmap, 0);
                BPprinter.setCarriageReturn();
                BPprinter.setCarriageReturn();
                BPprinter.setCarriageReturn();
                BPprinter.Initialize_Printer();
                TimeUnit.SECONDS.sleep(2);
            } else {
                BPprinter.POS_Set_Text_alingment((byte) 0x01);
                BPprinter.printImage(inputBitmap, 1);
                TimeUnit.SECONDS.sleep(2);
                BPprinter.setCarriageReturn();
                BPprinter.setCarriageReturn();
                BPprinter.setCarriageReturn();
                BPprinter.Initialize_Printer();
                TimeUnit.SECONDS.sleep(2);

            }
            BPprinter.POS_Set_Text_alingment((byte) 0x01);
            TimeUnit.SECONDS.sleep(2);
            BPprinter.setCarriageReturn();
            BPprinter.Initialize_Printer();
            BpScrybeDevice.disConnectPrinter();
            btn_barCode.setEnabled(false);
            btn_img.setEnabled(false);
            btn_Print.setEnabled(false);
            btn_QrCode.setEnabled(false);
            btn_text.setEnabled(false);

        } catch (IOException e) {
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
*/

    public void onPrintQRCodeRaster(View view) throws WriterException, IOException {
        if (BPprinter == null) {
            Toast.makeText(MainActivity.this, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        String text = "www.bluprints.in";
        if (text.isEmpty()) {
            showAlert("Write Text To Generate QR Code");
        } else {
            Writer writer = new QRCodeWriter();
            String finalData = Uri.encode(text, "UTF-8");
            showAlert("QR " + text);
            try {
                BitMatrix bm = writer.encode(finalData, BarcodeFormat.QR_CODE, 300, 300);
                Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
                for (int i = 0; i < 300; i++) {
                    for (int j = 0; j < 300; j++) {
                        bitmap.setPixel(i, j, bm.get(i, j) ? Color.BLACK : Color.WHITE);
                    }
                }
                if (glbPrinterWidth == 32) {
                    BPprinter.POS_Set_Text_alingment((byte) 0x01);
                    BPprinter.printImage(bitmap, 0);
                    BPprinter.setCarriageReturn();
                    BPprinter.setCarriageReturn();
                    BPprinter.setCarriageReturn();
                    BPprinter.Initialize_Printer();
                } else {
                    BPprinter.POS_Set_Text_alingment((byte) 0x01);
                    BPprinter.printImage(bitmap, 1);
                    BPprinter.setCarriageReturn();
                    BPprinter.setCarriageReturn();
                    BPprinter.setCarriageReturn();
                    BPprinter.Initialize_Printer();
                }
                BPprinter.POS_Set_Text_alingment((byte) 0x01);
                BPprinter.setCarriageReturn();
                BPprinter.Initialize_Printer();
                if (m_conn_type == CONN_TYPE_BT){
                    BpScrybeDevice.disConnectPrinter();
                }
                else{
                    m_BpUsbDevice.disConnectPrinter();
                }

                btn_barCode.setEnabled(false);
                btn_img.setEnabled(false);
                btn_Print.setEnabled(false);
                btn_QrCode.setEnabled(false);
                btn_text.setEnabled(false);


            } catch (WriterException e) {
                showAlert("Error WrQR: " + e.toString());
            }
        }
    }

    public void onPrint(View view) throws IOException {
        BPprinter.Initialize_Printer();
        String data1 = "CENTER ALIGNMENT\n";
        BPprinter.POS_Set_Text_alingment((byte) 0x01);
        BPprinter.print(data1);
        String data2 = "LEFT ALIGNMENT\n";
        BPprinter.POS_Set_Text_alingment((byte) 0x02);
        BPprinter.print(data2);
        String data3 = "RIGHT ALIGNMENT\n";
        BPprinter.POS_Set_Text_alingment((byte) 0x00);
        BPprinter.print(data3);
        String data4 = "TAHOMA\n";
        BPprinter.POS_Set_Char_Mode((byte) 0x01);
        BPprinter.print(data4);
        String data5 = "CALIBRI\n";
        BPprinter.POS_Set_Char_Mode((byte) 0x02);
        BPprinter.print(data5);
        String data6 = "VERDANA\n";
        BPprinter.POS_Set_Char_Mode((byte) 0x03);
        BPprinter.print(data6);
        String data7 = "NORMAL\n";
        BPprinter.POS_Set_Char_Mode((byte) 0x00);
        BPprinter.print(data7);
        String data8 = "DOUBLE HEIGHT\n";
        BPprinter.POS_Set_Char_Mode((byte) 0x10);
        BPprinter.print(data8);
        BPprinter.POS_Set_Char_Mode((byte) 0x00);
        String data9 = "DOUBLE WIDTH\n";
        BPprinter.POS_Set_Char_Mode((byte) 0x20);
        BPprinter.print(data9);
        BPprinter.POS_Set_Char_Mode((byte) 0x00);
        String data10 = "UNDERLINE TEXT\n";
        BPprinter.POS_Set_Char_Mode((byte) 0x80);
        BPprinter.print(data10);
        BPprinter.POS_Set_Char_Mode((byte) 0x00);
        String data11 = "NEGATIVE TEXT";
        BPprinter.POS_text_Character_Spacing((byte) 0x00);
        BPprinter.POS_text_Reverse_Printing((byte) 0x01);
        BPprinter.print(data11);
        BPprinter.setCarriageReturn();
        BPprinter.setCarriageReturn();
        BPprinter.POS_text_Reverse_Printing((byte) 0x00);
        BPprinter.POS_Set_Char_Mode((byte) 0x00);
        BPprinter.Initialize_Printer();
        BPprinter.setCarriageReturn();
        BPprinter.setCarriageReturn();
        BPprinter.POS_Set_Text_alingment((byte) 0x01);
        numChars = glbPrinterWidth;
        if (BPprinter == null) {
            Toast.makeText(MainActivity.this, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        BPprinter.POS_Set_Text_alingment((byte) 0x01);
        BPprinter.POS_Set_Text_alingment((byte) 0x01);
        BPprinter.Initialize_Printer();
        String data = "TWO INCH PRINTER: TEST PRINT\n ";
        BPprinter.POS_Set_Text_alingment((byte) 0x01);
        String d = " _______________________________\n";
        try {
            if (numChars == 32) {
                BPprinter.print(data);
                BPprinter.print(d);
                BPprinter.POS_Set_Text_alingment((byte) 0x01);
                data = "CODE|DESC|RATE(Rs)|QTY |AMT(Rs)\n";
                BPprinter.print(data);
                BPprinter.print(d);
                data = "13|ColgateGel |35.00|02|70.00\n" +
                        "29|Pears Soap |25.00|01|25.00\n" +
                        "88|Lux Shower |46.00|01|46.00\n" +
                        "15|Dabur Honey|65.00|01|65.00\n" +
                        "52|Dairy Milk |20.00|10|200.00\n" +
                        "128|Maggie TS |36.00|04|144.00\n" +
                        "_______________________________\n";

                BPprinter.print(data);
                BPprinter.POS_Set_Text_alingment((byte) 0x01);
                data = "  TOTAL AMOUNT (Rs.)   550.00\n";
                BPprinter.print(data);
                BPprinter.print(d);
                data = "Thank you! \n";
                BPprinter.setCarriageReturn();
                BPprinter.setCarriageReturn();

            } else {
                BPprinter.Initialize_Printer();
                data = "         THREE INCH PRINTER: TEST PRINT\n";
                BPprinter.print(data);
                d = "________________________________________________\n";
                BPprinter.print(d);
                data = "CODE|   DESCRIPTION   |RATE(Rs)|QTY |AMOUNT(Rs)\n";
                BPprinter.print(data);
                BPprinter.print(d);
                data = " 13 |Colgate Total Gel | 35.00  | 02 |  70.00\n" +
                        " 29 |Pears Soap 250g   | 25.00  | 01 |  25.00\n" +
                        " 88 |Lux Shower Gel 500| 46.00  | 01 |  46.00\n" +
                        " 15 |Dabur Honey 250g  | 65.00  | 01 |  65.00\n" +
                        " 52 |Cadbury Dairy Milk| 20.00  | 10 | 200.00\n" +
                        "128 |Maggie Totamto Sou| 36.00  | 04 | 144.00\n" +
                        "______________________________________________\n";

                BPprinter.print(data);
                data = "          TOTAL AMOUNT (Rs.)   550.00\n";
                BPprinter.print(data);
                BPprinter.print(d);
                data = "        Thank you! \n";
            }
            BPprinter.print(data);
            BPprinter.setCarriageReturn();
            BPprinter.setCarriageReturn();


        } catch (IOException e) {
            if (e.getMessage().contains("socket closed"))
                Toast.makeText(MainActivity.this, "Printer not connected", Toast.LENGTH_SHORT).show();

        }
        try {
            InputStream is = getAssets().open("bluprintlogo1.jpg");
            Bitmap inputBitmap = BitmapFactory.decodeStream(is);
            if (glbPrinterWidth == 32) {
                BPprinter.POS_Set_Text_alingment((byte) 0x01);
                BPprinter.printImage(inputBitmap, 0);
                BPprinter.setCarriageReturn();
                BPprinter.setCarriageReturn();
                BPprinter.setCarriageReturn();
                BPprinter.Initialize_Printer();
            } else {
                BPprinter.POS_Set_Text_alingment((byte) 0x01);
                BPprinter.printImage(inputBitmap, 1);
                BPprinter.setCarriageReturn();
                BPprinter.setCarriageReturn();
                BPprinter.setCarriageReturn();
                BPprinter.Initialize_Printer();

            }
            BPprinter.POS_Set_Text_alingment((byte) 0x01);
            BPprinter.Initialize_Printer();

        } catch (IOException e) {
        }

        if (BPprinter == null) {
            Toast.makeText(MainActivity.this, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        String text = "www.bluprints.in";
            Writer writer = new QRCodeWriter();
            String finalData = Uri.encode(text, "UTF-8");
            //showAlert("QR " + text);
            try {
                BitMatrix bm = writer.encode(finalData, BarcodeFormat.QR_CODE, 300, 300);
                Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
                for (int i = 0; i < 300; i++) {
                    for (int j = 0; j < 300; j++) {
                        bitmap.setPixel(i, j, bm.get(i, j) ? Color.BLACK : Color.WHITE);
                    }
                }
                if (glbPrinterWidth == 32) {
                    BPprinter.POS_Set_Text_alingment((byte) 0x01);
                    BPprinter.printImage(bitmap, 0);
                    BPprinter.setCarriageReturn();
                    BPprinter.setCarriageReturn();
                    BPprinter.Initialize_Printer();
                } else {
                    BPprinter.POS_Set_Text_alingment((byte) 0x01);
                    BPprinter.printImage(bitmap, 1);
                    BPprinter.setCarriageReturn();
                    BPprinter.setCarriageReturn();
                    BPprinter.Initialize_Printer();

                }
                BPprinter.POS_Set_Text_alingment((byte) 0x01);
                BPprinter.setCarriageReturn();
                BPprinter.Initialize_Printer();
               // TimeUnit.SECONDS.sleep(1);
                if (BPprinter == null) {
                    return;
                }
                text = "*BLUPRINTS*";
                if (text.isEmpty()) {
                    showAlert("Write Text TO Generate Barcode");
                } else {
                    try {
                        BPprinter.printBarcode(text, BpPrinter.BARCODE_TYPE.CODE39, BpPrinter.BARCODE_HEIGHT.HT_SMALL, BpPrinter.CHAR_POSITION.POS_BELOW);
                        BPprinter.setLineFeed(2);
                        String text1 = "12345678901";
                        BPprinter.printBarcode(text1, BpPrinter.BARCODE_TYPE.UPCE, BpPrinter.BARCODE_HEIGHT.HT_SMALL, BpPrinter.CHAR_POSITION.POS_ABOVE);
                        BPprinter.setLineFeed(2);
                        String text2 = "0123456";
                        BPprinter.printBarcode(text2, BpPrinter.BARCODE_TYPE.EAN8, BpPrinter.BARCODE_HEIGHT.HT_MEDIUM, BpPrinter.CHAR_POSITION.POS_NONE);
                        BPprinter.setLineFeed(2);
                        String text3 = "123456789123";
                        BPprinter.printBarcode(text3, BpPrinter.BARCODE_TYPE.EAN13, BpPrinter.BARCODE_HEIGHT.HT_LARGE, BpPrinter.CHAR_POSITION.POS_BELOW);
                        BPprinter.setLineFeed(2);
                        BPprinter.printBarcode(text1, BpPrinter.BARCODE_TYPE.UPCA, BpPrinter.BARCODE_HEIGHT.HT_LARGE, BpPrinter.CHAR_POSITION.POS_BOTH);
                        BPprinter.setLineFeed(2);
                        BPprinter.setCarriageReturn();
                        BPprinter.setCarriageReturn();
                        BPprinter.setCarriageReturn();
                        BPprinter.AutoCut();

                    } catch (IOException e) {
                        showAlert("Printer not connected");
                    }
                }

            } catch (WriterException e) {
                showAlert("Error WrQR: " + e.toString());
            }
        if (m_conn_type == CONN_TYPE_BT){
            BpScrybeDevice.disConnectPrinter();
        }
        else{
            m_BpUsbDevice.disConnectPrinter();
        }

        btn_barCode.setEnabled(false);
        btn_img.setEnabled(false);
        btn_Print.setEnabled(false);
        btn_QrCode.setEnabled(false);
        btn_text.setEnabled(false);

    }

    }
