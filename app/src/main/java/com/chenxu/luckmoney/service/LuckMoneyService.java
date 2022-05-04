package com.chenxu.luckmoney.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.chenxu.luckmoney.util.ScreenUtil;
import com.chenxu.luckmoney.util.threadpool.AppExecutors;

import java.util.List;
import java.util.Random;

/**
 * 抢红包辅助
 *
 * @author Tyhj
 * @date 2019/6/30
 */

public class LuckMoneyService extends BaseAccessbilityService {

    public static final String TAG = "LuckMoneyService";

    /**
     * 单独抢一个群
     */
    public static boolean isSingle = false;
    /**
     * 暂停抢红包
     */
    public static boolean isPause = false;

    /**
     * 当前界面是否在聊天消息里面
     */
    public static boolean isInChatList = false;


    /**
     * 微信包名
     */
    private static final String WX_PACKAGE_NAME = "com.tencent.mm";

    /**
     * 红包弹出的class的名字
     */
    private static final String ACTIVITY_DIALOG_LUCKYMONEY = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI";


    /**
     * 联系人列表的消息ID
     */
    public static final String HUMAN_LIST_MSG_CONTAINER_ID = "com.tencent.mm:id/btg";

    /**
     * 详情界面的红包的ID
     */
    public static final String RED_WARS_ID = "com.tencent.mm:id/b47";

    /**
     * 红包详情页
     */
    private static String LUCKY_MONEY_DETAIL = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";

    /**
     * 红包 开字 的ID
     */
    public static final String RED_WARS_OPEN_ID = "com.tencent.mm:id/giy";

    /**
     * 聊天标题ID
     */
    public static final String CHAT_TITLE_ID = "com.tencent.mm:id/bxx";

    /**
     * 红包的开字在屏幕中的比例
     */
    private static final float POINT_OPEN_Y_SCAL = 0.641F;

    /**
     * 红包弹窗的关闭按钮在屏幕中的比例
     */
    private static final float POINT_CANCEL_Y_SCAL = 0.81F;

    /**
     * 红包弹窗中，查看领取详情在屏幕中的比例
     */
    private static final float POINT_DETAIL_Y_SCAL = 0.705F;

    /**
     * 等待弹窗弹出最大延迟
     */
    public static int waitWindowTime_max = 3000;

    /**
     * 等待弹窗弹出最小延迟
     */
    public static int waitWindowTime_min = 1000;


    /**
     * 等待红包领取时间
     */
    public static int waitGetMoneyTime = 700;


    /**
     * 当前机型是否需要配置时间，是否能获取到弹窗
     */
    public static int needSetTime = -1;

    /**
     * 获取屏幕宽高
     */
    private int screenWidth = ScreenUtil.SCREEN_WIDTH;
    private int screenHeight = ScreenUtil.SCREEN_HEIGHT;

    /**
     * 计算领取红包的时间
     */
    private static long luckMoneyComingTime;

    /**
     * 是否在领取详情页
     */
    private static boolean inMoneyDetail = false;


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        //暂停
        if (isPause) {
            isInChatList = false;
            return;
        }

        String packageName = event.getPackageName().toString();
        if (!packageName.contains(WX_PACKAGE_NAME)) {
            //不是微信就退出
            isInChatList = false;
            return;
        }

        //点击聊天最近联系人未读红包消息
        clickHumanRedWarsByID(HUMAN_LIST_MSG_CONTAINER_ID);

        // 通过红包ID点开红包详情
        List<AccessibilityNodeInfo> red_wars = findViewsByID(RED_WARS_ID);
        if (red_wars != null && !red_wars.isEmpty()) {
            // 每次打开最新一个红包
            performViewClickRedWars(red_wars.get(red_wars.size() - 1));
            isInChatList = true;
            return;
        }


        //当前类名
        String className = event.getClassName().toString();
        Log.i(TAG, "onAccessibilityEvent: " + className);
        //当前为红包弹出窗（那个开的那个弹窗）
        if (className.equals(ACTIVITY_DIALOG_LUCKYMONEY)) {
            //进行红包开点击
            inMoneyDetail = false;
            clickOpen();
            return;
        }


        //通知栏消息，判断是不是红包消息
        if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
            Log.e(TAG,"接收到通知栏消息");
            //如果当前界面是在消息列表内，并且单独抢这个群，则不必点击通知消息
//            if(!isInChatList||!isSingle){
            Notification notification = (Notification) event.getParcelableData();
            //获取通知消息详情
            String content = notification.tickerText.toString();
            //解析消息
            String[] msg = content.split(":");
            String text = msg[1].trim();
            Log.i(TAG, "onAccessibilityEvent: " + text);
            if (text.contains(HONG_BAO_TXT)) {
                Log.e(TAG,"接收到通知栏红包消息");
                PendingIntent pendingIntent = notification.contentIntent;
                try {
                    //点击消息，进入聊天界面
                    pendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            }
//            }
//            return;
        }


        //红包领取后的详情页面，自动返回
        if (className.equals(LUCKY_MONEY_DETAIL)) {
            inMoneyDetail = true;
            Log.e(TAG, "领取红包时间为：" + (System.currentTimeMillis() - luckMoneyComingTime) + "ms");
            //返回聊天界面
            performGlobalAction(GLOBAL_ACTION_BACK);
            return;
        }
    }

    @Override
    public void onInterrupt() {

    }

    /**
     * 点击开红包按钮
     */
    private void clickOpen() {
        //获取开字的控件
        AccessibilityNodeInfo target = findViewByID(RED_WARS_OPEN_ID);
        long waitWindowTime = (long) waitWindowTime_min + (long) (Math.random() * (waitWindowTime_max - waitWindowTime_min));
        SystemClock.sleep(waitWindowTime);
        if (target != null && target.getClassName().equals("android.widget.Button")) {
            Log.e(TAG, "获取到了开按钮：" + target.getContentDescription());
            performViewClick(target);
            return;
        } else {
            //如果没有找到按钮，再进行模拟点击
            //此处根据手机性能进行等待弹窗弹出
            AppExecutors.getInstance().networkIO().execute(() -> {
                long startTime = System.currentTimeMillis();
                if (!inMoneyDetail) {
                    //计算了一下这个開字在屏幕中的位置，按照屏幕比例计算
                    clickOnScreen(screenWidth / 2, screenHeight * POINT_OPEN_Y_SCAL, 1, null);
                    SystemClock.sleep(100);
                }
                if (inMoneyDetail) {
                    Log.e(TAG, "按钮点击完成，已到领取详情页");
                    return;
                }
                //防止红包已经被领完后无法跳转到下一个界面
                SystemClock.sleep(waitGetMoneyTime);
                if (inMoneyDetail) {
                    Log.e(TAG, "等待时间后，已到领取详情页");
                    return;
                }
                if (isSingle) {
                    //点击取消按钮，返回聊天界面
                    clickOnScreen(screenWidth / 2, screenHeight * POINT_CANCEL_Y_SCAL, 1, null);
                } else {
                    //点击详情进入到详情界面，触发返回操作
                    clickOnScreen(screenWidth / 2, screenHeight * POINT_DETAIL_Y_SCAL, 1, null);
                }
            });
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        ScreenUtil.getScreenSize(this);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        // 创建唤醒锁
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "WxService:wakeLock");
        // 获得唤醒锁
        wakeLock.acquire();
        performGlobalAction(GLOBAL_ACTION_BACK);
        performGlobalAction(GLOBAL_ACTION_BACK);
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

}
