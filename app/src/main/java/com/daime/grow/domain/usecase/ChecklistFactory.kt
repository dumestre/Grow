package com.daime.grow.domain.usecase

import com.daime.grow.domain.model.ChecklistItem
import com.daime.grow.domain.model.PlantStage

object ChecklistFactory {
    fun defaultChecklist(plantId: Long, phase: String, now: Long): List<ChecklistItem> {
        val tasks = when (phase) {
            PlantStage.SEEDLING -> listOf(
                "Ajustar umidade para mudas",
                "Monitorar temperatura diária",
                "Checar luz 18h"
            )

            PlantStage.VEGETATIVE -> listOf(
                "Treinamento leve da copa",
                "Inspecionar pragas",
                "Reforçar ventilação"
            )

            PlantStage.FLOWER -> listOf(
                "Revisar suporte dos galhos",
                "Ajustar fotoperíodo 12/12",
                "Planejar janela de flush"
            )

            else -> emptyList()
        }

        return tasks.map {
            ChecklistItem(
                plantId = plantId,
                phase = phase,
                task = it,
                done = false,
                createdAt = now
            )
        }
    }
}

