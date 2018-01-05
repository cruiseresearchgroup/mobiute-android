package com.ute.mobi.utilities;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.Map;

/**
 * Created by jonathanliono on 24/12/2015.
 */
public class HttpPostMultipartProto {

  public static final String lineEnd = "\r\n";
  public static final String twoHyphens = "--";
  String boundary;

  public HttpURLConnection sendFileToServer(HttpURLConnection connection, final Map<String, Object> params) {
    DataOutputStream outputStream = null;
    boundary = "===" + System.currentTimeMillis() + "===";
    // DataInputStream inputStream = null;

    try {
      // Allow Inputs & Outputs
      connection.setDoInput(true);
      connection.setDoOutput(true);
      connection.setUseCaches(false);
      //connection.setChunkedStreamingMode(1024);
      // Enable POST method
      connection.setRequestMethod("POST");

      connection.setRequestProperty("Connection", "Keep-Alive");
      connection.setRequestProperty("Content-Type",
              "multipart/form-data; boundary=" + boundary);

      outputStream = new DataOutputStream(connection.getOutputStream());
      // loop
      for (Map.Entry<String, Object> entry : params.entrySet()) {
        Object value = entry.getValue();
        if(value instanceof File) {
        } else {
          this.attachFieldFormData(outputStream, entry.getKey(), value);
        }
      }

      for (Map.Entry<String, Object> entry : params.entrySet()) {
        Object value = entry.getValue();
        if(value instanceof File) {
          this.attachFile(outputStream, entry.getKey(), (File) value);
        } else {
        }
      }

      outputStream.writeBytes(lineEnd);
      outputStream.writeBytes(twoHyphens + boundary + twoHyphens
              + lineEnd);

      outputStream.flush();
      outputStream.close();

      return connection;

      /*// Responses from the server (code and message)
      int serverResponseCode = connection.getResponseCode();
      String serverResponseMessage = connection.getResponseMessage();
      AndroidLogger.i("Server Response Code ", "" + serverResponseCode);
      AndroidLogger.i("Server Response Message", serverResponseMessage);

      if (serverResponseCode == 200) {
        response = "true";
      }

      String CDate = null;
      Date serverTime = new Date(connection.getDate());
      try {
        CDate = df.format(serverTime);
      } catch (Exception e) {
        e.printStackTrace();
        AndroidLogger.e("Date Exception", e.getMessage() + " Parse Exception");
      }
      AndroidLogger.i("Server Response Time", CDate + "");

      filename = CDate
              + filename.substring(filename.lastIndexOf("."),
              filename.length());
      AndroidLogger.i("File Name in Server : ", filename);

      fileInputStream.close();
      outputStream.flush();
      outputStream.close();
      outputStream = null;*/
    } catch (Exception ex) {
      // Exception handling
      AndroidLogger.e("Send file Exception", ex.getMessage() + "");
      ex.printStackTrace();
    }
    return null;
  }

  public void attachFieldFormData(DataOutputStream outputStream, String name, Object value) throws IOException {
    outputStream.writeBytes(twoHyphens + boundary + lineEnd);
    outputStream.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"" + lineEnd);
    outputStream.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
    //outputStream.writeBytes("Content-Length: " + value.length() + lineEnd);
    outputStream.writeBytes(lineEnd);
    outputStream.writeBytes(value + lineEnd);
  }

  public void attachFile(DataOutputStream outputStream, String fieldName, File file) throws IOException {
    int bytesRead, bytesAvailable, bufferSize;
    byte[] buffer;
    int maxBufferSize = 1 * 1024;

    String fileName = file.getName();

    FileInputStream fileInputStream = new FileInputStream(file);

    String connstr = null;
    outputStream.writeBytes(twoHyphens + boundary + lineEnd);
    connstr = "Content-Disposition: form-data; name=\""+ fieldName + "\";filename=\""
            + fileName + "\"" + lineEnd;

    AndroidLogger.i("Connstr", connstr);

    outputStream.writeBytes(connstr);
    outputStream.writeBytes(
            "Content-Type: "
                    + URLConnection.guessContentTypeFromName(fileName) + lineEnd);
    outputStream.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);

    outputStream.writeBytes(lineEnd);

    bytesAvailable = fileInputStream.available();
    bufferSize = Math.min(bytesAvailable, maxBufferSize);
    buffer = new byte[bufferSize];

    // Read file
    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
    AndroidLogger.e("File length", bytesAvailable + "");
    while (bytesRead > 0) {
      outputStream.write(buffer, 0, bufferSize);
      bytesAvailable = fileInputStream.available();
      bufferSize = Math.min(bytesAvailable, maxBufferSize);
      bytesRead = fileInputStream.read(buffer, 0, bufferSize);
    }
    fileInputStream.close();
    outputStream.writeBytes(lineEnd);
  }
}
