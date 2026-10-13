package com.halalcheck.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.halalcheck.app.R
import com.halalcheck.app.domain.model.Madhab

@Composable
fun Madhab.displayName(): String = stringResource(
    when (this) {
        Madhab.HANAFI -> R.string.madhab_hanafi
        Madhab.MALIKI -> R.string.madhab_maliki
        Madhab.SHAFI -> R.string.madhab_shafi
        Madhab.HANBALI -> R.string.madhab_hanbali
    }
)
