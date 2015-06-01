package at.vcity.androidim.types;


public class MessageInfo {

	public static final String MESSAGE_LIST = "messageList";
	public static final String USERID = "from";
	public static final String SENDT = "sendt";
	public static final String CONTENT = "content";
	public static final String TYPE = "type";

	
	
	public String userid;
	public String sendt;
	public String content;
	public String type;
	public class MessageType{
		public static final String TEXT="text";
		public static final String IMAGE="image";
		public static final String Audio="audio";
	}
}
