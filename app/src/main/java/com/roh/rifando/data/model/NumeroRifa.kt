package com.roh.rifando.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class StatusNumero {
    LIVRE, PAGO, PENDENTE
}
@Parcelize
data class NumeroRifa(
    val numero: Int,
    var nome: String? = null,
    var telefone: String? = null,
    var endereco: String? = null,
    var status: StatusNumero = StatusNumero.LIVRE,
    var nomeComprador: String? = null
):Parcelable

