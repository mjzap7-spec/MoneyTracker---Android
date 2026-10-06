package com.example.moneytracker.data.repository

import com.example.moneytracker.data.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TransactionRepository {

    private val auth =
        FirebaseAuth.getInstance()

    private val firestore =
        FirebaseFirestore.getInstance()

    private fun userTransactions() =
        auth.currentUser?.uid?.let { userId ->

            firestore
                .collection("users")
                .document(userId)
                .collection("transactions")

        } ?: throw IllegalStateException(
            "User is not logged in"
        )

    suspend fun addTransaction(
        transaction: Transaction
    ) {

        val document =
            userTransactions()
                .document()

        val transactionWithId =
            transaction.copy(
                id = document.id
            )

        document
            .set(transactionWithId)
            .await()
    }

    suspend fun getTransactions():
            List<Transaction> {

        return userTransactions()
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject(
                    Transaction::class.java
                )
            }
    }

    suspend fun getTransaction(
        transactionId: String
    ): Transaction {

        return userTransactions()
            .document(transactionId)
            .get()
            .await()
            .toObject(
                Transaction::class.java
            )
            ?: throw IllegalStateException(
                "Transaction not found"
            )
    }

    suspend fun updateTransaction(
        transaction: Transaction
    ) {

        userTransactions()
            .document(transaction.id)
            .set(transaction)
            .await()
    }

    suspend fun deleteTransaction(
        transactionId: String
    ) {

        userTransactions()
            .document(transactionId)
            .delete()
            .await()
    }
}