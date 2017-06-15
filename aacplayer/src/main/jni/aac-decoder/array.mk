LOCAL_PATH := $(call my-dir)

AAC_DECODER_FEATURES  := FAAD2
cflags_array_features := -DAAC_ARRAY_FEATURES='0x00$(foreach feature,$(AAC_DECODER_FEATURES), | com_spoledge_aacplayer_Decoder_DECODER_$(feature))' $(foreach feature,$(AAC_DECODER_FEATURES),-DAAC_ARRAY_FEATURE_$(feature))

include $(CLEAR_VARS)

LOCAL_MODULE 			:= aacarray
LOCAL_SRC_FILES 		:= aac-common.c aac-array-common.c aac-array-decoder.c aac-faad2-decoder.c
LOCAL_C_INCLUDES        := $(LOCAL_PATH)/../faad2/include
LOCAL_CFLAGS 			:= $(cflags_array_features) -DAACD_LOGLEVEL_ERROR
#LOCAL_CFLAGS 			:= -DAACD_LOGLEVEL_ERROR -DAAC_ARRAY_FEATURES='0x000' -DAAC_ARRAY_FEATURE_FAAD
LOCAL_LDLIBS 			:= -llog 
LOCAL_SHARED_LIBRARIES 	:= faad2
include $(BUILD_SHARED_LIBRARY)

include $(LOCAL_PATH)/../faad2/Android.mk

