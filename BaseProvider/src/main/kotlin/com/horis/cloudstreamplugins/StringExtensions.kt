package com.horis.cloudstreamplugins

fun String.substring(left: String, right: String): String {
    return substringAfter(left).substringBefore(right)
}
