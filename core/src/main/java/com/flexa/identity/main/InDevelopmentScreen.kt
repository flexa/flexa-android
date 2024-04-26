package com.flexa.identity.main

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flexa.core.theme.FlexaTheme
import com.flexa.core.view.FlexaProgress

@Composable
internal fun InDevelopmentScreen(
    modifier: Modifier,
) {
    val palette = MaterialTheme.colorScheme

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "In Development", style = MaterialTheme.typography.headlineSmall.copy(color = palette.onBackground))
            Spacer(modifier = Modifier.height(16.dp))
            FlexaProgress(
                modifier = Modifier.size(72.dp),
                roundedCornersSize = 16.dp,
                borderWidth = 2.dp
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
internal fun InDevelopmentScreenPreview() {
    FlexaTheme {
        Surface {
            InDevelopmentScreen(modifier = Modifier.fillMaxSize())
        }
    }
}