package at.vcity.androidim.interfaces;


import org.apache.http.util.ByteArrayBuffer;

public interface INetworkOperator {
	
	public String sendHttpRequest(String params);
    public String sendHttpData(String filename,String type,byte[] data);
    public ByteArrayBuffer getHttpData(String filename,String type);
}
