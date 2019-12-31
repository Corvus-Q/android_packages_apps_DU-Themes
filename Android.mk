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
    --extra-packages androidx.appcompat \
    --extra-packages androidx.preference_preference

LOCAL_STATIC_JAVA_LIBRARIES := \
    androidx.appcompat_appcompat \
    androidx-constraintlayout_constraintlayout \
    androidx.preference_preference

include $(BUILD_PACKAGE)
