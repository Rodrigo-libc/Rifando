package com.roh.rifando.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class Rifa(
    val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val qtdNumeros: Int,
    val valorNumero: Double,
    val dataSorteio: String? = null,
    var numeros: MutableList<NumeroRifa> = mutableListOf()
) : Parcelable

