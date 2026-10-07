package com.java.myapplication

import android.app.Application
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class MaskApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ModernService.start()
    }
}

object ModernService {
    @Volatile var service: XposedService? = null
        private set
    fun start() {
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(value: XposedService) { service = value }
            override fun onServiceDied(value: XposedService) { if (service === value) service = null }
        })
    }
    fun requireService(): XposedService {
        val value = service ?: error("LSPosed 服务未连接，请在管理器启用模块后重启控制 APK")
        check(value.apiVersion >= 102) { "需要 libxposed API 102" }
        return value
    }
}