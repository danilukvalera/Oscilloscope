package com.danyliuk.oscilloscope;

//AsyncTask необходим для выполнения HTTP запросов в фоне, чтобы они не блокировали пользовательский интерфейс.
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.CharBuffer;

public class HttpRequestAsyncTask extends AsyncTask<Void, Void, Void> {
    // объявить необходимые переменные
    //region
    private String requestReply,ipAddress;
    private Context context;
    private AlertDialog alertDialog;
    private String parameterValue;
    //endregion
    /**
     * Description: Конструктор класса asyncTask. Назначить значения, используемые в других методах.
     * @param context контекст приложения, необходим для создания диалога
     * @param parameterValue включить/выкдючить светодиод
     * @param ipAddress ip адрес, на который необходимо послать запрос
     */
    HttpRequestAsyncTask(Context context, String parameterValue, String ipAddress) {
        this.context = context;
        this.ipAddress = ipAddress;
        this.parameterValue = parameterValue;
    }
    //Отправляет запрос на ip адрес
    @Override
    protected Void doInBackground(Void... voids) {
        requestReply = sendRequest(parameterValue,ipAddress);
        return null;
    }

    //выполняется перед отправкой HTTP запроса на ip адрес
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }
    //выполняется после возвращения ответа на HTTP запрос на ip адрес
    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
/*
        byte size =0;
        boolean dir = true;
        for(int i=0; i<Methods.sizeReadBuf; i++) {
            Methods.readBuf[i] = size;
            if(dir)
                size++;
            else size--;
            if(size==255)
                dir = false;
            if(size==0)
                dir = true;
        }
*/
        Methods.clearSignal_s();
        Methods.drawSignal_s(0);
    }
    /**
     * Description: Послать HTTP Get запрос на указанные ip адрес и порт.
     * Также послать параметр "parameterName" со значением "parameterValue".
     * @param parameterValue номер порта, у которого необходимо изменить состояние
     * @param ipAddress ip адрес, на который необходимо послать запрос
     * @return Текст ответа с ip адреса или сообщение ERROR, если не получилось получить ответ
     */
    public String sendRequest(String parameterValue, String ipAddress) {
        String serverResponse = "ERROR";

        try {
            // установить URL, например, http://myIpaddress:myport/?pin=13 (например, переключить вывод 13)
            //URL website = new URL("http://"+ipAddress+":"+portNumber+"/?"+parameterName+"="+parameterValue);

            String strURL = null;
            if(parameterValue.equals("ON")) strURL = "http://" + ipAddress+ "/"+ "LED=ON";
            else if(parameterValue.equals("OFF")) strURL = "http://" + ipAddress+ "/"+ "LED=OFF";
            URL website = new URL(strURL);
            URLConnection connection = website.openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection)connection;
            int responseCode = httpConnection.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK) {
                InputStream content = httpConnection.getInputStream();

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int read = 0;
                while ((read = content.read()) != -1) {
                    bos.write(read);
                }

                //Methods.readBuf = bos.toByteArray();
                //Methods.readBuf = new byte [Methods.sizeReadBuf];
                Methods.readBuf = bos.toByteArray();
                bos.close();
                // Закрыть соединение
                content.close();
            }
        } catch (MalformedURLException e) {
            // ошибка HTTP
            serverResponse = e.getMessage();
            e.printStackTrace();
            Log.d("MalformedURLException", serverResponse);
        } catch (IOException e) {
            // ошибка ввода/вывода
            serverResponse = e.getMessage();
            e.printStackTrace();
            Log.d("IOException", serverResponse);
        }
/*        catch (URISyntaxException e) {
            // ошибка синтаксиса URL
            serverResponse = e.getMessage();
            e.printStackTrace();
            Log.d("URISyntaxException", serverResponse);
        }
*/
        // вернуть текст отклика сервера
        return serverResponse;
    }
}
