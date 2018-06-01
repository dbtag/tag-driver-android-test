//package org.dbtag.data
//
//import android.callback.ResultCallback
//
//import org.dbtag.driver.Parts
//
//
//class Menu2 private constructor(val colleagues: Array<YLabel>, val following: Array<YLabel>, val topics: Array<YLabel>) {
//    companion object {
//
//        fun createAsync(db: UserQueue, callback: ResultCallback<Menu2>) {
//            db.getTopicsAsync(object : ResultCallback<Array<Parts.TopicAndMaxUsePerMessage>> {
//                override fun onResult(ex: Exception?, topics: Array<Parts.TopicAndMaxUsePerMessage>?) {
//                    if (topics == null)
//                        callback.onResult(ex, null)
//                    else {
//                        if (true || db.user == "admin")
//                            callback.onResult(null, Menu2(arrayOfNulls<YLabel>(0), arrayOfNulls<YLabel>(0), topics))
//                        else {
//                            db.getColleaguesAsync(object : ResultCallback<Array<YLabel>> {
//                                override fun onResult(ex: Exception?, colleagues: Array<YLabel>?) {
//                                    if (colleagues == null)
//                                        callback.onResult(ex, null)
//                                    else {
//                                        db.getFollowingAsync(object : ResultCallback<Array<YLabel>> {
//                                            override fun onResult(ex: Exception?, following: Array<YLabel>?) {
//                                                if (following == null)
//                                                    callback.onResult(ex, null)
//                                                else
//                                                    callback.onResult(null, Menu2(colleagues, following, topics))
//                                            }
//                                        })
//                                    }
//                                }
//                            })
//                        }
//                    }
//                }
//            })
//        }
//    }
//}
