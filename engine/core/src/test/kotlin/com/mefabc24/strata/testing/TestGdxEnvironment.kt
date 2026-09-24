package com.mefabc24.strata.testing

import com.badlogic.gdx.Application
import com.badlogic.gdx.Audio
import com.badlogic.gdx.Files
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.GdxNativesLoader
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class TestInputState internal constructor() : InvocationHandler {
    var inputProcessor: InputProcessor? = null
        private set

    internal val input: Input = proxy(Input::class.java, this)

    override fun invoke(
        proxy: Any,
        method: Method,
        arguments: Array<out Any?>?
    ): Any? {
        return when (method.name) {
            "setInputProcessor" -> {
                inputProcessor = arguments?.firstOrNull() as InputProcessor?
                null
            }

            "getInputProcessor" -> inputProcessor
            else -> defaultValue(method.returnType)
        }
    }
}

object TestGdxEnvironment {
    fun install(): TestInputState {
        GdxNativesLoader.load()

        val inputState = TestInputState()

        Gdx.app = proxy(
            Application::class.java
        ) { _, method, arguments ->
            when (method.name) {
                "getType" -> Application.ApplicationType.HeadlessDesktop
                "getVersion" -> 0
                "postRunnable" -> {
                    (arguments?.firstOrNull() as Runnable).run()
                    null
                }

                else -> defaultValue(method.returnType)
            }
        }

        Gdx.files = TestFiles
        Gdx.input = inputState.input
        Gdx.audio = proxy(
            Audio::class.java
        ) { _, method, _ ->
            when (method.name) {
                "newSound" -> proxy(
                    Sound::class.java
                ) { _, soundMethod, _ ->
                    defaultValue(soundMethod.returnType)
                }

                else -> defaultValue(method.returnType)
            }
        }

        Gdx.graphics = proxy(
            Graphics::class.java
        ) { _, method, _ ->
            when (method.name) {
                "getWidth", "getBackBufferWidth" -> 800
                "getHeight", "getBackBufferHeight" -> 600
                "getDeltaTime" -> 1f / 60f
                "getDensity" -> 1f
                else -> defaultValue(method.returnType)
            }
        }

        val gl = proxy(
            GL20::class.java
        ) { _, method, _ ->
            defaultValue(method.returnType)
        }

        Gdx.gl = gl
        Gdx.gl20 = gl

        return inputState
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> proxy(
    type: Class<T>,
    handler: InvocationHandler
): T {
    return Proxy.newProxyInstance(
        type.classLoader,
        arrayOf(type),
        handler
    ) as T
}

fun defaultValue(type: Class<*>): Any? {
    return when (type) {
        java.lang.Boolean.TYPE -> false
        java.lang.Byte.TYPE -> 0.toByte()
        java.lang.Character.TYPE -> 0.toChar()
        java.lang.Short.TYPE -> 0.toShort()
        java.lang.Integer.TYPE -> 0
        java.lang.Long.TYPE -> 0L
        java.lang.Float.TYPE -> 0f
        java.lang.Double.TYPE -> 0.0
        else -> null
    }
}

private object TestFiles : Files {
    override fun getFileHandle(
        path: String,
        type: Files.FileType
    ) = TestFileHandle(path, type)

    override fun classpath(path: String) =
        getFileHandle(path, Files.FileType.Classpath)

    override fun internal(path: String) =
        getFileHandle(path, Files.FileType.Internal)

    override fun external(path: String) =
        getFileHandle(path, Files.FileType.External)

    override fun absolute(path: String) =
        getFileHandle(path, Files.FileType.Absolute)

    override fun local(path: String) =
        getFileHandle(path, Files.FileType.Local)

    override fun getExternalStoragePath() = ""

    override fun isExternalStorageAvailable() = false

    override fun getLocalStoragePath() = ""

    override fun isLocalStorageAvailable() = false
}

private class TestFileHandle(
    path: String,
    type: Files.FileType
) : FileHandle(path, type)
