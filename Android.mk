LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := DU-Themes
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
LOCAL_PRIVATE_PLATFORM_APIS := true
LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_USE_AAPT2 := true

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages androidx.appcompat_appcompat \
    --extra-packages androidx.cardview_cardview \
    --extra-packages androidx.preference_preference \
    --extra-packages androidx.recyclerview_recyclerview

LOCAL_STATIC_JAVA_LIBRARIES := \
    androidx.appcompat_appcompat \
    androidx.cardview_cardview \
    androidx-constraintlayout_constraintlayout \
    androidx.core_core \
    androidx.preference_preference \
    androidx.recyclerview_recyclerview

include frameworks/base/packages/SettingsLib/common.mk

include $(BUILD_PACKAGE)
