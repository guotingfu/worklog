pluginManagement {
    repositories {
        // 1. 阿里云 Gradle 插件专用镜像 (针对插件下载优化)
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        // 2. 阿里云公共镜像
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 3. 腾讯云镜像 (作为阿里云的备用，互补性很强)
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // 4. 官方源兜底 (如果国内镜像都没有，最后尝试直连官方)
        gradlePluginPortal() // Gradle 插件官方门户，最全但国内慢
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 依赖包下载顺序同上
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // 常用第三方库
        maven { url = uri("https://jitpack.io") }

        // 官方源兜底
        google()
        mavenCentral()
    }
}

rootProject.name = "worklog"
include(":app")