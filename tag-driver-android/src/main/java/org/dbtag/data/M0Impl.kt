package org.dbtag.data

import org.dbtag.driver.Parts

open class M0Impl(args: MessageConstructArgs) : M0 {
    val mid = args.mid
    val date = args.date
    val until = args.until
    val updated = args.updated

    override fun mid() = mid
    override fun updated() = updated
    override fun date() = date
    override fun until() = until

    companion object {
        const val parts = Parts.Mid or Parts.Date or Parts.Until or Parts.Updated
        val construct = fun(args: MessageConstructArgs) = M0Impl(args)
    }
}
