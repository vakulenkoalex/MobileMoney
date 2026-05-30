package com.mobilemoney.domain.helper

object TransferMerger {

    data class TransactionInfo(
        val relatedTransactionId: String?,
        val isIncome: Boolean,
        val subtitle: String,
        val amount: Double
    )

    data class MergedTransfer(
        val title: String,
        val subtitle: String,
        val amount: Double,
        val relatedTransactionId: String
    )

    fun identifyMergedTransfers(transactions: List<TransactionInfo>): List<MergedTransfer> {
        val transferTransactions = transactions.filter { it.relatedTransactionId != null }
        val result = mutableListOf<MergedTransfer>()

        val grouped = transferTransactions.groupBy { it.relatedTransactionId }
        for ((relatedId, group) in grouped) {
            if (group.size >= 2) {
                val tx1 = group[0]
                val tx2 = group[1]
                result.add(
                    MergedTransfer(
                        title = if (tx1.isIncome) tx1.subtitle else tx2.subtitle,
                        subtitle = if (!tx1.isIncome) tx1.subtitle else tx2.subtitle,
                        amount = tx1.amount,
                        relatedTransactionId = relatedId ?: ""
                    )
                )
            }
        }
        return result
    }
}
