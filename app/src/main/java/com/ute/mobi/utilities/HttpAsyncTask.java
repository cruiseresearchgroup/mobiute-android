package com.ute.mobi.utilities;

import android.content.Context;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by jonathanliono on 8/01/15.
 */
public class HttpAsyncTask extends AsyncTask<String, String, String> {
  public static enum Method {
    GET,
    POST,
    POST_MULTIPART,
  }

  public static enum ContentType {
    NORMAL,
    JSON,
  }

  protected interface DeferredAction {
    void execute();
  }

  private class CustomTimeout {
    public int ConnectionTimeOut;
    public int SOTimeout;
  }

  public boolean fake;

  private final Context context;
  private final Method method;
  private final Map<String, Object> params;
  private final ContentType contentType;
  private String acceptHeader;
  private CustomTimeout customTimeout;
  private final HttpAsyncTaskCallbacks callback;
  private DeferredAction deferredAction;

  public HttpAsyncTask(Context context, Method method, Map<String, Object> params) {
    this(context, method, params, ContentType.NORMAL, null);
  }

  public HttpAsyncTask(Context context, Method method, Map<String, Object> params, HttpAsyncTaskCallbacks callback) {
    this(context, method, params, ContentType.NORMAL, callback);
  }

  public HttpAsyncTask(Context context, Method method, Map<String, Object> params, ContentType contentType, HttpAsyncTaskCallbacks callback) {
    this.context = context.getApplicationContext();
    this.method = method;
    this.params = params;
    this.contentType = contentType;
    this.acceptHeader = null;
    this.callback = callback;
    this.deferredAction = null;
  }

  public void setAcceptHeader(String accept) {
    this.acceptHeader = accept;
  }

  /**
   * Set Custom timeout.
   *
   * @param connectionTimeout in milliseconds
   * @param dataTimeout       in milliseconds
   */
  public void setCustomTimeout(int connectionTimeout, int dataTimeout) {
    this.customTimeout = new CustomTimeout();
    this.customTimeout.ConnectionTimeOut = connectionTimeout;
    this.customTimeout.SOTimeout = dataTimeout;
  }

  /*public static final MediaType JSON
          = MediaType.parse("application/json; charset=utf-8");*/

  private static final char PARAMETER_DELIMITER = '&';
  private static final char PARAMETER_EQUALS_CHAR = '=';
  public static String createQueryStringForParameters(Map<String, Object> parameters) throws UnsupportedEncodingException {
    StringBuilder parametersAsQueryString = new StringBuilder();
    if (parameters != null) {
      boolean firstParameter = true;

      for (String parameterName : parameters.keySet()) {
        if (!firstParameter) {
          parametersAsQueryString.append(PARAMETER_DELIMITER);
        }

        parametersAsQueryString.append(parameterName)
                .append(PARAMETER_EQUALS_CHAR)
                .append(URLEncoder.encode(parameters.get(parameterName).toString(), "UTF-8"));

        firstParameter = false;
      }
    }
    return parametersAsQueryString.toString();
  }

  @Override
  protected String doInBackground(String... uri) {
    if (NetworksUtilities.isNetworkAvailable(this.context) == false) {
      if (this.callback != null) {
        this.deferredAction = new DeferredAction() {
          @Override
          public void execute() {
            callback.onNoNetworkAvailable();
          }
        };
      }

      return null;
    }

    if(fake)
      return null;

        { // new solution
          URL url = null;
          try {
            url = new URL(uri[0]);
          } catch (MalformedURLException e) {
            AndroidLogger.e("UbiQSense", org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
            return null;
          }
          try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String responseString = null;

            if(this.customTimeout != null) {
              // Set the timeout in milliseconds until a connection is established.
              // The default value is zero, that means the timeout is not used.
              int timeoutConnection = this.customTimeout.ConnectionTimeOut;
              conn.setConnectTimeout(timeoutConnection);
              // Set the default socket timeout (SO_TIMEOUT)
              // in milliseconds which is the timeout for waiting for data.
              int timeoutSocket = this.customTimeout.SOTimeout;
              conn.setReadTimeout(timeoutSocket);
            }
            else {
              conn.setReadTimeout(10000);
              conn.setConnectTimeout(15000);
            }

            conn.setRequestProperty("Connection", "close");

            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches (false);
            switch (this.method) {
              case GET:
                conn.setRequestMethod("GET");
                if(contentType == ContentType.JSON) {
                  conn.setRequestProperty("Content-Type", "application/json");
                }

                if(this.acceptHeader != null) {
                  conn.setRequestProperty("Accept", this.acceptHeader);
                }

                break;
              case POST_MULTIPART:
                //new HttpPostMultipartProto().sendFileToServer(conn, this.params);
                String charset = "UTF-8";
                HttpPostMultipartUtility multipart = new HttpPostMultipartUtility(conn, charset);
                if(this.params != null) {
                  for (Map.Entry<String, Object> entry : this.params.entrySet()) {
                    Object value = entry.getValue();
                    if(value instanceof File) {
                    } else {
                      multipart.addFormField(entry.getKey(), value.toString());
                    }
                  }

                  //multipart.flush();

                  //flush

                  for (Map.Entry<String, Object> entry : this.params.entrySet()) {
                    Object value = entry.getValue();
                    if(value instanceof File) {
                      multipart.addFilePart(entry.getKey(), (File)value);
                    } else {
                    }
                  }

                  // flush
                }

                multipart.finish();
                break;

              case POST:
                conn.setRequestMethod("POST");
                if(this.acceptHeader != null) {
                  conn.setRequestProperty("Accept", this.acceptHeader);
                }
                try {
                  if(this.params != null) {
                    switch(this.contentType) {
                      case NORMAL:
                        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        String postParameters = this.createQueryStringForParameters(this.params);
                        conn.setFixedLengthStreamingMode(postParameters.getBytes().length);
                        //Send request
                        OutputStream os = conn.getOutputStream();
                        BufferedWriter writer = new BufferedWriter(
                                new OutputStreamWriter(os, "UTF-8"));
                        writer.write(postParameters);
                        writer.flush();
                        writer.close();
                        os.close();
                        break;
                      case JSON:
                        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        String jsonToSend = null;
                        if(this.params.size() > 1) {
                          Gson gson = new Gson();
                          jsonToSend = gson.toJson(this.params);
                        }
                        else {
                          for (Map.Entry<String, Object> entry : this.params.entrySet()) {
                            Object value = entry.getValue();
                            if(value instanceof JsonObject) {
                              jsonToSend = new Gson().toJson((JsonObject) value);
                            } else {
                              jsonToSend = value.toString();
                            }
                          }
                        }

                        conn.connect();
                        OutputStreamWriter writerS = new OutputStreamWriter(conn.getOutputStream());
                        String output = jsonToSend.toString();
                        writerS.write(output);
                        writerS.flush();
                        writerS.close();
                        break;
                    }
                  }
                } catch (UnsupportedEncodingException e) {
                }

                break;
              default: return null;
            }

            final int responseCode=conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
              String line;
              StringBuilder response = new StringBuilder();
              BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
              while ((line=br.readLine()) != null) {
                response.append(line);
              }
              responseString = response.toString();
              conn.disconnect();
            }
            else {
              if(responseCode == HttpsURLConnection.HTTP_UNAUTHORIZED) {
                // do callback unauthorized
                if(this.callback != null){
                  this.deferredAction = new DeferredAction() {
                    @Override
                    public void execute() {
                      callback.onErrorUnauthorized();
                    }
                  };
                }
              }
              else {
                // do callback on general error.
                if(this.callback != null) {
                  this.deferredAction = new DeferredAction() {
                    @Override
                    public void execute() {
                      callback.onErrorGeneralRequest(responseCode);
                    }
                  };
                }
              }
            }

            return responseString;
          } catch (final IOException e) {
            AndroidLogger.e("UbiQSense", "Connection Refused");
            if(this.callback != null){
              this.deferredAction = new DeferredAction() {
                @Override
                public void execute() {
                  callback.onExceptionThrown(e);
                }
              };
            }

            AndroidLogger.e("UbiQSense", org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
            return null;
          }
        }

        /*{ // old solution of HttpClient (Apache)
            org.apache.http.client.methods.HttpRequestBase requestType;
            switch (this.method) {
                case GET: requestType = new HttpGet(uri[0]);

                    if(contentType == ContentType.JSON) {
                        ((HttpGet)requestType).setHeader("Content-type", "application/json");
                    }

                    if(this.acceptHeader != null) {
                        ((HttpGet)requestType).setHeader("Accept", this.acceptHeader);
                    }

                    break;
                case POST: requestType = new HttpPost(uri[0]);
                    StringEntity stringEntity = null;
                    try {
                        stringEntity = new StringEntity("", HTTP.UTF_8);
                        if(this.params != null) {
                            switch(this.contentType) {
                                case NORMAL:
                                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                                    Iterator it = this.params.entrySet().iterator();
                                    while (it.hasNext()) {
                                        Map.Entry pairs = (Map.Entry)it.next();
                                        nameValuePairs.add(new BasicNameValuePair(pairs.getKey().toString(), pairs.getValue().toString()));
                                        it.remove(); // avoids a ConcurrentModificationException
                                    }
                                    stringEntity = new UrlEncodedFormEntity(nameValuePairs);
                                    break;
                                case JSON:
                                    String jsonToSend = null;
                                    if(this.params.size() > 1) {
                                        Gson gson = new Gson();
                                        jsonToSend = gson.toJson(this.params);
                                    }
                                    else {
                                        for (Map.Entry<String, Object> entry : this.params.entrySet()) {
                                            Object value = entry.getValue();
                                            if(value instanceof JsonObject) {
                                                jsonToSend = new Gson().toJson((JsonObject) value);
                                            } else {
                                                jsonToSend = value.toString();
                                            }
                                        }
                                    }

                                    stringEntity = new StringEntity(jsonToSend, HTTP.UTF_8);

                                    ((HttpPost)requestType).setHeader("Content-type", "application/json");
                                    break;
                            }
                        }

                        if(this.acceptHeader != null) {
                            ((HttpPost)requestType).setHeader("Accept", this.acceptHeader);
                        }
                        ((HttpPost)requestType).setEntity(stringEntity);
                    } catch (UnsupportedEncodingException e) {
                    }

                    break;
                default: return null;
            }

            HttpClient httpclient = null;
            if(this.customTimeout != null) {
                HttpParams httpParameters = new BasicHttpParams();
                // Set the timeout in milliseconds until a connection is established.
                // The default value is zero, that means the timeout is not used.
                int timeoutConnection = this.customTimeout.ConnectionTimeOut;
                HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
                // Set the default socket timeout (SO_TIMEOUT)
                // in milliseconds which is the timeout for waiting for data.
                int timeoutSocket = this.customTimeout.SOTimeout;
                HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
                httpclient = new DefaultHttpClient(httpParameters);
            }
            else {
                httpclient = new DefaultHttpClient();
            }

            HttpResponse response;
            String responseString = null;
            try {
                response = httpclient.execute(requestType);
                StatusLine statusLine = response.getStatusLine();
                final int statusCode = statusLine.getStatusCode();
                if(statusCode == HttpStatus.SC_OK){
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    responseString = out.toString();
                } else{
                    //Closes the connection.
                    response.getEntity().getContent().close();

                    if(statusCode == HttpStatus.SC_UNAUTHORIZED) {
                        // do callback unauthorized
                        if(this.callback != null){
                            this.deferredAction = new DeferredAction() {
                                @Override
                                public void execute() {
                                    callback.onErrorUnauthorized();
                                }
                            };
                        }
                    }
                    else {
                        // do callback on general error.
                        if(this.callback != null) {
                            this.deferredAction = new DeferredAction() {
                                @Override
                                public void execute() {
                                    callback.onErrorGeneralRequest(statusCode);
                                }
                            };
                        }
                    }
                }
            } catch (final ClientProtocolException e) {
                if(this.callback != null){
                    this.deferredAction = new DeferredAction() {
                        @Override
                        public void execute() {
                            callback.onExceptionThrown(e);
                        }
                    };
                }

            } catch (final IOException e) {
                if(this.callback != null){
                    this.deferredAction = new DeferredAction() {
                        @Override
                        public void execute() {
                            callback.onExceptionThrown(e);
                        }
                    };
                }
            }
            return responseString;
        }*/
  }

  @Override
  protected void onPostExecute(String result) {
    super.onPostExecute(result);

    if (this.deferredAction != null) {
      deferredAction.execute();
    }

    if (result == null) {
      return;
    }

    // do callback for result
    if (this.callback != null)
      this.callback.onSuccess(result);
  }
}
