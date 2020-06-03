package infix.imrankst1221.multiguna.setting

import android.content.Context
import android.content.Intent
import com.onesignal.OSNotificationOpenResult
import com.onesignal.OneSignal
import infix.imrankst1221.multiguna.MainActivity
import infix.imrankst1221.multiguna.ui.home.HomeActivity
import infix.imrankst1221.rocket.library.utility.Constants
import infix.imrankst1221.rocket.library.utility.UtilMethods
import org.json.JSONException
import java.lang.Exception

class OneSignalNotificationOpenedHandler(val mContext: Context) : OneSignal.NotificationOpenedHandler {
    private val TAG : String  = "---OneSignalHandler"
    override fun notificationOpened(result: OSNotificationOpenResult) {
        try {
            val customKeyURL: String?
            val customKeyType: String?
            val actionType = result.action.type
            val notification = result.notification
            val launchURL = notification.payload.launchURL
            val additionalData = notification.payload.additionalData

            var notificationUrl: String = ""
            var notificationUrlOpenType: String = ""

            if (launchURL != null) {
                notificationUrl = launchURL
                notificationUrlOpenType = "INSIDE"
                UtilMethods.printLog(TAG, "launchURL = $launchURL")
            }

            if (additionalData != null) {
                customKeyURL = additionalData.optString(Constants.KEY_NOTIFICATION_URL, "")
                if (customKeyURL.isNotEmpty() ){
                    customKeyType = additionalData.optString(Constants.KEY_NOTIFICATION_OPEN_TYPE, "INSIDE").toUpperCase()
                    notificationUrl = customKeyURL
                    notificationUrlOpenType = customKeyType

                    UtilMethods.printLog(TAG, "customType = $customKeyType")
                    UtilMethods.printLog(TAG, "customURL = $customKeyURL")
                }
            }


            if (HomeActivity::class.isInstance(mContext)){
                UtilMethods.printLog(TAG, "HomeActivity Instance.")
                try {
                    AppDataInstance.notificationUrl = notificationUrl
                    AppDataInstance.notificationUrlOpenType = notificationUrlOpenType
                    (mContext as HomeActivity).notificationClickSync()
                }catch (ex: Exception){
                    UtilMethods.printLog(TAG, ""+ex.message)
                }
            }else {
                val intent = Intent(mContext.applicationContext, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK

                if (notificationUrl.isNotEmpty()) {
                    intent.putExtra(Constants.KEY_NOTIFICATION_URL, notificationUrl)
                    intent.putExtra(Constants.KEY_NOTIFICATION_OPEN_TYPE, notificationUrlOpenType)
                }
                mContext.startActivity(intent)
            }

        } catch (ex: JSONException) {
            UtilMethods.printLog(TAG, ex.message.toString())
        }
    }
}