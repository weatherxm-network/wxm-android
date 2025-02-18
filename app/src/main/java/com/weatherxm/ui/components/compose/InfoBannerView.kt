package com.weatherxm.ui.components.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherxm.R

@Suppress("FunctionNaming", "LongParameterList", "MagicNumber")
@Composable
fun InfoBannerView(
    title: String,
    subtitle: String,
    actionLabel: String,
    showActionButton: Boolean,
    showCloseButton: Boolean,
    onAction: (() -> Unit)?,
    onClose: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .background(colorResource(R.color.layer1))
            .padding(
                dimensionResource(R.dimen.padding_normal),
                dimensionResource(R.dimen.padding_large)
            ),
        verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_small))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = colorResource(R.color.colorOnSurface),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(if (showCloseButton) 0.8F else 1F)
            )
            if (showCloseButton) {
                IconButton(
                    onClick = { onClose?.invoke() },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = colorResource(R.color.colorOnSurface)
                    ),
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.action_close)
                    )
                }
            }
        }
        if (subtitle.isNotEmpty()) {
            MediumText(subtitle)
        }
        if (showActionButton) {
            Button(
                onClick = { onAction?.invoke() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.colorSurface),
                    contentColor = colorResource(R.color.textColor)
                ),
                modifier = Modifier.padding(
                    0.dp, dimensionResource(R.dimen.padding_small), 0.dp, 0.dp
                )
            ) {
                LargeText(actionLabel, fontWeight = FontWeight.Bold, colorRes = R.color.textColor)
                Icon(
                    modifier = Modifier
                        .padding(start = dimensionResource(R.dimen.padding_small))
                        .size(18.dp),
                    painter = painterResource(R.drawable.ic_open_new),
                    tint = colorResource(R.color.textColor),
                    contentDescription = null
                )
            }
        }
    }
}

@Suppress("FunctionNaming")
@Preview
@Composable
fun PreviewInfoBannerView() {
    InfoBannerView(
        title = "Welcome to mainnet!",
        subtitle = "Starting the 14th of February all station rewards are distributed........",
        actionLabel = "Learn More",
        showActionButton = true,
        showCloseButton = true,
        onAction = {},
        onClose = {}
    )
}
