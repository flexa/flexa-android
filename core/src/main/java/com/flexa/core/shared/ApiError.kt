package com.flexa.core.shared

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import com.flexa.R
import com.flexa.core.Flexa
import java.util.Locale

sealed class ApiError(
    open val traceId: String? = null,
    open val message: String? = null,
) {

    class InfoEntity(
        val title: String? = null,
        override val message: String? = null,
        override val traceId: String? = null,
    ) : ApiError(traceId = traceId, message = message)

    class ReportEntity(
        val data: String? = null,
        override val message: String? = null,
        override val traceId: String? = null,
    ) : ApiError(traceId = traceId, message = message) {
        val title get() = Flexa.requiredContext.resources.getString(R.string.something_went_wrong)
        val text
            get() = message
                ?: Flexa.requiredContext.resources.getString(R.string.we_are_sorry_we_encountered_a_problem)

        fun sendEmailReport(activity: Activity) {
            activity.run {
                val errorType = traceId ?: getString(R.string.client_error)

                val messageBody = StringBuilder()
                messageBody.append(getString(R.string.report_email_message_body))
                messageBody.append("\n• Trace id: $errorType")
                messageBody.append("\n• ${
                    FlexaConstants.ANDROID.replaceFirstChar {
                        it.uppercase(Locale.getDefault())
                    }
                } API ${Build.VERSION.SDK_INT}"
                )
                messageBody.append("\n• ${Build.MANUFACTURER} ${Build.MODEL}")
                try {
                    val packageInfo = when {
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ->
                            application.packageManager
                                .getPackageInfo(application.packageName, 0)

                        else -> application.packageManager
                            .getPackageInfo(
                                application.packageName,
                                PackageManager.PackageInfoFlags.of(0)
                            )
                    }
                    val appName = getString(packageInfo.applicationInfo.labelRes)
                    val appVersion = packageInfo.versionName
                    messageBody.append("\n• App name: $appName")
                    messageBody.append("\n• App version: $appVersion").append("\n\n")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                data?.let { messageBody.append(it) }

                val uriText = "mailto:${getString(R.string.flexa_report_email)}" +
                        "?subject=" + Uri.encode(getString(R.string.report_an_issue)) +
                        "&body=" + Uri.encode(messageBody.toString())
                val uri: Uri = Uri.parse(uriText)
                val sendIntent = Intent(Intent.ACTION_SENDTO)
                sendIntent.data = uri
                try {
                    startActivity(Intent.createChooser(sendIntent, getString(R.string.send_email)))
                } catch (ex: ActivityNotFoundException) {
                    Log.e(ReportEntity::class.java.simpleName, ex.message, ex)
                }
            }
        }
    }
}
