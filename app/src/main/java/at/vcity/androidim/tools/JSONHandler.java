package at.vcity.androidim.tools;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Vector;

import at.vcity.androidim.interfaces.IUpdateData;
import at.vcity.androidim.types.FriendInfo;
import at.vcity.androidim.types.MessageInfo;
import at.vcity.androidim.types.STATUS;

/**
 * Created by zzkuang on 5/29/2015.
 */
public  class JSONHandler
{
    private String userKey = new String();
    private IUpdateData updater;
    private Vector<FriendInfo> mFriends = new Vector<FriendInfo>();
    private Vector<FriendInfo> mOnlineFriends = new Vector<FriendInfo>();
    private Vector<FriendInfo> mUnapprovedFriends = new Vector<FriendInfo>();

    private Vector<MessageInfo> mUnreadMessages = new Vector<MessageInfo>();


    public JSONHandler(IUpdateData updater) {
        super();
        this.updater = updater;
    }

    public static void HandleJSON(IUpdateData updater,String json){
        try {
            JSONObject jObject = new JSONObject(json);

            String userKey = jObject.getJSONObject("user").getString(FriendInfo.USER_KEY);

            JSONArray friendarray=jObject.getJSONArray("friend");
            JSONArray messagearray=jObject.getJSONArray("message");

            Vector<FriendInfo> mFriends = new Vector<FriendInfo>();
            Vector<FriendInfo> mOnlineFriends = new Vector<FriendInfo>();
            Vector<FriendInfo> mUnapprovedFriends = new Vector<FriendInfo>();
            Vector<MessageInfo> mUnreadMessages = new Vector<MessageInfo>();

            for(int i=0;i<friendarray.length();i++){
                FriendInfo friend = new FriendInfo();
                JSONObject friendjson=friendarray.getJSONObject(i);
                friend.userName = friendjson.getString(FriendInfo.USERNAME);
                String status = friendjson.getString(FriendInfo.STATUS);
                friend.ip = friendjson.getString(FriendInfo.IP);
                friend.port = friendjson.getString(FriendInfo.PORT);
                friend.userKey = friendjson.getString(FriendInfo.USER_KEY);

                if (status != null && status.equals("online"))
                {
                    friend.status = STATUS.ONLINE;
                    mOnlineFriends.add(friend);
                }
                else if (status.equals("unApproved"))
                {
                    friend.status = STATUS.UNAPPROVED;
                    mUnapprovedFriends.add(friend);
                }
                else
                {
                    friend.status = STATUS.OFFLINE;
                    mFriends.add(friend);
                }
            }

            for(int i=0;i<messagearray.length();i++){
                MessageInfo message = new MessageInfo();
                JSONObject messagejson=messagearray.getJSONObject(i);
                message.userid = messagejson.getString(MessageInfo.USERID);
                message.sendt = messagejson.getString(MessageInfo.SENDT);
                message.content = messagejson.getString(MessageInfo.CONTENT);
                message.type=messagejson.getString(MessageInfo.TYPE);
                Log.i("MessageLOG", message.userid + message.sendt + message.content);
                mUnreadMessages.add(message);
            }
            FriendInfo[] friends = new FriendInfo[mFriends.size() + mOnlineFriends.size()];
            MessageInfo[] messages = new MessageInfo[mUnreadMessages.size()];

            int onlineFriendCount = mOnlineFriends.size();
            for (int i = 0; i < onlineFriendCount; i++)
            {
                friends[i] = mOnlineFriends.get(i);
            }

            int offlineFriendCount = mFriends.size();
            for (int i = 0; i < offlineFriendCount; i++)
            {
                friends[i + onlineFriendCount] = mFriends.get(i);
            }

            int unApprovedFriendCount = mUnapprovedFriends.size();
            FriendInfo[] unApprovedFriends = new FriendInfo[unApprovedFriendCount];

            for (int i = 0; i < unApprovedFriends.length; i++) {
                unApprovedFriends[i] = mUnapprovedFriends.get(i);
            }

            int unreadMessagecount = mUnreadMessages.size();
            //Log.i("MessageLOG", "mUnreadMessages="+unreadMessagecount );
            for (int i = 0; i < unreadMessagecount; i++)
            {
                messages[i] = mUnreadMessages.get(i);
                Log.i("MessageLOG", "i="+i );
            }

            updater.updateData(messages, friends, unApprovedFriends, userKey);
        }
        catch (JSONException e)
        {

        }
    }



}
