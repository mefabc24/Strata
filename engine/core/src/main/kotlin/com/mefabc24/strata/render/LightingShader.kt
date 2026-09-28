package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Vector2
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.lighting.Lighting

/** Uploads scene lighting to the world SpriteBatch shader. */
internal class LightingShader(
    private val projection: IsoProjection,
    private val lighting: Lighting
) {

    val program = ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER).also {
        if (!it.isCompiled) {
            val log = it.log
            it.dispose()
            error("Failed to compile the Strata lighting shader:\n$log")
        }
    }

    private val projectedPosition = Vector2()
    private val pointLightData = FloatArray(Lighting.MAX_POINT_LIGHTS * 4)
    private val pointLightColors = FloatArray(Lighting.MAX_POINT_LIGHTS * 3)

    fun apply() {
        val ambient = lighting.shaderAmbientColor
        program.setUniformf(
            AMBIENT_COLOR_UNIFORM,
            ambient.r,
            ambient.g,
            ambient.b
        )
        program.setUniformf(
            AMBIENT_INTENSITY_UNIFORM,
            lighting.ambientIntensity
        )

        var activeCount = 0
        for (light in lighting.pointLights) {
            if (!light.enabled) continue

            val position = light.position
            projection.tileToWorld(
                position.x,
                position.y,
                projectedPosition
            )

            val dataOffset = activeCount * 4
            pointLightData[dataOffset] = projectedPosition.x
            pointLightData[dataOffset + 1] = projectedPosition.y
            pointLightData[dataOffset + 2] =
                light.radius * projection.tileStepLength
            pointLightData[dataOffset + 3] = light.intensity

            val color = light.shaderColor
            val colorOffset = activeCount * 3
            pointLightColors[colorOffset] = color.r
            pointLightColors[colorOffset + 1] = color.g
            pointLightColors[colorOffset + 2] = color.b

            activeCount++
        }

        program.setUniformi(POINT_LIGHT_COUNT_UNIFORM, activeCount)

        if (activeCount > 0) {
            program.setUniform4fv(
                POINT_LIGHT_DATA_UNIFORM,
                pointLightData,
                0,
                activeCount * 4
            )
            program.setUniform3fv(
                POINT_LIGHT_COLORS_UNIFORM,
                pointLightColors,
                0,
                activeCount * 3
            )
        }
    }

    fun dispose() {
        program.dispose()
    }

    private companion object {
        const val AMBIENT_COLOR_UNIFORM = "u_ambientColor"
        const val AMBIENT_INTENSITY_UNIFORM = "u_ambientIntensity"
        const val POINT_LIGHT_COUNT_UNIFORM = "u_pointLightCount"
        const val POINT_LIGHT_DATA_UNIFORM = "u_pointLightData"
        const val POINT_LIGHT_COLORS_UNIFORM = "u_pointLightColors"

        val VERTEX_SHADER = """
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;

            uniform mat4 u_projTrans;

            varying vec4 v_color;
            varying vec2 v_texCoords;
            varying vec2 v_worldPosition;

            void main() {
                v_color = a_color;
                v_color.a = v_color.a * (255.0 / 254.0);
                v_texCoords = a_texCoord0;
                v_worldPosition = a_position.xy;
                gl_Position = u_projTrans * a_position;
            }
        """.trimIndent()

        val FRAGMENT_SHADER = """
            #ifdef GL_ES
            precision mediump float;
            #endif

            varying vec4 v_color;
            varying vec2 v_texCoords;
            varying vec2 v_worldPosition;

            uniform sampler2D u_texture;
            uniform vec3 u_ambientColor;
            uniform float u_ambientIntensity;
            uniform int u_pointLightCount;
            uniform vec4 u_pointLightData[${Lighting.MAX_POINT_LIGHTS}];
            uniform vec3 u_pointLightColors[${Lighting.MAX_POINT_LIGHTS}];

            void main() {
                vec4 spriteColor = v_color * texture2D(u_texture, v_texCoords);
                vec3 lightColor = u_ambientColor * u_ambientIntensity;

                for (int index = 0; index < ${Lighting.MAX_POINT_LIGHTS}; index++) {
                    if (index >= u_pointLightCount) {
                        break;
                    }

                    vec4 light = u_pointLightData[index];
                    float attenuation = 1.0 - clamp(
                        distance(v_worldPosition, light.xy) / light.z,
                        0.0,
                        1.0
                    );
                    lightColor +=
                        u_pointLightColors[index] * light.w * attenuation;
                }

                lightColor = clamp(lightColor, 0.0, 1.0);
                gl_FragColor = vec4(
                    spriteColor.rgb * lightColor,
                    spriteColor.a
                );
            }
        """.trimIndent()
    }
}
