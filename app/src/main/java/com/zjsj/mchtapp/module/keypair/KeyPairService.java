package com.zjsj.mchtapp.module.keypair;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.health.UidHealthStats;
import android.support.annotation.Nullable;

import com.ruomm.base.http.config.ResponseText;
import com.ruomm.base.http.config.impl.TextHttpCallBack;
import com.ruomm.base.http.okhttp.TextOKHttp;
import com.ruomm.base.ioc.extend.BaseService;
import com.ruomm.base.ioc.extend.Thread_CanStop;
import com.ruomm.base.tools.Base64;
import com.ruomm.base.tools.RSAUtils;
import com.ruomm.baseconfig.debug.MLog;
import com.zjsj.mchtapp.config.http.ApiConfig;
import com.zjsj.mchtapp.config.http.AppStoreSafeImpl;
import com.zjsj.mchtapp.dal.event.KeyPairEvent;
import com.zjsj.mchtapp.dal.response.KeyPairDto;
import com.zjsj.mchtapp.dal.response.base.ResultFactory;

import org.greenrobot.eventbus.EventBus;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

public class KeyPairService extends BaseService {
    public static final int OptStart=1;
    public boolean isOnRequest=false;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        int value= getOptValue(intent);
        if(value==OptStart&&!isOnRequest&&!ApiConfig.isKeyPairOk())
        {
            new DownLoadThread().start();
        }
        else if(ApiConfig.isKeyPairOk()){
            stopSelf();
        }
    }

    private String uuid=ApiConfig.getAppUUID(this);
    class DownLoadThread extends Thread_CanStop{
        @Override
        public void run() {
            super.run();
            isOnRequest=true;
            getPublicKeyByUuid();
            final KeyPairEvent keyPairEvent=new KeyPairEvent();
            keyPairEvent.isKeyPairOk=ApiConfig.isKeyPairOk();
            EventBus.getDefault().post(keyPairEvent);
            Handler mHandler= new Handler(getMainLooper());
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    KeyPairService.this.stopSelf();
                }
            });
            mHandler=null;
            isOnRequest=false;
        }
    }
    private void getPublicKeyByUuid(){
        KeyPair keyPair= RSAUtils.generateRSAKeyPair();
        Map<String,String> map=new HashMap<>();
        map.put("uuid", uuid);
        map.put("keyType", ApiConfig.TRANSMIT_KEYTYPE);
        map.put("rasPublicKey", Base64.encode(keyPair.getPublic().getEncoded()));
        map.put("timeStamp", System.currentTimeMillis() + "");
        ResponseText responseText=
                new TextOKHttp().setUrl(ApiConfig.BASE_URL+"app/keypair/getPublicKeyByUuid").setRequestBodyText(map).doHttpSync(KeyPairDto.class, new TextHttpCallBack() {
                    @Override
                    public void httpCallBack(Object resultObject, String resultString, int status) {
                        MLog.i(resultString);
                    }
                });
        if(null==ResultFactory.getErrorTip(responseText)){
            KeyPairDto keyPairDto=ResultFactory.getResult(responseText);
            String pubKeyStr=Base64.encode(RSAUtils.decryptDataBig(Base64.decode(keyPairDto.publicKey),keyPair.getPrivate()));
            ApiConfig.loadTransmitKey(pubKeyStr);
        }
    }

}