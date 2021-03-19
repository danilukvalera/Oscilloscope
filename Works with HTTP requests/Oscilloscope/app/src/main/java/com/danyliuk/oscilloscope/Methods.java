package com.danyliuk.oscilloscope;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class Methods {
    static LineGraphSeries<DataPoint> series; //график
    static DataPoint[] points;         //буфер точек для графика
    static int sizeDataPoint = 360;    //размер буфера точек графика
    static int moveY = 0;              //текущее смещение по оси Y
    //чтения данных с сервера ESP8266
    static  byte [] readBuf;            //массив для чтения данных
    static int sizeReadBuf = 360;            //размер буфера чтения

    //***********Работа с графиком***********
    //очистка сигнала
    public static void clearSignal_s() {
        MainActivity.graph.removeAllSeries();
    }
    //прорисовка сигнала
    public static void drawSignal_s(int moveY) {
        points = new DataPoint[sizeDataPoint];
        for(int x=0; x<sizeDataPoint; x++) {
//            double sin_x = Math.sin(Math.toRadians(x*10)) * displayHeight/2 + move;
//            double sin_x = Math.sin(Math.toRadians(x));
            int dataY = readBuf[x] + moveY;
            points[x] = new DataPoint(x, dataY);
        }
        series = new LineGraphSeries<DataPoint>(points);
        MainActivity.graph.addSeries(series);
    }
}
