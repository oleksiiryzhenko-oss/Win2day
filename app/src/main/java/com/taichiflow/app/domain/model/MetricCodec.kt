package com.taichiflow.app.domain.model

/** History rows store their detail metrics as "Label=Value" pairs separated by '|'. */
object MetricCodec {
    fun encode(metrics: List<Metric>): String =
        metrics.joinToString("|") { "${clean(it.label)}=${clean(it.value)}" }

    fun decode(details: String): List<Metric> =
        details.split('|').mapNotNull { part ->
            val i = part.indexOf('=')
            if (i <= 0) null else Metric(part.substring(0, i), part.substring(i + 1))
        }

    private fun clean(s: String) = s.replace("|", "/").replace("=", ":")
}
