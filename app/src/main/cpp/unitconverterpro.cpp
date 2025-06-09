#include <jni.h>
#include <string>
#include <cmath>
#include <android/log.h>

#define LOG_TAG "UnitConverterCPP"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Helper function to convert jstring to std::string
std::string jstring2string(JNIEnv *env, jstring jStr) {
    if (!jStr) return "";

    const jclass stringClass = env->GetObjectClass(jStr);
    const jmethodID getBytes = env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
    const jbyteArray stringJbytes = (jbyteArray) env->CallObjectMethod(jStr, getBytes, env->NewStringUTF("UTF-8"));

    size_t length = (size_t) env->GetArrayLength(stringJbytes);
    jbyte* pBytes = env->GetByteArrayElements(stringJbytes, NULL);

    std::string ret = std::string((char*)pBytes, length);
    env->ReleaseByteArrayElements(stringJbytes, pBytes, JNI_ABORT);

    env->DeleteLocalRef(stringJbytes);
    env->DeleteLocalRef(stringClass);
    return ret;
}

// Length conversion function
double convertLengthInternal(double value, const std::string& fromUnit, const std::string& toUnit) {
    LOGD("Converting length: %f from %s to %s", value, fromUnit.c_str(), toUnit.c_str());

    // Convert everything to meters first, then to target unit
    double meters = 0.0;

    // Convert from source unit to meters
    if (fromUnit == "Meter") {
        meters = value;
    } else if (fromUnit == "Kilometer") {
        meters = value * 1000.0;
    } else if (fromUnit == "Inch") {
        meters = value * 0.0254;
    } else if (fromUnit == "Mile") {
        meters = value * 1609.34;
    }

    // Convert from meters to target unit
    double result = 0.0;
    if (toUnit == "Meter") {
        result = meters;
    } else if (toUnit == "Kilometer") {
        result = meters / 1000.0;
    } else if (toUnit == "Inch") {
        result = meters / 0.0254;
    } else if (toUnit == "Mile") {
        result = meters / 1609.34;
    }

    LOGD("Length conversion result: %f", result);
    return result;
}

// Temperature conversion function
double convertTemperatureInternal(double value, const std::string& fromUnit, const std::string& toUnit) {
    LOGD("Converting temperature: %f from %s to %s", value, fromUnit.c_str(), toUnit.c_str());

    double celsius = 0.0;

    // Convert from source unit to Celsius
    if (fromUnit == "Celsius") {
        celsius = value;
    } else if (fromUnit == "Fahrenheit") {
        celsius = (value - 32.0) * 5.0 / 9.0;
    } else if (fromUnit == "Kelvin") {
        celsius = value - 273.15;
    }

    // Convert from Celsius to target unit
    double result = 0.0;
    if (toUnit == "Celsius") {
        result = celsius;
    } else if (toUnit == "Fahrenheit") {
        result = celsius * 9.0 / 5.0 + 32.0;
    } else if (toUnit == "Kelvin") {
        result = celsius + 273.15;
    }

    LOGD("Temperature conversion result: %f", result);
    return result;
}

// Weight conversion function
double convertWeightInternal(double value, const std::string& fromUnit, const std::string& toUnit) {
    LOGD("Converting weight: %f from %s to %s", value, fromUnit.c_str(), toUnit.c_str());

    // Convert everything to grams first, then to target unit
    double grams = 0.0;

    // Convert from source unit to grams
    if (fromUnit == "Gram") {
        grams = value;
    } else if (fromUnit == "Kilogram") {
        grams = value * 1000.0;
    } else if (fromUnit == "Pound") {
        grams = value * 453.592;
    }

    // Convert from grams to target unit
    double result = 0.0;
    if (toUnit == "Gram") {
        result = grams;
    } else if (toUnit == "Kilogram") {
        result = grams / 1000.0;
    } else if (toUnit == "Pound") {
        result = grams / 453.592;
    }

    LOGD("Weight conversion result: %f", result);
    return result;
}

// JNI function implementations
extern "C" {

JNIEXPORT jdouble JNICALL
Java_com_example_unitconverterpro_MainActivity_convertLength(JNIEnv *env, jobject thiz,
                                                             jdouble value, jstring fromUnit, jstring toUnit) {
    LOGD("JNI convertLength called");
    std::string from = jstring2string(env, fromUnit);
    std::string to = jstring2string(env, toUnit);

    return convertLengthInternal(value, from, to);
}

JNIEXPORT jdouble JNICALL
Java_com_example_unitconverterpro_MainActivity_convertTemperature(JNIEnv *env, jobject thiz,
                                                                  jdouble value, jstring fromUnit, jstring toUnit) {
    LOGD("JNI convertTemperature called");
    std::string from = jstring2string(env, fromUnit);
    std::string to = jstring2string(env, toUnit);

    return convertTemperatureInternal(value, from, to);
}

JNIEXPORT jdouble JNICALL
Java_com_example_unitconverterpro_MainActivity_convertWeight(JNIEnv *env, jobject thiz,
                                                             jdouble value, jstring fromUnit, jstring toUnit) {
    LOGD("JNI convertWeight called");
    std::string from = jstring2string(env, fromUnit);
    std::string to = jstring2string(env, toUnit);

    return convertWeightInternal(value, from, to);
}
}