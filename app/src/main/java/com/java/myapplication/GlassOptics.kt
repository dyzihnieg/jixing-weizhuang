package com.java.myapplication

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
internal fun rememberDockGlassEffect(
    size: IntSize,
    cornerRadius: Dp,
    liquid: Boolean = true,
): RenderEffect? {
    val density = LocalDensity.current
    val refraction = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try { DockGlassEffectApi33.refraction() } catch (_: RuntimeException) { null }
        } else null
    }
    return remember(size, density.density, cornerRadius, liquid) {
        if (size.width <= 0 || size.height <= 0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return@remember null
        }

        // Liquid surfaces keep the captured page legible. The refraction edge
        // supplies the lens depth; a large blur only produces the old frosted look.
        val fallbackRadius = with(density) { (if (liquid) .7.dp else 8.dp).toPx() }
        val fallback = BlurEffect(fallbackRadius, fallbackRadius, TileMode.Clamp)
        if (refraction == null) return@remember fallback

        val halfExtent = minOf(size.width, size.height) * 0.5f
        val radius = with(density) { cornerRadius.toPx() }
            .takeIf { it.isFinite() }?.coerceIn(0f, halfExtent) ?: 0f
        try {
            refraction.create(
                size = size,
                cornerRadius = radius,
                blurRadius = with(density) { (if (liquid) .7.dp else 8.dp).toPx() },
                edgeWidth = with(density) { (if (liquid) 5.dp else 8.dp).toPx() }
                    .coerceAtMost(halfExtent),
                edgeAmount = with(density) { (if (liquid) 4.dp else 3.dp).toPx() },
            )
        } catch (_: RuntimeException) {
            fallback
        }
    }
}

internal interface DockReflection {
    val brush: Brush
    fun update(size: Size, touchPosition: Offset, press: Float, drag: Float, dark: Boolean)
}

@Composable
internal fun rememberDockReflection(cornerRadius: Dp): DockReflection? {
    val density = LocalDensity.current
    return remember(density.density, cornerRadius) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return@remember null
        }
        try {
            DockGlassEffectApi33.reflection(
                with(density) { cornerRadius.toPx() },
                density.density,
            )
        } catch (_: RuntimeException) {
            null
        }
    }
}

private interface DockRefraction {
    fun create(size: IntSize, cornerRadius: Float, blurRadius: Float, edgeWidth: Float, edgeAmount: Float): RenderEffect
}

// Keep Android 13 shader classes out of the paths used by older Android versions.
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object DockGlassEffectApi33 {
    fun reflection(cornerRadius: Float, density: Float): DockReflection {
        val shader = android.graphics.RuntimeShader(ReflectionShader)
        shader.setFloatUniform("pixelScale", density)
        return object : DockReflection {
            override val brush = ShaderBrush(shader)

            override fun update(size: Size, touchPosition: Offset, press: Float, drag: Float, dark: Boolean) {
                shader.setFloatUniform("surfaceSize", size.width, size.height)
                shader.setFloatUniform("cornerRadius", cornerRadius.coerceIn(0f, minOf(size.width, size.height) * .5f))
                shader.setFloatUniform("touchPosition", touchPosition.x, touchPosition.y)
                shader.setFloatUniform("pressAmount", press.coerceIn(0f, 1f))
                shader.setFloatUniform("dragAmount", drag.coerceIn(0f, 1f))
                shader.setFloatUniform("darkAmount", if (dark) 1f else 0f)
            }
        }
    }

    fun refraction(): DockRefraction {
        val shader = android.graphics.RuntimeShader(EdgeRefractionShader)
        return object : DockRefraction {
            override fun create(size: IntSize, cornerRadius: Float, blurRadius: Float, edgeWidth: Float, edgeAmount: Float): RenderEffect =
                createEffect(shader, size, cornerRadius, blurRadius, edgeWidth, edgeAmount)
        }
    }

    private fun createEffect(
        shader: android.graphics.RuntimeShader,
        size: IntSize,
        cornerRadius: Float,
        blurRadius: Float,
        edgeWidth: Float,
        edgeAmount: Float,
    ): RenderEffect {
        shader.setFloatUniform("surfaceSize", size.width.toFloat(), size.height.toFloat())
        shader.setFloatUniform("cornerRadius", cornerRadius)
        shader.setFloatUniform("edgeWidth", edgeWidth)
        shader.setFloatUniform("edgeAmount", edgeAmount)
        val refraction = android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content")
        val blur = android.graphics.RenderEffect.createBlurEffect(
            blurRadius,
            blurRadius,
            android.graphics.Shader.TileMode.CLAMP,
        )
        // Chain order is outer(inner(content)): refraction samples the blurred page.
        return android.graphics.RenderEffect.createChainEffect(refraction, blur).asComposeRenderEffect()
    }

    // Coordinates and uniforms are local physical pixels. Only the inner edge band
    // shifts the captured page; the middle stays still, and text is drawn separately.
    private val EdgeRefractionShader = """
        uniform shader content;
        uniform float2 surfaceSize;
        uniform float cornerRadius;
        uniform float edgeWidth;
        uniform float edgeAmount;

        float surfaceDistance(float2 position) {
            float2 halfSize = surfaceSize * 0.5;
            float2 edge = abs(position - halfSize) - halfSize + cornerRadius;
            return length(max(edge, float2(0.0)))
                + min(max(edge.x, edge.y), 0.0) - cornerRadius;
        }

        half4 main(float2 position) {
            float distance = surfaceDistance(position);
            float weight = 1.0 - smoothstep(0.0, max(edgeWidth, 0.5), max(-distance, 0.0));
            if (weight <= 0.0) return content.eval(position);

            float2 gradient = float2(
                surfaceDistance(position + float2(0.5, 0.0))
                    - surfaceDistance(position - float2(0.5, 0.0)),
                surfaceDistance(position + float2(0.0, 0.5))
                    - surfaceDistance(position - float2(0.0, 0.5))
            );
            float2 normal = gradient / max(length(gradient), 0.001);
            float2 displacement = normal * edgeAmount * weight * weight;
            float2 samplePosition = position + displacement;
            samplePosition = clamp(samplePosition, float2(0.5), surfaceSize - float2(0.5));
            half4 sampled = content.eval(samplePosition);
            float2 dispersion = normal * edgeAmount * 0.12 * weight * weight;
            half red = content.eval(clamp(samplePosition + dispersion, float2(0.5), surfaceSize - float2(0.5))).r;
            half blue = content.eval(clamp(samplePosition - dispersion, float2(0.5), surfaceSize - float2(0.5))).b;
            return half4(red, sampled.g, blue, sampled.a);
        }
    """.trimIndent()

    // Reflected light is rendered after the refracted page and before the icons.
    // Uniforms move on input; the shader and brush are cached for this geometry.
    private val ReflectionShader = """
        uniform float2 surfaceSize;
        uniform float cornerRadius;
        uniform float pixelScale;
        uniform float2 touchPosition;
        uniform float pressAmount;
        uniform float dragAmount;
        uniform float darkAmount;

        float surfaceDistance(float2 position) {
            float2 halfSize = surfaceSize * 0.5;
            float2 edge = abs(position - halfSize) - halfSize + cornerRadius;
            return length(max(edge, float2(0.0)))
                + min(max(edge.x, edge.y), 0.0) - cornerRadius;
        }

        half4 main(float2 position) {
            float distance = surfaceDistance(position);
            float mask = 1.0 - smoothstep(-0.7 * pixelScale, 0.7 * pixelScale, distance);
            if (mask <= 0.0) return half4(0.0);
            float2 gradient = float2(
                surfaceDistance(position + float2(0.5, 0.0))
                    - surfaceDistance(position - float2(0.5, 0.0)),
                surfaceDistance(position + float2(0.0, 0.5))
                    - surfaceDistance(position - float2(0.0, 0.5))
            );
            float2 normal = gradient / max(length(gradient), 0.001);
            float2 lightVector = touchPosition - surfaceSize * 0.5
                + float2(-surfaceSize.y * 0.18, -surfaceSize.y * 0.75);
            float2 light = lightVector / max(length(lightVector), 0.001);
            float facing = max(dot(normal, light), 0.0);
            float rim = exp(-pow((distance + 1.0 * pixelScale) / (1.15 * pixelScale), 2.0));
            float movingRim = pow(facing, 5.0) * (0.32 + 0.52 * pressAmount);
            float oppositeRim = pow(max(-dot(normal, light), 0.0), 9.0) * 0.20;
            float sweepCoordinate = position.x - touchPosition.x
                - (position.y - touchPosition.y) * (0.4 + 0.3 * dragAmount);
            float sweep = exp(-pow(sweepCoordinate / max(surfaceSize.y * 0.25, 1.0), 2.0));
            float crown = exp(-pow((position.y - surfaceSize.y * 0.27)
                / max(surfaceSize.y * 0.36, 1.0), 2.0));
            float reflected = rim * (0.08 + movingRim + oppositeRim)
                + sweep * crown * (0.015 + 0.14 * pressAmount + 0.055 * dragAmount);
            float alpha = min(reflected * mask * (1.0 - darkAmount * 0.15), 0.9);
            return half4(half3(alpha), half(alpha));
        }
    """.trimIndent()
}
