package com.netease.nertc.sendseimsg;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.netease.lava.api.IVideoRender;
import com.netease.lava.nertc.sdk.LastmileProbeResult;
import com.netease.lava.nertc.sdk.NERtc;
import com.netease.lava.nertc.sdk.NERtcCallbackEx;
import com.netease.lava.nertc.sdk.NERtcConstants;
import com.netease.lava.nertc.sdk.NERtcEx;
import com.netease.lava.nertc.sdk.NERtcOption;
import com.netease.lava.nertc.sdk.NERtcParameters;
import com.netease.lava.nertc.sdk.stats.NERtcAudioVolumeInfo;
import com.netease.lava.nertc.sdk.video.NERtcRemoteVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcVideoView;
import com.netease.nertc.config.DemoDeploy;

import java.util.ArrayList;
import java.util.Random;

public class SendSEIMsgActivity extends AppCompatActivity implements NERtcCallbackEx,View.OnClickListener{
    private static final String TAG = "SendSEIMsgActivity";

    private ArrayList<NERtcVideoView> mRemoteVideoList;
    private Button mStartJoinBtn;
    private EditText mRoomIdEt;
    private EditText mUserIdEt;
    private NERtcVideoView mLocalUserVv;

    private ImageView mBackIv;
    private RelativeLayout mContainer;
    private boolean mJoinChannel = false;
    private boolean mEnableLocalAudio = true;
    private boolean mEnableLocalVideo = true;
    private String mRoomId;
    private long mUserId;
    private Button mSendMsgBtn;
    private EditText etSeiMsg;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_seimsg);
        initView();
    }

    private void initView() {
        mRemoteVideoList = new ArrayList<>();
        mContainer = findViewById(R.id.rl_container);
        mBackIv = findViewById(R.id.iv_back);

        mStartJoinBtn = findViewById(R.id.btn_join_channel);
        mRoomIdEt = findViewById(R.id.et_room_id);
        mUserIdEt = findViewById(R.id.et_user_id);
        mLocalUserVv = findViewById(R.id.vv_local_user);
        mSendMsgBtn = findViewById(R.id.btn_send_msg);
        etSeiMsg = findViewById(R.id.et_sei_message);

        mRemoteVideoList.add((NERtcVideoView) findViewById(R.id.vv_remote_user_1));
        mRemoteVideoList.add((NERtcVideoView) findViewById(R.id.vv_remote_user_2));
        mRemoteVideoList.add((NERtcVideoView) findViewById(R.id.vv_remote_user_3));
        mRemoteVideoList.add((NERtcVideoView) findViewById(R.id.vv_remote_user_4));
        mUserId = new Random().nextInt(100000);
        mUserIdEt.setText(String.valueOf(mUserId));


        mStartJoinBtn.setOnClickListener(this);
        mBackIv.setOnClickListener(this);
        mSendMsgBtn.setOnClickListener(this);

    }
    /**
     * 设置本地音频可用性
     * @param enable
     */
    private void setLocalAudioEnable(boolean enable) {
        mEnableLocalAudio = enable;
        NERtcEx.getInstance().enableLocalAudio(mEnableLocalAudio);
    }

    /**
     * 设置本地视频的可用性
     */
    private void setLocalVideoEnable(boolean enable) {
        mEnableLocalVideo = enable;
        NERtcEx.getInstance().enableLocalVideo(mEnableLocalVideo);
        mLocalUserVv.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exit();
    }
    /**
     * 退出房间并关闭页面
     */
    private void exit(){
        if(mJoinChannel){
            leaveChannel();
        }
        finish();
    }
    private boolean leaveChannel(){

        mJoinChannel = false;
        setLocalAudioEnable(false);
        setLocalVideoEnable(false);
        int ret = NERtcEx.getInstance().leaveChannel();
        return ret == NERtcConstants.ErrorCode.OK;
    }
    private void userInfo() {
        Editable roomIdEdit = mRoomIdEt.getText();
        if(roomIdEdit == null || roomIdEdit.length() <= 0){
            return;
        }
        Editable userIdEdit = mUserIdEt.getText();
        if(userIdEdit == null || userIdEdit.length() <= 0){
            return;
        }
        mRoomId = roomIdEdit.toString();
        mUserId = Long.parseLong(userIdEdit.toString());
    }
    private void setupNERtc() {
        NERtcParameters parameters = new NERtcParameters();
        NERtcEx.getInstance().setParameters(parameters); //先设置参数，后初始化

        NERtcOption options = new NERtcOption();

        if (BuildConfig.DEBUG) {
            options.logLevel = NERtcConstants.LogLevel.INFO;
        } else {
            options.logLevel = NERtcConstants.LogLevel.WARNING;
        }

        try {
            NERtcEx.getInstance().init(getApplicationContext(), DemoDeploy.APP_KEY, this, options);
        } catch (Exception e) {
            // 可能由于没有release导致初始化失败，release后再试一次
            NERtcEx.getInstance().release();
            try {
                NERtcEx.getInstance().init(getApplicationContext(), DemoDeploy.APP_KEY, this, options);
            } catch (Exception ex) {
                Toast.makeText(this, "SDK初始化失败", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        setLocalAudioEnable(true);
        setLocalVideoEnable(true);
    }
    private void setuplocalVideo() {
        mLocalUserVv.setZOrderMediaOverlay(false);
        mLocalUserVv.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);
        NERtcEx.getInstance().setupLocalVideoCanvas(mLocalUserVv);
    }
    private void setupRemoteVideo(long userId, int index) {
        mRemoteVideoList.get(index).setZOrderMediaOverlay(true);
        mRemoteVideoList.get(index).setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);
        NERtc.getInstance().setupRemoteVideoCanvas(mRemoteVideoList.get(index), userId);
    }
    private void joinChannel(String roomId, long userId) {
        NERtcEx.getInstance().joinChannel("", roomId, userId);
    }
    private void startPushStream() {
        if(!mJoinChannel) {
            userInfo();
            setupNERtc();
            setuplocalVideo();
            joinChannel(mRoomId, mUserId);
        }
    }
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.iv_back){
            exit();
        }else if(id == R.id.btn_join_channel){
            startPushStream();
        }else if(id == R.id.btn_send_msg){
            Editable seiMsgEdit = etSeiMsg.getText();
            if(seiMsgEdit == null || seiMsgEdit.length() <= 0){
                return;
            }
            int ret = NERtcEx.getInstance().sendSEIMsg(seiMsgEdit.toString());
            if(ret == 0){
                Toast.makeText(this,"已发送："+ seiMsgEdit.toString(),Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onJoinChannel(int result, long channelId, long elapsed, long l2) {
        Log.i(TAG, "onJoinChannel result: " + result + " channelId: " + channelId + " elapsed: " + elapsed);
        if(result == NERtcConstants.ErrorCode.OK){
            mJoinChannel = true;
        }
    }

    @Override
    public void onLeaveChannel(int result) {
        Log.i(TAG, "onLeaveChannel result: " + result);
        NERtc.getInstance().release();
        finish();
    }

    @Override
    public void onUserJoined(long userId) {
        Log.i(TAG, "onUserJoined userId: " + userId);
        for(int i = 0;i < mRemoteVideoList.size();i++){
            Log.i(TAG, "onUserJoined i: " + i);
            if(mRemoteVideoList.get(i).getTag() == null){
                setupRemoteVideo(userId, i);
                mRemoteVideoList.get(i).setTag(userId);
                break;
            }
        }
    }

    @Override
    public void onUserLeave(long userId, int i) {
        Log.i(TAG, "onUserLeave uid: " + userId);
        NERtcVideoView userView = mContainer.findViewWithTag(userId);
        if(userView != null){
            //设置TAG为null，代表当前没有订阅
            userView.setTag(null);
            NERtcEx.getInstance().subscribeRemoteVideoStream(userId, NERtcRemoteVideoStreamType.kNERtcRemoteVideoStreamTypeHigh, false);
            //不展示远端
            userView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserAudioStart(long l) {

    }

    @Override
    public void onUserAudioStop(long l) {

    }

    @Override
    public void onUserVideoStart(long userId, int profile) {
        Log.i(TAG, "onUserVideoStart uid: " + userId + " profile: " + profile);

        NERtcVideoView userView = mContainer.findViewWithTag(userId);
        NERtcEx.getInstance().subscribeRemoteVideoStream(userId, NERtcRemoteVideoStreamType.kNERtcRemoteVideoStreamTypeHigh, true);
        userView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onUserVideoStop(long userId) {
        Log.i(TAG, "onUserVideoStop, uid=" + userId);
        NERtcVideoView userView = mContainer.findViewWithTag(userId);
        if(userView != null){
            userView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onDisconnect(int i) {

    }

    @Override
    public void onClientRoleChange(int i, int i1) {

    }

    @Override
    public void onUserSubStreamVideoStart(long l, int i) {

    }

    @Override
    public void onUserSubStreamVideoStop(long l) {

    }

    @Override
    public void onUserAudioMute(long l, boolean b) {

    }

    @Override
    public void onUserVideoMute(long l, boolean b) {

    }

    @Override
    public void onUserVideoMute(NERtcVideoStreamType neRtcVideoStreamType, long l, boolean b) {

    }

    @Override
    public void onFirstAudioDataReceived(long l) {

    }

    @Override
    public void onFirstVideoDataReceived(long l) {

    }

    @Override
    public void onFirstVideoDataReceived(NERtcVideoStreamType neRtcVideoStreamType, long l) {

    }

    @Override
    public void onFirstAudioFrameDecoded(long l) {

    }

    @Override
    public void onFirstVideoFrameDecoded(long l, int i, int i1) {

    }

    @Override
    public void onFirstVideoFrameDecoded(NERtcVideoStreamType neRtcVideoStreamType, long l, int i, int i1) {

    }

    @Override
    public void onUserVideoProfileUpdate(long l, int i) {

    }

    @Override
    public void onAudioDeviceChanged(int i) {

    }

    @Override
    public void onAudioDeviceStateChange(int i, int i1) {

    }

    @Override
    public void onVideoDeviceStageChange(int i) {

    }

    @Override
    public void onConnectionTypeChanged(int i) {

    }

    @Override
    public void onReconnectingStart() {

    }

    @Override
    public void onReJoinChannel(int i, long l) {

    }

    @Override
    public void onAudioMixingStateChanged(int i) {

    }

    @Override
    public void onAudioMixingTimestampUpdate(long l) {

    }

    @Override
    public void onAudioEffectFinished(int i) {

    }

    @Override
    public void onLocalAudioVolumeIndication(int i) {

    }

    @Override
    public void onLocalAudioVolumeIndication(int i, boolean b) {

    }

    @Override
    public void onRemoteAudioVolumeIndication(NERtcAudioVolumeInfo[] neRtcAudioVolumeInfos, int i) {

    }

    @Override
    public void onLiveStreamState(String s, String s1, int i) {

    }

    @Override
    public void onConnectionStateChanged(int i, int i1) {

    }

    @Override
    public void onCameraFocusChanged(Rect rect) {

    }

    @Override
    public void onCameraExposureChanged(Rect rect) {

    }

    @Override
    public void onRecvSEIMsg(long userId, String seiMsg) {
        Toast.makeText(this, "用户ID:" + userId + "   发送SEI消息:   " + seiMsg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAudioRecording(int i, String s) {

    }

    @Override
    public void onError(int i) {

    }

    @Override
    public void onWarning(int i) {

    }

    @Override
    public void onMediaRelayStatesChange(int i, String s) {

    }

    @Override
    public void onMediaRelayReceiveEvent(int i, int i1, String s) {

    }

    @Override
    public void onLocalPublishFallbackToAudioOnly(boolean b, NERtcVideoStreamType neRtcVideoStreamType) {

    }

    @Override
    public void onRemoteSubscribeFallbackToAudioOnly(long l, boolean b, NERtcVideoStreamType neRtcVideoStreamType) {

    }

    @Override
    public void onLastmileQuality(int i) {

    }

    @Override
    public void onLastmileProbeResult(LastmileProbeResult lastmileProbeResult) {

    }

    @Override
    public void onMediaRightChange(boolean b, boolean b1) {

    }

    @Override
    public void onVirtualBackgroundSourceEnabled(boolean b, int i) {

    }

    @Override
    public void onUserSubStreamAudioStart(long l) {

    }

    @Override
    public void onUserSubStreamAudioStop(long l) {

    }

    @Override
    public void onUserSubStreamAudioMute(long l, boolean b) {

    }

    @Override
    public void onLocalVideoWatermarkState(NERtcVideoStreamType neRtcVideoStreamType, int i) {

    }
}