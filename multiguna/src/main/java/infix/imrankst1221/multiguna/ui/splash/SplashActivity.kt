package infix.imrankst1221.multiguna.ui.splash

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import androidx.core.content.ContextCompat
import android.view.View
import infix.imrankst1221.multiguna.R
import infix.imrankst1221.multiguna.ui.home.HomeActivity
import infix.imrankst1221.rocket.library.setting.ThemeBaseActivity
import infix.imrankst1221.rocket.library.utility.PreferenceUtils
import infix.imrankst1221.rocket.library.utility.Constants
import infix.imrankst1221.rocket.library.utility.UtilMethods
import kotlinx.android.synthetic.main.activity_splash.*


class SplashActivity : ThemeBaseActivity() {
    private lateinit var mContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        mContext = this

        // setup background gradient
        val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext,UtilMethods.getThemePrimaryDarkColor())))
        gradientDrawable.cornerRadius = 0f
        view_background.background = gradientDrawable

        // fullscreen setup
        if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_FULL_SCREEN_ACTIVE, false)){
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LOW_PROFILE
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }

        // start
        Handler().postDelayed({
            /* Create an Intent that will start the Menu-Activity. */
            val mainIntent = Intent(this, HomeActivity::class.java)
            startActivity(mainIntent)
            finish()
        }, 3000)
    }

}
