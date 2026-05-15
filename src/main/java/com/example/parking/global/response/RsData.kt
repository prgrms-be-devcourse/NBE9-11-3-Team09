// RsData.kt
package com.example.parking.global.response

import com.fasterxml.jackson.annotation.JsonIgnore

data class RsData<T>(
        val msg: String,
        val resultCode: String,
        val data: T? = null
) {
  @JsonIgnore
  fun getStatusCode(): Int = resultCode.split("-")[0].toInt()
}