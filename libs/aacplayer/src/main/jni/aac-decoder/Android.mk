LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

needs_mks = $(LOCAL_PATH)/array.mk
#needs_mks += $(LOCAL_PATH)/faad2.mk
#needs_mks += $(LOCAL_PATH)/array.mk

# Build components:
include $(needs_mks)

