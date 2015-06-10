package at.vcity.androidim.communication;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.util.Log;

import org.apache.http.util.ByteArrayBuffer;

import at.vcity.androidim.interfaces.IAppManager;
import at.vcity.androidim.interfaces.INetworkOperator;


public class NetworkOperator implements INetworkOperator
{
	//private static final String AUTHENTICATION_SERVER_ADDRESS = "http://192.168.0.54/android-im/"; //TODO change to your WebAPI Address
	// private static final String AUTHENTICATION_SERVER_ADDRESS = "http://192.168.33.64:9986"; //TODO change to your WebAPI Address
    private static final String AUTHENTICATION_SERVER_ADDRESS = "http://10.88.26.103:9986"; //TODO change to your WebAPI Address
    private static final String SEND_DATA_SERVER_ADDRESS = "http://10.88.26.103:9987";
    private static final String GET_DATA_SERVER_ADDRESS = "http://10.88.26.103:9988";

    private int listeningPort = 0;
	
	private static final String HTTP_REQUEST_FAILED = null;
	

	private boolean listening;



	public NetworkOperator(IAppManager appManager) {
	}
	
	
	public String sendHttpRequest(String params)
	{		
		URL url;
		String result = new String();
		try 
		{
			url = new URL(AUTHENTICATION_SERVER_ADDRESS);
			HttpURLConnection connection;
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			
			PrintWriter out = new PrintWriter(connection.getOutputStream());
			
			out.println(params);
			out.close();

			BufferedReader in = new BufferedReader(
					new InputStreamReader(
							connection.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				result = result.concat(inputLine);				
			}
			in.close();			
		} 
		catch (MalformedURLException e) {
			e.printStackTrace();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}			
		
		if (result.length() == 0) {
			result = HTTP_REQUEST_FAILED;
		}
		
		return result;
	}

    public ByteArrayBuffer getHttpData(String filename,String type){
        URL url;
        String result = new String();
        try
        {
            url = new URL(GET_DATA_SERVER_ADDRESS);
            HttpURLConnection connection;
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true); // Allow Inputs
            connection.setDoOutput(true); // Allow Outputs
            connection.setUseCaches(false); // Don't use a Cached Copy
            connection.setRequestMethod("POST");
            connection.setRequestProperty("filename", filename);

            InputStream is=connection.getInputStream();

            BufferedInputStream bufferinstream = new BufferedInputStream(is);

            ByteArrayBuffer baf = new ByteArrayBuffer(5000);
            int current = 0;
            while((current = bufferinstream.read()) != -1){
                baf.append((byte) current);
            }
            return baf;
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        if (result.length() == 0) {
            result = HTTP_REQUEST_FAILED;
        }

        return null;
    }

    public String sendHttpData(String filename,String type,byte[] buffer){
        URL url;
        String result = new String();
        try
        {
            url = new URL(SEND_DATA_SERVER_ADDRESS);
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;


            HttpURLConnection connection;
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true); // Allow Inputs
            connection.setDoOutput(true); // Allow Outputs
            connection.setUseCaches(false); // Don't use a Cached Copy
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("ENCTYPE", "multipart/form-data");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            connection.setRequestProperty("uploaded_file", filename);
            String[] checksum=filename.split("\\.");
            connection.setRequestProperty("checksum", checksum[0]);

            dos = new DataOutputStream(connection.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                            + filename + "\"" + lineEnd);
            dos.writeBytes(lineEnd);

            // create a buffer of  maximum size
            bytesAvailable = buffer.length;
            int maxBufferSize = 5 * 1024 * 1024;

            bufferSize = Math.min(bytesAvailable, maxBufferSize);

            // read file and write it into form...
            //bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            //while (bytesRead > 0) {

             dos.write(buffer, 0, bufferSize);
              //  bytesAvailable = fileInputStream.available();
               // bufferSize = Math.min(bytesAvailable, maxBufferSize);
               // bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            //}

            // send multipart form data necesssary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // Responses from the server (code and message)
            int serverResponseCode = connection.getResponseCode();
            result = connection.getResponseMessage();

            Log.i("uploadFile", "HTTP Response is : "
                    + result + ": " + serverResponseCode);
/*
            if(serverResponseCode == 200){

                runOnUiThread(new Runnable() {
                    public void run() {

                        String msg = "File Upload Completed.\n\n See uploaded file here : \n\n"
                                +" http://www.androidexample.com/media/uploads/"
                                +uploadFileName;

                        messageText.setText(msg);
                        Toast.makeText(UploadToServer.this, "File Upload Complete.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
*/
            //close the streams //
            dos.flush();
            dos.close();
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        if (result.length() == 0) {
            result = HTTP_REQUEST_FAILED;
        }

        return result;
    }
}
