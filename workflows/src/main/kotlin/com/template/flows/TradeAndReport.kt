package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.HighlyRegulatedContract
import com.template.states.HighlyRegulatedState
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class TradeAndReport(
        val buyer: Party,
        val stateRegulator: Party,
        val nationalRegulator: Party
) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        val transactionBuilder = TransactionBuilder(notary).addOutputState(HighlyRegulatedState(buyer, ourIdentity), HighlyRegulatedContract.ID)
                .addCommand(HighlyRegulatedContract.Commands.Trade(), ourIdentity.owningKey)

        val signedTransaction = serviceHub.signInitialTransaction((transactionBuilder))
        val sessions = listOf(initiateFlow(buyer), initiateFlow(stateRegulator))

        // We distribute the transaction to both the buyer and the state regulator using 'Finality Flow'
        //subFlow(FinalityFlow(signedTransaction, sessions))
        // We also distribute the transaction to the national regulator manually
        subFlow(ReportManually(signedTransaction, nationalRegulator))
        return subFlow(FinalityFlow(signedTransaction, sessions))
    }


    @InitiatedBy(TradeAndReport::class)
    class TradeAndReportResponder(val counterPartySession: FlowSession) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            //Both the buyer and the state regulator record all of the transaction's states using
            // 'ReceiveFinalityFlow' with the 'ALL_VISIBLE' flag.
            return subFlow((ReceiveFinalityFlow(counterPartySession, statesToRecord = StatesToRecord.ALL_VISIBLE)))
        }


    }
}