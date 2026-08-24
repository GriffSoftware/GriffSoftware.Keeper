package com.griff.keeper.presentation.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.griff.keeper.presentation.theme.GriffGradients
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.ThemePreviews

/**
 * The redesign's floating action button: a 52dp navy-gradient circle. Stock
 * [androidx.compose.material3.FloatingActionButton] only accepts a solid `containerColor`, so the
 * add actions on Subscriptions and Obligations build the gradient fill directly instead.
 */
@Composable
fun GriffFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(Size)
            .shadow(elevation = 12.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(GriffGradients.accent())
            .clickable(onClickLabel = contentDescription, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = contentDescription,
            tint = GriffGradients.OnAccent,
        )
    }
}

private val Size = 52.dp

@ThemePreviews
@Composable
private fun GriffFabPreview() {
    GriffThemePreview {
        GriffFab(onClick = {}, contentDescription = "Add")
    }
}
