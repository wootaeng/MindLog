package com.ws.skelton.remind

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.ws.skelton.remind.utils.AdManager

class MindLogApplication : Application(), Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var currentActivity: Activity? = null

    override fun onCreate() {
        super<Application>.onCreate()
        this.registerActivityLifecycleCallbacks(this)
        
        AdManager.init(this)
        
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        // DefaultLifecycleObserver의 onStart는 기본적으로 비어있으므로 super 호출 생략 가능
        // DefaultLifecycleObserver의 onStart는 기본적으로 비어있으므로 super 호출 생략 가능
    }

    // ActivityLifecycleCallbacks
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        currentActivity = null
    }
}
