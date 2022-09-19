package com.medina.juanantonio.watcher.shared.utils

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A lazy property that gets cleaned up when the fragment's view is destroyed.
 *
 * Accessing this variable while the fragment's view is destroyed will throw NPE.
 */
class AutoClearedValue<T : Any>(val fragment: Fragment) : ReadWriteProperty<Fragment, T> {
    private var _value: T? = null
    
    init {
        fragment.lifecycle.addObserver(object: LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
               if (event == Lifecycle.Event.ON_CREATE) {
                   fragment.viewLifecycleOwnerLiveData.observe(fragment) {
                       it.lifecycle.addObserver(object : LifecycleEventObserver {
                           override fun onStateChanged(
                               source: LifecycleOwner,
                               event: Lifecycle.Event
                           ) {
                               if (event == Lifecycle.Event.ON_DESTROY) {
                                   _value = null
                               }
                           }
                       })
                   }
               }
            }
        })
    }
    
    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        return _value ?: throw IllegalStateException(
            "should never call auto-cleared-value get when it might not be available"
        )
    }
    
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T) {
        _value = value
    }
}

/**
 * Creates an [AutoClearedValue] associated with this fragment.
 */
fun <T : Any> Fragment.autoCleared() = AutoClearedValue<T>(this)