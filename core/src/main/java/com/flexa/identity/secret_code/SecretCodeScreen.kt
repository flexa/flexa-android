package com.flexa.identity.secret_code

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flexa.R
import com.flexa.core.entity.AppAccount
import com.flexa.core.shared.ErrorDialog
import com.flexa.core.theme.FlexaTheme
import com.flexa.core.view.FlexaProgress
import com.flexa.identity.domain.FakeInteractor
import com.flexa.identity.mirror

@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@Composable
internal fun SecretCodeScreen(
    modifier: Modifier = Modifier,
    viewModel: SecretCodeViewModel,
    onBack: () -> Unit = {},
    onClose: (List<AppAccount>?) -> Unit = {}
) {
    val palette = MaterialTheme.colorScheme
    var showDialog by remember { mutableStateOf(false) }
    val result by viewModel.result.collectAsStateWithLifecycle(initialValue = null)

    BackHandler { onBack() }

    Column(modifier = modifier) {
        IconButton(onClick = { onBack() }) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = palette.onBackground
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .weight(1F),
        ) {
            val progress by viewModel.progress.collectAsStateWithLifecycle()

            Spacer(modifier = Modifier.height(30.dp))
            Box {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !progress,
                    enter = scaleIn(initialScale = .7F) + fadeIn(),
                    exit = scaleOut(targetScale = .7F) + fadeOut()
                ) {
                    Icon(
                        modifier = Modifier
                            .size(42.dp)
                            .mirror()
                            .fillMaxSize(),
                        imageVector = Icons.Outlined.LockReset,
                        contentDescription = null,
                        tint = palette.primary
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = progress,
                    enter = scaleIn(initialScale = .7F) + fadeIn(),
                    exit = scaleOut(targetScale = .7F) + fadeOut()
                ) {
                    FlexaProgress(
                        modifier = Modifier
                            .size(70.dp)
                            .fillMaxSize()
                            .padding(8.dp),
                        roundedCornersSize = 12.dp,
                        borderWidth = 2.dp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (!progress) stringResource(id = R.string.enter_your_verification_code)
                else stringResource(id = R.string.processing) + "...",
                style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Normal),
                color = palette.onBackground
            )
            Spacer(modifier = Modifier.height(24.dp))
            val secretCode by viewModel.secretCode.collectAsStateWithLifecycle()
            AnimatedVisibility(visible = !progress) {
                SecretCode(
                    modifier = Modifier.fillMaxWidth(),
                    clickable = !progress,
                    value = secretCode?:"",
                    onValueChanged = { viewModel.secretCode.value = it },
                    onFulfilled = {
                        viewModel.loginWithMagicCode(it)
                    }
                )
            }
        }
        TextButton(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = { showDialog = true }) {
            Text(text = stringResource(id = R.string.didnt_get_a_code))
        }
        Spacer(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(24.dp)
        )
    }
    if (showDialog) {
        AlertDialog(
            shape = RoundedCornerShape(25.dp),
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.didnt_get_a_code),
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.secret_code_description),
                    style = TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
                )
            },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text(text = stringResource(id = R.string.got_it))
                }
            }
        )

    }
    ErrorDialog(errorHandler = viewModel.errorHandler) {
        viewModel.secretCode.value = null
    }
    if (result != null) {
        AlertDialog(
            confirmButton = {
                TextButton(onClick = { onClose.invoke(result) }) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            title = { Text(text = "Success!") },
            text = { Text(text = "You are signed in.") },
            onDismissRequest = { onClose.invoke(result) }
        )
    }
}

@Preview(name = "Light")
@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun SecretCodeScreenPreview() {
    FlexaTheme {
        Surface {
            SecretCodeScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel(initializer = {
                    SecretCodeViewModel(FakeInteractor())
                }),
            )
        }
    }
}
