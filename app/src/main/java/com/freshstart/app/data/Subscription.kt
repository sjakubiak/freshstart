package com.freshstart.app.data

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.freshstart.app.BuildConfig
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * The FreshStart Premium subscription, sold through Google Play in the store build only.
 * The free trial and prices are set up in Play Console on the [PRODUCT_ID] subscription;
 * the app shows whatever base plans and offers Play says this user can buy.
 * The last known state is cached so the app still opens offline.
 */
class Subscription(context: Context) : PurchasesUpdatedListener {
    /** Sideloaded builds are free and never show the paywall. */
    val required: Boolean = BuildConfig.STORE_BUILD

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("subscription", Context.MODE_PRIVATE)
    private val scope = MainScope()

    /** A base plan the user can pick, e.g. monthly or yearly, with its trial if they still qualify. */
    data class Plan(
        val basePlanId: String,
        val price: String,
        val yearly: Boolean,
        val trialDays: Int,
        val offerToken: String,
        val details: ProductDetails
    )

    var active by mutableStateOf(prefs.getBoolean("active", false))
        private set

    /** True once Play has answered at least once since the app started. */
    var checked by mutableStateOf(false)
        private set

    var plans by mutableStateOf<List<Plan>>(emptyList())
        private set

    var problem by mutableStateOf<String?>(null)
        private set

    private val client: BillingClient? = if (required) {
        BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
    } else null

    /** Asks Play for the current subscription and prices. Safe to call often (e.g. on every resume). */
    fun refresh() {
        val client = client ?: return
        scope.launch {
            if (!connect(client)) {
                problem = "Couldn't reach Google Play. Check your connection and try again."
                return@launch
            }
            val purchases = queryPurchases(client)
            if (purchases == null) {
                problem = "Couldn't check your subscription with Google Play. Try again."
                return@launch
            }
            handle(purchases)
            checked = true
            if (!active) loadPlans(client)
        }
    }

    fun buy(activity: Activity, plan: Plan) {
        val client = client ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(plan.details)
                        .setOfferToken(plan.offerToken)
                        .build()
                )
            )
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingResponseCode.OK) problem = "Google Play couldn't start the purchase (${result.debugMessage})."
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> handle(purchases.orEmpty())
            BillingResponseCode.USER_CANCELED -> Unit
            BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            else -> problem = "The purchase didn't go through. Please try again."
        }
    }

    /** Google Play's page where the user can cancel or change their subscription. */
    fun manageUrl(): String =
        "https://play.google.com/store/account/subscriptions?sku=$PRODUCT_ID&package=${appContext.packageName}"

    private fun handle(purchases: List<Purchase>) {
        val owned = purchases.filter { PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
        // Play refunds purchases that aren't acknowledged within 3 days.
        owned.filter { !it.isAcknowledged }.forEach { purchase ->
            client?.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            ) { }
        }
        if (purchases.any { PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PENDING }) {
            problem = "Your payment is pending. FreshStart unlocks as soon as Google Play confirms it."
        }
        saveActive(owned.isNotEmpty())
        if (active) problem = null
    }

    private fun saveActive(value: Boolean) {
        active = value
        prefs.edit().putBoolean("active", value).apply()
    }

    private suspend fun loadPlans(client: BillingClient) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(ProductType.SUBS)
                        .build()
                )
            )
            .build()
        val details = suspendCancellableCoroutine<List<ProductDetails>?> { cont ->
            client.queryProductDetailsAsync(params) { result, found ->
                cont.resume(if (result.responseCode == BillingResponseCode.OK) found.productDetailsList else null)
            }
        }
        plans = details.orEmpty().flatMap { plansOf(it) }.sortedBy { it.yearly }
        problem = when {
            details == null -> "Couldn't load prices from Google Play. Try again."
            plans.isEmpty() -> "FreshStart Premium isn't available to buy yet. Try again later."
            else -> null
        }
    }

    private fun plansOf(details: ProductDetails): List<Plan> =
        details.subscriptionOfferDetails.orEmpty().groupBy { it.basePlanId }.mapNotNull { (basePlanId, offers) ->
            // Play only lists offers this user is eligible for, so a free-trial offer here means they get the trial.
            val offer = offers.firstOrNull { o -> o.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L } }
                ?: offers.firstOrNull { it.offerId == null }
                ?: return@mapNotNull null
            val phases = offer.pricingPhases.pricingPhaseList
            val regular = phases.lastOrNull() ?: return@mapNotNull null
            Plan(
                basePlanId = basePlanId,
                price = regular.formattedPrice,
                yearly = regular.billingPeriod.endsWith("Y"),
                trialDays = phases.firstOrNull { it.priceAmountMicros == 0L }?.let { days(it.billingPeriod) } ?: 0,
                offerToken = offer.offerToken,
                details = details
            )
        }

    private suspend fun connect(client: BillingClient): Boolean {
        if (client.isReady) return true
        return suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (cont.isActive) cont.resume(result.responseCode == BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() {
                    if (cont.isActive) cont.resume(false)
                }
            })
        }
    }

    private suspend fun queryPurchases(client: BillingClient): List<Purchase>? =
        suspendCancellableCoroutine { cont ->
            client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build()) { result, purchases ->
                cont.resume(if (result.responseCode == BillingResponseCode.OK) purchases else null)
            }
        }

    companion object {
        /** The subscription's product ID in Play Console. */
        const val PRODUCT_ID = "freshstart_premium"

        /** Days in an ISO-8601 period such as P14D, P2W or P1M. */
        fun days(period: String): Int {
            val m = Regex("P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)W)?(?:(\\d+)D)?").matchEntire(period) ?: return 0
            val (y, mo, w, d) = m.destructured
            return (y.toIntOrNull() ?: 0) * 365 + (mo.toIntOrNull() ?: 0) * 30 + (w.toIntOrNull() ?: 0) * 7 + (d.toIntOrNull() ?: 0)
        }
    }
}
