package com.danyliuk.oscilloscope;

import android.app.Activity;
import android.provider.Telephony;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class SocketOscill {
    public   Socket  mSocket = null;
    private  String  mHost   = null;
    private  int     mPort   = 0;

    public static final String LOG_TAG = "SOCKET";
    String strReceive = "";


    public SocketOscill (final String host, final int port)
    {
        this.mHost = host;
        this.mPort = port;
    }

    // Метод открытия сокета
    public void openConnection() throws Exception
    {
        // Если сокет уже открыт, то он закрывается
        closeConnection();
        try {
            // Создание сокета
            mSocket = new Socket(mHost, mPort);
        } catch (IOException e) {
            throw new Exception("Невозможно создать сокет: "
                    + e.getMessage());
        }
    }
    /**
     * Метод закрытия сокета
     */
    public void closeConnection()
    {
        if (mSocket != null && !mSocket.isClosed()) {
            try {
                mSocket.close();
                mSocket = null;
            } catch (IOException e) {
                Log.e(LOG_TAG, "Ошибка при закрытии сокета :"
                        + e.getMessage());
            } finally {
                mSocket = null;
            }
        }
        mSocket = null;
    }
    /**
     * Метод отправки данных
     */
    public void sendData(byte[] data) throws Exception {
        // Проверка открытия сокета
        if (mSocket == null || mSocket.isClosed()) {
            throw new Exception("Ошибка отправки данных. " +
                    "Сокет не создан или закрыт");
        }
        // Отправка данных
        try {
            mSocket.getOutputStream().write(data);
            mSocket.getOutputStream().flush();
        } catch (IOException e) {
            throw new Exception("Ошибка отправки данных : "
                    + e.getMessage());
        }
    }
    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        closeConnection();
    }

}
