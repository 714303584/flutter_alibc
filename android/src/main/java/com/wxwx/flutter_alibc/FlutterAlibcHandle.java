package com.wxwx.flutter_alibc;

import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.widget.Toast;
import com.alibaba.baichuan.android.trade.AlibcTrade;
import com.alibaba.baichuan.android.trade.AlibcUrlCenter;
import com.alibaba.baichuan.android.trade.callback.AlibcTradeCallback;
import com.alibaba.baichuan.android.trade.model.AlibcShowParams;
import com.alibaba.baichuan.android.trade.model.OpenType;
import com.alibaba.baichuan.android.trade.page.AlibcBasePage;
import com.alibaba.baichuan.android.trade.page.AlibcDetailPage;
import com.alibaba.baichuan.android.trade.page.AlibcMyCartsPage;
import com.alibaba.baichuan.android.trade.page.AlibcShopPage;
import com.alibaba.baichuan.trade.biz.applink.adapter.AlibcFailModeType;
import com.alibaba.baichuan.trade.biz.context.AlibcTradeResult;
import com.alibaba.baichuan.trade.biz.core.taoke.AlibcTaokeParams;
import com.alibaba.baichuan.trade.biz.login.AlibcLogin;
import com.alibaba.baichuan.trade.biz.login.AlibcLoginCallback;
import com.alibaba.baichuan.android.trade.AlibcTradeSDK;
import com.alibaba.baichuan.android.trade.callback.AlibcTradeInitCallback;
import com.alibaba.baichuan.trade.common.utils.AlibcLogger;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.MethodCall;
import java.util.HashMap;
import android.app.AlertDialog;
import static com.wxwx.flutter_alibc.PluginConstants.*;
import static com.wxwx.flutter_alibc.PluginUtil.*;

import java.util.Map;

/**
 * TODO 可能缺少一些逻辑 明天会补上
 * @Author karedem
 * @Date 2019/9/7 19:55
 * @Description 接口处理者
**/
public class FlutterAlibcHandle{

    private static FlutterAlibcHandle handle;
    private Registrar register;

    //第一次调用getInstance register不能为空
    public static FlutterAlibcHandle getInstance(Registrar register){
        if (handle == null){
            synchronized (FlutterAlibcHandle.class){
                handle = new FlutterAlibcHandle();
                handle.register = register;
            }
        }
        return handle;
    }

    /**
     * 初始化阿里百川
     * @param call
     * @param result
     */
    public void initAlibc(MethodCall call, Result result){
        AlibcTradeSDK.asyncInit(register.activity().getApplication(), new AlibcTradeInitCallback() {
            @Override
            public void onSuccess() {
                //do something
            }
            @Override
            public void onFailure(int code, String msg) {
                result.success(new PluginResponse(Integer.toString(code), msg, null).toMap());
            }
        });
    }

    /**
     * 登陆淘宝
     * @param result
     */
    public void loginTaoBao(Result result){
        final AlibcLogin alibcLogin = AlibcLogin.getInstance();
        alibcLogin.showLogin(new AlibcLoginCallback() {
            @Override
            public void onSuccess(int loginResult, String openId, String userNick) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("loginResult", Integer.toString(loginResult));// loginResult(0--登录初始化成功；1--登录初始化完成；2--登录成功)
                userInfo.put("openId", openId); // openId：用户id
                userInfo.put("userNick", userNick); // userNick: 用户昵称
                userInfo.put("session", AlibcLogin.getInstance().getSession());
                result.success(PluginResponse.success(userInfo).toMap());
            }
            @Override
            public void onFailure(int code, String msg) {
                // code：错误码  msg： 错误信息
                result.success(new PluginResponse(Integer.toString(code), msg, null).toMap());
            }
        });
    }

    /**
     * 登出
     * @param result
     */
    public void loginOut(Result result){
        AlibcLogin alibcLogin = AlibcLogin.getInstance();
        alibcLogin.logout(new AlibcLoginCallback() {
            @Override
            public void onSuccess(int loginResult, String openId, String userNick) {
                Toast.makeText(register.activity(), "登出成功", Toast.LENGTH_SHORT).show();
                Map<String, String> userInfo = new HashMap<>();
                userInfo.put("loginResult", Integer.toString(loginResult)); // loginResult(3--登出成功)
                userInfo.put("openId", openId); // openId：用户id
                userInfo.put("userNick", userNick); // userNick: 用户昵称
                result.success(PluginResponse.success(userInfo).toMap());
            }
            @Override
            public void onFailure(int code, String msg) {
                // code：错误码  msg： 错误信息
                result.success(new PluginResponse(Integer.toString(code), msg, null).toMap());
            }
        });
    }

    /**
     * 通过URL方式打开淘宝
     * @param call
     * @param result
     */
    public void openByUrl(MethodCall call, Result result){
        AlibcShowParams showParams = new AlibcShowParams();
        AlibcTaokeParams taokeParams = new AlibcTaokeParams("","","");

        showParams.setBackUrl(call.argument(key_BackUrl));

        if (call.argument(key_OpenType) != null){
            System.out.println("openType" + call.argument(key_OpenType));
            showParams.setOpenType(getOpenType(""+call.argument(key_OpenType)));
        }
        if (call.argument(key_ClientType) != null){
            System.out.println("clientType " + call.argument(key_ClientType));
            showParams.setClientType(getClientType(""+call.argument(key_ClientType)));
        }
        if (call.argument("taokeParams") != null){
            taokeParams  = getTaokeParams(call.argument("taokeParams"));
        }
        if ("false".equals(call.argument("isNeedCustomNativeFailMode"))){
            showParams.setNativeOpenFailedMode(AlibcFailModeType.AlibcNativeFailModeNONE);
        }else if (call.argument(key_NativeFailMode) != null){
            showParams.setNativeOpenFailedMode(getFailModeType(""+call.argument(key_NativeFailMode)));
        }

        Map<String, String> trackParams = new HashMap<>();
        String url = call.argument("url");
        // 以显示传入url的方式打开页面（第二个参数是套件名称）
        AlibcTrade.openByUrl(register.activity(), "", url, null,
                new WebViewClient(), new WebChromeClient(), showParams,
                taokeParams, trackParams, new AlibcTradeCallback() {
                    @Override
                    public void onTradeSuccess(AlibcTradeResult tradeResult) {
                        result.success(PluginResponse.success(tradeResult).toMap());
                    }
                    @Override
                    public void onFailure(int code, String msg) {
                        result.success(new PluginResponse(Integer.toString(code), msg, null).toMap());
                    }
                });
    }

    /**
     * 打开商店
     * @param call
     * @param result
     */
    public void openShop(MethodCall call, Result result){
        AlibcBasePage page = new AlibcShopPage(call.argument("shopId"));
        openByBizCode(page, "shop", call, result);
    }

    /**
     * 打开购物车
     * @param result
     */
    public void openCart(MethodCall call, Result result){
        AlibcBasePage page = new AlibcMyCartsPage();
        openByBizCode(page, "cart",call, result);
    }

    /**
     * 打开商品详情
     * @param call   call.argument["itemID"]  详情id
     * @param result
     */
    public void openItemDetail(MethodCall call, Result result){
        AlibcBasePage page = new AlibcDetailPage(call.argument("itemID"));
        openByBizCode(page, "detail", call, result);
    }

    private void openByBizCode(AlibcBasePage page,String type, MethodCall call, Result result){
        AlibcShowParams showParams = new AlibcShowParams();
        AlibcTaokeParams taokeParams = new AlibcTaokeParams("", "", "");

        showParams.setBackUrl(call.argument(key_BackUrl));

        if (call.argument(key_OpenType) != null){
            showParams.setOpenType(getOpenType(""+call.argument(key_OpenType)));
        }
        if (call.argument(key_ClientType) != null){
            showParams.setClientType(getClientType(""+call.argument(key_ClientType)));
        }
        if (call.argument("taokeParams") != null){
            taokeParams  = getTaokeParams(call.argument("taokeParams"));
        }

        if ("false".equals(call.argument("isNeedCustomNativeFailMode"))){
            showParams.setNativeOpenFailedMode(AlibcFailModeType.AlibcNativeFailModeNONE);
        }else if (call.argument(key_NativeFailMode) != null){
            showParams.setNativeOpenFailedMode(getFailModeType(""+call.argument(key_NativeFailMode)));
        }

        Map<String, String> trackParams = new HashMap<>();
        AlibcTrade.openByBizCode(register.activity(), page, null, new WebViewClient(),
                new WebChromeClient(), type, showParams, taokeParams,
                trackParams, new AlibcTradeCallback() {
                    @Override
                    public void onTradeSuccess(AlibcTradeResult tradeResult) {
                        // 交易成功回调（其他情形不回调）
                        result.success(PluginResponse.success(tradeResult).toMap());
                    }
                    @Override
                    public void onFailure(int code, String msg) {
                        // 失败回调信息
                        result.success(new PluginResponse(Integer.toString(code), msg, null).toMap());
                    }
                });
    }

    /**
     * 设置淘客打点策略 是否异步
     * @param call
     */
    public void syncForTaoke(MethodCall call){
        AlibcTradeSDK.setSyncForTaoke(call.argument("isSync"));
    }

    /**
     * TODO
     * @param call
     */
    public void useAlipayNative(MethodCall call){
        AlibcTradeSDK.setShouldUseAlipay(call.argument("isNeed"));
    }


}