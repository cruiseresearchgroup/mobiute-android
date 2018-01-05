package com.ute.mobi.utilities;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jonathanliono on 22/12/2015.
 */
public class HttpPostMultipartUtility {
  private final String boundary;
  private static final String TWO_HYPHENS = "--";
  private static final String LINE_FEED = "\r\n";
  private HttpURLConnection httpConn;
  private String charset;
  //private OutputStream outputStream;
  //private PrintWriter writer;
  DataOutputStream outputStream = null;

  /**
   * This constructor initializes a new HTTP POST request with content type
   * is set to multipart/form-data
   * @param requestURL
   * @param charset
   * @throws IOException
   */
  public HttpPostMultipartUtility(HttpURLConnection httpConnS, String charset)
          throws IOException {
    this.charset = charset;

    // creates a unique boundary based on time stamp
    boundary = "===" + System.currentTimeMillis() + "===";

    httpConn = httpConnS;
    httpConn.setUseCaches(false);
    httpConn.setDoOutput(true); // indicates POST method
    httpConn.setDoInput(true);
    httpConn.setChunkedStreamingMode(1024);
    httpConn.setRequestProperty("Connection", "close"); // disables Keep Alive
    //httpConn.setRequestProperty("Connection", "Keep-Alive");
    httpConn.setRequestProperty("Content-Type",
            "multipart/form-data; boundary=" + boundary);
    httpConn.setRequestProperty("User-Agent", "Android Agent");
    httpConn.setRequestProperty("Test", "UbiQSense");
    //outputStream = httpConn.getOutputStream();
    /*writer = new PrintWriter(new OutputStreamWriter(outputStream, charset),
            true);*/
    outputStream = new DataOutputStream(httpConn.getOutputStream());
  }

  /**
   * Adds a form field to the request
   * @param name field name
   * @param value field value
   */
  public void addFormField(String name, String value) throws IOException {
    outputStream.writeBytes(TWO_HYPHENS + boundary + LINE_FEED);
    outputStream.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"" + LINE_FEED);
    //outputStream.writeBytes("Content-Type: text/plain; charset=" + charset + LINE_FEED);
    outputStream.writeBytes(LINE_FEED);
    outputStream.writeBytes(value);
    outputStream.writeBytes(LINE_FEED);
    outputStream.flush();
    /*writer.append("--" + boundary).append(LINE_FEED);
    writer.append("Content-Disposition: form-data; name=\"" + name + "\"")
            .append(LINE_FEED);
    writer.append("Content-Type: text/plain; charset=" + charset).append(
            LINE_FEED);
    writer.append(LINE_FEED);
    writer.append(value).append(LINE_FEED);
    writer.flush();*/

  }

  /**
   * Adds a upload file section to the request
   * @param fieldName name attribute in <input type="file" name="..." />
   * @param uploadFile a File to be uploaded
   * @throws IOException
   */
  public void addFilePart(String fieldName, File uploadFile)
          throws IOException {
    int bytesRead, bytesAvailable, bufferSize;
    byte[] buffer;
    int maxBufferSize = 1 * 1024;
    String fileName = uploadFile.getName();
    /*writer.append("--" + boundary).append(LINE_FEED);
    writer.append(
            "Content-Disposition: form-data; name=\"" + fieldName
                    + "\"; filename=\"" + fileName + "\"")
            .append(LINE_FEED);
    writer.append(
            "Content-Type: "
                    + URLConnection.guessContentTypeFromName(fileName))
            .append(LINE_FEED);
    writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
    writer.append(LINE_FEED);
    writer.flush();*/

    outputStream.writeBytes(TWO_HYPHENS + boundary + LINE_FEED);
    outputStream.writeBytes(
            "Content-Disposition: form-data; name=\"" + fieldName
                    + "\"; filename=\"" + fileName + "\"" + LINE_FEED);
    /*outputStream.writeBytes(
            "Content-Type: "
                    + URLConnection.guessContentTypeFromName(fileName) + LINE_FEED);
    outputStream.writeBytes("Content-Transfer-Encoding: binary" + LINE_FEED);*/
    outputStream.writeBytes(LINE_FEED);
    //outputStream.flush();

    FileInputStream inputStream = new FileInputStream(uploadFile);
    bytesAvailable = inputStream.available();
    bufferSize = Math.min(bytesAvailable, maxBufferSize);
    buffer = new byte[bufferSize];
    // Read file
    bytesRead = inputStream.read(buffer, 0, bufferSize);
    AndroidLogger.e("File length", bytesAvailable + "");
    while (bytesRead > 0) {
      outputStream.write(buffer, 0, bufferSize);
      bytesAvailable = inputStream.available();
      bufferSize = Math.min(bytesAvailable, maxBufferSize);
      bytesRead = inputStream.read(buffer, 0, bufferSize);
    }
    inputStream.close();

    /*FileInputStream inputStream = new FileInputStream(uploadFile);
    byte[] buffer = new byte[4096];
    int bytesRead = -1;
    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }
    outputStream.flush();
    inputStream.close();*/

    /*writer.append(LINE_FEED);
    writer.flush();*/
    outputStream.writeBytes(LINE_FEED);
    outputStream.flush();
  }

  /**
   * Adds a header field to the request.
   * @param name - name of the header field
   * @param value - value of the header field
   */
  public void addHeaderField(String name, String value) throws IOException {
    /*writer.append(name + ": " + value).append(LINE_FEED);
    writer.flush();*/
    outputStream.writeBytes(name + ": " + value + LINE_FEED);
    outputStream.flush();
  }

  /**
   * Completes the request and receives response from the server.
   * @return a list of Strings as response in case the server returned
   * status OK, otherwise an exception is thrown.
   * @throws IOException
   */
  public HttpURLConnection finish() throws IOException {
    List<String> response = new ArrayList<String>();

    /*writer.append(LINE_FEED).flush();
    writer.append("--" + boundary + "--").append(LINE_FEED);
    writer.close();*/
    outputStream.writeBytes(LINE_FEED);
    outputStream.writeBytes("--" + boundary + "--" + LINE_FEED);
    flushAndClose();


    return httpConn;
  }

  public void flushAndClose() throws IOException {
    outputStream.flush();
    outputStream.close();
  }
}
