LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE 			:= aacfaad2
LOCAL_SRC_FILES 		:= aac-faad2-decoder.c
LOCAL_C_INCLUDES 		:= $(LOCAL_PATH)/../faad2/include
LOCAL_CFLAGS 			:= -DAACD_LOGLEVEL_ERROR 
LOCAL_LDLIBS 			:= -llog 
#LOCAL_STATIC_LIBRARIES  := faad2
LOCAL_SHARED_LIBRARIES  := faad2 aacarray
#include $(BUILD_STATIC_LIBRARY)
include $(BUILD_SHARED_LIBRARY)

include $(LOCAL_PATH)/../faad2/Android.mk

