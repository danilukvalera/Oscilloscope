package com.danyliuk.oscilloscope;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;


public class MainActivity extends AppCompatActivity  implements SeekBar.OnSeekBarChangeListener, View.OnClickListener{
//public class MainActivity extends AppCompatActivity  implements View.OnClickListener {
    //region определения
    //*********для полноэкранного режима
    View mDecorView;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private final Handler mHideHandler = new Handler();
    //************************************************************
    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;

    ConstraintLayout mConstraintLayout;
    DrawCreate mDrawCreate;
    Button btVDivP, btVDivM;
    TextView tvVDiv;
    EditText etIP;
    public final static String PREF_IP = "PREF_IP_ADDRESS";
    // общие объекты параметров, используемые для сохранения IP адреса и порта, чтобы
    // пользователь не вводил их в следующий раз, когда он открывает приложение
    SharedPreferences.Editor editor;
    SharedPreferences sharedPreferences;

    FrameLayout fLayoutMovY;
    SeekBar seekBarMoveY;       //ползунок вертикального смещения
    int horizontalMiddle;       //середина экрана по горизонтали
    int verticalMiddle;         //середина экрана по вертикали
    int displayWidth;           //ширина экрана
    int displayHeight;          //высота экрана
    boolean buttonVisible = true;//признак видимости органов управления осциллографа
    GraphView graph;            //объект для рисования графика
    LineGraphSeries<DataPoint> series; //график
    DataPoint[] points;         //буфер точек для графика
    int sizeDataPoint = 360;    //размер буфера точек графика
    int moveY = 0;              //текущее смещение по оси Y
    //чтения данных с микрофона
    static  byte [] readBuf;            //массив для чтения данных
    int sizeReadBuf;            //размер буфера чтения
    AudioRecord audioRecord;
    boolean isReading = false;
    //boolean isDrawing = false;
    Thread threadRead;
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        componentInitialization();  //инициализация компонентов экрана
        settingFullScreen();        //настройка экрана и полноэкранного режима
        requestAudioPermissions();  //запрос разрешения на работу с audio и микрофоном

//        mDrawView = new DrawView(this);
//        mConstraintLayout.addView(mDrawView);
        mDrawCreate = new DrawCreate(this);   //создает объект DrawCreate в котором будет канва на которой рисовать
        //после этого системой вызывается метод onDraw(
        mConstraintLayout.addView(mDrawCreate);     //добавляет DrawCreate на экран



        //drawSignal(0);          //рисование графика
    }

    @Override
    protected void onResume() {
        super.onResume();
        createAudioRecorder();                  //создание AudioRecorder
        if(audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.startRecording();       //если AudioRecorder создан, стартует запись данных
        }                                       // с микрофона в буфер AudioRecorder
        startReadAudioRecorder();               //старт постоянного чтения данных с буфера AudioRecorder (в фоновом потоке)
        settingGraphView();                     //настройка объекта рисования графика
        threadRead.start();                     //старт чтения данных из буфера
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isReading = false;
        if (audioRecord != null) {
            audioRecord.release();
        }
    }
    //Получение данных о разрешениях
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // разрешение было получено
                    return;
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Разрешение записи Audio не получено. Функциональность приложения будет ограничена", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }}

//********Конец системных методов**********
    //*************работа с микрофоном**********
    //запрос разрешения на работу с audio и микрофоном
    private void requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Для работы приложения требуется разрешить запись Audio", Toast.LENGTH_LONG).show();

                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);

            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);
            }
        }
        //If permission is granted, then go ahead recording audio
        else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
        }
    }
    //создание AudioRecorder
    private void createAudioRecorder() {
        int sampleRate = 16000;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        //int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int audioFormat = AudioFormat.ENCODING_PCM_8BIT;

        int minInternalBufferSize = AudioRecord.getMinBufferSize(sampleRate,
                channelConfig, audioFormat);
        int internalBufferSize = minInternalBufferSize;// * 4;
        sizeReadBuf = internalBufferSize * 2;
        points = new DataPoint[sizeDataPoint];
        readBuf = new byte[sizeReadBuf];

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate, channelConfig, audioFormat, internalBufferSize);
    }
    //чтение данных с микрофона (+старт рисования графика)
    private void startReadAudioRecorder() {
        isReading = true;
        threadRead = new Thread(new Runnable() {
            @Override
            public void run() {
                if (audioRecord == null)
                    return;
                while (isReading) {
                    audioRecord.read(readBuf, 0, sizeReadBuf);
                    clearSignal();
                    drawSignal(moveY);
                }
            }
        });
    }

    //**********Работа с экраном***********
    //инициализация компонентов экрана
    private void componentInitialization() {
        mConstraintLayout = findViewById(R.id.constraintLayout);
        btVDivP = findViewById(R.id.btVDivP);           btVDivP.setOnClickListener(this);
        btVDivM = findViewById(R.id.btVDivM);           btVDivM.setOnClickListener(this);
        etIP = findViewById(R.id.etIP);

        sharedPreferences = getSharedPreferences("HTTP_HELPER_PREFS",Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        // получить IP адрес из последнего раза, когда пользователь использовал
        // приложение, или поместить пустую строку "", если это первый раз
        etIP.setText(sharedPreferences.getString(PREF_IP,""));


        fLayoutMovY = findViewById(R.id.fLayoutMovY);
        seekBarMoveY = findViewById(R.id.seekBarMovY);
        seekBarMoveY.getLayoutParams().height = fLayoutMovY.getLayoutParams().width;
        seekBarMoveY.getLayoutParams().width = fLayoutMovY.getLayoutParams().height;
        seekBarMoveY.setOnSeekBarChangeListener(this);
        graph = findViewById(R.id.graph);
    }
    //Этот метод дает реальную высоту экрана в пикселах за вычетом statusbar и др.
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View content = getWindow().findViewById(Window.ID_ANDROID_CONTENT);
        displayHeight = content.getHeight();
        verticalMiddle = displayHeight/2;
        if (hasFocus) {
            hideSystemUI();
        }
    }
    //настройка экрана и полноэкранного режима
    private void settingFullScreen() {
        //setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //установка альбомного режима

        mDecorView = getWindow().getDecorView();
        hideSystemUI();
        // Слушатель "OnSystemUiVisibilityChangeListener"
// Срабатывает когда появляются ИЛИ исчезают системные панели
// параметр visibility видимо содержит информацию о установленных флагах
// если (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0 то панели спрятаны
// "visibility" скорее всего то же самое что   "mDecorView.getSystemUiVisibility()"
        mDecorView.setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        //Если панели спрятаны
                        if((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            //                       if((mDecorView.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            //показать панели
                            showSystemUI();
                            //запустить скрытие панелей через аремя AUTO_HIDE_DELAY_MILLIS
                            mHideHandler.postDelayed(mHideRunnable, AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });
        //Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        Display display = getWindowManager().getDefaultDisplay();
        Point p = new Point();
        display.getSize(p);
        displayWidth = p.x;
        displayHeight = p.y;
        horizontalMiddle = displayWidth/2;
        verticalMiddle = displayHeight/2;
    }
    //Автоскрытие панелей управления
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hideSystemUI();
        }
    };
    // Прячем системные панели (панель навигации и строку состояния)
    private void hideSystemUI() {
        // Используем флаг IMMERSIVE.
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // прячем панель навигации
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // прячем строку состояния
                        | View.SYSTEM_UI_FLAG_IMMERSIVE  //режим погружения, панели появляются и сами не исчезают
//                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY  //"липкий" режим погружения, панели полупрозрачны и сами исчезают через время
        );
    }
    // Программно выводим системные панели обратно
    private void showSystemUI() {
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
    //скрыть органы управления осциллографом
    private void hideButton() {
        buttonVisible = false;
        seekBarMoveY.setVisibility(View.INVISIBLE);
    }
    //показать органы управления осциллографом
    private void showButton() {
        buttonVisible = true;
        seekBarMoveY.setVisibility(View.VISIBLE);
    }

    //**********Работа с меню*************
    //создание меню
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        menu.add(0, 1, 1, "Показать/скрыть"); //getResources().getString(R.string.homePage));
        menu.add(0, 2, 2, "about");    //getResources().getString(R.string.about));
        menu.add(0, 3, 3, "quit");     //getResources().getString(R.string.quit));
        return super.onCreateOptionsMenu(menu);
    }
    //обновление меню
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        return super.onPrepareOptionsMenu(menu);
    }
    // обработка нажатий
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case 1: {
                if(buttonVisible) hideButton();
                else showButton();
            }

        }

        return super.onOptionsItemSelected(item);
    }
    //***********Работа с графиком***********
    //очистка сигнала
    private void clearSignal() {
        graph.removeAllSeries();
    }
    //прорисовка сигнала
    private void drawSignal(int moveY) {
        for(int x=0; x<sizeDataPoint; x++) {
//            double sin_x = Math.sin(Math.toRadians(x*10)) * displayHeight/2 + move;
//            double sin_x = Math.sin(Math.toRadians(x));
            int dataY = readBuf[x] + moveY;
            points[x] = new DataPoint(x, dataY);
        }
        series = new LineGraphSeries<DataPoint>(points);
        graph.addSeries(series);
        //threadRead.start();
    }

    //*********Ползунок перемещения по оси Y*************
    //Изменение положения ползунка SeekBar
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        moveY = (seekBarMoveY.getMax()/2 - progress) * displayHeight/seekBarMoveY.getMax();
    }
    //Начало изменение положения ползунка SeekBar
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }
    //Окончание изменение положения ползунка SeekBar
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    //обработчик кнопок
    @Override
    public void onClick(View v) {
        // номер вывода
        String parameterValue = "";
        // получить ip адрес
        String ipAddress = etIP.getText().toString().trim();  // trim() убирает пробелы вначале и в конце строки
        // сохранить IP адрес и номер порта для следующего использования приложения
        editor.putString(PREF_IP,ipAddress);    // установить значение ip адреса для сохранения
        editor.commit();                        // сохранить IP и PORT

        switch (v.getId()) {
            case R.id.btVDivP : {
                parameterValue = "ON";
                break;
            }
            case R.id.btVDivM : {
                parameterValue ="OFF";
                break;
            }
            default: break;
        }
        // выполнить HTTP запрос
        if(ipAddress.length()>0) {
            new HttpRequestAsyncTask(
                    v.getContext(), parameterValue, ipAddress
            ).execute();
        }
    }

    //**********Объект для тисования на канве**********
    class DrawCreate extends View {
        public  DrawCreate(Context context) {
            super(context);
        }

        //вызывается системой когда создается объект DrawCreate
        //Рисование сетки экрана осциллографа
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.BLACK);
            //определяем сторону квадрата сетки и длины черточки относительно размеров экрана
            int sideSquare;         //сторона квадрата сетки
            int l;                  //длина черточки на сетке
            if(displayWidth < displayHeight) {
                sideSquare = (displayWidth-10)/8;
                l = displayWidth/36;
            }else {
                sideSquare = (displayHeight-10)/8;
                l = displayHeight/36;
            }

            //цвет канвы
            canvas.drawARGB(80, 102, 204, 255);

            //Рисуем сетку
            int  temp;
            //создаем вертикальные линии
            //центральная вертикальная линия
            paint.setStrokeWidth(3);
            canvas.drawLine(horizontalMiddle, 0, horizontalMiddle, displayHeight, paint);
            //линии влево от центра
            temp = horizontalMiddle;
            paint.setStrokeWidth(3);
            while(temp > 0) {
                temp -= sideSquare;
                canvas.drawLine(temp, 0, temp, displayHeight, paint);
            }
            //линии вправо от центра
            temp = horizontalMiddle;
            while(temp < displayWidth) {
                temp += sideSquare;
                canvas.drawLine(temp, 0, temp, displayHeight, paint);
            }
            //создаем горизонтальные линии
            //центральная горизонтальная линия
            paint.setStrokeWidth(3);
            canvas.drawLine(0, verticalMiddle, displayWidth, verticalMiddle , paint);
            //линии вверх от центра
            temp = verticalMiddle;
            paint.setStrokeWidth(3);
            while(temp > 0) {
                temp -= sideSquare;
                canvas.drawLine(0, temp, displayWidth, temp , paint);
            }
            //линии вниз от центра
            temp = verticalMiddle;
            while(temp < displayHeight) {
                temp += sideSquare;
                canvas.drawLine(0, temp, displayWidth, temp , paint);
            }
            //рисуем насечки на осях
            //насечки по горизональной оси
            //насечки влево от середины
            paint.setStrokeWidth(3);
            temp = horizontalMiddle;
            int n = 1;
            while(temp >0) {
                for (int i = 0; i < 4; i++) {
                    temp -= sideSquare / 5;
                    if(temp > 0)
                        canvas.drawLine(temp, verticalMiddle - l / 2, temp, verticalMiddle + l / 2, paint);
                }
                temp = horizontalMiddle - n * sideSquare;
                n++;
            }
            //насечки вправо от середины
            temp = horizontalMiddle;
            n = 1;
            while(temp < displayWidth) {
                for (int i = 0; i < 4; i++) {
                    temp += sideSquare / 5;
                    if(temp > 0)
                        canvas.drawLine(temp, verticalMiddle - l / 2, temp, verticalMiddle + l / 2, paint);
                }
                temp = horizontalMiddle + n * sideSquare;
                n++;
            }
            //насечки по вертикальной оси
             //насечки вверх от середины
            paint.setStrokeWidth(3);
            temp = verticalMiddle;
            n = 1;
            while(temp >0) {
                for (int i = 0; i < 4; i++) {
                    temp -= sideSquare / 5;
                    if(temp > 0)
                        canvas.drawLine(horizontalMiddle -l/2, temp, horizontalMiddle + l / 2, temp, paint);
                }
                temp = verticalMiddle - n * sideSquare;
                n++;
            }
            //насечки вниз от середины
            paint.setStrokeWidth(3);
            temp = verticalMiddle;
            n = 1;
            while(temp < displayHeight) {
                for (int i = 0; i < 4; i++) {
                    temp -= sideSquare / 5;
                    if(temp < displayHeight)
                        canvas.drawLine(horizontalMiddle -l/2, temp, horizontalMiddle + l / 2, temp, paint);
                }
                temp = verticalMiddle + n * sideSquare;
                n++;
            }

        }
    }
    //Настройка объекта рисования на канве
    private void settingGraphView() {
        //устанавливает размер шкалы по вертикали
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMaxY(displayHeight/2);
        graph.getViewport().setMinY(-1 * displayHeight/2);
        //устанавливае размер шкалы по горизонтали
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(sizeDataPoint);

        //graph.getViewport().setScrollableY(true); //разрешает скролинг по вертикали
        //graph.getViewport().setScrollable(true); //разрешает скролинг по горизонтали
        //устанавливает видимость гориз. и вертик. меток на осях
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        //устанавливает название графика (его отсутсвие)
        graph.setTitle(null);
        //устанавливает видимость толстой нулевой линии
        graph.getGridLabelRenderer().setHighlightZeroLines(false);
        //устанавливает стиль сетки - горизонтальные, вертиальные линии, и те и другие, без линий
        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
    }

}

