package at.vcity.androidim;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.UUID;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.util.ByteArrayBuffer;

import at.vcity.androidim.interfaces.IAppManager;
import at.vcity.androidim.services.IMService;
import at.vcity.androidim.tools.BitmapLoaderTask;
import at.vcity.androidim.tools.FriendController;
import at.vcity.androidim.tools.LocalStorageHandler;
import at.vcity.androidim.types.FriendInfo;
import at.vcity.androidim.types.MessageInfo;

/*
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;
*/

public class Messaging extends Activity {

	private static final int MESSAGE_CANNOT_BE_SENT = 0;
    private static final int GALLERY_ACTIVITY=1;
	public String username;
	private EditText messageText;
	private EditText messageHistoryText;
	private Button sendMessageButton;
	private Button sendImageButton;
	private IAppManager imService;
    private ScrollView messageHistoryScroll;
    private LinearLayout messageHistoryLayout;
	private FriendInfo friend = new FriendInfo();
	private LocalStorageHandler localstoragehandler; 
	private Cursor dbCursor;
	
	private ServiceConnection mConnection = new ServiceConnection() {


		
		
		public void onServiceConnected(ComponentName className, IBinder service) {          
            imService = ((IMService.IMBinder)service).getService();
        }
        public void onServiceDisconnected(ComponentName className) {
        	imService = null;
            Toast.makeText(Messaging.this, R.string.local_service_stopped,
                    Toast.LENGTH_SHORT).show();
        }
    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);


        bindService(new Intent(Messaging.this, IMService.class), mConnection , Context.BIND_AUTO_CREATE);

        IntentFilter i = new IntentFilter();
        i.addAction(IMService.TAKE_MESSAGE);

        registerReceiver(messageReceiver, i);

        FriendController.setActiveFriend(friend.userName);



        setContentView(R.layout.messaging_screen); //messaging_screen);
				
		//messageHistoryText = (EditText) findViewById(R.id.messageHistory);
        messageHistoryScroll=(ScrollView) findViewById(R.id.messageScroll);
        messageHistoryLayout=(LinearLayout)findViewById(R.id.messageHistory2);

		messageText = (EditText) findViewById(R.id.message);
		
		messageText.requestFocus();			
		
		sendMessageButton = (Button) findViewById(R.id.sendMessageButton);
		sendImageButton = (Button ) findViewById(R.id.sendImageButton);


		Bundle extras = this.getIntent().getExtras();
		
		
		friend.userName = extras.getString(FriendInfo.USERNAME);
		friend.ip = extras.getString(FriendInfo.IP);
		friend.port = extras.getString(FriendInfo.PORT);
		String msg = extras.getString(MessageInfo.CONTENT);
        String msgtype=extras.getString(MessageInfo.TYPE);

		setTitle("Messaging with " + friend.userName);

		localstoragehandler = new LocalStorageHandler(this);
		dbCursor = localstoragehandler.get(friend.userName, IMService.USERNAME );
		
		if (dbCursor.getCount() > 0){
		int noOfScorer = 0;
		dbCursor.moveToFirst();
		    while ((!dbCursor.isAfterLast())&&noOfScorer<dbCursor.getCount()) 
		    {
		        noOfScorer++;

				this.appendToMessageHistory(dbCursor.getString(2) , dbCursor.getString(3), dbCursor.getString(4));
		        dbCursor.moveToNext();
		    }
		}
		localstoragehandler.close();
		
		if (msg != null) 
		{
			this.appendToMessageHistory(friend.userName, msg,msgtype);
			((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel((friend.userName+msg).hashCode());
		}

		sendImageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(Messaging.this, Gallery.class);
				Messaging.this.startActivityForResult(i, GALLERY_ACTIVITY);
			}
		});

		sendMessageButton.setOnClickListener(new OnClickListener(){
			CharSequence message;
			Handler handler = new Handler();
			public void onClick(View arg0) {
				message = messageText.getText();
				if (message.length()>0) 
				{		
					appendToMessageHistory(imService.getUsername(), message.toString(), MessageInfo.MessageType.TEXT);
					
					localstoragehandler.insert(imService.getUsername(), friend.userName, message.toString(),MessageInfo.MessageType.TEXT);
								
					messageText.setText("");
                    SendMessageTask sendMessageTask=new SendMessageTask(message.toString(),MessageInfo.MessageType.TEXT);
                    sendMessageTask.execute();
					/*Thread thread = new Thread(){
						public void run() {

						}
					};
					thread.start();*/
										
				}
				
			}});
		
		messageText.setOnKeyListener(new OnKeyListener(){
			public boolean onKey(View v, int keyCode, KeyEvent event) 
			{
				if (keyCode == 66){
					sendMessageButton.performClick();
					return true;
				}
				return false;
			}
			
			
		});
				
	}

    private class SendMessageTask extends AsyncTask<Void, Void, String>{

        String message;
        String type;
        public SendMessageTask(String msg, String t){
            message=msg;
            type=t;
        }

        @Override
        protected String doInBackground(Void... n) {

            try {
                return imService.sendMessage(imService.getUsername(), friend.userName, message, type);
            } catch (UnsupportedEncodingException e) {
                Toast.makeText(getApplicationContext(),R.string.message_cannot_be_sent, Toast.LENGTH_LONG).show();

                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            //super.onPostExecute(s);
            Handler handler = new Handler();
            if(s==null){
                handler.post(new Runnable(){
                    public void run() {
                        Toast.makeText(getApplicationContext(),R.string.message_cannot_be_sent, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }


	@Override
	protected Dialog onCreateDialog(int id) {
		int message = -1;
		switch (id)
		{
		case MESSAGE_CANNOT_BE_SENT:
			message = R.string.message_cannot_be_sent;
		break;
		}
		
		if (message == -1)
		{
			return null;
		}
		else
		{
			return new AlertDialog.Builder(Messaging.this)       
			.setMessage(message)
			.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					/* User clicked OK so do some stuff */
				}
			})        
			.create();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(messageReceiver);
		unbindService(mConnection);
		
		FriendController.setActiveFriend(null);
		
	}

	@Override
	protected void onResume() 
	{		
		super.onResume();
		bindService(new Intent(Messaging.this, IMService.class), mConnection , Context.BIND_AUTO_CREATE);
				
		IntentFilter i = new IntentFilter();
		i.addAction(IMService.TAKE_MESSAGE);
		
		registerReceiver(messageReceiver, i);
		
		FriendController.setActiveFriend(friend.userName);		
		
		
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==GALLERY_ACTIVITY && resultCode== RESULT_OK)
        {
            ArrayList<String> selectedImagePaths = data.getExtras().getStringArrayList("ImagePaths");
            for(String s:selectedImagePaths ){

                Bitmap scaled = compressAndMoveImage(s);

                ByteArrayOutputStream imageByteStream = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.PNG,100,imageByteStream);

                byte[] imageByteArray=imageByteStream.toByteArray();
                MessageDigest md = getMessageDigest();
                byte[] hash=md.digest(imageByteArray);
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < hash.length; i++) {
                    sb.append(Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(1));
                }

                String hashedname=sb.toString()+".jpg";
                try {
                    File file=new File(localstoragehandler.fileCacheFolder, hashedname);
                    FileOutputStream fos = new FileOutputStream(file);
                    scaled.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                } catch (Exception e) {}

                appendToMessageHistory(imService.getUsername(), hashedname, MessageInfo.MessageType.IMAGE);
                localstoragehandler.insert(imService.getUsername(), friend.userName, hashedname,MessageInfo.MessageType.IMAGE);

                scaled.recycle();
                System.gc();


                UploadDataTask uploadDataTask=new UploadDataTask(hashedname, MessageInfo.MessageType.IMAGE);
                uploadDataTask.execute(imageByteArray);
            }
        }
    }

    class UploadDataTask extends AsyncTask<byte[],Void,String>
    {
        String hashedname;
        String type;
        public UploadDataTask(String hashname, String t)
        {
            hashedname=hashname;
            type=t;
        }

        @Override
        protected String doInBackground(byte[]... dataByteArray){
            String result="";
            try{
                result = imService.sendData(hashedname, type, dataByteArray[0]);
            }catch (UnsupportedEncodingException e) {

            }
            if(result.equals("OK"))
                return result;
            else
                return "Failed";
        }

        @Override
        protected void onPostExecute(String result)
        {
            if(result.equals("OK")){
                SendMessageTask sendMessageTask=new SendMessageTask(hashedname.toString(),MessageInfo.MessageType.IMAGE);
                sendMessageTask.execute();
            }else{
                Toast.makeText(getApplicationContext(),R.string.message_cannot_be_sent, Toast.LENGTH_LONG).show();
            }

        }
    }


    // for command hash computation
    private MessageDigest messageDigest = null;
    private MessageDigest getMessageDigest() {
        if (messageDigest != null)
            return messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
            return messageDigest;
        } catch (Exception e) {
            // should throw exception
            return null;
        }
    }

    private Bitmap  compressAndMoveImage(String imagePath){
        BitmapFactory.Options opt = new BitmapFactory.Options();
        Bitmap original = BitmapFactory.decodeFile(imagePath, opt);
        int originalwidth=original.getWidth();
        int originalheight=original.getHeight();

        float scalefactor=originalwidth>1024? 1024.0f/originalwidth:1;

        int targetdimx=(int)(originalwidth*scalefactor);
        int targetdimy=(int)(originalheight*scalefactor);
        Bitmap scaled = Bitmap.createScaledBitmap(original, targetdimx, targetdimy, false);
        original.recycle();
        System.gc();
        return scaled;

    }
	public class  MessageReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) 
		{		
			Bundle extra = intent.getExtras();
			String username = extra.getString(MessageInfo.USERID);			
			String content = extra.getString(MessageInfo.CONTENT);
			String type = extra.getString(MessageInfo.TYPE);
			if (username != null && content != null)
			{
				if (friend.userName.equals(username)) {
					appendToMessageHistory(username, content,type);
					localstoragehandler.insert(username,imService.getUsername(), content,type);
				}
				else {
					if (content.length() > 15) {
                        content = content.substring(0, 15);
					}
                    if(type.equals(MessageInfo.MessageType.TEXT)){
                        Toast.makeText(Messaging.this,  username + " says '"+
                                        content + "'",
                                Toast.LENGTH_SHORT).show();
                    }else if(type.equals(MessageInfo.MessageType.IMAGE)){
                        Toast.makeText(Messaging.this,  username + " sent you an image.",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(Messaging.this,  username + " sent you an voice clip.",Toast.LENGTH_SHORT).show();
                    }
                }
			}			
		}
		
	};
	private MessageReceiver messageReceiver = new MessageReceiver();
	
	public  void appendToMessageHistory(String username, String message, String type) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 5;
        if (username.equals(IMService.USERNAME)) {
            params.gravity = Gravity.RIGHT;
            params.rightMargin = 10;
        } else {
            params.gravity = Gravity.LEFT;
            params.leftMargin = 10;
        }
        if (type.equals(MessageInfo.MessageType.TEXT)) {
            if (username != null && message != null) {
//			    messageHistoryText.append(username + ":\n");
//			    messageHistoryText.append(message + "\n");

                TextView textView = new TextView(this);
                textView.setText(message);

                if (username.equals(IMService.USERNAME)) {
                    textView.setGravity(Gravity.RIGHT);
                    textView.setBackgroundColor(Color.parseColor("#00FF00"));
                } else {
                    textView.setGravity(Gravity.LEFT);
                    textView.setBackgroundColor(Color.parseColor("#B0B0B0"));
                }
                textView.setLayoutParams(params);
                messageHistoryLayout.addView(textView);
            }
        } else {
            //String[] strings = {message, type};
            //ShowDataObjectTask showDataObjectTask = new ShowDataObjectTask(this,message,type,params);
            //showDataObjectTask.execute();
        }
    }

    class ShowDataObjectTask extends AsyncTask<Void,Void,String>{
        Context context;
        String filename;
        String type;
        LinearLayout.LayoutParams params;
        boolean fileExists=false;

        public ShowDataObjectTask(Context c,String fn,String t, LinearLayout.LayoutParams ll){
            context=c;
            params=ll;
            filename=fn;
            type=t;
        }

        @Override
        protected String doInBackground(Void... voids) {
            String imagePath=localstoragehandler.fileCacheFolder+"/"+filename;
            File file=new File(imagePath);
            fileExists=file.exists();
            if(!fileExists){
                ByteArrayBuffer buffer=imService.getData(filename, type);
                if(buffer!=null){
                try{
                    FileOutputStream fos = new FileOutputStream( localstoragehandler.fileCacheFolder+"/"+filename);
                    try{
                        fos.write(buffer.toByteArray());
                        fos.flush();
                        fos.close();
                        return "OK";
                    }
                    catch (IOException e){

                    }
                }
                catch (FileNotFoundException e){
                }
                }
                return "Shit";
            }
            else{
                return "OK";
            }
        }

        @Override
        protected void onPostExecute(String status) {
            String imagePath=localstoragehandler.fileCacheFolder+"/"+filename;
            if(status.equals("OK")){
                if(type.equals(MessageInfo.MessageType.IMAGE)){
                    ImageView imageView = new ImageView(context);
                    BitmapLoaderTask task=new BitmapLoaderTask(imageView);
                    task.execute(imagePath);
                    imageView.setLayoutParams(params);
                    messageHistoryLayout.addView(imageView);
                }
            }
            else{

            }
            //super.onPostExecute(aVoid);
        }
    }
	
	
	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    if (localstoragehandler != null) {
	    	localstoragehandler.close();
	    }
	    if (dbCursor != null) {
	    	dbCursor.close();
	    }
	}
	

}
