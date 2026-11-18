package io.github.xvsu.quickspacewidget.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import io.github.xvsu.quickspacewidget.R
import io.github.xvsu.quickspacewidget.model.QuickSpaceSettings
import io.github.xvsu.quickspacewidget.model.ResolvedWidgetContent
import io.github.xvsu.quickspacewidget.model.TextAlignment
import io.github.xvsu.quickspacewidget.model.WidgetStyle
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

object DoubleShadowBitmapRenderer {

    private const val MAX_BITMAP_PIXELS = 650_000f
    private const val AMBIENT_SHADOW_BASE_ALPHA = 0x40 // 64
    private const val KEY_SHADOW_BASE_ALPHA = 0x89     // 137

    fun render(
        context: Context,
        settings: QuickSpaceSettings,
        content: ResolvedWidgetContent,
        colors: ResolvedWidgetColors,
        targetWidthPx: Int,
        targetHeightPx: Int,
    ): Bitmap {
        val metrics = context.resources.displayMetrics
        val rawW = max(targetWidthPx, 120)
        val rawH = max(targetHeightPx, 60)

        val totalPixels = rawW.toFloat() * rawH.toFloat()
        val scale = if (totalPixels > MAX_BITMAP_PIXELS) {
            sqrt(MAX_BITMAP_PIXELS / totalPixels)
        } else {
            1f
        }

        val width = max((rawW * scale).roundToInt(), 100)
        val height = max((rawH * scale).roundToInt(), 48)
        val dp = metrics.density * scale

        fun spToPx(sp: Float): Float =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics) * scale

        val shadowFactor = if (colors.isLightText) settings.shadowStrengthPercent / 100f else 0f
        val shadowBleed = 4f * dp * shadowFactor
        val hPad = max(settings.horizontalPaddingDp * dp, ceil(shadowBleed))
        val availableWidth = max((width - hPad * 2f).toInt(), 40)

        val layoutAlign = when (settings.alignment) {
            TextAlignment.START -> Layout.Alignment.ALIGN_NORMAL
            TextAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlignment.END -> Layout.Alignment.ALIGN_OPPOSITE
        }

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            color = colors.textColor
        }
        val accentPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            color = colors.accentColor
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            color = colors.textColor
            letterSpacing = 0.01f
        }

        fun buildRows(fitScale: Float): Pair<List<RenderRow>, Float> {
            val spacing = settings.lineSpacingDp * dp * fitScale
            val rows = mutableListOf<RenderRow>()

            if (settings.style == WidgetStyle.EXTENDED) {
                val greetSize = spToPx((settings.titleSizeSp - 2).coerceAtLeast(13) * fitScale)
                accentPaint.textSize = greetSize
                rows += RenderRow.SingleText(
                    layout = buildLayout(content.greeting, accentPaint, availableWidth, layoutAlign, maxLines = 1),
                    paint = TextPaint(accentPaint),
                )

                val dateLineSize = spToPx((settings.titleSizeSp - 4).coerceAtLeast(12) * fitScale)
                accentPaint.textSize = dateLineSize
                titlePaint.textSize = dateLineSize
                rows += RenderRow.PrefixAndText(
                    prefix = content.clockPrefix,
                    prefixPaint = TextPaint(accentPaint),
                    mainText = content.dateText,
                    mainPaint = TextPaint(titlePaint),
                    gapPx = 6f * dp * fitScale,
                    maxWidth = availableWidth,
                )
            } else {
                titlePaint.textSize = spToPx(settings.titleSizeSp * fitScale)
                rows += RenderRow.SingleText(
                    layout = buildLayout(content.dateText, titlePaint, availableWidth, layoutAlign, maxLines = 1),
                    paint = TextPaint(titlePaint),
                )
            }

            val quote = content.quoteText
            if (!quote.isNullOrBlank()) {
                subtitlePaint.textSize = spToPx(settings.subtitleSizeSp * fitScale)
                val rawIconSize = if (content.showEasterIcon) (20f * dp * fitScale).roundToInt() else 0
                val rawIconGap = if (content.showEasterIcon) (6f * dp * fitScale) else 0f
                val canFitIcon = rawIconSize > 0 && availableWidth > rawIconSize + rawIconGap + 24f
                val iconSizePx = if (canFitIcon) rawIconSize else 0
                val iconGapPx = if (canFitIcon) rawIconGap else 0f
                val subTextMaxW = max(availableWidth - iconSizePx - iconGapPx.roundToInt(), 24)
                val subLayout = buildLayout(quote, subtitlePaint, subTextMaxW, layoutAlign, maxLines = 2)
                rows += RenderRow.Subtitle(
                    layout = subLayout,
                    paint = TextPaint(subtitlePaint),
                    iconSizePx = iconSizePx,
                    iconGapPx = iconGapPx,
                )
            }

            val totalH = rows.foldIndexed(0f) { index, acc, row ->
                acc + row.height + if (index > 0) spacing else 0f
            }
            return rows to totalH
        }

        val usableHeight = max(24f, height - 2f * shadowBleed)
        var (rows, totalContentHeight) = buildRows(fitScale = 1f)
        var spacing = settings.lineSpacingDp * dp
        if (totalContentHeight > usableHeight && totalContentHeight > 0f) {
            val fitScale = (usableHeight / totalContentHeight).coerceIn(0.55f, 1f)
            spacing *= fitScale
            val rebuilt = buildRows(fitScale)
            rows = rebuilt.first
            totalContentHeight = rebuilt.second
        }

        val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        var currentY = max(shadowBleed, (height - totalContentHeight) / 2f)

        rows.forEachIndexed { index, row ->
            if (index > 0) currentY += spacing
            when (row) {
                is RenderRow.SingleText -> {
                    canvas.withSave {
                        translate(hPad, currentY)
                        drawDoubleShadowLayout(this, row.layout, row.paint, dp, shadowFactor)
                    }
                }
                is RenderRow.PrefixAndText -> {
                    val prefixW = row.prefixPaint.measureText(row.prefix)
                    val mainW = row.mainPaint.measureText(row.mainText)
                    val combinedW = min(prefixW + row.gapPx + mainW, row.maxWidth.toFloat())
                    val startX = when (settings.alignment) {
                        TextAlignment.START -> hPad
                        TextAlignment.CENTER -> hPad + (row.maxWidth - combinedW) / 2f
                        TextAlignment.END -> hPad + (row.maxWidth - combinedW)
                    }
                    val fm = row.mainPaint.fontMetrics
                    val baselineY = currentY - fm.ascent
                    drawDoubleShadowText(
                        canvas, row.prefix, startX, baselineY, row.prefixPaint, dp, shadowFactor
                    )
                    val remainingMainW = max(row.maxWidth - prefixW - row.gapPx, 20f)
                    val clippedMain = TextUtils.ellipsize(
                        row.mainText, row.mainPaint, remainingMainW, TextUtils.TruncateAt.END
                    ).toString()
                    drawDoubleShadowText(
                        canvas,
                        clippedMain,
                        startX + prefixW + row.gapPx,
                        baselineY,
                        row.mainPaint,
                        dp,
                        shadowFactor,
                    )
                }
                is RenderRow.Subtitle -> {
                    val lineCount = row.layout.lineCount
                    var maxLineW = 0f
                    for (i in 0 until lineCount) {
                        maxLineW = max(maxLineW, row.layout.getLineWidth(i))
                    }
                    val totalRowW = row.iconSizePx + row.iconGapPx + maxLineW
                    val rowStartX = when (settings.alignment) {
                        TextAlignment.START -> hPad
                        TextAlignment.CENTER -> hPad + max(0f, (availableWidth - totalRowW) / 2f)
                        TextAlignment.END -> hPad + max(0f, availableWidth - totalRowW)
                    }
                    if (row.iconSizePx > 0) {
                        val iconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_quickspace_crdroid)?.mutate()
                        if (iconDrawable != null) {
                            val top = (currentY + (row.height - row.iconSizePx) / 2f).roundToInt()
                            val left = rowStartX.roundToInt()
                            iconDrawable.setBounds(left, top, left + row.iconSizePx, top + row.iconSizePx)
                            iconDrawable.colorFilter = PorterDuffColorFilter(colors.textColor, PorterDuff.Mode.SRC_IN)
                            iconDrawable.draw(canvas)
                        }
                    }
                    val textOffsetX = hPad + row.iconSizePx + row.iconGapPx
                    canvas.withSave {
                        translate(textOffsetX, currentY)
                        drawDoubleShadowLayout(this, row.layout, row.paint, dp, shadowFactor)
                    }
                }
            }
            currentY += row.height
        }

        val immutable = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        return if (immutable != null) {
            bitmap.recycle()
            immutable
        } else {
            bitmap
        }
    }

    private fun buildLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        alignment: Layout.Alignment,
        maxLines: Int,
    ): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, max(width, 20))
            .setAlignment(alignment)
            .setIncludePad(false)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()

    private fun drawDoubleShadowLayout(
        canvas: Canvas,
        layout: StaticLayout,
        paint: TextPaint,
        dp: Float,
        shadowFactor: Float,
    ) {
        if (shadowFactor <= 0.01f) {
            paint.clearShadowLayer()
            layout.draw(canvas)
            return
        }
        val textAlpha = Color.alpha(paint.color)
        val ambientBlur = max(0.5f, 3f * dp * shadowFactor)
        val ambientColor = scaledShadowColor(AMBIENT_SHADOW_BASE_ALPHA, shadowFactor, textAlpha)
        paint.setShadowLayer(ambientBlur, 0f, 0f, ambientColor)
        layout.draw(canvas)

        val keyBlur = max(0.5f, 2f * dp * shadowFactor)
        val keyDx = 0.5f * dp * shadowFactor
        val keyColor = scaledShadowColor(KEY_SHADOW_BASE_ALPHA, shadowFactor, textAlpha)
        paint.setShadowLayer(keyBlur, keyDx, 0f, keyColor)
        layout.draw(canvas)
    }

    private fun drawDoubleShadowText(
        canvas: Canvas,
        text: String,
        x: Float,
        baselineY: Float,
        paint: TextPaint,
        dp: Float,
        shadowFactor: Float,
    ) {
        if (shadowFactor <= 0.01f) {
            paint.clearShadowLayer()
            canvas.drawText(text, x, baselineY, paint)
            return
        }
        val textAlpha = Color.alpha(paint.color)
        val ambientBlur = max(0.5f, 3f * dp * shadowFactor)
        paint.setShadowLayer(
            ambientBlur, 0f, 0f, scaledShadowColor(AMBIENT_SHADOW_BASE_ALPHA, shadowFactor, textAlpha)
        )
        canvas.drawText(text, x, baselineY, paint)

        val keyBlur = max(0.5f, 2f * dp * shadowFactor)
        val keyDx = 0.5f * dp * shadowFactor
        paint.setShadowLayer(
            keyBlur, keyDx, 0f, scaledShadowColor(KEY_SHADOW_BASE_ALPHA, shadowFactor, textAlpha)
        )
        canvas.drawText(text, x, baselineY, paint)
    }

    private fun scaledShadowColor(baseAlpha: Int, shadowFactor: Float, textAlpha: Int): Int {
        val alpha = (baseAlpha * shadowFactor * (textAlpha / 255f)).roundToInt().coerceIn(0, 255)
        return Color.argb(alpha, 0, 0, 0)
    }

    private sealed class RenderRow {
        abstract val height: Float

        class SingleText(val layout: StaticLayout, val paint: TextPaint) : RenderRow() {
            override val height: Float = layout.height.toFloat()
        }

        class PrefixAndText(
            val prefix: String,
            val prefixPaint: TextPaint,
            val mainText: String,
            val mainPaint: TextPaint,
            val gapPx: Float,
            val maxWidth: Int,
        ) : RenderRow() {
            override val height: Float = ceil(
                (mainPaint.fontMetrics.descent - mainPaint.fontMetrics.ascent).toDouble()
            ).toFloat()
        }

        class Subtitle(
            val layout: StaticLayout,
            val paint: TextPaint,
            val iconSizePx: Int,
            val iconGapPx: Float,
        ) : RenderRow() {
            override val height: Float = max(layout.height.toFloat(), iconSizePx.toFloat())
        }
    }
}
