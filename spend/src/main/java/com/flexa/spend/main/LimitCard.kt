package com.flexa.spend.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flexa.core.theme.FlexaTheme
import com.flexa.spend.R

@Composable
internal fun LimitCard(
    modifier: Modifier,
    toLearnMore: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = modifier
    ) {
        IconButton(
            modifier = Modifier.align(Alignment.TopEnd),
            onClick = { onClose.invoke() }) {
            Icon(
                modifier = Modifier.size(22.dp),
                imageVector = Icons.Rounded.Close,
                contentDescription = null
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, start = 20.dp)
        ) {
            val palette = MaterialTheme.colorScheme
            Canvas(modifier = Modifier.size(46.dp),
                onDraw = { drawCircle(color = palette.outline) })
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 30.dp),
                    text = stringResource(id = R.string.weekly_limit),
                    style = TextStyle(
                        fontWeight = FontWeight.W600,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 30.dp),
                    text = stringResource(id = R.string.upgrade_id),
                    style = TextStyle(
                        fontWeight = FontWeight.W400,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Divider(
                    modifier = Modifier.padding(start = 16.dp),
                    thickness = .5.dp,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    modifier = Modifier.padding(start = 4.dp),
                    onClick = {toLearnMore.invoke()}) {
                    Text(
                        text = stringResource(id = R.string.learn_more),
                        style = TextStyle(
                            fontWeight = FontWeight.W400,
                            fontSize = 18.sp,
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xff232323)
@Composable
private fun LimitCardPreview() {
    FlexaTheme {
        Surface {
            LimitCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.White)
                    .clip(RoundedCornerShape(14.dp))
                ,
                toLearnMore = {},
                onClose = {}
            )
        }
    }
}
