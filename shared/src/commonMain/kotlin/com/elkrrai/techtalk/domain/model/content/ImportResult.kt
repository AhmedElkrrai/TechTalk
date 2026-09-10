package com.elkrrai.techtalk.domain.model.content

data class ImportResult(
    val topicsCreated: Int = 0,
    val topicsMatched: Int = 0,
    val tipsImported: Int = 0,
    val unknownTechnologies: List<String> = emptyList(),
    val errors: List<String> = emptyList()
) {
    val isSuccess: Boolean get() = errors.isEmpty() && unknownTechnologies.isEmpty()
}
