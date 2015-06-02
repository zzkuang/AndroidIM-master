package at.vcity.androidim;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.UUID;

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
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import at.vcity.androidim.interfaces.IAppManager;
import at.vcity.androidim.services.IMService;
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
		
		setContentView(R.layout.messaging_screen); //messaging_screen);
				
		messageHistoryText = (EditText) findViewById(R.id.messageHistory);
		
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
					Thread thread = new Thread(){					
						public void run() {
							try {
								if (imService.sendMessage(imService.getUsername(), friend.userName, message.toString()) == null)
								{
									
									handler.post(new Runnable(){	

										public void run() {
											
									        Toast.makeText(getApplicationContext(),R.string.message_cannot_be_sent, Toast.LENGTH_LONG).show();

											
											//showDialog(MESSAGE_CANNOT_BE_SENT);										
										}
										
									});
								}
							} catch (UnsupportedEncodingException e) {
								Toast.makeText(getApplicationContext(),R.string.message_cannot_be_sent, Toast.LENGTH_LONG).show();

								e.printStackTrace();
							}
						}						
					};
					thread.start();
										
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
                String hashedname=sb.toString();
                try {
                    File ofile=new File(localstoragehandler.fileCacheFolder, hashedname+".jpg");
                    FileOutputStream fos = new FileOutputStream(ofile);
                    scaled.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                } catch (Exception e) {
                }

                scaled.recycle();
                System.gc();
            }

            /*
            for(int i=0;i<selectedimgpaths.size();i++)
            {
                //CreateThumbnailsAndPictures task=new CreateThumbnailsAndPictures(thumbnailAdapter);
                //task.execute(selectedimgpaths.get(i));
                compressAndMoveImage()
            }*/
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

    private Bitmap compressAndMoveImage(String imagePath){
        BitmapFactory.Options opt = new BitmapFactory.Options();
        Bitmap original = BitmapFactory.decodeFile(imagePath, opt);
        int originalwidth=original.getWidth();
        int originalheight=original.getHeight();

        float scalefactor=originalwidth>1024? 1024.0f/originalwidth:1;

        int targetdimx=(int)(originalwidth*scalefactor);
        int targetdimy=(int)(originalheight*scalefactor);


        Bitmap scaled = Bitmap.createBitmap(original, 0, 0, targetdimx, targetdimy,null, true);

        original.recycle();
        System.gc();

        return scaled;
    }
/*
    private class CreateThumbnailsAndPictures extends AsyncTask<String, Void, UUID> {
        private String filePath;
        public CreateThumbnailsAndPictures(){}
        @Override
        protected UUID doInBackground(String... params) {
            // String path = mContext.getExternalFilesDir(null).getPath() +
            // "/DemoFile.jpg";
            filePath = params[0];
            Bitmap original = null;
            try {
                BitmapFactory.Options opt = new BitmapFactory.Options();
                original = BitmapFactory.decodeFile(filePath, opt);
                int originalwidth=original.getWidth();
                int originalheight=original.getHeight();

                float scalefactor=originalwidth>1024? 1024.0f/originalwidth:1;

                int targetdimx=(int)(originalwidth*scalefactor);
                int targetdimy=(int)(originalheight*scalefactor);


                Bitmap scaledimg = Bitmap.createBitmap(original, 0, 0, targetdimx, targetdimy,null, true);

                original.recycle();
                System.gc();

                transformed.recycle();
                System.gc();

                //PropertyPicture.AddPropertyThumbnail(fileid, tn);
                //return fileid;


            } catch (Exception e) {
            }

            return null;
        }
    }
	*/
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
        if(type.equals(MessageInfo.MessageType.TEXT))
        {
            if (username != null && message != null) {
			    messageHistoryText.append(username + ":\n");
			    messageHistoryText.append(message + "\n");
		    }
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
