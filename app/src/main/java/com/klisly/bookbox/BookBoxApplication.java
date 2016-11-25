package com.klisly.bookbox;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.v7.app.NotificationCompat;

import com.antfortune.freeline.FreelineCore;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.karumi.dexter.Dexter;
import com.klisly.bookbox.model.Notification;
import com.klisly.bookbox.ui.activity.SplashActivity;
import com.klisly.bookbox.utils.CrashHandler;
import com.klisly.bookbox.utils.ToastHelper;
import com.klisly.common.SharedPreferenceUtils;
import com.tencent.bugly.crashreport.CrashReport;
import com.umeng.message.IUmengRegisterCallback;
import com.umeng.message.PushAgent;
import com.umeng.message.UmengMessageHandler;
import com.umeng.message.entity.UMessage;

import timber.log.Timber;

public class BookBoxApplication extends Application {
    private static BookBoxApplication appContext = null;
    private SharedPreferenceUtils preferenceUtils;

    public static BookBoxApplication getInstance() {
        return appContext;
    }
    private PushAgent mPushAgent;
    private Gson gson = new Gson();
    private Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;
        FreelineCore.init(this);
        Timber.plant(new Timber.DebugTree());
        ToastHelper.init(this);
        Dexter.initialize(this);
        preferenceUtils = new SharedPreferenceUtils(this);
        handler = new Handler();
        CrashHandler.getInstance().init(this.getApplicationContext());
        CrashReport.initCrashReport(getApplicationContext(), "900028744", false);
        initPush();
    }

    private void initPush() {
        mPushAgent = PushAgent.getInstance(this);
        //注册推送服务，每次调用register方法都会回调该接口
        mPushAgent.register(new IUmengRegisterCallback() {

            @Override
            public void onSuccess(String deviceToken) {
                Timber.i("device token:"+deviceToken);
            }

            @Override
            public void onFailure(String s, String s1) {
                Timber.i("err device token:"+s+" "+s1);
            }
        });
        UmengMessageHandler messageHandler = new UmengMessageHandler() {
            @Override
            public void dealWithCustomMessage(final Context context, final UMessage msg) {
                Timber.i("dealWithCustomMessage:"+msg.custom);
                try {
                    Notification notification = gson.fromJson(msg.custom, Notification.class);
                    if(Constants.NOTIFI_TYPE_MOMENT.equals(notification.getType())){
                        showMomentNotifi(notification.getTitle(), notification.getDesc());
                    } else if(Constants.NOTIFI_TYPE_NOVEL_UPDATE.equals(notification.getType())){
                        showNovelUpdate(notification.getTitle(), notification.getDesc(), notification.getTarget());
                    }
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        };
        mPushAgent.setMessageHandler(messageHandler);
        mPushAgent.setDebugMode(BuildConfig.DEBUG);
    }

    public void showMomentNotifi(String title, String msg) {
        NotificationManager manager = (NotificationManager)this.getSystemService(NOTIFICATION_SERVICE);
        //为了版本兼容  选择V7包下的NotificationCompat进行构造
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        //Ticker是状态栏显示的提示
        builder.setTicker(title);
        //第一行内容  通常作为通知栏标题
        builder.setContentTitle(title);
        //第二行内容 通常是通知正文
        builder.setContentText(msg);
        builder.setAutoCancel(true);
        //系统状态栏显示的小图标
        builder.setSmallIcon(R.drawable.ic_launcher);
        //下拉显示的大图标
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher));
        Intent intent = new Intent(this,SplashActivity.class);
        intent.putExtra("target", Constants.NOTIFI_ACTION_MOMENT);
        PendingIntent pIntent = PendingIntent.getActivity(this,1,intent,PendingIntent.FLAG_CANCEL_CURRENT);
        //点击跳转的intent
        builder.setContentIntent(pIntent);
        //通知默认的声音 震动 呼吸灯
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        android.app.Notification notification = builder.build();
        manager.notify(Constants.NOTIFI_ID_MOMENT, notification);
    }

    /**
     *
     * @param title
     * @param msg
     * @param target 文章id
     */
    public void showNovelUpdate(String title, String msg, String target) {
        NotificationManager manager = (NotificationManager)this.getSystemService(NOTIFICATION_SERVICE);
        //为了版本兼容  选择V7包下的NotificationCompat进行构造
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        //Ticker是状态栏显示的提示
        builder.setTicker(title);
        //第一行内容  通常作为通知栏标题
        builder.setContentTitle(title);
        //第二行内容 通常是通知正文
        builder.setContentText(msg);
        //可以点击通知栏的删除按钮删除
        builder.setAutoCancel(true);
        //系统状态栏显示的小图标
        builder.setSmallIcon(R.drawable.ic_launcher);
        //下拉显示的大图标
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher));
        Intent intent = new Intent(this,SplashActivity.class);
        intent.putExtra("target", Constants.NOTIFI_ACTION_NOVEL_UPDATE);
        intent.putExtra("novelid", target);
        PendingIntent pIntent = PendingIntent.getActivity(this,1,intent,PendingIntent.FLAG_CANCEL_CURRENT);
        //点击跳转的intent
        builder.setContentIntent(pIntent);
        //通知默认的声音 震动 呼吸灯
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        android.app.Notification notification = builder.build();
        manager.notify(Constants.NOTIFI_ID_NOVEL_UPDATE,notification);
    }


    public SharedPreferenceUtils getPreferenceUtils() {
        return preferenceUtils;
    }

    public Gson getGson() {
        return gson;
    }

    public Handler getHandler() {
        return handler;
    }

    public PushAgent getPushAgent() {
        return mPushAgent;
    }
}
