package com.weatherxm.ui.components.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.weatherxm.R
import com.weatherxm.ui.common.UploadPhotosState

@Suppress("FunctionNaming", "MagicNumber")
@Composable
fun PhotoUploadState(state: UploadPhotosState, showStationName: Boolean) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_extra_small))
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_small))
        ) {
            if (!state.isError) {
                Text(
                    text = "${state.progress}%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.colorOnSurface)
                )
                if (state.isSuccess) {
                    MediumText(text = stringResource(R.string.photo_upload_completed))
                } else {
                    MediumText(text = stringResource(R.string.uploading))
                }
            } else {
                MediumText(text = stringResource(R.string.upload_failed_retry))
            }
        }
        if (!state.isError) {
            val progressForIndicator = state.progress.toFloat() / 100
            LinearProgressIndicator(
                progress = { progressForIndicator },
                modifier = Modifier.fillMaxWidth(),
                strokeCap = StrokeCap.Round,
                color = colorResource(R.color.colorPrimary),
                trackColor = colorResource(R.color.layer2)
            )
        }
        if (showStationName) {
            SmallText(
                text = state.device.getDefaultOrFriendlyName(),
                colorRes = R.color.darkGrey
            )
        }
    }
}
