package com.shenjj.edu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shenjj.edu.R
import com.shenjj.edu.ui.theme.Ink
import com.shenjj.edu.ui.theme.Ink2
import com.shenjj.edu.ui.theme.Radii
import com.shenjj.edu.ui.theme.Spark

/**
 * 启动页（原型 splash）：品牌 mark + TalkFlow 标题 + 副标题。
 * 登录态分流由 NavHost 的 splashGraph 负责，本页只呈现品牌视觉。
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 30.dp),
    ) {
        Column(modifier = Modifier.weight(1f).padding(bottom = 60.dp)) {
            Box(modifier = Modifier.padding(top = 40.dp)) {
                // .mark 62dp 墨底圆角方块 + 白色耳机/麦克风图标
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .background(Ink, RoundedCornerShape(Radii.Mark)),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            // TalkFlow：Flow 用 --spark
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Ink)) { append("Talk") }
                    withStyle(SpanStyle(color = Spark)) { append("Flow") }
                },
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.padding(top = 26.dp, bottom = 12.dp),
            )
            Text(
                text = "开口即学\n生词在对话里自己长出来",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.5.sp, lineHeight = 25.sp),
                color = Ink2,
            )
        }
    }
}
