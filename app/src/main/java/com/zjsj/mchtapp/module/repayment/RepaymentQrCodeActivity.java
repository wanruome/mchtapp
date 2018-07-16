package com.zjsj.mchtapp.module.repayment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.ruomm.base.http.okhttp.TextOKHttp;
import com.ruomm.base.ioc.annotation.view.InjectAll;
import com.ruomm.base.ioc.annotation.view.InjectView;
import com.ruomm.base.ioc.extend.Thread_CanStop;
import com.ruomm.base.tools.DisplayUtil;
import com.ruomm.base.tools.StringUtils;
import com.ruomm.base.tools.ToastUtil;
import com.ruomm.base.view.dialog.BaseDialogClickListener;
import com.ruomm.base.view.menutopview.MenuTopListener;
import com.ruomm.base.view.percentview.FrameLayout_PercentHeight;
import com.ruomm.base.zxing.ZxingCreateUtil;
import com.ruomm.baseconfig.debug.MLog;
import com.ruomm.resource.ui.AppMultiActivity;
import com.zjsj.mchtapp.R;
import com.zjsj.mchtapp.config.LoginUserFactory;
import com.zjsj.mchtapp.config.http.ApiConfig;
import com.zjsj.mchtapp.config.impl.TextHttpCallBack;
import com.zjsj.mchtapp.dal.response.RepaymentBankCard;
import com.zjsj.mchtapp.dal.response.RepaymentBindCardDto;
import com.zjsj.mchtapp.dal.response.RepaymentQrCodeDto;
import com.zjsj.mchtapp.dal.response.base.ResultDto;
import com.zjsj.mchtapp.dal.response.base.ResultFactory;
import com.zjsj.mchtapp.dal.store.UserBankCardForQrCode;
import com.zjsj.mchtapp.view.BankCardWheelDialog;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class RepaymentQrCodeActivity extends AppMultiActivity {
    @InjectAll
    Views views=new Views();
    class Views{
        @InjectView(id=R.id.qrly_barcode)
        FrameLayout_PercentHeight qrly_barcode;
        @InjectView(id=R.id.qrly_qrcode)
        FrameLayout_PercentHeight qrly_qrcode;
        @InjectView(id=R.id.qrimg_barcode)
        ImageView qrimg_barcode;
        @InjectView(id=R.id.qrimg_qrcode)
        ImageView qrimg_qrcode;
        @InjectView(id=R.id.qrly_cardbanks)
        RelativeLayout qrly_cardbanks;
        @InjectView(id=R.id.qrtext_qrno)
        TextView qrtext_qrno;
        @InjectView(id=R.id.qrtext_cardbank)
        TextView qrtext_cardbank;
        @InjectView(id=R.id.qrtext_validtime)
        TextView qrtext_validtime;
    }
    private List<RepaymentBankCard> listBankCards;
    private RepaymentBankCard bankCardForQrCode;
    private BankCardWheelDialog bankCardWheelDialog;
    Bitmap bitmapBarCode=null;
    Bitmap bitmapQrCode=null;
    private long lastQueryTime=0;
    private boolean isOnQueryQrNo=false;
    private QueryQrcodeThread queryQrcodeThread=new QueryQrcodeThread();
    private String qrNo=null;
    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setMenuTop();
        setInitContentView(R.layout.repayment_qrcode_lay);
        views.qrly_barcode.setHeightPercent(0.25f);
        int dispalyWidth=DisplayUtil.getDispalyWidth(mContext);
        int qrcode_width= dispalyWidth-getResources().getDimensionPixelSize(R.dimen.dpx032)*2-getResources().getDimensionPixelSize(R.dimen.dpx050)*2;

        views.qrly_qrcode.setHeightPercent(qrcode_width*1.0f/dispalyWidth);
        setDataForViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        queryQrcodeThread.stopTask();
        queryQrcodeThread=null;
    }

    private void setMenuTop(){

        mymenutop.setCenterText("付款码");
        mymenutop.setMenuClickListener(new MenuTopListener() {
            @Override
            public void onMenuTopClick(View v, int vID) {
                if(vID== R.id.menutop_left)
                {
                    finish();
                }
            }
        });
    }
    private void setDataForViews(){
        listBankCards= LoginUserFactory.getBankCardInfo();
        if(null==listBankCards||listBankCards.size()<=0)
        {
            ToastUtil.makeFailToastThr(mContext,"你还没有绑定银行卡");
            finish();
            return;
        }
        bankCardForQrCode=LoginUserFactory.getBankCardForQrCode(listBankCards);
        if(null==bankCardForQrCode)
        {
            ToastUtil.makeFailToastThr(mContext,"你还没有绑定银行卡");
            finish();
            return;
        }
        views.qrtext_qrno.setText(" ");
        views.qrly_cardbanks.setOnClickListener(myOnClickListener);
        views.qrtext_validtime.setOnClickListener(myOnClickListener);
        views.qrtext_cardbank.setText(bankCardForQrCode.accountNo);
        queryQrcodeThread.start();
    }
    private View.OnClickListener myOnClickListener=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int vID=v.getId();
            if(vID==R.id.qrly_cardbanks)
            {
                bankCardWheelDialog=new BankCardWheelDialog(mContext, listBankCards,bankCardForQrCode, new BaseDialogClickListener() {
                    @Override
                    public void onDialogItemClick(View v, Object tag) {
                        int vID=v.getId();
                        if(vID==R.id.wheelbtn_over)
                        {
                            RepaymentBankCard tmpBankCard=(RepaymentBankCard) v.getTag();
                            if(null!=tmpBankCard&&!tmpBankCard.cardIndex.equals(bankCardForQrCode.cardIndex)){
                                LoginUserFactory.saveBankCardForQrCode(tmpBankCard);
                                lastQueryTime=SystemClock.elapsedRealtime()-60*1000;
                                bankCardForQrCode=tmpBankCard;

                                lastQueryTime=0;
                                views.qrtext_cardbank.setText(bankCardForQrCode.accountNo);

                            }
                        }


                    }
                });
                bankCardWheelDialog.show();
            }
            else if(vID==R.id.qrtext_validtime&&StringUtils.isBlank(qrNo))
            {
                doHttpGetQrCode();
            }
        }
    };
    private void doHttpGetQrCode(){
        if(isOnQueryQrNo)
        {
            return;
        }
        if(null!=bankCardWheelDialog&&bankCardWheelDialog.isShowing())
        {
            return;
        }
        isOnQueryQrNo=true;
        showLoading();
        qrNo=null;
        createQrCode();
        Map<String,String> map= ApiConfig.createRequestMap(true);
        map.put("cardIndex",bankCardForQrCode.cardIndex);
        ApiConfig.signRequestMap(map);
        new TextOKHttp().setUrl(ApiConfig.BASE_URL+"app/repayment/doCallQrcode").setRequestBodyText(map).doHttp(RepaymentQrCodeDto.class, new TextHttpCallBack() {
            @Override
            public void httpCallBack(Object resultObject, String resultString, int status) {
                isOnQueryQrNo=false;
                lastQueryTime=SystemClock.elapsedRealtime();
                String errMsg= ResultFactory.getErrorTip(resultObject,status);
                if(!StringUtils.isEmpty(errMsg)){
                    ResultDto resultDto=(ResultDto) resultObject;
                    RepaymentQrCodeDto repaymentQrCodeDto=ResultFactory.getResult(resultObject,status);
                    if(null!=resultDto&&ResultFactory.ERR_REPAYMENT.equals(resultDto.code))
                    {
                        errMsg=null==repaymentQrCodeDto?errMsg:repaymentQrCodeDto.responseRemark;
                    }
                    ToastUtil.makeFailToastThr(mContext,errMsg);
//                    views.qrtext_qrno.setText(" ");
                    qrNo=null;
                    createQrCode();

                }
                else{
                    RepaymentQrCodeDto repaymentQrCodeDto=ResultFactory.getResult(resultObject,status);
                    MLog.i(repaymentQrCodeDto);
                    qrNo=repaymentQrCodeDto.qrNo;
                    if(new Random().nextInt(10)>7)
                    {
                        qrNo=null;
                    }
                    createQrCode();

                }
                dismissLoading();
            }
        });
    }
    class QueryQrcodeThread extends Thread_CanStop{
        @Override
        public void run() {
            super.run();
            while (true&&!isStopTask()){
                SystemClock.sleep(500);
                RepaymentQrCodeActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        long timeRun= SystemClock.elapsedRealtime()-lastQueryTime;
                        long s=timeRun/1000;
                        updateQrTips();
                        if(s>=60)
                        {
                            doHttpGetQrCode();
                        }

                    }
                });

            }
        }
    }
    private void updateQrTips()
    {
        if(StringUtils.isBlank(qrNo))
        {
            if(isOnQueryQrNo){
                views.qrtext_validtime.setText("正在申请付款码");
            }
            else {
                views.qrtext_validtime.setText("付款码丢失，点击重新获取");
            }

        }
        else{
            long timeRun= SystemClock.elapsedRealtime()-lastQueryTime;
            long s=timeRun/1000;
            if(s<0){
                views.qrtext_validtime.setText("付款码有效时间：60秒");
            }
            else if(s>60)
            {
                views.qrtext_validtime.setText("付款码有效时间：0秒");
            }
            else{
                long sTmp=60-s;
                views.qrtext_validtime.setText("付款码有效时间："+sTmp+"秒");
            }

        }
    }
    private void createQrCode()
    {

        if(StringUtils.isBlank(qrNo))
        {
            views.qrtext_qrno.setText(" ");
            views.qrimg_qrcode.setBackgroundResource(R.color.bg_gray);
            views.qrimg_barcode.setImageBitmap(null);
            views.qrimg_qrcode.setImageBitmap(null);

            if(null!=bitmapBarCode)
            {
                bitmapBarCode.recycle();
                bitmapBarCode=null;

            }
            if(null!=bitmapQrCode)
            {
                bitmapQrCode.recycle();
                bitmapQrCode=null;

            }
            updateQrTips();
            return;
        }
        views.qrtext_qrno.setText(qrNo);
        views.qrimg_qrcode.setBackgroundResource(R.color.bg_white);
        Bitmap bitmaoBarCodeTemp= ZxingCreateUtil.creatBarcode(mContext,qrNo,105,20,false);
        Bitmap bitmapQrCodeTemp= ZxingCreateUtil.createQRCode(qrNo,300);
        views.qrimg_barcode.setImageBitmap(bitmaoBarCodeTemp);
        views.qrimg_qrcode.setImageBitmap(bitmapQrCodeTemp);
        if(null!=bitmapBarCode)
        {
            bitmapBarCode.recycle();
            bitmapBarCode=null;

        }
        if(null!=bitmapQrCode)
        {
            bitmapQrCode.recycle();
            bitmapQrCode=null;

        }
        bitmapBarCode=bitmaoBarCodeTemp;
        bitmapQrCode=bitmapQrCodeTemp;
        updateQrTips();
    }
}
