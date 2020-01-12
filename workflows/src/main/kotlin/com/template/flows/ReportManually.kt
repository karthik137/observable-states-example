package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

@InitiatingFlow
class ReportManually(
        val signedTransaction: SignedTransaction,
        val regulator: Party
) : FlowLogic<Unit>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        val session = initiateFlow(regulator)
        session.send(signedTransaction)
    }
}

@InitiatedBy(ReportManually::class)
class ReportManuallyResponder(val counterParySession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val signedTransaction = counterParySession.receive<SignedTransaction>().unwrap{it}

        // The national regulator recorda all of the transaction's states using
        // 'recordTransactions ' with the 'ALL_VISIBLE' flag.

        serviceHub.recordTransactions(StatesToRecord.ALL_VISIBLE, listOf(signedTransaction))
    }


}