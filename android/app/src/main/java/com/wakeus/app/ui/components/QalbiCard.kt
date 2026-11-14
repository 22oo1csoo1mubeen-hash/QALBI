package com.wakeus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard surface card used for sections and list items in QALBI's romantic theme.
 */
@Composable
fun QalbiCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 1.dp,
    innerPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF9FA),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = BorderStroke(1.dp, Color(0xFFF3D4DC)),
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(innerPadding),
            content = content,
        )
    }
}
