package com.ayacucho.medicitas.view.tutorial

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.ayacucho.medicitas.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * TutorialOverlayView — Vista que maneja el tutorial interactivo estilo spotlight.
 *
 * Dibuja un overlay oscuro con un "agujero" (cutout) sobre el elemento objetivo
 * y posiciona dinámicamente el tooltip encima o debajo del campo resaltado.
 */
class TutorialOverlayView(context: Context) : FrameLayout(context) {

    // ─── Callbacks ────────────────────────────────────────────────────────────
    var onNext: (() -> Unit)? = null
    var onSkip: (() -> Unit)? = null

    // ─── Paint para el overlay ────────────────────────────────────────────────
    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#CC000000") // 80% negro
        isAntiAlias = true
    }
    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        isAntiAlias = true
    }

    // ─── Estado del spotlight ─────────────────────────────────────────────────
    private val spotlightRect = RectF()
    private var spotlightRadius = 18f
    private var animatedAlpha = 0f
    private var spotlightAnimator: ValueAnimator? = null

    // ─── Tooltip view ─────────────────────────────────────────────────────────
    private val tooltipView: View
    private val tvTooltipTitulo: TextView
    private val tvTooltipDesc: TextView
    private val tvTooltipPaso: TextView
    private val ivTooltipIcon: ImageView
    private val btnSiguiente: MaterialButton
    private val tvOmitir: TextView
    private val dotsContainer: ViewGroup

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setWillNotDraw(false)

        // Inflar el tooltip
        tooltipView = LayoutInflater.from(context).inflate(R.layout.tooltip_tutorial, this, false)
        tvTooltipTitulo = tooltipView.findViewById(R.id.tvTooltipTitulo)
        tvTooltipDesc = tooltipView.findViewById(R.id.tvTooltipDesc)
        tvTooltipPaso = tooltipView.findViewById(R.id.tvTooltipPaso)
        ivTooltipIcon = tooltipView.findViewById(R.id.ivTooltipIcon)
        btnSiguiente = tooltipView.findViewById(R.id.btnTooltipSiguiente)
        tvOmitir = tooltipView.findViewById(R.id.tvTooltipOmitir)
        dotsContainer = tooltipView.findViewById(R.id.dotsContainer)

        addView(tooltipView)

        btnSiguiente.setOnClickListener { onNext?.invoke() }
        tvOmitir.setOnClickListener { onSkip?.invoke() }

        // Interceptar taps en el overlay (no dejar pasar a las vistas de fondo)
        setOnClickListener { /* consumir */ }
    }

    // ─── API pública ──────────────────────────────────────────────────────────

    fun mostrarPaso(
        targetView: View,
        titulo: String,
        descripcion: String,
        paso: Int,
        total: Int,
        iconRes: Int,
        esUltimo: Boolean
    ) {
        // Obtener posición del target en coordenadas de pantalla
        val loc = IntArray(2)
        targetView.getLocationOnScreen(loc)

        val overlayLoc = IntArray(2)
        getLocationOnScreen(overlayLoc)

        val relX = loc[0] - overlayLoc[0]
        val relY = loc[1] - overlayLoc[1]

        val padding = 20f
        spotlightRect.set(
            relX - padding,
            relY - padding,
            relX + targetView.width + padding,
            relY + targetView.height + padding
        )

        // Actualizar contenido del tooltip
        tvTooltipTitulo.text = titulo
        tvTooltipDesc.text = descripcion
        tvTooltipPaso.text = "${paso + 1} / $total"
        ivTooltipIcon.setImageResource(iconRes)
        btnSiguiente.text = if (esUltimo) "¡Entendido! ✓" else "Siguiente →"

        // Actualizar dots
        actualizarDots(paso, total)

        // Posicionar tooltip
        posicionarTooltip(relY.toFloat(), targetView.height)

        // Animar entrada
        animarEntrada()
    }

    private fun posicionarTooltip(targetRelY: Float, targetHeight: Int) {
        tooltipView.measure(
            MeasureSpec.makeMeasureSpec(width - 64, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        val tooltipH = tooltipView.measuredHeight
        val margen = 24f
        val padding = 20f

        val espacioAbajo = height - (targetRelY + targetHeight + padding)
        val espacioArriba = targetRelY - padding

        val tooltipY = if (espacioAbajo >= tooltipH + margen) {
            // Hay espacio abajo
            targetRelY + targetHeight + padding + margen
        } else if (espacioArriba >= tooltipH + margen) {
            // Hay espacio arriba
            targetRelY - padding - tooltipH - margen
        } else {
            // Sin espacio: centrar verticalmente en el área libre
            (height / 2f) - (tooltipH / 2f)
        }

        tooltipView.translationX = 32f
        tooltipView.translationY = tooltipY
        tooltipView.layoutParams = LayoutParams(width - 64, LayoutParams.WRAP_CONTENT)
    }

    private fun actualizarDots(pasoActual: Int, total: Int) {
        dotsContainer.removeAllViews()
        val sizePx = resources.getDimensionPixelSize(R.dimen.tutorial_dot_size)
        val marginPx = resources.getDimensionPixelSize(R.dimen.tutorial_dot_margin)

        for (i in 0 until total) {
            val dot = View(context)
            val params = LayoutParams(sizePx, sizePx).apply {
                leftMargin = marginPx
                rightMargin = marginPx
            }
            dot.layoutParams = params
            dot.background = if (i == pasoActual) {
                ContextCompat.getDrawable(context, R.drawable.bg_dot_active)
            } else {
                ContextCompat.getDrawable(context, R.drawable.bg_dot_inactive)
            }
            dotsContainer.addView(dot)
        }
    }

    private fun animarEntrada() {
        // Fade in del overlay
        animatedAlpha = 0f
        val alphaAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animatedAlpha = it.animatedValue as Float
                invalidate()
            }
        }

        // Slide + fade in del tooltip
        tooltipView.alpha = 0f
        tooltipView.translationX = tooltipView.translationX - 30f
        tooltipView.animate()
            .alpha(1f)
            .translationXBy(30f)
            .setDuration(350)
            .setStartDelay(150)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        alphaAnim.start()
    }

    fun ocultarConAnimacion(onEnd: () -> Unit) {
        animate()
            .alpha(0f)
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd()
                }
            })
            .start()
    }

    // ─── Dibujo del overlay con spotlight ────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        // Crear bitmap temporal para el efecto CLEAR
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(bmp)

        // Dibujar fondo oscuro completo
        overlayPaint.alpha = (255 * animatedAlpha).toInt()
        tempCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // "Borrar" el área del spotlight (cutout redondeado)
        if (!spotlightRect.isEmpty) {
            tempCanvas.drawRoundRect(spotlightRect, spotlightRadius, spotlightRadius, clearPaint)
        }

        canvas.drawBitmap(bmp, 0f, 0f, null)
        bmp.recycle()

        super.onDraw(canvas)
    }
}
