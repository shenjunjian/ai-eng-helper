package com.shenjj.edu.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.shenjj.edu.R
import com.shenjj.edu.ui.theme.Ink

/**
 * 二级页顶部导航条（原型 navBar）：左返回箭头 + 标题，右可挂操作区。
 * 不含状态栏留白，调用方自行加 statusBarsPadding。
 */
@Composable
fun NavBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (onBack != null) {
            val interaction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "返回",
                    tint = Ink,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onBack == null) 14.dp else 2.dp),
        )
        if (trailing != null) {
            trailing()
        }
    }
}
