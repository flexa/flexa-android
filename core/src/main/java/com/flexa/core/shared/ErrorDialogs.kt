package com.flexa.core.shared

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ErrorDialog(
    errorHandler: ApiErrorHandler,
    onDismissed: (() -> Unit)? = null
) {
    if (errorHandler.hasError) {
        val context = LocalContext.current
        when (val error = errorHandler.error) {
            is ApiError.InfoEntity -> {
                AlertDialog(
                    shape = RoundedCornerShape(14.dp),
                    onDismissRequest = {
                        errorHandler.error = null
                        onDismissed?.invoke()
                    },
                    title = {
                        Text(
                            text = error.title ?: "",
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            errorHandler.error = null
                            onDismissed?.invoke()
                        }) {
                            Text(text = stringResource(id = android.R.string.ok))
                        }
                    }
                )
            }
            is ApiError.ReportEntity -> {
                AlertDialog(
                    shape = RoundedCornerShape(14.dp),
                    onDismissRequest = {
                        errorHandler.error = null
                        onDismissed?.invoke()
                    },
                    title = {
                        Text(
                            text = error.title,
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        )
                    },
                    text = { Text(text = error.text, style = TextStyle(fontSize = 16.sp)) },
                    dismissButton = {
                        TextButton(onClick = {
                            if (context is Activity) error.sendEmailReport(context)
                            errorHandler.error = null
                            onDismissed?.invoke()
                        }) {
                            Text(text = "Report")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            errorHandler.error = null
                            onDismissed?.invoke()
                        }) {
                            Text(text = stringResource(id = android.R.string.ok))
                        }
                    }
                )
            }
            else -> {}
        }
    }
}
