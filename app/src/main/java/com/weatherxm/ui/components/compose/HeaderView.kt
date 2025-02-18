package com.weatherxm.ui.components.compose

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.weatherxm.R

@Suppress("FunctionNaming")
@Composable
fun HeaderView(
    title: String,
    subtitle: String?,
    onInfoButton: (() -> Unit)?
) {
    Column(verticalArrangement = spacedBy(dimensionResource(R.dimen.margin_extra_small))) {
        Row(
            horizontalArrangement = spacedBy(dimensionResource(R.dimen.margin_small)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Title(title)
            onInfoButton?.let {
                IconButton(
                    onClick = { onInfoButton() },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = colorResource(R.color.colorOnSurface)
                    ),
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_learn_more_info),
                        contentDescription = stringResource(R.string.read_more)
                    )
                }
            }
        }
        subtitle?.let {
            MediumText(subtitle, colorRes = R.color.darkGrey)
        }
    }
}

// tint = colorResource(data.action.foregroundTint),
@Suppress("FunctionNaming")
@Preview
@Composable
fun PreviewHeaderView() {
    HeaderView(title = "Title", subtitle = "Subtitle", onInfoButton = {})
}
