package com.ws.skelton.remind.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {
    // Ad Unit IDs
    // Ad unit IDs - Now using official Google Test IDs
    const val BANNER_ID = com.ws.skelton.remind.BuildConfig.ADMOB_BANNER_ID
    const val INTERSTITIAL_ID = com.ws.skelton.remind.BuildConfig.ADMOB_INTERSTITIAL_ID
    const val NATIVE_ID = com.ws.skelton.remind.BuildConfig.ADMOB_NATIVE_ID
    const val OPENING_ID = com.ws.skelton.remind.BuildConfig.ADMOB_OPENING_ID


    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private var exitAdView: AdView? = null // Preloaded Exit Ad

    fun init(context: Context) {
        MobileAds.initialize(context) {}
        loadInterstitial(context)
        
        // Preload Exit Ad (Medium Rectangle)
        preloadExitAd(context.applicationContext)
    }

    private fun preloadExitAd(context: Context) {
        if (exitAdView != null) return
        exitAdView = AdView(context).apply {
            setAdSize(AdSize.MEDIUM_RECTANGLE)
            adUnitId = BANNER_ID
            loadAd(AdRequest.Builder().build())
        }
    }

    fun getExitAdView(context: Context): AdView {
        // If not preloaded or context is different, return a new one but try to use cached
        val adView = exitAdView ?: AdView(context).apply {
            setAdSize(AdSize.MEDIUM_RECTANGLE)
            adUnitId = BANNER_ID
            loadAd(AdRequest.Builder().build())
        }
        // Important: Remove from parent if it was already attached
        (adView.parent as? android.view.ViewGroup)?.removeView(adView)
        return adView
    }


    fun loadInterstitial(context: Context) {
        if (isInterstitialLoading || interstitialAd != null) return
        isInterstitialLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, INTERSTITIAL_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                isInterstitialLoading = false
                
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        interstitialAd = null
                        loadInterstitial(context) // Reload for next time
                    }
                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        interstitialAd = null
                    }
                }
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitialAd = null
                isInterstitialLoading = false
            }
        })
    }

    fun showInterstitial(activity: Activity, onClosed: () -> Unit = {}) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial(activity.applicationContext)
                    onClosed()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    loadInterstitial(activity.applicationContext)
                    onClosed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            // 광고가 로드되지 않은 경우, 다음을 위해 로드만 시도하고 즉시 콜백 실행
            loadInterstitial(activity.applicationContext)
            onClosed()
        }
    }


}
