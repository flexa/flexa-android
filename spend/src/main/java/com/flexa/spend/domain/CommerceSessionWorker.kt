package com.flexa.spend.domain

import android.content.Context
import android.util.Log
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flexa.core.Flexa
import com.flexa.spend.Spend
import com.flexa.spend.SpendConstants
import com.flexa.spend.main.confirm.TransactionError

class CommerceSessionWorker(
    appContext: Context, params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        restoreContext(applicationContext)

        inputData.getString(SpendConstants.COMMERCE_SESSION_KEY)?.let { id ->
            return try {
                Log.d(null, "CommerceSessionWorker doWork: Id > $id")
                Spend.interactor.closeCommerceSession(id)
                Spend.onTransactionRequest?.invoke(kotlin.Result.failure(TransactionError(id)))
                Log.d(null, "CommerceSessionWorker success: Id > $id")
                Result.success()
            } catch (ex: Exception) {
                Log.d(null, "CommerceSessionWorker retry: Id > $id\"")
                Result.retry()
            }
        }
        Log.d(null, "CommerceSessionWorker retry: ")
        return Result.retry()
    }

    private fun restoreContext(context: Context) {
        if (Flexa.context == null) Flexa.context = context
    }

    companion object {
        fun execute(
            context: Context,
            commerceSessionId: String
        ) {
            val data = Data.Builder()
            data.putString(SpendConstants.COMMERCE_SESSION_KEY, commerceSessionId)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequest
                .Builder(CommerceSessionWorker::class.java)
                .setInputData(data.build())
                .setConstraints(constraints)
                .build()
            if (!WorkManager.isInitialized()) {
                val config = Configuration.Builder().build()
                runCatching { WorkManager.initialize(context, config) }
            }
            WorkManager.getInstance(context)
                .enqueue(request)
        }
    }
}
