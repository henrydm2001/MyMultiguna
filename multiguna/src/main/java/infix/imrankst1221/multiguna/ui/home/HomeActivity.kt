package infix.imrankst1221.multiguna.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.MailTo
import android.net.Uri
import android.net.http.SslError
import android.os.*
import android.provider.MediaStore
import com.google.android.material.navigation.NavigationView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.AppCompatButton
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.*
import android.widget.*
import com.github.ybq.android.spinkit.style.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.onesignal.OneSignal
import infix.imrankst1221.multiguna.R
import infix.imrankst1221.multiguna.setting.AppDataInstance
import infix.imrankst1221.multiguna.setting.GPSTrack
import infix.imrankst1221.multiguna.setting.OneSignalNotificationOpenedHandler
import infix.imrankst1221.rocket.library.setting.ThemeBaseActivity
import infix.imrankst1221.rocket.library.setting.WebAppInterface
import infix.imrankst1221.rocket.library.utility.PreferenceUtils
import infix.imrankst1221.rocket.library.utility.Constants
import infix.imrankst1221.rocket.library.utility.UtilMethods
import kotlinx.android.synthetic.main.activity_home.*
import java.io.File
import java.io.IOException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.*
import javax.crypto.spec.SecretKeySpec

class HomeActivity : ThemeBaseActivity(), NavigationView.OnNavigationItemSelectedListener{
    private val TAG: String = "---HomeActivity"
    private lateinit var mContext: Context

    // for attach files
    private var isViewLoaded: Boolean = false
    var ASWV_F_TYPE = "*/*"
    var mFileCamMessage: String? = null
    private var mFileMessage: ValueCallback<Uri>? = null
    var mFilePath: ValueCallback<Array<Uri>>? = null
    private var doubleBackToExitPressedOnce = false

    private lateinit var mAboutUsPopup: PopupWindow
    private lateinit var mAppUpdateManager : AppUpdateManager

    companion object {
        private const val REQUEST_CODE_IMMEDIATE_UPDATE = 17362
    }

    // download manager
    private var mOnGoingDownload: Long? = null
    private var mDownloadManger: DownloadManager? = null

    // for adMob
    private var isApplicationAlive = true
    private lateinit var mRewardedVideoAd: RewardedVideoAd
    private lateinit var mInterstitial: InterstitialAd

    private val FILE_CHOOSER: Int = 201
    private val PERMISSIONS_REQUEST_ALL: Int = 101
    private val PERMISSIONS_REQUEST_CAMERA = 102
    private val PERMISSIONS_REQUEST_LOCATION = 103
    private val PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 104
    private val PERMISSIONS_REQUEST_MICROPHONE = 105
    private val UPDATE_IMMEDIATE_REQUEST_CODE = 4502
    private val UPDATE_FLEXIBLE_REQUEST_CODE = 6708

    // for web view
    private var mWebviewPop : WebView? = null
    private var mPermissionRequest: PermissionRequest? = null

    // for web view url
    private var defaultURL = "http://www.google.com/"
    private var lastUrl : String = ""
    private var successLoadedUrl : String = ""

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        mContext = this

        initView()

        initSliderMenu()

        initThemeColor()

        initThemeStyle()

        if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_PERMISSION_DIALOG_ACTIVE, true))
            requestPermission()

        if (savedInstanceState == null) {
            loadBaseWebView()
        }

        initClickEvent()

        initAdMob()

        initOneSignal()

        //checkForAppUpdate()
    }


    private fun initView(){
        defaultURL = PreferenceUtils.getInstance().getStringValue(Constants.KEY_WEB_URL, defaultURL)!!

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setCookie(defaultURL, "DEVICE=android")
        cookieManager.setCookie(defaultURL, "DEV_API=" + Build.VERSION.SDK_INT)
        cookieManager.setAcceptThirdPartyCookies(web_view, true)

        // fullscreen setup
        if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_FULL_SCREEN_ACTIVE, false)){
            setActiveFullScreen()
        }

        // pull to refresh enable
        pull_to_refresh.isEnabled = PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_PULL_TO_REFRESH_ENABLE, false)
    }

    private fun initOneSignal(){
        OneSignal.startInit(mContext)
                .setNotificationOpenedHandler(OneSignalNotificationOpenedHandler(mContext))
                .autoPromptLocation(true)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init()
    }

    private fun initAdMob(){
        MobileAds.initialize(this, getString(R.string.admob_app_id))

        // Banner ad
        val adMobBannerID = PreferenceUtils.getInstance().getStringValue(Constants.ADMOB_KEY_AD_BANNER,"")
        if(adMobBannerID!!.isEmpty()){
            layout_footer.visibility = View.GONE
            Log.d(TAG, "Banner adMob ID is empty!")
        }else{
            initBannerAdMob(adMobBannerID)
        }

        // Interstitial ad
        val interstitialAdID = PreferenceUtils.getInstance().getStringValue(Constants.ADMOB_KEY_AD_INTERSTITIAL,"")
        if(interstitialAdID!!.isEmpty()){
            Log.d(TAG, "Banner adMob ID is empty!")
        }else{
            Handler().postDelayed({
                initInterstitialAdMob(interstitialAdID)
            }, PreferenceUtils.getInstance().getIntegerValue(Constants.ADMOB_KEY_AD_DELAY,1 * 60 * 10000).toLong())
        }

        val rewardedAdID = PreferenceUtils.getInstance().getStringValue(Constants.ADMOB_KEY_AD_REWARDED,"")
        if(rewardedAdID!!.isEmpty()){
            Log.d(TAG, "Banner adMob ID is empty!")
        }else{
            Handler().postDelayed({
                initRewardedAdMob(rewardedAdID)
            }, (PreferenceUtils.getInstance().getIntegerValue(Constants.ADMOB_KEY_AD_DELAY, 2 * 60 * 10000).toLong()) + 2 * 60* 10000)
        }
    }

    /**
     * Set adMob with Banner ID which get from adMob account
     * When ID is "" (empty) then ad will hide
     * */
    private fun initBannerAdMob(adMobBannerID: String){
        val adMobBanner = AdView(this)
        adMobBanner.adSize = AdSize.BANNER
        adMobBanner.adUnitId = adMobBannerID
        val adRequest: AdRequest =  AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                //.addTestDevice("F901B815E265F8281206A2CC49D4E432")
                .build()

        adMobBanner.adListener = object: AdListener() {
            override fun onAdLoaded() {
                runOnUiThread {
                    view_admob.visibility = View.VISIBLE
                    if(layout_footer.visibility == View.GONE){
                        layout_footer.visibility = View.VISIBLE
                        val slideUp: Animation  = AnimationUtils.loadAnimation(mContext, R.anim.anim_slide_up)
                        layout_footer.startAnimation(slideUp)
                    }
                }
            }

            override fun onAdFailedToLoad(errorCode : Int) {
                // Code to be executed when an ad request fails.
                UtilMethods.printLog(TAG, "Banner ad error code: $errorCode")
                Handler().postDelayed({
                    if(isApplicationAlive)
                        initBannerAdMob(adMobBannerID)
                }, PreferenceUtils.getInstance().getIntegerValue(Constants.ADMOB_KEY_AD_DELAY,10000).toLong())
            }

            override fun onAdLeftApplication() {
                super.onAdLeftApplication()
                isApplicationAlive = false
            }
        }

        Handler().postDelayed({
            if(isApplicationAlive) {
                adMobBanner.loadAd(adRequest)
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
                view_admob.addView(adMobBanner, params)
            }
        }, PreferenceUtils.getInstance().getIntegerValue(Constants.ADMOB_KEY_AD_DELAY,10000).toLong())
    }

    private fun initInterstitialAdMob(interstitialAdID : String){
        // setup interstitial ad
        val requestBuilder = AdRequest.Builder()
        mInterstitial = InterstitialAd(mContext)
        mInterstitial.adUnitId = interstitialAdID
        mInterstitial.loadAd(requestBuilder.build())
        mInterstitial.adListener = object: AdListener() {
            override fun onAdLoaded() {
                if(isApplicationAlive)
                    mInterstitial.show()
            }
            override fun onAdFailedToLoad(errorCode: Int) {
                Log.e(TAG, "Admob Interstitial error -$errorCode")
                Handler().postDelayed({
                    if(isApplicationAlive) {
                        initInterstitialAdMob(interstitialAdID)
                    }
                }, PreferenceUtils.getInstance().getIntegerValue(Constants.ADMOB_KEY_AD_DELAY,1 * 60 * 10000).toLong())
            }
            override fun onAdLeftApplication() {
                super.onAdLeftApplication()
                isApplicationAlive = false
            }
        }
    }


    private fun initRewardedAdMob(rewardedAdID : String){
        // setup Rewarded ad
        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this)
        mRewardedVideoAd.loadAd(rewardedAdID, AdRequest.Builder().build())
        mRewardedVideoAd.rewardedVideoAdListener = object: RewardedVideoAdListener {
            override fun onRewardedVideoCompleted() {}

            override fun onRewardedVideoAdLoaded() {
                if(isApplicationAlive)
                    mRewardedVideoAd.show()
            }

            override fun onRewardedVideoAdOpened() {}

            override fun onRewardedVideoStarted() {}

            override fun onRewardedVideoAdClosed() {}

            override fun onRewarded(rewardItem: RewardItem) {}

            override fun onRewardedVideoAdLeftApplication() {}

            override fun onRewardedVideoAdFailedToLoad(i: Int) {
                Log.d(TAG, "RewardedAd failed error - $i")
                Handler().postDelayed({
                    if(isApplicationAlive) {
                        initRewardedAdMob(rewardedAdID)
                    }
                }, (PreferenceUtils.getInstance().getIntegerValue(Constants.ADMOB_KEY_AD_DELAY,
                        2 * 60 * 10000).toLong()) + 20000)
            }
        }
    }

    private fun initThemeColor(){
        //setTheme(getThemeIdChooseByUser())

        // make problem on loading
        layout_toolbar.background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

        navigation_view.getHeaderView(0).background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

        // radius for button
        val buttonGradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

        buttonGradientDrawable.cornerRadius = 20f
        btn_try_again.background = buttonGradientDrawable
        btn_error_home.background = buttonGradientDrawable
        btn_error_try_again.background = buttonGradientDrawable

        // set other view color
        loadingIndicator.setColor(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()))
        img_no_internet.setColorFilter(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()))
        txt_title.setTextColor(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor()))
        txt_detail.setTextColor(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()))
        btn_ad_show.setColorFilter(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()))

        pull_to_refresh.setColorSchemeColors(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor()))
    }

    private fun initThemeStyle(){
        // loader style
        val loaderStyle = PreferenceUtils.getInstance().getStringValue(Constants.KEY_LOADER, Constants.LOADER_HIDE)
        if(loaderStyle == Constants.LOADER_ROTATING_PLANE){
            loader_background.visibility = View.VISIBLE
            loadingIndicator.visibility = View.VISIBLE
            loadingIndicator.setIndeterminateDrawable(RotatingPlane())
        }else if(loaderStyle == Constants.LOADER_DOUBLE_BOUNCE){
            loader_background.visibility = View.VISIBLE
            loadingIndicator.visibility = View.VISIBLE
            loadingIndicator.setIndeterminateDrawable(DoubleBounce())
        }else if(loaderStyle == Constants.LOADER_WAVE){
            loader_background.visibility = View.VISIBLE
            loadingIndicator.visibility = View.VISIBLE
            loadingIndicator.setIndeterminateDrawable(Wave())
        }else if(loaderStyle == Constants.LOADER_WANDERING_CUBE){
            loader_background.visibility = View.VISIBLE
            loadingIndicator.visibility = View.VISIBLE
            loadingIndicator.setIndeterminateDrawable(WanderingCubes())
        }else if(loaderStyle == Constants.LOADER_PULSE){
            loader_background.visibility = View.VISIBLE
            loadingIndicator.visibility = View.VISIBLE
            loadingIndicator.setIndeterminateDrawable(Pulse())
        }else if(loaderStyle == Constants.LOADER_CHASING_DOTS){
            loader_background.visibility = View.VISIBLE
            loadingIndicator.visibility = View.VISIBLE
            loadingIndicator.setIndeterminateDrawable(ChasingDots())
        }else if(loaderStyle == Constants.LOADER_THREE_BOUNCE){
            loader_background.visibility = View.VISIBLE
            loadingIndicator.visibility = View.VISIBLE
            loadingIndicator.setIndeterminateDrawable(ThreeBounce())
        }else if(loaderStyle == Constants.LOADER_CIRCLE){
            loader_background.visibility = View.VISIBLE
            loadingIndicator.visibility = View.VISIBLE
            loadingIndicator.setIndeterminateDrawable(Circle())
        }else if(loaderStyle == Constants.LOADER_CUBE_GRID){
            loader_background.visibility = View.VISIBLE
            loadingIndicator.visibility = View.VISIBLE
            loadingIndicator.setIndeterminateDrawable(CubeGrid())
        }else if(loaderStyle == Constants.LOADER_FADING_CIRCLE){
            loader_background.visibility = View.VISIBLE
            loadingIndicator.visibility = View.VISIBLE
            loadingIndicator.setIndeterminateDrawable(FadingCircle())
        }else if(loaderStyle == Constants.LOADER_FOLDING_CUBE){
            loader_background.visibility = View.VISIBLE
            loadingIndicator.visibility = View.VISIBLE
            loadingIndicator.setIndeterminateDrawable(FoldingCube())
        }else if(loaderStyle == Constants.LOADER_ROTATING_CIRCLE){
            loader_background.visibility = View.VISIBLE
            loadingIndicator.visibility = View.VISIBLE
            loadingIndicator.setIndeterminateDrawable(RotatingCircle())
        }else if(loaderStyle == Constants.LOADER_HIDE){
            loader_background.visibility = View.GONE
            loadingIndicator.visibility = View.GONE
        }

        // navigation bar style
        val navigationBar = PreferenceUtils.getInstance().getStringValue(Constants.KEY_NAVIGATION_STYLE, Constants.NAVIGATION_CLASSIC)
        if(navigationBar == Constants.NAVIGATION_STANDER){
            layout_toolbar.visibility = View.VISIBLE
            txt_toolbar_title.gravity = Gravity.LEFT
        }else if(navigationBar == Constants.NAVIGATION_ORDINARY){
            layout_toolbar.visibility = View.VISIBLE
            txt_toolbar_title.gravity = Gravity.RIGHT
        }else if(navigationBar == Constants.NAVIGATION_CLASSIC){
            layout_toolbar.visibility = View.VISIBLE
            txt_toolbar_title.gravity = Gravity.CENTER
        }else if(navigationBar == Constants.NAVIGATION_HIDE){
            layout_toolbar.visibility = View.GONE
        }

        // navigation left menu style
        val navigationLeftMenu = PreferenceUtils.getInstance().getStringValue(Constants.KEY_LEFT_MENU_STYLE, Constants.LEFT_MENU_SLIDER)
        if(navigationLeftMenu == Constants.LEFT_MENU_HIDE){
            img_left_menu.visibility = View.GONE
            img_left_menu.setImageResource(android.R.color.transparent)
        }else if(navigationLeftMenu == Constants.LEFT_MENU_SLIDER){
            img_left_menu.visibility = View.VISIBLE
            img_left_menu.setImageResource(R.drawable.ic_menu)
        }else if(navigationLeftMenu == Constants.LEFT_MENU_RELOAD){
            img_left_menu.visibility = View.VISIBLE
            img_left_menu.setImageResource(R.drawable.ic_reload)
        }else if(navigationLeftMenu == Constants.LEFT_MENU_SHARE){
            img_left_menu.visibility = View.VISIBLE
            img_left_menu.setImageResource(R.drawable.ic_share_toolbar)
        }else if(navigationLeftMenu == Constants.LEFT_MENU_HOME){
            img_left_menu.visibility = View.VISIBLE
            img_left_menu.setImageResource(R.drawable.ic_home_toolbar)
        }else if(navigationLeftMenu == Constants.LEFT_MENU_EXIT){
            img_left_menu.visibility = View.VISIBLE
            img_left_menu.setImageResource(R.drawable.ic_exit_toolbar)
        }

        // navigation right menu style
        val navigationRightMenu = PreferenceUtils.getInstance().getStringValue(Constants.KEY_RIGHT_MENU_STYLE, Constants.RIGHT_MENU_SHARE)
        if(navigationRightMenu == Constants.RIGHT_MENU_HIDE){
            img_right_menu.visibility = View.GONE
            img_right_menu.setImageResource(android.R.color.transparent)
        }else if(navigationRightMenu == Constants.RIGHT_MENU_SLIDER){
            img_right_menu.visibility = View.VISIBLE
            img_right_menu.setImageResource(R.drawable.ic_menu)
        }else if(navigationRightMenu == Constants.RIGHT_MENU_RELOAD){
            img_right_menu.visibility = View.VISIBLE
            img_right_menu.setImageResource(R.drawable.ic_reload)
        }else if(navigationRightMenu == Constants.RIGHT_MENU_SHARE){
            img_right_menu.visibility = View.VISIBLE
            img_right_menu.setImageResource(R.drawable.ic_share_toolbar)
        }else if(navigationRightMenu == Constants.RIGHT_MENU_HOME){
            img_right_menu.visibility = View.VISIBLE
            img_right_menu.setImageResource(R.drawable.ic_home_toolbar)
        }else if(navigationRightMenu == Constants.RIGHT_MENU_EXIT){
            img_right_menu.visibility = View.VISIBLE
            img_right_menu.setImageResource(R.drawable.ic_exit_toolbar)
        }

        if (layout_toolbar.visibility == View.VISIBLE && (navigationLeftMenu == Constants.LEFT_MENU_SLIDER || navigationRightMenu == Constants.RIGHT_MENU_SLIDER)){
            // enable drawer
            drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

            // set navigation slider
            navigation_view.setNavigationItemSelectedListener(this)
            val toggle = ActionBarDrawerToggle(this, drawer_layout, null, R.string.open_drawer, R.string.close_drawer)
            drawer_layout.addDrawerListener(toggle)
            toggle.syncState()
        }else{
            // disable drawer
            drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
    }


    private fun showLoader() {
        if( PreferenceUtils.getInstance().getStringValue(Constants.KEY_LOADER, Constants.LOADER_HIDE) != Constants.LOADER_HIDE)
            layout_progress.visibility = View.VISIBLE
    }

    private fun hideLoader(){
        layout_progress.visibility = View.GONE
    }

    @SuppressLint("SwitchIntDef")
    private fun checkForAppUpdate() {
        // Creates instance of the manager.
        val appUpdateManager = AppUpdateManagerFactory.create(this)

        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            when (appUpdateInfo.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        // Immediate, required update
                        appUpdateManager.startUpdateFlowForResult(
                                // Pass the intent that is returned by 'getAppUpdateInfo()'.
                                appUpdateInfo,
                                // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                                AppUpdateType.IMMEDIATE,
                                // The current activity making the update request.
                                this,
                                // Include a request code to later monitor this update request.
                                UPDATE_IMMEDIATE_REQUEST_CODE
                        )
                    } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        // Flexible, optional update
                        // Create a listener to track request state updates.
                        val listener = { state: InstallState ->
                            // Show module progress, log state, or install the update.
                            when (state.installStatus()) {
                                InstallStatus.DOWNLOADED -> {
                                    // After the update is downloaded, show a notification
                                    // and request user confirmation to restart the app.
                                    val snackbar: Snackbar = Snackbar.make(
                                            root_container,
                                            "Update download finished!",
                                            Snackbar.LENGTH_INDEFINITE
                                    )
                                    snackbar.setAction(
                                            "Update restart!",
                                            { view -> appUpdateManager.completeUpdate() })
                                    snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.colorAccent))
                                    snackbar.show()
                                }
                                InstallStatus.FAILED -> {
                                    val snackbar: Snackbar = Snackbar.make(
                                            root_container,
                                            "Update download failed!",
                                            Snackbar.LENGTH_LONG
                                    )
                                    snackbar.setAction("Update retry!", { checkForAppUpdate() })
                                    snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.colorAccent))
                                    snackbar.show()
                                }
                            }
                        }

                        // Before starting an update, register a listener for updates.
                        appUpdateManager.registerListener(listener)

                        // Start an update.
                        appUpdateManager.startUpdateFlowForResult(
                                // Pass the intent that is returned by 'getAppUpdateInfo()'.
                                appUpdateInfo,
                                // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                                AppUpdateType.FLEXIBLE,
                                // The current activity making the update request.
                                this,
                                // Include a request code to later monitor this update request.
                                UPDATE_FLEXIBLE_REQUEST_CODE
                        )

                        // When status updates are no longer needed, unregister the listener.
                        appUpdateManager.unregisterListener(listener)
                    }
                }
                /*UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                    android.app.AlertDialog.Builder(this)
                            .setTitle(getString(R.string.update_notavailable_title))
                            .setMessage(getString(R.string.update_notavailable_message))
                            .setPositiveButton(getString(R.string.update_notavailable_ok), null)
                            .show()
                }
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    android.app.AlertDialog.Builder(this)
                            .setTitle(getString(R.string.update_inprogress_title))
                            .setMessage(getString(R.string.update_inprogress_message))
                            .setPositiveButton(getString(R.string.update_inprogress_ok), null)
                            .show()
                }*/
            }
        }
    }

    private fun initClickEvent() {
        // try again
        btn_try_again.setOnClickListener {
            if(UtilMethods.isConnectedToInternet(mContext)){
                // request for show website
                //webviewReload()
                web_view.reload()
            }else{
                UtilMethods.showSnackbar(root_container, getString(R.string.massage_nointernet))
            }
        }

        pull_to_refresh.setOnRefreshListener {
            //webviewReload()
            web_view.reload()
            Handler().postDelayed({
                pull_to_refresh.isRefreshing = false
            }, 2000)
        }

        // menu click toggle left
        img_left_menu.setOnClickListener{
            // slier menu open from left
            val params = DrawerLayout.LayoutParams(DrawerLayout.LayoutParams.WRAP_CONTENT, DrawerLayout.LayoutParams.MATCH_PARENT)
            val gravityCompat: Int

            if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_RTL_ACTIVE, false)){
                params.gravity = Gravity.END
                gravityCompat = GravityCompat.END
                navigation_view.layoutParams = params
            }else{
                params.gravity = Gravity.START
                gravityCompat = GravityCompat.START
                navigation_view.layoutParams = params
            }

            val navigationLeftMenu = PreferenceUtils.getInstance().getStringValue(Constants.KEY_LEFT_MENU_STYLE, Constants.LEFT_MENU_SLIDER)
            if(navigationLeftMenu == Constants.LEFT_MENU_SLIDER){
                Handler().postDelayed({
                    if (drawer_layout.isDrawerOpen(gravityCompat)){
                        drawer_layout.closeDrawer(gravityCompat)
                    }else{
                        drawer_layout.openDrawer(gravityCompat)
                    }
                }, 100)
            }else if(navigationLeftMenu == Constants.LEFT_MENU_RELOAD){
                // request for reload again website
                webviewReload()
            }else if(navigationLeftMenu == Constants.LEFT_MENU_SHARE){
                UtilMethods.shareTheApp(mContext,
                        "Download "+ getString(R.string.app_name)+"" +
                                " app from play store. Click here: "+"" +
                                "https://play.google.com/store/apps/details?id="+packageName+"/")
            }else if(navigationLeftMenu == Constants.LEFT_MENU_HOME){
                isViewLoaded = false
                loadBaseWebView()
            }else if(navigationLeftMenu == Constants.LEFT_MENU_EXIT){
                exitHomeScreen()
            }else if(navigationLeftMenu == Constants.LEFT_MENU_HIDE){
                // menu is hidden
            }
        }

        // menu click toggle right
        img_right_menu.setOnClickListener{
            // slier menu open from left
            val params = DrawerLayout.LayoutParams(DrawerLayout.LayoutParams.WRAP_CONTENT, DrawerLayout.LayoutParams.MATCH_PARENT)
            val gravityCompat: Int

            if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_RTL_ACTIVE, false)){
                params.gravity = Gravity.START
                gravityCompat = GravityCompat.START
                navigation_view.layoutParams = params
            }else{
                params.gravity = Gravity.END
                gravityCompat = GravityCompat.END
                navigation_view.layoutParams = params
            }

            val navigationRightMenu = PreferenceUtils.getInstance().getStringValue(Constants.KEY_RIGHT_MENU_STYLE, Constants.RIGHT_MENU_SLIDER)
            if(navigationRightMenu == Constants.RIGHT_MENU_SLIDER){
                Handler().postDelayed({
                    if (drawer_layout.isDrawerOpen(gravityCompat)) {
                        drawer_layout.closeDrawer(gravityCompat)
                    } else {
                        drawer_layout.openDrawer(gravityCompat)
                    }
                }, 100)
            }else if(navigationRightMenu == Constants.RIGHT_MENU_RELOAD){
                // request for reload again website
                webviewReload()
            }else if(navigationRightMenu == Constants.RIGHT_MENU_SHARE){
                UtilMethods.shareTheApp(mContext,
                        "Download "+ getString(R.string.app_name)+"" +
                                " app from play store. Click here: "+"" +
                                "https://play.google.com/store/apps/details?id="+packageName+"/")
            }else if(navigationRightMenu == Constants.RIGHT_MENU_HOME){
                isViewLoaded = false
                loadBaseWebView()
            }else if(navigationRightMenu == Constants.RIGHT_MENU_EXIT){
                exitHomeScreen()
            }else if(navigationRightMenu == Constants.RIGHT_MENU_HIDE){
                // menu is hidden
            }
        }

        // on error reload again
        btn_error_try_again.setOnClickListener{
            // request for reload again website
            //successLoadedUrl = ""
            isViewLoaded = false
            //web_view.clearCache(true)
            //web_view.clearHistory()
            if (successLoadedUrl != ""){
                loadWebView(successLoadedUrl)
            }else if (lastUrl != ""){
                loadWebView(lastUrl)
            }else{
                loadBaseWebView()
            }
        }

        // on error go home
        btn_error_home.setOnClickListener {
            // request for reload again website
            successLoadedUrl = ""
            isViewLoaded = false
            web_view.clearCache(true)
            web_view.clearHistory()

            loadBaseWebView()
        }


        // show or hide adMob
        btn_ad_show.setOnClickListener {
            if(view_admob.visibility == View.GONE){
                view_admob.visibility = View.VISIBLE
                img_ad_show.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_down_arrow))
            }else{
                view_admob.visibility = View.GONE
                img_ad_show.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_up_arrow))
            }
        }
    }

    private fun loadBaseWebView() {
        defaultURL = PreferenceUtils.getInstance().getStringValue(Constants.KEY_WEB_URL, defaultURL)!!

        if(AppDataInstance.deepLinkUrl.isNotEmpty()){
            loadWebView(AppDataInstance.deepLinkUrl)
            AppDataInstance.deepLinkUrl = ""
        } else if(AppDataInstance.notificationUrl.isNotEmpty()) {
            when (Constants.WEBVIEW_OPEN_TYPE.valueOf(AppDataInstance.notificationUrlOpenType.toUpperCase())){
                Constants.WEBVIEW_OPEN_TYPE.EXTERNAL -> {
                    loadWebView(defaultURL)
                    Handler().postDelayed({
                        UtilMethods.browseUrlExternal(mContext, AppDataInstance.notificationUrl)
                        AppDataInstance.notificationUrl = ""
                    }, 5000)
                }
                Constants.WEBVIEW_OPEN_TYPE.CUSTOM_TAB -> {
                    loadWebView(defaultURL)
                    Handler().postDelayed({
                        UtilMethods.browseUrlCustomTab(mContext, AppDataInstance.notificationUrl)
                        AppDataInstance.notificationUrl = ""
                    }, 5000)
                }
                else -> {
                    loadWebView(AppDataInstance.notificationUrl)
                    AppDataInstance.notificationUrl = ""
                }
            }
        }else {
            loadWebView(defaultURL)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadWebView(loadUrl: String) {

        web_view.visibility = View.VISIBLE
        layout_no_internet.visibility = View.GONE
        hideLoader()
        layout_error.visibility = View.GONE

        // initial webview setting
        initConfigureWebView()

        // init web client
        initWebClient()

        // init chrome client
        initChromeClient()

        web_view.loadUrl(loadUrl)
    }

    private fun webviewReload(){
        isViewLoaded = false
        web_view.loadUrl("about:blank")
        //web_view.clearCache(true)
        //web_view.clearHistory()
        web_view.reload()
    }

    //not used, optional
    fun clearCache() {
        web_view.clearCache(true)
        this.deleteDatabase("webview.db")
        this.deleteDatabase("webviewCache.db")
        web_view.clearCache(false)
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun initConfigureWebView(){
        // clear view'
        //web_view.clearHistory()
        //web_view.clearCache(true)

        try {
            web_view.settings.apply {
                // enable java script
                javaScriptEnabled = PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_JAVASCRIPT_ACTIVE, true)
                javaScriptCanOpenWindowsAutomatically = true

                // ensure the page is given a larger viewport
                useWideViewPort = true
                loadWithOverviewMode = true

                // HTML5 Cache setup
                databaseEnabled = true
                domStorageEnabled = true
                setAppCacheEnabled(false)
                setAppCachePath(applicationContext.filesDir.absolutePath+ "/cache")

                cacheMode = WebSettings.LOAD_NO_CACHE
                layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL

                /**loadsImagesAutomatically = true
                loadsImagesAutomatically = true
                loadWithOverviewMode = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW*/

                // file access
                allowFileAccess = true

                // location setting
                setGeolocationEnabled(true)
                //setGeolocationDatabasePath(filesDir.getPath())
                //setRenderPriority(WebSettings.RenderPriority.HIGH)

                // enable for flash only
                //mediaPlaybackRequiresUserGesture = true // google login not work

                // other setting
                allowContentAccess = true
                setSupportMultipleWindows(false) // google popuo login

                //allowFileAccessFromFileURLs = true
                //allowUniversalAccessFromFileURLs = true

                // website zoom
                //displayZoomControls = false
                builtInZoomControls = PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_WEBSITE_ZOOM_ACTIVE, false)
                setSupportZoom(PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_WEBSITE_ZOOM_ACTIVE, false))

                /**userAgentString [Do not use it, it's make problem in youtube video]**/
                if(PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_DESKTOP_MOOD_ACTIVE, false)){
                    userAgentString = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0"
                    builtInZoomControls = true
                    setSupportZoom(true)
                    displayZoomControls = true
                }else {
                    userAgentString = "Mozilla/5.0 (Linux; Android 5.1.1; Nexus 5 Build/LMY48B; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/67.0.2357.65 Mobile Safari/537.36"
                }

            }

            // focus when touch
            web_view.isFocusable = true
            web_view.isFocusableInTouchMode = true

            // enable java script
            web_view.addJavascriptInterface(WebAppInterface(mContext), "Android ")
            web_view.setLayerType(View.LAYER_TYPE_HARDWARE, null)

            // disable web view scroll
            /** Do not use is it's make problem on scroll HTML slider
             * web_view.setOnTouchListener { v, event -> (event!!.action == MotionEvent.ACTION_MOVE) }**/
            web_view.isScrollContainer = false
            web_view.isVerticalScrollBarEnabled = false
            web_view.isHorizontalScrollBarEnabled = false
        }catch (ex: Exception){
            ex.printStackTrace()
        }

        // set download listener
        if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_DOWNLOADS_IN_WEBVIEW_ACTIVE, true)) {
            web_view.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                if (Build.VERSION.SDK_INT >= 23) {
                    if (askForPermission(PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE, true)) {
                        //UtilMethods.browseUrlCustomTab(mContext, url)
                        startDownload(url, userAgent, mimetype)
                    }
                } else {
                    //UtilMethods.browseUrlCustomTab(mContext, url)
                    startDownload(url, userAgent, mimetype)
                }
            }
        } else{
            web_view.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                /*val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)*/
                UtilMethods.browseUrlCustomTab(mContext, url)
            }
        }
    }

    private fun initWebClient(){
        web_view.webViewClient = object : WebViewClient() {
            // shouldOverrideUrlLoading only call on real device
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                val url: String = url!!

                UtilMethods.printLog(TAG, "URL: $url")

                // already success loaded
                if(successLoadedUrl == url || url.startsWith("file:///android_asset")){
                    UtilMethods.printLog(TAG, "Page already loaded!")
                    return false
                }

                if(url == "http://google.com"){
                    UtilMethods.browseUrlExternal(mContext, url)
                    return true
                }

                if (UtilMethods.isConnectedToInternet(mContext)) {
                    if (url.startsWith("http:") || url.startsWith("https:")) {
                        val host = Uri.parse(url).host?:""

                        /*if (host.contains("m.facebook.com")
                                || host.contains("facebook.co")
                                || host.contains("www.facebook.com")){

                            val activityCode = 104
                            val intent = Intent()
                            intent.setClassName("com.facebook.katana", "com.facebook.katana.ProxyAuth")
                            intent.putExtra("client_id", application.packageName)
                            startActivityForResult(intent, activityCode)
                            return true
                        }*/

                        if (host.contains("drive.google.com")){
                            UtilMethods.browseUrlCustomTab(mContext, url)
                            return true
                        }

                        if (host.contains ("t.me")) {
                            UtilMethods.browseUrlExternal (mContext, url)
                            return true
                        }

                        if(host.contains("m.facebook.com")
                                || host.contains("facebook.co")
                                || host.contains("www.facebook.com")
                                || host.contains(".google.com")
                                || host.contains(".google.co")
                                || host.contains("accounts.google.com")
                                || host.contains("accounts.google.co.in")
                                || host.contains("www.accounts.google.com")
                                || host.contains("www.twitter.com")
                                || host.contains("secure.payu.in")
                                || host.contains("https://secure.payu.in")
                                || host.contains("oauth.googleusercontent.com")
                                || host.contains("content.googleapis.com")
                                || host.contains("ssl.gstatic.com")){
                             /*val intent = Intent(Intent.ACTION_VIEW)
                             intent.data = Uri.parse(url)
                             startActivity(intent)
                             return true*/
                            showLoader()
                            return false
                        }else if (host == Uri.parse(defaultURL).host){
                            if (mWebviewPop != null) {
                                mWebviewPop!!.visibility = View.GONE
                                //frame_web_view.removeView(mWebviewPop)
                                mWebviewPop = null
                            }
                        }
                        showLoader()
                        return false
                    } else if (url.startsWith("tel:")) {
                        val intent = Intent(Intent.ACTION_DIAL)
                        intent.data = Uri.parse(url)
                        try{
                            startActivity(intent)
                        }catch (ex: ActivityNotFoundException){
                            UtilMethods.showShortToast(mContext, "Activity Not Found")
                        }
                        return true

                    } else if (url.startsWith("sms:")) {
                        val intent = Intent(Intent.ACTION_SENDTO)
                        intent.data = Uri.parse(url)
                        try{
                            startActivity(intent)
                        }catch (ex: ActivityNotFoundException){
                            UtilMethods.showShortToast(mContext, "Activity Not Found")
                        }
                        return true

                    } else if (url.startsWith("mailto:")) {
                        val mail = Intent(Intent.ACTION_SEND)
                        mail.type = "message/rfc822"

                        val mailTo: MailTo = MailTo.parse(url)
                        //val addressMail = url.replace("mailto:", "")
                        mail.putExtra(Intent.EXTRA_EMAIL, arrayOf(mailTo.to))
                        mail.putExtra(Intent.EXTRA_CC, mailTo.cc)
                        mail.putExtra(Intent.EXTRA_SUBJECT, mailTo.subject)
                        //mail.putExtra(Intent.EXTRA_TEXT, mailTo.body)
                        try{
                            startActivity(intent)
                        }catch (ex: ActivityNotFoundException){
                            UtilMethods.showShortToast(mContext, "Activity Not Found")
                        }
                        return true

                    } else if(url.contains("intent:")){
                        try {
                            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent)
                                return true
                            }
                            //try to find fallback url
                            val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                            if (fallbackUrl != null) {
                                web_view.loadUrl(fallbackUrl)
                                return true
                            }
                            //invite to install
                            val marketIntent = Intent(Intent.ACTION_VIEW).setData(
                                    Uri.parse("market://details?id=" + intent.getPackage()!!))
                            if (marketIntent.resolveActivity(packageManager) != null) {
                                try{
                                    startActivity(intent)
                                }catch (ex: ActivityNotFoundException){
                                    UtilMethods.showShortToast(mContext, "Activity Not Found")
                                }
                                return true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    } else if(url.contains("whatsapp://") || url.contains("app.whatsapp")){
                        try{
                            val sendIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            sendIntent.setPackage("com.whatsapp")
                            startActivity(sendIntent);
                        }catch (ex: Exception){
                            ex.printStackTrace()
                        }
                        return true;

                    } else if (url.contains("facebook.com/sharer")||
                            url.contains("twitter.com/intent")||
                            url.contains("plus.google.com")||
                            url.contains("pinterest.com/pin")){
                        UtilMethods.browseUrlExternal(mContext, url)
                        return true
                    } else if (url.contains("geo:")
                            || url.contains("market://")
                            || url.contains("market://")
                            || url.contains("play.google")
                            || url.contains("vid:")
                            || url.contains("youtube")
                            || url.contains("fb-messenger")
                            || url.contains("?target=external")) {

                        UtilMethods.browseUrlCustomTab(mContext, url)
                        return true
                    }
                    showLoader()
                    return false
                } else {
                    web_view.visibility = View.GONE
                    layout_no_internet.visibility = View.VISIBLE
                    hideLoader()
                }
                lastUrl = url
                showLoader()
                return false
                //return super.shouldOverrideUrlLoading(view, url)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url: String = request.url.toString()

                UtilMethods.printLog(TAG, "URL: $url")

                // already success loaded
                if(successLoadedUrl == url || url.startsWith("file:///android_asset")){
                    UtilMethods.printLog(TAG, "Page already loaded!")
                    return false
                }

                if(url == "http://google.com"){
                    UtilMethods.browseUrlExternal(mContext, url)
                    return true
                }

                if (UtilMethods.isConnectedToInternet(mContext)) {
                    if (url.startsWith("http:") || url.startsWith("https:")) {
                        val host = Uri.parse(url).host?:""

                        /*if (host.contains("m.facebook.com")
                              || host.contains("facebook.co")
                              || host.contains("www.facebook.com")){

                          val activityCode = 104
                          val intent = Intent()
                          intent.setClassName("com.facebook.katana", "com.facebook.katana.ProxyAuth")
                          intent.putExtra("client_id", application.packageName)
                          startActivityForResult(intent, activityCode)
                          return true
                      }*/

                        if (host.contains("drive.google.com")){
                            UtilMethods.browseUrlCustomTab(mContext, url)
                            return true
                        }

                        if (host.contains ("t.me")) {
                            UtilMethods.browseUrlExternal (mContext, url)
                            return true
                        }

                        if(host.contains("m.facebook.com")
                                || host.contains("facebook.co")
                                || host.contains("www.facebook.com")
                                || host.contains(".google.com")
                                || host.contains(".google.co")
                                || host.contains("accounts.google.com")
                                || host.contains("accounts.google.co.in")
                                || host.contains("www.accounts.google.com")
                                || host.contains("www.twitter.com")
                                || host.contains("secure.payu.in")
                                || host.contains("https://secure.payu.in")
                                || host.contains("oauth.googleusercontent.com")
                                || host.contains("content.googleapis.com")
                                || host.contains("ssl.gstatic.com")){
                            /* val intent = Intent(Intent.ACTION_VIEW)
                             intent.data = Uri.parse(url)
                             startActivity(intent)
                             return true*/
                            showLoader()
                            return false
                        } else if (host == Uri.parse(defaultURL).host){
                            if (mWebviewPop != null) {
                                mWebviewPop!!.visibility = View.GONE
                                //frame_web_view.removeView(mWebviewPop)
                                mWebviewPop = null
                            }
                        }
                        showLoader()
                        return false
                    } else if (url.startsWith("tel:")) {
                        val intent = Intent(Intent.ACTION_DIAL)
                        intent.data = Uri.parse(url)
                        try{
                            startActivity(intent)
                        }catch (ex: ActivityNotFoundException){
                            UtilMethods.showShortToast(mContext, "Activity Not Found")
                        }
                        return true

                    } else if (url.startsWith("sms:")) {
                        val intent = Intent(Intent.ACTION_SENDTO)
                        intent.data = Uri.parse(url)
                        try{
                            startActivity(intent)
                        }catch (ex: ActivityNotFoundException){
                            UtilMethods.showShortToast(mContext, "Activity Not Found")
                        }
                        return true

                    } else if (url.startsWith("mailto:")) {
                        val mail = Intent(Intent.ACTION_SEND)
                        mail.type = "message/rfc822"

                        val mailTo: MailTo = MailTo.parse(url)
                        mail.putExtra(Intent.EXTRA_EMAIL, arrayOf(mailTo.to))
                        mail.putExtra(Intent.EXTRA_CC, mailTo.cc)
                        mail.putExtra(Intent.EXTRA_SUBJECT, mailTo.subject)
                        //mail.putExtra(Intent.EXTRA_TEXT, mailTo.body)
                        try{
                            startActivity(mail)
                        }catch (ex: ActivityNotFoundException){
                            UtilMethods.showShortToast(mContext, "Activity Not Found")
                        }
                        return true

                    } else if(url.contains("intent:")){
                        try {
                            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent)
                                return true
                            }
                            //try to find fallback url
                            val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                            if (fallbackUrl != null) {
                                web_view.loadUrl(fallbackUrl)
                                return true
                            }
                            //invite to install
                            val marketIntent = Intent(Intent.ACTION_VIEW).setData(
                                    Uri.parse("market://details?id=" + intent.getPackage()!!))
                            if (marketIntent.resolveActivity(packageManager) != null) {
                                try{
                                    startActivity(intent)
                                }catch (ex: ActivityNotFoundException){
                                    UtilMethods.showShortToast(mContext, "Activity Not Found")
                                }
                                return true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    } else if(url.contains("whatsapp://") || url.contains("app.whatsapp")){
                        try{
                            val sendIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            sendIntent.setPackage("com.whatsapp")
                            startActivity(sendIntent);
                        }catch (ex: Exception){
                            ex.printStackTrace()
                        }
                        return true;

                    } else if (url.contains("facebook.com/sharer")||
                            url.contains("twitter.com/intent")||
                            url.contains("plus.google.com")||
                            url.contains("pinterest.com/pin")){
                        UtilMethods.browseUrlExternal(mContext, url)
                        return true
                    } else if (url.contains("geo:")
                            || url.contains("market://")
                            || url.contains("market://")
                            || url.contains("play.google")
                            || url.contains("vid:")
                            || url.contains("youtube")
                            || url.contains("fb-messenger")
                            || url.contains("?target=external")) {

                        UtilMethods.browseUrlCustomTab(mContext, url)
                        return true
                    }
                    showLoader()
                    return false
                } else {
                    web_view.visibility = View.GONE
                    layout_no_internet.visibility = View.VISIBLE
                    hideLoader()
                }
                lastUrl = url
                showLoader()
                return false
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (UtilMethods.isConnectedToInternet(mContext) || url.startsWith("file:///android_asset")) {
                    isViewLoaded = false
                    web_view.visibility = View.VISIBLE
                    layout_error.visibility = View.GONE
                    showLoader()

                    Handler().postDelayed({
                        layout_no_internet.visibility = View.GONE
                        if(layout_progress.visibility == View.VISIBLE){
                            hideLoader()
                        }
                    }, PreferenceUtils.getInstance().getIntegerValue(Constants.KEY_LOADER_DELAY, 1000).toLong())
                }else {
                    web_view.visibility = View.GONE
                    layout_no_internet.visibility = View.VISIBLE
                    hideLoader()
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                isViewLoaded = true
                if(layout_error.visibility != View.VISIBLE){
                    web_view.visibility = View.VISIBLE
                }
                hideLoader()

                // success url for load once
                successLoadedUrl = url

                if (url.startsWith("https://m.facebook.com/v2.7/dialog/oauth")) {
                    if (mWebviewPop != null) {
                        mWebviewPop!!.visibility = View.GONE
                        //frame_web_view.removeView(mWebviewPop)
                        mWebviewPop = null
                    }
                    web_view.loadUrl(lastUrl)
                    return
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)

                //web_view.loadUrl("about:blank")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    UtilMethods.printLog(TAG, "${error!!.description}")

                    if(error.description.contains("net::ERR_UNKNOWN_URL_SCHEME")
                            || error.description.contains("net::ERR_FILE_NOT_FOUND")
                            || error.description.contains("net::ERR_CONNECTION_REFUSED")){
                        web_view.visibility = View.GONE
                        hideLoader()
                        return
                    }else if(error.description.contains("net::ERR_NAME_NOT_RESOLVED")) {
                        // webviewReload()
                        return
                    }else if(error.description.contains("net::ERR_CONNECTION_TIMED_OUT")){
                        // show offline page
                        return
                    }else if(error.description.contains("net::ERR_CLEARTEXT_NOT_PERMITTED")){
                        return
                    }
                }else{
                    UtilMethods.printLog(TAG, error.toString())
                }

                if (view!!.canGoBack()) {
                    view.goBack()
                }

            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)
                UtilMethods.printLog(TAG, "HTTP Error: ${errorResponse.toString()}")
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                super.onReceivedSslError(view, handler, error)
                UtilMethods.printLog(TAG, "SSL Error: ${error.toString()}")
            }

        }
    }

    private fun initChromeClient() {
        var mCustomView: View? = null
        var mCustomViewCallback: WebChromeClient.CustomViewCallback? = null
        var mOriginalOrientation: Int = 0
        var mOriginalSystemUiVisibility: Int = 0

        web_view.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                    webView: WebView, filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: FileChooserParams): Boolean {
                if (askForPermission(PERMISSIONS_REQUEST_CAMERA, true) &&
                        askForPermission(PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE, true)) {
                    if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_FILE_UPLOAD_ENABLE, true)) {
                        if (mFilePath != null) {
                            mFilePath!!.onReceiveValue(null)
                        }
                        mFilePath = filePathCallback
                        var takePictureIntent: Intent? = null

                        if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_PHOTO_UPLOAD_ENABLE, true)) {
                            takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            if (takePictureIntent.resolveActivity(this@HomeActivity.packageManager) != null) {
                                var photoFile: File? = null
                                try {
                                    photoFile = createImageFile()
                                    takePictureIntent.putExtra("PhotoPath", mFileCamMessage)
                                } catch (ex: IOException) {
                                    Log.e(TAG, "Image file creation failed", ex)
                                }

                                if (photoFile != null) {
                                    mFileCamMessage = "file:" + photoFile.absolutePath
                                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
                                } else {
                                    takePictureIntent = null
                                }
                            }
                        }

                        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)

                        if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_MULTIPLE_FILE_UPLOAD_ENABLE, false)) {
                            contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            contentSelectionIntent.type = "*/*"
                        }

                        if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_CAMERA_PHOTO_UPLOAD_ENABLE, false)) {
                            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                            contentSelectionIntent.type = "image/*"
                        }

                        val intentArray: Array<Intent?>
                        if (takePictureIntent != null) {
                            intentArray = arrayOf(takePictureIntent)
                        } else {
                            intentArray = arrayOfNulls(0)
                        }

                        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                        chooserIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.label_file_chooser))
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                        startActivityForResult(chooserIntent, FILE_CHOOSER)
                        return true
                    } else {
                        return false
                    }
                }
                return false
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                if (Build.VERSION.SDK_INT < 23 || askForPermission(PERMISSIONS_REQUEST_LOCATION, true)) {
                    // location permissions were granted previously so auto-approve
                    callback.invoke(origin, true, false)
                }
            }

            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                val newWebView = WebView(mContext)
                newWebView.settings.javaScriptEnabled = true
                newWebView.settings.setSupportZoom(true)
                newWebView.settings.builtInZoomControls = true
                newWebView.settings.pluginState = WebSettings.PluginState.ON
                newWebView.settings.setSupportMultipleWindows(true)
                view!!.addView(newWebView)
                val transport = resultMsg!!.obj as WebView.WebViewTransport
                transport.webView = newWebView
                resultMsg.sendToTarget()

                newWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                        view.loadUrl(url)
                        return true
                    }
                }

                return true
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                return super.onJsAlert(view, url, message, result)
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                /**
                 * Set progress view
                 *
                 * progress.setProgress(newProgress);
                if (newProgress == 100) {
                progress.setProgress(0);
                }**/
            }

            override fun onShowCustomView(paramView: View?, paramCustomViewCallback: CustomViewCallback) {
                if (mCustomView != null) {
                    onHideCustomView()
                    return
                }
                mCustomView = paramView
                mOriginalSystemUiVisibility = window.decorView.systemUiVisibility
                mOriginalOrientation = requestedOrientation
                mCustomViewCallback = paramCustomViewCallback
                (window.decorView as FrameLayout).addView(mCustomView, FrameLayout.LayoutParams(-1, -1))
                window.decorView.systemUiVisibility = 3846
            }


            override fun onHideCustomView() {
                (window.decorView as FrameLayout).removeView(mCustomView)
                mCustomView = null
                window.decorView.systemUiVisibility = mOriginalSystemUiVisibility
                requestedOrientation = mOriginalOrientation
                mCustomViewCallback?.onCustomViewHidden()
                mCustomViewCallback = null
            }

            override fun onPermissionRequest(permissionRequest: PermissionRequest?) {
                mPermissionRequest = permissionRequest

                UtilMethods.printLog(TAG, "onJSPermissionRequest")
                for (request in permissionRequest?.resources!!) {
                    UtilMethods.printLog(TAG, "AskForPermission for" + permissionRequest.origin.toString() + "with" + request)
                    when (request) {
                        "android.webkit.resource.AUDIO_CAPTURE" -> askForPermission(PERMISSIONS_REQUEST_MICROPHONE, true)
                        "android.webkit.resource.VIDEO_CAPTURE" -> askForPermission(PERMISSIONS_REQUEST_CAMERA, true)
                    }
                }
            }

            override fun onPermissionRequestCanceled(request: PermissionRequest?) {
                super.onPermissionRequestCanceled(request)
                Toast.makeText(mContext,"Permission Denied",Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        @SuppressLint("SimpleDateFormat")
        val fileName = SimpleDateFormat("yyyy_mm_ss").format(Date())
        val newName = "file_" + fileName + "_"
        val sdDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(newName, ".jpg", sdDirectory)
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    private fun generateKey(): SecretKey {
        val random = SecureRandom()
        val key = byteArrayOf(1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0)
        //random.nextBytes(key)
        return SecretKeySpec(key, "AES")
    }

    // download manager
    internal var mDownloadCompleteListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            clearDownloadingState()
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)
            val fileUri = mDownloadManger!!.getUriForDownloadedFile(id)
        }
    }

    private fun startDownload(url: String, userAgent: String, mimetype: String) {
        val fileUri = Uri.parse(url)
        val fileName = fileUri.lastPathSegment
        val cookies = CookieManager.getInstance().getCookie(url)

        try {
            val request = DownloadManager.Request(fileUri)
            request.setMimeType(mimetype)
                    .addRequestHeader("cookie", cookies)
                    .addRequestHeader("User-Agent", userAgent)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            UtilMethods.showLongToast(mContext, "Downloading File")
        }catch (ex: java.lang.Exception){
            UtilMethods.showLongToast(mContext, "Download failed!")
            UtilMethods.printLog(TAG, "${ex.message}")
        }
    }


    //cancel download, no active
    protected fun cancelDownload() {
        if (mOnGoingDownload != null) {
            mDownloadManger!!.remove(mOnGoingDownload!!)
            clearDownloadingState()
        }
    }

    protected fun clearDownloadingState() {
        unregisterReceiver(mDownloadCompleteListener)
        //mCancel.setVisibility(View.GONE);
        mOnGoingDownload = null
    }

    // get file callback
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == REQUEST_CODE_IMMEDIATE_UPDATE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    UtilMethods.showLongToast(mContext, "Application update successfully!")
                }
                Activity.RESULT_CANCELED -> {
                    UtilMethods.showLongToast(mContext, "Application update is mandatory!")
                }
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                    UtilMethods.showLongToast(mContext, "Application update is mandatory!")
                }
            }
        }

        if (Build.VERSION.SDK_INT >= 21) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
           /* window.statusBarColor = resources.getColor(R.color.colorPrimary)*/
            window.setStatusBarColor(R.color.colorPrimary)
            var results: Array<Uri>? = null
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == FILE_CHOOSER) {
                    if (mFilePath == null) {
                        return
                    }
                    if (intent == null || intent.data == null) {
                        if (mFileCamMessage != null) {
                            results = arrayOf(Uri.parse(mFileCamMessage))
                        }
                    } else {
                        val dataString = intent.dataString
                        if (intent.clipData != null) {
                            val numSelectedFiles: Int = intent.clipData?.itemCount?: 0
                            results = Array(numSelectedFiles) { Uri.EMPTY}
                            for (i in 0 until numSelectedFiles) {
                                results[i] = intent.clipData!!.getItemAt(i).uri
                            }
                        }else if (dataString != null) {
                            results = arrayOf(Uri.parse(dataString))
                        }else{
                            UtilMethods.printLog(TAG, "Image upload data is empty!")
                        }
                    }
                }
            }
            mFilePath!!.onReceiveValue(results)
            mFilePath = null
        } else {
            if (requestCode == FILE_CHOOSER) {
                if (null == mFileMessage) return
                val result = if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
                mFileMessage?.onReceiveValue(result)
                mFileMessage = null
            }
        }
    }


    // request for all permission
    private fun requestPermission(){
        var mIndex: Int = -1
        val requestList: Array<String> = Array(10, { "" } )

        // Access photos Permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            mIndex ++
            requestList[mIndex] = Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            mIndex ++
            requestList[mIndex] = Manifest.permission.WRITE_EXTERNAL_STORAGE
        }

        // Location Permission
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mIndex ++
            requestList[mIndex] = Manifest.permission.ACCESS_FINE_LOCATION
        }else{
            getLocation()
        }

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            mIndex ++
            requestList[mIndex] = Manifest.permission.CAMERA
        }

        if(mIndex != -1){
            ActivityCompat.requestPermissions(this, requestList, PERMISSIONS_REQUEST_ALL)
        }
    }

    private fun jsPermissionAccepted(){
        if (mPermissionRequest != null){
            mPermissionRequest!!.grant(mPermissionRequest!!.resources)
        }
    }
    private fun askForPermission(permissionCode: Int, request: Boolean): Boolean{
        when(permissionCode){
            PERMISSIONS_REQUEST_LOCATION ->
                if (Build.VERSION.SDK_INT > 23 && ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if(request) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this@HomeActivity,
                                        Manifest.permission.ACCESS_FINE_LOCATION)){
                            UtilMethods.showSnackbar(root_container, "Location permission is required, Please allow from permission manager!!")
                        }else {
                            ActivityCompat.requestPermissions(this@HomeActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_LOCATION)
                        }
                    }
                    return false
                }else{
                    jsPermissionAccepted()
                    return true
                }
            PERMISSIONS_REQUEST_CAMERA ->
                if (Build.VERSION.SDK_INT > 23 && ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    if(request) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this@HomeActivity,
                                        Manifest.permission.CAMERA)){
                            UtilMethods.showSnackbar(root_container, "Camera permission is required, Please allow from permission manager!!")
                        }else {
                            ActivityCompat.requestPermissions(this@HomeActivity, arrayOf(Manifest.permission.CAMERA), PERMISSIONS_REQUEST_CAMERA)
                        }
                    }
                    return false
                }else{
                    jsPermissionAccepted()
                    return true
                }
            PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE ->
                if (Build.VERSION.SDK_INT > 23 && ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if(request) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this@HomeActivity,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                            UtilMethods.showSnackbar(root_container, "Write permission is required, Please allow from permission manager!!")
                        }else {
                            ActivityCompat.requestPermissions(this@HomeActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
                        }
                    }
                    return false
                }else{
                    jsPermissionAccepted()
                    return true
                }
            PERMISSIONS_REQUEST_MICROPHONE ->
                if (Build.VERSION.SDK_INT > 23 && ContextCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    if(request) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this@HomeActivity,
                                        Manifest.permission.RECORD_AUDIO)) {
                            UtilMethods.showSnackbar(root_container, "Audio permission is required, Please allow from permission manager!!")
                        } else
                            ActivityCompat.requestPermissions(this@HomeActivity, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSIONS_REQUEST_MICROPHONE)
                    }
                    return false
                }else{
                    jsPermissionAccepted()
                    return true
                }
        }
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_ALL -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission accept location
                    if (ContextCompat.checkSelfPermission(this,
                                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        UtilMethods.printLog(TAG, "External permission accept.")
                    }

                    // permission accept location
                    if (ContextCompat.checkSelfPermission(this,
                                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        UtilMethods.printLog(TAG, "Location permission accept.")
                        getLocation()
                    }

                } else {
                    //UtilMethods.showSnackbar(root_container, "Permission Failed!")
                }
                return
            }
            PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UtilMethods.printLog(TAG, "Write permission accept.")
                    jsPermissionAccepted()
                } else {
                    UtilMethods.showSnackbar(root_container, "Write Permission Failed!")
                }
            }
            PERMISSIONS_REQUEST_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UtilMethods.printLog(TAG, "Camera permission accept.")
                    jsPermissionAccepted()
                } else {
                    UtilMethods.showSnackbar(root_container, "Camera Permission Failed!")
                }
            }
            PERMISSIONS_REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UtilMethods.printLog(TAG, "Location permission accept.")
                    getLocation()
                    jsPermissionAccepted()
                } else {
                    UtilMethods.showSnackbar(root_container, "Location Permission Failed!")
                }
            }
            PERMISSIONS_REQUEST_MICROPHONE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UtilMethods.printLog(TAG, "Microphone Permission Accept.")
                    jsPermissionAccepted()
                }
                else {
                    UtilMethods.showSnackbar(root_container, "Microphone Permission Failed!")
                }
            }
        }
    }


    // get user location for
    private fun getLocation(): String {
        var newloc = "0,0"
        //Checking for location permissions
        if (askForPermission(PERMISSIONS_REQUEST_LOCATION, false)) {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            val gps = GPSTrack(mContext)
            val latitude = gps.getLatitude()
            val longitude = gps.getLongitude()
            if (gps.canGetLocation()) {
                if (latitude != 0.0 || longitude != 0.0) {
                    cookieManager.setCookie(defaultURL, "lat=$latitude")
                    cookieManager.setCookie(defaultURL, "long=$longitude")
                    newloc = "$latitude,$longitude"
                } else {
                    UtilMethods.printLog(TAG, "Location null.")
                }
            } else {
                UtilMethods.printLog(TAG, "Location read failed.")
            }
        }
        return newloc
    }

    // show about us
    private fun showAboutUs(){
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popUpView = inflater.inflate(R.layout.popup_about_us, null)

        val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
        colorDrawable.alpha = 70

        mAboutUsPopup = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT)
        mAboutUsPopup.setBackgroundDrawable(colorDrawable)
        mAboutUsPopup.isOutsideTouchable = true

        if (Build.VERSION.SDK_INT >= 21) {
            mAboutUsPopup.setElevation(5.0f)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mAboutUsPopup.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
        } else {
            mAboutUsPopup.showAsDropDown(popUpView, Gravity.CENTER, 0)
        }

        val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton
        val btnEmail = popUpView.findViewById<View>(R.id.img_email) as ImageView
        val btnSkype = popUpView.findViewById<View>(R.id.img_skype) as ImageView
        val btnWebsite = popUpView.findViewById<View>(R.id.img_website) as ImageView
        val txtLink = popUpView.findViewById<View>(R.id.txt_link) as TextView

        btnConfirm.background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

        btnConfirm.setOnClickListener { mAboutUsPopup.dismiss() }

        btnEmail.setOnClickListener {
            UtilMethods.sandMailTo(mContext, "Contact with email!", getString(R.string.title_user_email),
                "Contact with via "+ R.string.app_name +" app", "")
        }

        btnWebsite.setOnClickListener { UtilMethods.browseUrlCustomTab(mContext, getString(R.string.title_user_website)) }

        btnSkype.setOnClickListener { UtilMethods.openSkype(mContext, getString(R.string.title_user_skype)) }

        txtLink.setOnClickListener { UtilMethods.browseUrlCustomTab(mContext, getString(R.string.title_user_website)) }
    }

    private fun initSliderMenu() {
        if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_RTL_ACTIVE, false)) {
            navigation_view.layoutDirection = View.LAYOUT_DIRECTION_RTL
            navigation_view.textDirection = View.TEXT_DIRECTION_RTL
        }

        val navigationMenu = navigation_view.menu
        navigationMenu.clear()

        /**
         * If you need to add menu with icon
         * menu.add(0, R.string.menu_home, Menu.NONE, R.string.menu_home).setIcon(R.drawable.ic_home)**/

        var subMenu: SubMenu
        subMenu = navigationMenu.addSubMenu("Home")
        subMenu.add(0, R.string.menu_home, Menu.NONE, getString(R.string.menu_home)).setIcon(R.drawable.ic_home)

        var i = 1
        for(menu in AppDataInstance.navigationMenus){
            when(menu.url) {
                "ABOUT" -> subMenu.add(i++, R.string.menu_about, Menu.NONE, getString(R.string.menu_about)).setIcon(R.drawable.ic_info)
                "RATE" ->  subMenu.add(i++, R.string.menu_rate, Menu.NONE, getString(R.string.menu_rate)).setIcon(R.drawable.ic_rate)
                "SHARE" -> subMenu.add(i++, R.string.menu_share, Menu.NONE, getString(R.string.menu_share)).setIcon(R.drawable.ic_share)
                "EXIT" -> subMenu.add(i++, R.string.menu_exit, Menu.NONE, getString(R.string.menu_exit)).setIcon(R.drawable.ic_exit)


                /*
                // For using menu gorup
                "http://infixsoft.com/" -> {
                    subMenu = navigationMenu.addSubMenu("Website")
                    subMenu.add(i++, i-2, Menu.NONE, menu.menu).setIcon(R.drawable.ic_label)
                }
                */

                // In cage you need to user custom icon
                // "http://infixsoft.com/" ->  subMenu.add(i++, i-2, Menu.NONE, menu.menu).setIcon(R.drawable.ic_label)
                else -> subMenu.add(i++, i-2, Menu.NONE, menu.menu).setIcon(R.drawable.ic_label)
            }
        }

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.string.menu_home -> btn_error_home.callOnClick()

            R.string.menu_about -> showAboutUs()

            R.string.menu_rate -> UtilMethods.rateTheApp(mContext)

            R.string.menu_share -> UtilMethods.shareTheApp(mContext,
                    "Download "+ getString(R.string.app_name)+"" +
                            " app from play store. Click here: "+"" +
                            "https://play.google.com/store/apps/details?id="+packageName+"/")

            R.string.menu_exit -> exitHomeScreen()

            else ->
                try {
                    web_view.loadUrl(AppDataInstance.navigationMenus[item.itemId].url)
                }catch (ex: Exception){
                    ex.printStackTrace()
                }
        }

        if (drawer_layout.isDrawerOpen(GravityCompat.START)) run {
            drawer_layout.closeDrawer(GravityCompat.START)
        }else if (drawer_layout.isDrawerOpen(GravityCompat.END)) run {
            drawer_layout.closeDrawer(GravityCompat.END)
        }
        return true
    }

    private fun exitHomeScreen(){
        this.finish()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            //setActiveFullScreen()
        }
    }

    private fun setActiveFullScreen() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }


    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) run {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else if (drawer_layout.isDrawerOpen(GravityCompat.END)) run {
            drawer_layout.closeDrawer(GravityCompat.END)
        } else if (web_view.canGoBack())
            web_view.goBack()
        else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed()
            }

            doubleBackToExitPressedOnce = true
            UtilMethods.showSnackbar(root_container, getString(R.string.massage_exit))

            Handler().postDelayed({ run {
                doubleBackToExitPressedOnce = false
            }
            }, 2000)

        }
    }

    fun notificationClickSync(){
        if (AppDataInstance.notificationUrl != "" && isApplicationAlive){
            loadBaseWebView()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web_view.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        web_view.restoreState(savedInstanceState)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            readBundle(intent.extras)
        }else{
            UtilMethods.printLog(TAG, "New intent Extras is empty!")
        }
    }

    private fun readBundle(extras: Bundle?) {
        if(extras != null){
            AppDataInstance.notificationUrl = extras.getString(Constants.KEY_NOTIFICATION_URL).orEmpty()
            AppDataInstance.notificationUrlOpenType = extras.getString(Constants.KEY_NOTIFICATION_OPEN_TYPE).orEmpty()

            notificationClickSync()

            UtilMethods.printLog(TAG, "URL: "+AppDataInstance.notificationUrl)
            UtilMethods.printLog(TAG,  "Type: "+AppDataInstance.notificationUrlOpenType)

        }else{
            UtilMethods.printLog(TAG, "New intent Bundle is empty!!")
        }
    }

    override fun onStart() {
        super.onStart()
        isApplicationAlive = true
    }

    @SuppressLint("SwitchIntDef")
    override fun onResume() {
        super.onResume()
        isApplicationAlive = true
        initView()
        initThemeColor()
        initThemeStyle()
        web_view.onResume()
        try {
            mRewardedVideoAd.pause(this)
        }catch (ex: java.lang.Exception){
            ex.printStackTrace()
        }

        /*val appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            when (appUpdateInfo.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                            val snackbar: Snackbar = Snackbar.make(
                                    root_container,
                                    "Update download_finished",
                                    Snackbar.LENGTH_INDEFINITE
                            )
                            snackbar.setAction(
                                    "Update restart",
                                    { view -> appUpdateManager.completeUpdate() })
                            snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.colorAccent))
                            snackbar.show()
                        }
                    }
                }
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE,
                                this,
                                UPDATE_IMMEDIATE_REQUEST_CODE
                        )
                    }
                }
            }
        }*/
    }

    override fun onPause() {
        super.onPause()
        web_view.onPause()
        isApplicationAlive = false
        if(::mRewardedVideoAd.isInitialized &&  mRewardedVideoAd.isLoaded){
            if(isApplicationAlive)
                mRewardedVideoAd.show()
        }
        try {

            if(::mRewardedVideoAd.isInitialized &&  mRewardedVideoAd.isLoaded){
                if(isApplicationAlive)
                    mRewardedVideoAd.pause(this)
            }
        }catch (ex: java.lang.Exception){
            ex.printStackTrace()
        }
    }


    override fun onRestart() {
        super.onRestart()
        isApplicationAlive = true
        notificationClickSync()
        if(lastUrl != ""){
            isViewLoaded = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isApplicationAlive = false
        try {
            if(::mRewardedVideoAd.isInitialized &&  mRewardedVideoAd.isLoaded){
                if(isApplicationAlive)
                    mRewardedVideoAd.pause(this)
            }
        }catch (ex: java.lang.Exception){
            ex.printStackTrace()
        }
    }
}
