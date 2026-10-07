package com.java.myapplication.ui.theme

enum class ThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromStored(value: String?): ThemeMode =
            entries.firstOrNull { it.storageKey == value } ?: SYSTEM
    }
}

enum class AccentColor(val storageKey: String, val displayName: String) {
    TEAL("teal", "青绿"),
    BLUE("blue", "蓝色"),
    ROSE("rose", "粉色"),
    AMBER("amber", "黄色"),
    RED("red", "红色"),
    BLACK("black", "黑色"),
    PURPLE("purple", "紫色");

    companion object {
        fun fromStored(value: String?, fallback: AccentColor = TEAL): AccentColor =
            entries.firstOrNull { it.storageKey == value } ?: fallback
    }
}

enum class ColorStyle(val storageKey: String) {
    SOLID("solid"),
    SPLIT("split");

    companion object {
        fun fromStored(value: String?): ColorStyle =
            entries.firstOrNull { it.storageKey == value } ?: SOLID
    }
}

enum class AnimationSpeed(
    val storageKey: String,
    val displayName: String,
    val durationMillis: Int,
) {
    SLOW("slow", "慢速", 520),
    STANDARD("standard", "标准", 320),
    FAST("fast", "快速", 180);

    companion object {
        fun fromStored(value: String?): AnimationSpeed =
            entries.firstOrNull { it.storageKey == value } ?: STANDARD
    }
}

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.TEAL,
    val colorStyle: ColorStyle = ColorStyle.SOLID,
    val secondaryAccent: AccentColor = AccentColor.BLUE,
    val animationSpeed: AnimationSpeed = AnimationSpeed.STANDARD,
)
