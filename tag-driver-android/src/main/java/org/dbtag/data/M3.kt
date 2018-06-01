package org.dbtag.data

import org.dbtag.driver.Parts

class M3 internal constructor(args: MessageConstructArgs) : M0Impl(args) {
    val content = args.content
    val tags by lazy { content.parseTagsOnly() }

    companion object {
        const val parts = M0Impl.parts or Parts.Content
        val construct = fun(args: MessageConstructArgs) = M3(args)
    }
}
