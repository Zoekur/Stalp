package com.example.stalp.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object StalpIcons {
    val Settings: ImageVector
        get() {
            if (_settings != null) return _settings!!
            _settings = ImageVector.Builder(
                name = "Settings",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 24.0f,
                viewportHeight = 24.0f
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1.0f,
                    stroke = null,
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(19.14f, 12.94f)
                    curveTo(19.18f, 12.64f, 19.2f, 12.33f, 19.2f, 12.0f)
                    curveTo(19.2f, 11.67f, 19.18f, 11.36f, 19.14f, 11.06f)
                    lineTo(21.17f, 9.48f)
                    curveTo(21.35f, 9.34f, 21.4f, 9.07f, 21.29f, 8.87f)
                    lineTo(19.37f, 5.55f)
                    curveTo(19.25f, 5.33f, 19.0f, 5.26f, 18.78f, 5.33f)
                    lineTo(16.39f, 6.29f)
                    curveTo(15.89f, 5.91f, 15.36f, 5.59f, 14.77f, 5.35f)
                    lineTo(14.41f, 2.81f)
                    curveTo(14.37f, 2.57f, 14.17f, 2.4f, 13.93f, 2.4f)
                    lineTo(10.07f, 2.4f)
                    curveTo(9.83f, 2.4f, 9.63f, 2.57f, 9.59f, 2.81f)
                    lineTo(9.23f, 5.35f)
                    curveTo(8.64f, 5.59f, 8.11f, 5.91f, 7.61f, 6.29f)
                    lineTo(5.22f, 5.33f)
                    curveTo(5.0f, 5.26f, 4.75f, 5.33f, 4.63f, 5.55f)
                    lineTo(2.71f, 8.87f)
                    curveTo(2.6f, 9.07f, 2.65f, 9.34f, 2.83f, 9.48f)
                    lineTo(4.86f, 11.06f)
                    curveTo(4.82f, 11.36f, 4.8f, 11.67f, 4.8f, 12.0f)
                    curveTo(4.8f, 12.33f, 4.82f, 12.64f, 4.86f, 12.94f)
                    lineTo(2.83f, 14.52f)
                    curveTo(2.65f, 14.66f, 2.6f, 14.93f, 2.71f, 15.13f)
                    lineTo(4.63f, 18.45f)
                    curveTo(4.75f, 18.67f, 5.0f, 18.74f, 5.22f, 18.67f)
                    lineTo(7.61f, 17.71f)
                    curveTo(8.11f, 18.09f, 8.64f, 18.41f, 9.23f, 18.65f)
                    lineTo(9.59f, 21.19f)
                    curveTo(9.63f, 21.43f, 9.83f, 21.6f, 10.07f, 21.6f)
                    lineTo(13.93f, 21.6f)
                    curveTo(14.17f, 21.6f, 14.37f, 21.43f, 14.41f, 21.19f)
                    lineTo(14.77f, 18.65f)
                    curveTo(15.36f, 18.41f, 15.89f, 18.09f, 16.39f, 17.71f)
                    lineTo(18.78f, 18.67f)
                    curveTo(19.0f, 18.74f, 19.25f, 18.67f, 19.37f, 18.45f)
                    lineTo(21.29f, 15.13f)
                    curveTo(21.4f, 14.93f, 21.35f, 14.66f, 21.17f, 14.52f)
                    lineTo(19.14f, 12.94f)
                    close()
                    moveTo(12.0f, 15.6f)
                    curveTo(10.01f, 15.6f, 8.4f, 13.99f, 8.4f, 12.0f)
                    curveTo(8.4f, 10.01f, 10.01f, 8.4f, 12.0f, 8.4f)
                    curveTo(13.99f, 8.4f, 15.6f, 10.01f, 15.6f, 12.0f)
                    curveTo(15.6f, 13.99f, 13.99f, 15.6f, 12.0f, 15.6f)
                    close()
                }
            }.build()
            return _settings!!
        }

    private var _settings: ImageVector? = null

    val ArrowBack: ImageVector
        get() {
            if (_arrowBack != null) return _arrowBack!!
            _arrowBack = ImageVector.Builder(
                name = "ArrowBack",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 24.0f,
                viewportHeight = 24.0f
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1.0f,
                    stroke = null,
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(20.0f, 11.0f)
                    horizontalLineTo(7.83f)
                    lineTo(13.42f, 5.41f)
                    lineTo(12.0f, 4.0f)
                    lineTo(4.0f, 12.0f)
                    lineTo(12.0f, 20.0f)
                    lineTo(13.41f, 18.59f)
                    lineTo(7.83f, 13.0f)
                    horizontalLineTo(20.0f)
                    verticalLineTo(11.0f)
                    close()
                }
            }.build()
            return _arrowBack!!
        }

    private var _arrowBack: ImageVector? = null
}
