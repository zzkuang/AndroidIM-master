package at.vcity.androidim.interfaces;


import org.apache.http.util.ByteArrayBuffer;

public interface ISocketOperator {
	
	public String sendHttpRequest(String params);
    public String sendHttpData(String filename,String type,byte[] data);
    public ByteArrayBuffer getHttpData(String filename,String type);
	public int startListening(int port);
	public void stopListening();
	public void exit();
	public int getListeningPort();

}
