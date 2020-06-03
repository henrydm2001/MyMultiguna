package infix.imrankst1221.multiguna

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.onesignal.OneSignal
import infix.imrankst1221.multiguna.setting.AppDataInstance
import infix.imrankst1221.multiguna.ui.home.HomeActivity
import infix.imrankst1221.multiguna.ui.splash.SplashActivity
import infix.imrankst1221.rocket.library.setting.ConfigureRocketWeb
import infix.imrankst1221.rocket.library.utility.Constants
import infix.imrankst1221.rocket.library.utility.PreferenceUtils
import infix.imrankst1221.rocket.library.utility.UtilMethods
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private val TAG: String = "---MainActivity"

    private lateinit var mContext: Context
    private lateinit var configureRocketWeb: ConfigureRocketWeb

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mContext = this

        initSetup()

        readBundle(intent.extras)
    }

    private fun readBundle(extras: Bundle?) {
        if(extras != null){
            AppDataInstance.notificationUrl = extras.getString(Constants.KEY_NOTIFICATION_URL).orEmpty()
            AppDataInstance.notificationUrlOpenType = extras.getString(Constants.KEY_NOTIFICATION_OPEN_TYPE).orEmpty()

            UtilMethods.printLog(TAG, AppDataInstance.notificationUrl)
            UtilMethods.printLog(TAG,  AppDataInstance.notificationUrlOpenType)
        }else{
            UtilMethods.printLog(TAG, "Bundle is empty!!")
        }

        try {
            val data = this.intent.data
            if (data != null && data.isHierarchical) {
                AppDataInstance.deepLinkUrl = this.intent.dataString.toString()
                UtilMethods.printLog(TAG, "Deep link clicked "+AppDataInstance.deepLinkUrl)
            }
        }catch (ex: java.lang.Exception){
            UtilMethods.printLog(TAG, "$ex.message")
        }
    }

    private fun initSetup(){
        FirebaseApp.initializeApp(mContext)
        FirebaseMessaging.getInstance().isAutoInitEnabled = true

        AppDataInstance.getINSTANCE(mContext)
        PreferenceUtils.getInstance().initPreferences(mContext)

        configureRocketWeb  = ConfigureRocketWeb(mContext)
        configureRocketWeb.readConfigureData("rocket_web.io")


        if (configureRocketWeb.isConfigEmpty()){
            layout_error.visibility = View.VISIBLE
            UtilMethods.showLongToast(mContext, getString(R.string.massage_error_io))
        }else{
            goNextActivity()
        }

        AppDataInstance.navigationMenus =  configureRocketWeb.getMenuData("rocket_web.io")
        if(configureRocketWeb.configureData!!.themeColor == Constants.THEME_PRIMARY) {
            PreferenceUtils.getInstance().editIntegerValue(Constants.KEY_COLOR_PRIMARY, R.color.colorPrimary)
            PreferenceUtils.getInstance().editIntegerValue(Constants.KEY_COLOR_PRIMARY_DARK, R.color.colorPrimaryDark)
        }

        val isSelectedRtl = TextUtils.getLayoutDirectionFromLocale(Locale
                .getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL
        PreferenceUtils.getInstance().editBoolenValue(Constants.KEY_RTL_ACTIVE, isSelectedRtl)

        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .init()
    }

    private fun goNextActivity(){
        if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_SPLASH_SCREEN_ACTIVE, true)) {
            startActivity(Intent(this@MainActivity, SplashActivity::class.java))
        }else{
            startActivity(Intent(this@MainActivity, HomeActivity::class.java))
        }
        finish()
    }


}
